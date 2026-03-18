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

- [ ] **Semantic Versioning** 규칙 정하기 (0.x = API 변경 가능, 1.0 = 안정 계약)
- [ ] **CHANGELOG.md** (또는 GitHub Releases) — 버전별 변경 요약
- [ ] **groupId / artifactId / version** 최종 확정 (`io.github.sctf:sctf-test-slice`)

### 의존성 선언

- [ ] **소비자 입장 점검**: `spring-boot-starter-test`를 `implementation` 둘지, `compileOnly`+문서 둘지 결정
- [ ] **불필요한 전이 의존 최소화** (공개 시 특히 중요)

---

## 사내/사설 Maven 전용 (Nexus, Artifactory, GitHub Packages 등)

- [ ] 저장소 URL·인증(credentials)·`publish` 태스크 설정
- [ ] 스냅샷 vs 릴리즈 저장소 분리 여부 결정
- [ ] 내부용이면 README에 **“실험/내부 전용”** 한 줄 명시 권장

---

## Maven Central (Sonatype OSSRH) 공개 시 추가

### 법적·메타데이터

- [ ] **LICENSE** 파일 + POM `<licenses>`
- [ ] **SCM** (`<scm>`: GitHub URL, tag 규칙)
- [ ] **developers** (`<name>`, `<email>` 등)
- [ ] **groupId 도메인 검증** (`io.github.<계정>` ↔ GitHub 소유 확인)

### 아티팩트

- [ ] **GPG 서명** (배포 JAR/POM)
- [ ] **sources JAR** (`java.withSourcesJar()`)
- [ ] **javadoc JAR** (`java.withJavadocJar()`), Javadoc 경고/실패 처리
- [ ] **POM 필수 필드** 누락 없이 통과 (Central 규칙)

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
| CHANGELOG       | 선택           | ✅           |
| LICENSE + POM   | 사내 정책 따름 | ✅ 필수      |
| GPG + sources/javadoc | —        | ✅ 필수      |

---

*완료한 항목은 위 표대로 `- [x]`로 표기합니다.*
