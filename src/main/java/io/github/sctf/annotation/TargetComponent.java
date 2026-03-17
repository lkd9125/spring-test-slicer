package io.github.sctf.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 통합테스트에서 로딩할 대상 컴포넌트를 지정하는 어노테이션.
 *
 * <p>{@code value}에 지정된 클래스들을 루트로 하여 의존성 그래프를 재귀 탐색하고,
 * 발견된 클래스들만 Spring ApplicationContext에 빈으로 등록한다.</p>
 *
 * <pre>{@code
 * @TargetComponentTest(basePackage = "com.example")
 * @TargetComponent({OrderService.class, PaymentService.class})
 * class OrderServiceTest { ... }
 * }</pre>
 *
 * @see TargetComponentTest
 * @see io.github.sctf.core.DependencyGraphScanner
 */
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface TargetComponent {

    /**
     * 의존성 탐색의 루트가 되는 타겟 클래스 목록.
     *
     * @return 탐색 시작점이 되는 클래스 배열
     */
    Class<?>[] value();

    /**
     * 부모 클래스의 의존성도 포함할지 여부.
     * <p>현재 미구현 상태이며, 향후 확장을 위해 예약되어 있다.</p>
     *
     * @return 부모 포함 여부 (기본값: {@code true})
     */
    boolean includeParents() default true;

}
