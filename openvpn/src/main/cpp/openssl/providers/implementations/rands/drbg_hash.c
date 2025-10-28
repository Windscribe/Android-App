/*
 * Copyright 2011-2025 The OpenSSL Project Authors. All Rights Reserved.
 *
 * Licensed under the Apache License 2.0 (the "License").  You may not use
 * this file except in compliance with the License.  You can obtain a copy
 * in the file LICENSE in the source distribution or at
 * https://www.openssl.org/source/license.html
 */


#include <assert.h>
#include <stdlib.h>
#include <string.h>
#include <openssl/sha.h>
#include <openssl/crypto.h>
#include <openssl/err.h>
#include <openssl/rand.h>
#include <openssl/core_dispatch.h>
#include <openssl/proverr.h>
#include "internal/cryptlib.h"
#include "internal/thread_once.h"
#include "prov/providercommon.h"
#include "prov/provider_ctx.h"
#include "prov/provider_util.h"
#include "prov/implementations.h"
#include "prov/drbg.h"
#include "crypto/evp.h"
#include "crypto/evp/evp_local.h"
#include "internal/provider.h"

static OSSL_FUNC_rand_newctx_fn drbg_hash_new_wrapper;
static OSSL_FUNC_rand_freectx_fn drbg_hash_free;
static OSSL_FUNC_rand_instantiate_fn drbg_hash_instantiate_wrapper;
static OSSL_FUNC_rand_uninstantiate_fn drbg_hash_uninstantiate_wrapper;
static OSSL_FUNC_rand_generate_fn drbg_hash_generate_wrapper;
static OSSL_FUNC_rand_reseed_fn drbg_hash_reseed_wrapper;
static OSSL_FUNC_rand_settable_ctx_params_fn drbg_hash_settable_ctx_params;
static OSSL_FUNC_rand_set_ctx_params_fn drbg_hash_set_ctx_params;
static OSSL_FUNC_rand_gettable_ctx_params_fn drbg_hash_gettable_ctx_params;
static OSSL_FUNC_rand_get_ctx_params_fn drbg_hash_get_ctx_params;
static OSSL_FUNC_rand_verify_zeroization_fn drbg_hash_verify_zeroization;

static int drbg_hash_set_ctx_params_locked
        (PROV_DRBG *drbg, const struct drbg_set_ctx_params_st *p);
static int drbg_hash_set_ctx_params_decoder(const OSSL_PARAM params[],
                                            struct drbg_set_ctx_params_st *p);

/* 888 bits from SP800-90Ar1 10.1 table 2 */
#define HASH_PRNG_MAX_SEEDLEN    (888/8)

/* 440 bits from SP800-90Ar1 10.1 table 2 */
#define HASH_PRNG_SMALL_SEEDLEN   (440/8)

/* Determine what seedlen to use based on the block length */
#define MAX_BLOCKLEN_USING_SMALL_SEEDLEN (256/8)
#define INBYTE_IGNORE ((unsigned char)0xFF)

typedef struct rand_drbg_hash_st {
    PROV_DIGEST digest;
    EVP_MD_CTX *ctx;
    size_t blocklen;
    unsigned char V[HASH_PRNG_MAX_SEEDLEN];
    unsigned char C[HASH_PRNG_MAX_SEEDLEN];
    /* Temporary value storage: should always exceed max digest length */
    unsigned char vtmp[HASH_PRNG_MAX_SEEDLEN];
} PROV_DRBG_HASH;

/*
 * SP800-90Ar1 10.3.1 Derivation function using a Hash Function (Hash_df).
 * The input string used is composed of:
 *    inbyte - An optional leading byte (ignore if equal to INBYTE_IGNORE)
 *    in - input string 1 (A Non NULL value).
 *    in2 - optional input string (Can be NULL).
 *    in3 - optional input string (Can be NULL).
 *    These are concatenated as part of the DigestUpdate process.
 */
static int hash_df(PROV_DRBG *drbg, unsigned char *out,
                   const unsigned char inbyte,
                   const unsigned char *in, size_t inlen,
                   const unsigned char *in2, size_t in2len,
                   const unsigned char *in3, size_t in3len)
{
    PROV_DRBG_HASH *hash = (PROV_DRBG_HASH *)drbg->data;
    EVP_MD_CTX *ctx = hash->ctx;
    unsigned char *vtmp = hash->vtmp;
    /* tmp = counter || num_bits_returned || [inbyte] */
    unsigned char tmp[1 + 4 + 1];
    int tmp_sz = 0;
    size_t outlen = drbg->seedlen;
    size_t num_bits_returned = outlen * 8;
    /*
     * No need to check outlen size here, as the standard only ever needs
     * seedlen bytes which is always less than the maximum permitted.
     */

    /* (Step 3) counter = 1 (tmp[0] is the 8 bit counter) */
    tmp[tmp_sz++] = 1;
    /* tmp[1..4] is the fixed 32 bit no_of_bits_to_return */
    tmp[tmp_sz++] = (unsigned char)((num_bits_returned >> 24) & 0xff);
    tmp[tmp_sz++] = (unsigned char)((num_bits_returned >> 16) & 0xff);
    tmp[tmp_sz++] = (unsigned char)((num_bits_returned >> 8) & 0xff);
    tmp[tmp_sz++] = (unsigned char)(num_bits_returned & 0xff);
    /* Tack the additional input byte onto the end of tmp if it exists */
    if (inbyte != INBYTE_IGNORE)
        tmp[tmp_sz++] = inbyte;

    /* (Step 4) */
    for (;;) {
        /*
         * (Step 4.1) out = out || Hash(tmp || in || [in2] || [in3])
         *            (where tmp = counter || num_bits_returned || [inbyte])
         */
        if (!(EVP_DigestInit_ex(ctx, ossl_prov_digest_md(&hash->digest), NULL)
                && EVP_DigestUpdate(ctx, tmp, tmp_sz)
                && EVP_DigestUpdate(ctx, in, inlen)
                && (in2 == NULL || EVP_DigestUpdate(ctx, in2, in2len))
                && (in3 == NULL || EVP_DigestUpdate(ctx, in3, in3len))))
            return 0;

        if (outlen < hash->blocklen) {
            if (!EVP_DigestFinal(ctx, vtmp, NULL))
                return 0;
            memcpy(out, vtmp, outlen);
            OPENSSL_cleanse(vtmp, hash->blocklen);
            break;
        } else if (!EVP_DigestFinal(ctx, out, NULL)) {
            return 0;
        }

        outlen -= hash->blocklen;
        if (outlen == 0)
            break;
        /* (Step 4.2) counter++ */
        tmp[0]++;
        out += hash->blocklen;
    }
    return 1;
}

