# SCTF Test Slice (`sctf-test-slice`)

**원하는 지점(타겟 컴포넌트)부터 의존성 슬라이스만 올려서 통합 테스트를 빠르게 수행**하기 위한 Spring Test 확장 라이브러리입니다.  
`@TargetComponentTest` + `@TargetComponent` 조합으로 “이 지점부터 필요한 빈만” ApplicationContext에 수동 등록합니다.

### Maven / Gradle 좌표

```gradle
implementation 'io.github.sctf:sctf-test-slice:0.1.0'
```

> 로컬 개발 중 스냅샷을 쓰려면 `build.gradle`의 `version`을 예: `0.2.0-SNAPSHOT`으로 바꾼 뒤 `publishToMavenLocal` 하면 됩니다.

**배포·공개 전 점검**은 [`docs/PUBLISH_CHECKLIST.md`](docs/PUBLISH_CHECKLIST.md)를 보면 됩니다.

동작 검증용 테스트: `SliceBeanScopeIntegrationTest`, `WithDatabaseFalseIntegrationTest`, `WithDatabaseDefaultIntegrationTest`, `ExampleTest` (`./gradlew test`).

### 누가 쓰나요? · 지원 범위

| 구분 | 내용 |
|------|------|
| **대상** | Spring Boot 기반 앱을 **JUnit 5**로 통합 테스트하는 개발자 |
| **Java** | **17+** (이 저장소는 17 툴체인으로 빌드) |
| **Spring Boot** | **3.x** 계열을 전제로 함. 라이브러리는 **3.5.11**로 검증·빌드됨. 다른 3.x는 동작할 가능성이 높으나, 사용 전 해당 버전에서 한 번 확인하는 것을 권장 |
| **테스트 API** | **JUnit Jupiter** (`@Test` 등). `@TargetComponentTest`가 `@ExtendWith(SpringExtension.class)`를 포함 |
| **사용 위치** | **테스트 소스**(`src/test`)에서만 의존성 추가·애노테이션 사용을 권장 (앱 런타임에는 넣지 않음) |

---

## 핵심 아이디어

- **일반 `@SpringBootTest`**: 애플리케이션 전체 컨텍스트를 올리며 느릴 수 있음
- **SCTF Test Slice**: 테스트가 시작되는 “지점”을 지정하고, 그 지점의 **의존성 그래프만** 컨텍스트에 등록

즉, “특정 서비스/컴포넌트부터”의 통합 테스트를 **슬라이스 단위**로 수행합니다.

---

## 사용 방법

### 1) 테스트 클래스에 `@TargetComponentTest` 붙이기

`@TargetComponentTest`는 Spring Test 부트스트랩을 이 프레임워크로 전환합니다.

- 파일: `src/main/java/io/github/sctf/annotation/TargetComponentTest.java`

### 2) 테스트 시작 지점을 `@TargetComponent(...)`로 지정하기

`@TargetComponent`의 `value()`에 **원하는 시작 지점(루트 컴포넌트/서비스)**를 지정합니다.

- 파일: `src/main/java/io/github/sctf/annotation/TargetComponent.java`

예시(프로젝트의 `ExampleTest` 형태):

```java
@TargetComponentTest(basePackage = "io.github.sctf") // 앱 코드가 있는 패키지 루트
@TargetComponent({ExampleService.class})
class ExampleTest {
    @Autowired ExampleService exampleService;
}
```

---

## 옵션

### `@TargetComponentTest`

- **`basePackage()`** (필수): 의존성 그래프·스캔 필터 기준 패키지. 이 접두사 아래의 `@Component` 계열만 슬라이스 후보로 탐색합니다. (예: `"com.myapp"`)
- **`withDatabase()`** (기본 `true`): `false`이면 환경에  
  **`spring.boot.enableautoconfiguration=false`** 를 넣어 **Spring Boot 자동 설정 전체를 끕니다.**  
  - 이름은 “DB”지만 실제로는 **DataSource뿐 아니라** Redis, JPA, Security auto-config 등 **인프라 자동 설정이 한꺼번에 비활성화**됩니다.  
  - DB만 끄고 나머지는 두고 싶다면 이 플래그 대신 테스트용 `@SpringBootApplication(exclude = …)` 등을 따로 쓰는 편이 맞습니다.
