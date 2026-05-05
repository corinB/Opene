# Opene · YOLOv5 Outfit Color Recommender

> 사진 한 장으로 비슷한 색감의 코디를 추천하는 YOLOv5 + KMeans 기반 컬러 매칭 웹앱

[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.1.11-6DB33F?logo=springboot&logoColor=white)](#)
[![Java](https://img.shields.io/badge/Java-17-007396?logo=openjdk&logoColor=white)](#)
[![Python](https://img.shields.io/badge/Python-3.9-3776AB?logo=python&logoColor=white)](#)
[![PyTorch](https://img.shields.io/badge/PyTorch-2.3-EE4C2C?logo=pytorch&logoColor=white)](#)
[![YOLOv5](https://img.shields.io/badge/YOLOv5-custom%20weights-00FFFF)](#)
[![License](https://img.shields.io/badge/License-Mixed-blue)](#license)

---

## Table of Contents

- [Overview](#overview)
- [Demo Flow](#demo-flow)
- [System Architecture](#system-architecture)
- [ML / Data Pipeline](#ml--data-pipeline)
- [Tech Stack](#tech-stack)
- [Repository Layout](#repository-layout)
- [Branch Map](#branch-map)
- [Getting Started](#getting-started)
- [API Reference](#api-reference)
- [Style Catalog](#style-catalog)
- [Tradeoffs & Known Limits](#tradeoffs--known-limits)
- [Roadmap](#roadmap)
- [License](#license)
- [Credits](#credits)
- [Contact](#contact)

---

## Overview

평소 입던 옷을 사진 한 장으로 올리면, 그 사진 속 **상의·하의의 주요 색감과 비슷한 코디**를 같은 스타일 카테고리 안에서 골라 보여주는 웹 서비스입니다.

문제 정의는 단순합니다 — "이 색이랑 어울리는 옷을 어디서 찾지?"라는 일상적인 질문을, 다음 한 줄짜리 파이프라인으로 풉니다:

> **YOLOv5로 상/하의 영역을 검출 → 영역별 KMeans로 대표 RGB 추출 → 무신사에서 미리 크롤링·라벨링한 코디 DB에서 RGB 유클리드 거리가 가까운 후보 선정**

총 **5개 스타일 카테고리 · 약 2,914장**의 무신사 코디 이미지가 전처리된 RGB와 함께 서버에 적재되어 있어, 별도 외부 API 호출 없이 로컬에서 추천이 끝납니다.

## Demo Flow

```
1. 옷 사진 업로드     →  2. 스타일 카테고리 선택     →  3. 추천 코디 캐러셀
   (jpg / png / webp)     (아메카지/캐주얼/시크/             (Bootstrap 캐러셀로
    50MB 이하)              비즈니스캐주얼/스트릿)              순차 표시)
```

## System Architecture

두 개의 프로세스가 `localhost`에서 협업합니다:

```
 ┌──────────────┐     POST /home/upload/image (multipart)
 │   Browser    │ ─────────────────────────────────────────┐
 │  Thymeleaf   │                                          │
 └──────┬───────┘                                          ▼
        │                                  ┌──────────────────────────┐
        │  GET /home/upload (form)         │  Spring Boot 3.1 :8080   │
        │  GET /home (landing)             │  • FileUploadController  │
        │◀─────────────────────────────────│  • FileSystemRepository  │
                                           │       → C:/upload-dir    │
                                           │  • FileUploadService     │
                                           └─────────────┬────────────┘
                                                         │
                                          GET /api?style=&path=…
                                                         ▼
                                           ┌──────────────────────────┐
                                           │   Flask 2.x  :5000       │
                                           │   pythonProject/fun.py   │
                                           │   • YOLOv5 (best.pt)     │
                                           │   • KMeans (n=1)         │
                                           │   • CSV filter           │
                                           └─────────────┬────────────┘
                                                         │ JSON
                                                         ▼
                                                [{"path": "casual_images/x.webp"}, …]
```

요청 흐름:

1. 브라우저가 multipart로 이미지 + 스타일을 Spring에 POST.
2. `FileSystemRepository`가 `C:/upload-dir`을 비우고 새 파일을 저장(이미지 MIME 검사 포함, 디렉터리 탈출 방지).
3. `FileUploadService`가 Flask `/api`에 GET 호출, 절대 경로와 스타일을 쿼리 파라미터로 전달.
4. Flask가 YOLOv5로 추론 → KMeans로 TOP/BOTTOM RGB 추출 → 해당 스타일 CSV에서 유클리드 거리가 가까운 행 필터 → JSON 배열 반환.
5. Spring이 응답 path 앞에 `/img/`를 붙여 정적 리소스 URL로 변환하고 Thymeleaf 캐러셀에 렌더링.

## ML / Data Pipeline

총 5단계로, 각 단계가 어느 브랜치/노트북에서 만들어졌는지 함께 표기합니다:

| # | Stage | Tool / Module | Owner |
|---|-------|---------------|-------|
| 1 | 이미지 크롤링 | `che.이미지크롤링.ipynb` (BeautifulSoup + Selenium, 무신사 6 카테고리) | 권체은 (`origin/che`) |
| 2 | 데이터 정제 | `che.csv빈칸삭제.ipynb` (pandas로 RGB 빈칸 행 제거), PNG → WebP 포맷 변환 | 권체은 (`origin/che`) |
| 3 | 객체 검출 라벨링 / 학습 | `1.상의하의검출.ipynb` (Roboflow + Colab), 결과 가중치 `best.pt` / `last.pt` | 백종현 (`origin/mycolab`, `origin/baek`) |
| 4 | 추론 + 색상 추출 | `pythonProject/fun.py` — YOLOv5 추론 → 바운딩 박스 crop → KMeans(n=1) 대표색 | 백종현 (`origin/baek`) |
| 5 | 코디 매칭 | `fun.py:filter_paths` — TOP/BOTTOM 각각 RGB Euclidean 거리 < 45, 빈 결과 시 BOTTOM만 50으로 완화 | 백종현 (`origin/baek`) |

> KMeans는 클러스터 1개로 호출하기 때문에 사실상 **bbox 영역의 평균 색**에 가까운 대표 RGB를 뽑습니다(픽셀 분포의 mean을 K-means 수렴으로 구하는 형태). 픽셀 단위로 dominant color를 잡고 싶다면 `n_clusters`를 올려 빈도 1위 클러스터를 고르는 식으로 확장할 수 있습니다.

## Tech Stack

| Layer | Tech | Notes |
|-------|------|-------|
| Frontend | Thymeleaf, Bootstrap 4.5, vanilla JS | 단일 페이지 업로드 폼 + Bootstrap carousel |
| Web BE | Spring Boot 3.1.11, Java 17, Gradle (WAR), Lombok | `RestTemplate`로 Flask 호출, multipart 50MB |
| AI Server | Flask, YOLOv5 (ultralytics), PyTorch 2.3, OpenCV 4.9, scikit-learn KMeans, pandas | conda Python 3.9 환경 |
| Data | 5 styles × WebP 이미지 합계 ≈ 2,914장, 스타일별 CSV (`TOP_RGB,BOTTOM_RGB,path`) | 무신사에서 크롤링 |
| Infra | 로컬 단일 머신 (Spring `:8080` + Flask `:5000`), 업로드 임시 폴더 `C:/upload-dir` | 단일 사용자 데모용 |

## Repository Layout

```
Opene/
├── openSource/                       # Spring Boot WAR module
│   ├── build.gradle                  # Spring Boot 3.1.11, Java 17
│   └── src/main/
│       ├── java/com/example/openSource/
│       │   ├── OpenSourceApplication.java
│       │   ├── ServletInitializer.java
│       │   ├── config/AppConfig.java         # RestTemplate 빈
│       │   ├── controller/FileUploadController.java
│       │   ├── service/FileUploadService.java # Flask 호출 + path 변환
│       │   ├── repository/FileSystemRepository.java # C:/upload-dir 저장
│       │   ├── dto/FlaskResDto.java
│       │   └── util/DirectoryCleaner.java
│       └── resources/
│           ├── application.yaml      # multipart 50MB
│           ├── static/css/           # stylesFile.css, stylesImageShow.css
│           ├── static/img/           # {style}_images/ × 5  (≈2,914 WebP)
│           └── templates/files/      # home.html, file.html, imageShow.html
│
└── pythonProject/                    # Flask AI server
    ├── fun.py                        # 서버 entrypoint (Flask /api)
    ├── app.py                        # 단독 실험 스크립트 (서버 아님)
    ├── best.pt / last.pt             # YOLOv5 가중치
    ├── packagelist.txt               # conda 환경 정의
    └── che_csv/                      # 스타일별 RGB+path CSV × 5
```

## Branch Map

각 원격 브랜치는 팀원 한 명의 작업 영역과 일대일로 대응합니다.

| Branch | Owner | 산출물 | 한 줄 요약 |
|--------|-------|--------|-----------|
| `master` | (통합) | 위 트리 전부 | Spring + Flask + 5 스타일 데이터셋의 통합 데모 가능 형태 |
| `baek` | 백종현 | `fun.py`, `best.pt`, `last.pt`, `baek.txt` | YOLOv5 학습 가중치 + Flask `/api` 추론 서버 |
| `bin` | 진다빈 | `openSource/**`, `1.상의하의검출.ipynb`, `dabin.text` | Spring Boot 백엔드와 Thymeleaf 프론트, BE↔AI 통신 와이어링 |
| `che` | 권체은 | `che.이미지크롤링.ipynb`, `che.csv빈칸삭제.ipynb`, `che.txt`, 6 스타일 결과 CSV | 무신사 크롤러 + RGB CSV 정제 + WebP 포맷 변환 |
| `mycolab` | 백종현 (보조) | `flask.ipynb`, `Untitled0.ipynb`, `colab_requirements.txt` | Colab 기반 학습/실험 환경 (`streamlit`, `pyngrok`로 외부 노출 시도) |

> 통합 시 `bin`의 `Service` 패키지(대문자) → `service`(소문자)로 표기를 맞추고, `che`가 만든 6개 카테고리(amecaji/businesscasual/casual/chic/street/dandy) 중 dandy는 master에서 제외했습니다.

## Getting Started

### Prerequisites

- JDK 17 (Amazon Corretto 권장)
- Anaconda / Miniconda
- Windows 환경 — 업로드 디렉터리 `C:/upload-dir`이 코드에 하드코딩되어 있으므로 미리 생성해 두세요. macOS/Linux는 [Tradeoffs](#tradeoffs--known-limits) 참고.
- YOLOv5 가중치 `best.pt` (origin/baek 브랜치 또는 별도 학습 결과물)

### Run the Spring Boot app

```powershell
cd openSource
./gradlew bootRun           # http://localhost:8080/home
./gradlew build             # build/libs/*.war
./gradlew test              # JUnit 5
```

### Run the Flask AI server

```powershell
cd pythonProject
conda create --name opene --file packagelist.txt
conda activate opene
python fun.py               # http://localhost:5000/api
```

> 두 서버를 동시에 띄운 뒤 브라우저로 `http://localhost:8080/home/upload`에 접속해 이미지를 업로드합니다.

## API Reference

### Spring (port 8080)

| Method | Path | Description |
|--------|------|-------------|
| GET | `/home` | 랜딩 페이지 |
| GET | `/home/upload` | 업로드 폼 (스타일 셀렉트박스) |
| POST | `/home/upload/image` | multipart `file`, form `selectbox` (style key) → 캐러셀 페이지로 forward |

### Flask (port 5000)

```http
GET /api?style={amecaji|casual|chic|businesscasual|street}&path={absolute_image_path}
```

응답:

```json
[
  { "path": "casual_images/12_3.webp" },
  { "path": "casual_images/27_1.webp" }
]
```

Spring은 위 응답의 각 `path` 앞에 `/img/`를 붙여 `static/img/{style}_images/...`로 매핑합니다.

## Style Catalog

스타일 키는 **3곳에서 동일하게 유지**되어야 합니다 — 셀렉트박스 옵션 / CSV 파일명 / 정적 이미지 디렉터리.

| Key | 한글명 | CSV (`pythonProject/che_csv/`) | Images (`static/img/.../`) | Count |
|-----|-------|-------------------------------|---------------------------|------:|
| `amecaji` | 아메카지 | `amecaji.csv` | `amecaji_images/` | 606 |
| `casual` | 캐주얼 | `casual.csv` | `casual_images/` | 600 |
| `chic` | 시크 | `chic.csv` | `chic_images/` | 600 |
| `businesscasual` | 비즈니스캐주얼 | `businesscasual.csv` | `businesscasual_images/` | 600 |
| `street` | 스트릿 | `street.csv` | `street_images/` | 508 |

CSV 스키마: index, `TOP_RGB`, `BOTTOM_RGB`, `path` (예: `(48, 43, 48)`, `(103, 113, 127)`, `casual_images/1_0.webp`).

## Tradeoffs & Known Limits

포트폴리오로 공개하기 전에 알고 넘겨야 할 한계점들 — 의도적으로 정직하게 적습니다.

- **Windows 경로 하드코딩**: `C:/upload-dir`이 `FileSystemRepository.java:18`과 `FileUploadService.java:35`에 박혀 있습니다. 다른 OS에서는 코드 수정이 필요합니다.
- **검출 순서 의존**: `fun.py:58-69`는 YOLOv5 결과의 인덱스 0을 TOP, 1을 BOTTOM으로 가정합니다. 검출 개수가 1개이거나 순서가 반대인 경우 라벨이 어긋나 다운스트림에서 `result['TOP_RGB'].values[0]`이 NaN이 되며 호출이 실패합니다.
- **유클리드 거리 휴리스틱**: 매칭 임계값 45 → 빈 결과 시 BOTTOM만 50으로 완화. RGB 공간은 인지적 거리에 비례하지 않으므로 LAB + Delta-E로 옮기면 추천 품질이 개선될 가능성이 큽니다.
- **macOS/Linux 호환**: `fun.py:18-19`의 `pathlib.PosixPath = pathlib.WindowsPath` 패치는 Windows에서 학습된 가중치를 unpickle하기 위함이며, 다른 OS에서는 주석 처리해야 합니다.
- **`app.py`는 서버가 아님**: 동일 폴더의 `app.py`는 초기 실험 스크립트로, `cv2.imread('t1.webp'))`(잉여 괄호) 등 syntax 오류가 남아 있습니다. 서버 entrypoint는 `fun.py`입니다.
- **단일 사용자 데모**: 업로드 폴더를 매 요청마다 비우므로 동시 요청이 들어오면 race condition이 발생합니다.

## Roadmap

- [ ] Docker Compose로 Spring + Flask 동시 기동 + 경로 환경 변수화
- [ ] 매칭 색공간을 RGB → CIE LAB / Delta-E로 교체
- [ ] 업로드 스토리지를 로컬 폴더 → 객체 스토리지(S3 등)로 분리
- [ ] 모바일 반응형 + 스타일 카드 UI 개편
- [ ] 사용자 좋아요/싫어요 피드백 루프로 매칭 가중치 학습

## License

오픈소스 라이브러리 라이선스를 그대로 따릅니다. 코디 이미지는 [무신사](https://www.musinsa.com)에서 크롤링한 자료로, 학습 및 비상업 데모 용도로만 사용하세요.

| Component | License |
|-----------|---------|
| Spring Boot | Apache 2.0 |
| Bootstrap | MIT |
| YOLOv5 | GPL-3.0 |
| PyTorch | BSD-3-Clause |
| Crawled images | © MUSINSA — 비상업 데모 한정 |

## Credits

| 이름 | 역할 | 브랜치 |
|------|------|--------|
| 백종현 | YOLOv5 학습, Flask API 서버, BE↔AI 연결 | `baek`, `mycolab` |
| 진다빈 | Spring Boot 메인 서버, 프론트엔드, BE↔AI 연결 | `bin` |
| 권체은 | 무신사 크롤링, AI 결과 CSV 정제, PNG → WebP 포맷 변환 | `che` |

## Contact

- Email: gkdisrha2020@gmail.com
- GitHub: [corinB/Opene](https://github.com/corinB/Opene)
