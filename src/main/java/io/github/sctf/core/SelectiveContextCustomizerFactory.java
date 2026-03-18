package io.github.sctf.core;

import java.util.List;
import java.util.Set;

import org.springframework.test.context.ContextConfigurationAttributes;
import org.springframework.test.context.ContextCustomizer;
import org.springframework.test.context.ContextCustomizerFactory;
import org.springframework.test.context.TestContextAnnotationUtils;

import io.github.sctf.annotation.TargetComponent;
import io.github.sctf.annotation.TargetComponentTest;

public class SelectiveContextCustomizerFactory implements ContextCustomizerFactory {

    @Override
    public ContextCustomizer createContextCustomizer(Class<?> testClass, List<ContextConfigurationAttributes> configAttributes) {
        
        TargetComponent targetAnnotation = TestContextAnnotationUtils.findMergedAnnotation(testClass, TargetComponent.class);

        if (targetAnnotation == null) {
            return null;
        }

        TargetComponentTest testAnnotation = TestContextAnnotationUtils.findMergedAnnotation(testClass, TargetComponentTest.class);
        boolean withDatabase = (testAnnotation != null) && testAnnotation.withDatabase();
        String basePackage = (testAnnotation != null) ? testAnnotation.basePackage() : "";
        boolean stubSecurityInfrastructure = (testAnnotation != null) && testAnnotation.stubSecurityInfrastructure();

        Class<?>[] rootClasses = targetAnnotation.value();
        if(rootClasses == null || rootClasses.length == 0){
            throw new IllegalArgumentException("@TargetComponent에 최소 1개 이상의 타겟 클래스를 지정해야 합니다.");
        }

        // 💡 1. Scanner에서 분리된 결과(ScanResult)를 받아옴
        DependencyGraphScanner scanner = new DependencyGraphScanner();
        Set<Class<?>> scannedClasses = scanner.scan(rootClasses, basePackage);

        SelectiveCacheKeyGenerator generator = new SelectiveCacheKeyGenerator();
        String hashKey = generator.generateKey(scannedClasses, withDatabase, basePackage, stubSecurityInfrastructure);

        return new SelectiveContextCustomizer(scannedClasses, hashKey, withDatabase, basePackage, stubSecurityInfrastructure);
    }
}