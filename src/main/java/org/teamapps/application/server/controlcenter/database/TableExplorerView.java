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
package org.teamapps.application.server.controlcenter.database;

import org.apache.commons.io.FileUtils;
import org.teamapps.application.api.application.AbstractApplicationView;
import org.teamapps.application.api.application.ApplicationInstanceData;
import org.teamapps.application.api.localization.Dictionary;
import org.teamapps.application.api.theme.ApplicationIcons;
import org.teamapps.application.ux.combo.ComboBoxUtils;
import org.teamapps.application.ux.form.FormPanel;
import org.teamapps.common.format.Color;
import org.teamapps.common.format.RgbaColor;
import org.teamapps.data.extract.PropertyProvider;
import org.teamapps.data.value.SortDirection;
import org.teamapps.databinding.TwoWayBindableValue;
import org.teamapps.event.Event;
import org.teamapps.universaldb.index.ColumnIndex;
import org.teamapps.universaldb.index.ColumnType;
import org.teamapps.universaldb.index.TableIndex;
import org.teamapps.universaldb.index.file.FileIndex;
import org.teamapps.universaldb.index.file.FileValue;
import org.teamapps.universaldb.index.numeric.IntegerIndex;
import org.teamapps.universaldb.index.numeric.LongIndex;
import org.teamapps.universaldb.index.numeric.ShortIndex;
import org.teamapps.universaldb.index.reference.multi.MultiReferenceIndex;
import org.teamapps.universaldb.index.reference.single.SingleReferenceIndex;
import org.teamapps.universaldb.index.text.TextIndex;
import org.teamapps.universaldb.index.translation.TranslatableText;
import org.teamapps.universaldb.index.translation.TranslatableTextIndex;
import org.teamapps.ux.application.view.View;
import org.teamapps.ux.component.Component;
import org.teamapps.ux.component.absolutelayout.Length;
import org.teamapps.ux.component.field.*;
import org.teamapps.ux.component.field.combobox.ComboBox;
import org.teamapps.ux.component.field.datetime.InstantDateTimeField;
import org.teamapps.ux.component.field.datetime.LocalDateField;
import org.teamapps.ux.component.field.datetime.LocalTimeField;
import org.teamapps.ux.component.field.richtext.RichTextEditor;
import org.teamapps.ux.component.form.ResponsiveForm;
import org.teamapps.ux.component.form.ResponsiveFormLayout;
import org.teamapps.ux.component.format.Spacing;
import org.teamapps.ux.component.panel.Panel;
import org.teamapps.ux.component.table.ListTableModel;
import org.teamapps.ux.component.table.Table;
import org.teamapps.ux.component.table.TableColumn;
import org.teamapps.ux.component.template.BaseTemplate;
import org.teamapps.ux.component.timegraph.*;
import org.teamapps.ux.component.timegraph.graph.LineGraph;
import org.teamapps.ux.component.timegraph.model.LineGraphModel;
import org.teamapps.ux.component.timegraph.model.timestamps.PartitioningTimestampsLineGraphModel;
import org.teamapps.ux.component.timegraph.model.timestamps.StaticTimestampsModel;

import java.io.File;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

public class TableExplorerView extends AbstractApplicationView {

	private final TableIndex tableIndex;
	private final View timeLineView;
	private final View tableView;
	private final View formView;
	private final boolean deletedRecords;

	public TableExplorerView(ApplicationInstanceData applicationInstanceData, TableIndex tableIndex, View timeLineView, View tableView, View formView, boolean deletedRecords) {
		super(applicationInstanceData);
		this.tableIndex = tableIndex;
		this.timeLineView = timeLineView;
		this.tableView = tableView;
		this.formView = formView;
		this.deletedRecords = deletedRecords;
		createUi();
	}

