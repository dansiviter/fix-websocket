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

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static uk.dansiviter.fixws.annotations.FromApp.Literal.fromApp;
import static uk.dansiviter.fixws.annotations.MsgType.Literal.msgType;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.inject.Inject;

import org.jboss.weld.junit5.EnableWeld;
import org.jboss.weld.junit5.WeldInitiator;
import org.jboss.weld.junit5.WeldSetup;
import org.junit.jupiter.api.Test;

import quickfix.Message;
import quickfix.fix50.MarketDataRequest;
import quickfix.fix50.NewOrderSingle;
import uk.dansiviter.fixws.annotations.FromApp;
import uk.dansiviter.fixws.annotations.MsgType;

/**
 * @author Daniel Siviter
 * @since v1.0 [13 Nov 2019]
 */
@EnableWeld
public class FromAppTest {
	@WeldSetup
	public WeldInitiator weld = WeldInitiator.of(Service.class);

	@Inject
	private Service service;

	@Test
	public void typeSafe() throws InterruptedException {
		final Message message = new MarketDataRequest();

		assertNull(service.mdr());

		this.weld.event().select(fromApp()).fire(message);

		assertNotNull(service.mdr());
	}

	@Test
	public void messageType() {
		final Message message = new NewOrderSingle();

		assertNull(service.nos());

		weld.event().select(fromApp(), msgType(NewOrderSingle.MSGTYPE)).fire(message);

		final Service service = weld.select(Service.class).get();
		assertNotNull(service.nos());
	}

	// --- Inner Classes ---

	@ApplicationScoped
	public static class Service {
		private MarketDataRequest mdr;
		private Message nos;

		public void on(@Observes @FromApp MarketDataRequest mdr) {
			this.mdr = mdr;
		}

		public void on(@Observes @FromApp @MsgType(NewOrderSingle.MSGTYPE) Message nos) {
			this.nos = nos;
		}

		/**
		 * @return the mdr
		 */
		public MarketDataRequest mdr() {
			return mdr;
		}

		/**
		 * @return the nos
		 */
		public Message nos() {
			return nos;
		}
	}
}
