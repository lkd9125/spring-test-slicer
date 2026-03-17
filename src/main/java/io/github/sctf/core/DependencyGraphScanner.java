package io.github.sctf.core;

import java.lang.reflect.Constructor;
import java.util.HashSet;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.util.ReflectionUtils;

/**
 * 타겟 클래스들의 의존성 그래프를 재귀적으로 탐색하는 스캐너.
 *
 * <p>루트 클래스들로부터 시작하여 생성자 주입과 {@link Autowired @Autowired} 필드 주입을
 * 따라가며, 베이스 패키지에 속하는 모든 의존 클래스를 수집한다.</p>
 *
 * <p><b>지원하는 의존성 탐지 방식:</b></p>
 * <ul>
 *   <li>단일 생성자의 파라미터 타입</li>
 *   <li>복수 생성자 중 {@link Autowired @Autowired}가 붙은 생성자의 파라미터 타입</li>
 *   <li>{@link Autowired @Autowired}가 붙은 필드 타입</li>
 * </ul>
 *
 * <p><b>제한사항:</b> setter 주입, {@code @Bean} 메서드, 인터페이스 구현체 탐색은 지원하지 않는다.</p>
 *
 * @see SelectiveContextCustomizerFactory
 */
public class DependencyGraphScanner {

    /**
     * 루트 클래스들로부터 의존성 그래프를 재귀 탐색하여 모든 의존 클래스를 수집한다.
     *
     * @param roots       탐색 시작점이 되는 타겟 클래스 배열
     * @param basePackage 탐색 범위를 제한하는 베이스 패키지 (이 패키지에 속하지 않는 클래스는 무시)
     * @return 탐색된 모든 의존 클래스의 집합 (루트 클래스 포함)
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
     * 단일 클래스의 의존성을 재귀적으로 탐색한다.
     *
     * <p>방문 여부를 추적하여 순환 의존성에 의한 무한 루프를 방지하며,
     * 베이스 패키지 밖의 클래스는 탐색하지 않는다.</p>
     *
     * @param clazz       현재 탐색 중인 클래스
     * @param visited     이미 방문한 클래스 집합 (순환 방지)
     * @param result      탐색 결과를 누적하는 집합
     * @param basePackage 탐색 범위를 제한하는 베이스 패키지
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
        if (!clazz.getName().startsWith(basePackage)) {
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
