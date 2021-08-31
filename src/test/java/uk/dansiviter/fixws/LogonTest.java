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

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.hamcrest.MatcherAssert.assertThat;
import static uk.dansiviter.fixws.FixUtil.sessionId;
import static uk.dansiviter.fixws.FixUtil.setReverse;

import java.io.IOException;
import java.util.concurrent.LinkedTransferQueue;
import java.util.concurrent.TransferQueue;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Event;
import javax.enterprise.event.Observes;
import javax.inject.Inject;
import javax.websocket.DeploymentException;
import javax.websocket.EncodeException;
import javax.websocket.Endpoint;
import javax.websocket.EndpointConfig;
import javax.websocket.MessageHandler.Whole;
import javax.websocket.Session;

import org.hamcrest.Matchers;
import org.jboss.weld.junit5.EnableWeld;
import org.jboss.weld.junit5.WeldInitiator;
import org.jboss.weld.junit5.WeldSetup;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import quickfix.FixVersions;
import quickfix.InvalidMessage;
import quickfix.Message;
import quickfix.SessionID;
import quickfix.field.DefaultApplVerID;
import quickfix.field.EncryptMethod;
import quickfix.field.Headline;
import quickfix.field.HeartBtInt;
import quickfix.field.ResetSeqNumFlag;
import quickfix.field.Text;
import quickfix.fix50.News;
import quickfix.fix50.News.NoLinesOfText;
import quickfix.fixt11.Logon;
import uk.dansiviter.fixws.annotations.FromApp;
import uk.dansiviter.fixws.annotations.ToApp;
import uk.dansiviter.juli.cdi.LogExtension;

/**
 * @author Daniel Siviter
 * @since v1.0 [13 Nov 2019]
 */
@EnableWeld
public class LogonTest extends AbstractTest {
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

	private final TransferQueue<Message> queue = new LinkedTransferQueue<Message>();

	@BeforeAll
	public static void beforeAll() {
		System.setProperty("java.util.logging.SimpleFormatter.format", "%1$tTT%1$tF%1$tz %3$s%n%4$s: %5$s%6$s%n");
	}

	@Test
	public void test() throws DeploymentException, IOException, InterruptedException, InvalidMessage, EncodeException {
		var client = createClient();

		var session = client.connectToServer(new Endpoint() {
			@Override
			public void onOpen(Session session, EndpointConfig config) {
				try {
					session.addMessageHandler(Message.class, new Whole<Message>() {
						@Override
						public void onMessage(Message message) {
							queue.add(message);
						}
					});
					session.getBasicRemote().sendObject(defaults(logon()));
				} catch (Exception e) {
					throw new IllegalStateException(e);
				}
			}

			@Override
			public void onError(Session session, Throwable thr) {
				thr.printStackTrace();
			}
		}, clientConfig(FixVersions.FIX50), getURI("/fix"));

		var logon = queue.poll(5, SECONDS);
		assertThat(logon, Matchers.isA(Logon.class));
		session.getBasicRemote().sendObject(defaults(news("Howdy", "foo")));

		var snapshot = queue.poll(5, SECONDS);
		assertThat(snapshot, Matchers.isA(News.class));

		session.close();
	}


	// --- Static Methods ---

	private static Logon logon() {
		final Logon logon = new Logon();
		logon.set(new HeartBtInt(30));
		logon.set(new ResetSeqNumFlag(true));
		logon.set(new DefaultApplVerID("9"));
		logon.set(new EncryptMethod(0));
		return logon;
	}

	private static News news(String headline, String... lines) {
		final News news = new News(new Headline(headline));
		for (String line : lines) {
			final NoLinesOfText text = new NoLinesOfText();
			text.set(new Text(line));
			news.addGroup(text);
		}
		return news;
	}


	// --- Inner Classes ---

	/**
	 *
	 */
	@ApplicationScoped
	public static class TestHandler {
		@Inject @ToApp
		private Event<Message> evt;

		public void on(@Observes @FromApp News req) {
			final SessionID sessionId = sessionId(req);
			evt.fire(setReverse(sessionId, news("World", "Hello")));
		}
	}
}
