package spring.slicer.test;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;

import io.github.sctf.annotation.TargetComponent;
import io.github.sctf.annotation.TargetComponentTest;
import io.github.sctf.example.ExampleService;

/**
 * 기본 {@code withDatabase = true} 일 때 전체 auto-config 끔 프로퍼티가 적용되지 않는지 검증한다.
 */
@TargetComponentTest(basePackage = "io.github.sctf")
@TargetComponent({ExampleService.class})
class WithDatabaseDefaultIntegrationTest {

    @Autowired
    private Environment environment;

    @Autowired
    private ExampleService exampleService;

    @Test
    void doesNotForceDisableAllAutoConfiguration() {
        assertThat(environment.getProperty("spring.boot.enableautoconfiguration"))
                .isNotEqualTo("false");
    }

    @Test
    void sliceStillWiresAndRuns() {
        exampleService.sum(3, 4);
    }
}
