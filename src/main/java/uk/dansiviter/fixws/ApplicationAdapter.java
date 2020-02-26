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

import quickfix.Application;
import quickfix.DoNotSend;
import quickfix.FieldNotFound;
import quickfix.IncorrectDataFormat;
import quickfix.IncorrectTagValue;
import quickfix.Message;
import quickfix.RejectLogon;
import quickfix.SessionID;
import quickfix.UnsupportedMessageType;

/**
 * @author Daniel Siviter
 * @since v1.0 [26 Feb 2020]
 */
public abstract class ApplicationAdapter implements Application {

	@Override
	public void onCreate(SessionID sessionId) {
		// nothing to see here
	}

	@Override
	public void onLogon(SessionID sessionId) {
		// nothing to see here
	}

	@Override
	public void onLogout(SessionID sessionId) {
		// nothing to see here
	}

	@Override
	public void toAdmin(Message message, SessionID sessionId) {
		// nothing to see here
	}

	@Override
	public void fromAdmin(Message message, SessionID sessionId)
			throws FieldNotFound, IncorrectDataFormat, IncorrectTagValue, RejectLogon
	{
		// nothing to see here
	}

	@Override
	public void toApp(Message message, SessionID sessionId) throws DoNotSend {
		// nothing to see here
	}

	@Override
	public void fromApp(Message message, SessionID sessionId)
			throws FieldNotFound, IncorrectDataFormat, IncorrectTagValue, UnsupportedMessageType
	{
		// nothing to see here
	}
}
