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
