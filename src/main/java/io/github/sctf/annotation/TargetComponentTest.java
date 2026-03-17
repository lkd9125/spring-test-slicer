package io.github.sctf.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.test.context.BootstrapWith;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import io.github.sctf.bootstrap.SelectiveContextBootstrapper;

/**
 * 선택적 컨텍스트 로딩을 활성화하는 테스트용 메타 어노테이션.
 *
 * <p>{@link org.junit.jupiter.api.extension.ExtendWith @ExtendWith(SpringExtension.class)}와
 * {@link org.springframework.test.context.BootstrapWith @BootstrapWith(SelectiveContextBootstrapper.class)}를
 * 조합하여, {@link TargetComponent}에 지정된 클래스의 의존성만 로딩하는 경량 통합테스트 환경을 구성한다.</p>
 *
 * <pre>{@code
 * @TargetComponentTest(basePackage = "com.example")
 * @TargetComponent({OrderService.class})
 * class OrderServiceTest { ... }
 * }</pre>
 *
 * @see TargetComponent
 * @see SelectiveContextBootstrapper
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@ExtendWith(SpringExtension.class)
@BootstrapWith(SelectiveContextBootstrapper.class)
public @interface TargetComponentTest {

    /**
     * Spring 자동 설정(DB, Redis 등 인프라) 포함 여부.
     * <p>{@code false}로 설정하면 {@code spring.boot.enableautoconfiguration=false}가 적용되어
     * 모든 자동 설정이 비활성화된다.</p>
     *
     * @return 자동 설정 포함 여부 (기본값: {@code true})
     */
    boolean withDatabase() default true;

    /**
     * 의존성 탐색 및 컴포넌트 스캔 필터링의 기준이 되는 베이스 패키지.
     * <p>이 패키지에 속하는 클래스만 의존성 그래프에 포함되며,
     * 이 패키지 밖의 클래스(외부 라이브러리 등)는 탐색에서 제외된다.</p>
     *
     * @return 베이스 패키지 경로 (예: {@code "com.example"})
     */
    String basePackage();

}
