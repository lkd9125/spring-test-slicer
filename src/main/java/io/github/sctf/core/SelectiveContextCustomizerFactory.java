package io.github.sctf.core;

import java.util.List;
import java.util.Set;

import org.springframework.test.context.ContextConfigurationAttributes;
import org.springframework.test.context.ContextCustomizer;
import org.springframework.test.context.ContextCustomizerFactory;
import org.springframework.test.context.TestContextAnnotationUtils;

import io.github.sctf.annotation.TargetComponent;
import io.github.sctf.annotation.TargetComponentTest;

/**
 * {@link ContextCustomizerFactory} 구현체로, {@code META-INF/spring.factories}를 통해
 * Spring Test에 자동 등록된다.
 *
 * <p>테스트 클래스에 {@link TargetComponent @TargetComponent} 어노테이션이 있을 경우,
 * 의존성 그래프를 스캔하고 {@link SelectiveContextCustomizer}를 생성하여 반환한다.
 * 어노테이션이 없으면 {@code null}을 반환하여 Spring 기본 동작을 수행하게 한다.</p>
 *
 * @see SelectiveContextCustomizer
 * @see DependencyGraphScanner
 * @see SelectiveCacheKeyGenerator
 */
public class SelectiveContextCustomizerFactory implements ContextCustomizerFactory{

    /**
     * 테스트 클래스의 어노테이션을 분석하여 {@link SelectiveContextCustomizer}를 생성한다.
     *
     * <p>{@link TargetComponent @TargetComponent}가 없으면 {@code null}을 반환한다.
     * 타겟 클래스가 지정되지 않은 경우 {@link IllegalArgumentException}을 발생시킨다.</p>
     *
     * @param testClass        테스트 클래스
     * @param configAttributes 컨텍스트 설정 속성 목록
     * @return {@link SelectiveContextCustomizer} 또는 {@code null}
     * @throws IllegalArgumentException {@code @TargetComponent}에 타겟 클래스가 지정되지 않은 경우
     */
    @Override
    public ContextCustomizer createContextCustomizer(Class<?> testClass, List<ContextConfigurationAttributes> configAttributes) {
        
        // 1.테스트 클래스에서 @TargetComponent 어노테이션 조회
        TargetComponent targetAnnotation = TestContextAnnotationUtils.findMergedAnnotation(testClass, TargetComponent.class);

        // 2. 어노테이션이 없는경우 우리가 개입할 테스트가 아니므로 null을 반환 (Spring 기본 동작 수행)
        if (targetAnnotation == null) {
            return null;
        }

        // 3. 테스트 메타 설정
        TargetComponentTest testAnnotation = TestContextAnnotationUtils.findMergedAnnotation(testClass, TargetComponentTest.class);
        boolean withDatabase = (testAnnotation != null) && testAnnotation.withDatabase();
        String basePackage = (testAnnotation != null) ? testAnnotation.basePackage() : "";
        boolean stubSecurityInfrastructure = (testAnnotation != null) && testAnnotation.stubSecurityInfrastructure();

        // 지정된 value class가 없는 경우 exception
        Class<?>[] rootClasses = targetAnnotation.value();
        if(rootClasses == null || rootClasses.length == 0){
            throw new IllegalArgumentException("@TargetComponent에 최소 1개 이상의 타겟 클래스를 지정해야 합니다.");
        }

        // 4. TargetComponent의 필요한 의존성 조회
        DependencyGraphScanner scanner = new DependencyGraphScanner();
        Set<Class<?>> scannedClasses = scanner.scan(rootClasses, basePackage);

        // 5. 찾아온 클래스 목록으로 캐시 키 생성기(SelectiveCacheKeyGenerator)를 돌려 해시 생성
        SelectiveCacheKeyGenerator generator = new SelectiveCacheKeyGenerator();
        String hashKey = generator.generateKey(scannedClasses, withDatabase, basePackage, stubSecurityInfrastructure);

        // 6. selective context contommizer에게 bean definition을 전부 만들기를 위임
        return new SelectiveContextCustomizer(scannedClasses, hashKey, withDatabase, basePackage, stubSecurityInfrastructure);
    }

}