/* Helper function that just passes 2 input parameters to hash_df() */
static int hash_df1(PROV_DRBG *drbg, unsigned char *out,
                    const unsigned char in_byte,
                    const unsigned char *in1, size_t in1len)
{
    return hash_df(drbg, out, in_byte, in1, in1len, NULL, 0, NULL, 0);
}

/*
 * Add 2 byte buffers together. The first elements in each buffer are the top
 * most bytes. The result is stored in the dst buffer.
 * The final carry is ignored i.e: dst =  (dst + in) mod (2^seedlen_bits).
 * where dst size is drbg->seedlen, and inlen <= drbg->seedlen.
 */
static int add_bytes(PROV_DRBG *drbg, unsigned char *dst,
                     unsigned char *in, size_t inlen)
{
    size_t i;
    int result;
    const unsigned char *add;
    unsigned char carry = 0, *d;

    assert(drbg->seedlen >= 1 && inlen >= 1 && inlen <= drbg->seedlen);

    d = &dst[drbg->seedlen - 1];
    add = &in[inlen - 1];

    for (i = inlen; i > 0; i--, d--, add--) {
        result = *d + *add + carry;
        carry = (unsigned char)(result >> 8);
        *d = (unsigned char)(result & 0xff);
    }

    if (carry != 0) {
        /* Add the carry to the top of the dst if inlen is not the same size */
        for (i = drbg->seedlen - inlen; i > 0; --i, d--) {
            *d += 1;     /* Carry can only be 1 */
            if (*d != 0) /* exit if carry doesn't propagate to the next byte */
                break;
        }
    }
    return 1;
}

/* V = (V + Hash(inbyte || V  || [additional_input]) mod (2^seedlen) */
static int add_hash_to_v(PROV_DRBG *drbg, unsigned char inbyte,
                         const unsigned char *adin, size_t adinlen)
{
    PROV_DRBG_HASH *hash = (PROV_DRBG_HASH *)drbg->data;
    EVP_MD_CTX *ctx = hash->ctx;

    return EVP_DigestInit_ex(ctx, ossl_prov_digest_md(&hash->digest), NULL)
           && EVP_DigestUpdate(ctx, &inbyte, 1)
           && EVP_DigestUpdate(ctx, hash->V, drbg->seedlen)
           && (adin == NULL || EVP_DigestUpdate(ctx, adin, adinlen))
           && EVP_DigestFinal(ctx, hash->vtmp, NULL)
           && add_bytes(drbg, hash->V, hash->vtmp, hash->blocklen);
}

/*
 * The Hashgen() as listed in SP800-90Ar1 10.1.1.4 Hash_DRBG_Generate_Process.
 *
 * drbg contains the current value of V.
 * outlen is the requested number of bytes.
 * out is a buffer to return the generated bits.
 *
 * The algorithm to generate the bits is:
 *     data = V
 *     w = NULL
 *     for (i = 1 to m) {
 *        W = W || Hash(data)
 *        data = (data + 1) mod (2^seedlen)
 *     }
 *     out = Leftmost(W, outlen)
 *
 * Returns zero if an error occurs otherwise it returns 1.
 */
static int hash_gen(PROV_DRBG *drbg, unsigned char *out, size_t outlen)
{
    PROV_DRBG_HASH *hash = (PROV_DRBG_HASH *)drbg->data;
    unsigned char one = 1;

    if (outlen == 0)
        return 1;
    memcpy(hash->vtmp, hash->V, drbg->seedlen);
    for (;;) {
        if (!EVP_DigestInit_ex(hash->ctx, ossl_prov_digest_md(&hash->digest),
                               NULL)
                || !EVP_DigestUpdate(hash->ctx, hash->vtmp, drbg->seedlen))
            return 0;

        if (outlen < hash->blocklen) {
            if (!EVP_DigestFinal(hash->ctx, hash->vtmp, NULL))
                return 0;
            memcpy(out, hash->vtmp, outlen);
            return 1;
        } else {
            if (!EVP_DigestFinal(hash->ctx, out, NULL))
                return 0;
            outlen -= hash->blocklen;
            if (outlen == 0)
                break;
            out += hash->blocklen;
        }
        add_bytes(drbg, hash->vtmp, &one, 1);
    }
    return 1;
}

/*
 * SP800-90Ar1 10.1.1.2 Hash_DRBG_Instantiate_Process:
 *
 * ent is entropy input obtained from a randomness source of length ent_len.
 * nonce is a string of bytes of length nonce_len.
 * pstr is a personalization string received from an application. May be NULL.
 *
 * Returns zero if an error occurs otherwise it returns 1.
 */
static int drbg_hash_instantiate(PROV_DRBG *drbg,
                                 const unsigned char *ent, size_t ent_len,
                                 const unsigned char *nonce, size_t nonce_len,
                                 const unsigned char *pstr, size_t pstr_len)
{
    PROV_DRBG_HASH *hash = (PROV_DRBG_HASH *)drbg->data;

    EVP_MD_CTX_free(hash->ctx);
    hash->ctx = EVP_MD_CTX_new();

    /* (Step 1-3) V = Hash_df(entropy||nonce||pers, seedlen) */
    return hash->ctx != NULL
           && hash_df(drbg, hash->V, INBYTE_IGNORE,
                      ent, ent_len, nonce, nonce_len, pstr, pstr_len)
           /* (Step 4) C = Hash_df(0x00||V, seedlen) */
           && hash_df1(drbg, hash->C, 0x00, hash->V, drbg->seedlen);
}

