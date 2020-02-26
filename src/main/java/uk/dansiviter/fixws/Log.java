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

import static org.jboss.logging.Logger.Level.DEBUG;
import static org.jboss.logging.Logger.Level.ERROR;
import static org.jboss.logging.Logger.Level.WARN;

import java.io.IOException;

import org.jboss.logging.BasicLogger;
import org.jboss.logging.annotations.Cause;
import org.jboss.logging.annotations.LogMessage;
import org.jboss.logging.annotations.Message;
import org.jboss.logging.annotations.MessageLogger;

import quickfix.SessionID;

/**
 *
 * @author Daniel Siviter
 * @since v1.0 [13 Nov 2019]
 */
@MessageLogger(projectCode = "FIXWS")
public interface Log extends BasicLogger, quickfix.Log {
	@LogMessage
	@Message("Incoming. [%s]")
	@Override
	void onIncoming(String message);

	@LogMessage
	@Message("Outgoing. [%s]")
	@Override
	void onOutgoing(String message);

	@LogMessage
	@Message("Event. [%s]")
	@Override
	void onEvent(String text);

	@LogMessage(level = WARN)
	@Message("Error event. [%s]")
	@Override
    void onErrorEvent(String text);

	@LogMessage(level = WARN)
	@Message("Unable to send! [%s]")
	void send(String id, @Cause IOException e);

	@LogMessage(level = WARN)
	@Message("Unable to close! [%s]")
	void close(String id, @Cause IOException e);

	@LogMessage
	@Message("Open. [id=%s]")
	void onOpen(String id);

	@LogMessage
	@Message("Close. [id=%s,code=%d,phrase=%s]")
	void onClose(String id, int closeCode, String reasonPhrase);

	@LogMessage(level = WARN)
	@Message("Error! [%s]")
	void onError(String id, @Cause Throwable t);

	@LogMessage(level = WARN)
	@Message("Disconnecting; received message for unknown session. [%s]")
	void fixSessionNotFound(String msg);

	@LogMessage(level = ERROR)
	@Message("Unknown session ID during logon. [%s]")
	void unknownSessionIdLogon(SessionID sessionId);

	@LogMessage(level = WARN)
	@Message("Ignoring non-logon message before session established. [%s]")
	void ignoringLogon(quickfix.Message msg);

	@Override
	default void clear() {
		// required for implementing quickfix.Log
	}
}
