package io.github.sctf.example;

import org.springframework.stereotype.Component;

/**
 * {@link ExampleService}와 의존 관계가 없는 컴포넌트.
 * 슬라이스가 {@link ExampleService}만 루트로 할 때 컨텍스트에 포함되지 않아야 함을 검증하기 위한 마커용 빈이다.
 */
@Component
public class ExampleStandaloneComponent {

    public String marker() {
        return "standalone";
    }
}
