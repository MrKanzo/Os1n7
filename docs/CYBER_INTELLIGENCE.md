# OS1N7 Cyber Intelligence — sources, refresh, and limitations

## Features
The cloud library opens **Cyber Intelligence** through "Кіберновини" and "Статистика". The Android client downloads only `content/cyber-intel.json` from this repository, validates source links, and saves the last good snapshot in private app preferences so it remains usable offline. Neither signing secrets nor user information is sent to the news sources.

## Official news sources
- Ukraine CERT-UA (original official RSS): https://cert.gov.ua/api/articles/rss
- CISA Cybersecurity Advisories (official RSS): https://www.cisa.gov/cybersecurity-advisories/all.xml
- UK National Cyber Security Centre (official RSS): https://www.ncsc.gov.uk/api/1/services/v1/report-rss-feed.xml

Each item retains its original title, published date, source, short feed description and link. In-feed links are accepted only for the expected official HTTPS domain. The app does not invent article summaries or translate official article text. Failed feeds retain their last good items, which must continue to show their original dates.

## Ukrainian localization (no APK update for new news)
The published JSON uses Ukrainian `title` and `summary` fields so existing OS1N7 app versions render Ukrainian news automatically. `originalTitle` and `originalSummary` retain the exact RSS metadata for auditability; original HTTPS source URLs and dates do not change.

- CERT-UA advisories already published in Ukrainian remain in the source language.
- English CISA and NCSC UK headlines/short descriptions are localized to Ukrainian with an offline Argos Translate en→uk model on GitHub Actions; the first snapshot was also editorially translated.
- No user account or paid translator API is needed, and no article text is sent to an online translation provider.
- Previously translated items are reused, instead of repeatedly rephrasing the same official headline every 6 hours.
- If automatic translation cannot run, the workflow fails before publishing new English-language headlines; the last approved Ukrainian snapshot remains available.
- **Machine translations can contain errors.** Treat them as short informational summaries and follow the official link for legally/technically significant wording, CVE IDs, patch instructions, and product versions.

## Yearly statistics — read these labels carefully
- **CISA KEV:** Number of vulnerabilities **added to the Known Exploited Vulnerabilities catalog in each calendar year** (`dateAdded`), computed from CISA's official GitHub mirror: https://github.com/cisagov/kev-data and https://www.cisa.gov/known-exploited-vulnerabilities-catalog . An exploited vulnerability is not a confirmed count of compromises or breached systems.
- **FBI IC3:** Number of **complaints alleging internet crime received by FBI IC3**, not confirmed hacks. Source: FBI Internet Crime Reports: https://www.ic3.gov/annualreport/reports . Verified historic snapshot (2020–2025) is maintained in `scripts/sync_cyber_intel.py`. Year 2025: 1,008,597 complaints (FBI press release April 6, 2026). 2024: 859,532; 2023: 880,418; 2022: 800,944; 2021: 847,376; 2020: 791,790. This series is updated upon a new, independently verified FBI report; it is not automatically scraped.

**Do not label either series "the number of hacks worldwide."** They have different methodologies, scope, and geography and must not be combined in one total.

## Updating
The `.github/workflows/cyber-intel.yml` workflow fetches source data every 6 hours and on manual dispatch; initial setup or script changes also trigger a run. It runs offline parser tests, uses Python standard library, and commits only if the snapshot changes. The Android APK workflow ignores changes under `content/**`. If a source is unreachable, the preceding official items/data remain.

## Manual QA checklist
- [ ] Inside Cloud Library, open both news and statistics and return to Cloud Library.
- [ ] With a network connection, load recent official news and open one original source.
- [ ] Change between KEV and FBI annual series and select an individual year, then "all years".
- [ ] Turn off Wi-Fi and mobile data; re-open the screen and confirm offline snapshot remains.
- [ ] Check the CISA KEV metric explicitly reads "added to KEV", not "number of hacks".
- [ ] Check the FBI IC3 metric explicitly reads "complaints", not "confirmed breaches".
- [ ] Check the Github workflow reports at least one available source; if all inaccessible, investigate access separately.
- [ ] Confirm signed Android APK installs over prior signed release without wiping preferences.
