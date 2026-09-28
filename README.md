<img src="docs/images/logo.png" alt="조각조각" height="60" />

## 프로젝트 목표
Gemini 2.0 모델을 사용하여 이력서, JD를 분석 후 부족한 부분을 체크리스트 형식으로 제공하여 취업에 도움 되는 내용을 알려주는 서비스입니다.

**프로젝트 기간:** 2025.05 ~ 2025.10

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
- Spring Scheduler로 이메일 알림 서비스를 구현
- Google Analytics로 사용자 패턴을 기록
- AI는 Gemini 2.0 Flash 모델을 사용
- CI/CD 파이프라인은 Docker, GitHub Actions, EC2로 구축
- 도메인 주소, HTTPS, RDS 등 배포에 필요한 서비스는 AWS를 이용

## ERD
<img width="760" height="587" alt="Image" src="docs/images/erd.png" />

## 화면 구성

<img width="900" alt="랜딩 페이지" src="docs/images/screen-01-landing.png" />

<img width="900" alt="채용공고 등록" src="docs/images/screen-02-add-job.png" />

<img width="900" alt="AI 분석 중" src="docs/images/screen-03-ai-analyzing.png" />

<img width="900" alt="조각 상세 (칸반보드)" src="docs/images/screen-04-kanban.png" />

<img width="900" alt="마이페이지 - 채용공고 목록" src="docs/images/screen-05-mypage.png" />

<img width="900" alt="멈춘 조각 점검 알림 설정" src="docs/images/screen-06-inactivity-modal.png" />

<img width="900" alt="이메일 알림" src="docs/images/screen-07-email-notification.png" />

## 프로젝트 기능 및 설계

### 담당 기능

- **로그인 / 회원가입**
    - OAuth 2.0을 이용하여 구글, 카카오 소셜 로그인 시 회원가입이 함께 진행된다.
    - Access Token은 30분, Refresh Token은 7일 후 만료되고 Access Token 만료 시 Refresh Token으로 재발급한다.
    - Refresh Token을 HttpOnly 쿠키에 저장하여 XSS 공격으로 인한 토큰 탈취를 방지한다.
    - Spring Security Stateless 세션 정책을 적용하여 서버 측 세션을 사용하지 않는다.
    - 회원탈퇴 시 Kakao Admin Key 방식, Google Token Revoke 방식으로 토큰 만료와 무관하게 OAuth 연동을 즉시 해제한다.

- **이메일 알림**
    - 알림 설정이 활성화된 사용자 중 채용공고 To do list를 3일 이상 갱신하지 않은 경우 독려 이메일을 발송한다.
    - 동일 채용공고에 대한 알림은 최대 3회로 제한하고, 마감된 채용공고는 발송 대상에서 자동 제외한다.
    - Spring Scheduler를 통해 매일 오전 8시에 일괄 발송한다.
    - 알림 대상 JD를 회원 단위로 그룹화하여 회원별로 이메일 발송과 알림 이력 저장을 처리한다.
    - 이메일 템플릿에 사용되는 로고, 아이콘 등 정적 이미지는 AWS S3에 업로드된 리소스를 참조한다.

- **JWT와 Redis를 활용한 이중 토큰 전략**
    - Access Token 탈취 위험을 최소화하기 위해 TTL을 짧게 설정하고 Refresh Token으로 재발급하는 이중 토큰 전략을 사용한다.
    - Refresh Token은 인메모리 기반의 Redis에 저장하여 자체 TTL 만료 시 자동 삭제되도록 구현한다.
    - RDS 대신 Redis를 도입하여 로그인, 로그아웃, 재발급마다 발생하는 삽입, 삭제, 검증 쿼리를 메모리 단에서 빠르게 처리해 높은 트래픽 상황에서 RDS 부하를 줄인다.

- **CORS 정책을 통한 안전한 크로스 오리진 API 환경 구축**
    - 프론트엔드는 Vercel, 백엔드는 EC2 서버에 서브도메인을 나누어 배포하여 서로 다른 출처 간 요청을 CORS로 허용하도록 구성한다.
    - Refresh Token을 HttpOnly 쿠키로 주고받기 때문에 인증 정보가 포함된 요청까지 안전하게 허용하도록 설정한다.
    - allowedMethods, allowedHeaders를 실제 사용하는 메서드와 헤더로 명시하고, 로컬/배포 도메인을 화이트리스트로 등록해 허용 범위를 최소화한다.
    - Set-Cookie, Authorization 등 클라이언트가 읽어야 하는 응답 헤더를 exposedHeaders에 명시하여 인증 토큰이 프론트엔드에서 정상적으로 수신되도록 한다.

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

