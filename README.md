# AI Recruit Platform

> **LLM + RAG 기반 AI 채용 매칭 플랫폼** — Spring Boot MSA 아키텍처

---

## 왜 만들었나

기존에는 모놀리식 아키텍처로만 개발해왔습니다. 하나의 큰 프로젝트에 모든 기능을 넣다 보니, 작은 이슈 하나가 전체 서비스를 멈추는 상황을 반복해서 경험했습니다. MSA는 각 기능이 독립적으로 배포·운영되어 하나의 서비스에 장애가 발생해도 다른 핵심 기능은 계속 동작한다는 점에 관심이 생겼고, 직접 경험해보고 싶었습니다.

MSA와 함께 실무에서 많이 쓰이는 **Kafka, Redis 캐싱, Elasticsearch, RAG** 기술 스택도 사이드 프로젝트를 통해 경험하고 싶었습니다. "이 기술들을 모두 써볼 수 있는 적합한 주제가 뭘까?"를 고민하다, 원티드·사람인 같은 채용 플랫폼을 벤치마킹한 **AI 채용 플랫폼**으로 방향을 잡았습니다.

- **MSA + Kafka**: 서비스 독립성 확보, AI 분석을 비동기 파이프라인으로 처리
- **Elasticsearch + RAG**: 키워드 검색을 넘어 이력서 임베딩 기반 의미 유사 공고 추천
- **Redis 캐싱**: AI 응답을 캐싱해 반복 요청 시 14초 → 0.01초 실현
- **Claude API**: 이력서-JD 적합도 분석, 코칭 피드백, 합격 전략 제안

---

## 화면

> 스크린샷 추가 예정 (채용공고 목록, AI 리뷰 카드, AI 코칭 결과)

<!-- 아래에 스크린샷 추가
![채용공고 목록](docs/screenshots/jobs-list.png)
![AI 리뷰](docs/screenshots/ai-review.png)
![AI 코칭](docs/screenshots/ai-coaching.png)
-->

---

구직자와 채용공고를 AI로 매칭하는 **Spring Boot MSA 아키텍처** 기반 채용 플랫폼입니다.
Claude AI를 활용해 이력서를 분석하고, 맞춤 채용공고 추천 및 코칭 피드백을 제공합니다.

---

## 주요 기능

- **JWT 인증/인가** — 로그인, 로그아웃, 토큰 재발급, Redis 블랙리스트
- **소셜 로그인** — Kakao, Apple OAuth2
- **채용공고** — CRUD, Elasticsearch 전문 검색, 크롤러 (Wanted / 사람인)
- **이력서 관리** — PDF 업로드, 이력서 목록/상세 조회
- **지원 관리** — 지원하기, 지원 현황, 지원 취소
- **AI 분석** — Claude API로 이력서-JD 적합도 스코어링, 피드백 생성
- **AI 리뷰** — 공고 상세 진입 시 이력서 적합도 즉시 분석 (점수·강점·약점·합격전략)
- **AI 코칭** — 이력서 종합 점수, 개선 포인트 제안
- **AI 공고 추천** — 이력서 임베딩 kNN 검색으로 의미 기반 유사 공고 추천 (OpenAI text-embedding-3-small + ES dense_vector)
- **이메일 알림** — Kafka 이벤트 기반 비동기 알림 (notification-service)

---

## 아키텍처

```
Client (React Native / Expo)
    │
    ▼
API Gateway :8080  ← JWT 검증, X-User-Id 헤더 주입
    │
    ├── auth-service       :8081  JWT 발급/검증
    ├── user-service       :8082  회원가입, 소셜 로그인, 프로필
    ├── job-service        :8083  채용공고, ES 검색, 크롤러
    ├── application-service:8084  지원/이력서/AI 분석
    └── notification-service:8085 Kafka Consumer, 이메일
```

### 데이터 흐름 (지원하기 + AI 피드백)

