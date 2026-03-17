package io.github.sctf.core;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.GenericBeanDefinition;

/**
 * 스캔된 클래스 집합을 Spring {@link BeanDefinition} 목록으로 변환하는 수집기.
 *
 * <p>{@link io.github.sctf.core.DependencyGraphScanner}가 탐색한 클래스들을
 * {@link GenericBeanDefinition}으로 래핑하여 {@link org.springframework.beans.factory.support.BeanDefinitionRegistry}에
 * 등록할 수 있는 형태로 만든다.</p>
 *
 * @see DependencyGraphScanner
 * @see SelectiveContextCustomizer
 */
public class BeanDefinitionCollector {

    /**
     * 클래스 집합을 {@link BeanDefinition} 리스트로 변환한다.
     *
     * @param classes 스캐너가 탐색한 의존성 클래스 목록
     * @return Spring 컨텍스트에 등록할 {@link BeanDefinition} 리스트
     */
    public List<BeanDefinition> collect(Set<Class<?>> classes) {
        List<BeanDefinition> definitions = new ArrayList<>();

        for (Class<?> clazz : classes) {
            // 1. Spring의 기본 명세서 객체를 생성
            GenericBeanDefinition beanDefinition = new GenericBeanDefinition(); //
            
            // 2. clazz 기반에 해당되는 beanDefinition 생성
            beanDefinition.setBeanClass(clazz); 
            
            definitions.add(beanDefinition);
        }

        return definitions;
    }
}