static int drbg_hash_instantiate_wrapper(void *vdrbg, unsigned int strength,
                                         int prediction_resistance,
                                         const unsigned char *pstr,
                                         size_t pstr_len,
                                         const OSSL_PARAM params[])
{
    PROV_DRBG *drbg = (PROV_DRBG *)vdrbg;
    struct drbg_set_ctx_params_st p;
    int ret = 0;

    if (drbg == NULL || !drbg_hash_set_ctx_params_decoder(params, &p))
        return 0;

    if (drbg->lock != NULL && !CRYPTO_THREAD_write_lock(drbg->lock))
        return 0;

    if (!ossl_prov_is_running()
            || !drbg_hash_set_ctx_params_locked(drbg, &p))
        goto err;
    ret = ossl_prov_drbg_instantiate(drbg, strength, prediction_resistance,
                                     pstr, pstr_len);
 err:
    if (drbg->lock != NULL)
        CRYPTO_THREAD_unlock(drbg->lock);
    return ret;
}

/*
 * SP800-90Ar1 10.1.1.3 Hash_DRBG_Reseed_Process:
 *
 * ent is entropy input bytes obtained from a randomness source.
 * addin is additional input received from an application. May be NULL.
 *
 * Returns zero if an error occurs otherwise it returns 1.
 */
static int drbg_hash_reseed(PROV_DRBG *drbg,
                            const unsigned char *ent, size_t ent_len,
                            const unsigned char *adin, size_t adin_len)
{
    PROV_DRBG_HASH *hash = (PROV_DRBG_HASH *)drbg->data;

    /* (Step 1-2) V = Hash_df(0x01 || V || entropy_input || additional_input) */
    /* V about to be updated so use C as output instead */
    if (!hash_df(drbg, hash->C, 0x01, hash->V, drbg->seedlen, ent, ent_len,
                 adin, adin_len))
        return 0;
    memcpy(hash->V, hash->C, drbg->seedlen);
    /* (Step 4) C = Hash_df(0x00||V, seedlen) */
    return hash_df1(drbg, hash->C, 0x00, hash->V, drbg->seedlen);
}

static int drbg_hash_reseed_wrapper(void *vdrbg, int prediction_resistance,
                                    const unsigned char *ent, size_t ent_len,
                                    const unsigned char *adin, size_t adin_len)
{
    PROV_DRBG *drbg = (PROV_DRBG *)vdrbg;

    return ossl_prov_drbg_reseed(drbg, prediction_resistance, ent, ent_len,
                                 adin, adin_len);
}

/*
 * SP800-90Ar1 10.1.1.4 Hash_DRBG_Generate_Process:
 *
 * Generates pseudo random bytes using the drbg.
 * out is a buffer to fill with outlen bytes of pseudo random data.
 * addin is additional input received from an application. May be NULL.
 *
 * Returns zero if an error occurs otherwise it returns 1.
 */
static int drbg_hash_generate(PROV_DRBG *drbg,
                              unsigned char *out, size_t outlen,
                              const unsigned char *adin, size_t adin_len)
{
    PROV_DRBG_HASH *hash = (PROV_DRBG_HASH *)drbg->data;
    unsigned char counter[4];
    int reseed_counter = drbg->generate_counter;

    counter[0] = (unsigned char)((reseed_counter >> 24) & 0xff);
    counter[1] = (unsigned char)((reseed_counter >> 16) & 0xff);
    counter[2] = (unsigned char)((reseed_counter >> 8) & 0xff);
    counter[3] = (unsigned char)(reseed_counter & 0xff);

    return hash->ctx != NULL
           && (adin == NULL
           /* (Step 2) if adin != NULL then V = V + Hash(0x02||V||adin) */
               || adin_len == 0
               || add_hash_to_v(drbg, 0x02, adin, adin_len))
           /* (Step 3) Hashgen(outlen, V) */
           && hash_gen(drbg, out, outlen)
           /* (Step 4/5) H = V = (V + Hash(0x03||V) mod (2^seedlen_bits) */
           && add_hash_to_v(drbg, 0x03, NULL, 0)
           /* (Step 5) V = (V + H + C + reseed_counter) mod (2^seedlen_bits) */
           /* V = (V + C) mod (2^seedlen_bits) */
           && add_bytes(drbg, hash->V, hash->C, drbg->seedlen)
           /* V = (V + reseed_counter) mod (2^seedlen_bits) */
           && add_bytes(drbg, hash->V, counter, 4);
}

static int drbg_hash_generate_wrapper
    (void *vdrbg, unsigned char *out, size_t outlen, unsigned int strength,
     int prediction_resistance, const unsigned char *adin, size_t adin_len)
{
    PROV_DRBG *drbg = (PROV_DRBG *)vdrbg;

    return ossl_prov_drbg_generate(drbg, out, outlen, strength,
                                   prediction_resistance, adin, adin_len);
}

static int drbg_hash_uninstantiate(PROV_DRBG *drbg)
{
    PROV_DRBG_HASH *hash = (PROV_DRBG_HASH *)drbg->data;

    OPENSSL_cleanse(hash->V, sizeof(hash->V));
    OPENSSL_cleanse(hash->C, sizeof(hash->C));
    OPENSSL_cleanse(hash->vtmp, sizeof(hash->vtmp));
    return ossl_prov_drbg_uninstantiate(drbg);
}

static int drbg_hash_uninstantiate_wrapper(void *vdrbg)
{
    PROV_DRBG *drbg = (PROV_DRBG *)vdrbg;
    int ret;

    if (drbg->lock != NULL && !CRYPTO_THREAD_write_lock(drbg->lock))
        return 0;

    ret = drbg_hash_uninstantiate(drbg);

    if (drbg->lock != NULL)
        CRYPTO_THREAD_unlock(drbg->lock);

    return ret;
}

