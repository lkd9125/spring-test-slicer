package io.github.sctf.core;

import java.lang.reflect.Proxy;
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
import org.springframework.util.ClassUtils;

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

        TestPropertyValues.of(
            "spring.autoconfigure.exclude=" +
            "org.springframework.boot.autoconfigure.security.reactive.ReactiveSecurityAutoConfiguration," +
            "org.springframework.boot.autoconfigure.security.reactive.ReactiveUserDetailsServiceAutoConfiguration," +
            "org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration," +
            "org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration"
        ).applyTo(context);

        registerMockIfPresent(registry, "org.springframework.security.authentication.ReactiveAuthenticationManager", "reactiveAuthenticationManager");
        registerMockIfPresent(registry, "org.springframework.security.authentication.AuthenticationManager", "authenticationManager");

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

    /**
     * 주어진 타입명을 로드할 수 있을 때, 해당 타입을 위한 <em>더미(Proxy) 빈</em>을 동적으로 등록합니다.
     *
     * <p>이 메서드는 Mockito 같은 mocking 라이브러리를 사용하지 않고, JDK Dynamic Proxy를 이용해
     * 어떤 메서드를 호출하더라도 {@code null}을 반환하는 인스턴스를 생성해 빈으로 제공합니다.</p>
     *
     * <h3>주의/제약</h3>
     * <ul>
     *   <li>JDK Proxy는 <strong>인터페이스</strong>만 프록시할 수 있습니다. (구체 클래스는 불가)</li>
     *   <li>프록시가 항상 {@code null}을 반환하므로, 기본 타입 반환(예: {@code int})이나
     *       {@code toString}/{@code equals}/{@code hashCode}에 의존하는 코드는 예상치 못한 동작을 할 수 있습니다.</li>
     *   <li>타입이 클래스패스에 없으면 {@link ClassNotFoundException}이 발생하며, 이 경우 조용히 무시합니다.</li>
     * </ul>
     *
     * @param registry 빈을 등록할 레지스트리
     * @param className 로드할 대상 타입의 FQCN (Fully Qualified Class Name)
     * @param beanName 등록할 빈 이름
     */
    private void registerMockIfPresent(BeanDefinitionRegistry registry, String className, String beanName) {
        try {
            Class<?> clazz = ClassUtils.forName(className, getClass().getClassLoader());

            // Spring 5+ 기능: Supplier를 이용해 깡통 객체를 빈으로 직접 등록!
            RootBeanDefinition mockDef = new RootBeanDefinition(clazz);
            mockDef.setInstanceSupplier(() ->
                Proxy.newProxyInstance(
                    clazz.getClassLoader(),
                    new Class<?>[]{clazz},
                    (proxy, method, args) -> null // 깡통 객체: 어떤 메서드를 호출하든 null을 반환
                )
            );

            registry.registerBeanDefinition(beanName, mockDef);
            
            log.info("Sctf Framework: [{}] 순수 Java Proxy Dummy 주입 완료!", className);
        } catch (ClassNotFoundException e) {
            // 해당 프로젝트는 Security 모듈을 사용하지 않으므로 부드럽게 무시합니다.
        }
    }

}
