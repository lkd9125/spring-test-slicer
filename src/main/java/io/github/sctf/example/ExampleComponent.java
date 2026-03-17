package io.github.sctf.example;

import org.springframework.stereotype.Component;

/**
 * 테스트용 예제 컴포넌트.
 *
 * <p>{@link ExampleService}의 의존성으로 사용되며,
 * 의존성 그래프 탐색과 선택적 컨텍스트 로딩이 정상 동작하는지 검증하기 위한 샘플 클래스이다.</p>
 */
@Component
public class ExampleComponent {

    /**
     * 두 정수의 합을 반환한다.
     *
     * @param a 첫 번째 정수
     * @param b 두 번째 정수
     * @return 두 정수의 합
     */
    public int exampleSum(int a, int b){
        return a+b;
    }

}