	private void createUi() {
		TableExplorerModel model = new TableExplorerModel(tableIndex, deletedRecords, getApplicationInstanceData());

		Map<String, Function<Integer, Object>> fieldValueFunctionMap = createFieldValueFunctionMap(tableIndex);
		Table<Integer> table = new Table<>();
		table.setModel(model);
		table.onSortingChanged.addListener(event -> model.setSorting(event.getSortField(), event.getSortDirection() == SortDirection.ASC));
		table.setDisplayAsList(true);
		table.setPropertyProvider(createTablePropertyProvider(fieldValueFunctionMap, tableIndex));

		addColumn("ID", new NumberField(0), 70, table);
		createTableFields(tableIndex, table);

		ResponsiveForm<?> form = new ResponsiveForm<>(120, 200, 0);
		ResponsiveFormLayout formLayout = form.addResponsiveFormLayout(450);
		formLayout.addSection().setCollapsible(false).setDrawHeaderLine(false);
		addFormColumn("ID", new NumberField(0), formLayout);
		createFormFields(tableIndex, formLayout, table.onSingleRowSelected);
		table.onSingleRowSelected.addListener(id -> {
			form.setFieldValue("ID", id);
			tableIndex.getColumnIndices().stream().filter(c -> c.getColumnType() != ColumnType.MULTI_REFERENCE).forEach(c -> {
				Function<Integer, Object> valueFunction = fieldValueFunctionMap.get(c.getName());
				if (valueFunction != null) {
					form.setFieldValue(c.getName(), valueFunction.apply(id));
				}
			});
		});

		ColumnIndex defaultTimeLineColumn = model.getDefaultTimeLineColumn();
		if (defaultTimeLineColumn != null) {
			TwoWayBindableValue<ColumnIndex> timeLineColumn = TwoWayBindableValue.create();
			List<ColumnIndex> timeLineColumns = model.getTimeLineColumns();
			ComboBox<ColumnIndex> timeLineComboBox = ComboBoxUtils.createRecordComboBox(timeLineColumns, (columnIndex, collection) -> {
				Map<String, Object> map = new HashMap<>();
				map.put(BaseTemplate.PROPERTY_ICON, ApplicationIcons.CALENDAR_CLOCK);
				map.put(BaseTemplate.PROPERTY_CAPTION, DbExplorerUtils.createTitleFromCamelCase(columnIndex.getName()));
				return map;
			}, BaseTemplate.LIST_ITEM_MEDIUM_ICON_SINGLE_LINE);
			timeLineComboBox.setValue(defaultTimeLineColumn);
			timeLineComboBox.onValueChanged.addListener(timeLineColumn::set);

			RgbaColor color = Color.MATERIAL_BLUE_700;
			TimeGraph timeGraph = new TimeGraph();
			StaticTimestampsModel timestampsModel = new StaticTimestampsModel() {
				@Override
				public Interval getDomainX() {
					Interval domainX = super.getDomainX();
					long diff = (domainX.getMax() - domainX.getMin()) / 20;
					return new Interval(domainX.getMin() - diff, domainX.getMax() + diff);
				}
			};
			timeLineColumn.onChanged().addListener(col -> {
				Function<Integer, Long> timeLineDataFunction = model.createTimeLineDataFunction(col);
				BitSet recordBitSet = deletedRecords ? tableIndex.getDeletedRecords() : tableIndex.getRecords();
				long[] data = new long[recordBitSet.cardinality()];
				int pos = 0;
				for (int id = recordBitSet.nextSetBit(0); id >= 0; id = recordBitSet.nextSetBit(id + 1)) {
					data[pos] = timeLineDataFunction.apply(id);
					pos++;
				}
				timestampsModel.setEventTimestamps(data);
			});
			timeLineColumn.set(defaultTimeLineColumn);
			LineGraphModel lineGraphModel = new PartitioningTimestampsLineGraphModel(timestampsModel);
			LineGraph lineGraph = new LineGraph(lineGraphModel, LineChartCurveType.MONOTONE, 0.5f, color, color.withAlpha(0.05f));
			lineGraph.setAreaColorScaleMin(color.withAlpha(0.05f));
			lineGraph.setAreaColorScaleMax(color.withAlpha(0.5f));
			lineGraph.setYScaleType(ScaleType.SYMLOG);
			lineGraph.setYScaleZoomMode(LineChartYScaleZoomMode.DYNAMIC_INCLUDING_ZERO);
			lineGraph.setYZeroLineVisible(false);
			timeGraph.addGraph(lineGraph);

			timeGraph.onIntervalSelected.addListener(interval -> model.setTimeLineFilter(timeLineComboBox.getValue().getName(), interval));
			timeLineView.getPanel().setRightHeaderField(timeLineComboBox);
			timeLineView.setComponent(timeGraph);
		} else {
			timeLineView.setComponent(null);
		}

		tableView.setComponent(table);
		tableView.setTitle(tableIndex.getName() + " (" + model.getCount() + ")");
		model.onAllDataChanged().addListener(() -> tableView.setTitle(tableIndex.getName() + " (" + model.getCount() + ")"));
		TextField queryField = new TextField();
		queryField.setEmptyText(getLocalized(Dictionary.SEARCH___));
		queryField.onTextInput.addListener(model::setQuery);
		tableView.getPanel().setRightHeaderField(queryField);

		formView.setComponent(form);
		table.onSingleRowSelected.addListener(id -> formView.setTitle("ID: " + id));


	}

