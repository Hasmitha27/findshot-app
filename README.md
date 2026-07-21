
# FindShot

**Search your photo gallery by what's written in it — entirely on your device.**

![Kotlin](https://img.shields.io/badge/Kotlin-2.0-7F52FF?logo=kotlin&logoColor=white)
![Jetpack Compose](https://img.shields.io/badge/UI-Jetpack%20Compose-4285F4?logo=jetpackcompose&logoColor=white)
![On-device ML](https://img.shields.io/badge/ML-On--device%20OCR-34A853)
![Status](https://img.shields.io/badge/status-active%20development-yellow)

FindShot reads the visible text in every screenshot and photo on your phone — using Google ML Kit's on-device text recognizer — so you can search your gallery the way you'd search your notes. No photo, and no extracted text, ever leaves the device.

<p align="center">
  <!-- Add a screenshot or screen-recording GIF here once you have one — this is the single highest-impact thing to add. -->
  <i>📸 Screenshot / demo GIF goes here</i>
</p>

---

## Contents

- [What it does](#what-it-does)
- [What's real vs. what's not (yet)](#whats-real-vs-whats-not-yet)
- [How search actually works](#how-search-actually-works)
- [Tech stack](#tech-stack)
- [Architecture](#architecture)
- [Setup](#setup)
- [Project structure](#project-structure)
- [Roadmap](#roadmap)
- [Engineering notes](#engineering-notes-worth-reading)

---

## What it does

- Reads every photo on your device via `MediaStore`
- Runs Google ML Kit's on-device OCR on each one, in the background, without blocking the UI
- Watches your gallery live (`ContentObserver`) — a new screenshot is searchable within seconds, no restart needed
- Lets you type a **full sentence**, not just a keyword — *"find that image which shows the wifi password"* gets parsed down to the words that matter and ranked by relevance
- Tap a result to view it full-screen; long-press to see exactly what text OCR extracted from it
- 100% on-device — no network calls for search, no photos or extracted text uploaded anywhere

<details>
<summary><b>See it in action</b> (click to expand)</summary>

1. Take a screenshot of anything with visible text
2. Open FindShot — it's indexed automatically within seconds
3. Type a few words from that text (even a rambling sentence works)
4. Tap the result to view full-screen, or long-press to inspect the raw OCR text

</details>

---

## What's real vs. what's not (yet)

Being precise about this matters more than it sounds — it's the difference between a credible engineering writeup and a feature list.

| Built and working | Not yet built |
|---|---|
| On-device OCR indexing (real ML Kit model) | Semantic/embedding search (so "hill station" would find a mountain photo with *no* visible text) |
| Live re-indexing via `ContentObserver` | Hybrid ranking (OCR + semantic combined) |
| Full-sentence query parsing (keyword extraction) | Date-range parsing ("last week", "yesterday") |
| Case-insensitive + basic plural matching | A measured evaluation harness (precision@k) |
| Full-screen image viewer + OCR inspector | MCP interface for agentic/external querying |

The current match is **substring text matching**, not a ranking model — results are ordered by how many extracted keywords appear in each image's OCR text, not by any learned relevance score. That's an honest, deliberate scope for this stage, not a placeholder.

---

## How search actually works



# Screenshot & Photo Search — Milestone 1

On-device search across your photo gallery using OCR (no cloud calls, no data leaves your device).

## What's implemented right now

- Reads all images on the device via `MediaStore`
- Runs on-device OCR (Google ML Kit Text Recognition) on each image in the background (`WorkManager`)
- Stores extracted text + metadata locally in a `Room` database
- Lets you search by keyword against that extracted text (e.g. "wifi" will find a screenshot with a visible wifi password)

This is genuinely working, on-device ML (ML Kit's text recognizer is a real trained model) — it's the **text-match half** of the final hybrid search design. Nothing here is a mock or a placeholder.

## What's NOT implemented yet (by design — this is Milestone 1)

- **Semantic/embedding search** — so a query like "hill station" won't yet find a mountain photo with no visible text. That needs a CLIP-style embedding model (TFLite/LiteRT), which is Milestone 2.
- **Hybrid ranking** (combining semantic + OCR score) — Milestone 2/3.
- **Date-range parsing** for queries like "last week" — Milestone 3.
- **MCP interface** so an external agent can call your search — Milestone 4.
- **ContentObserver** for auto-indexing new photos as they're taken (currently: indexing runs once on permission grant; re-run the button/relaunch to pick up new photos) — small addition, next step.
- **Evaluation harness** (precision@k measurement) — do this once search itself is stable.

## How to run it

1. Open this folder (`ScreenshotSearch/`) in **Android Studio** (Hedgehog or newer recommended).
2. Let Gradle sync — it will download dependencies (Compose, Room, WorkManager, ML Kit, Coil). You'll need internet access for this step.
3. Connect a **physical Android device** (recommended — you want your real camera roll/screenshots to search) with USB debugging enabled, or use an emulator with some sample images pushed to it.
4. Click **Run**. On first launch, grant the photo permission when prompted.
5. Indexing runs automatically in the background. Give it a few seconds (longer if you have a large gallery), then type a search query and hit **Search**.

### Testing tip
Take a screenshot of something with visible text (a wifi settings screen, a message, anything) right before running the app, then search for a word from that text — that's the fastest way to see it working end-to-end.

### Known issue already fixed in this project: 16 KB page size compatibility
Google requires all apps to support 16 KB memory page-size devices since November 1, 2025. The *bundled* ML Kit text-recognition artifact (`com.google.mlkit:text-recognition`) ships a native `.so` that isn't aligned for this yet, which throws `APK ... is not compatible with 16 KB devices` on newer devices/emulator images. Fix: switched to the *unbundled* Play Services variant (`com.google.android.gms:play-services-mlkit-text-recognition`), which downloads the model via Play Services at runtime instead of bundling it statically — Play Services itself is already 16 KB-compliant. A manifest `meta-data` entry triggers that model download at install time rather than lazily on first use.


## Project structure

```
app/src/main/java/com/example/screenshotsearch/
├── MainActivity.kt              # Compose UI: permission, search bar, results grid
├── data/
│   ├── ScreenshotEntity.kt       # Room entity (image + extracted OCR text)
│   ├── ScreenshotDao.kt          # Room queries, including the search query
│   ├── AppDatabase.kt            # Room database setup
│   └── ScreenshotRepository.kt   # MediaStore access + OCR + search logic
└── worker/
    └── IndexingWorker.kt         # Background indexing (WorkManager)
```

## Next steps (in order)

1. **ContentObserver** — auto re-index when a new screenshot/photo is taken, instead of only indexing once at launch.
2. **Semantic embeddings** — add a TFLite CLIP-style model, embed each image + the query text into the same vector space, add cosine-similarity ranking alongside OCR match.
3. **Hybrid ranking** — combine OCR exact-match score + semantic similarity score into one ranked list.
4. **Date parsing** — detect relative date phrases ("last week", "yesterday") in the query and filter by `dateAdded` before ranking.
5. **Evaluation** — tag ~30-50 of your own images with ground-truth labels, write test queries, measure precision@k for OCR-only vs. semantic-only vs. hybrid. This is what turns the README into a credible engineering writeup, not just a feature list.
6. **MCP interface** — expose `search_photos(query)` as a callable tool via Android AppFunctions/MCP so an external agent can query your gallery.
