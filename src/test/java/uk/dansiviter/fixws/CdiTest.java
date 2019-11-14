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

import static org.awaitility.Awaitility.await;
import static org.junit.Assert.assertThat;

import java.io.IOException;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.enterprise.context.Dependent;
import javax.inject.Inject;
import javax.websocket.ClientEndpointConfig;
import javax.websocket.CloseReason;
import javax.websocket.DeploymentException;
import javax.websocket.Endpoint;
import javax.websocket.EndpointConfig;
import javax.websocket.MessageHandler;
import javax.websocket.OnError;
import javax.websocket.OnMessage;
import javax.websocket.OnOpen;
import javax.websocket.Session;
import javax.websocket.server.ServerEndpoint;

import org.glassfish.tyrus.client.ClientManager;
import org.glassfish.tyrus.server.Server;
import org.glassfish.tyrus.test.tools.TestContainer;
import org.hamcrest.Matchers;
import org.jboss.weld.junit5.EnableWeld;
import org.jboss.weld.junit5.WeldInitiator;
import org.jboss.weld.junit5.WeldSetup;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * @author Daniel Siviter
 * @since v1.0 [13 Nov 2019]
 */
@EnableWeld
public class CdiTest extends TestContainer {
	@WeldSetup
    public WeldInitiator weld = WeldInitiator.of(Service.class);

	private Server server;

	@BeforeEach
	public void before() throws DeploymentException {
		this.server = startServer(EchoEndpoint.class);
	}

	@Test
    public void testEcho() throws InterruptedException, IOException, DeploymentException {
		final ClientManager client = createClient();

		final AtomicBoolean messageFlag = new AtomicBoolean();
        final AtomicBoolean onOpenFlag = new AtomicBoolean();
        final AtomicBoolean onCloseFlag = new AtomicBoolean();

		final Session session = client.connectToServer(new Endpoint() {
			@Override
			public void onOpen(Session session, EndpointConfig EndpointConfig) {
				System.out.println("Client: " + session.getNegotiatedSubprotocol());
				try {
					session.addMessageHandler(new MessageHandler.Whole<String>() {
						@Override
						public void onMessage(String message) {
							if (message.equals("Do or do not, there is no try. (from your server)")) {
								messageFlag.set(true);
							} else if (message.equals("onOpen")) {
								onOpenFlag.set(true);
							}
						}
					});

					session.getBasicRemote().sendText("Do or do not, there is no try.");
				} catch (IOException e) {
					throw new RuntimeException(e);
				}
			}

			@Override
			public void onClose(Session session, CloseReason closeReason) {
				onCloseFlag.set(true);
			}
		},
				ClientEndpointConfig.Builder.create().preferredSubprotocols(Arrays.asList("v12.stomp", "v10.stomp")).build(),
				getURI(EchoEndpoint.class));

		await().untilAsserted(() -> assertThat(messageFlag.get(), Matchers.is(true)));
		await().untilAsserted(() -> assertThat(onOpenFlag.get(), Matchers.is(true)));
		await().untilAsserted(() -> assertThat(onCloseFlag.get(), Matchers.is(true)));

		session.close();
	}

	@AfterEach
	public void after() {
		this.server.stop();
	}

	/**
	 *
	 */
	@ServerEndpoint(value = "/echo", subprotocols = { "v12.stomp", "v11.stomp", "v10.stomp" })
	public static class EchoEndpoint {
		@Inject
		private Service service;

		@OnOpen
		public void onOpen(Session session) throws IOException {
			System.out.println("Server: " + session.getNegotiatedSubprotocol());
			session.getBasicRemote().sendText("onOpen");
		}

		@OnMessage
		public void echo(Session session, String message) throws IOException {
			this.service.echo(session, message);
		}

		@OnError
		public void onError(Throwable t) {
			t.printStackTrace();
		}
	}

	@Dependent
	public static class Service {
		public void echo(Session session, String message) throws IOException {
			session.getBasicRemote().sendText(message + " (from your server)");
			session.close();
		}
	}
}
