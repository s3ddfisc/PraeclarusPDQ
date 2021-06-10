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

package com.processdataquality.praeclarus.writer;

import com.processdataquality.praeclarus.plugin.Options;
import tech.tablesaw.api.Table;

import java.io.IOException;
import java.io.OutputStream;

/**
 * @author Michael Adams
 * @date 9/6/21
 */
public abstract class AbstractDataWriter implements DataWriter {

    protected final Options _options = new Options();
    protected OutputStream _stream;

    protected AbstractDataWriter() { }


    @Override
    public abstract void write(Table table) throws IOException;


    @Override
    public Options getOptions() {
        return _options;
    }

    @Override
    public int getMaxInputs() {
        return 1;
    }

    @Override
    public int getMaxOutputs() {
        return 0;
    }

    @Override
    public OutputStream getOutputStream() {
        return _stream;
    }

    @Override
    public void setOutputStream(OutputStream stream) {
        _stream = stream;
    }

}