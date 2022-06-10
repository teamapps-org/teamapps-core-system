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
package org.teamapps.application.server.system.bootstrap.installer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.teamapps.application.server.system.bootstrap.ApplicationInfo;
import org.teamapps.application.server.system.bootstrap.ApplicationInfoDataElement;
import org.teamapps.universaldb.UniversalDB;
import org.teamapps.universaldb.schema.*;

import java.lang.invoke.MethodHandles;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class DataModelInstallationPhase implements ApplicationInstallationPhase {
	private static final Logger LOGGER = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

	private final UniversalDB universalDB;

	public DataModelInstallationPhase(UniversalDB universalDB) {
		this.universalDB = universalDB;
	}

	@Override
	public void checkApplication(ApplicationInfo applicationInfo) {
		try {
			if (!applicationInfo.getErrors().isEmpty()) {
				return;
			}
			SchemaInfoProvider databaseModel = applicationInfo.getBaseApplicationBuilder().getDatabaseModel();
			if (databaseModel == null) {
				applicationInfo.addWarning("No data model!");
				return;
			}
			Schema schema = databaseModel.getSchema();
			List<Database> databases = schema.getDatabases();
			if (databases.size() > 1) {
				applicationInfo.addError("More than one database:" + databases.stream().map(Database::getName).collect(Collectors.joining(", ")));
				return;
			}
			if (databases.isEmpty()) {
				applicationInfo.addError("Data model with missing database!");
				return;
			}
			Database database = databases.get(0);
			String databaseName = database.getName();
			if (!databaseName.equals(applicationInfo.getName())) {
				applicationInfo.addError("Name of database is not equal to application name! Application name: " + applicationInfo.getName() + ", db name: " + databaseName);
				return;
			}
			if (!universalDB.getSchemaIndex().getSchema().isCompatibleWith(schema)) {
				applicationInfo.addError("Incompatible database models!");
				return;
			}
			ApplicationInfoDataElement modelInfo = new ApplicationInfoDataElement();
			modelInfo.setData(databaseModel.getSchema().getSchemaDefinition());
			Database installedDb = universalDB.getSchemaIndex().getSchema().getDatabases().stream().filter(db -> db.getName().equals(databaseName)).findFirst().orElse(null);
			if (installedDb == null) {
				for (Table table : database.getTables()) {
					for (Column column : table.getColumns()) {
						modelInfo.added(column.getFQN() + ": " + column.getType());
					}
				}
			} else {
				Set<String> columnNameSet = database.getTables().stream().flatMap(t -> t.getColumns().stream()).map(Column::getFQN).collect(Collectors.toSet());
				Set<String> installedColumnNameSet = installedDb.getTables().stream().flatMap(t -> t.getColumns().stream()).map(Column::getFQN).collect(Collectors.toSet());
				for (Table table : database.getTables()) {
					for (Column column : table.getColumns()) {
						if (!installedColumnNameSet.contains(column.getFQN())) {
							modelInfo.added(column.getFQN() + ": " + column.getType());
						}
					}
				}
				for (Table table : installedDb.getTables()) {
					for (Column column : table.getColumns()) {
						if (!columnNameSet.contains(column.getFQN())) {
							modelInfo.removed(column.getFQN() + ": " + column.getType());
						}
					}
				}
			}
			applicationInfo.setDataModelData(modelInfo);
		} catch (Exception e) {
			applicationInfo.addError("Error checking data model:" + e.getMessage());
			LOGGER.error("Error checking data model:", e);
		}
	}

	@Override
	public void installApplication(ApplicationInfo applicationInfo) {
		SchemaInfoProvider databaseModel = applicationInfo.getBaseApplicationBuilder().getDatabaseModel();
		if (databaseModel == null) {
			return;
		}
		try {
			ClassLoader classLoader = applicationInfo.getApplicationClassLoader();
			if (classLoader == null) {
				classLoader = this.getClass().getClassLoader();
			}
			universalDB.addAuxiliaryModel(databaseModel, classLoader);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Override
	public void loadApplication(ApplicationInfo applicationInfo) {
		SchemaInfoProvider databaseModel = applicationInfo.getBaseApplicationBuilder().getDatabaseModel();
		if (databaseModel == null) {
			return;
		}
		try {
			ClassLoader classLoader = applicationInfo.getApplicationClassLoader();
			if (classLoader == null) {
				classLoader = this.getClass().getClassLoader();
			}
			universalDB.addAuxiliaryModel(databaseModel, classLoader);
		} catch (RuntimeException runtimeException) {
			throw new RuntimeException(runtimeException);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
