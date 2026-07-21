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
