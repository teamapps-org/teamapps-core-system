/*-
 * ========================LICENSE_START=================================
 * TeamApps Application Server
 * ---
 * Copyright (C) 2020 - 2022 TeamApps.org
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

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.teamapps.ux.resource.Resource;
import org.teamapps.ux.resource.ResourceProvider;

import java.lang.invoke.MethodHandles;
import java.time.Duration;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class PublicLinkResourceProvider implements ResourceProvider {

	private static final Logger LOGGER = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
	public static final String SERVLET_PATH_PREFIX = "/pl/";
	private static final ScheduledExecutorService SCHEDULED_EXECUTOR_SERVICE = Executors.newSingleThreadScheduledExecutor();

	private final Map<String, Resource> resourceByUuid = new ConcurrentHashMap<>();

	private static final PublicLinkResourceProvider INSTANCE = new PublicLinkResourceProvider();

	public static PublicLinkResourceProvider getInstance() {
		return INSTANCE;
	}

	private PublicLinkResourceProvider() {
	}

	@Override
	public Resource getResource(String servletPath, String relativeResourcePath, String httpSessionId) {
		return resourceByUuid.get(relativeResourcePath.replace("/", ""));
	}

	public String createLinkForResource(Resource resource, Duration availabilityDuration) {
		String suffix = FilenameUtils.getExtension(resource.getName());
		String linkName = UUID.randomUUID().toString() + (StringUtils.isNotBlank(suffix) ? "." + suffix : "");
		resourceByUuid.put(linkName, resource);
		SCHEDULED_EXECUTOR_SERVICE.schedule(() -> {
			resourceByUuid.remove(linkName);
		}, availabilityDuration.toSeconds(), TimeUnit.SECONDS);
		String link = SERVLET_PATH_PREFIX + linkName;
		LOGGER.info("Generating link for resource {} --> {}", resource.getName(), link);
		return link;
	}
}
