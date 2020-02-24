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

import static org.eclipse.microprofile.metrics.MetricType.COUNTER;
import static uk.dansiviter.fixws.FixUtil.msgType;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;

import org.eclipse.microprofile.metrics.Metadata;
import org.eclipse.microprofile.metrics.MetricRegistry;
import org.eclipse.microprofile.metrics.Tag;

import quickfix.Message;
import quickfix.SessionID;

/**
 * @author Daniel Siviter
 * @since v1.0 [4 Feb 2020]
 */
@ApplicationScoped
public class Metrics {
	private static final Metadata METADATA = Metadata.builder()
			.withName("fix/message.count")
			.withDisplayName("FIX Message Count")
			.withDescription("The count of inbound and outbound messages.")
			.withType(COUNTER)
			.build();
	private static final Tag CLIENT = new Tag("kind", "client");
	private static final Tag SERVER = new Tag("kind", "server");
	private static final Map<String, Tag> MSG_TYPE = new ConcurrentHashMap<>();

	@Inject
	private Instance<MetricRegistry> registry;

	void on(Message msg, SessionID id, boolean inbound) {
		if (!this.registry.isResolvable()) {
			return;
		}
		final Tag msgType = MSG_TYPE.computeIfAbsent(msgType(msg), k -> new Tag("msgType", k));
		this.registry.get().counter(METADATA, inbound ? CLIENT : SERVER, msgType).inc();
	}
}
