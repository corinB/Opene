# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Big picture

Two cooperating processes on `localhost`:

- `openSource/` — Spring Boot 3.1 / Java 17 web app on **:8080**. Serves Thymeleaf pages, accepts an image upload, calls the Flask API, and renders the returned image carousel.
- `pythonProject/` — Flask app (`fun.py`) on **:5000**. Loads a custom YOLOv5 model (`best.pt`), detects top/bottom clothing bboxes, extracts the dominant RGB of each via KMeans, and filters a per-style CSV by RGB Euclidean distance.

Request flow for `POST /home/upload/image`:

1. `FileUploadController` → `FileUploadService.store` → `FileSystemRepository` writes the multipart file to **`C:/upload-dir`** (hard-coded, Windows-only path), after `DirectoryCleaner` wipes that folder.
2. `FileUploadService.process` calls `GET http://localhost:5000/api?style={style}&path={absolutePath}`.
3. Flask's `api_call` (`fun.py:140`) runs YOLOv5 on the file at `path`, reads `che_csv/{style}.csv`, and returns `[{"path": "..."}, ...]`.
4. Spring prefixes each returned `path` with `/img/` and renders `files/imageShow.html`. Static images live at `openSource/src/main/resources/static/img/{style}_images/`, so the CSV `path` values must be relative to `static/img/`.

The `style` values are a closed set wired across three places — keep them in sync if adding a new one:

- `<option value="...">` in `templates/files/file.html` (`amecaji`, `casual`, `chic`, `businesscasual`, `street`)
- a CSV file at `pythonProject/che_csv/{style}.csv` (columns: `TOP_RGB`, `BOTTOM_RGB`, `path`)
- a static image directory at `openSource/src/main/resources/static/img/{style}_images/`

## Color matching gotcha

`fun.py:113 filter_paths` uses Euclidean RGB distance with a threshold of **45**, and falls back to **50 for the bottom only** if the first pass returns no matches. Class label is assigned by detection index: idx 0 → TOP, idx 1 → BOTTOM (`fun.py:58`). If YOLOv5 returns detections in a different order or only one bbox, the wrong slot ends up populated and downstream lookups crash on `result['TOP_RGB'].values[0]`.

## Build & run

Run each subproject from its own directory.

```powershell
# Spring (openSource/) — packaged as WAR
cd openSource
./gradlew bootRun        # dev run on :8080
./gradlew build          # produces build/libs/*.war
./gradlew test           # JUnit 5

# Flask (pythonProject/) — conda env, Python 3.9/3.10
cd pythonProject
conda create --name opene --file packagelist.txt
conda activate opene
python fun.py            # Flask dev server on :5000
```

Notes:

- `pythonProject/app.py` is a **standalone color-extraction script, not the server** (it has a syntax error at `cv2.imread('t1.webp'))` and no Flask app). The server entrypoint is `fun.py`.
- `fun.py:18-20` and `app.py:8-9` monkey-patch `pathlib.PosixPath = pathlib.WindowsPath` so YOLOv5 can unpickle Windows-trained weights. **Comment those lines out on macOS/Linux.**
- Before first run, ensure `C:/upload-dir` exists (the Spring code reads/writes it but does not create it).
- Multipart limit is 50 MB (`application.yaml`).

## Conventions

- Internal artifacts (commit messages, code comments, planning notes) in **English**; user-facing strings and conversational replies in **Korean** — inherited from the parent `~/CLAUDE.md`.
- Spring package root: `com.example.openSource` (controller / service / repository / dto / util / config). `AppConfig` exposes a singleton `RestTemplate`.
