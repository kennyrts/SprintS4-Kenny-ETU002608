package mg.itu.prom16.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

// Define the Auth annotation
@Target({ElementType.METHOD, ElementType.TYPE}) // The annotation can be applied to methods and classes
@Retention(RetentionPolicy.RUNTIME) // The annotation is available at runtime
public @interface Auth {
    String value(); // The required role
} 