package io.github.sctf.bootstrap;

import org.springframework.boot.test.context.SpringBootTestContextBootstrapper;
import org.springframework.test.context.ContextLoader;

/**
 * {@link io.github.sctf.annotation.TargetComponentTest @TargetComponentTest}가 사용하는 테스트 컨텍스트 부트스트래퍼.
 *
 * <p>{@link SpringBootTestContextBootstrapper}를 확장하여 기본 {@link ContextLoader} 대신
 * {@link SelectiveContextLoader}를 사용하도록 교체한다.</p>
 *
 * @see SelectiveContextLoader
 * @see io.github.sctf.annotation.TargetComponentTest
 */
public class SelectiveContextBootstrapper extends SpringBootTestContextBootstrapper {

    /**
     * 기본 ContextLoader를 {@link SelectiveContextLoader}로 교체한다.
     *
     * @param testClass 테스트 클래스
     * @return {@link SelectiveContextLoader} 클래스
     */
    @Override
    protected Class<? extends ContextLoader> getDefaultContextLoaderClass(Class<?> testClass) {
        return SelectiveContextLoader.class;
    }
}
