package io.github.sctf.core;

import java.lang.reflect.AnnotatedType;
import java.lang.reflect.Constructor;
import java.util.HashSet;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.stereotype.Component;
import org.springframework.util.ReflectionUtils;

public class DependencyGraphScanner {

    /**
     * class dependency graph scan
     * 
     * @param roots 타겟 클래스들 (예: @TargetComponent에 적힌 클래스들)
     * @return 꼬리를 물고 찾아낸 모든 클래스들의 집합
     */
    public Set<Class<?>> scan(Class<?>[] roots) { //
        Set<Class<?>> result = new HashSet<>(); // 반환할 전체 클래스 목록
        Set<Class<?>> visited = new HashSet<>(); // 중복방지용 Set

        // 지정된 class의 의존성이 부여된 class 조회
        for (Class<?> root : roots) {
            scanRecursive(root, visited, result);
        }

        return result;
    }

    /**
     * class 의존성 재귀 탐색
     * @param clazz 재귀탐색할 root class
     * @param visited 방문여부 Set
     * @param result dependency RS Set
     */
    private void scanRecursive(Class<?> clazz, Set<Class<?>> visited, Set<Class<?>> result) { //
        // 1. 이미 참조된 클래스인 경우 return
        if (visited.contains(clazz)) {
            return;
        }

        // di 중복 탐색 방지
        visited.add(clazz);

        // 2. 해당 클래스가 Spring Bean이 맞는지 확인 (@Component, @Service 등) 
        if (!AnnotatedElementUtils.hasAnnotation(clazz, Component.class)) {
            return; 
        }

        // 결과 목록에 현재 클래스 추가
        result.add(clazz); 

        // 3. 생성자 주입 분석: Constructor.getParameterTypes() 추출 후 재귀 호출 [cite: 40, 65]
        Constructor<?>[] constructors = clazz.getDeclaredConstructors();
        Constructor<?> targetConstructor = null;

        if (constructors.length == 1) {
            // 생성자가 1개면 Spring이 무조건 그걸 사용해서 자동 주입함
            targetConstructor = constructors[0];
        } else {
            // 생성자가 여러 개면 @Autowired가 붙은 녀석을 찾음
            for (Constructor<?> c : constructors) {
                if (AnnotatedElementUtils.hasAnnotation(c, Autowired.class)) {
                    targetConstructor = c;
                    break;
                }
            }
        }

        if (targetConstructor != null) {
            // 찾은 생성자의 파라미터 타입들을 싹 가져와서 꼬리를 물고 재귀 호출
            for (Class<?> paramType : targetConstructor.getParameterTypes()) {
                scanRecursive(paramType, visited, result);
            }
        }
        
        // 4. @Autowired 필드 분석: ReflectionUtils로 필드 타입 추출 후 재귀 호출
        ReflectionUtils.doWithFields(clazz, field -> {
            // 필드에 @Autowired가 붙어있다면?
            if (AnnotatedElementUtils.hasAnnotation(field, Autowired.class)) {
                // 그 필드의 타입으로 다시 재귀 탐색 시작!
                scanRecursive(field.getType(), visited, result);
            }
        });
    }
}
