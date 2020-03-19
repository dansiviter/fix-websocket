/*
 * Copyright 2019-2020 Daniel Siviter
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

import static javax.enterprise.inject.spi.CDI.current;

import org.glassfish.tyrus.core.ComponentProvider;

/**
 * @author Daniel Siviter
 * @since v1.0 [13 Nov 2019]
 */
public class CdiComponentProvider extends ComponentProvider {
	@Override
	public boolean isApplicable(Class<?> c) {
		return !current().getBeanManager().getBeans(c).isEmpty();
	}

	@Override
	public <T> Object create(Class<T> c) {
		return current().select(c).get();
	}

	@Override
	public boolean destroy(Object o) {
		try {
			current().destroy(o);
			return true;
		} catch (UnsupportedOperationException | IllegalStateException e) {
			return false;
		}
	}
}
