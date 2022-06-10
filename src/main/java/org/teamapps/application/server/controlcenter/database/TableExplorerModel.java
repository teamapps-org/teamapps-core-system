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

import org.teamapps.application.api.application.ApplicationInstanceData;
import org.teamapps.universaldb.TableConfig;
import org.teamapps.universaldb.index.ColumnIndex;
import org.teamapps.universaldb.index.IndexType;
import org.teamapps.universaldb.index.SortEntry;
import org.teamapps.universaldb.index.TableIndex;
import org.teamapps.universaldb.index.numeric.IntegerIndex;
import org.teamapps.universaldb.index.numeric.LongIndex;
import org.teamapps.universaldb.query.Filter;
import org.teamapps.universaldb.query.SortDirection;
import org.teamapps.universaldb.query.Sorting;
import org.teamapps.universaldb.schema.Table;
import org.teamapps.ux.component.table.AbstractTableModel;
import org.teamapps.ux.component.timegraph.Interval;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

public class TableExplorerModel extends AbstractTableModel<Integer> {

	private final TableIndex tableIndex;
	private final boolean deletedRecords;
	private final ApplicationInstanceData applicationInstanceData;

	private int recordCount;
	private List<Integer> resultRecords;


	private String query;
	private Sorting currentSorting;
	private Interval timeLineFilterInterval;
	private Function<Integer, Long> timeFilterDataFunction;

	public TableExplorerModel(TableIndex tableIndex, boolean deletedRecords, ApplicationInstanceData applicationInstanceData) {
		this.tableIndex = tableIndex;
		this.deletedRecords = deletedRecords;
		this.applicationInstanceData = applicationInstanceData;
		executeQuery();
	}

	public ColumnIndex getDefaultTimeLineColumn() {
		if (!tableIndex.getTableConfig().getOption(TableConfig.TRACK_MODIFICATION)) {
			List<ColumnIndex> timeLineColumns = getTimeLineColumns();
			return timeLineColumns.size() > 0 ? timeLineColumns.get(0) : null;
		}
		return deletedRecords ? tableIndex.getColumnIndex(Table.FIELD_DELETION_DATE) : tableIndex.getColumnIndex(Table.FIELD_MODIFICATION_DATE);
	}

	public List<ColumnIndex> getTimeLineColumns() {
		return tableIndex.getColumnIndices().stream().filter(c -> c.getColumnType().isDateBased()).collect(Collectors.toList());
	}

	public void setQuery(String query) {
		this.query = query;
		executeQuery();
		onAllDataChanged().fire();
	}

	public void setTimeLineFilter(String field, Interval interval) {
		if (interval == null || field == null || tableIndex.getColumnIndex(field) == null) {
			timeFilterDataFunction = null;
			timeLineFilterInterval = null;
		} else {
			timeLineFilterInterval = interval;
			timeFilterDataFunction = createTimeLineDataFunction(tableIndex.getColumnIndex(field));
		}
		executeQuery();
		onAllDataChanged().fire();
	}

	public Function<Integer, Long> createTimeLineDataFunction(ColumnIndex timeLineFilterColumn) {
		if (timeLineFilterColumn.getType() == IndexType.INT) {
			IntegerIndex index = (IntegerIndex) timeLineFilterColumn;
			return (id) -> (index.getValue(id) * 1000L);
		} else {
			LongIndex index = (LongIndex) timeLineFilterColumn;
			return index::getValue;
		}
	}

	private void executeQuery() {
		BitSet recordBitSet = deletedRecords ? tableIndex.getDeletedRecords() : tableIndex.getRecords();
		List<Integer> result = null;

		if (query != null && !query.isBlank()) {
			Filter fullTextFilter = tableIndex.createFullTextFilter(query);
			recordBitSet = fullTextFilter.filter(recordBitSet);
		}

		if (timeFilterDataFunction != null && timeLineFilterInterval != null) {
			BitSet timeLineRecords = new BitSet();
			long min = timeLineFilterInterval.getMin();
			long max = timeLineFilterInterval.getMax();
			for (int id = recordBitSet.nextSetBit(0); id >= 0; id = recordBitSet.nextSetBit(id + 1)) {
				long value = timeFilterDataFunction.apply(id);
				if (value >= min && value <= max) {
					timeLineRecords.set(id);
				}
			}
			recordBitSet = timeLineRecords;
		}

		if (currentSorting != null) {
			ColumnIndex columnIndex = tableIndex.getColumnIndex(currentSorting.getSortFieldName());
			if (columnIndex != null) {
				List<SortEntry> sortEntries = SortEntry.createSortEntries(recordBitSet);
				columnIndex.sortRecords(sortEntries, currentSorting.getSortDirection() == SortDirection.ASCENDING, applicationInstanceData.getUser());
				result = sortEntries.stream().map(entry -> entry.getId()).collect(Collectors.toList());
			}
		}

		if (result == null) {
			result = new ArrayList<>();
			for (int id = recordBitSet.nextSetBit(0); id >= 0; id = recordBitSet.nextSetBit(id + 1)) {
				result.add(id);
			}
		}
		resultRecords = result;
		recordCount = resultRecords.size();
	}

	@Override
	public int getCount() {
		return recordCount;
	}

	public void setSorting(String fieldName, boolean ascending) {
		if (fieldName == null) {
			currentSorting = null;
		} else {
			currentSorting = new Sorting(fieldName, ascending);
		}
		executeQuery();
	}

	@Override
	public List<Integer> getRecords(int startIndex, int length) {
		return resultRecords.stream().skip(startIndex).limit(length).collect(Collectors.toList());
	}

//	private boolean sortingChanged(Sorting sorting) {
//		if (currentSorting == null && sorting == null) {
//			return false;
//		}
//		if (currentSorting != null && sorting != null &&
//				currentSorting.getFieldName() != null &&
//				currentSorting.getFieldName().equals(sorting.getFieldName()) &&
//				currentSorting.getSorting() == sorting.getSorting()
//		) {
//			return false;
//		}
//		return true;
//	}
}
