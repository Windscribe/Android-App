# Drifted translations

Keys whose English text was reworded after the translations landed, so the
translations still describe the previous wording. Regenerate with:

    python3 tools/translate/find_drifted_translations.py --markdown drifted_translations.md

A change that is only a typo or capitalisation fix leaves its translations
correct; check the two wordings below before sending a key out for retranslation.

### complete_puzzle_to_continue
- affected: 23/23 locales — ar, bn, de, el, es, fa, fr, hi, in, it, ja, ko, pl, pt-rBR, pt, ru, sk, tr, uk, ur, vi, zh-rTW, zh
- English was: 'Complete CAPCHA"'
- English now: 'Complete CAPTCHA'

### login
- affected: 22/23 locales — ar, bn, de, el, es, fa, fr, hi, in, it, ja, ko, pl, pt-rBR, pt, ru, sk, tr, uk, vi, zh-rTW, zh
- English was: 'Login'
- English now: 'Log In'

### voucher_code_used_already
- affected: 21/23 locales — ar, bn, de, el, es, fa, hi, in, it, ja, pl, pt-rBR, pt, ru, sk, tr, uk, ur, vi, zh-rTW, zh
- English was: 'A voucher is already applied to this account or this voucher code is already used.'
- English now: 'A voucher is already applied to this account or the code is already used.'

### advance
- affected: 18/23 locales — ar, bn, de, es, fa, fr, hi, in, it, ja, pl, pt, ru, tr, uk, vi, zh-rTW, zh
- English was: 'Advance Parameters'
- English now: 'Advanced Parameters'

### text_sign_up
- affected: 18/23 locales — ar, bn, de, es, fa, fr, hi, in, it, ja, pl, pt, ru, tr, uk, vi, zh-rTW, zh
- English was: 'Sign up'
- English now: 'Sign Up'

### set_up_later
- affected: 5/23 locales — fr, hi, ko, vi, zh
- English was: 'Setup later'
- English now: 'Setup Later'

### auto_start_on_boot
- affected: 5/23 locales — fr, ja, ko, uk, vi
- English was: 'Auto-connect on Boot'
- English now: 'Auto-connect On Boot'

### keep_alive
- affected: 2/23 locales — ja, ko
- English was: 'keep alive'
- English now: 'Keep Alive'

### confirm_your_email
- affected: 1/23 locales — pl
- English was: 'Confirm your email'
- English now: 'Please confirm your email'

### you_ve_violated_our_terms
- affected: 1/23 locales — ru
- English was: 'Your account has been \\ndisabled for violating \\nour terms'
- English now: 'Your account has been disabled for violating our terms'

### enter_valid_ip_or_url
- affected: 1/23 locales — ru
- English was: 'Enter valid IP or URL'
- English now: 'Enter valid IP or Url'
