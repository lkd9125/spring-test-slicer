# SCTF Test Slice (`sctf-test-slice`)

> 🚀 **무거운 스프링 통합 테스트는 이제 그만!** > 원하는 컴포넌트(Service, Controller 등)를 타겟으로 지정하면, **해당 컴포넌트와 엮인 의존성만 쏙쏙 골라서 ApplicationContext에 올려주는 초고속 슬라이스 테스트 라이브러리**입니다.

---

## 목차

1. [지원 범위](#1-지원-범위)
2. [의존성 추가](#2-의존성-추가)
3. [빠른 시작 예제](#3-빠른-시작-예제)
4. [주요 옵션](#4-주요-옵션)
5. [제약 및 주의사항 (필독)](#5-제약-및-주의사항-필독)
6. [빌드·실행](#6-빌드실행)
7. [라이선스](#7-라이선스)

---

## 1. 지원 범위

| 구분 | 내용 |
|------|------|
| **대상** | Spring Boot 기반 앱을 **JUnit 5**로 통합 테스트하는 개발자 |
| **Java** | **17 이상** (본 저장소는 17 툴체인으로 빌드됨) |
| **Spring Boot** | **3.x 계열** (3.5.11 버전으로 검증 및 빌드됨. 타 3.x 버전도 호환 가능) |
| **테스트 환경** | **JUnit Jupiter** (내부적으로 `@ExtendWith(SpringExtension.class)` 포함) |
| **사용 위치** | `src/test` 디렉토리 하위의 테스트 코드에서만 사용 권장 |

---

## 2. 의존성 추가

`build.gradle`의 `dependencies` 블록에 아래 내용을 추가합니다.

```gradle
// SCTF Test Slice 라이브러리 추가
testImplementation 'io.github.lkd9125:sctf-test-slice:0.1.0'

// Spring Boot 테스트 스타터 (보통 이미 존재함)
testImplementation 'org.springframework.boot:spring-boot-starter-test'
```

* 💡 **참고:** 이 라이브러리는 `spring-boot-starter-test`를 전이(Transit)하지 않으므로, 소비하는 프로젝트에서 직접 의존성을 가지고 있어야 합니다.
* 🛠 **로컬 테스트용:** 스냅샷 버전을 로컬에서 테스트하려면 `version`을 `0.2.0-SNAPSHOT` 등으로 변경 후 `./gradlew publishToMavenLocal`을 실행하여 사용하세요.

---

## 3. 빠른 시작 예제

테스트하고 싶은 루트 클래스를 `@TargetComponent`로 지정하기만 하면, 프레임워크가 알아서 연관된 의존성만 찾아 빈으로 등록합니다.

```java
import io.github.sctf.annotation.TargetComponent;
import io.github.sctf.annotation.TargetComponentTest;
import org.springframework.context.annotation.Import;

// 1. 스캔할 기준 패키지 지정
@TargetComponentTest(basePackage = "com.myapp")
// 2. 테스트의 시작점이 되는 타겟 클래스 지정
@TargetComponent(OrderService.class)
// 3. 🚨 필수: 스프링 자동설정이 아닌, 내가 직접 만든 Config는 반드시 수동 Import!
@Import({JpaQueryDSLConfig.class, WebClientConfig.class})
@ActiveProfiles("test")
class OrderServiceTest {

    @Autowired
    OrderService orderService; // OrderService와 연관된 빈들만 깔끔하게 로드됩니다!

    @Test
    void testOrderCreation() {
        // ... 테스트 로직 ...
    }
}
```

---

## 4. 주요 옵션

### 🎯 `@TargetComponentTest`
테스트 클래스에 선언하여 SCTF 슬라이스 테스트 환경을 구동합니다.

* **`basePackage` (필수)**
  * 의존성을 추적할 **기준 패키지**입니다. (예: `"com.myapp"`)
  * 이 패키지 하위에 있는 클래스들만 추적하여 빈으로 등록합니다.
* **`withDatabase` (기본값: `true`)**
  * `false`로 설정 시, `spring.boot.enableautoconfiguration=false`가 적용되어 **Spring Boot의 모든 자동 설정(DB, Redis, JPA 등)이 꺼집니다.**
  * DB만 끄고 싶다면 이 옵션보다는 테스트 클래스에 `@SpringBootTest(exclude = {...})` 등을 별도로 사용하는 것을 권장합니다.
* **`stubSecurityInfrastructure` (기본값: `false`)**
  * `true`로 설정 시, Spring Security 관련 자동 설정을 제외하고 `AuthenticationManager` 같은 필수 Security 빈들을 **가짜(Stub) 객체로 자동 등록**해 줍니다. Security 로직 검증이 필요 없는 서비스 슬라이스 테스트에 유용합니다.

### 🎯 `@TargetComponent`
* **`value` (필수)**
  * 슬라이스의 **루트(시작점)**가 되는 클래스를 지정합니다. 배열 형태로 여러 개 지정할 수 있습니다. (예: `@TargetComponent({OrderService.class, PaymentService.class})`)

---

## 5. 제약 및 주의사항 (필독)

이 라이브러리는 리플렉션(Reflection)을 통해 클래스의 생성자와 필드를 쫓아가며 의존성을 파악합니다. 따라서 아래의 주의사항을 반드시 숙지해야 합니다.

### 🚨 1. 내가 만든 `@Configuration` 클래스는 자동으로 로드되지 않습니다!
SCTF 스캐너는 서비스나 컴포넌트 간의 의존성은 잘 찾아내지만, `@Bean` 메서드를 통해 설정된 환경 클래스는 추적하지 못합니다.
* **QueryDSL 설정 (`JpaQueryDSLConfig`)**
* **외부 API 통신 설정 (`WebClientConfig`, `ApiConfig`)**
* **기타 내가 만든 커스텀 빈 설정 클래스들**

> **👉 해결책:** 테스트 클래스 상단에 반드시 `@Import({내가만든설정.class})`를 추가하여 수동으로 컨텍스트에 올려주어야 합니다!

### 🚨 2. 의존성 탐색 규칙 (포함 O vs 포함 X)

| ✅ 이런 빈은 자동으로 포함됩니다 | ❌ 이런 빈은 무시됩니다 (수동 처리 필요) |
|----------------|-------------------------|
| 타겟 컴포넌트의 **생성자**에 주입된 타입 | **Setter**로 주입되는 `@Autowired` 타입 |
| 타겟 컴포넌트의 **필드**에 주입된 `@Autowired` 타입 | `@Configuration` 안의 **`@Bean` 메서드**로만 등록되는 빈 |
| `basePackage` 하위에 있는 `@Component`, `@Service` 등 | `basePackage` 범위 밖의 외부 라이브러리 클래스 |
| - | 순수 인터페이스만 있고 구현체가 스캔 범위에 없는 경우 (이 경우 `@MockBean` 사용 권장) |

### 🚨 3. Security Stub 옵션의 한계
`stubSecurityInfrastructure = true` 옵션으로 띄워지는 JDK Proxy 스텁 빈은 **모든 메서드 호출에 대해 `null`을 반환**하는 깡통 객체입니다. 따라서 해당 객체의 반환값을 직접 조작하거나 검증해야 하는 경우, 이 옵션을 끄고 Mockito(`@MockBean`)를 직접 사용하는 것이 좋습니다.

---

## 6. 빌드·실행

**요구 사항**
* **Java**: 17 이상
* **Spring Boot Gradle Plugin**: `3.5.11` 이상 권장

**테스트 실행**
```bash
./gradlew test
```

**라이브러리 JAR 빌드**
```bash
./gradlew build
```

---

## 7. 라이선스

이 프로젝트는 **Apache License 2.0**을 따릅니다.
자세한 내용은 프로젝트 루트의 `LICENSE` 파일을 참고하세요.