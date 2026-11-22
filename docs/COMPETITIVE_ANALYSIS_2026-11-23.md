# OS1N7 — competitive landscape, UX audit and original product roadmap

**Research date:** 2026-11-23
**Repository:** https://github.com/MrKanzo/Os1n7
**Scope:** reference comparison of official product documentation plus review of actual OS1N7 Kotlin source. This is a selective competitive scan, **not** proof that any feature is globally unique.

## 1. Product direction

**OS1N7 = a calm, offline-first Ukrainian OSINT workshop: LEARN → CHECK → DOUBT → EXPLAIN.**

Not "a bigger pile of tools." The distinguishing design thesis is to guide users from raw observations through source verification and doubt to a defensible conclusion, with a small customizable companion that explains *what to do next* according to verified learning progress.

**Non-goals:** facial identification, automated profiling of private people, hidden scraping of accounts, unapproved network scans, unsupported probability scores such as "94% same person", AI that invents evidence, intrusive pop-ups, forced streaks/leaderboards, a permanently active animated 3D character.

## 2. Competitive reference scan

| Product | Verified strength | OS1N7 can learn from | Boundaries and original direction |
|---|---|---|---|
| OSINT Framework | Navigable category tree and references to specialized tools | Clear taxonomy, quick "where do I start?" routing | OS1N7 needs actionable local lessons and method, not another large link forest |
| Bellingcat Online Investigations Toolkit | Categorized tool reviews and practical investigative resources | Source descriptions, tool limits, independent evaluation | Add Ukrainian source context, age/freshness markers and an explanation of why a tool fits |
| SpiderFoot | Automated OSINT modules and collection | Lesson-to-tool routing; eventually opt-in interfaces to legitimate tools | Never present OS1N7's current directory as a working SpiderFoot integration |
| Maltego | Link analysis, graphing, investigation workflows and an AI assistant | Graph-linked evidence, cautious analysis | Our local guide should work offline and critique inferences without pretending to conduct investigations |
| Hunchly | Webpage capture, notes, SHA-256 hashes, evidence preservation | Source provenance, dates and integrity checks | A future OS1N7 chain-of-evidence must distinguish original capture from a user's typed claim; hashes validate integrity, not truth |
| TryHackMe / HTB Academy | Guided pathways, exercises, XP, achievements | Meaningful practice progression with low-pressure encouragement | No daily streak pressure or empty tap-farming |
| Finch | Customizable virtual pet motivated by completing activities | Warm small mascot; useful behavior changes after progress | Companion is a quiet guide, never a mandatory separate game |
| Obsidian Canvas | Local visual notes and connections | Compact graph with clickable claims and sources | An OSINT graph also needs source provenance, uncertainty and alternative explanations |

### Primary-source links

