package net.bluemind.startup.services;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.SOURCE)
@Target({ ElementType.FIELD })
public @interface InjectedIn {

	Class<? extends BmDependencyActivator> value();

}
