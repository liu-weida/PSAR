package annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 *  Marque une fonction de commande pour lecture par réflexion.
 */
@Retention(RetentionPolicy.RUNTIME) // L'annotation au moment de l'exécution
@Target(ElementType.METHOD) // L'annotation peut seulement être appliquée aux méthodes
public @interface CommandMethod {
}
