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
package uk.dansiviter.fixws;

import static uk.dansiviter.juli.annotations.Message.Level.DEBUG;
import static uk.dansiviter.juli.annotations.Message.Level.WARN;

import uk.dansiviter.juli.annotations.Message;


public interface FixLog extends quickfix.Log {
	@Override
	@Message(value = "Incoming. [{0}]", level = DEBUG)
	void onIncoming(String message);

	@Override
	@Message(value = "Outgoing. [{0}]", level = DEBUG)
	void onOutgoing(String message);

	@Override
	@Message(value = "Event. [{0}]", level = DEBUG)
	void onEvent(String text);

	@Override
	@Message(value = "Error event. [{0}]", level = WARN)
	void onErrorEvent(String text);

	@Override
	default void clear() { }
}