- **`stubSecurityInfrastructure()`** (기본 `false`): `true`일 때만  
  Security 관련 auto-config를 `spring.autoconfigure.exclude`로 제외하고, `AuthenticationManager` 등에 JDK Proxy 스텁 빈을 등록합니다.  
  **기본값은 끔** — Security를 건드리지 않는 일반 슬라이스 테스트에는 아무 설정도 넣지 않습니다.

### `@TargetComponent`

- **`value()`**: (필수) 슬라이스의 루트가 되는 클래스(들)

---

## 동작 원리(요약)

- `@TargetComponentTest`
  - `@BootstrapWith(SelectiveContextBootstrapper.class)`로 부트스트랩 교체
- `spring.factories`
  - `ContextCustomizerFactory`를 등록해 테스트 컨텍스트 생성 시점에 개입
- `SelectiveContextCustomizerFactory`
  - 테스트 클래스에서 `@TargetComponent`를 읽고
  - `DependencyGraphScanner`로 의존성(생성자/필드 `@Autowired`)을 재귀 탐색한 뒤
  - 모은 클래스들을 `SelectiveContextCustomizer`로 전달
- `SelectiveContextCustomizer`
  - 클래스 집합을 `BeanDefinition`으로 변환 후 `BeanDefinitionRegistry`에 **수동 등록**
  - 컨텍스트 캐싱을 위해 해시 키 기반 `equals/hashCode` 사용

등록 정보:

- `src/main/resources/META-INF/spring.factories`

---

## 제약/주의사항 (의존성 그래프 한계)

슬라이스에 **어떤 빈이 들어갈지**는 `DependencyGraphScanner`가 **리플렉션으로 따라가는 경로**로만 결정됩니다.

| 포함되는 경우 | 포함되지 않는 경우(대표) |
|----------------|-------------------------|
| 타겟부터 **생성자 파라미터 타입**으로 이어지는 `@Component` 계열 | **setter** `@Autowired` |
| 타겟/하위 빈의 **필드** `@Autowired` 타입 | `@Configuration`의 **`@Bean` 메서드**로만 제공되는 빈 |
| `@Service` / `@Component` 등 **메타 `@Component`** 타입 | 인터페이스만 두고 구현체가 그래프에 안 잡힌 경우 |
| `basePackage` 아래에 있는 클래스 | `basePackage` 밖 타입(라이브러리 내부 구현 등)은 탐색 제외 |

- **JDK Proxy 스텁**(`stubSecurityInfrastructure=true` 시): 인터페이스만 프록시 가능, 메서드는 기본 `null` 반환.

---

## 요구 사항 (빌드 이 저장소만 할 때)

- **Java**: 17
- **Spring Boot Gradle Plugin**: `3.5.11`

소비 프로젝트의 Boot 버전은 위 **「지원 범위」** 표를 참고하면 됩니다.

---

## 실행

테스트 실행:

```bash
./gradlew test
```

빌드(라이브러리 JAR):

```bash
./gradlew build
```

---

## 프로젝트 구조(요약)

- `src/main/java/io/github/sctf/annotation/`
  - `TargetComponent.java`: 슬라이스 시작 지점 지정
  - `TargetComponentTest.java`: 테스트 부트스트랩 전환
- `src/main/java/io/github/sctf/core/`
  - `DependencyGraphScanner`: 의존성 그래프 탐색
  - `SelectiveContextCustomizerFactory`: 테스트 애노테이션 파싱/슬라이스 구성
  - `SelectiveContextCustomizer`: 슬라이스 빈 수동 등록
- `src/main/resources/META-INF/spring.factories`
  - Spring Test 확장 등록

