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

import static javax.enterprise.inject.spi.CDI.current;
import static quickfix.MessageUtils.getReverseSessionID;
import static quickfix.MessageUtils.isLogon;
import static quickfix.MessageUtils.parse;
import static quickfix.mina.SessionConnector.QF_SESSION;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.websocket.CloseReason;
import javax.websocket.Endpoint;
import javax.websocket.EndpointConfig;
import javax.websocket.MessageHandler.Whole;
import javax.websocket.Session;
import javax.websocket.server.ServerEndpointConfig;
import javax.websocket.server.ServerEndpointConfig.Builder;

import quickfix.FieldNotFound;
import quickfix.IncorrectDataFormat;
import quickfix.IncorrectTagValue;
import quickfix.InvalidMessage;
import quickfix.Message;
import quickfix.RejectLogon;
import quickfix.Responder;
import quickfix.SessionID;
import quickfix.UnsupportedMessageType;
import quickfix.field.ApplVerID;
import quickfix.field.DefaultApplVerID;
import quickfix.field.HeartBtInt;
import quickfix.field.MsgType;
import quickfix.mina.SessionConnector;

/**
 * @author Daniel Siviter
 * @since v1.0 [13 Nov 2019]
 * @see quickfix.mina.acceptor.AcceptorIoHandler
 */
public class FixEndpoint extends Endpoint {
	private final Log log = LogProducer.log(FixEndpoint.class);

	private SessionProvider sessionProvider;

	@Override
	public void onOpen(Session session, EndpointConfig config) {
		// non-annotated Endpoints do not get injection!
		this.sessionProvider = current().select(SessionProvider.class).get();

		this.log.onOpen(session.getId());
		session.addMessageHandler(String.class, new Whole<String>() {
			@Override
			public void onMessage(String message) {
				try {
					on(message, session);
				} catch (IOException | FieldNotFound | RejectLogon | IncorrectDataFormat | IncorrectTagValue
						| UnsupportedMessageType | InvalidMessage e) {
					throw new IllegalStateException(e);
				}
			}
		});
	}

	public void on(String msgStr, Session session)
			throws IOException, FieldNotFound, RejectLogon,
			IncorrectDataFormat, IncorrectTagValue, UnsupportedMessageType, InvalidMessage
	{
		SessionID remoteSessionID = getReverseSessionID(msgStr);
		quickfix.Session quickFixSession = findQFSession(session, remoteSessionID);
        if (quickFixSession != null) {
            final boolean rejectGarbledMessage = quickFixSession.isRejectGarbledMessage();
            final quickfix.Log sessionLog = quickFixSession.getLog();
            sessionLog.onIncoming(msgStr);
            try {
                Message fixMessage = parse(quickFixSession, msgStr);
                processMessage(session, quickFixSession, fixMessage);
            } catch (InvalidMessage e) {
                if (rejectGarbledMessage) {
                    final Message fixMessage = e.getFixMessage();
                    if (fixMessage != null) {
                        sessionLog.onErrorEvent("Processing garbled message: " + e.getMessage());
                        processMessage(session, quickFixSession, fixMessage);
                        return;
                    }
                }
                if (isLogon(msgStr)) {
                    sessionLog.onErrorEvent("Invalid LOGON message, disconnecting: " + e.getMessage());
                    session.close();
                } else {
                    sessionLog.onErrorEvent("Invalid message: " + e.getMessage());
                }
            }
        } else {
            log.fixSessionNotFound(msgStr);
            session.close();
        }
	}

