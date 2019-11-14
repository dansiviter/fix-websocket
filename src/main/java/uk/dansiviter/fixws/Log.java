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

import static org.jboss.logging.Logger.Level.DEBUG;
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
public interface Log extends BasicLogger {
	@LogMessage(level = DEBUG)
	@Message("Incomming. [%s]")
	void onIncoming(String message);

	@LogMessage(level = DEBUG)
	@Message("Outgoing. [%s]")
	void onOutgoing(String message);

	@LogMessage(level = DEBUG)
	@Message("Event. [%s]")
	void onEvent(String text);

	@LogMessage(level = WARN)
	@Message("Error event. [%s]")
    void onErrorEvent(String text);

	@LogMessage(level = DEBUG)
	@Message("On create. [%s]")
	void onCreate(SessionID sessionId);

	@LogMessage(level = DEBUG)
	@Message("On logon. [%s]")
	void onLogon(SessionID sessionId);

	@LogMessage(level = DEBUG)
	@Message("To admin. [id=%s,msg=%s]")
	void toAdmin(SessionID sessionId, quickfix.Message message);

	@LogMessage(level = DEBUG)
	@Message("From admin. [id=%s,msg=%s]")
	void fromAdmin(SessionID sessionId, quickfix.Message message);

	@LogMessage(level = DEBUG)
	@Message("To app. [id=%s,msg=%s]")
	void toApp(SessionID sessionId, quickfix.Message message);

	@LogMessage(level = DEBUG)
	@Message("From app. [id=%s,msg=%s]")
	void fromApp(SessionID sessionId, quickfix.Message message);

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
}
