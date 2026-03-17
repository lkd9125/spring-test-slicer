package io.github.sctf.core;

import java.util.List;
import java.util.Objects;
import java.util.Set;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.BeanNameGenerator;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.AnnotationBeanNameGenerator;
import org.springframework.test.context.ContextCustomizer;
import org.springframework.test.context.MergedContextConfiguration;

public class SelectiveContextCustomizer implements ContextCustomizer{

    private final Set<Class<?>> scannedClasses;
    private final String hashKey;

    public SelectiveContextCustomizer(Set<Class<?>> scannedClasses, String hashKey) {
        this.scannedClasses = scannedClasses;
        this.hashKey = hashKey;
    }


    @Override
    public void customizeContext(ConfigurableApplicationContext context, MergedContextConfiguration mergedConfig) {
        System.out.println("hash key : " + hashKey);
        
        // 1. 의존성 scan된 class가 없으면 context 생성 종료
        if(scannedClasses == null || scannedClasses.isEmpty()){
            return;
        }
        
        // 2. class -> bean defintion으로 변환 
        BeanDefinitionCollector collector = new BeanDefinitionCollector();
        List<BeanDefinition> definitions = collector.collect(scannedClasses);
        
        // 3. bean defintion을 bean으로 등록
        BeanDefinitionRegistry registry = (BeanDefinitionRegistry) context.getBeanFactory();
        BeanNameGenerator nameGenerator = new AnnotationBeanNameGenerator();
        
        for (BeanDefinition definition : definitions) {
            // 빈의 고유 이름은 클래스명 + 인덱스로 겹치지 않게 지어줍니다.
            String beanName = nameGenerator.generateBeanName(definition, registry);
            registry.registerBeanDefinition(beanName, definition);
            System.out.println("bean definition 등록 완료: " + beanName);
        }
        
    }

    // 캐시 키로 사용하기 위해 equals와 hashCode를 해시 키 기반으로 오버라이드!
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SelectiveContextCustomizer that = (SelectiveContextCustomizer) o;
        return Objects.equals(hashKey, that.hashKey);
    }

    @Override
    public int hashCode() {
        return Objects.hash(hashKey);
    }

}
