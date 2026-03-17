package spring.slicer.test;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import io.github.sctf.annotation.TargetComponent;
import io.github.sctf.annotation.TargetComponentTest;
import io.github.sctf.example.ExampleService;

@TargetComponentTest(basePackage = "")
@TargetComponent({ExampleService.class})
class ExampleTest {

	@Autowired
	ExampleService exampleService;

	@Test
	void test() throws Exception{
		exampleService.sum(1, 2);

	}

}
