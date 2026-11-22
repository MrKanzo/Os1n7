# OS1N7: public feedback and support (first release)

**What users can do.** On the main screen select **Відгуки та підтримка**. Browse public feedback in the GitHub repository, filter by **Усі / Помилка / Ідея / Покращення**, toggle resolved issues on/off, refresh the list, and open any issue for existing comments and maintainer replies. Reports are retrieved from GitHub's public Issues REST endpoint, not a secret OS1N7 backend. The app caches only issue titles, issue IDs, status, date, and number of comments for offline display.

**Submitting a short report.** Tap **✎ Нове звернення**, choose the category, enter a title (5–85 chars) and optional short description (max 450 chars). The app composes a GitHub Issues creation URL with a Ukrainian title prefix `[Помилка]`, `[Ідея]`, or `[Покращення]`, and opens the system browser. **The report is not submitted from inside the app**; the user must sign in to GitHub and confirm creation. No GitHub token is bundled in the APK and the app never tries to post as the user.

**Privacy and moderation.** Reports and replies are publicly visible. Tell users not to include passwords, private phone numbers, addresses, tokens, screenshots with sensitive data or personal identifying details. Moderators of `MrKanzo/Os1n7` can reply, close issues, report abuse, and restrict conversations using standard GitHub tools. Reports are not screened for private information by OS1N7, and users are responsible for the content they decide to publish. Only issue **titles** are shown inside the Android support feed, not arbitrary bodies or third-party comments.

**Filtering convention.** Only public issues with one of the three exact category title prefixes are displayed. Pull requests are excluded. The feed reads up to the 100 latest issues, retains at most 80 matching items, and can therefore omit older items if a repository eventually becomes busy. Category and state filters operate locally. GitHub API availability and unauthenticated rate limits apply; when disconnected, the last local snapshot is shown.

**Configuration.** Repository **Issues must remain enabled**. GitHub may require login before issue creation. If it is disabled, enable it at repository Settings → General → Features → Issues. No server deployment, authentication secret, or extra Android permission is required.

## QA checklist
- [ ] Support screen opens and Android back returns to home.
- [ ] Category chips filter only issues starting with their matching prefixes.
- [ ] Resolved toggle hides closed issues and restores them when checked.
- [ ] New report rejects a too-short title and limits descriptions to 450 chars.
- [ ] Submit button opens a *prefilled* GitHub issue form; browser prompts sign-in if necessary.
- [ ] Issue submission does not happen until user confirms on GitHub.
- [ ] Published issue appears after Refresh (subject to GitHub API caching and rate limits).
- [ ] Opening a report opens only a numeric issue URL in this repository.
- [ ] Offline reopen preserves the last downloaded public issue-title list.
- [ ] Closing an issue on GitHub reflects on Refresh (API permitting).
- [ ] No GitHub write token, user email or personal information is stored in the APK.
