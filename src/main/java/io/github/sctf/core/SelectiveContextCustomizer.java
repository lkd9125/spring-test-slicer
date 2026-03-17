package io.github.sctf.core;

import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

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

/**
 * 스캔된 의존성 클래스만 선택적으로 ApplicationContext에 등록하는 {@link ContextCustomizer} 구현체.
 *
 * <p>이 클래스는 다음 작업을 수행한다:</p>
 * <ol>
 *   <li>{@link SelectiveTypeExcludeFilter}를 등록하여 불필요한 컴포넌트 스캔을 차단</li>
 *   <li>{@code withDatabase} 설정에 따라 Spring 자동 설정을 제어</li>
 *   <li>스캔된 구체 클래스들을 {@link BeanDefinition}으로 변환하여 수동 등록</li>
 * </ol>
 *
 * <p>{@code equals}와 {@code hashCode}는 해시 키 기반으로 구현되어
 * Spring Test의 컨텍스트 캐싱에 활용된다.</p>
 *
 * @see SelectiveContextCustomizerFactory
 * @see SelectiveTypeExcludeFilter
 * @see BeanDefinitionCollector
 */
public class SelectiveContextCustomizer implements ContextCustomizer{

    private static final Logger log = LoggerFactory.getLogger(SelectiveContextCustomizer.class);

    private final Set<Class<?>> scannedClasses;
    private final String hashKey;
    private final boolean withDatabase;
    private final String basePackage;

    /**
     * SelectiveContextCustomizer를 생성한다.
     *
     * @param scannedClasses 의존성 그래프에서 탐색된 클래스 집합
     * @param hashKey        컨텍스트 캐싱에 사용되는 SHA-256 해시 키
     * @param withDatabase   Spring 자동 설정(DB, Redis 등) 포함 여부
     * @param basePackage    컴포넌트 스캔 필터링의 기준 베이스 패키지
     */
    public SelectiveContextCustomizer(Set<Class<?>> scannedClasses, String hashKey, boolean withDatabase, String basePackage) {
        this.scannedClasses = scannedClasses;
        this.hashKey = hashKey;
        this.withDatabase = withDatabase;
        this.basePackage = basePackage;
    }


    /**
     * ApplicationContext를 커스터마이징하여 스캔된 의존성만 빈으로 등록한다.
     *
     * <p>스캔된 클래스가 없으면 아무 작업도 수행하지 않는다.
     * 인터페이스는 빈 등록 대상에서 제외된다.</p>
     *
     * @param context      커스터마이징할 ApplicationContext
     * @param mergedConfig 병합된 테스트 컨텍스트 설정
     */
    @Override
    public void customizeContext(ConfigurableApplicationContext context, MergedContextConfiguration mergedConfig) {
        log.debug("Selective context cache key: {}", hashKey);

        // 1. 의존성 scan된 class가 없으면 context 생성 종료
        if(scannedClasses == null || scannedClasses.isEmpty()){
            return;
        }

        BeanDefinitionRegistry registry = (BeanDefinitionRegistry) context.getBeanFactory();

        // 2. ComponentScan 전면 차단 — 빈 등록은 아래에서 수동으로 처리
        RootBeanDefinition filterDef = new RootBeanDefinition(SelectiveTypeExcludeFilter.class);
        filterDef.getConstructorArgumentValues().addGenericArgumentValue(this.scannedClasses);
        filterDef.getConstructorArgumentValues().addGenericArgumentValue(this.basePackage);
        registry.registerBeanDefinition("selectiveTypeExcludeFilter", filterDef);

        // 3. spring 자동 설정 off
        if (!withDatabase) {
            log.info("Spring auto-configuration disabled (withDatabase=false)");
            TestPropertyValues.of("spring.boot.enableautoconfiguration=false").applyTo(context);
        }

        Set<Class<?>> concreteClasses = scannedClasses.stream()
                .filter(clazz -> !clazz.isInterface()) // 인터페이스 컷!
                .collect(Collectors.toSet());

        // 4. class -> bean defintion으로 변환
        BeanDefinitionCollector collector = new BeanDefinitionCollector();
        List<BeanDefinition> definitions = collector.collect(concreteClasses);

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