```
Client → API Gateway → application-service
                           │
                           ├── MySQL: applications 저장
                           └── Kafka: "application.submitted" 발행
                                           │
                              ┌────────────┴────────────┐
                              ▼                         ▼
                   application-service           notification-service
                   (Kafka Consumer)              (Kafka Consumer)
                   Claude API 이력서 분석          지원 완료 이메일
                   ai_feedback 저장
                   "resume.analyzed" 발행
                              │
                              ▼
                   notification-service
                   AI 분석 완료 이메일
```

---

## 기술 스택

| 분류 | 기술 | 선택 이유 |
|------|------|----------|
| Language | Java 17 | LTS 버전, Record/Pattern Matching 등 최신 문법 지원 |
| Framework | Spring Boot 3.3.5 | MSA에 최적화된 생태계, Spring Cloud와 자연스러운 통합 |
| Build | Maven (멀티모듈) | 모듈 간 의존성 관리 및 공통 설정 일원화 |
| Gateway | Spring Cloud Gateway 2023.0.3 | Reactive 기반 고성능 라우팅, JWT 필터 통합 용이 |
| DB | MySQL 8.0 | 안정적인 RDBMS, JPA와의 높은 호환성 |
| Cache | Redis 7 | JWT 블랙리스트·AI 응답 캐싱, TTL 기반 자동 만료 |
| Message Queue | Apache Kafka (Confluent 7.5.0) | AI 분석·이메일 알림 비동기 처리, 서비스 간 느슨한 결합 |
| Search | Elasticsearch 8.11.0 | 채용공고 키워드/필터 전문 검색 + dense_vector kNN 벡터 검색 (dims=1536, cosine similarity) |
| LLM | Claude API (`claude-sonnet-4-6`, `claude-haiku-4-5`) | 정확도가 중요한 이력서 분석은 Sonnet, 속도가 중요한 코칭/추천은 Haiku로 모델 분리 |
| Container | Docker Compose | AWS 프리티어 한계로 로컬 개발환경 구성, 단일 명령으로 전체 인프라 실행 |

---

## 프로젝트 구조

```
ai-recruit-platform/
├── common/               # 공유 라이브러리 (ResultData, Status, BizException)
├── api-gateway/          # Spring Cloud Gateway + JWT 필터
├── auth-service/         # 인증 서비스
├── user-service/         # 사용자 서비스
├── job-service/          # 채용공고 서비스
├── application-service/  # 지원/이력서/AI 서비스
├── notification-service/ # 알림 서비스
└── docker-compose.yml    # 로컬 인프라 전체
```

---

## 시작하기

### 사전 요구사항

- Java 17
- Docker & Docker Compose
- Maven 3.8+

### 1. 인프라 실행

```bash
docker compose up -d
```

| 서비스 | 포트 |
|--------|------|
| MySQL | 3306 |
| Redis | 6379 |
| Kafka | 9092 |
| Elasticsearch | 9200 |
| Kibana | 5601 |

### 2. 환경 변수 설정

프로젝트 루트에 `.env` 파일을 생성합니다.

```bash
DB_HOST=localhost
DB_USERNAME=airecruit
DB_PASSWORD=password
REDIS_HOST=localhost
KAFKA_HOST=localhost
ES_HOST=localhost
JWT_SECRET_KEY=your_jwt_secret_key
ENCRYPTION_SECRET_KEY=your_encryption_key
MAIL_USERNAME=your_email@gmail.com
MAIL_PASSWORD=your_app_password
KAKAO_CLIENT_ID=your_kakao_client_id
KAKAO_CLIENT_SECRET=your_kakao_client_secret
CLAUDE_API_KEY=your_claude_api_key
OPENAI_API_KEY=your_openai_api_key
```

### 3. 빌드

```bash
# common 모듈 먼저 설치
JAVA_HOME=$(/usr/libexec/java_home -v 17) ./mvnw install -pl common -am

# 전체 빌드
JAVA_HOME=$(/usr/libexec/java_home -v 17) ./mvnw clean package -DskipTests
```

### 4. 서비스 실행

각 서비스를 개별 터미널에서 실행합니다.

