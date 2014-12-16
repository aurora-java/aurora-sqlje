package aurora.sqlje.core.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface Table {
	/**
	 * table name
	 * 
	 * @return
	 */
	String name();

	/**
	 * sequence that use to generate primary key
	 * @return
	 */
	String sequence() default "";

	/**
	 * is standard who enabled,default true
	 * 
	 * @return
	 */
	boolean stdwho() default true;
}
