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

import static java.util.Collections.emptySet;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import javax.websocket.ClientEndpointConfig;
import javax.websocket.DecodeException;
import javax.websocket.Decoder;
import javax.websocket.DeploymentException;
import javax.websocket.EncodeException;
import javax.websocket.Encoder;
import javax.websocket.Endpoint;
import javax.websocket.EndpointConfig;
import javax.websocket.server.ServerApplicationConfig;
import javax.websocket.server.ServerEndpointConfig;

import org.glassfish.tyrus.server.Server;
import org.glassfish.tyrus.test.tools.TestContainer;
import org.jboss.weld.junit5.EnableWeld;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;

import quickfix.DefaultMessageFactory;
import quickfix.FixVersions;
import quickfix.InvalidMessage;
import quickfix.Message;
import quickfix.MessageFactory;
import quickfix.Message.Header;
import quickfix.MessageUtils;
import quickfix.field.ApplVerID;
import quickfix.field.MsgSeqNum;
import quickfix.field.SenderCompID;
import quickfix.field.SendingTime;
import quickfix.field.TargetCompID;

/**
 * @author Daniel Siviter
 * @since v1.0 [13 Nov 2019]
 */
@EnableWeld
public abstract class AbstractTest extends TestContainer {
	protected final AtomicInteger msgSeqNum = new AtomicInteger();
	protected Server server;

	@BeforeEach
	public void before() throws DeploymentException {
		this.server = startServer(TestConfig.class);
	}

	@AfterEach
	public void after() {
		this.server.stop();
	}

	// --- Static Methods ---

	/**
	 *
	 * @param <M>
	 * @param message
	 * @return
	 */
	protected <M extends Message> M defaults(M message) {
		return defaults(message, "client", "server");
	}

	/**
	 *
	 * @param <M>
	 * @param message
	 * @param senderCompId
	 * @param targetCompId
	 * @return
	 */
	protected <M extends Message> M defaults(M message, String senderCompId, String targetCompId) {
		final Header header = message.getHeader();
		header.setField(new MsgSeqNum(this.msgSeqNum.incrementAndGet()));
		header.setField(new SendingTime(LocalDateTime.now()));
		header.setField(new SenderCompID(senderCompId));
		header.setField(new TargetCompID(targetCompId));
		header.setField(new ApplVerID(ApplVerID.FIX50));
		return message;
	}

	/**
	 *
	 * @param subprotocols
	 * @return
	 */
	public static ClientEndpointConfig clientConfig(String... subprotocols) {
		return ClientEndpointConfig.Builder.create().preferredSubprotocols(FixEndpoint.subprotocols(subprotocols))
				.decoders(List.of(Encoding.class))
				.encoders(List.of(Encoding.class))
				.build();
	}


	// --- Inner Classes ---

	/**
	 *
	 */
	public static class TestConfig implements ServerApplicationConfig {
		@Override
		public Set<ServerEndpointConfig> getEndpointConfigs(Set<Class<? extends Endpoint>> endpointClasses) {
			return Set.of(FixEndpoint.config("/fix", List.of(FixVersions.FIX50)));
		}

		@Override
		public Set<Class<?>> getAnnotatedEndpointClasses(Set<Class<?>> scanned) {
			return emptySet();
		}
	}

	/**
	 *
	 */
	public static class Encoding implements Decoder.Text<Message>, Encoder.Text<Message> {
		private final MessageFactory messageFactory = new DefaultMessageFactory(ApplVerID.FIX50);

		@Override
		public void init(EndpointConfig config) {
		}

		@Override
		public void destroy() {
		}

		@Override
		public Message decode(String s) throws DecodeException {
			try {
				return MessageUtils.parse(messageFactory, null, s);
			} catch (InvalidMessage e) {
				throw new DecodeException(s, e.getLocalizedMessage(), e);
			}
		}

		@Override
		public boolean willDecode(String s) {
			return true;
		}

		@Override
		public String encode(Message object) throws EncodeException {
			return object.toString();
		}
	}
}
