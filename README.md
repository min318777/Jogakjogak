# 조각조각                        
## 프로젝트 목표
Gemini 2.0 모델을 사용하여 이력서, JD를 분석 후 부족한 부분을 체크리스트 형식으로 제공하여 취업에 도움 되는 내용을 알려주는 서비스입니다.

**프로젝트 기간:** 2025.06 ~ 2025.10

**참여 인원:** PM 2명, 디자이너 2명, FE 2명, BE 3명

## 기술 스택

**Backend**
- Java Spring Boot, Spring JPA, Spring Security, Spring Batch
- JWT, OAuth 2.0, QueryDSL
- Java Mail Sender, Swagger

**Database**
- MySQL, Redis, RDS

**Infrastructure**
- Docker, EC2, Route 53, S3

## 아키텍처 설계
- Java Spring Boot로 백엔드 구현
- Spring Security 프레임워크 기반으로 구현하며 카카오, 구글 소셜 로그인 연동을 위해 OAuth 2.0을 사용
- RESTful API로 설계
- DB는 오픈소스이면서 빠르고 범용성이 좋은 MySQL을 사용
- Spring Batch로 이메일 알림 서비스를 구현
- Google Analytics로 사용자 패턴을 기록
- AI는 Gemini 2.0 Flash 모델을 사용
- CI/CD 파이프라인은 Docker, GitHub Actions, EC2로 구축
- 도메인 주소, HTTPS, RDS 등 배포에 필요한 서비스는 AWS를 이용

## ERD
<img width="760" height="587" alt="Image" src="https://github.com/user-attachments/assets/7db7c6d4-0086-42b8-bbf4-5d929440100c" />

## 프로젝트 기능 및 설계

### 담당 기능

- 로그인 / 회원가입
    - OAuth 2.0을 이용하여 구글, 카카오 소셜 로그인 시 회원가입이 함께 진행된다.
    - Access Token은 30분, Refresh Token은 7일 후 만료되고 Access Token 만료 시 Refresh Token으로 재발급한다.
    - Refresh Token을 HttpOnly 쿠키에 저장하여 XSS 공격으로 인한 토큰 탈취를 방지한다.
    - Spring Security Stateless 세션 정책을 적용하여 서버 측 세션을 사용하지 않는다.
    - 회원탈퇴 시 Kakao Admin Key 방식, Google Token Revoke 방식으로 토큰 만료와 무관하게 OAuth 연동을 즉시 해제한다.

- 이메일 알림 배치
    - 알림 설정이 활성화된 사용자 중 채용공고 To do list를 3일 이상 갱신하지 않은 경우 독려 이메일을 발송한다.
    - 동일 채용공고에 대한 알림은 최대 3회로 제한하고, 마감된 채용공고는 발송 대상에서 자동 제외한다.
    - Spring Batch 스케줄러를 통해 매일 오전 10시에 일괄 발송한다.
    - Spring Batch Two-Step 구조로 구성하여 1단계에서 알림 대상 JD 처리, 2단계에서 이메일 발송을 분리한다.
    - 이메일 템플릿에 사용되는 로고, 아이콘 등 정적 이미지는 AWS S3에 업로드된 리소스를 참조한다.

### 성능 개선 및 기술 선택 근거

**1. Spring Batch 이메일 알림 배치 최적화**

Reader/Writer 조합을 변경하여 대용량 데이터 처리 성능을 개선했습니다.

| 구성 | 10만 건 | 50만 건 |
|------|---------|---------|
| JpaPagingItemReader + JpaItemWriter (개선 전) | 2m 53s | 41m 5s |
| JdbcCursorItemReader + JdbcBatchItemWriter (개선 후) | 22s | 1m 50s |

**Reader: JpaPagingItemReader → JdbcCursorItemReader**

| 항목 | 내용 |
|------|------|
| 선택 근거 | JpaPaging은 페이지마다 OFFSET 쿼리를 재실행하여 데이터가 많을수록 비용이 급증. JdbcCursor는 최초 쿼리 실행 후 커서를 고정하고 순차 스캔하므로 OFFSET 비용 없음. MySQL에서 `TYPE_FORWARD_ONLY` + `fetchSize` 설정으로 서버 메모리 부담 없이 스트리밍 처리 가능 |
| 트레이드오프 | JPQL 대신 SQL을 직접 작성해야 하므로 ORM 추상화를 포기. 컬럼명, 조인 조건을 수동 관리해야 하며 엔티티 변경 시 SQL도 함께 수정 필요 |

**Writer: JpaItemWriter → JdbcBatchItemWriter**

