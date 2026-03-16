package io.github.sctf.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface TargetComponent {

    Class<?>[] value();

    boolean includeParents() default true; // 기본값은 true로 설정합니다[cite: 25].

}
