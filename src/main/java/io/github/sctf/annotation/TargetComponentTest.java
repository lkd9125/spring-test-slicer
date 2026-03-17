package io.github.sctf.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.test.context.BootstrapWith;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import io.github.sctf.bootstrap.SelectiveContextBootstrapper;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@ExtendWith(SpringExtension.class)
@BootstrapWith(SelectiveContextBootstrapper.class)
public @interface TargetComponentTest {

    boolean withDatabase() default true;

    String basePackage();

}
