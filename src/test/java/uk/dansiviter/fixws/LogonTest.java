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

import static uk.dansiviter.fixws.FixUtil.setReverse;
import static uk.dansiviter.fixws.FixUtil.sessionId;

import static org.junit.Assert.assertThat;

import java.io.IOException;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.TimeUnit;

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

import org.glassfish.tyrus.client.ClientManager;
import org.hamcrest.Matchers;
import org.jboss.weld.junit5.EnableWeld;
import org.jboss.weld.junit5.WeldInitiator;
import org.jboss.weld.junit5.WeldSetup;
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

/**
 * @author Daniel Siviter
 * @since v1.0 [13 Nov 2019]
 */
@EnableWeld
public class LogonTest extends AbstractTest {
	@WeldSetup
	public WeldInitiator weld = WeldInitiator.of(
		LogProducer.class,
		SessionProviderProducer.class,
		SessionFactoryProducer.class,
		FixApplication.class,
		SessionSettingsProducer.class,
		MessageStoreFactoryProducer.class,
		TestHandler.class);

	private SynchronousQueue<Message> queue = new SynchronousQueue<Message>();

	@Test
	public void test() throws DeploymentException, IOException, InterruptedException, InvalidMessage, EncodeException {
		final ClientManager client = createClient();

		final Session session = client.connectToServer(new Endpoint() {
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

		final Message logon = queue.poll(5, TimeUnit.MINUTES);
		assertThat(logon, Matchers.isA(Logon.class));
		session.getBasicRemote().sendObject(defaults(news("Howdy", "foo")));

		final Message snapshot = queue.poll(5, TimeUnit.MINUTES);
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
