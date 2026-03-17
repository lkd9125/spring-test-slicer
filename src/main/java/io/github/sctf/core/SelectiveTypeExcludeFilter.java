package io.github.sctf.core;

import java.util.Arrays;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.boot.context.TypeExcludeFilter;
import org.springframework.core.type.classreading.MetadataReader;
import org.springframework.core.type.classreading.MetadataReaderFactory;

public class SelectiveTypeExcludeFilter extends TypeExcludeFilter{

    private final Set<String> allowedClassNames; // 스캐너가 합격시킨 35개 명단
    private final String[] basePackages; // 예: "com.kac.utm"

    public SelectiveTypeExcludeFilter(Set<Class<?>> scannedClasses, String[] basePackages) {
        this.allowedClassNames = scannedClasses.stream()
                .map(Class::getName)
                .collect(Collectors.toSet());
        this.basePackages = basePackages;
    }


    @Override
    public boolean match(MetadataReader metadataReader, MetadataReaderFactory metadataReaderFactory) {
        String className = metadataReader.getClassMetadata().getClassName();

        // 1. 현재 스캔 중인 클래스가 우리 회사/프로젝트 패키지 소속인가?
        boolean isOurPackage = Arrays.stream(basePackages).anyMatch(className::startsWith);

        if (isOurPackage) {
            // 2. 우리 프로젝트 소속인데, 스캐너의 합격 명단에 없다면? 
            // ➔ 가차 없이 true(스캔 제외)를 반환해서 빈 등록을 막아버립니다!
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
                Arrays.equals(basePackages, that.basePackages);
    }

    @Override
    public int hashCode() {
        int result = Objects.hash(allowedClassNames);
        result = 31 * result + Arrays.hashCode(basePackages);
        return result;
    }

}
