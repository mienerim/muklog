# 실행 계획: 뭐먹었지 (1개월 스프린트)

**기반 문서**: 뭐먹었지_PRD_v0.1.md
**원칙**: 의존성 순서대로 진행, 매 주차 끝에 "눈으로 확인 가능한 결과물" 확보
**확정 스택**: Spring Boot + PostgreSQL (백엔드), Flutter (프론트엔드), 구글 OAuth (인증)

---

## 1주차 — 기반 다지기

목표: "빈 화면이지만 로그인해서 들어가지는" 앱이 뜬다.

### 백엔드
- [x] Spring Boot 프로젝트 초기화 (Gradle, 패키지 구조 설계)
- [x] PostgreSQL 연결 설정 (로컬 개발 DB — DevSpace 경험 재사용 가능)
- [x] `User`, `Record` 엔티티 및 테이블 마이그레이션 (Flyway 또는 JPA DDL) — 확정된 ERD 그대로 반영
- [x] 구글 OAuth 연동 (Spring Security + OAuth2 Client) — 로그인 성공 시 JWT 발급까지 (실제 구현은 모바일 앱에 맞춰 Google ID Token 서버 검증 + 자체 JWT 발급 방식으로 진행, `.oauth2Login()` 리다이렉트 플로우 대신)
- [x] 헬스체크 API 하나 만들어서 배포 파이프라인 최소 확인 (`GET /health`)

### 프론트엔드
- [x] Flutter SDK 설치 (3.44.6 stable, 로컬 환경)
- [x] Flutter 프로젝트 초기화 (`frontend/`, org `com.muklog`, android/ios/web/windows 타겟 포함, Chrome에서 기본 화면 구동 확인)
- [ ] 폴더 구조 설계 (feature-first 또는 layer-first 택1) — 현재는 기본 스캐폴드(`lib/main.dart`) 상태, 아직 미설계
- [ ] Android SDK 설치 (Android Studio) — 실제 Android 기기/에뮬레이터 실행에 필요, 아직 미설치
- [x] 구글 로그인 SDK 연동, 로그인 화면 UI — Flutter Web 대상으로 구현 완료 (`google_sign_in` v7 + `google_sign_in_web`, `lib/screens/login_screen.dart`, `lib/services/auth_service.dart`). Android/iOS 네이티브 로그인은 별도 OAuth 클라이언트 필요해서 스코프 아님 (아래 항목 참고)
- [x] 로그인 성공 시 홈 화면(빈 상태)으로 이동하는 라우팅 — `lib/screens/home_screen.dart` + `pushReplacement` 구현 완료
- [ ] **구글 로그인 실제 동작 검증 (E2E)** — 코드/설정은 다 됐지만 아직 미완료. 진행 중 발견/해결한 이슈:
  - 웹 클라이언트에 Authorized JavaScript origin(`http://localhost:5000`) 누락되어 있던 것 발견 → Cloud Console에서 추가함. 다만 구글 쪽 설정 반영에 지연이 있어("5분~몇 시간 걸릴 수 있음" 안내) 아직 "origin not allowed" 에러로 검증 대기 중
  - 백엔드에 CORS 설정이 아예 없어서 (원래 모바일 전용 설계라 불필요했음) Flutter Web에서 `POST /auth/google` 호출이 브라우저에 의해 차단됨 → `SecurityConfig`에 개발용 CORS 설정 추가로 해결 (`http://localhost:5000` 허용)
  - 로컬 테스트용으로 Postgres는 Docker 컨테이너(포트 55432, 다른 프로젝트와 충돌 방지), 백엔드는 포트 8081로 임시 실행 중 (다른 로컬 프로젝트가 5432/8080을 이미 점유하고 있어서) — 영구 설정 아님, 임시 검증용

### 배포/인프라
- [ ] Cloudtype 또는 유사 플랫폼에 백엔드 배포 파이프라인 최소 구성
- [x] 구글 Cloud Console 프로젝트(`nomlog-yumi`) OAuth 동의 화면 구성 (외부, 테스트 중) + 테스트 사용자 등록 (본인 + 지인 1명)
- [x] 구글 Cloud Console에 OAuth 클라이언트 등록 — 웹 (백엔드 ID 토큰 검증용, `GOOGLE_CLIENT_ID`로 반영 완료; Authorized JavaScript origin `http://localhost:5000`도 추가 완료, 전파 대기 중)
- [ ] 구글 Cloud Console에 OAuth 클라이언트 등록 — Android (패키지명 + SHA-1 인증서 지문 필요, Flutter 프로젝트는 생성됐으니 진행 가능)
- [ ] 구글 Cloud Console에 OAuth 클라이언트 등록 — iOS (번들 ID 필요)

**주차 끝 확인 지점**: 구글 로그인 → 빈 홈 화면 진입까지 실제 기기에서 동작.

---

## 2주차 — 기록 CRUD

목표: 기록을 추가/조회/수정/삭제할 수 있고, 홈 화면에서 실제 데이터가 보인다.

