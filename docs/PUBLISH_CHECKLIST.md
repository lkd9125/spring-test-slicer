# 배포·공개 준비 체크리스트

라이브러리를 **사내 Maven**에만 올릴지, **Maven Central**에 공개할지에 따라 필요 항목이 다릅니다.

### 완료했다고 표기하는 방법

마크다운에서 **체크박스**는 이렇게 바꿉니다.

| 상태 | 마크다운 |
|------|----------|
| 미완료 | `- [ ] 항목 내용` |
| **완료** | `- [x] 항목 내용` (대괄호 안에 소문자 **x**) |

- VS Code / Cursor: 줄 앞의 체크박스를 클릭해도 `[ ]` ↔ `[x]`로 바뀌는 경우가 많음.
- GitHub에서 편집할 때도 동일하게 `x`를 넣으면 렌더링 시 체크된 박스로 보입니다.

---

## 공통 (사내·공개 모두)

### 제품·API

- [x] **목표 사용자·지원 범위 명시** (README 상단 표 **「누가 쓰나요? · 지원 범위」**): 대상, Java 17+, Boot 3.x, JUnit 5, 테스트 전용 권장
- [x] **미구현·예약 API 정리**: `@TargetComponent.includeParents()` 제거됨 (애노테이션은 `value()`만 유지)
- [x] **`withDatabase=false` 동작 문서화** (README **옵션**): `spring.boot.enableautoconfiguration=false` 적용, **전체 auto-config off** 부작용 명시
- [x] **의존성 그래프 한계 문서화** (README **제약/주의사항** 표): 생성자·필드 `@Autowired`, `@Bean`/setter/`basePackage` 밖 제외 등
- [x] **기본 동작 “깜짝 설정” 옵션화**: `@TargetComponentTest.stubSecurityInfrastructure()` 기본 `false`, `true`일 때만 Security exclude·스텁 등록 (README 동일)

### 품질

- [x] **단위·통합 테스트**: `SliceBeanScopeIntegrationTest`(슬라이스에 무관 빈 미포함), `WithDatabaseFalseIntegrationTest` / `WithDatabaseDefaultIntegrationTest`(`withDatabase` 조합·프로퍼티)
- [ ] **`./gradlew test` CI** (GitHub Actions 등) 연결
- [x] **README 예제·복붙**: `ExampleTest`가 README와 동일 패턴(`basePackage = "io.github.sctf"`)으로 `./gradlew test` 통과 확인됨. 소비 프로젝트에서는 `basePackage`만 본인 앱 루트 패키지로 바꾸면 됨

### 빌드·좌표

#### 확정된 Maven 좌표

| 항목 | 값 | 위치 |
|------|-----|------|
| **groupId** | `io.github.lkd9125` | `build.gradle` → `group` |
| **artifactId** | `sctf-test-slice` | `settings.gradle` → `rootProject.name` |
| **version** | 예: `0.1.0` | `build.gradle` → `version` (배포 JAR/POM과 동일) |

소비자 좌표: `io.github.lkd9125:sctf-test-slice:{version}` (README **Maven / Gradle 좌표**와 동일).

#### 릴리즈·버전 관리 — **GitHub 태그** 기준

별도 `CHANGELOG.md` 없이, **태그(및 선택적 GitHub Release)** 로 릴리즈를 구분합니다.

1. **`build.gradle`의 `version`** 을 이번 릴리즈 번호로 올린 뒤 커밋한다.
2. **Git 태그** `v{version}` 을 같은 커밋에 붙인다.  
   - 예: `version = '0.2.0'` → 태그 `v0.2.0`  
   - 태그 이름의 버전과 `build.gradle`의 `version`이 **항상 일치**하도록 할 것.
3. 태그를 원격에 푸시: `git push origin v0.2.0` (또는 `--tags`).
4. (선택) GitHub **Releases**에서 해당 태그로 릴리즈를 만들고, 변경 요약을 본문에 적는다.

#### 체크리스트

- [x] **groupId / artifactId** — `io.github.lkd9125` / `sctf-test-slice` 유지
- [ ] **Semantic Versioning** — 0.x 동안 MINOR에서 API 변경 가능 등, 팀 규칙 한 줄 README 또는 본 문서에 명시
- [ ] **릴리즈마다** `build.gradle` `version` ↔ Git 태그 `v*` 동기화 습관화

### 의존성 선언

