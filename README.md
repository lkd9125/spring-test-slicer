# spring-test-slicer

**원하는 지점(타겟 컴포넌트)부터 의존성 슬라이스만 올려서 통합 테스트를 빠르게 수행**하기 위한 Spring Test 확장 프로젝트입니다.  
`@TargetComponentTest` + `@TargetComponent` 조합으로 “이 지점부터 필요한 빈만” ApplicationContext에 수동 등록합니다.

---

## 핵심 아이디어

- **일반 `@SpringBootTest`**: 애플리케이션 전체 컨텍스트를 올리며 느릴 수 있음
- **spring-test-slicer**: 테스트가 시작되는 “지점”을 지정하고, 그 지점의 **의존성 그래프만** 컨텍스트에 등록

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
@TargetComponentTest
@TargetComponent({ExampleService.class})
class ExampleTest {
    @Autowired ExampleService exampleService;
}
```

---

## 옵션

### `@TargetComponentTest`

- **`withDatabase()`**: 기본값 `true`  
  - `false`로 두면 “DB 등 자동 설정”을 끄는 방향으로 동작합니다. (현재는 속성 기반으로 auto-config 비활성화를 시도)
- **`exclude()`**: 기본값 `{}`  
  - 슬라이스에서 제외할 클래스를 지정합니다. (의존성 스캔 결과에서 제외 처리)

### `@TargetComponent`

- **`value()`**: (필수) 슬라이스의 루트가 되는 클래스(들)
- **`includeParents()`**: 기본값 `true`  
  - 현재 코드에서는 **미사용(예약 옵션)** 입니다.

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

## 제약/주의사항

- **의존성 탐색 범위**: 생성자 주입, 필드 `@Autowired`만 추적합니다. (setter 주입, `@Bean` 메서드 기반 빈 등은 포함되지 않을 수 있음)
- **대상 제한**: `@Component` 계열로 인식되는 타입만 슬라이스 후보로 취급합니다.
- **JDK Proxy 더미 빈**: 인터페이스만 프록시 가능하며(구체 클래스 불가), 메서드는 기본적으로 `null`을 반환합니다.

---

## 요구 사항

- **Java**: 17
- **Spring Boot**: `3.5.11` (Gradle 플러그인 기준)

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

