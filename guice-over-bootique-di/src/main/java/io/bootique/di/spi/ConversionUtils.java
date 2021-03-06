package io.bootique.di.spi;

import java.lang.annotation.Annotation;

import com.google.inject.name.Named;
import com.google.inject.name.Names;
import io.bootique.di.TypeLiteral;

/**
 * Guice &lt;-&gt; Bootique DI conversion utils
 */
public final class ConversionUtils {

    private ConversionUtils() {
    }

    public static <T> io.bootique.di.Key<T> toBootiqueKey(com.google.inject.Key<T> key) {
        TypeLiteral<T> typeLiteral = toTypeLiteral(key.getTypeLiteral());
        if(key.getAnnotationType() != null) {
            if (key.getAnnotationType().equals(Named.class)) {
                String name = ((Named) key.getAnnotation()).value();
                return io.bootique.di.Key.get(typeLiteral, name);
            }
            if (key.getAnnotationType().equals(javax.inject.Named.class)) {
                String name = ((javax.inject.Named) key.getAnnotation()).value();
                return io.bootique.di.Key.get(typeLiteral, name);
            }
        }
        return io.bootique.di.Key.get(typeLiteral, key.getAnnotationType());
    }

    public static <T> io.bootique.di.Key<T> toBootiqueKey(com.google.inject.TypeLiteral<T> typeLiteral) {
        return io.bootique.di.Key.get(toTypeLiteral(typeLiteral));
    }

    public static <T> com.google.inject.Key<T> toGuiceKey(com.google.inject.TypeLiteral<T> typeLiteral, io.bootique.di.Key<T> bootiqueKey) {
        String name = bootiqueKey.getBindingName();
        if(name != null) {
            return com.google.inject.Key.get(typeLiteral, Names.named(name));
        }

        Class<? extends Annotation> annotationType = bootiqueKey.getBindingAnnotation();
        if(annotationType != null) {
            return com.google.inject.Key.get(typeLiteral, annotationType);
        }

        return com.google.inject.Key.get(typeLiteral);
    }

    public static <T> io.bootique.di.TypeLiteral<T> toTypeLiteral(com.google.inject.TypeLiteral<T> typeLiteral) {
        return io.bootique.di.TypeLiteral.of(typeLiteral.getType());
    }

}
