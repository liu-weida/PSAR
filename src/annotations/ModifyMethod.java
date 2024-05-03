package annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marqueur pour les instructions de modification, utilisé pour la recherche par réflexion.
 */
@Retention(RetentionPolicy.RUNTIME) // Conserve l'annotation à l'exécution.
@Target(ElementType.METHOD) // L'annotation ne peut être appliquée qu'aux méthodes.
public @interface ModifyMethod {
}
