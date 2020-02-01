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

import static quickfix.LogUtil.logThrowable;
import static quickfix.MessageUtils.toApplVerID;
import static uk.dansiviter.fixws.Messages.messages;
import static uk.dansiviter.fixws.annotations.MsgType.Literal.msgType;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Event;
import javax.enterprise.event.Observes;
import javax.enterprise.event.ObservesAsync;
import javax.inject.Inject;

import quickfix.Application;
import quickfix.DataDictionaryProvider;
import quickfix.DoNotSend;
import quickfix.FieldNotFound;
import quickfix.FixVersions;
import quickfix.IncorrectDataFormat;
import quickfix.IncorrectTagValue;
import quickfix.Message;
import quickfix.MessageUtils;
import quickfix.RejectLogon;
import quickfix.Session;
import quickfix.SessionID;
import quickfix.SessionNotFound;
import quickfix.UnsupportedMessageType;
import quickfix.field.ApplVerID;
import quickfix.field.MsgType;
import uk.dansiviter.fixws.annotations.FromApp;
import uk.dansiviter.fixws.annotations.ToApp;

/**
 * @author Daniel Siviter
 * @since v1.0 [13 Nov 2019]
 */
@ApplicationScoped
public class FixApplication implements Application {
	@Inject
	private Log log;
	@Inject
	@FromApp
	private Event<Message> messageEvent;

	@Override
	public void onCreate(SessionID sessionId) {
		this.log.onCreate(sessionId);
	}

	@Override
	public void onLogon(SessionID sessionId) {
		this.log.onLogon(sessionId);
	}

	@Override
	public void onLogout(SessionID sessionId) {
		this.log.onLogon(sessionId);
	}

	@Override
	public void toAdmin(Message message, SessionID sessionId) {
		this.log.toAdmin(sessionId, message);
	}

	@Override
	public void fromAdmin(Message message, SessionID sessionId)
			throws FieldNotFound, IncorrectDataFormat, IncorrectTagValue, RejectLogon {
		this.log.fromAdmin(sessionId, message);
	}

	@Override
	public void toApp(Message message, SessionID sessionId) throws DoNotSend {
		this.log.toApp(sessionId, message);
	}

	@Override
	public void fromApp(Message message, SessionID sessionId)
			throws FieldNotFound, IncorrectDataFormat, IncorrectTagValue, UnsupportedMessageType {
		this.log.fromApp(sessionId, message);

		final String msgType = message.getHeader().getString(MsgType.FIELD);
		final Event<Message> evt = this.messageEvent.select(msgType(msgType));
		evt.fire(message);
		evt.fireAsync(message);
	}

	/**
	 *
	 * @param message
	 */
	public void onAsync(@ObservesAsync @ToApp Message message) {
		on(message);
	}

	/**
	 *
	 * @param message
	 */
	public void on(@Observes @ToApp Message message) {
		final SessionID sessionId = MessageUtils.getSessionID(message);
		final quickfix.Session session = quickfix.Session.lookupSession(sessionId);
		if (session == null) {
			throw messages().sessionNotFound(sessionId);
		}

		if (sessionId.isFIXT()) {  // required for correct deserialisation on client app.
			if (!message.getHeader().isSetField(ApplVerID.FIELD)) {
				message.getHeader().setField(session.getSenderDefaultApplicationVersionID());
			}
		}

		final DataDictionaryProvider dataDictionaryProvider = session.getDataDictionaryProvider();
		if (dataDictionaryProvider != null) {
			try {
				dataDictionaryProvider.getApplicationDataDictionary(applVerId(session, message)).validate(message);
			} catch (FieldNotFound | IncorrectTagValue | IncorrectDataFormat e) {
				logThrowable(sessionId, "Outgoing message failed validation!", e);
				return;
			}
		}
		try {
			Session.sendToTarget(message, sessionId);
		} catch (SessionNotFound e) {
			logThrowable(sessionId, "Outgoing message failed!", e);
		}
	}


	// --- Static Methods ---

	/**
	 *
	 * @param session
	 * @param message
	 * @return
	 */
	public ApplVerID applVerId(quickfix.Session session, Message message) {
		final String beginString = session.getSessionID().getBeginString();
		if (FixVersions.BEGINSTRING_FIXT11.equals(beginString)) {
			return new ApplVerID(ApplVerID.FIX50);
		}
		return toApplVerID(beginString);
	}
}