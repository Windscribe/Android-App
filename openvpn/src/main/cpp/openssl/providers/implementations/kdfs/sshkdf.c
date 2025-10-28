/*
 * Copyright 2018-2025 The OpenSSL Project Authors. All Rights Reserved.
 *
 * Licensed under the Apache License 2.0 (the "License").  You may not use
 * this file except in compliance with the License.  You can obtain a copy
 * in the file LICENSE in the source distribution or at
 * https://www.openssl.org/source/license.html
 */


#include <stdlib.h>
#include <stdarg.h>
#include <string.h>
#include <openssl/evp.h>
#include <openssl/kdf.h>
#include <openssl/core_names.h>
#include <openssl/proverr.h>
#include "internal/cryptlib.h"
#include "internal/numbers.h"
#include "crypto/evp.h"
#include "prov/provider_ctx.h"
#include "prov/providercommon.h"
#include "prov/implementations.h"
#include "prov/provider_util.h"
#include "prov/securitycheck.h"

/* See RFC 4253, Section 7.2 */
static OSSL_FUNC_kdf_newctx_fn kdf_sshkdf_new;
static OSSL_FUNC_kdf_dupctx_fn kdf_sshkdf_dup;
static OSSL_FUNC_kdf_freectx_fn kdf_sshkdf_free;
static OSSL_FUNC_kdf_reset_fn kdf_sshkdf_reset;
static OSSL_FUNC_kdf_derive_fn kdf_sshkdf_derive;
static OSSL_FUNC_kdf_settable_ctx_params_fn kdf_sshkdf_settable_ctx_params;
static OSSL_FUNC_kdf_set_ctx_params_fn kdf_sshkdf_set_ctx_params;
static OSSL_FUNC_kdf_gettable_ctx_params_fn kdf_sshkdf_gettable_ctx_params;
static OSSL_FUNC_kdf_get_ctx_params_fn kdf_sshkdf_get_ctx_params;

static int SSHKDF(const EVP_MD *evp_md,
                  const unsigned char *key, size_t key_len,
                  const unsigned char *xcghash, size_t xcghash_len,
                  const unsigned char *session_id, size_t session_id_len,
                  char type, unsigned char *okey, size_t okey_len);

typedef struct {
    void *provctx;
    PROV_DIGEST digest;
    unsigned char *key; /* K */
    size_t key_len;
    unsigned char *xcghash; /* H */
    size_t xcghash_len;
    char type; /* X */
    unsigned char *session_id;
    size_t session_id_len;
    OSSL_FIPS_IND_DECLARE
} KDF_SSHKDF;

static void *kdf_sshkdf_new(void *provctx)
{
    KDF_SSHKDF *ctx;

    if (!ossl_prov_is_running())
        return NULL;

    if ((ctx = OPENSSL_zalloc(sizeof(*ctx))) != NULL) {
        ctx->provctx = provctx;
        OSSL_FIPS_IND_INIT(ctx)
    }
    return ctx;
}

static void kdf_sshkdf_free(void *vctx)
{
    KDF_SSHKDF *ctx = (KDF_SSHKDF *)vctx;

    if (ctx != NULL) {
        kdf_sshkdf_reset(ctx);
        OPENSSL_free(ctx);
    }
}

static void kdf_sshkdf_reset(void *vctx)
{
    KDF_SSHKDF *ctx = (KDF_SSHKDF *)vctx;
    void *provctx = ctx->provctx;

    ossl_prov_digest_reset(&ctx->digest);
    OPENSSL_clear_free(ctx->key, ctx->key_len);
    OPENSSL_clear_free(ctx->xcghash, ctx->xcghash_len);
    OPENSSL_clear_free(ctx->session_id, ctx->session_id_len);
    memset(ctx, 0, sizeof(*ctx));
    ctx->provctx = provctx;
}

static void *kdf_sshkdf_dup(void *vctx)
{
    const KDF_SSHKDF *src = (const KDF_SSHKDF *)vctx;
    KDF_SSHKDF *dest;

    dest = kdf_sshkdf_new(src->provctx);
    if (dest != NULL) {
        if (!ossl_prov_memdup(src->key, src->key_len,
                              &dest->key, &dest->key_len)
                || !ossl_prov_memdup(src->xcghash, src->xcghash_len,
                                     &dest->xcghash , &dest->xcghash_len)
                || !ossl_prov_memdup(src->session_id, src->session_id_len,
                                     &dest->session_id , &dest->session_id_len)
                || !ossl_prov_digest_copy(&dest->digest, &src->digest))
            goto err;
        dest->type = src->type;
        OSSL_FIPS_IND_COPY(dest, src)
    }
    return dest;

 err:
    kdf_sshkdf_free(dest);
    return NULL;
}

