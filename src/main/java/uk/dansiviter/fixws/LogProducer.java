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

import java.util.Map;
import java.util.WeakHashMap;

import javax.annotation.Nonnull;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.context.Dependent;
import javax.enterprise.inject.Produces;
import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.InjectionPoint;

import org.jboss.logging.Logger;

import quickfix.LogFactory;
import quickfix.SessionID;

/**
 * Produces {@link Log} instances for injection.
 *
 * @author Daniel Siviter
 * @since v1.0 [13 Nov 2019]
 */
@ApplicationScoped
public class LogProducer implements LogFactory {
	private static final Map<String, Log> LOGS = new WeakHashMap<>();

	/**
	 *
	 * @param ip the injection point.
	 * @return the log instance.
	 */
	@Produces
	@Dependent
	public static Log log(InjectionPoint ip) {
		final Bean<?> bean = ip.getBean();
		final Class<?> cls = bean != null ? bean.getBeanClass() : ip.getMember().getDeclaringClass();
		return log(cls);
	}

	/**
	 * @param cls the class to create the logger for.
	 * @return the logger instance.
	 */
	public static Log log(@Nonnull Class<?> cls) {
		return log(cls.getName());
	}

	/**
	 * @param name the name to create the logger for.
	 * @return the logger instance.
	 */
	public static Log log(@Nonnull String name) {
		return LOGS.computeIfAbsent(name, k -> Logger.getMessageLogger(Log.class, k));
	}

	@Override
	public quickfix.Log create(SessionID sessionId) {
		return log("quickfix.Log:" + sessionId.toString());
	}
}