	private void processMessage(Session session, quickfix.Session qfSession, Message message)
			throws IOException, FieldNotFound, RejectLogon, IncorrectDataFormat,
			IncorrectTagValue, UnsupportedMessageType, InvalidMessage
	{
        if (qfSession == null) {
            if (message.getHeader().getString(MsgType.FIELD).equals(MsgType.LOGON)) {
                final SessionID sessionID = getReverseSessionID(message);
                qfSession = this.sessionProvider.get(sessionID);
                if (qfSession != null) {
                    final quickfix.Log sessionLog = qfSession.getLog();
                    if (qfSession.hasResponder()) {
                        // Session is already bound to another connection
                        sessionLog.onErrorEvent("Multiple logons/connections for this session are not allowed");
                        session.close();
                        return;
                    }
                    sessionLog.onEvent("Accepting session " + qfSession.getSessionID() + " from ??");
                            // + protocolSession.getRemoteAddress());
                    final int heartbeatInterval = message.isSetField(HeartBtInt.FIELD) ? message.getInt(HeartBtInt.FIELD) : 0;
                    qfSession.setHeartBeatInterval(heartbeatInterval);
                    sessionLog.onEvent("Acceptor heartbeat set to " + heartbeatInterval + " seconds");
                    session.getUserProperties().put(QF_SESSION, qfSession);
                    qfSession.setResponder(new WsResponder(session));
                    if (sessionID.isFIXT()) { // QFJ-592
                        if (message.isSetField(DefaultApplVerID.FIELD)) {
                            final ApplVerID applVerID = new ApplVerID(
                                    message.getString(DefaultApplVerID.FIELD));
                            qfSession.setTargetDefaultApplicationVersionID(applVerID);
                            sessionLog.onEvent("Setting DefaultApplVerID (" + DefaultApplVerID.FIELD + "="
                                    + applVerID.getValue() + ") from Logon");
                        }
                    }
                } else {
                    log.unknownSessionIdLogon(sessionID);
                    return;
                }
            } else {
                log.ignoringLogon(message);
                session.close();
                return;
            }
        }

        qfSession.next(message);
	}

	/**
	 *
	 * @param ioSession
	 * @param sessionID
	 * @return
	 */
	private quickfix.Session findQFSession(Session session, SessionID sessionID) {
        quickfix.Session qfSession = findQFSession(session);
        if (qfSession == null) {
            qfSession = quickfix.Session.lookupSession(sessionID);
		}
		if (qfSession == null) {
			qfSession = sessionProvider.get(sessionID);
			qfSession.setResponder(new WsResponder(session));
		}
        return qfSession;
	}

	private static quickfix.Session findQFSession(Session session) {
		return (quickfix.Session) session.getUserProperties().get(SessionConnector.QF_SESSION);
	}

	@Override
	public void onClose(Session session, CloseReason closeReason) {
		this.log.onClose(session.getId(), closeReason.getCloseCode().getCode(), closeReason.getReasonPhrase());
	}

	@Override
	public void onError(Session session, Throwable t) {
		this.log.onError(session.getId(), t);
	}


	// --- Static Methods ---

	/**
	 *
	 * @param path
	 * @return
	 */
	public static ServerEndpointConfig config(String path, List<String> subprotocols) {
		return Builder.create(FixEndpoint.class, path).subprotocols(subprotocols(subprotocols)).build();
	}

	/**
	 *
	 * @param in
	 * @return
	 */
	public static List<String> subprotocols(String... in) {
		return subprotocols(Arrays.asList(in));
	}

	/**
	 *
	 * @param in
	 * @return
	 */
	public static List<String> subprotocols(List<String> in) {
		final List<String> out = new ArrayList<>();
		in.forEach(s -> out.add(s.toLowerCase().replaceAll("\\.", "")));
		return out;
	}


	// --- Inner Classes ---

	/**
	 * @author Daniel Siviter
	 * @since v1.0 [13 Nov 2019]
	 */
	private static class WsResponder implements Responder {
		private final Log log = LogProducer.log(WsResponder.class);
		private final Session session;

		WsResponder(Session session) {
			this.session = session;
		}

		@Override
		public boolean send(String data) {
			try {
				this.session.getBasicRemote().sendText(data);
				return true;
			} catch (IOException e) {
				this.log.send(session.getId(), e);
				return false;
			}
		}

		@Override
		public void disconnect() {
			try {
				this.session.close();
			} catch (IOException e) {
				this.log.close(session.getId(), e);
			}
		}

		@Override
		public String getRemoteAddress() {
			// no cross platform way of doing this!
			return null;
		}
	}
}
