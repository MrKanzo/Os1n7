# Os1n7 — Android architecture v1

## Product decision (2026-11-21)
One unified category contains four tabs: Theory, Books, Practice, Tests. No separate Academy/Library navigation.

## Navigation
Home -> CategoryList -> CategoryDetail(categoryId) -> ContentReader(contentId)
CategoryDetail has tabs: Theory | Books | Practice | Tests.
ContentReader renders structured blocks; exercises and quizzes use dedicated block renderers.
Bottom navigation: Home | Search | Saved | Progress.

## Android stack
Kotlin, Jetpack Compose, Material 3, Navigation Compose, ViewModel + StateFlow, Retrofit/OkHttp, Room for offline cache/progress, DataStore for settings. Dependencies should be selected to match the existing Gradle setup when implemented.

## Models
Category(id, slug, title, description, icon, order)
ContentItem(id, categoryId, type, title, summary, difficulty, estimatedMinutes, language, version, blocks)
ContentType: THEORY, BOOK, PRACTICE, QUIZ
ContentBlock: Heading, Paragraph, Image, Link, Code, Checklist, Question
Progress(contentId, status, completedAt, quizScore)
Bookmark(contentId, createdAt)

Use stable IDs and version numbers. Book entries should store metadata and links to legitimately accessible material, not unauthorized copies.

## API contract (proposal)
GET /api/v1/categories
GET /api/v1/categories/{id}/content?type=theory|book|practice|quiz
GET /api/v1/content/{id}
GET /api/v1/health
Responses JSON UTF-8, versioned. Android should support cached content when offline. The Termux prototype is not assumed to implement these endpoints yet.

## Suggested first category
digital-footprint:
- Theory: What is a digital footprint?
- Book: Publicly accessible recommended reading and legal source links
- Practice: Audit your own publicly visible accounts
- Quiz: 5 questions about privacy and source verification

## Build phases
1. Inspect existing repo and preserve all files.
2. Implement models and local seed dataset; render Compose category list/detail/reader.
3. Add exercise checklists, quiz scoring and local progress.
4. Implement API client and compatible Termux endpoints.
5. Offline caching, search, bookmarks, accessibility and tests.
6. GitHub Actions APK build verification.

## Safety and privacy
Use synthetic examples or accounts/assets owned by the learner or used with permission. No harvesting private credentials, covert surveillance, or publication of sensitive personal information. Store progress locally by default; do not send personal identifiers to the prototype server.

## Acceptance criteria for first APK
- Open category from home.
- See four tabs and filter content correctly.
- Open theory item and read text.
- Complete checklist and take quiz.
- Progress survives app restart.
- App works with seed content when server unavailable.
