/*-
 * ========================LICENSE_START=================================
 * TeamApps Core System
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
package org.teamapps.application.server.system.bootstrap.installer;

import io.github.classgraph.ClassGraph;
import io.github.classgraph.ClassInfo;
import io.github.classgraph.ClassInfoList;
import io.github.classgraph.ScanResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.teamapps.application.api.application.AbstractApplicationBuilder;
import org.teamapps.application.api.application.AbstractBaseApplicationBuilder;
import org.teamapps.application.api.application.ApplicationBuilder;
import org.teamapps.application.api.application.BaseApplicationBuilder;
import org.teamapps.application.server.system.bootstrap.ApplicationInfo;
import org.teamapps.universaldb.index.file.FileUtil;

import java.lang.invoke.MethodHandles;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.stream.Collectors;

public class ApplicationJarInstallationPhase implements ApplicationInstallationPhase {
	private static final Logger LOGGER = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

	@Override
	public void checkApplication(ApplicationInfo applicationInfo) {
		try {
			if (applicationInfo.getApplicationJar() == null) {
				return;
			}
			boolean unmanagedApplication = false;
			BaseApplicationBuilder baseApplicationBuilder = null;
			String fileHash = FileUtil.createFileHash(applicationInfo.getApplicationJar());
			URLClassLoader classLoader = new URLClassLoader(new URL[]{applicationInfo.getApplicationJar().toURI().toURL()});
			ScanResult scanResult = new ClassGraph()
					.overrideClassLoaders(classLoader)
					.enableAllInfo()
					.scan();
			ClassInfoList classes = scanResult.getClassesImplementing(ApplicationBuilder.class.getName()).getStandardClasses();
			if (classes.isEmpty()) {
				classes = scanResult.getSubclasses(AbstractApplicationBuilder.class.getName()).getStandardClasses();
			}
			if (classes.isEmpty()) {
				classes = scanResult.getClassesImplementing(AbstractApplicationBuilder.class.getName()).getStandardClasses();
				unmanagedApplication = true;
			}
			if (classes.isEmpty()) {
				classes = scanResult.getSubclasses(AbstractBaseApplicationBuilder.class.getName()).getStandardClasses();
				unmanagedApplication = true;
			}

			if (classes.isEmpty()) {
				applicationInfo.addError("Could not find application in jar file");
				return;
			}
			if (classes.size() > 1) {
				applicationInfo.addError("Too many application classes in jar file: " + classes.stream().map(ClassInfo::getName).collect(Collectors.joining(", ")));
				return;
			}

			classLoader = new URLClassLoader(new URL[]{applicationInfo.getApplicationJar().toURI().toURL()}, ApplicationJarInstallationPhase.class.getClassLoader());
			Class<?> builderClass = Class.forName(classes.get(0).getName(), true, classLoader);
//			Class<?> builderClass = classes.get(0).loadClass();
			scanResult.close();
			if (unmanagedApplication) {
				baseApplicationBuilder = (ApplicationBuilder) builderClass.getDeclaredConstructor().newInstance();
			} else {
				baseApplicationBuilder = (ApplicationBuilder) builderClass.getDeclaredConstructor().newInstance();
			}
			applicationInfo.setBaseApplicationBuilder(baseApplicationBuilder);
			applicationInfo.setUnmanagedPerspectives(unmanagedApplication);
			applicationInfo.setBinaryHash(fileHash);
			applicationInfo.setApplicationClassLoader(classLoader);
		} catch (Exception e) {
			applicationInfo.addError("Error checking jar file for app " + applicationInfo.getApplication() + e.getMessage());
			LOGGER.error("Error checking jar file:", e);
		}
	}

	@Override
	public void installApplication(ApplicationInfo applicationInfo) {

	}

	@Override
	public void loadApplication(ApplicationInfo applicationInfo) {

	}
}
