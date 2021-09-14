/*
 * Copyright 2019-2021 Daniel Siviter
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

import static uk.dansiviter.juli.LogProducer.log;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Produces;

import quickfix.LogFactory;

/**
 * Produces {@link Log} instances for injection.
 *
 * @author Daniel Siviter
 * @since v1.0 [13 Nov 2019]
 */
public class LogFactoryProducer {
	@Produces @ApplicationScoped
	public static LogFactory logFactory() {
		return id -> log(Log.class, "quickfix:" + id.toString());
	}
}
