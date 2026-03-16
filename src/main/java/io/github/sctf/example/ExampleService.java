package io.github.sctf.example;

import org.springframework.stereotype.Service;

@Service
public class ExampleService {

    private final ExampleComponent exampleComponent;

    public ExampleService(ExampleComponent exampleComponent){
        this.exampleComponent = exampleComponent;
    }

    public void sum(int a, int b){
        System.out.println("DATA :: " + exampleComponent.exampleSum(a, b));
    }
}
