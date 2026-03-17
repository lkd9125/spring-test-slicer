package io.github.sctf.core;

import java.lang.reflect.Constructor;
import java.util.HashSet;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.util.ReflectionUtils;

public class DependencyGraphScanner {

    /**
     * class dependency graph scan
     * 
     * @param roots 타겟 클래스들 (예: @TargetComponent에 적힌 클래스들)
     * @return 꼬리를 물고 찾아낸 모든 클래스들의 집합
     */
    public Set<Class<?>> scan(Class<?>[] roots, String basePackage) { 
        Set<Class<?>> result = new HashSet<>(); 
        Set<Class<?>> visited = new HashSet<>(); 

        for (Class<?> root : roots) {
            scanRecursive(root, visited, result, basePackage);
        }
        return result;
    }
    /**
     * class 의존성 재귀 탐색
     * @param clazz 재귀탐색할 root class
     * @param visited 방문여부 Set
     * @param result dependency RS Set
     */
    private void scanRecursive(Class<?> clazz, Set<Class<?>> visited, Set<Class<?>> result, String basePackage) { 
        if (visited.contains(clazz)) {
            return;
        }
        visited.add(clazz);

        // =========================================================
        // 💡 [핵심] @Component 검사 삭제! 
        // 대신 "우리 프로젝트(basePackage) 소속인가?" 만 검사합니다.
        // 이렇게 하면 Repository(인터페이스)든 Helper(컴포넌트 누락)든 다 가져옵니다!
        // =========================================================
        if (clazz.getName() == null || !clazz.getName().startsWith(basePackage)) {
            return; // 외부 라이브러리(String, List 등)는 스캔 중지
        }

        // 우리 동네 사람이면 결과에 추가!
        result.add(clazz); 

        // 3. 생성자 주입 분석 (기존 동일)
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
        
        // 4. @Autowired 필드 분석 (기존 동일)
        ReflectionUtils.doWithFields(clazz, field -> {
            if (AnnotatedElementUtils.hasAnnotation(field, Autowired.class)) {
                scanRecursive(field.getType(), visited, result, basePackage);
            }
        });
    }
}
