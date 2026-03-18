package io.github.sctf.core;

import java.lang.reflect.Constructor;
import java.util.HashSet;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.util.ReflectionUtils;

public class DependencyGraphScanner {

    public Set<Class<?>> scan(Class<?>[] roots, String basePackage) {
        Set<Class<?>> result = new HashSet<>(); 
        Set<Class<?>> visited = new HashSet<>(); 

        for (Class<?> root : roots) {
            scanRecursive(root, visited, result, basePackage);
        }
        return result;
    }

    private void scanRecursive(Class<?> clazz, Set<Class<?>> visited, Set<Class<?>> result, String basePackage) {
        if (visited.contains(clazz)) {
            return;
        }
        visited.add(clazz);

        String className = clazz.getName();

        // 1. 우리 프로젝트(basePackage) 소속이 아니면 추적 중단 (Spring 설정, 라이브러리 등은 알아서 뜨게 냅둠)
        if (!className.startsWith(basePackage)) {
            return;
        }

        // 2. 우리 동네 사람(basePackage 소속)이면 무조건 타겟과 연관된 필수 클래스임! 결과에 추가.
        result.add(clazz);

        // 3. 인터페이스는 내부에 의존성이 없으므로 여기서 탐색 종료 (Spring Data JPA, @HttpExchange 등)
        if (clazz.isInterface()) {
            return;
        }

        // --- 4. 생성자 주입 분석 ---
        Constructor<?>[] constructors = clazz.getDeclaredConstructors();
        Constructor<?> targetConstructor = null;

        if (constructors.length == 1) {
            targetConstructor = constructors[0];
        } else {
            for (Constructor<?> c : constructors) {
                if (AnnotatedElementUtils.hasAnnotation(c, Autowired.class)) {
                    targetConstructor = c;
                    break;
                }
            }
        }

        if (targetConstructor != null) {
            for (Class<?> paramType : targetConstructor.getParameterTypes()) {
                scanRecursive(paramType, visited, result, basePackage);
            }
        }
        
        // --- 5. @Autowired 필드 분석 ---
        ReflectionUtils.doWithFields(clazz, field -> {
            if (AnnotatedElementUtils.hasAnnotation(field, Autowired.class)) {
                scanRecursive(field.getType(), visited, result, basePackage);
            }
        });
    }
}