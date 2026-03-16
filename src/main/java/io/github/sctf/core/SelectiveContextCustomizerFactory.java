package io.github.sctf.core;

import java.util.List;
import java.util.Set;

import org.springframework.test.context.ContextConfigurationAttributes;
import org.springframework.test.context.ContextCustomizer;
import org.springframework.test.context.ContextCustomizerFactory;
import org.springframework.test.context.TestContextAnnotationUtils;

import io.github.sctf.annotation.TargetComponent;

public class SelectiveContextCustomizerFactory implements ContextCustomizerFactory{

    @Override
    public ContextCustomizer createContextCustomizer(Class<?> testClass, List<ContextConfigurationAttributes> configAttributes) {
        
        // 1.테스트 클래스에서 @TargetComponent 어노테이션 조회
        TargetComponent targetAnnotation = TestContextAnnotationUtils.findMergedAnnotation(testClass, TargetComponent.class);

        // 2. 어노테이션이 없는경우 우리가 개입할 테스트가 아니므로 null을 반환 (Spring 기본 동작 수행)
        if (targetAnnotation == null) {
            return null;
        }

        // 지정된 value class가 없는 경우 exception
        Class<?>[] rootClasses = targetAnnotation.value();
        if(rootClasses == null || rootClasses.length == 0){
            throw new IllegalArgumentException("@TargetComponent에 최소 1개 이상의 타겟 클래스를 지정해야 합니다.");
        }

        // ==========================================
        // 💡 여기서부터가 우리 프레임워크의 심장 박동 시작입니다!
        // ==========================================

        // TODO: 3. 스캐너(DependencyGraphScanner)를 호출해서 필요한 의존성을 싹 다 찾아옵니다.
        // DependencyGraphScanner scanner = new DependencyGraphScanner();
        // Set<Class<?>> scannedClasses = scanner.scan(rootClasses);
        Set<Class<?>> scannedClasses = Set.of(rootClasses); // 일단 컴파일을 위해 임시로 원본만 넣습니다.

        // TODO: 4. 찾아온 클래스 목록으로 캐시 키 생성기(SelectiveCacheKeyGenerator)를 돌려 해시를 만듭니다.
        // SelectiveCacheKeyGenerator keyGenerator = new SelectiveCacheKeyGenerator();
        // String hashKey = keyGenerator.generateKey(scannedClasses);
        String hashKey = "temp-hash-key-123"; // 일단 임시 해시 키

        // 5. 스캔된 클래스 목록과 해시 키를 들고 있는 '작업자(Customizer)'를 생성해서 파견합니다!
        // return new SelectiveContextCustomizer(scannedClasses, hashKey);
        return null;
    }

}