```bash
# 예: auth-service
JAVA_HOME=$(/usr/libexec/java_home -v 17) ./mvnw spring-boot:run -pl auth-service

# 예: job-service
JAVA_HOME=$(/usr/libexec/java_home -v 17) ./mvnw spring-boot:run -pl job-service
```

또는 `start.sh` 스크립트를 사용합니다.

```bash
./start.sh
```

---

## API 엔드포인트

모든 요청은 API Gateway(`http://localhost:8080`)를 통해 처리됩니다.

### Auth

| 메서드 | 경로 | 설명 | 인증 |
|--------|------|------|------|
| POST | `/api/v1/auth/login` | 로그인 (accessToken + refreshToken 발급) | 불필요 |
| POST | `/api/v1/auth/logout` | 로그아웃 (토큰 블랙리스트 등록) | 필요 |
| POST | `/api/v1/auth/reissue` | 액세스 토큰 재발급 | 불필요 |

### User

| 메서드 | 경로 | 설명 | 인증 |
|--------|------|------|------|
| POST | `/api/v1/users/signup` | 회원가입 | 불필요 |
| POST | `/api/v1/users/email/send` | 이메일 인증 코드 발송 | 불필요 |
| POST | `/api/v1/users/email/verify` | 이메일 인증 코드 확인 | 불필요 |
| GET | `/api/v1/users/me` | 내 정보 조회 | 필요 |
| GET | `/api/v1/users/kakao/callback` | 카카오 소셜 로그인 | 불필요 |

### Job

| 메서드 | 경로 | 설명 | 인증 |
|--------|------|------|------|
| GET | `/api/v1/jobs` | 채용공고 목록 (페이징) | 불필요 |
| GET | `/api/v1/jobs/{id}` | 채용공고 상세 | 불필요 |
| GET | `/api/v1/jobs/search` | Elasticsearch 검색 | 불필요 |
| POST | `/api/v1/jobs` | 채용공고 등록 | 필요 |
| PUT | `/api/v1/jobs/{id}` | 채용공고 수정 | 필요 |
| DELETE | `/api/v1/jobs/{id}` | 채용공고 마감 | 필요 |
| POST | `/api/v1/jobs/crawl` | 크롤링 수동 트리거 | 필요 |
| POST | `/api/v1/jobs/reindex` | Elasticsearch 전체 재색인 | 필요 |
| POST | `/api/v1/jobs/search/vector` | 벡터 기반 kNN 유사 공고 검색 | 불필요 |

### Application

| 메서드 | 경로 | 설명 | 인증 |
|--------|------|------|------|
| POST | `/api/v1/applications` | 지원하기 | 필요 |
| GET | `/api/v1/applications` | 내 지원 목록 | 필요 |
| GET | `/api/v1/applications/{id}` | 지원 상세 (AI 피드백 포함) | 필요 |
| DELETE | `/api/v1/applications/{id}` | 지원 취소 | 필요 |

### Resume

| 메서드 | 경로 | 설명 | 인증 |
|--------|------|------|------|
| POST | `/api/v1/resumes` | 이력서 업로드 (PDF/텍스트) | 필요 |
| GET | `/api/v1/resumes` | 내 이력서 목록 | 필요 |
| GET | `/api/v1/resumes/{id}` | 이력서 상세 | 필요 |
| DELETE | `/api/v1/resumes/{id}` | 이력서 삭제 | 필요 |
| POST | `/api/v1/resumes/{id}/coaching` | AI 코칭 피드백 요청 | 필요 |
| GET | `/api/v1/resumes/{id}/recommended-jobs` | AI 공고 추천 | 필요 |

---

## Kafka 토픽

| 토픽 | Producer | Consumer | 설명 |
|------|----------|----------|------|
| `job.created` | job-service | notification-service | 새 공고 등록 시 |
| `application.submitted` | application-service | application-service, notification-service | 지원 완료 시 |
| `resume.analyzed` | application-service | notification-service | AI 분석 완료 시 |

---

## AI 성능 최적화

**1차 최적화 (2026-03-07)**

