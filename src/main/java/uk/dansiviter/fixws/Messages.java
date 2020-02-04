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

import org.jboss.logging.annotations.Cause;
import org.jboss.logging.annotations.Message;
import org.jboss.logging.annotations.MessageBundle;

import quickfix.FieldNotFound;
import quickfix.SessionID;

@MessageBundle(projectCode = "FIXWS")
public interface Messages {
	Messages INSTANCE = org.jboss.logging.Messages.getBundle(Messages.class);

	@Message(id = 1, value = "Session '%s' not found!")
	IllegalStateException sessionNotFound(SessionID sessionID);

	@Message(id = 2, value = "'msgType' Field not found!")
	IllegalStateException msgTypeNotFound(@Cause FieldNotFound cause);

	public static Messages messages() {
		return INSTANCE;
	}
}
