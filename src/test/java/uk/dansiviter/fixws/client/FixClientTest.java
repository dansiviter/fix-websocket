/*
 * Copyright 2021 Daniel Siviter
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
package uk.dansiviter.fixws.client;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.hamcrest.MatcherAssert.assertThat;
import static uk.dansiviter.fixws.FixUtil.sessionId;
import static uk.dansiviter.fixws.FixUtil.setReverse;

import java.util.concurrent.LinkedTransferQueue;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Event;
import javax.enterprise.event.Observes;
import javax.inject.Inject;

import org.hamcrest.Matchers;
import org.jboss.weld.junit5.WeldInitiator;
import org.jboss.weld.junit5.WeldSetup;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import quickfix.FieldNotFound;
import quickfix.IncorrectDataFormat;
import quickfix.IncorrectTagValue;
import quickfix.Message;
import quickfix.SessionID;
import quickfix.UnsupportedMessageType;
import quickfix.field.Headline;
import quickfix.field.Text;
import quickfix.fix50.News;
import quickfix.fix50.News.NoLinesOfText;
import uk.dansiviter.fixws.AbstractTest;
import uk.dansiviter.fixws.FixApplication;
import uk.dansiviter.fixws.LogFactoryProducer;
import uk.dansiviter.fixws.MessageStoreFactoryProducer;
import uk.dansiviter.fixws.Metrics;
import uk.dansiviter.fixws.SessionFactoryProducer;
import uk.dansiviter.fixws.SessionProviderProducer;
import uk.dansiviter.fixws.SessionSettingsProducer;
import uk.dansiviter.fixws.annotations.FromApp;
import uk.dansiviter.fixws.annotations.ToApp;
import uk.dansiviter.juli.cdi.LogExtension;

public class FixClientTest extends AbstractTest {
	@WeldSetup
	public WeldInitiator weld = WeldInitiator.of(
		LogExtension.class,
		LogFactoryProducer.class,
		SessionProviderProducer.class,
		SessionFactoryProducer.class,
		FixApplication.class,
		SessionSettingsProducer.class,
		MessageStoreFactoryProducer.class,
		TestHandler.class,
		Metrics.class);

	@BeforeAll
	public static void beforeAll() {
		System.setProperty("java.util.logging.SimpleFormatter.format", "%1$tFT%1$tT.%1$tL%1$tz %3$s%n%4$s: %5$s%6$s%n");
	}

	@Test
	void test() throws Exception {
		var queue = new LinkedTransferQueue<Message>();

		var container = createClient();
		var client = new FixClient(container, getURI("/fix"), getClass().getResourceAsStream("/client.qfxj")) {
			@Override
			public void onLogon(quickfix.SessionID sessionId) {
				send(news("Howdy", "foo"));
			}

			@Override
			public void fromApp(Message message, SessionID sessionId)
					throws FieldNotFound, IncorrectDataFormat, IncorrectTagValue, UnsupportedMessageType {
				queue.add(message);
			}
		};

		client.start();

		var news = queue.poll(5, SECONDS);
		assertThat(news, Matchers.isA(News.class));

		client.close();
	}

	private static News news(String headline, String... lines) {
		var news = new News(new Headline(headline));
		for (var line : lines) {
			var text = new NoLinesOfText();
			text.set(new Text(line));
			news.addGroup(text);
		}
		return news;
	}

	/**
	 *
	 */
	@ApplicationScoped
	public static class TestHandler {
		@Inject @ToApp
		private Event<Message> evt;

		public void on(@Observes @FromApp News req) {
			var sessionId = sessionId(req);
			evt.fire(setReverse(sessionId, news("World", "Hello")));
		}
	}
}
