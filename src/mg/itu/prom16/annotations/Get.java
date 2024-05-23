package mg.itu.prom16.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

// Définir l'annotation
@Target(ElementType.METHOD) // L'annotation peut être appliquée aux méthodes
@Retention(RetentionPolicy.RUNTIME) // L'annotation est disponible à l'exécution
public @interface Get {
    String url();
}