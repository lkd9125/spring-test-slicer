package io.github.sctf.bootstrap;

import org.springframework.boot.test.context.SpringBootTestContextBootstrapper;
import org.springframework.test.context.ContextLoader;

public class SelectiveContextBootstrapper extends SpringBootTestContextBootstrapper {

    /**
     * 기본 ContextLoader 대신, 우리가 나중에 만들 SelectiveContextLoader를 사용하도록 교체합니다.
     */
    @Override
    protected Class<? extends ContextLoader> getDefaultContextLoaderClass(Class<?> testClass) {
        return SelectiveContextLoader.class; // (아직 안 만들었으니 빨간불이 나도 일단 둡니다!)
    }
}
