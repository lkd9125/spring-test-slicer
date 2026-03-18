package spring.slicer.test;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;

import io.github.sctf.annotation.TargetComponent;
import io.github.sctf.annotation.TargetComponentTest;
import io.github.sctf.example.ExampleService;

/**
 * {@code withDatabase = false} 시 자동 설정 비활성화 프로퍼티가 적용되고, 슬라이스 빈은 여전히 동작하는지 검증한다.
 */
@TargetComponentTest(basePackage = "io.github.sctf", withDatabase = false)
@TargetComponent({ExampleService.class})
class WithDatabaseFalseIntegrationTest {

    @Autowired
    private Environment environment;

    @Autowired
    private ExampleService exampleService;

    @Test
    void autoConfigurationDisabledPropertyIsSet() {
        assertThat(environment.getProperty("spring.boot.enableautoconfiguration")).isEqualTo("false");
    }

    @Test
    void sliceStillWiresAndRuns() {
        exampleService.sum(1, 2);
    }
}
