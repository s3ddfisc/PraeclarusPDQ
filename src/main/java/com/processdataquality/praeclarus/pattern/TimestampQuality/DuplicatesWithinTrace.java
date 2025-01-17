/*
 * Copyright (c) 2021 Queensland University of Technology
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language
 * governing permissions and limitations under the License.
 */

package com.processdataquality.praeclarus.pattern.TimestampQuality;

import com.processdataquality.praeclarus.annotations.Pattern;
import com.processdataquality.praeclarus.annotations.Plugin;
import com.processdataquality.praeclarus.option.ColumnNameListOption;
import com.processdataquality.praeclarus.option.Options;
import com.processdataquality.praeclarus.pattern.PatternGroup;
import com.processdataquality.praeclarus.plugin.uitemplate.PluginUI;

import tech.tablesaw.api.Row;
import tech.tablesaw.api.Table;
import tech.tablesaw.selection.Selection;

/**
 * A base class for imperfect label plugins
 * 
 * @author Dominik Fischer
 * @date 17/08/22
 */

@Plugin(name = "Duplicates within Trace", author = "Dominik Fischer", version = "1.0", synopsis = "Detects duplicate timestamps within activities")
@Pattern(group = PatternGroup.UNGROUPED)

public class DuplicatesWithinTrace extends TimestampPattern {

    protected PluginUI _ui;
    protected Options options;
    protected Table _detected;

    public DuplicatesWithinTrace() {
        super();
        getOptions().addDefault(new ColumnNameListOption("Trace Column"));
    }

    @Override
    public void createErrorTableModel(Table table) {
        setErrorTable(table.emptyCopy());
    }

    @Override
    public void detectErrors(Table table) {
        Selection emptyTimestamps = Selection.with();
        for (Row row : table) {
            if (row.getDateTime(getSelectedColumnNameValue("Timestamp Column")) == null) {
                emptyTimestamps.add(row.getRowNumber());
            }
        }
        table.dropRows(emptyTimestamps.toArray());
        table = table.sortOn(getSelectedColumnNameValue("Trace Column"), 
        getSelectedColumnNameValue("Timestamp Column"));
		boolean prevEventDetected = false;
		for (Row row : table) {
			if (row.getRowNumber() != 0 && table.row(row.getRowNumber())
            .getDateTime(getSelectedColumnNameValue("Timestamp Column")) == table.row(row.getRowNumber() - 1)
           .getDateTime(getSelectedColumnNameValue("Timestamp Column")) && table.row(row.getRowNumber())
            .getString(getSelectedColumnNameValue("Trace Column")).equals(table.row(row.getRowNumber() - 1)
            .getString(getSelectedColumnNameValue("Trace Column")))) {
				_detected.append(row);
				if (!prevEventDetected) {
                    _detected.append(table.row(row.getRowNumber()-1));
                } 
                prevEventDetected = true;
			}
            else {
                prevEventDetected = false;
            }
		}
    }
}

//