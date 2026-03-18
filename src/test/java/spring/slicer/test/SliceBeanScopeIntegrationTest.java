package spring.slicer.test;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;

import io.github.sctf.annotation.TargetComponent;
import io.github.sctf.annotation.TargetComponentTest;
import io.github.sctf.example.ExampleComponent;
import io.github.sctf.example.ExampleService;
import io.github.sctf.example.ExampleStandaloneComponent;

/**
 * 슬라이스에 루트·전이 의존만 올라가고, 동일 패키지의 무관 컴포넌트는 제외되는지 검증한다.
 */
@TargetComponentTest(basePackage = "io.github.sctf")
@TargetComponent({ExampleService.class})
class SliceBeanScopeIntegrationTest {

    @Autowired
    private ApplicationContext applicationContext;

    @Test
    void sliceContainsServiceAndItsDependencyOnly() {
        assertThat(applicationContext.getBeansOfType(ExampleService.class)).hasSize(1);
        assertThat(applicationContext.getBeansOfType(ExampleComponent.class)).hasSize(1);
        assertThat(applicationContext.getBeansOfType(ExampleStandaloneComponent.class)).isEmpty();
    }
}
