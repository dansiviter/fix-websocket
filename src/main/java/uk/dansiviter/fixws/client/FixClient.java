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
package uk.dansiviter.fixws.client;

import java.io.Closeable;
import java.io.FileInputStream;
import java.io.IOException;

import quickfix.Acceptor;
import quickfix.ApplicationAdapter;
import quickfix.ConfigError;
import quickfix.DefaultMessageFactory;
import quickfix.FileLogFactory;
import quickfix.FileStoreFactory;
import quickfix.LogFactory;
import quickfix.MessageFactory;
import quickfix.MessageStoreFactory;
import quickfix.RuntimeError;
import quickfix.SessionSettings;
import quickfix.ThreadedSocketAcceptor;

public class FixClient extends ApplicationAdapter implements Closeable {
	private final SessionSettings settings;
	private final MessageStoreFactory storeFactory;
	private final LogFactory logFactory;
	private final MessageFactory messageFactory;
	private final Acceptor acceptor;

	FixClient(String fileName) throws IOException, ConfigError {
		this.settings = new SessionSettings(new FileInputStream(fileName));
		this.storeFactory = new FileStoreFactory(settings);
		this.logFactory = new FileLogFactory(settings);
		this.messageFactory = new DefaultMessageFactory();
		this.acceptor = new ThreadedSocketAcceptor(this, storeFactory, settings, logFactory, messageFactory);
	}

	public void start() throws RuntimeError, ConfigError {
		this.acceptor.start();
	}

	@Override
	public void close() throws IOException {
		acceptor.stop();
	}
}
