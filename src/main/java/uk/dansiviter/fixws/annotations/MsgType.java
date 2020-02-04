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
package uk.dansiviter.fixws.annotations;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import javax.enterprise.util.AnnotationLiteral;
import javax.inject.Qualifier;

import quickfix.Message;
import uk.dansiviter.fixws.FixUtil;

/**
 * @author Daniel Siviter
 * @since v1.0 [13 Nov 2019]
 * @see quickfix.MsgType
 */
@Qualifier
@Retention(RUNTIME)
@Target({ METHOD, FIELD, PARAMETER, TYPE })
public @interface MsgType {
	/**
	 * @return the message type.
	 */
	String value();

	@SuppressWarnings("all")
	public static final class Literal extends AnnotationLiteral<MsgType> implements MsgType {
		private static final long serialVersionUID = 1L;

		private final String value;

		private Literal(String value) {
			this.value = value;
		}

		@Override
		public String value() {
			return value;
		}

		public static MsgType msgType(quickfix.field.MsgType value) {
			return msgType(value.getValue());
		}

		public static MsgType msgType(String value) {
			return new Literal(value);
		}

		public static MsgType msgType(Message msg) {
			return msgType(FixUtil.msgType(msg));
		}
	}
}
