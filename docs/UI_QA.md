# OS1N7 — UI consistency and navigation QA

## Design system (v0.6.7)
- Shared `Os1n7Ui.kt` controls the dark palette, 48dp minimum button height, icon chips, and consistent page headings.
- Navigation: home is the destination for top-level back buttons. Internal sections must return to their parent list, not jump straight to home.
- Icons represent meaning: library ▣, catalog ▦, tools ⚒, lab ⌘, graph ◎, cases ▤, security ⬡, academy ✦, cloud ☁.
- Category and tool glyphs are derived from stable names, not random list positions.

## Manual checks on a device
- [ ] Home: all ten sections open; title and subtitle fit narrow screens; no clipping after system bars.
- [ ] Category Library: back to home, category list, theory, books, practice, tests; Android back unwinds the same hierarchy; progress persists.
- [ ] Tool Catalog: filter tools, open SpiderFoot/Maltego/Shodan, back to filtered catalog (follow-up improvement).
- [ ] Offline Tools: DNS, URL, EXIF simulated exercises and return to home.
- [ ] Academy and OSINT from Zero: complete lessons and navigate back without losing progress.
- [ ] Lab and Case Files: buttons, evidence, simulations and completed progress.
- [ ] OSINT Graph: open a case, select nodes and return to the case list.
- [ ] Security Center: DNS error reporting, privacy checklist, and back navigation.
- [ ] Cloud Library: GitHub ⇄ Termux toggle, search, offline copy, synchronization state.
- [ ] Android light/dark system theme: text must remain readable in the app's dark theme.
- [ ] Profile: tap the upper-left avatar; the upper-right cloud shortcut remains unchanged.
- [ ] Edit nickname (1–24 characters) and choose a built-in avatar; both persist after reopening.
- [ ] Profile progress uses existing lesson, academy, lab, case, and foundation keys without resetting data.
- [ ] Bookmark a lesson, return to profile, open it from Favorites, then unbookmark it.
- [ ] Achievements unlock only at their stated thresholds.
- [ ] Profile is local-only: no login, user upload, or new permission.
- [ ] APK update: signed release installs over the prior signed release without clearing local progress.
- [ ] Rotate or background/restore where supported; avoid duplicate update prompts.

## Release verification
After GitHub Actions succeeds, install signed APK on a test phone and check the whole checklist. Source edits alone do not establish a successful Android build.
