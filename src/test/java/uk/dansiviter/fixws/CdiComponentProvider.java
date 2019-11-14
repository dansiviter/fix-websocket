/*
 * Copyright 2016-2019 Daniel Siviter
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package uk.dansiviter.fixws;

import java.lang.annotation.Annotation;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.enterprise.context.spi.CreationalContext;
import javax.enterprise.inject.spi.AnnotatedType;
import javax.enterprise.inject.spi.BeanManager;
import javax.enterprise.inject.spi.CDI;
import javax.enterprise.inject.spi.InjectionTarget;

import org.glassfish.tyrus.core.ComponentProvider;

/**
 * @author Daniel Siviter
 * @since v1.0 [13 Nov 2019]
 */
public class CdiComponentProvider extends ComponentProvider {
	private final BeanManager beanManager = CDI.current().getBeanManager();;

	private final Map<Object, CdiInjectionContext<?>> cdiBeanToContext = new ConcurrentHashMap<>();

	@Override
	public boolean isApplicable(Class<?> c) {
		Annotation[] annotations = c.getAnnotations();

		for (Annotation annotation : annotations) {
			String annotationClassName = annotation.annotationType().getCanonicalName();
			if (annotationClassName.equals("javax.ejb.Singleton") || annotationClassName.equals("javax.ejb.Stateful")
					|| annotationClassName.equals("javax.ejb.Stateless")) {
				return false;
			}
		}

		return true;
	}

	@Override
	public <T> Object create(Class<T> c) {
		synchronized (beanManager) {
			T managedObject;
			AnnotatedType<T> annotatedType = beanManager.createAnnotatedType(c);
			InjectionTarget<T> it = beanManager.createInjectionTarget(annotatedType);
			CreationalContext<T> cc = beanManager.createCreationalContext(null);
			managedObject = (T) it.produce(cc);
			it.inject(managedObject, cc);
			it.postConstruct(managedObject);
			cdiBeanToContext.put(managedObject, new CdiInjectionContext<T>(it, cc, managedObject));

			return managedObject;
		}
	}

	@Override
	public boolean destroy(Object o) {
		// if the object is not in map, nothing happens
		if (cdiBeanToContext.containsKey(o)) {
			cdiBeanToContext.remove(o).destory();
			return true;
		}
		return false;
	}

	private static class CdiInjectionContext<T> {
		final InjectionTarget<T> it;
		final CreationalContext<T> cc;
		final T instance;

		CdiInjectionContext(InjectionTarget<T> it, CreationalContext<T> cc, T instance) {
			this.it = it;
			this.cc = cc;
			this.instance = instance;
		}

		public void destory() {
			it.preDestroy(instance);
			it.dispose(instance);
			cc.release();
		}
	}
}
