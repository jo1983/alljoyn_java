package org.alljoyn.bus.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Bus Methods, Signals, Properties and Interfaces can all have Annotations,
 * which have a name and a value
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.FIELD, ElementType.METHOD, ElementType.TYPE})
public @interface BusAnnotations {

    BusAnnotation[] value();
}