	private Map<String, Function<Integer, Object>> createFieldValueFunctionMap(TableIndex tableIndex) {
		Map<String, Function<Integer, Object>> fieldValueFunctionMap = new HashMap<>();
		for (ColumnIndex<?, ?> columnIndex : tableIndex.getColumnIndices()) {
			Function<Integer, Object> valueFunction = columnIndex::getGenericValue;
			String fieldName = columnIndex.getName();
			switch (columnIndex.getColumnType()) {
				case TRANSLATABLE_TEXT -> {
					TranslatableTextIndex translatableTextIndex = (TranslatableTextIndex) columnIndex;
					valueFunction = (id) -> {
						TranslatableText value = translatableTextIndex.getValue(id);
						return value != null ? value.getText() : null;
					};
				}
				case FILE -> {
					FileIndex fileIndex = (FileIndex) columnIndex;
					valueFunction = (id) -> {
						FileValue fileValue = fileIndex.getValue(id);
						if (fileValue != null) {
							String fileName = fileValue.getFileName();
							String size = FileUtils.byteCountToDisplaySize(fileValue.getSize());
							File file = fileValue.retrieveFile();
							return fileName + " (" + size + ")" + ", " + file.length() + ", " + file.getPath();
						} else {
							return null;
						}
					};
				}
				case SINGLE_REFERENCE -> {
					SingleReferenceIndex singleReferenceIndex = (SingleReferenceIndex) columnIndex;
					List<ColumnIndex> textIndices = singleReferenceIndex.getReferencedTable().getColumnIndices().stream()
							.filter(c -> c.getColumnType() == ColumnType.TEXT)
							.limit(3)
							.collect(Collectors.toList());
					valueFunction = (id) -> {
						int refId = singleReferenceIndex.getValue(id);
						return refId == 0 ? null : "➞" + refId + ": " + textIndices.stream().map(idx -> idx.getStringValue(refId)).filter(Objects::nonNull).collect(Collectors.joining(" "));
					};
				}
				case MULTI_REFERENCE -> {
					MultiReferenceIndex multiReferenceIndex = (MultiReferenceIndex) columnIndex;
					valueFunction = (id) -> {
						int count = multiReferenceIndex.getReferencesCount(id);
						if (count == 0) {
							return null;
						} else {
							List<Integer> references = multiReferenceIndex.getReferencesAsList(id);
							return count + " (" + references.stream().limit(25).map(v -> "" + v).collect(Collectors.joining(", ")) + ")";
						}
					};
				}
				case TIMESTAMP -> valueFunction = (id) -> {
					IntegerIndex integerIndex = (IntegerIndex) columnIndex;
					int value = integerIndex.getValue(id);
					return value == 0 ? null : Instant.ofEpochSecond(value);
				};
				case DATE -> valueFunction = (id) -> {
					LongIndex integerIndex = (LongIndex) columnIndex;
					long value = integerIndex.getValue(id);
					return value == 0 ? null : Instant.ofEpochMilli(value).atOffset(ZoneOffset.UTC).toLocalDate();
				};
				case TIME -> valueFunction = (id) -> {
					IntegerIndex integerIndex = (IntegerIndex) columnIndex;
					int value = integerIndex.getValue(id);
					return value == 0 ? null : Instant.ofEpochSecond(value).atOffset(ZoneOffset.UTC).toLocalTime();
				};
				case DATE_TIME -> valueFunction = (id) -> {
					LongIndex longIndex = (LongIndex) columnIndex;
					long value = longIndex.getValue(id);
					return value == 0 ? null : Instant.ofEpochMilli(value);
				};
				case LOCAL_DATE -> valueFunction = (id) -> {
					LongIndex longIndex = (LongIndex) columnIndex;
					long value = longIndex.getValue(id);
					return value == 0 ? null : Instant.ofEpochMilli(value).atOffset(ZoneOffset.UTC).toLocalDate();
				};
				case ENUM -> {
					List<String> enumValues = columnIndex.getTable().getTable().getColumn(columnIndex.getName()).getEnumValues();
					ShortIndex enumIndex = (ShortIndex) columnIndex;
					valueFunction = (id) -> {
						int value = enumIndex.getValue(id);
						return value == 0 ? null : enumValues.get(value - 1);
					};
				}
				case BINARY -> valueFunction = columnIndex::getStringValue;
			}
			fieldValueFunctionMap.put(fieldName, valueFunction);
		}
		return fieldValueFunctionMap;
	}