static int sshkdf_set_membuf(unsigned char **dst, size_t *dst_len,
                             const OSSL_PARAM *p)
{
    OPENSSL_clear_free(*dst, *dst_len);
    *dst = NULL;
    *dst_len = 0;
    return OSSL_PARAM_get_octet_string(p, (void **)dst, 0, dst_len);
}

#ifdef FIPS_MODULE
static int fips_digest_check_passed(KDF_SSHKDF *ctx, const EVP_MD *md)
{
    OSSL_LIB_CTX *libctx = PROV_LIBCTX_OF(ctx->provctx);
    /*
     * Perform digest check
     *
     * According to NIST SP 800-135r1 section 5.2, the valid hash functions are
     * specified in FIPS 180-3. ACVP also only lists the same set of hash
     * functions.
     */
    int digest_unapproved = !EVP_MD_is_a(md, SN_sha1)
        && !EVP_MD_is_a(md, SN_sha224)
        && !EVP_MD_is_a(md, SN_sha256)
        && !EVP_MD_is_a(md, SN_sha384)
        && !EVP_MD_is_a(md, SN_sha512);

    if (digest_unapproved) {
        if (!OSSL_FIPS_IND_ON_UNAPPROVED(ctx, OSSL_FIPS_IND_SETTABLE0,
                                         libctx, "SSHKDF", "Digest",
                                         ossl_fips_config_sshkdf_digest_check)) {
            ERR_raise(ERR_LIB_PROV, PROV_R_DIGEST_NOT_ALLOWED);
            return 0;
        }
    }
    return 1;
}

static int fips_key_check_passed(KDF_SSHKDF *ctx)
{
    OSSL_LIB_CTX *libctx = PROV_LIBCTX_OF(ctx->provctx);
    int key_approved = ossl_kdf_check_key_size(ctx->key_len);

    if (!key_approved) {
        if (!OSSL_FIPS_IND_ON_UNAPPROVED(ctx, OSSL_FIPS_IND_SETTABLE1,
                                         libctx, "SSHKDF", "Key size",
                                         ossl_fips_config_sshkdf_key_check)) {
            ERR_raise(ERR_LIB_PROV, PROV_R_INVALID_KEY_LENGTH);
            return 0;
        }
    }
    return 1;
}
#endif

static int kdf_sshkdf_derive(void *vctx, unsigned char *key, size_t keylen,
                             const OSSL_PARAM params[])
{
    KDF_SSHKDF *ctx = (KDF_SSHKDF *)vctx;
    const EVP_MD *md;

    if (!ossl_prov_is_running() || !kdf_sshkdf_set_ctx_params(ctx, params))
        return 0;

    md = ossl_prov_digest_md(&ctx->digest);
    if (md == NULL) {
        ERR_raise(ERR_LIB_PROV, PROV_R_MISSING_MESSAGE_DIGEST);
        return 0;
    }
    if (ctx->key == NULL) {
        ERR_raise(ERR_LIB_PROV, PROV_R_MISSING_KEY);
        return 0;
    }
    if (ctx->xcghash == NULL) {
        ERR_raise(ERR_LIB_PROV, PROV_R_MISSING_XCGHASH);
        return 0;
    }
    if (ctx->session_id == NULL) {
        ERR_raise(ERR_LIB_PROV, PROV_R_MISSING_SESSION_ID);
        return 0;
    }
    if (ctx->type == 0) {
        ERR_raise(ERR_LIB_PROV, PROV_R_MISSING_TYPE);
        return 0;
    }

    return SSHKDF(md, ctx->key, ctx->key_len,
                  ctx->xcghash, ctx->xcghash_len,
                  ctx->session_id, ctx->session_id_len,
                  ctx->type, key, keylen);
}

