package io.github.sctf.core;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.GenericBeanDefinition;

public class BeanDefinitionCollector {

    /**
     * class type 값을 BeanDefinition으로 변환
     * 
     * @param classes 스캐너가 찾아온 의존성 클래스 목록
     * @return Spring에 등록할 BeanDefinition 리스트
     */
    public List<BeanDefinition> collect(Set<Class<?>> classes) { //
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