| 기법 | 내용 |
|------|------|
| 모델 분리 | 지원 분석(비동기) → `claude-sonnet-4-6`, 코칭/추천/리뷰(사용자 대기) → `claude-haiku-4-5-20251001` |
| Prompt Caching | `cache_control: ephemeral` + `anthropic-beta: prompt-caching-2024-07-31` 헤더 |
| 공고 사전 필터링 | 이력서 기술 키워드 추출 → 공고 20개 → 상위 8개 압축 (프롬프트 ~60% 단축) |
| Redis 결과 캐싱 | `ai:coaching:{id}`, `ai:recommend:{id}`, `ai:review:{jobId}:{resumeId}` TTL 1시간 |
| 529 Retry | Exponential backoff (1초 → 2초, 최대 3회) |

**2차 최적화 (2026-03-13) — 프롬프트 압축 + max_tokens 축소**

| 기법 | 내용 |
|------|------|
| 입력 토큰 축소 | 이력서 최대 3000자 → 800~1500자, JD 불필요 섹션 제거 |
| 출력 항목 최소화 | improvements 3개 → 2개, 각 필드 20자 이내 명시 |
| max_tokens 축소 | 코칭 1024→640, 추천 1024→512, 리뷰 1024→400 |

**3차 최적화 (2026-03-13) — kNN 도입에 따른 추천 프롬프트 재설계**

| 기법 | 내용 |
|------|------|
| 공고 desc 제거 | kNN이 의미 매칭을 담당하므로 Claude 입력에서 description 불필요 |
| 공고 포맷 간소화 | JSON → `id\|title\|company` 한 줄 |
| 이력서 추가 축소 | 800자 → 300자 |

**누적 성능 결과:**

| 기능 | 최초 | 1차 최적화 후 | 2차 최적화 후 | 3차 최적화 후 | 누적 개선 |
|------|------|-------------|-------------|-------------|---------|
| AI 코칭 | 14.9초 | 5.9초 | **3.66초** | — | **75%↓** |
| AI 추천 | 14.3초 | 6.2초 | 4.35초 | **3.7초** | **74%↓** |
| AI 리뷰 | — | 6.1초 (첫 구현) | **3.32초** | — | **46%↓** |
| 캐시 히트 | — | 0.01초 | 0.01초 | 0.01초 | — |

---

## 트러블슈팅

실제 개발 과정에서 겪은 주요 문제와 해결 과정입니다.

### 1. Redis write silent failure — 로그아웃 후 토큰이 무효화되지 않음

**문제:** 로그아웃 API는 200을 반환하지만, 로그아웃한 토큰으로 계속 API 호출이 가능했습니다.

**원인:** `RedisConfig`에서 `new LettuceConnectionFactory("localhost", 6379)`로 수동 생성하면 Spring Boot 자동 구성이 적용되지 않아, application.yml의 Redis 설정이 무시된 채 기본값으로 다른 인스턴스에 연결됩니다. 블랙리스트 저장 자체는 성공(`void` 반환)이지만 실제 Redis에는 반영되지 않았습니다.

**해결:** `LettuceConnectionFactory`를 직접 생성하는 대신, Spring Boot가 주입하는 `RedisConnectionFactory` 빈을 그대로 사용하도록 변경했습니다.

---

### 2. Elasticsearch 검색 결과 0건 — 필드 타입 불일치

**문제:** 채용공고 검색 API가 항상 0건을 반환했습니다.

**원인:** ES 인덱스의 `title`, `company` 필드가 `text` 타입으로 색인되어 있었으나, 검색 쿼리는 `keyword` 타입에 맞는 exact match로 동작하고 있었습니다. ES는 타입 불일치 시 에러 없이 빈 결과를 반환합니다.

**해결:** `es-mappings/job_postings.json` 매핑 파일에서 검색 대상 필드를 `text` (+ `keyword` sub-field)로 재정의하고, `POST /api/v1/jobs/reindex` 엔드포인트로 100건 전체 재색인했습니다.

---

### 3. PDF 파일 저장 500 에러

