# SCTF Test Slice (`sctf-test-slice`)

**원하는 지점(타겟 컴포넌트)부터 의존성 슬라이스만 올려서 통합 테스트를 빠르게 수행**하기 위한 Spring Test 확장 라이브러리입니다.  
`@TargetComponentTest` + `@TargetComponent` 조합으로 “이 지점부터 필요한 빈만” ApplicationContext에 수동 등록합니다.

---

## 목차

1. [지원 범위](#지원-범위)
2. [의존성 추가](#의존성-추가)
3. [빠른 시작 예제](#빠른-시작-예제)
4. [주요 옵션](#주요-옵션)
5. [제약·주의사항](#제약주의사항)
6. [빌드·실행](#빌드실행)
7. [라이선스](#라이선스)

---

## 지원 범위

| 구분 | 내용 |
|------|------|
| **대상** | Spring Boot 기반 앱을 **JUnit 5**로 통합 테스트하는 개발자 |
| **Java** | **17+** (이 저장소는 17 툴체인으로 빌드) |
| **Spring Boot** | **3.x** 계열을 전제로 함. 라이브러리는 **3.5.11**로 검증·빌드됨. 다른 3.x는 동작할 가능성이 높으나, 사용 전 해당 버전에서 한 번 확인하는 것을 권장 |
| **테스트 API** | **JUnit Jupiter** (`@Test` 등). `@TargetComponentTest`가 `@ExtendWith(SpringExtension.class)`를 포함 |
| **사용 위치** | **테스트 소스**(`src/test`)에서만 의존성 추가·애노테이션 사용 권장 (앱 런타임에는 넣지 않음) |

---

## 의존성 추가

```gradle
testImplementation 'io.github.lkd9125:sctf-test-slice:0.1.0'
// Boot 프로젝트에 보통 이미 있음 — 없으면 추가
testImplementation 'org.springframework.boot:spring-boot-starter-test'
```

- 라이브러리는 `spring-boot-starter-test`를 **전이하지 않습니다.**  
  소비 프로젝트에서 위처럼 `starter-test`를 직접 두는 것이 일반적입니다.
- 로컬 개발 중 스냅샷을 쓰려면 `build.gradle`의 `version`을 예: `0.2.0-SNAPSHOT`으로 바꾼 뒤 `publishToMavenLocal` 을 실행하면 됩니다.

**좌표·릴리즈 정책**

- **groupId**: `io.github.lkd9125`  
- **artifactId**: `sctf-test-slice`  
- 공식 릴리즈는 **`build.gradle`의 `version`** 과 같은 번호의 **GitHub 태그 `v{version}`** (예: `v0.1.0`)으로 구분합니다.  
  자세한 체크리스트는 [`docs/PUBLISH_CHECKLIST.md`](docs/PUBLISH_CHECKLIST.md)의 **빌드·좌표** 절을 참고하세요.

---

## 빠른 시작 예제

1. **테스트 클래스에 `@TargetComponentTest` 붙이기**
   - `@TargetComponentTest`는 Spring Test 부트스트랩을 이 라이브러리로 전환합니다.

2. **테스트 시작 지점을 `@TargetComponent(...)`로 지정하기**
   - `@TargetComponent`의 `value()`에 **루트 컴포넌트/서비스**를 지정합니다.

예시 (이 저장소의 `ExampleTest`):

```java
@TargetComponentTest(basePackage = "io.github.sctf") // 앱 코드가 있는 패키지 루트
@TargetComponent({ExampleService.class})
class ExampleTest {
    @Autowired ExampleService exampleService;
}
```

이렇게 하면 `ExampleService`부터 시작해 **의존성 그래프에 따라 필요한 빈만** ApplicationContext에 등록하여 통합 테스트를 수행합니다.

---

## 주요 옵션

### `@TargetComponentTest`

- **`basePackage()`** (필수):  
  의존성 그래프·스캔 필터 기준 패키지입니다. 이 접두사 아래의 `@Component` 계열만 슬라이스 후보로 탐색합니다. (예: `"com.myapp"`)

- **`withDatabase()`** (기본 `true`):  
  `false`이면 환경에 `spring.boot.enableautoconfiguration=false` 를 넣어 **Spring Boot 자동 설정 전체를 끕니다.**  
  - 이름은 “DB”지만 실제로는 DataSource뿐 아니라 Redis, JPA, Security auto-config 등 인프라 자동 설정이 한꺼번에 비활성화됩니다.  
  - DB만 끄고 나머지는 두고 싶다면 이 플래그 대신 테스트용 `@SpringBootApplication(exclude = …)` 등을 별도로 사용하는 편이 좋습니다.

- **`stubSecurityInfrastructure()`** (기본 `false`):  
  `true`일 때만 Security 관련 auto-config를 `spring.autoconfigure.exclude`로 제외하고,  
  `AuthenticationManager` 등에 JDK Proxy 기반 스텁 빈을 등록합니다.  
  기본값은 `false`이며, Security를 건드리지 않는 일반 슬라이스 테스트에는 아무 설정도 추가되지 않습니다.

### `@TargetComponent`

- **`value()`** (필수):  
  슬라이스의 루트가 되는 클래스(들)를 지정합니다.

---

## 제약·주의사항

슬라이스에 **어떤 빈이 포함되는지**는 `DependencyGraphScanner`가 **리플렉션으로 따라가는 경로**로만 결정됩니다.

| 포함되는 경우 | 포함되지 않는 경우(대표) |
|----------------|-------------------------|
| 타겟부터 **생성자 파라미터 타입**으로 이어지는 `@Component` 계열 | **setter** `@Autowired` |
| 타겟/하위 빈의 **필드** `@Autowired` 타입 | `@Configuration`의 **`@Bean` 메서드**로만 제공되는 빈 |
| `@Service` / `@Component` 등 **메타 `@Component`** 타입 | 인터페이스만 두고 구현체가 그래프에 안 잡힌 경우 |
| `basePackage` 아래에 있는 클래스 | `basePackage` 밖 타입(라이브러리 내부 구현 등)은 탐색 제외 |

- `stubSecurityInfrastructure = true` 인 경우 등록되는 **JDK Proxy 스텁**은 인터페이스만 프록시 가능하며, 메서드는 기본적으로 `null`을 반환합니다.

---

## 빌드·실행

**요구 사항 (이 저장소 빌드 기준)**:

- **Java**: 17
- **Spring Boot Gradle Plugin**: `3.5.11`

테스트 실행:

```bash
./gradlew test
```

라이브러리 JAR 빌드:

```bash
./gradlew build
```

---

## 라이선스

이 프로젝트는 **Apache License 2.0** 을 따릅니다.  
자세한 내용은 루트의 `LICENSE` 파일을 참고하세요.