/* Machine generated by util/perl/OpenSSL/paramnames.pm */
#ifndef sshkdf_set_ctx_params_list
static const OSSL_PARAM sshkdf_set_ctx_params_list[] = {
    OSSL_PARAM_utf8_string(OSSL_KDF_PARAM_PROPERTIES, NULL, 0),
    OSSL_PARAM_utf8_string(OSSL_KDF_PARAM_DIGEST, NULL, 0),
    OSSL_PARAM_octet_string(OSSL_KDF_PARAM_KEY, NULL, 0),
    OSSL_PARAM_octet_string(OSSL_KDF_PARAM_SSHKDF_XCGHASH, NULL, 0),
    OSSL_PARAM_octet_string(OSSL_KDF_PARAM_SSHKDF_SESSION_ID, NULL, 0),
    OSSL_PARAM_utf8_string(OSSL_KDF_PARAM_SSHKDF_TYPE, NULL, 0),
# if defined(FIPS_MODULE)
    OSSL_PARAM_int(OSSL_KDF_PARAM_FIPS_DIGEST_CHECK, NULL),
# endif
# if defined(FIPS_MODULE)
    OSSL_PARAM_int(OSSL_KDF_PARAM_FIPS_KEY_CHECK, NULL),
# endif
    OSSL_PARAM_END
};
#endif

#ifndef sshkdf_set_ctx_params_st
struct sshkdf_set_ctx_params_st {
    OSSL_PARAM *digest;
    OSSL_PARAM *engine;
# if defined(FIPS_MODULE)
    OSSL_PARAM *ind_d;
# endif
# if defined(FIPS_MODULE)
    OSSL_PARAM *ind_k;
# endif
    OSSL_PARAM *key;
    OSSL_PARAM *propq;
    OSSL_PARAM *sid;
    OSSL_PARAM *type;
    OSSL_PARAM *xcg;
};
#endif

#ifndef sshkdf_set_ctx_params_decoder
static int sshkdf_set_ctx_params_decoder
    (const OSSL_PARAM *p, struct sshkdf_set_ctx_params_st *r)
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
            case 'k':
                switch(s[1]) {
                default:
                    break;
                case 'e':
                    switch(s[2]) {
                    default:
                        break;
                    case 'y':
                        switch(s[3]) {
                        default:
                            break;
                        case '-':
# if defined(FIPS_MODULE)
                            if (ossl_likely(strcmp("check", s + 4) == 0)) {
                                /* OSSL_KDF_PARAM_FIPS_KEY_CHECK */
                                if (ossl_unlikely(r->ind_k != NULL)) {
                                    ERR_raise_data(ERR_LIB_PROV, PROV_R_REPEATED_PARAMETER,
                                                   "param %s is repeated", s);
                                    return 0;
                                }
                                r->ind_k = (OSSL_PARAM *)p;
                            }
# endif
                            break;
                        case '\0':
                            if (ossl_unlikely(r->key != NULL)) {
                                ERR_raise_data(ERR_LIB_PROV, PROV_R_REPEATED_PARAMETER,
                                               "param %s is repeated", s);
                                return 0;
                            }
                            r->key = (OSSL_PARAM *)p;
                        }
                    }
                }
                break;
            case 'p':
                if (ossl_likely(strcmp("roperties", s + 1) == 0)) {
                    /* OSSL_KDF_PARAM_PROPERTIES */
                    if (ossl_unlikely(r->propq != NULL)) {
                        ERR_raise_data(ERR_LIB_PROV, PROV_R_REPEATED_PARAMETER,
                                       "param %s is repeated", s);
                        return 0;
                    }
                    r->propq = (OSSL_PARAM *)p;
                }
                break;
            case 's':
                if (ossl_likely(strcmp("ession_id", s + 1) == 0)) {
                    /* OSSL_KDF_PARAM_SSHKDF_SESSION_ID */
                    if (ossl_unlikely(r->sid != NULL)) {
                        ERR_raise_data(ERR_LIB_PROV, PROV_R_REPEATED_PARAMETER,
                                       "param %s is repeated", s);
                        return 0;
                    }
                    r->sid = (OSSL_PARAM *)p;
                }
                break;
            case 't':
                if (ossl_likely(strcmp("ype", s + 1) == 0)) {
                    /* OSSL_KDF_PARAM_SSHKDF_TYPE */
                    if (ossl_unlikely(r->type != NULL)) {
                        ERR_raise_data(ERR_LIB_PROV, PROV_R_REPEATED_PARAMETER,
                                       "param %s is repeated", s);
                        return 0;
                    }
                    r->type = (OSSL_PARAM *)p;
                }
                break;
            case 'x':
                if (ossl_likely(strcmp("cghash", s + 1) == 0)) {
                    /* OSSL_KDF_PARAM_SSHKDF_XCGHASH */
                    if (ossl_unlikely(r->xcg != NULL)) {
                        ERR_raise_data(ERR_LIB_PROV, PROV_R_REPEATED_PARAMETER,
                                       "param %s is repeated", s);
                        return 0;
                    }
                    r->xcg = (OSSL_PARAM *)p;
                }
            }
    return 1;
}
#endif
/* End of machine generated */

