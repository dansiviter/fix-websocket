/*
 * Copyright 2021 Daniel Siviter
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

import static uk.dansiviter.juli.annotations.Message.Level.ERROR;
import static uk.dansiviter.juli.annotations.Message.Level.WARN;

import quickfix.SessionID;
import quickfix.SessionNotFound;
import uk.dansiviter.fixws.FixLog;
import uk.dansiviter.juli.annotations.Log;
import uk.dansiviter.juli.annotations.Message;

@Log
public interface ClientLog extends FixLog {
	@Message("Open. [id={0}]")
	void onOpen(String id);

	@Message("Close. [id={0},code={1},phrase={2}]")
	void onClose(String id, int closeCode, String reasonPhrase);

	@Message(value = "Error! [id={0}]", level = WARN)
	void onError(String id, Throwable cause);

	@Message(value = "Session not found! [id={0}]", level = ERROR)
	void sessionNotFound(SessionID id, SessionNotFound e);
}
