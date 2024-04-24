package com.gerbugy.springframework.web.request;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.core.annotation.AliasFor;

@Target({ ElementType.TYPE, ElementType.PARAMETER })
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RequestMap {

    @AliasFor("allowStrip")
    boolean value() default true;

    @AliasFor("value")
    boolean allowStrip() default true;

    boolean allowDistinct() default true;
}