# OS1N7 • Кіберправо України

## About this module

An offline educational reference for OSINT researchers and Android users. The **Кіберправо України** entry on the home screen presents concise Ukrainian summaries of rules, offenses, potential sanctions, and practical examples; search covers the article identifier, summary, use-case and penalties. Users may filter **Усе / Правила OSINT / Кримінальний кодекс / Інші закони**. Open a card for details and follow **Читати закон** to the official Ukrainian parliamentary legislation portal.

This is NOT legal advice and does not decide whether a particular tool, action or investigation is legal. Fact-specific authorization, the law in force at the time of the action, the scope of the act and a court's findings determine liability.

## Reviewed sources (22 November 2026)

Primary links for reading the currently published official text:
- Criminal Code of Ukraine, Law № 2341-III (including arts. 182, 361, 361-1, 361-2, 362, 363, 363-1): https://zakon.rada.gov.ua/laws/show/2341-14#Text
- Personal Data Protection Law № 2297-VI: https://zakon.rada.gov.ua/laws/show/2297-17#Text
- Access to Public Information Law № 2939-VI: https://zakon.rada.gov.ua/laws/show/2939-17#Text
- Main Cybersecurity Principles Law № 2163-VIII: https://zakon.rada.gov.ua/laws/show/2163-19#Text

The statutory sanctions in cards were cross-checked against accessible up-to-date reproductions including:
- Criminal Code consolidated as of 12 November 2026, art. 361: https://textbook.com.ua/pravo/1794448084/s-457
- Art. 361-1: https://textbook.com.ua/pravo/1794448084/s-458
- Art. 361-2: https://textbook.com.ua/pravo/1794448084/s-459
- Arts. 182, 362 and 363: https://legal.org.ua/zakon/kryminalnyi-kodeks

The live official website was intermittently timing out during research. Accordingly the module does **not** assert that a frozen paraphrase is an official up-to-the-minute legal text; users must consult the linked authoritative site before relying on it. Each card shows the review date.

## Essential caution for OSINT

- Being publicly accessible does not abolish personal-data and privacy protections.
- Scanning, exploitation and testing third-party systems without appropriate authorization and scope can be unlawful.
- Paragraph 6 of Criminal Code article 361 contains a **specific** carve-out for conduct carried out under the established vulnerability-search and discovery procedure. It is not a broad unrestricted legal license to perform penetration testing.
- The same software may be used for lawful defense or unlawful interference; legal conclusions depend on purpose, action, authority and consequences.
- A law's penalty includes distinct alternatives, offense grades and qualifying conditions. The summary does not predict an actual sentence; never misrepresent penalty units as exact hryvnia values without checking the applicable legal calculation.

## QA

- [ ] Home > Кіберправо України opens, stays within system-bar insets, and Android back works.
- [ ] Search by '361-1', 'OSINT', 'персональні' and 'покарання'; filtering is immediate.
- [ ] Category chips correctly show only matching topics.
- [ ] Opening a card shows summary, practical example, possible punishment, source and review date.
- [ ] Official source button opens HTTPS zakon.rada.gov.ua; invalid domains cannot be opened through these controls.
- [ ] Offline airplane mode still displays the 14 reference cards.
- [ ] All precise sanctions checked against updated official texts before future legal content releases.
- [ ] New Android build must pass the debug and signed release workflows before recommending install.
