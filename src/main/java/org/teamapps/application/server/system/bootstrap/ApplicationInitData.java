/*-
 * ========================LICENSE_START=================================
 * TeamApps Core System
 * ---
 * Copyright (C) 2020 - 2023 TeamApps.org
 * ---
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * =========================LICENSE_END==================================
 */
package org.teamapps.application.server.system.bootstrap;

import org.teamapps.application.api.application.ApplicationInitializer;
import org.teamapps.message.protocol.message.Message;
import org.teamapps.message.protocol.model.PojoObjectDecoder;
import org.teamapps.universaldb.message.MessageCache;
import org.teamapps.universaldb.message.MessageStore;

import java.io.File;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ApplicationInitData implements ApplicationInitializer {

	private final File basePath;
	private final Map<String, MessageStore> messageStoreMap = new ConcurrentHashMap<>();

	public ApplicationInitData(File basePath) {
		this.basePath = basePath;
	}

	@Override
	public File getAppBasePath() {
		return basePath;
	}

	@Override
	public <MESSAGE extends Message> void createMessageStore(String name, PojoObjectDecoder<MESSAGE> messageDecoder, boolean withCache) {
		MessageStore messageStore = MessageStore.create(basePath, name, messageDecoder, withCache ? MessageCache.lruCache(1000) : null);
		messageStoreMap.put(name, messageStore);
	}

	@Override
	public <MESSAGE extends Message> MessageStore<MESSAGE> getMessageStore(String name) {
		return messageStoreMap.get(name);
	}
}
