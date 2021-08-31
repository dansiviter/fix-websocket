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
package uk.dansiviter.fixws;

import static uk.dansiviter.juli.annotations.Message.Level.DEBUG;
import static uk.dansiviter.juli.annotations.Message.Level.WARN;

import java.io.IOException;

import quickfix.SessionID;
import uk.dansiviter.juli.annotations.Message;

/**
 *
 * @author Daniel Siviter
 * @since v1.0 [13 Nov 2019]
 */
@uk.dansiviter.juli.annotations.Log
public interface Log {
	@Message(value = "Incoming. [{0}]", level = DEBUG)
	void onIncoming(String message);

	@Message(value = "Outgoing. [{0}]", level = DEBUG)
	void onOutgoing(String message);

	@Message("Event. [{0}]")
	void onEvent(String text);

	@Message(value = "Error event. [{0}]", level = WARN)
	void onErrorEvent(String text);

	@Message(value = "Unable to send! [{0}]", level = WARN)
	void send(String id, IOException e);

	@Message(value = "Unable to close! [{0}]", level = WARN)
	void close(String id, IOException e);

	@Message("Open. [id={0}]")
	void onOpen(String id);

	@Message("Close. [id={0},code={1},phrase={2}]")
	void onClose(String id, int closeCode, String reasonPhrase);

	@Message(value = "Error! [id={0}]", level = WARN)
	void onError(String id, Throwable cause);

	@Message(value = "Disconnecting; received message for unknown session. [{0}]", level = WARN)
	void fixSessionNotFound(String msg);

	@Message(value = "Unknown session ID during logon. [{0}]",level = WARN)
	void unknownSessionIdLogon(SessionID sessionId);

	@Message(value = "Ignoring non-logon message before session established. [{0}]", level = WARN)
	void ignoringLogon(quickfix.Message msg);
}
