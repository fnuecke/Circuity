package li.cil.lib.api.serialization;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotate fields with this annotation to allow serializing the type declaring
 * the fields by serializing the values of such annotated fields. This should be
 * relatively self-explanatory for the most part, with one point to watch out for
 * explained in the following.
 * <p>
 * Example use:
 * <pre>
 * class A {
 *     &#64;Serialize
 *     public int counter;
 *
 *     &#64;Serialize
 *     public List&lt;String&gt; names;
 * }
 *
 * A a1 = new A();
 * a1.counter = 10;
 * a1.names.add("foo");
 * a1.names.add("bar");
 * NBTBase nbt = Serialization.get(world).serialize(a);
 *
 * A a2 = Serialization.get(world).deserialize(A.class, nbt);
 * System.out.println(a2.counter); // 10
 * System.out.println(a2.names.size()); // 2
 * </pre>
 * <p>
 * Fields annotated with this annotation must not be final. They will be assigned
 * a new value when the instance of the declaring type is deserialized (into).
 * This means no direct references should be kept to values of serializable fields,
 * to avoid the referencing field becoming invalid after deserialization.
 * <p>
 * If the value is only deserialized once, it is safe to keep direct references to
 * the value after the first deserialization. Typically the only critical case are
 * references to a default value created during construction of the declaring type,
 * or more generally, between and including construction and the first deserialization.
 * <p>
 * For example, <em>this will break</em>:
 * <pre>
 * class A {}
 *
 * class B {
 *     public A a;
 *
 *     public B(A a) { this.a = a; }
 * }
 *
 * class C {
 *     &#64;Serialize
 *     A a = new A();
 *
 *     B b = new B(a);
 * }
 * </pre>
 * After deserializing <code>C</code>, the field <code>a</code> will have been
 * assigned a new value, but the instance of <code>B</code> stored in <code>b</code>
 * will still reference the original instance.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface Serialize {
    /**
     * The name to use when storing the value.
     * <p>
     * Defaults to the field name. Useful in case there are multiple serialized
     * fields in the class hierarchy with the same name (which in this one's
     * strong opinion should never be the case anyway, and should be changed,
     * but hey).
     *
     * @return the name to store the field as.
     */
    String value() default "";
}