### 백엔드
- [x] `POST /records` — 기록 생성 (food_name, category, image_url, eaten_at)
- [x] `GET /records?date=` — 특정 날짜 기록 목록 조회
- [x] `PATCH /records/{id}` — 기록 수정
- [x] `DELETE /records/{id}` — 기록 삭제
- [x] `GET /records/autocomplete?q=` — food_name/category 자동완성 (과거 기록 기반 접두어 검색)
- [ ] 이미지 업로드용 presigned URL 발급 API (스토리지는 S3 호환 or Supabase Storage 등 가벼운 걸로) — 스토리지 계정 미확정으로 보류

### 프론트엔드
- [ ] 기록 추가 화면 구현 (와이어프레임 v2 반영: 텍스트 입력 + 자동완성 + 카테고리 pill + 시간 자동값 + 사진 선택)
- [ ] 홈 화면 — 날짜별 기록 리스트 뷰
- [ ] 홈 화면 — 날짜 캐러셀(좌우 슬라이딩) 구현
- [ ] 기록 수정/삭제 인터랙션 (스와이프 or 롱프레스 등 택1)

**주차 끝 확인 지점**: 실제로 오늘 먹은 걸 기록하고, 날짜를 넘겨가며 과거 기록도 확인 가능.

> 이 시점에 본인이 며칠간 직접 써보면서 "기록이 몇 탭 만에 끝나는지" 스스로 체감해보는 걸 추천 — PRD의 핵심 설계 원칙이라 개발자 본인 dogfooding이 제일 빠른 검증.

---

## 3주차 — 통계 + 공유

목표: 쌓인 기록이 의미 있는 숫자로 보이고, 공유까지 가능하다.

### 백엔드
- [ ] `GET /stats/monthly?year=&month=` — 월간 통계 (기록 횟수, 카테고리별 빈도, Top N 음식)
- [ ] `GET /stats/yearly?year=` — 연간 통계 (월간과 동일 로직, 기간만 확장)
- [ ] (통계는 별도 테이블 없이 `Record`에서 집계 쿼리로 처리 — ERD 설계 원칙 그대로)

### 프론트엔드
- [ ] 통계 화면 구현 (와이어프레임 반영: 요약 카드 2개 + 카테고리별 바 차트)
- [ ] 이미지 카드 공유 기능 — 통계 화면을 이미지로 캡처/렌더링해서 카톡/인스타로 내보내기
  - Flutter의 `RepaintBoundary` + 이미지 저장/공유 패키지 활용

**주차 끝 확인 지점**: 한 달 치 기록을 통계로 확인하고, 카드 이미지 하나를 실제로 카톡에 공유해봄.

---

## 4주차 — 다듬기 + 배포 + 테스트

목표: 지인이 실제로 설치해서 며칠간 써볼 수 있는 상태.

### 버그 수정 및 다듬기
- [ ] 1~3주차 기능 전체 QA (본인 기준 + 가능하면 1명에게 먼저 테스트 요청)
- [ ] 에러 처리 (네트워크 실패, 빈 상태 UI 등 — 크래시 없이 우아하게 처리되는지)
- [ ] 로딩 상태 UI (기록 저장 중, 통계 불러오는 중 등)

### 배포
- [ ] Android: APK 빌드 후 직접 배포 (스토어 등록 없이 파일 공유) 또는 Google Play 비공개 테스트 트랙
- [ ] iOS: TestFlight 베타 배포 (Apple Developer 계정 필요 시에만 — 테스트 인원에 iOS 사용자가 있는지에 따라 결정)
- [ ] 개인정보처리방침 최소 문서 1페이지 작성 (스토어/TestFlight 요구사항 대응용)

### 테스트 & 피드백 수집
- [ ] 지인 1~4명에게 배포, 사용 안내
- [ ] PRD 7.2 정성 지표 기준 피드백 수집 (기록 마찰, 이탈 이유, 소셜 니즈)
- [ ] PRD 7.3 Go/No-go 판단을 위한 회고 정리

**주차 끝 확인 지점**: PRD 7.3 기준에 따라 Go / Pivot / No-go 판단.

---

## 참고: 이번 스프린트에 포함하지 않는 것

PRD 5.2, 6장 오픈 퀘스천 그대로 유지 — 아래는 태스크 목록에 의도적으로 넣지 않음:

- 친구 추가/피드/시간대 동기화 등 소셜 기능 일체 (6.1 오픈 퀘스천 — 4주차 피드백 이후 결정)
- AI 이미지 음식 인식
- 인스타/배달앱 연동
- Redis, Nginx, MQTT 등 인프라 고도화
- 스토어 정식 등록 (구글/애플 유료 등록은 Go 판단 이후)

---

## 클로드 코드 활용 메모

- 각 체크박스 단위가 클로드 코드에 던질 하나의 작업 단위로 적당한 크기 (너무 크면 "1주차 백엔드 다 해줘"보다 "구글 OAuth 연동해줘" 식으로 쪼개서 요청하는 게 검증하기 쉬움)
- 매 기능 구현 후 실제로 눌러보고 확인 — 특히 2주차 기록 추가 화면은 PRD 핵심 원칙(입력 마찰)과 직결되니 눈으로 직접 탭 수를 세어볼 것
