# KIM-244: Privacy policy for App Store / Play Store submission

**Status:** Done · **Priority:** Urgent · **Labels:** _none_
**Created:** 2026-05-31T17:09:42.110Z · **Completed:** 2026-06-04T07:18:21.013Z
**Linear URL:** https://linear.app/kimmo-m/issue/KIM-244/privacy-policy-for-app-store-play-store-submission

## Description

Both stores require a privacy policy URL before submission. Create a minimal hosted privacy policy covering data collected (none stored server-side in BYOK mode), API key handling (stored locally on device), and contact info.

## Completed

Privacy policy published at [**https://goaheadand.dev/rewinds/privacy**](<https://goaheadand.dev/rewinds/privacy>)

Covers:

* No server-side data collection (BYOK mode)
* API key stored locally on device (iOS Keychain / Android secure storage)
* Anthropic API as the only external service (user-initiated, user's own key)
* Contact: hi@goaheadand.dev

Hosted on Cloudflare Pages (`rewinds` project), served from the `goaheadand.dev` custom domain.

## Comments

### kimmo.myllyviita@gmail.com — 2026-06-04T07:16:19.570Z

Put back to in-progress cause not published yet.

URL should be

[https://goaheadand.dev/rewinds/privacy](<https://goaheadand.dev/privacy>)