- [OSINT Framework](https://osintframework.com/)
- [Bellingcat toolkit](https://bellingcat.gitbook.io/toolkit)
- [SpiderFoot (maintainer source)](https://github.com/smicallef/spiderfoot)
- [Maltego products](https://support.maltego.com/en/support/solutions/articles/15000019166-what-is-maltego-)
- [Maltego AI assistant capabilities](https://support.maltego.com/en/support/solutions/articles/15000061166-capabilities)
- [Hunchly evidence and hashing](https://support.hunch.ly/article/52-2-content-photo-and-attachment-hashing)
- [TryHackMe streak mechanics](https://help.tryhackme.com/en/articles/6596150-how-do-streaks-work)
- [HTB Academy modules and paths](https://help.hackthebox.com/en/articles/12741910-academy-modules-paths)
- [HTB Academy XP and achievements](https://help.hackthebox.com/en/articles/13543761-weekly-streaks-xp-on-academy)
- [Finch companion](https://finchcare.com/about-finch)
- [Obsidian Canvas](https://obsidian.md/help/plugins/canvas)

This list demonstrates *what the references do*, not that they lack any unmentioned capability. Features may change. Avoid superiority or exclusivity claims without broader market verification.

## 3. Evidence-based OS1N7 snapshot

Current baseline inspected in the repository:
- `LessonLibrary.kt`: 18 bundled Ukrainian lessons, each with title, category, explanation, example and task.
- `CategoryLibraryActivity.kt`: category listing, search, theory/books/practice/tests, local favorites and progress.
- `OsintTreeActivity.kt` and `CyberTreeHeroView.kt`: 13 tree branches and light animated hero.
- `CaseFilesActivity.kt`: a fictional case involving repeated usernames, a photograph and an item marked R17.
- `GraphActivity.kt`: user-created local case graphs with nodes, notes, links, sources and copyable text reports.
- `ToolCatalogActivity.kt`: 12 educational cards; not live integrations with these third-party tools.
- `CompanionEngine.kt`, `CompanionOrbView.kt`, `CompanionActivity.kt`: local help, progress-based answer detail, compact static/live mascot with three color skins.
- `ServerLibraryActivity.kt` / `CyberIntelActivity.kt`: cloud/Termux content access and news/statistics separate from offline lessons.
- `MainActivity.kt`: two home modes and many navigation actions. User feedback explicitly reports that the interface can still feel crowded.

### Gaps actually observed in code

1. **Evidence quality:** graph has a free-text source field, but not structured source date, capture type, corroboration status, contrary hypothesis, or reproducible evidence export.
2. **Reasoning practice:** primary case checks an answer but did not yet explicitly test source independence and alternative explanations.
3. **Learning depth:** 18 short lessons and one fictional case are a solid MVP, not a broad curriculum. No verified skills prerequisite graph.
4. **Contextual help:** companion's current offline bot works through keywords and built-in lessons; it is not a language model or a complete case-level tutor.
5. **Navigation density:** classic home exposes many similar entry points; animated banner adds another route. Do not increase home-card count while reducing clutter.
6. **Data integrity:** privacy, versioned backups, evidence provenance and link/content freshness need dedicated QA before public launch.
7. **Performance verification:** use measured on-device startup, scrolling and battery load; code alone cannot guarantee a smooth experience.

## 4. Priority roadmap — originality vs. cost

| Order | Idea | User value | Difficulty | UI budget |
|---|---|---|---|---|
| **P0 / now** | **Дзеркало доказів (Evidence Mirror)** | Trains users to distinguish observed signs from unsupported identity conclusions, detect circular sources and name missing proof | Low | One *collapsed* section inside existing Case Files; no home button |
| **P0 / QA** | Reliable no-regression checks, stable signed APK, privacy clarity | Installs without data loss; trustworthy experience | Medium | No extra visible controls |
| **P1** | **Паспорт доказу**: observation, URL/source, visit date, claim, limitations, counterargument, optional local capture hash | Improves reproducibility; builds methodological habit | Medium | Appears only when adding evidence in Graph |
| **P1** | **Маршрут наступного кроку**: optional recommendation based on completed lessons and weakest skill | Reduces wandering; companion becomes useful, not decorative | Medium | One short line inside companion |
| **P1** | **Режим “Сумнівайся”**: counter-hypothesis exercise before finalizing a case | Teaches anti-bias reasoning | Medium | Optional card on the final case step |
| **P2** | **Сигнатура дослідника**: earned skill constellations instead of arbitrary badges | Visually original progression reflecting mastered capabilities | Medium | Compact profile visualization; not on every page |
| **P2** | Portable offline research dossier, provenance/export with redaction preview | Makes training skills usable responsibly outside OS1N7 | High | Context action in Graph |
| **P2** | Content freshness labels with last-reviewed date and broken-source checks | Credible tool directory; no stale links silently presented as current | Medium | Small metadata text only |
| **P3** | Opt-in source connectors and permitted self-audit tools | Real practical utility without covert collection | High | Separate advanced tools workspace |
| **P3** | More fictional cases and offline progression | Increased depth with no web services | Medium | Inside Academy / Case Files |

**Don't** start by creating a large animation engine, a marketplace of skins, 100 new home buttons, or a fake AI that claims external access. Preserve the current companion appearance until user approves a new visual design.

## 5. OS1N7 signature experience

### "Чотири лінзи" — always ask:
1. **Спостереження:** що ми реально побачили?
2. **Джерело:** де першоджерело і чи незалежне підтвердження?
3. **Альтернатива:** яке інше пояснення підходить до фактів?
4. **Межа висновку:** що можемо сказати, а що **не** можемо довести?

The companion can eventually label these four lenses for every teaching case and graph claim. No opaque percentage score. An incomplete dossier should be displayed as "потребує перевірки", not "95% достовірно".

### Visual grammar
- Normal home: no permanent animated motion; compact static 2D companion.
- Live home: same actions plus lightweight animation only while on screen.
- One source of truth for themes, icons, spacing, contrast and accessibility.
- Only one primary action per card; details behind drill-down.
- All new experiences nested in existing modules rather than new root tiles.
- Animation respects Android accessibility and is paused when not visible.
- Never trade off readable text/tap targets to make a list visually smaller.

## 6. What was actually built in this research iteration

**MVP — Evidence Mirror (Дзеркало доказів).**
- New pure Kotlin `EvidenceMirror.kt` models four reasoning traps in the existing fully fictional case.
- `CaseFilesActivity.kt` shows a collapsed "Дзеркало доказів" mini-exercise. The user may open it on demand, answer, and get a grounded explanation of the first mistake or missing question.
- On correct completion, `case001_mirror` is saved locally once; the existing dashboard and companion XP derive their progress from this saved completion.
- `CompanionEngine.kt` routes relevant questions about evidence and uncertainty to Case Files, without connecting to a third-party service.
- This is a **methodology trainer**, not live identity verification or evidence extraction. It does not conclude that any real person owns a profile.

## 7. Acceptance checklist

- [ ] Verify release APK compiles successfully on GitHub Actions
- [ ] Install on Android using same signed key and preserve existing data
- [ ] Open Case Files → collapse/expand Evidence Mirror → 4 questions
- [ ] Submit with missing answers → non-success result
- [ ] Submit one wrong answer → specific conceptual explanation
- [ ] Correct all four → mark reviewed, gain progress once
- [ ] Reopen app → review completion persisted
- [ ] XP/profile total/dashboard remain consistent
- [ ] Switch classic/live home; mascot remains unchanged by this iteration
- [ ] With network disabled, Mirror and companion both still work
- [ ] Check touch targets and readability on a narrow screen
- [ ] Confirm original Case Files and Graph navigation still work

## 8. Metrics to collect later (targets, not achieved results)

- First useful task reached in **≤ 2 taps** from the home screen.
- New practice section hidden until requested, reducing default visible clutter.
- Consistent completion counts across Profile, Home and Companion.
- No additional runtime permissions and no network requests in Evidence Mirror.
- Measure APK size, cold-start time, frame smoothness and battery usage on physical Android devices before claiming efficiency.

**Integrity note:** OS1N7 should not claim that a feature exists nowhere else. The defensible novelty is the combination of Ukrainian-language, phone-native, offline-first training, evidence self-critique, user-guided progress and a nonintrusive evolving companion.