#### 결정 요약 (`build.gradle`과 동일)

| 구분 | 내용 |
|------|------|
| **`spring-boot-starter-test`** | **`compileOnly`** — 라이브러리 컴파일에만 사용. **발행 POM에 전이되지 않음** (Mockito·AssertJ 등 중복·버전 꼬임 방지). |
| **이 저장소 테스트** | **`testImplementation`** `spring-boot-starter-test` — `./gradlew test` 런타임에 필요. |
| **`spring-boot-starter`** | **`implementation`** 유지 — 런타임/슬라이스에 필요한 Spring Boot 코어 계열. |

#### 소비자(앱 프로젝트) 쪽

- **Gradle:** `testImplementation 'io.github.sctf:sctf-test-slice:…'` 와 함께, Boot 기본처럼 **`testImplementation 'org.springframework.boot:spring-boot-starter-test'`** 가 있어야 함 (대부분 프로젝트에 이미 있음). README **Maven / Gradle 좌표**에 한 줄 명시됨.
- **Maven:** `sctf-test-slice`를 `<scope>test</scope>` 로 두고, **`spring-boot-starter-test`** 를 테스트 의존성으로 유지.

#### 전이 의존

- starter-test를 POM에 올리지 않아 **불필요한 테스트 스택 전이 최소화**함.
- 향후 공개 시 `./gradlew publishToMavenLocal` 후 생성 POM에서 `spring-boot-starter-test` 가 **의존성 목록에 없는지** 한 번 확인하면 됨.

- [x] **소비자 입장 점검** — `compileOnly` + README/문서 필수 조합 (`testImplementation` starter-test)
- [x] **불필요한 전이 의존 최소화** — 위와 같이 starter-test 비전이

---

## Maven Central (Sonatype OSSRH) 공개 시 추가

### 법적·메타데이터

- [x] **LICENSE** 파일 + POM `<licenses>` — 루트 `LICENSE`에 Apache License 2.0, `publishing.publications.mavenJava.pom.licenses`와 일치
- [x] **SCM** (`<scm>`: GitHub URL, tag 규칙) — `https://github.com/lkd9125/spring-test-slicer`, https/ssh git URL, `tag = 'HEAD'` (릴리즈 시 `v{version}`으로 교체)
- [x] **developers** (`<id>`, `<name>`, `<email>`, `<url>`) — Gradle `pom.developers`에 `id=lkd9125`, `name=Lee, Kyungdo`, GitHub URL
- [x] **groupId 도메인 검증** (`io.github.<계정>` ↔ GitHub 소유 확인). 현재 `group = "io.github.lkd9125"` 이고, GitHub 계정도 `lkd9125` 이므로 규칙 충족

### 아티팩트

- [x] **GPG 서명** (배포 JAR/POM) — GPG 키 생성 후 Gradle `signing` 플러그인으로 `pom`/`jar`/`sources`/`javadoc`에 `.asc` 생성 (`publishToMavenLocal` 확인)
- [x] **sources JAR** (`java.withSourcesJar()`) — `build/libs/*-sources.jar` 생성 확인
- [x] **javadoc JAR** (`java.withJavadocJar()`), Javadoc 경고/실패 처리 — `build/libs/*-javadoc.jar` 생성. 현재 경고는 있으나 빌드 성공
- [x] **POM 필수 필드** 누락 없이 통과 (Central 규칙) — `publishToMavenLocal` 결과 POM에 `name`/`description`/`url`/`licenses`/`developers`/`scm` 존재

### 프로세스

- [ ] Sonatype JIRA 또는 새 포털로 **groupId 등록**
- [ ] 첫 스테이징 → **Close / Release** 플로우 연습
- [ ] 배포 후 **Central 검색 반영**까지 확인 (수 시간 소요될 수 있음)

---

## 빠른 자가 점수

| 항목            | 사내 배포 최소 | Central 권장 |
|-----------------|----------------|--------------|
| README + 예제   | ✅             | ✅           |
| 자동 테스트     | 권장           | ✅ 필수에 가깝다 |
| 변경 요약       | 태그/Release면 충분 | ✅ (CHANGELOG 또는 Release) |
| LICENSE + POM   | 사내 정책 따름 | ✅ 필수      |
| GPG + sources/javadoc | —        | ✅ 필수      |

---

*완료한 항목은 위 표대로 `- [x]`로 표기합니다.*
