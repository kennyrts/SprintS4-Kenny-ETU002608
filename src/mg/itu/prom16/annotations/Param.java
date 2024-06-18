package mg.itu.prom16.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

// Définition de l'annotation
@Retention(RetentionPolicy.RUNTIME) // Annotation disponible au moment de l'exécution
@Target(ElementType.PARAMETER) // Annotation applicable aux paramètres de méthode
public @interface Param {
    String name();
}
