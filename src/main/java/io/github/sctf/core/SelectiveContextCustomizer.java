package io.github.sctf.core;

import java.util.List;
import java.util.Objects;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.BeanNameGenerator;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.AnnotationBeanNameGenerator;
import org.springframework.test.context.ContextCustomizer;
import org.springframework.test.context.MergedContextConfiguration;

public class SelectiveContextCustomizer implements ContextCustomizer{

    private static final Logger log = LoggerFactory.getLogger(SelectiveContextCustomizer.class);

    private final Set<Class<?>> scannedClasses;
    private final String hashKey;
    private final boolean withDatabase;

    public SelectiveContextCustomizer(Set<Class<?>> scannedClasses, String hashKey, boolean withDatabase) {
        this.scannedClasses = scannedClasses;
        this.hashKey = hashKey;
        this.withDatabase = withDatabase;
    }


    @Override
    public void customizeContext(ConfigurableApplicationContext context, MergedContextConfiguration mergedConfig) {
        log.debug("Selective context cache key: {}", hashKey);

        // 1. 의존성 scan된 class가 없으면 context 생성 종료
        if(scannedClasses == null || scannedClasses.isEmpty()){
            return;
        }

        BeanDefinitionRegistry registry = (BeanDefinitionRegistry) context.getBeanFactory();

        // 2. ComponentScan 전면 차단 — 빈 등록은 아래에서 수동으로 처리
        // RootBeanDefinition filterDef = new RootBeanDefinition(SelectiveTypeExcludeFilter.class);
        // registry.registerBeanDefinition("selectiveTypeExcludeFilter", filterDef);

        // 3. spring 자동 설정 off
        if (!withDatabase) {
            log.info("Spring auto-configuration disabled (withDatabase=false)");
            TestPropertyValues.of("spring.boot.enableautoconfiguration=false").applyTo(context);
        }

        // 4. class -> bean defintion으로 변환
        BeanDefinitionCollector collector = new BeanDefinitionCollector();
        List<BeanDefinition> definitions = collector.collect(scannedClasses);

        // 5. bean defintion을 bean으로 등록
        BeanNameGenerator nameGenerator = new AnnotationBeanNameGenerator();

        for (BeanDefinition definition : definitions) {
            String beanName = nameGenerator.generateBeanName(definition, registry);
            registry.registerBeanDefinition(beanName, definition);
            log.info("Bean definition registered: {}", beanName);
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
