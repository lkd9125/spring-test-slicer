package spring.slicer.test;

import java.util.Set;

import org.junit.jupiter.api.Test;

import io.github.sctf.core.DependencyGraphScanner;
import io.github.sctf.example.ExampleService;

public class CoreTest {

    @Test
    void dependencyGraphScan() throws Exception{
        DependencyGraphScanner scanner = new DependencyGraphScanner();
        Set<Class<?>> classSet = scanner.scan(new Class[]{ExampleService.class}, "");

        for(Class<?> clazz : classSet){
            System.out.println(clazz);
        }
    }

}
