package mg.itu.prom16.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation permettant d'associer une URL à une méthode ou une classe.
 */
@Target({ElementType.METHOD, ElementType.TYPE}) // Peut être utilisée sur les classes et les méthodes
@Retention(RetentionPolicy.RUNTIME) // L'annotation est disponible à l'exécution
public @interface Url {
    String value(); // Définit l'URL associée
}
