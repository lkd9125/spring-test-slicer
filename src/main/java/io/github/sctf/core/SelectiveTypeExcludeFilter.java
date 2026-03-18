package io.github.sctf.core;

import java.util.Objects;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.TypeExcludeFilter;
import org.springframework.core.type.classreading.MetadataReader;
import org.springframework.core.type.classreading.MetadataReaderFactory;

public class SelectiveTypeExcludeFilter extends TypeExcludeFilter {

    private final String basePackage;
    private final Set<Class<?>> scannedClasses;

    private static final Logger log = LoggerFactory.getLogger(SelectiveTypeExcludeFilter.class);

    public SelectiveTypeExcludeFilter(Set<Class<?>> scannedClasses, String basePackage) {
        this.scannedClasses = scannedClasses;
        this.basePackage = basePackage;
    }

    @Override
    public boolean match(MetadataReader metadataReader, MetadataReaderFactory metadataReaderFactory) {
        String className = metadataReader.getClassMetadata().getClassName();

        // 1. basePackage 소속이 아니다 = 스프링 기본 설정이거나 라이브러리다. 
        // -> 건드리지 않고 스프링이 알아서 띄우게 통과시킴 (false 반환)
        if (!className.startsWith(basePackage)) {
            return false;
        }

        // 2. basePackage 소속이다 = 내 코드다.
        // 스캐너가 찾아온 명단(scannedClasses)에 있는지 확인!
        boolean isRequired = scannedClasses.stream().anyMatch(c -> c.getName().equals(className));
        
        // 3. 명단에 없다면? -> 타겟 컴포넌트와 엮이지 않은 잉여 빈이다.
        // -> 컴포넌트 스캔에서 제외! (true 반환)
        return !isRequired;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SelectiveTypeExcludeFilter that = (SelectiveTypeExcludeFilter) o;
        return Objects.equals(scannedClasses, that.scannedClasses) &&
                Objects.equals(basePackage, that.basePackage);
    }

    @Override
    public int hashCode() {
        return Objects.hash(scannedClasses, basePackage);
    }
}