package li.cil.lib.api.serialization;

import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marker annotation for types containing {@link Serialize}s fields.
 * <p>
 * The main purpose of this interface is to avoid having to perform a
 * deep inspection of every type when determining whether it has
 * serialized fields.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Inherited
public @interface Serializable {
}
