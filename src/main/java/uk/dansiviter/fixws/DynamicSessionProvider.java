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

import static quickfix.SessionSettings.BEGINSTRING;
import static quickfix.SessionSettings.SENDERCOMPID;
import static quickfix.SessionSettings.SENDERLOCID;
import static quickfix.SessionSettings.SENDERSUBID;
import static quickfix.SessionSettings.TARGETCOMPID;
import static quickfix.SessionSettings.TARGETLOCID;
import static quickfix.SessionSettings.TARGETSUBID;

import java.util.List;
import java.util.Properties;

import org.quickfixj.QFJException;

import quickfix.ConfigError;
import quickfix.Session;
import quickfix.SessionFactory;
import quickfix.SessionID;
import quickfix.SessionSettings;

/**
 * @author Daniel Siviter
 * @since v1.0 [13 Nov 2019]
 * @see quickfix.mina.acceptor.DynamicAcceptorSessionProvider
 */
public class DynamicSessionProvider implements SessionProvider {
	public static final String WILDCARD = "*";
	private static final SessionID ANY_SESSION = new SessionID(WILDCARD, WILDCARD, WILDCARD, WILDCARD, WILDCARD,
			WILDCARD, WILDCARD, null);

	private final List<TemplateMapping> templateMappings;
	protected final SessionSettings settings;
	protected final SessionFactory sessionFactory;

	/**
	 * Mapping from a sessionID pattern to a session template ID.
	 */
	public static class TemplateMapping {
		private final SessionID pattern;
		private final SessionID templateID;

		public TemplateMapping(SessionID pattern, SessionID templateID) {
			super();
			this.pattern = pattern;
			this.templateID = templateID;
		}

		public SessionID getPattern() {
			return pattern;
		}

		public SessionID getTemplateID() {
			return templateID;
		}

		@Override
		public String toString() {
			return "<" + pattern + "," + templateID + ">";
		}
	}

	/**
	 * @param settings       session settings
	 * @param templateID     this is a session ID for a session definition in the
	 *                       session settings that will be used for default dynamic
	 *                       session values. The BeginString, SenderCompID, and
	 *                       TargetCompID settings will be replaced with those in
	 *                       the received logon message.
	 * @param sessionFactory session factory for the dynamic sessions
	 */
	public DynamicSessionProvider(SessionSettings settings, SessionID templateID, SessionFactory sessionFactory) {
		this(settings, List.of(new TemplateMapping(ANY_SESSION, templateID)), sessionFactory);
	}

	/**
	 * @param settings         session settings
	 * @param templateMappings this is a list of session ID patterns mapped to
	 *                         session IDs in the settings file. The session IDs
	 *                         represent the template for a specified session ID
	 *                         pattern. The template is used to dynamically create
	 *                         acceptor sessions. Use "*" to represent a wildcard
	 *                         for a pattern element. For example, new
	 *                         SessionID("FIX.4.2", "*", "*") would match for any
	 *                         FIX 4.2 session ID. This allows separate template
	 *                         session configurations for FIX versions (or CompIDs)
	 *                         being accepted dynamically on a single TCP port.
	 * @param sessionFactory   session factory for the dynamic sessions
	 * @see TemplateMapping
	 */
	public DynamicSessionProvider(
			SessionSettings settings,
			List<TemplateMapping> templateMappings,
			SessionFactory sessionFactory)
	{
		this.settings = settings;
		this.templateMappings = templateMappings;
		this.sessionFactory = sessionFactory;
	}

	@Override
	public synchronized Session get(SessionID sessionID) {
		var s = Session.lookupSession(sessionID);
		if (s == null) {
			try {
				var templateID = lookupTemplateID(sessionID);
				if (templateID == null) {
					throw new ConfigError("Unable to find a session template for " + sessionID);
				}
				var dynamicSettings = new SessionSettings();
				copySettings(dynamicSettings, settings.getDefaultProperties());
				copySettings(dynamicSettings, settings.getSessionProperties(templateID));
				dynamicSettings.setString(BEGINSTRING, sessionID.getBeginString());
				dynamicSettings.setString(SENDERCOMPID, sessionID.getSenderCompID());
				optionallySetValue(dynamicSettings, SENDERSUBID, sessionID.getSenderSubID());
				optionallySetValue(dynamicSettings, SENDERLOCID, sessionID.getSenderLocationID());
				dynamicSettings.setString(TARGETCOMPID, sessionID.getTargetCompID());
				optionallySetValue(dynamicSettings, TARGETSUBID, sessionID.getTargetSubID());
				optionallySetValue(dynamicSettings, TARGETLOCID, sessionID.getTargetLocationID());
				s = sessionFactory.create(sessionID, dynamicSettings);
			} catch (ConfigError e) {
				throw new QFJException(e);
			}
		}
		return s;
	}

	protected void optionallySetValue(SessionSettings dynamicSettings, String key, String value) {
		dynamicSettings.setString(key, value);
	}

	protected SessionID lookupTemplateID(SessionID sessionID) {
		for (var mapping : templateMappings) {
			if (isMatching(mapping.getPattern(), sessionID)) {
				return mapping.getTemplateID();
			}
		}
		return null;
	}

	private boolean isMatching(SessionID pattern, SessionID sessionID) {
		return isMatching(pattern.getBeginString(), sessionID.getBeginString())
				&& isMatching(pattern.getSenderCompID(), sessionID.getSenderCompID())
				&& isMatching(pattern.getSenderSubID(), sessionID.getSenderSubID())
				&& isMatching(pattern.getSenderLocationID(), sessionID.getSenderLocationID())
				&& isMatching(pattern.getTargetCompID(), sessionID.getTargetCompID())
				&& isMatching(pattern.getTargetSubID(), sessionID.getTargetSubID())
				&& isMatching(pattern.getTargetLocationID(), sessionID.getTargetLocationID());
	}

	private boolean isMatching(String pattern, String value) {
		return WILDCARD.equals(pattern) || (pattern != null && pattern.equals(value));
	}

	protected void copySettings(SessionSettings settings, Properties properties) {
		properties.forEach((k, v) -> settings.setString(k.toString(), v.toString()));
	}
}
