package io.github.sctf.core;

import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.TypeExcludeFilter;
import org.springframework.core.type.classreading.MetadataReader;
import org.springframework.core.type.classreading.MetadataReaderFactory;

/**
 * 베이스 패키지 내에서 스캔 허용된 클래스만 통과시키는 {@link TypeExcludeFilter} 구현체.
 *
 * <p>Spring의 컴포넌트 스캔 과정에서 동작하며, 베이스 패키지에 속하지만
 * 의존성 그래프에 포함되지 않은 클래스를 제외(exclude)한다.
 * 베이스 패키지 밖의 클래스(Spring 자동 설정 등)는 필터링하지 않고 통과시킨다.</p>
 *
 * <p>{@link SelectiveContextCustomizer}에 의해 빈으로 등록되며,
 * Spring Boot의 {@link TypeExcludeFilter} 메커니즘을 통해 자동으로 적용된다.</p>
 *
 * @see SelectiveContextCustomizer
 */
public class SelectiveTypeExcludeFilter extends TypeExcludeFilter{

    private final Set<String> allowedClassNames;
    private final String basePackage;

    private static final Logger log = LoggerFactory.getLogger(SelectiveTypeExcludeFilter.class);

    /**
     * SelectiveTypeExcludeFilter를 생성한다.
     *
     * @param scannedClasses 의존성 그래프에서 탐색되어 허용된 클래스 집합
     * @param basePackage    필터링 대상이 되는 베이스 패키지
     */
    public SelectiveTypeExcludeFilter(Set<Class<?>> scannedClasses, String basePackage) {
        this.allowedClassNames = scannedClasses.stream()
                .map(Class::getName)
                .collect(Collectors.toSet());
        this.basePackage = basePackage;
    }


    /**
     * 주어진 클래스가 컴포넌트 스캔에서 제외되어야 하는지 판단한다.
     *
     * <p>베이스 패키지에 속하면서 허용 목록에 없는 클래스만 제외한다.
     * 베이스 패키지 밖의 클래스는 항상 통과시킨다.</p>
     *
     * @param metadataReader        스캔 대상 클래스의 메타데이터
     * @param metadataReaderFactory 메타데이터 리더 팩토리
     * @return 제외할 경우 {@code true}, 통과시킬 경우 {@code false}
     */
    @Override
    public boolean match(MetadataReader metadataReader, MetadataReaderFactory metadataReaderFactory) {
        String className = metadataReader.getClassMetadata().getClassName();

        // 1. 현재 스캔 중인 클래스가 우리 프로젝트 패키지 소속인가?
        boolean isOurPackage = className.startsWith(basePackage);

        if (isOurPackage) {
            // 2. 우리 프로젝트 소속인데, 스캐너의 합격 명단에 없다면?
            // ➔ true(스캔 제외)를 반환해서 빈 등록을 막는다
            return !allowedClassNames.contains(className);
        }

        return false;
    }

    // 캐시 키 무결성을 위한 equals & hashCode
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SelectiveTypeExcludeFilter that = (SelectiveTypeExcludeFilter) o;
        return Objects.equals(allowedClassNames, that.allowedClassNames) &&
                Objects.equals(basePackage, that.basePackage);
    }

    @Override
    public int hashCode() {
        return Objects.hash(allowedClassNames, basePackage);
    }

}
