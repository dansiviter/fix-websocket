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

import static quickfix.MessageUtils.getReverseSessionID;
import static quickfix.MessageUtils.getSessionID;
import static uk.dansiviter.fixws.Messages.messages;

import javax.annotation.Nonnull;

import quickfix.FieldNotFound;
import quickfix.Message;
import quickfix.Message.Header;
import quickfix.SessionID;
import quickfix.field.BeginString;
import quickfix.field.SenderCompID;
import quickfix.field.SenderLocationID;
import quickfix.field.SenderSubID;
import quickfix.field.TargetCompID;
import quickfix.field.TargetLocationID;
import quickfix.field.TargetSubID;

/**
 * @author Daniel Siviter
 * @since v1.0 [13 Nov 2019]
 */
public enum FixUtil {
	;
	/**
	 *
	 * @param message
	 * @return
	 * @see quickfix.MessageUtils#getSessionID(Message)
	 */
	public static @Nonnull SessionID sessionId(@Nonnull Message message) {
		return getSessionID(message);
	}

	/**
	 *
	 * @param message
	 * @return
	 * @see quickfix.MessageUtils#getReverseSessionID(Message)
	 */
	public static @Nonnull SessionID reverseSessionID(@Nonnull Message message) {
		return getReverseSessionID(message);
	}

	/**
	 *
	 * @param sessionId
	 * @return
	 */
	public static @Nonnull SessionID reverse(@Nonnull SessionID sessionId) {
		return new SessionID(sessionId.getBeginString(), sessionId.getTargetCompID(), sessionId.getTargetSubID(),
				sessionId.getTargetLocationID(), sessionId.getSenderCompID(), sessionId.getSenderSubID(),
				sessionId.getSenderLocationID(), null);
	}

	/**
	 *
	 * @param <M>
	 * @param sessionId
	 * @param message
	 * @return
	 * @see Message#setSessionID(SessionID)
	 */
	public static @Nonnull <M extends Message> M set(@Nonnull SessionID sessionId, @Nonnull M message) {
		final Header header = message.getHeader();
		header.setString(BeginString.FIELD, sessionId.getBeginString());
		header.setString(SenderCompID.FIELD, sessionId.getSenderCompID());
		optionallySetID(header, SenderSubID.FIELD, sessionId.getSenderSubID());
		optionallySetID(header, SenderLocationID.FIELD, sessionId.getSenderLocationID());
		header.setString(TargetCompID.FIELD, sessionId.getTargetCompID());
		optionallySetID(header, TargetSubID.FIELD, sessionId.getTargetSubID());
		optionallySetID(header, TargetLocationID.FIELD, sessionId.getTargetLocationID());
		return message;
	}

	/**
	 *
	 * @param <M>
	 * @param sessionId
	 * @param message
	 * @return
	 */
	public static <M extends Message> M setReverse(@Nonnull SessionID sessionId, @Nonnull M message) {
		return set(reverse(sessionId), message);
	}

	private static void optionallySetID(@Nonnull Header header, int field, @Nonnull String value) {
		if (!value.equals(SessionID.NOT_SET)) {
			header.setString(field, value);
		}
	}

	/**
	 *
	 * @param msg
	 * @return
	 */
	public static @Nonnull String msgType(@Nonnull Message msg) {
		try {
			return msg.getHeader().getString(quickfix.field.MsgType.FIELD);
		} catch (FieldNotFound e) {
			throw messages().msgTypeNotFound(e);
		}
	}
}
