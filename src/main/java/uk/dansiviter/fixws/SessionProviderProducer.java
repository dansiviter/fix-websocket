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

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Produces;
import javax.inject.Inject;

import quickfix.FixVersions;
import quickfix.SessionFactory;
import quickfix.SessionID;
import quickfix.SessionSettings;

/**
 * @author Daniel Siviter
 * @since v1.0 [13 Nov 2019]
 */
@ApplicationScoped
public class SessionProviderProducer {
	@Inject
	private SessionSettings sessionSettings;
	@Inject
	private SessionFactory sessionFactory;

	@Produces @ApplicationScoped
	public SessionProvider sessionProvider() {
		return new DynamicSessionProvider(
				this.sessionSettings,
				new SessionID(FixVersions.BEGINSTRING_FIXT11, "*", "*"),
				this.sessionFactory);
	}
}