**문제:** 이력서 PDF 업로드 시 서버에서 500 에러가 발생했습니다.

**원인:** `file.transferTo(File)` 메서드는 서블릿 컨테이너 임시 디렉토리의 파일을 대상으로 동작하는데, Spring Boot 내장 Tomcat 환경에서 `MultipartFile`이 이미 클리어된 이후 호출되면 `IllegalStateException`이 발생합니다.

**해결:** `Files.write(filePath, file.getBytes())`로 교체해 바이트 배열을 직접 쓰도록 변경했습니다. `getBytes()`는 임시 파일 의존 없이 메모리에서 바로 읽습니다.

---

### 4. Kafka 역직렬화 실패 — notification-service 무한 재시도

**문제:** notification-service가 Kafka 메시지를 소비하지 못하고 `ClassCastException` 또는 `SerializationException`으로 무한 재시도했습니다.

**원인:** Producer(application-service)는 `JsonSerializer`로 직렬화하지만, Consumer(notification-service)는 `JsonDeserializer`를 설정하면서 타입 정보 헤더를 신뢰하도록(`spring.json.trusted.packages`) 설정하지 않아 역직렬화에 실패했습니다.

**해결:** `StringDeserializer + StringJsonMessageConverter` 조합으로 전환했습니다. 메시지를 먼저 `String`으로 수신한 뒤 `ObjectMapper`로 수동 역직렬화하면 타입 헤더 의존성이 사라집니다. 또한 이메일 발송 오류를 `try-catch`로 흡수해 Consumer 리트라이 무한 루프를 방지했습니다.

---

### 5. Claude API 응답 JsonEOFException — max_tokens 부족

**문제:** AI 코칭/추천 응답이 간헐적으로 `JsonProcessingException: Unexpected end-of-input`으로 실패했습니다.

**원인:** `max_tokens: 512`로 설정했을 때 한국어 JSON 응답이 중간에 잘려서 파싱이 실패했습니다. 한국어는 영어 대비 토큰 효율이 낮아 예상보다 더 많은 토큰을 소비합니다.

**해결:** `max_tokens`를 1024로 올리고, 프롬프트에서 출력 항목 수와 각 필드 길이를 명시적으로 제한해 실제 응답 길이를 줄이는 방향으로 최적화했습니다. (예: `"improvements": 2개, 각 20자 이내`)

---

## DB 소유권 (DB per Service)

| 서비스 | 저장소 |
|--------|--------|
| auth-service | Redis (토큰 블랙리스트) |
| user-service | MySQL: `members`, `social_accounts` |
| job-service | MySQL: `companies`, `job_postings` + Elasticsearch |
| application-service | MySQL: `resumes`, `applications`, `ai_feedback` |
| notification-service | 없음 (Kafka 이벤트만 처리) |

---

## 코드 컨벤션

- **클래스**: PascalCase
- **메서드/변수**: camelCase
- **Lombok**: Entity → `@Getter @NoArgsConstructor @Builder`, Service → `@RequiredArgsConstructor`
- **로깅**: `@Slf4j` + `log.info/warn/error`
- **응답 포맷**: `ResultData<T>` (common 모듈)
- **서비스 간 사용자 식별**: `X-User-Id` 헤더 (API Gateway가 JWT에서 추출하여 주입)

---

## 로드맵

- [x] Phase 1~4: 인프라, 인증, 사용자, 채용공고, 프론트엔드 연동
- [x] Phase 5: 지원/이력서 관리, Claude AI 분석, AI 코칭/추천, 속도 최적화
- [x] Phase 6: notification-service Kafka Consumer + 이메일 알림, AI 리뷰 기능, 2차 속도 최적화
- [x] Phase 7: RAG 기반 AI 매칭 고도화 — ES dense_vector + OpenAI 임베딩 + kNN 검색, 3차 속도 최적화
- [ ] Phase 8: 관심 기업 팔로우 + 공고 스크랩 + Kafka 이메일 알림 + 공고 상세 기업 정보/지도 + 채용 트렌드
- [ ] Phase 9: 통합 테스트 및 배포