	private void createFormFields(TableIndex tableIndex, ResponsiveFormLayout formLayout, Event<Integer> onTableRowSelected) {
		for (ColumnIndex<?, ?> columnIndex : getSortedColumns(tableIndex)) {
			String fieldName = columnIndex.getName();
			if (isMetaUserColumn(columnIndex)) {
				addFormColumn(fieldName, getApplicationInstanceData().getComponentFactory().createUserTemplateField(), formLayout);
				continue;
			}
			switch (columnIndex.getColumnType()) {
				case BOOLEAN, BITSET_BOOLEAN -> addFormColumn(fieldName, new CheckBox(), formLayout);
				case SHORT, INT, LONG -> addFormColumn(fieldName, new NumberField(0), formLayout);
				case FLOAT, DOUBLE -> addFormColumn(fieldName, new NumberField(2), formLayout);
				case TEXT -> addFormColumn(fieldName, getFormTextField((TextIndex) columnIndex), formLayout);
				case TRANSLATABLE_TEXT, BINARY, ENUM, SINGLE_REFERENCE, FILE -> addFormColumn(fieldName, new TextField(), formLayout);
				case MULTI_REFERENCE -> {
					MultiReferenceIndex multiReferenceIndex = (MultiReferenceIndex) columnIndex;
					TableIndex referencedTable = multiReferenceIndex.getReferencedTable();
					Map<String, Function<Integer, Object>> subTableValueFunctionMap = createFieldValueFunctionMap(referencedTable);
					Table<Integer> subTable = new Table<>();
					ListTableModel<Integer> listTableModel = new ListTableModel<>();
					subTable.setModel(listTableModel);
					subTable.setPropertyProvider(createTablePropertyProvider(subTableValueFunctionMap, referencedTable));
					subTable.setDisplayAsList(true);
					createTableFields(referencedTable, subTable);
					FormPanel formPanel = new FormPanel(getApplicationInstanceData());
					formPanel.setTopSpace(100);
					formPanel.setTable(subTable, true, false, false);
					Panel panel = formPanel.getPanel();
					panel.setHideTitleBar(false);
					panel.setIcon(ApplicationIcons.TABLE);
					String title = getTitle(fieldName);
					panel.setTitle(title);
					addFormColumn(fieldName, panel, formLayout);

					onTableRowSelected.addListener(selectedId -> {
						List<Integer> referencesAsList = multiReferenceIndex.getReferencesAsList(selectedId);
						listTableModel.setList(referencesAsList);
						panel.setTitle(title + " (" + referencesAsList.size() + ")");
					});
				}
				case TIMESTAMP, DATE_TIME -> addFormColumn(fieldName, new InstantDateTimeField(), formLayout);
				case DATE, LOCAL_DATE -> addFormColumn(fieldName, new LocalDateField(), formLayout);
				case TIME -> addFormColumn(fieldName, new LocalTimeField(), formLayout);
			}
		}
	}

	private void createTableFields(TableIndex tableIndex, Table<Integer> table) {
		for (ColumnIndex<?, ?> columnIndex : getSortedColumns(tableIndex)) {
			String fieldName = columnIndex.getName();
			if (isMetaUserColumn(columnIndex)) {
				addColumn(fieldName, getApplicationInstanceData().getComponentFactory().createUserTemplateField(), 250, table);
				continue;
			}
			switch (columnIndex.getColumnType()) {
				case BOOLEAN, BITSET_BOOLEAN -> addColumn(fieldName, new CheckBox(), 70, table);
				case SHORT, INT, LONG -> addColumn(fieldName, new NumberField(0), 70, table);
				case FLOAT, DOUBLE -> addColumn(fieldName, new NumberField(2), 100, table);
				case TEXT -> addColumn(fieldName, new TextField(), 200, table);
				case TRANSLATABLE_TEXT -> addColumn(fieldName, new TextField(), 210, table);
				case FILE -> addColumn(fieldName, new TextField(), 180, table);
				case SINGLE_REFERENCE -> addColumn(fieldName, new TextField(), 250, table);
				case MULTI_REFERENCE -> addColumn(fieldName, new TextField(), 175, table);
				case TIMESTAMP -> addColumn(fieldName, new InstantDateTimeField(), 200, table);
				case DATE -> addColumn(fieldName, new LocalDateField(), 200, table);
				case TIME -> addColumn(fieldName, new LocalTimeField(), 200, table);
				case DATE_TIME -> addColumn(fieldName, new InstantDateTimeField(), 170, table);
				case LOCAL_DATE -> addColumn(fieldName, new LocalDateField(), 150, table);
				case ENUM -> addColumn(fieldName, new TextField(), 150, table);
				case BINARY -> addColumn(fieldName, new TextField(), 120, table);
			}
		}
	}

