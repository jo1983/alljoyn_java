package org.alljoyn.bus.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Bus Methods, Signals, Properties and Interfaces can all have Annotations,
 * which have a name and a value
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
public @interface BusAnnotation {

    /**
     * Override of method name.
     */
    String name();

    /**
     * Override of method value
     * The default value of the annotation
     */
    String value();
}