static int kdf_sshkdf_set_ctx_params(void *vctx, const OSSL_PARAM params[])
{
    struct sshkdf_set_ctx_params_st p;
    KDF_SSHKDF *ctx = vctx;
    OSSL_LIB_CTX *provctx;

    if (ctx == NULL || !sshkdf_set_ctx_params_decoder(params, &p))
        return 0;

    provctx = PROV_LIBCTX_OF(ctx->provctx);

    if (!OSSL_FIPS_IND_SET_CTX_FROM_PARAM(ctx, OSSL_FIPS_IND_SETTABLE0, p.ind_d))
        return 0;
    if (!OSSL_FIPS_IND_SET_CTX_FROM_PARAM(ctx, OSSL_FIPS_IND_SETTABLE1, p.ind_k))
        return 0;

    if (p.digest != NULL) {
        const EVP_MD *md = NULL;

        if (!ossl_prov_digest_load(&ctx->digest, p.digest,
                                   p.propq, p.engine, provctx))
            return 0;

        md = ossl_prov_digest_md(&ctx->digest);
        if (EVP_MD_xof(md)) {
            ERR_raise(ERR_LIB_PROV, PROV_R_XOF_DIGESTS_NOT_ALLOWED);
            return 0;
        }

#ifdef FIPS_MODULE
        if (!fips_digest_check_passed(ctx, md))
            return 0;
#endif
    }

    if (p.key != NULL) {
        if (!sshkdf_set_membuf(&ctx->key, &ctx->key_len, p.key))
            return 0;

#ifdef FIPS_MODULE
        if (!fips_key_check_passed(ctx))
            return 0;
#endif
    }

    if (p.xcg != NULL
            && !sshkdf_set_membuf(&ctx->xcghash, &ctx->xcghash_len, p.xcg))
        return 0;

    if (p.sid != NULL
            && !sshkdf_set_membuf(&ctx->session_id, &ctx->session_id_len, p.sid))
        return 0;

    if (p.type != NULL) {
        const char *kdftype;

        if (!OSSL_PARAM_get_utf8_string_ptr(p.type, &kdftype))
            return 0;
        /* Expect one character (byte in this case) */
        if (kdftype == NULL || p.type->data_size != 1)
            return 0;
        if (kdftype[0] < 65 || kdftype[0] > 70) {
            ERR_raise(ERR_LIB_PROV, PROV_R_VALUE_ERROR);
            return 0;
        }
        ctx->type = kdftype[0];
    }
    return 1;
}

static const OSSL_PARAM *kdf_sshkdf_settable_ctx_params(ossl_unused void *ctx,
                                                        ossl_unused void *p_ctx)
{
    return sshkdf_set_ctx_params_list;
}

/* Machine generated by util/perl/OpenSSL/paramnames.pm */
#ifndef sshkdf_get_ctx_params_list
static const OSSL_PARAM sshkdf_get_ctx_params_list[] = {
    OSSL_PARAM_size_t(OSSL_KDF_PARAM_SIZE, NULL),
# if defined(FIPS_MODULE)
    OSSL_PARAM_int(OSSL_KDF_PARAM_FIPS_APPROVED_INDICATOR, NULL),
# endif
    OSSL_PARAM_END
};
#endif

#ifndef sshkdf_get_ctx_params_st
struct sshkdf_get_ctx_params_st {
# if defined(FIPS_MODULE)
    OSSL_PARAM *ind;
# endif
    OSSL_PARAM *size;
};
#endif

#ifndef sshkdf_get_ctx_params_decoder
static int sshkdf_get_ctx_params_decoder
    (const OSSL_PARAM *p, struct sshkdf_get_ctx_params_st *r)
{
    const char *s;

    memset(r, 0, sizeof(*r));
    if (p != NULL)
        for (; (s = p->key) != NULL; p++)
            switch(s[0]) {
            default:
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
            case 's':
                if (ossl_likely(strcmp("ize", s + 1) == 0)) {
                    /* OSSL_KDF_PARAM_SIZE */
                    if (ossl_unlikely(r->size != NULL)) {
                        ERR_raise_data(ERR_LIB_PROV, PROV_R_REPEATED_PARAMETER,
                                       "param %s is repeated", s);
                        return 0;
                    }
                    r->size = (OSSL_PARAM *)p;
                }
            }
    return 1;
}
#endif
/* End of machine generated */