static int drbg_hash_verify_zeroization(void *vdrbg)
{
    PROV_DRBG *drbg = (PROV_DRBG *)vdrbg;
    PROV_DRBG_HASH *hash = (PROV_DRBG_HASH *)drbg->data;
    int ret = 0;

    if (drbg->lock != NULL && !CRYPTO_THREAD_read_lock(drbg->lock))
        return 0;

    PROV_DRBG_VERIFY_ZEROIZATION(hash->V);
    PROV_DRBG_VERIFY_ZEROIZATION(hash->C);
    PROV_DRBG_VERIFY_ZEROIZATION(hash->vtmp);

    ret = 1;
 err:
    if (drbg->lock != NULL)
        CRYPTO_THREAD_unlock(drbg->lock);
    return ret;
}

static int drbg_hash_new(PROV_DRBG *ctx)
{
    PROV_DRBG_HASH *hash;

    hash = OPENSSL_secure_zalloc(sizeof(*hash));
    if (hash == NULL)
        return 0;

    OSSL_FIPS_IND_INIT(ctx)

    ctx->data = hash;
    ctx->seedlen = HASH_PRNG_MAX_SEEDLEN;
    ctx->max_entropylen = DRBG_MAX_LENGTH;
    ctx->max_noncelen = DRBG_MAX_LENGTH;
    ctx->max_perslen = DRBG_MAX_LENGTH;
    ctx->max_adinlen = DRBG_MAX_LENGTH;

    /* Maximum number of bits per request = 2^19  = 2^16 bytes */
    ctx->max_request = 1 << 16;
    return 1;
}

static void *drbg_hash_new_wrapper(void *provctx, void *parent,
                                   const OSSL_DISPATCH *parent_dispatch)
{
    return ossl_rand_drbg_new(provctx, parent, parent_dispatch,
                              &drbg_hash_new, &drbg_hash_free,
                              &drbg_hash_instantiate, &drbg_hash_uninstantiate,
                              &drbg_hash_reseed, &drbg_hash_generate);
}

static void drbg_hash_free(void *vdrbg)
{
    PROV_DRBG *drbg = (PROV_DRBG *)vdrbg;
    PROV_DRBG_HASH *hash;

    if (drbg != NULL && (hash = (PROV_DRBG_HASH *)drbg->data) != NULL) {
        EVP_MD_CTX_free(hash->ctx);
        ossl_prov_digest_reset(&hash->digest);
        OPENSSL_secure_clear_free(hash, sizeof(*hash));
    }
    ossl_rand_drbg_free(drbg);
}

#define drbg_hash_get_ctx_params_st  drbg_get_ctx_params_st

/* Machine generated by util/perl/OpenSSL/paramnames.pm */
#ifndef drbg_hash_get_ctx_params_list
static const OSSL_PARAM drbg_hash_get_ctx_params_list[] = {
    OSSL_PARAM_utf8_string(OSSL_DRBG_PARAM_DIGEST, NULL, 0),
    OSSL_PARAM_int(OSSL_RAND_PARAM_STATE, NULL),
    OSSL_PARAM_uint(OSSL_RAND_PARAM_STRENGTH, NULL),
    OSSL_PARAM_size_t(OSSL_RAND_PARAM_MAX_REQUEST, NULL),
    OSSL_PARAM_size_t(OSSL_DRBG_PARAM_MIN_ENTROPYLEN, NULL),
    OSSL_PARAM_size_t(OSSL_DRBG_PARAM_MAX_ENTROPYLEN, NULL),
    OSSL_PARAM_size_t(OSSL_DRBG_PARAM_MIN_NONCELEN, NULL),
    OSSL_PARAM_size_t(OSSL_DRBG_PARAM_MAX_NONCELEN, NULL),
    OSSL_PARAM_size_t(OSSL_DRBG_PARAM_MAX_PERSLEN, NULL),
    OSSL_PARAM_size_t(OSSL_DRBG_PARAM_MAX_ADINLEN, NULL),
    OSSL_PARAM_uint(OSSL_DRBG_PARAM_RESEED_COUNTER, NULL),
    OSSL_PARAM_time_t(OSSL_DRBG_PARAM_RESEED_TIME, NULL),
    OSSL_PARAM_uint(OSSL_DRBG_PARAM_RESEED_REQUESTS, NULL),
    OSSL_PARAM_uint64(OSSL_DRBG_PARAM_RESEED_TIME_INTERVAL, NULL),
# if defined(FIPS_MODULE)
    OSSL_PARAM_int(OSSL_KDF_PARAM_FIPS_APPROVED_INDICATOR, NULL),
# endif
    OSSL_PARAM_END
};
#endif

#ifndef drbg_hash_get_ctx_params_st
struct drbg_hash_get_ctx_params_st {
    OSSL_PARAM *digest;
# if defined(FIPS_MODULE)
    OSSL_PARAM *ind;
# endif
    OSSL_PARAM *maxadlen;
    OSSL_PARAM *maxentlen;
    OSSL_PARAM *maxnonlen;
    OSSL_PARAM *maxperlen;
    OSSL_PARAM *maxreq;
    OSSL_PARAM *minentlen;
    OSSL_PARAM *minnonlen;
    OSSL_PARAM *reseed_cnt;
    OSSL_PARAM *reseed_int;
    OSSL_PARAM *reseed_req;
    OSSL_PARAM *reseed_time;
    OSSL_PARAM *state;
    OSSL_PARAM *str;
};
#endif

