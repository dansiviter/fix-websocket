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

import javax.enterprise.context.ApplicationScoped;

import quickfix.LogFactory;
import quickfix.SessionID;
import uk.dansiviter.juli.LogProducer;

/**
 * Produces {@link Log} instances for injection.
 *
 * @author Daniel Siviter
 * @since v1.0 [13 Nov 2019]
 */
@ApplicationScoped
public class LogFactoryProducer implements LogFactory {

	@Override
	public quickfix.Log create(SessionID sessionId) {
		return new LogImpl(LogProducer.log(Log.class, "quickfix.Log:" + sessionId.toString()));
	}

	/**
	 *
	 * @author Daniel Siviterweld-junit5
	 * @since v1.0 [13 Nov 2019]
	 */
	private static class LogImpl implements quickfix.Log {
		private final Log log;

		LogImpl(Log log) {
			this.log = log;
		}

		@Override
		public void onIncoming(String message) {
			this.log.onIncoming(message);
		}

		@Override
		public void onOutgoing(String message) {
			this.log.onOutgoing(message);
		}

		@Override
		public void onEvent(String text) {
			this.log.onEvent(text);
		}

		@Override
		public void onErrorEvent(String text) {
			this.log.onErrorEvent(text);
		}

		@Override
		public void clear() {
			// NOOP
		}
	}
}