static int kdf_sshkdf_get_ctx_params(void *vctx, OSSL_PARAM params[])
{
    KDF_SSHKDF *ctx = (KDF_SSHKDF *)vctx;
    struct sshkdf_get_ctx_params_st p;

    if (ctx == NULL || !sshkdf_get_ctx_params_decoder(params, &p))
        return 0;

    if (p.size != NULL && !OSSL_PARAM_set_size_t(p.size, SIZE_MAX))
        return 0;

    if (!OSSL_FIPS_IND_GET_CTX_FROM_PARAM(ctx, p.ind))
        return 0;
    return 1;
}

static const OSSL_PARAM *kdf_sshkdf_gettable_ctx_params(ossl_unused void *ctx,
                                                        ossl_unused void *p_ctx)
{
    return sshkdf_get_ctx_params_list;
}

const OSSL_DISPATCH ossl_kdf_sshkdf_functions[] = {
    { OSSL_FUNC_KDF_NEWCTX, (void(*)(void))kdf_sshkdf_new },
    { OSSL_FUNC_KDF_DUPCTX, (void(*)(void))kdf_sshkdf_dup },
    { OSSL_FUNC_KDF_FREECTX, (void(*)(void))kdf_sshkdf_free },
    { OSSL_FUNC_KDF_RESET, (void(*)(void))kdf_sshkdf_reset },
    { OSSL_FUNC_KDF_DERIVE, (void(*)(void))kdf_sshkdf_derive },
    { OSSL_FUNC_KDF_SETTABLE_CTX_PARAMS,
      (void(*)(void))kdf_sshkdf_settable_ctx_params },
    { OSSL_FUNC_KDF_SET_CTX_PARAMS, (void(*)(void))kdf_sshkdf_set_ctx_params },
    { OSSL_FUNC_KDF_GETTABLE_CTX_PARAMS,
      (void(*)(void))kdf_sshkdf_gettable_ctx_params },
    { OSSL_FUNC_KDF_GET_CTX_PARAMS, (void(*)(void))kdf_sshkdf_get_ctx_params },
    OSSL_DISPATCH_END
};

static int SSHKDF(const EVP_MD *evp_md,
                  const unsigned char *key, size_t key_len,
                  const unsigned char *xcghash, size_t xcghash_len,
                  const unsigned char *session_id, size_t session_id_len,
                  char type, unsigned char *okey, size_t okey_len)
{
    EVP_MD_CTX *md = NULL;
    unsigned char digest[EVP_MAX_MD_SIZE];
    unsigned int dsize = 0;
    size_t cursize = 0;
    int ret = 0;

    md = EVP_MD_CTX_new();
    if (md == NULL)
        return 0;

    if (!EVP_DigestInit_ex(md, evp_md, NULL))
        goto out;

    if (!EVP_DigestUpdate(md, key, key_len))
        goto out;

    if (!EVP_DigestUpdate(md, xcghash, xcghash_len))
        goto out;

    if (!EVP_DigestUpdate(md, &type, 1))
        goto out;

    if (!EVP_DigestUpdate(md, session_id, session_id_len))
        goto out;

    if (!EVP_DigestFinal_ex(md, digest, &dsize))
        goto out;

    if (okey_len < dsize) {
        memcpy(okey, digest, okey_len);
        ret = 1;
        goto out;
    }

    memcpy(okey, digest, dsize);

    for (cursize = dsize; cursize < okey_len; cursize += dsize) {

        if (!EVP_DigestInit_ex(md, evp_md, NULL))
            goto out;

        if (!EVP_DigestUpdate(md, key, key_len))
            goto out;

        if (!EVP_DigestUpdate(md, xcghash, xcghash_len))
            goto out;

        if (!EVP_DigestUpdate(md, okey, cursize))
            goto out;

        if (!EVP_DigestFinal_ex(md, digest, &dsize))
            goto out;

        if (okey_len < cursize + dsize) {
            memcpy(okey + cursize, digest, okey_len - cursize);
            ret = 1;
            goto out;
        }

        memcpy(okey + cursize, digest, dsize);
    }

    ret = 1;

out:
    EVP_MD_CTX_free(md);
    OPENSSL_cleanse(digest, EVP_MAX_MD_SIZE);
    return ret;
}