	private boolean isMetaUserColumn(ColumnIndex<?, ?> columnIndex) {
		String fieldName = columnIndex.getName();
		if (columnIndex.getColumnType() == ColumnType.INT && isMetaField(fieldName)) {
			if (
					fieldName.equals(org.teamapps.universaldb.schema.Table.FIELD_CREATED_BY) ||
					fieldName.equals(org.teamapps.universaldb.schema.Table.FIELD_MODIFIED_BY) ||
					fieldName.equals(org.teamapps.universaldb.schema.Table.FIELD_DELETED_BY) ||
					fieldName.equals(org.teamapps.universaldb.schema.Table.FIELD_RESTORED_BY)
			)			{
				return true;
			}
		}
		return false;
	}

	private boolean isMetaField(String fieldName) {
		return org.teamapps.universaldb.schema.Table.isReservedMetaName(fieldName);
	}

	private List<ColumnIndex> getSortedColumns(TableIndex tableIndex) {
		return tableIndex.getColumnIndices().stream().sorted((o1, o2) -> {
			if (isMetaField(o1) && !isMetaField(o2)) {
				return 1;
			} else if (!isMetaField(o1) && isMetaField(o2)) {
				return -1;
			} else {
				return 0;
			}
		}).collect(Collectors.toList());
	}

	private AbstractField<String> getFormTextField(TextIndex textIndex) {
		BitSet bitSet = tableIndex.getRecords();
		int checkedRecords = 0;
		int maxLength = 0;
		boolean containsHtml = false;
		boolean containsNewLines = false;
		for (int id = bitSet.nextSetBit(0); id >= 0; id = bitSet.nextSetBit(id + 1)) {
			String value = textIndex.getValue(id);
			if (value != null) {
				if (value.toLowerCase().contains("</p>") || value.toLowerCase().contains("</td>")) {
					containsHtml = true;
				}
				if (value.contains("\n")) {
					containsNewLines = true;
					break;
				}
				maxLength = Math.max(maxLength, value.length());
			}
			checkedRecords++;
			if (checkedRecords > 1000) {
				break;
			}
		}
		AbstractField<String> field;
		if (containsHtml) {
			field = new RichTextEditor();
			field.setCssStyle("height", Length.ofPixels(350).toCssString());

		} else if (containsNewLines || maxLength > 255) {
			field = new MultiLineTextField();
			field.setCssStyle("height", Length.ofPixels(250).toCssString());
		} else {
			field = new TextField();
		}
		return field;
	}

	private boolean isMetaField(ColumnIndex<?, ?> columnIndex) {
		return org.teamapps.universaldb.schema.Table.isReservedMetaName(columnIndex.getName());
	}

	private void addFormColumn(String fieldName, AbstractField<?> field, ResponsiveFormLayout formLayout) {
		String label = getTitle(fieldName);
		formLayout.addLabelAndField(null, label, fieldName, field);
		if (isMetaField(fieldName)) {
			field.setEditingMode(FieldEditingMode.READONLY);
		}
	}

	private void addFormColumn(String fieldName, Component component, ResponsiveFormLayout formLayout) {
		formLayout.addSection().setCollapsible(false).setDrawHeaderLine(false).setMargin(Spacing.px(0, 5));
		formLayout.addLabelAndComponent( component);
		formLayout.addSection().setCollapsible(false).setDrawHeaderLine(false).setMargin(Spacing.px(0, 5));;
	}

	private String getTitle(String fieldName) {
		return DbExplorerUtils.createTitleFromCamelCase(fieldName);
	}

	private void addColumn(String name, AbstractField<?> field, int width, Table<Integer> table) {
		TableColumn<Integer, ?> column = new TableColumn<>(name, getTitle(name), field);
		column.setDefaultWidth(width);
		table.addColumn(column);
	}

	private PropertyProvider<Integer> createTablePropertyProvider(Map<String, Function<Integer, Object>> fieldValueFunctionMap, TableIndex tableIndex) {
		return (id, collection) -> {
			Map<String, Object> map = new HashMap<>();
			map.put("ID", id);
			for (ColumnIndex<?, ?> columnIndex : tableIndex.getColumnIndices()) {
				Function<Integer, Object> valueFunction = fieldValueFunctionMap.get(columnIndex.getName());
				map.put(columnIndex.getName(), valueFunction.apply(id));
			}
			return map;
		};
	}


}
