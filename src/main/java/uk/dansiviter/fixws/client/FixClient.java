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
package uk.dansiviter.fixws.client;

import static java.lang.String.format;
import static java.util.Objects.requireNonNull;
import static quickfix.Initiator.SETTING_DYNAMIC_SESSION;
import static quickfix.Session.sendToTarget;
import static uk.dansiviter.juli.LogProducer.log;

import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;

import javax.websocket.CloseReason;
import javax.websocket.DeploymentException;
import javax.websocket.Endpoint;
import javax.websocket.EndpointConfig;
import javax.websocket.Session;
import javax.websocket.WebSocketContainer;

import quickfix.ApplicationAdapter;
import quickfix.ConfigError;
import quickfix.DefaultSessionFactory;
import quickfix.FieldConvertError;
import quickfix.FieldNotFound;
import quickfix.FileStoreFactory;
import quickfix.IncorrectDataFormat;
import quickfix.IncorrectTagValue;
import quickfix.InvalidMessage;
import quickfix.LogFactory;
import quickfix.Message;
import quickfix.MessageStoreFactory;
import quickfix.MessageUtils;
import quickfix.RejectLogon;
import quickfix.Responder;
import quickfix.RuntimeError;
import quickfix.SessionFactory;
import quickfix.SessionID;
import quickfix.SessionNotFound;
import quickfix.SessionSettings;
import quickfix.UnsupportedMessageType;
import quickfix.field.ApplVerID;
import quickfix.field.DefaultApplVerID;
import quickfix.field.MsgType;
import uk.dansiviter.juli.LogProducer;

/**
 * Client implementation that leverages WebSockets
 *
 * @see quickfix.mina.initiator.InitiatorIoHandler
 */
public abstract class FixClient extends ApplicationAdapter implements Closeable {
	private final ClientLog log = LogProducer.log(ClientLog.class);

	private final WebSocketContainer container;
	private final URI uri;
	private final SessionSettings settings;
	private final MessageStoreFactory storeFactory;
	private final LogFactory logFactory;
	private final SessionFactory sessionFactory;

	private Session session;
	private quickfix.Session qfSession;

	public FixClient(WebSocketContainer container, URI uri, File fixSettings) throws IOException, ConfigError {
		this(container, uri, new FileInputStream(fixSettings));
	}

	public FixClient(WebSocketContainer container, URI uri, InputStream fixSettings) throws ConfigError {
		this.container = requireNonNull(container);
		this.uri = requireNonNull(uri);
		this.settings = new SessionSettings(fixSettings);
		this.storeFactory = new FileStoreFactory(requireNonNull(settings));
		this.logFactory = id -> log(ClientLog.class, "client.quickfix:" + id.toString());
		this.sessionFactory = new DefaultSessionFactory(this, requireNonNull(storeFactory), requireNonNull(logFactory));
	}

	/**
	 *
	 * @throws RuntimeError
	 * @throws ConfigError
	 * @throws FieldConvertError
	 * @throws DeploymentException
	 * @throws IOException
	 */
	public void start() throws RuntimeError, ConfigError, FieldConvertError, DeploymentException, IOException {
		for (var i = settings.sectionIterator(); i.hasNext();) {
			var sessionId = i.next();
			if (isInitiatorSession(this.settings, sessionId)) {
				if (this.qfSession != null) {
					throw new ConfigError("More than one initiator session!");
				}
				try {
					if (!settings.isSetting(sessionId, SETTING_DYNAMIC_SESSION)
							|| !settings.getBool(sessionId, SETTING_DYNAMIC_SESSION)) {
						this.qfSession = sessionFactory.create(sessionId, settings);
					}
				} catch (Throwable e) {
					throw e instanceof ConfigError ? (ConfigError) e : new ConfigError("error during session initialization", e);
				}
			}
		}

		this.session = container.connectToServer(new EndpointImpl(), this.uri);
		this.qfSession.setResponder(new WsResponder(this.session));
		this.qfSession.next();  // logon
	}

	/**
	 *
	 * @param msg
	 */
	public void send(Message msg) {
		var id = this.qfSession.getSessionID();
		try {
			sendToTarget(msg, id);
		} catch (SessionNotFound e) {
			this.log.sessionNotFound(id, e);
		}
	}

	@Override
	public void close() throws IOException {
		this.qfSession.close();
		this.session.close();
		this.qfSession = null;
		this.session = null;
	}

	private void processMessage(String message) {
		var qfSession = this.qfSession;
		var sessionLog = qfSession.getLog();
		try {
			processMessage(MessageUtils.parse(qfSession, message));
		} catch (InvalidMessage e) {
			if (qfSession.isRejectGarbledMessage()) {
				var fixMessage = e.getFixMessage();
				if (fixMessage != null) {
					sessionLog.onErrorEvent("Processing garbled message: " + e.getMessage());
					processMessage(fixMessage);
					return;
				}
			}
		}
	}

	private void processMessage(Message message) {
		try {
			var sessionID = MessageUtils.getReverseSessionID(message);
			var session = quickfix.Session.lookupSession(sessionID);
			var msgTypeField = message.getHeader().getOptionalString(MsgType.FIELD);
			if (msgTypeField.isPresent() && msgTypeField.get().equals(MsgType.LOGON)) {
				if (sessionID.isFIXT()) {
					if (message.isSetField(DefaultApplVerID.FIELD)) {
						var applVerID = new ApplVerID(message.getString(DefaultApplVerID.FIELD));
						session.setTargetDefaultApplicationVersionID(applVerID);
						session.getLog().onEvent(
								format("Setting DefaultApplVerID (%s=%s) from Logon", DefaultApplVerID.FIELD, applVerID.getValue()));
					}
				}
			}
			session.next(message);
		} catch (FieldNotFound | RejectLogon | IncorrectDataFormat | IncorrectTagValue | UnsupportedMessageType | IOException | InvalidMessage e) {
			throw new IllegalStateException(e);
		}
	}

	private static boolean isInitiatorSession(SessionSettings settings, SessionID sessionId)
			throws ConfigError, FieldConvertError {
		return !settings.isSetting(sessionId, SessionFactory.SETTING_CONNECTION_TYPE)
				|| settings.getString(sessionId, SessionFactory.SETTING_CONNECTION_TYPE).equals("initiator");
	}

	private class EndpointImpl extends Endpoint {
		@Override
		public void onOpen(Session session, EndpointConfig config) {
			session.addMessageHandler(String.class, FixClient.this::processMessage);
		}

		@Override
		public void onClose(Session session, CloseReason closeReason) {
			log.onClose(session.getId(), closeReason.getCloseCode().getCode(), closeReason.getReasonPhrase());
		}

		@Override
		public void onError(Session session, Throwable t) {
			log.onError(session.getId(), t);
		}
	}

	private class WsResponder implements Responder {
		private final Session session;

		WsResponder(Session session) {
			this.session = requireNonNull(session);
		}

		@Override
		public boolean send(String data) {
			try {
				this.session.getBasicRemote().sendText(data);
				return true;
			} catch (IOException e) {
				e.printStackTrace();
				// log.send(session.getId(), e);
				return false;
			}
		}

		@Override
		public void disconnect() {
			try {
				this.session.close();
			} catch (IOException e) {
				e.printStackTrace();
				// log.close(session.getId(), e);
			}
		}

		@Override
		public String getRemoteAddress() {
			// no cross platform way of doing this!
			return null;
		}
	}
}