| 항목 | 내용 |
|------|------|
| 선택 근거 | JpaItemWriter는 청크 내 아이템마다 `em.merge()`를 호출하고 청크 끝에 flush. JdbcBatchItemWriter는 `executeBatch()`로 청크 전체를 단일 네트워크 요청으로 처리. `rewriteBatchedStatements=true` 옵션으로 다건 INSERT/UPDATE를 단일 쿼리로 병합하여 DB 왕복 횟수를 최소화 |
| 트레이드오프 | 영속성 컨텍스트를 사용하지 않으므로 1차 캐시, 더티체킹, 지연 로딩 등 JPA 기능을 사용할 수 없음. 연관 엔티티가 복잡한 경우 직접 관리 필요 |

**2. Spring Batch Two-Step 구조 선택**

| 항목 | 내용 |
|------|------|
| 선택 근거 | Step1(알림 대상 JD 조회 → Notification 저장)과 Step2(이메일 발송 → 상태 업데이트)를 분리하여 Step1 실패 시 Step2를 실행하지 않도록 제어. 발송 실패 건만 재시도(Retry 3회) 및 Skip(최대 50건)이 가능하고 DEAD_LETTER 상태로 관리 |
| 트레이드오프 | Notification 중간 테이블이 추가되어 스키마 복잡도 증가. 배치 실행마다 PENDING 레코드가 생성되므로 주기적인 정리 정책 필요 |

**3. JpaPagingItemReader OFFSET 문제 — 데이터 누락**

- **문제:** Step2에서 PENDING 상태의 Notification을 JpaPagingItemReader로 읽으며 이메일 발송 후 SENT로 업데이트하면, 다음 페이지 OFFSET이 밀려 일부 데이터 누락
- **원인:** 청크 처리 후 상태가 PENDING → SENT로 변경되면 전체 결과셋 크기가 줄어들어 OFFSET 계산이 어긋남
- **해결:** JdbcCursorItemReader로 교체하여 최초 쿼리 실행 시 커서를 고정, 이후 상태 변경과 무관하게 순차적으로 읽도록 변경

**4. Redis 기반 인증 보안 강화**

**RefreshToken 저장소: RDB → Redis**

| 항목 | 내용 |
|------|------|
| 선택 근거 | RDB 저장 시 토큰 재발급마다 DB I/O 발생. Redis는 인메모리 구조로 조회 속도가 빠르며 TTL 설정으로 만료 토큰을 자동 삭제. `refresh:<userId>` 키 구조로 사용자당 하나의 토큰만 유지하여 토큰 탈취 감지(재발급 시 저장된 값과 불일치하면 탈취로 판단, Redis 즉시 삭제) |
| 트레이드오프 | Redis 장애 시 로그인된 사용자도 재발급 불가. 인프라 의존성 추가로 운영 복잡도 증가 |

**AccessToken 블랙리스트**

| 항목 | 내용 |
|------|------|
| 선택 근거 | JWT는 stateless 특성상 발급 후 서버에서 무효화 불가. 로그아웃 시 AccessToken의 JTI(JWT ID)를 Redis에 저장하고 요청마다 블랙리스트 확인하여 즉시 무효화 구현. TTL을 AccessToken 잔여 만료 시간으로 설정하여 불필요한 키 자동 삭제 |
| 트레이드오프 | 모든 API 요청마다 Redis 조회 1회 추가. 토큰 유효 시간(30분)이 짧아 실질적 영향은 제한적이나, Redis 장애 시 블랙리스트 확인 불가 |

### 팀원 구현 기능

- 이력서
    - 로그인한 사용자는 제목, 내용을 입력하여 이력서를 등록할 수 있다.
    - 이력서 내용은 5000자 이하여야 한다.
    - 로그인한 사용자는 하나의 이력서만 등록할 수 있다.
    - 로그인한 사용자는 이력서 수정, 조회, 삭제가 가능하다.

- 채용공고
    - 로그인한 사용자는 이력서 등록 후 채용공고의 제목, URL, 회사명, 직무, 내용, 마감일을 입력하여 이력서와 채용공고를 비교한 보완 todolist를 분석받을 수 있다.
    - 채용공고 내용은 최소 300자 이상이어야 한다.
    - 로그인한 사용자는 특정 채용공고 분석 내용 조회, 전체 리스트 조회, 즐겨찾기 등록, 지원 완료, 메모 수정을 할 수 있다.
    - 채용공고 분석은 최대 20개까지 가능하다.

- To do list
    - 채용공고 분석 시 Gemini 2.0을 이용하여 분석하고 해요체로 todolist를 작성한다.
    - todolist 타입은 구조적 보완 계획, 내용 강조 및 재구성 제안, 일정 관리 및 기타 3가지로 분류된다.
    - 각 타입별 todolist는 10개를 초과할 수 없다.
    - 로그인한 사용자는 todolist를 추가, 수정, 삭제할 수 있다.


> 원본 레포지토리: [mallangC/Jogakjogak](https://github.com/mallangC/Jogakjogak)


