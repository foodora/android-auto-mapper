package de.foodora.automapper;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * An annotation to indicate the auto-parcel that the annotated class needs to be {@link android.os.Parcelable}
 *
 * <pre>
 * <code>
 * {@literal @}AutoParcel public abstract class Foo  {...}
 * </code>
 * </pre>
 */
@Target(ElementType.TYPE) // on class level
@Retention(RetentionPolicy.SOURCE)
public @interface AutoMapper {
    int version() default 0;
    Class<?> map() default void.class;
    String prefix() default "FD";
    String targetName() default "";
    boolean parcelable() default false;
}
