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
package org.teamapps.application.server.system.bootstrap.installer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.teamapps.application.server.system.bootstrap.ApplicationInfo;
import org.teamapps.application.server.system.bootstrap.ApplicationInfoDataElement;
import org.teamapps.commons.util.collections.ByKeyComparisonResult;
import org.teamapps.commons.util.collections.CollectionUtil;
import org.teamapps.universaldb.DatabaseManager;
import org.teamapps.universaldb.UniversalDB;
import org.teamapps.universaldb.UniversalDbBuilder;
import org.teamapps.universaldb.model.DatabaseModel;
import org.teamapps.universaldb.model.FieldModel;
import org.teamapps.universaldb.model.TableModel;
import org.teamapps.universaldb.schema.Column;
import org.teamapps.universaldb.schema.Database;
import org.teamapps.universaldb.schema.ModelProvider;
import org.teamapps.universaldb.schema.Table;

import java.lang.invoke.MethodHandles;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

public class DataModelInstallationPhase implements ApplicationInstallationPhase {
	private static final Logger LOGGER = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

	private final DatabaseManager databaseManager;
	private final Function<String, UniversalDbBuilder> dbBuilderFunction;

	public DataModelInstallationPhase(DatabaseManager databaseManager, Function<String, UniversalDbBuilder> dbBuilderFunction) {
		this.databaseManager = databaseManager;
		this.dbBuilderFunction = dbBuilderFunction;
	}

	@Override
	public void checkApplication(ApplicationInfo applicationInfo) {
		try {
			if (!applicationInfo.getErrors().isEmpty()) {
				return;
			}
			ModelProvider databaseModel = applicationInfo.getBaseApplicationBuilder().getDatabaseModel();
			if (databaseModel == null) {
				applicationInfo.addWarning("No data model!");
				return;
			}
			DatabaseModel model = databaseModel.getModel();
			if (!model.isValid()) {
				applicationInfo.addError("Data model is invalid!");
			}
			if (model.getTables().isEmpty()) {
				applicationInfo.addError("Data model with missing tables!");
				return;
			}

			if (!model.getName().equals(applicationInfo.getName())) {
				applicationInfo.addError("Name of database is not equal to application name! Application name: " + applicationInfo.getName() + ", db name: " + model.getName());
				return;
			}

			UniversalDB universalDB = databaseManager.getDatabase(applicationInfo.getName());
			DatabaseModel installedModel = null;
			if (universalDB != null) {
				installedModel = universalDB.getTransactionIndex().getCurrentModel();
				if (!universalDB.getTransactionIndex().isValidModel(model)) {
					applicationInfo.addError("Incompatible database models!");
					return;
				}
			}

			ApplicationInfoDataElement modelInfo = new ApplicationInfoDataElement();
			modelInfo.setData(model.toString());
			List<String> newList = model.getTables().stream().flatMap(t -> t.getFields().stream()).map(f -> f.getTableModel().getName() + "." + f.getName() + " (" + f.getFieldType() + ")").toList();
			if (installedModel == null) {
				newList.forEach(modelInfo::added);
			} else {
				List<String> existingList = installedModel.getTables().stream().flatMap(t -> t.getFields().stream()).map(f -> f.getTableModel().getName() + "." + f.getName() + " (" + f.getFieldType() + ")").toList();
				ByKeyComparisonResult<String, String, String> comparisonResult = CollectionUtil.compareByKey(existingList, newList, s -> s, s -> s);
				comparisonResult.getBEntriesNotInA().forEach(modelInfo::added);
				comparisonResult.getAEntriesNotInB().forEach(modelInfo::removed);
			}
			applicationInfo.setDataModelData(modelInfo);
		} catch (Exception e) {
			applicationInfo.addError("Error checking data model:" + e.getMessage());
			LOGGER.error("Error checking data model:", e);
		}
	}

	@Override
	public void installApplication(ApplicationInfo applicationInfo) {
		ModelProvider databaseModel = applicationInfo.getBaseApplicationBuilder().getDatabaseModel();
		if (databaseModel == null) {
			return;
		}
		try {
			ClassLoader classLoader = applicationInfo.getApplicationClassLoader();
			if (classLoader == null) {
				classLoader = this.getClass().getClassLoader();
			}
			UniversalDB universalDB = databaseManager.getDatabase(applicationInfo.getName());
			if (universalDB == null) {
				dbBuilderFunction.apply(applicationInfo.getName())
						.modelProvider(databaseModel)
						.classLoader(classLoader)
						.build();
			} else {
				universalDB.installModelUpdate(databaseModel, classLoader);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Override
	public void loadApplication(ApplicationInfo applicationInfo) {
		ModelProvider databaseModel = applicationInfo.getBaseApplicationBuilder().getDatabaseModel();
		if (databaseModel == null) {
			return;
		}
		try {
			ClassLoader classLoader = applicationInfo.getApplicationClassLoader();
			if (classLoader == null) {
				classLoader = this.getClass().getClassLoader();
			}
			UniversalDB universalDB = databaseManager.getDatabase(applicationInfo.getName());
			if (universalDB == null) {
				dbBuilderFunction.apply(applicationInfo.getName())
						.modelProvider(databaseModel)
						.classLoader(classLoader)
						.build();
			} else {
				universalDB.installModelUpdate(databaseModel, classLoader);
			}
		} catch (RuntimeException runtimeException) {
			throw new RuntimeException(runtimeException);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
