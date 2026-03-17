package io.github.sctf.bootstrap;

import org.springframework.boot.test.context.SpringBootContextLoader;

/**
 * 선택적 컨텍스트 로딩을 위한 커스텀 {@link org.springframework.test.context.ContextLoader}.
 *
 * <p>현재는 {@link SpringBootContextLoader}를 그대로 상속하며,
 * 향후 컨텍스트 로딩 과정의 커스터마이징이 필요할 때 확장 포인트로 사용된다.</p>
 *
 * @see SelectiveContextBootstrapper
 */
public class SelectiveContextLoader extends SpringBootContextLoader{

}
