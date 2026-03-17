package io.github.sctf.core;

import java.util.List;
import java.util.Set;

import org.springframework.test.context.ContextConfigurationAttributes;
import org.springframework.test.context.ContextCustomizer;
import org.springframework.test.context.ContextCustomizerFactory;
import org.springframework.test.context.TestContextAnnotationUtils;

import io.github.sctf.annotation.TargetComponent;
import io.github.sctf.annotation.TargetComponentTest;

public class SelectiveContextCustomizerFactory implements ContextCustomizerFactory{

    @Override
    public ContextCustomizer createContextCustomizer(Class<?> testClass, List<ContextConfigurationAttributes> configAttributes) {
        
        // 1.테스트 클래스에서 @TargetComponent 어노테이션 조회
        TargetComponent targetAnnotation = TestContextAnnotationUtils.findMergedAnnotation(testClass, TargetComponent.class);

        // 2. 어노테이션이 없는경우 우리가 개입할 테스트가 아니므로 null을 반환 (Spring 기본 동작 수행)
        if (targetAnnotation == null) {
            return null;
        }

        // 3. db 자동설정 여부
        TargetComponentTest testAnnotation = TestContextAnnotationUtils.findMergedAnnotation(testClass, TargetComponentTest.class);
        boolean withDatabase = (testAnnotation != null) && testAnnotation.withDatabase();

        // 지정된 value class가 없는 경우 exception
        Class<?>[] rootClasses = targetAnnotation.value();
        if(rootClasses == null || rootClasses.length == 0){
            throw new IllegalArgumentException("@TargetComponent에 최소 1개 이상의 타겟 클래스를 지정해야 합니다.");
        }

        // 4. TargetComponent의 필요한 의존성 조회
        DependencyGraphScanner scanner = new DependencyGraphScanner();
        Set<Class<?>> scannedClasses = scanner.scan(rootClasses);

        // 5. 찾아온 클래스 목록으로 캐시 키 생성기(SelectiveCacheKeyGenerator)를 돌려 해시 생성
        SelectiveCacheKeyGenerator generator = new SelectiveCacheKeyGenerator();
        String hashKey = generator.generateKey(scannedClasses); // 일단 임시 해시 키

        // 6. selective context contommizer에게 bean definition을 전부 만들기를 위임
        return new SelectiveContextCustomizer(scannedClasses, hashKey, withDatabase);
    }

}
