package io.github.sctf.core;

import java.lang.reflect.Proxy;
import java.util.Objects;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.test.context.ContextCustomizer;
import org.springframework.test.context.MergedContextConfiguration;
import org.springframework.util.ClassUtils;

public class SelectiveContextCustomizer implements ContextCustomizer {

    private static final Logger log = LoggerFactory.getLogger(SelectiveContextCustomizer.class);

    private final Set<Class<?>> scannedClasses;
    private final String hashKey;
    private final boolean withDatabase;
    private final String basePackage;
    private final boolean stubSecurityInfrastructure;

    public SelectiveContextCustomizer(Set<Class<?>> scannedClasses, String hashKey, boolean withDatabase,
            String basePackage, boolean stubSecurityInfrastructure) {
        this.scannedClasses = scannedClasses;
        this.hashKey = hashKey;
        this.withDatabase = withDatabase;
        this.basePackage = basePackage;
        this.stubSecurityInfrastructure = stubSecurityInfrastructure;
    }

    @Override
    public void customizeContext(ConfigurableApplicationContext context, MergedContextConfiguration mergedConfig) {
        log.debug("Selective context cache key: {}", hashKey);

        if(scannedClasses == null || scannedClasses.isEmpty()){
            return;
        }

        BeanDefinitionRegistry registry = (BeanDefinitionRegistry) context.getBeanFactory();

        // 💡 핵심: 우리가 스캔한 명단을 들고 있는 TypeExcludeFilter를 스프링에 등록!
        RootBeanDefinition filterDef = new RootBeanDefinition(SelectiveTypeExcludeFilter.class);
        filterDef.getConstructorArgumentValues().addGenericArgumentValue(this.scannedClasses);
        filterDef.getConstructorArgumentValues().addGenericArgumentValue(this.basePackage);
        registry.registerBeanDefinition("selectiveTypeExcludeFilter", filterDef);

        // spring 자동 설정 off (설정에 따라)
        if (!withDatabase) {
            log.info("Spring auto-configuration disabled (withDatabase=false)");
            TestPropertyValues.of("spring.boot.enableautoconfiguration=false").applyTo(context);
        }

        // Security 스텁 처리
        if (stubSecurityInfrastructure) {
            TestPropertyValues.of(
                "spring.autoconfigure.exclude=" +
                "org.springframework.boot.autoconfigure.security.reactive.ReactiveSecurityAutoConfiguration," +
                "org.springframework.boot.autoconfigure.security.reactive.ReactiveUserDetailsServiceAutoConfiguration," +
                "org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration," +
                "org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration"
            ).applyTo(context);
            registerMockIfPresent(registry, "org.springframework.security.authentication.ReactiveAuthenticationManager", "reactiveAuthenticationManager");
            registerMockIfPresent(registry, "org.springframework.security.authentication.AuthenticationManager", "authenticationManager");
        }
    }

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

    // Security 용 Dummy Mock (이건 Spring 설정에서 강제로 뺄 때 쓰는 용도니까 유지)
    private void registerMockIfPresent(BeanDefinitionRegistry registry, String className, String beanName) {
        try {
            Class<?> clazz = ClassUtils.forName(className, getClass().getClassLoader());
            RootBeanDefinition mockDef = new RootBeanDefinition(clazz);
            mockDef.setInstanceSupplier(() ->
                Proxy.newProxyInstance(clazz.getClassLoader(), new Class<?>[]{clazz},
                    (proxy, method, args) -> {
                        String methodName = method.getName();
                        if ("hashCode".equals(methodName)) return System.identityHashCode(proxy);
                        if ("equals".equals(methodName)) return proxy == (args != null && args.length > 0 ? args[0] : null);
                        if ("toString".equals(methodName)) return "SecurityDummy@" + Integer.toHexString(System.identityHashCode(proxy));
                        return null;
                    }
                )
            );
            registry.registerBeanDefinition(beanName, mockDef);
            log.info("Sctf Framework: [{}] Security Dummy 주입 완료!", className);
        } catch (ClassNotFoundException e) {
            // 조용히 무시
        }
    }
}