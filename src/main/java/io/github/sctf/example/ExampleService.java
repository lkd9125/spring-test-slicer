package io.github.sctf.example;

import org.springframework.stereotype.Service;

/**
 * 테스트용 예제 서비스.
 *
 * <p>생성자 주입을 통해 {@link ExampleComponent}에 의존하며,
 * {@link io.github.sctf.core.DependencyGraphScanner}가 생성자 주입 기반의
 * 의존성을 정상적으로 탐색하는지 검증하기 위한 샘플 클래스이다.</p>
 */
@Service
public class ExampleService {

    private final ExampleComponent exampleComponent;

    /**
     * ExampleService를 생성한다.
     *
     * @param exampleComponent 의존하는 예제 컴포넌트
     */
    public ExampleService(ExampleComponent exampleComponent){
        this.exampleComponent = exampleComponent;
    }

    /**
     * 두 정수의 합을 콘솔에 출력한다.
     *
     * @param a 첫 번째 정수
     * @param b 두 번째 정수
     */
    public void sum(int a, int b){
        System.out.println("DATA :: " + exampleComponent.exampleSum(a, b));
    }
}