#ifndef drbg_hash_get_ctx_params_decoder
static int drbg_hash_get_ctx_params_decoder
    (const OSSL_PARAM *p, struct drbg_hash_get_ctx_params_st *r)
{
    const char *s;

    memset(r, 0, sizeof(*r));
    if (p != NULL)
        for (; (s = p->key) != NULL; p++)
            switch(s[0]) {
            default:
                break;
            case 'd':
                if (ossl_likely(strcmp("igest", s + 1) == 0)) {
                    /* OSSL_DRBG_PARAM_DIGEST */
                    if (ossl_unlikely(r->digest != NULL)) {
                        ERR_raise_data(ERR_LIB_PROV, PROV_R_REPEATED_PARAMETER,
                                       "param %s is repeated", s);
                        return 0;
                    }
                    r->digest = (OSSL_PARAM *)p;
                }
                break;
            case 'f':
# if defined(FIPS_MODULE)
                if (ossl_likely(strcmp("ips-indicator", s + 1) == 0)) {
                    /* OSSL_KDF_PARAM_FIPS_APPROVED_INDICATOR */
                    if (ossl_unlikely(r->ind != NULL)) {
                        ERR_raise_data(ERR_LIB_PROV, PROV_R_REPEATED_PARAMETER,
                                       "param %s is repeated", s);
                        return 0;
                    }
                    r->ind = (OSSL_PARAM *)p;
                }
# endif
                break;
            case 'm':
                switch(s[1]) {
                default:
                    break;
                case 'a':
                    switch(s[2]) {
                    default:
                        break;
                    case 'x':
                        switch(s[3]) {
                        default:
                            break;
                        case '_':
                            switch(s[4]) {
                            default:
                                break;
                            case 'a':
                                if (ossl_likely(strcmp("dinlen", s + 5) == 0)) {
                                    /* OSSL_DRBG_PARAM_MAX_ADINLEN */
                                    if (ossl_unlikely(r->maxadlen != NULL)) {
                                        ERR_raise_data(ERR_LIB_PROV, PROV_R_REPEATED_PARAMETER,
                                                       "param %s is repeated", s);
                                        return 0;
                                    }
                                    r->maxadlen = (OSSL_PARAM *)p;
                                }
                                break;
                            case 'e':
                                if (ossl_likely(strcmp("ntropylen", s + 5) == 0)) {
                                    /* OSSL_DRBG_PARAM_MAX_ENTROPYLEN */
                                    if (ossl_unlikely(r->maxentlen != NULL)) {
                                        ERR_raise_data(ERR_LIB_PROV, PROV_R_REPEATED_PARAMETER,
                                                       "param %s is repeated", s);
                                        return 0;
                                    }
                                    r->maxentlen = (OSSL_PARAM *)p;
                                }
                                break;
                            case 'n':
                                if (ossl_likely(strcmp("oncelen", s + 5) == 0)) {
                                    /* OSSL_DRBG_PARAM_MAX_NONCELEN */
                                    if (ossl_unlikely(r->maxnonlen != NULL)) {
                                        ERR_raise_data(ERR_LIB_PROV, PROV_R_REPEATED_PARAMETER,
                                                       "param %s is repeated", s);
                                        return 0;
                                    }
                                    r->maxnonlen = (OSSL_PARAM *)p;
                                }
                                break;
                            case 'p':
                                if (ossl_likely(strcmp("erslen", s + 5) == 0)) {
                                    /* OSSL_DRBG_PARAM_MAX_PERSLEN */
                                    if (ossl_unlikely(r->maxperlen != NULL)) {
                                        ERR_raise_data(ERR_LIB_PROV, PROV_R_REPEATED_PARAMETER,
                                                       "param %s is repeated", s);
                                        return 0;
                                    }
                                    r->maxperlen = (OSSL_PARAM *)p;
                                }
                                break;
                            case 'r':
                                if (ossl_likely(strcmp("equest", s + 5) == 0)) {
                                    /* OSSL_RAND_PARAM_MAX_REQUEST */
                                    if (ossl_unlikely(r->maxreq != NULL)) {
                                        ERR_raise_data(ERR_LIB_PROV, PROV_R_REPEATED_PARAMETER,
                                                       "param %s is repeated", s);
                                        return 0;
                                    }
                                    r->maxreq = (OSSL_PARAM *)p;
                                }
                            }
                        }
                    }
                    break;
                case 'i':
                    switch(s[2]) {
                    default:
                        break;
                    case 'n':
                        switch(s[3]) {
                        default:
                            break;
                        case '_':
                            switch(s[4]) {
                            default:
                                break;
                            case 'e':
                                if (ossl_likely(strcmp("ntropylen", s + 5) == 0)) {
                                    /* OSSL_DRBG_PARAM_MIN_ENTROPYLEN */
                                    if (ossl_unlikely(r->minentlen != NULL)) {
                                        ERR_raise_data(ERR_LIB_PROV, PROV_R_REPEATED_PARAMETER,
                                                       "param %s is repeated", s);
                                        return 0;
                                    }
                                    r->minentlen = (OSSL_PARAM *)p;
                                }
                                break;
                            case 'n':
                                if (ossl_likely(strcmp("oncelen", s + 5) == 0)) {
                                    /* OSSL_DRBG_PARAM_MIN_NONCELEN */
                                    if (ossl_unlikely(r->minnonlen != NULL)) {
                                        ERR_raise_data(ERR_LIB_PROV, PROV_R_REPEATED_PARAMETER,
                                                       "param %s is repeated", s);
                                        return 0;
                                    }
                                    r->minnonlen = (OSSL_PARAM *)p;
                                }
                            }
                        }
                    }
                }
                break;
            case 'r':
                switch(s[1]) {
                default:
                    break;
                case 'e':
                    switch(s[2]) {
                    default:
                        break;
                    case 's':
                        switch(s[3]) {
                        default:
                            break;
                        case 'e':
                            switch(s[4]) {
                            default:
                                break;
                            case 'e':
                                switch(s[5]) {
                                default:
                                    break;
                                case 'd':
                                    switch(s[6]) {
                                    default:
                                        break;
                                    case '_':
                                        switch(s[7]) {
                                        default:
                                            break;
                                        case 'c':
                                            if (ossl_likely(strcmp("ounter", s + 8) == 0)) {
                                                /* OSSL_DRBG_PARAM_RESEED_COUNTER */
                                                if (ossl_unlikely(r->reseed_cnt != NULL)) {
                                                    ERR_raise_data(ERR_LIB_PROV, PROV_R_REPEATED_PARAMETER,
                                                                   "param %s is repeated", s);
                                                    return 0;
                                                }
                                                r->reseed_cnt = (OSSL_PARAM *)p;
                                            }
                                            break;
                                        case 'r':
                                            if (ossl_likely(strcmp("equests", s + 8) == 0)) {
                                                /* OSSL_DRBG_PARAM_RESEED_REQUESTS */
                                                if (ossl_unlikely(r->reseed_req != NULL)) {
                                                    ERR_raise_data(ERR_LIB_PROV, PROV_R_REPEATED_PARAMETER,
                                                                   "param %s is repeated", s);
                                                    return 0;
                                                }
                                                r->reseed_req = (OSSL_PARAM *)p;
                                            }
                                            break;
                                        case 't':
                                            switch(s[8]) {
                                            default:
                                                break;
                                            case 'i':
                                                switch(s[9]) {
                                                default:
                                                    break;
                                                case 'm':
                                                    switch(s[10]) {
                                                    default:
                                                        break;
                                                    case 'e':
                                                        switch(s[11]) {
                                                        default:
                                                            break;
                                                        case '_':
                                                            if (ossl_likely(strcmp("interval", s + 12) == 0)) {
                                                                /* OSSL_DRBG_PARAM_RESEED_TIME_INTERVAL */
                                                                if (ossl_unlikely(r->reseed_int != NULL)) {
                                                                    ERR_raise_data(ERR_LIB_PROV, PROV_R_REPEATED_PARAMETER,
                                                                                   "param %s is repeated", s);
                                                                    return 0;
                                                                }
                                                                r->reseed_int = (OSSL_PARAM *)p;
                                                            }
                                                            break;
                                                        case '\0':
                                                            if (ossl_unlikely(r->reseed_time != NULL)) {
                                                                ERR_raise_data(ERR_LIB_PROV, PROV_R_REPEATED_PARAMETER,
                                                                               "param %s is repeated", s);
                                                                return 0;
                                                            }
                                                            r->reseed_time = (OSSL_PARAM *)p;
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                break;
            case 's':
                switch(s[1]) {
                default:
                    break;
                case 't':
                    switch(s[2]) {
                    default:
                        break;
                    case 'a':
                        if (ossl_likely(strcmp("te", s + 3) == 0)) {
                            /* OSSL_RAND_PARAM_STATE */
                            if (ossl_unlikely(r->state != NULL)) {
                                ERR_raise_data(ERR_LIB_PROV, PROV_R_REPEATED_PARAMETER,
                                               "param %s is repeated", s);
                                return 0;
                            }
                            r->state = (OSSL_PARAM *)p;
                        }
                        break;
                    case 'r':
                        if (ossl_likely(strcmp("ength", s + 3) == 0)) {
                            /* OSSL_RAND_PARAM_STRENGTH */
                            if (ossl_unlikely(r->str != NULL)) {
                                ERR_raise_data(ERR_LIB_PROV, PROV_R_REPEATED_PARAMETER,
                                               "param %s is repeated", s);
                                return 0;
                            }
                            r->str = (OSSL_PARAM *)p;
                        }
                    }
                }
            }
    return 1;
}
#endif
/* End of machine generated */

static int drbg_hash_get_ctx_params(void *vdrbg, OSSL_PARAM params[])
{
    PROV_DRBG *drbg = (PROV_DRBG *)vdrbg;
    PROV_DRBG_HASH *hash;
    const EVP_MD *md;
    struct drbg_get_ctx_params_st p;
    int ret = 0, complete = 0;

    if (drbg == NULL || !drbg_hash_get_ctx_params_decoder(params, &p))
        return 0;

    if (!ossl_drbg_get_ctx_params_no_lock(drbg, &p, params, &complete))
        return 0;

    if (complete)
        return 1;

    hash = (PROV_DRBG_HASH *)drbg->data;

    if (drbg->lock != NULL && !CRYPTO_THREAD_read_lock(drbg->lock))
        return 0;

    if (p.digest != NULL) {
        md = ossl_prov_digest_md(&hash->digest);
        if (md == NULL
                || !OSSL_PARAM_set_utf8_string(p.digest, EVP_MD_get0_name(md)))
            goto err;
    }

    ret = ossl_drbg_get_ctx_params(drbg, &p);
 err:
    if (drbg->lock != NULL)
        CRYPTO_THREAD_unlock(drbg->lock);

    return ret;
}

static const OSSL_PARAM *drbg_hash_gettable_ctx_params(ossl_unused void *vctx,
                                                       ossl_unused void *p_ctx)
{
    return drbg_hash_get_ctx_params_list;
}

static int drbg_fetch_digest_from_prov(const struct drbg_set_ctx_params_st *p,
                                       OSSL_LIB_CTX *libctx,
                                       EVP_MD **digest)
{
    OSSL_PROVIDER *prov = NULL;
    EVP_MD *md = NULL;
    int ret = 0;

    if (digest == NULL)
        return 0;

    if (p->prov == NULL || p->prov->data_type != OSSL_PARAM_UTF8_STRING)
        return 0;
    if ((prov = ossl_provider_find(libctx, (const char *)p->prov->data, 1)) == NULL)
        return 0;

    if (p->digest == NULL) {
        ret = 1;
        goto done;
    }

    if (p->digest->data_type != OSSL_PARAM_UTF8_STRING)
        goto done;

    md = evp_digest_fetch_from_prov(prov, (const char *)p->digest->data, NULL);
    if (md) {
        EVP_MD_free(*digest);
        *digest = md;
        ret = 1;
    }

done:
    ossl_provider_free(prov);
    return ret;
}

static int drbg_hash_set_ctx_params_locked
        (PROV_DRBG *ctx, const struct drbg_set_ctx_params_st *p)
{
    PROV_DRBG_HASH *hash = (PROV_DRBG_HASH *)ctx->data;
    OSSL_LIB_CTX *libctx = PROV_LIBCTX_OF(ctx->provctx);
    EVP_MD *prov_md = NULL;
    const EVP_MD *md;
    int md_size;

    if (!OSSL_FIPS_IND_SET_CTX_FROM_PARAM(ctx, OSSL_FIPS_IND_SETTABLE0, p->ind_d))
        return 0;

    /* try to fetch digest from provider */
    (void)ERR_set_mark();
    if (!drbg_fetch_digest_from_prov(p, libctx, &prov_md)) {
        (void)ERR_pop_to_mark();
        /* fall back to full implementation search */
        if (!ossl_prov_digest_load(&hash->digest, p->digest, p->propq,
                                   p->engine, libctx))
            return 0;
    } else {
        (void)ERR_clear_last_mark();
        if (prov_md)
            ossl_prov_digest_set_md(&hash->digest, prov_md);
    }

    md = ossl_prov_digest_md(&hash->digest);
    if (md != NULL) {
        if (!ossl_drbg_verify_digest(ctx, libctx, md))
            return 0;   /* Error already raised for us */

        /* These are taken from SP 800-90 10.1 Table 2 */
        md_size = EVP_MD_get_size(md);
        if (md_size <= 0)
            return 0;
        hash->blocklen = md_size;
        /* See SP800-57 Part1 Rev4 5.6.1 Table 3 */
        ctx->strength = (unsigned int)(64 * (hash->blocklen >> 3));
        if (ctx->strength > 256)
            ctx->strength = 256;
        if (hash->blocklen > MAX_BLOCKLEN_USING_SMALL_SEEDLEN)
            ctx->seedlen = HASH_PRNG_MAX_SEEDLEN;
        else
            ctx->seedlen = HASH_PRNG_SMALL_SEEDLEN;

        ctx->min_entropylen = ctx->strength / 8;
        ctx->min_noncelen = ctx->min_entropylen / 2;
    }

    return ossl_drbg_set_ctx_params(ctx, p);
}

#define drbg_hash_set_ctx_params_st  drbg_set_ctx_params_st

/* Machine generated by util/perl/OpenSSL/paramnames.pm */
#ifndef drbg_hash_set_ctx_params_list
static const OSSL_PARAM drbg_hash_set_ctx_params_list[] = {
    OSSL_PARAM_utf8_string(OSSL_DRBG_PARAM_PROPERTIES, NULL, 0),
    OSSL_PARAM_utf8_string(OSSL_DRBG_PARAM_DIGEST, NULL, 0),
    OSSL_PARAM_utf8_string(OSSL_PROV_PARAM_CORE_PROV_NAME, NULL, 0),
    OSSL_PARAM_uint(OSSL_DRBG_PARAM_RESEED_REQUESTS, NULL),
    OSSL_PARAM_uint64(OSSL_DRBG_PARAM_RESEED_TIME_INTERVAL, NULL),
# if defined(FIPS_MODULE)
    OSSL_PARAM_int(OSSL_KDF_PARAM_FIPS_DIGEST_CHECK, NULL),
# endif
    OSSL_PARAM_END
};
#endif

#ifndef drbg_hash_set_ctx_params_st
struct drbg_hash_set_ctx_params_st {
    OSSL_PARAM *digest;
    OSSL_PARAM *engine;
# if defined(FIPS_MODULE)
    OSSL_PARAM *ind_d;
# endif
    OSSL_PARAM *propq;
    OSSL_PARAM *prov;
    OSSL_PARAM *reseed_req;
    OSSL_PARAM *reseed_time;
};
#endif

#ifndef drbg_hash_set_ctx_params_decoder
static int drbg_hash_set_ctx_params_decoder
    (const OSSL_PARAM *p, struct drbg_hash_set_ctx_params_st *r)
{
    const char *s;

    memset(r, 0, sizeof(*r));
    if (p != NULL)
        for (; (s = p->key) != NULL; p++)
            switch(s[0]) {
            default:
                break;
            case 'd':
                switch(s[1]) {
                default:
                    break;
                case 'i':
                    switch(s[2]) {
                    default:
                        break;
                    case 'g':
                        switch(s[3]) {
                        default:
                            break;
                        case 'e':
                            switch(s[4]) {
                            default:
                                break;
                            case 's':
                                switch(s[5]) {
                                default:
                                    break;
                                case 't':
                                    switch(s[6]) {
                                    default:
                                        break;
                                    case '-':
# if defined(FIPS_MODULE)
                                        if (ossl_likely(strcmp("check", s + 7) == 0)) {
                                            /* OSSL_KDF_PARAM_FIPS_DIGEST_CHECK */
                                            if (ossl_unlikely(r->ind_d != NULL)) {
                                                ERR_raise_data(ERR_LIB_PROV, PROV_R_REPEATED_PARAMETER,
                                                               "param %s is repeated", s);
                                                return 0;
                                            }
                                            r->ind_d = (OSSL_PARAM *)p;
                                        }
# endif
                                        break;
                                    case '\0':
                                        if (ossl_unlikely(r->digest != NULL)) {
                                            ERR_raise_data(ERR_LIB_PROV, PROV_R_REPEATED_PARAMETER,
                                                           "param %s is repeated", s);
                                            return 0;
                                        }
                                        r->digest = (OSSL_PARAM *)p;
                                    }
                                }
                            }
                        }
                    }
                }
                break;
            case 'e':
                if (ossl_likely(strcmp("ngine", s + 1) == 0)) {
                    /* OSSL_ALG_PARAM_ENGINE */
                    if (ossl_unlikely(r->engine != NULL)) {
                        ERR_raise_data(ERR_LIB_PROV, PROV_R_REPEATED_PARAMETER,
                                       "param %s is repeated", s);
                        return 0;
                    }
                    r->engine = (OSSL_PARAM *)p;
                }
                break;
            case 'p':
                switch(s[1]) {
                default:
                    break;
                case 'r':
                    switch(s[2]) {
                    default:
                        break;
                    case 'o':
                        switch(s[3]) {
                        default:
                            break;
                        case 'p':
                            if (ossl_likely(strcmp("erties", s + 4) == 0)) {
                                /* OSSL_DRBG_PARAM_PROPERTIES */
                                if (ossl_unlikely(r->propq != NULL)) {
                                    ERR_raise_data(ERR_LIB_PROV, PROV_R_REPEATED_PARAMETER,
                                                   "param %s is repeated", s);
                                    return 0;
                                }
                                r->propq = (OSSL_PARAM *)p;
                            }
                            break;
                        case 'v':
                            if (ossl_likely(strcmp("ider-name", s + 4) == 0)) {
                                /* OSSL_PROV_PARAM_CORE_PROV_NAME */
                                if (ossl_unlikely(r->prov != NULL)) {
                                    ERR_raise_data(ERR_LIB_PROV, PROV_R_REPEATED_PARAMETER,
                                                   "param %s is repeated", s);
                                    return 0;
                                }
                                r->prov = (OSSL_PARAM *)p;
                            }
                        }
                    }
                }
                break;
            case 'r':
                switch(s[1]) {
                default:
                    break;
                case 'e':
                    switch(s[2]) {
                    default:
                        break;
                    case 's':
                        switch(s[3]) {
                        default:
                            break;
                        case 'e':
                            switch(s[4]) {
                            default:
                                break;
                            case 'e':
                                switch(s[5]) {
                                default:
                                    break;
                                case 'd':
                                    switch(s[6]) {
                                    default:
                                        break;
                                    case '_':
                                        switch(s[7]) {
                                        default:
                                            break;
                                        case 'r':
                                            if (ossl_likely(strcmp("equests", s + 8) == 0)) {
                                                /* OSSL_DRBG_PARAM_RESEED_REQUESTS */
                                                if (ossl_unlikely(r->reseed_req != NULL)) {
                                                    ERR_raise_data(ERR_LIB_PROV, PROV_R_REPEATED_PARAMETER,
                                                                   "param %s is repeated", s);
                                                    return 0;
                                                }
                                                r->reseed_req = (OSSL_PARAM *)p;
                                            }
                                            break;
                                        case 't':
                                            if (ossl_likely(strcmp("ime_interval", s + 8) == 0)) {
                                                /* OSSL_DRBG_PARAM_RESEED_TIME_INTERVAL */
                                                if (ossl_unlikely(r->reseed_time != NULL)) {
                                                    ERR_raise_data(ERR_LIB_PROV, PROV_R_REPEATED_PARAMETER,
                                                                   "param %s is repeated", s);
                                                    return 0;
                                                }
                                                r->reseed_time = (OSSL_PARAM *)p;
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
    return 1;
}
#endif
/* End of machine generated */

static int drbg_hash_set_ctx_params(void *vctx, const OSSL_PARAM params[])
{
    PROV_DRBG *drbg = (PROV_DRBG *)vctx;
    struct drbg_set_ctx_params_st p;
    int ret;

    if (drbg == NULL || !drbg_hash_set_ctx_params_decoder(params, &p))
        return 0;

    if (drbg->lock != NULL && !CRYPTO_THREAD_write_lock(drbg->lock))
        return 0;

    ret = drbg_hash_set_ctx_params_locked(drbg, &p);

    if (drbg->lock != NULL)
        CRYPTO_THREAD_unlock(drbg->lock);

    return ret;
}

static const OSSL_PARAM *drbg_hash_settable_ctx_params(ossl_unused void *vctx,
                                                       ossl_unused void *p_ctx)
{
    return drbg_hash_set_ctx_params_list;
}

const OSSL_DISPATCH ossl_drbg_hash_functions[] = {
    { OSSL_FUNC_RAND_NEWCTX, (void(*)(void))drbg_hash_new_wrapper },
    { OSSL_FUNC_RAND_FREECTX, (void(*)(void))drbg_hash_free },
    { OSSL_FUNC_RAND_INSTANTIATE,
      (void(*)(void))drbg_hash_instantiate_wrapper },
    { OSSL_FUNC_RAND_UNINSTANTIATE,
      (void(*)(void))drbg_hash_uninstantiate_wrapper },
    { OSSL_FUNC_RAND_GENERATE, (void(*)(void))drbg_hash_generate_wrapper },
    { OSSL_FUNC_RAND_RESEED, (void(*)(void))drbg_hash_reseed_wrapper },
    { OSSL_FUNC_RAND_ENABLE_LOCKING, (void(*)(void))ossl_drbg_enable_locking },
    { OSSL_FUNC_RAND_LOCK, (void(*)(void))ossl_drbg_lock },
    { OSSL_FUNC_RAND_UNLOCK, (void(*)(void))ossl_drbg_unlock },
    { OSSL_FUNC_RAND_SETTABLE_CTX_PARAMS,
      (void(*)(void))drbg_hash_settable_ctx_params },
    { OSSL_FUNC_RAND_SET_CTX_PARAMS, (void(*)(void))drbg_hash_set_ctx_params },
    { OSSL_FUNC_RAND_GETTABLE_CTX_PARAMS,
      (void(*)(void))drbg_hash_gettable_ctx_params },
    { OSSL_FUNC_RAND_GET_CTX_PARAMS, (void(*)(void))drbg_hash_get_ctx_params },
    { OSSL_FUNC_RAND_VERIFY_ZEROIZATION,
      (void(*)(void))drbg_hash_verify_zeroization },
    { OSSL_FUNC_RAND_GET_SEED, (void(*)(void))ossl_drbg_get_seed },
    { OSSL_FUNC_RAND_CLEAR_SEED, (void(*)(void))ossl_drbg_clear_seed },
    OSSL_DISPATCH_END
};
