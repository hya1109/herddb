/*
 Licensed to Diennea S.r.l. under one
 or more contributor license agreements. See the NOTICE file
 distributed with this work for additional information
 regarding copyright ownership. Diennea S.r.l. licenses this file
 to you under the Apache License, Version 2.0 (the
 "License"); you may not use this file except in compliance
 with the License.  You may obtain a copy of the License at

 http://www.apache.org/licenses/LICENSE-2.0

 Unless required by applicable law or agreed to in writing,
 software distributed under the License is distributed on an
 "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 KIND, either express or implied.  See the License for the
 specific language governing permissions and limitations
 under the License.

 */
package herddb.model;

import herddb.utils.ExtendedDataInputStream;
import herddb.utils.ExtendedDataOutputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Index definition
 *
 * @author enrico.olivelli
 */
public class Index implements ColumnsList {

    public static final String TYPE_HASH = "hash";

    public final String name;
    public final String table;
    public final String type;
    public final String tablespace;
    public final Column[] columns;
    public final String[] columnNames;
    public final Map<String, Column> columnByName = new HashMap<>();

    @Override
    public String[] getPrimaryKey() {
        return columnNames;
    }

    private Index(String name, String table, String tablespace, String type, Column[] columns) {
        this.name = name;
        this.table = table;
        this.tablespace = tablespace;
        this.columns = columns;
        this.type = type;
        this.columnNames = new String[columns.length];
        int i = 0;
        for (Column c : columns) {
            this.columnNames[i++] = c.name;
            columnByName.put(c.name, c);
        }
    }

    @Override
    public Column[] getColumns() {
        return columns;
    }

    @Override
    public Column getColumn(String name) {
        return columnByName.get(name);
    }

    public static Builder builder() {
        return new Builder();
    }

    public static Index deserialize(byte[] data) {
        try {
            ByteArrayInputStream ii = new ByteArrayInputStream(data);
            ExtendedDataInputStream dii = new ExtendedDataInputStream(ii);
            String tablespace = dii.readUTF();
            String name = dii.readUTF();
            String table = dii.readUTF();
            int flags = dii.readVInt(); // for future implementations
            String type = dii.readUTF();
            int ncols = dii.readVInt();
            Column[] columns = new Column[ncols];
            for (int i = 0; i < ncols; i++) {
                String cname = dii.readUTF();
                int ctype = dii.readVInt();
                int serialPosition = dii.readVInt();
                dii.readVInt(); // for future implementations
                columns[i] = Column.column(cname, ctype, serialPosition);
            }
            return new Index(name, table, tablespace, type, columns);
        } catch (IOException err) {
            throw new IllegalArgumentException(err);
        }
    }

    public byte[] serialize() {
        ByteArrayOutputStream oo = new ByteArrayOutputStream();
        try (ExtendedDataOutputStream doo = new ExtendedDataOutputStream(oo);) {
            doo.writeUTF(tablespace);
            doo.writeUTF(name);
            doo.writeUTF(table);
            doo.writeVInt(0); // for future implementation
            doo.writeUTF(type);
            doo.writeVInt(columns.length);
            for (Column c : columns) {
                doo.writeUTF(c.name);
                doo.writeVInt(c.type);
                doo.writeVInt(c.serialPosition);
                doo.writeVInt(0); // flags for future implementations
            }
        } catch (IOException ee) {
            throw new RuntimeException(ee);
        }
        return oo.toByteArray();
    }

    public static class Builder {

        private final List<Column> columns = new ArrayList<>();
        private String name;
        private String table;
        private String type = TYPE_HASH;
        private String tablespace = TableSpace.DEFAULT;

        private Builder() {
        }

        public Builder onTable(Table table) {
            this.table = table.name;
            this.tablespace = table.tablespace;
            return this;
        }

        public Builder name(String name) {
            this.name = name;
            return this;
        }

        public Builder type(String type) {
            this.type = type;
            return this;
        }

        public Builder table(String table) {
            this.table = table;
            return this;
        }

        public Builder tablespace(String tablespace) {
            this.tablespace = tablespace;
            return this;
        }

        public Builder column(String name, int type) {
            if (name == null || name.isEmpty()) {
                throw new IllegalArgumentException();
            }
            if (this.columns.stream().filter(c -> (c.name.equals(name))).findAny().isPresent()) {
                throw new IllegalArgumentException("column " + name + " already exists");
            }
            this.columns.add(Column.column(name, type, 0));
            return this;
        }

        public Index build() {
            if (table == null || table.isEmpty()) {
                throw new IllegalArgumentException("table is not defined");
            }
            if (!TYPE_HASH.equals(type)) {
                throw new IllegalArgumentException("only index type " + TYPE_HASH + " is supported");
            }
            if (columns.isEmpty()) {
                throw new IllegalArgumentException("specify at least one column to index");
            }
            if (name == null || name.isEmpty()) {
                name = table + "_" + columns.stream().map(s -> s.name.toLowerCase()).collect(Collectors.joining("_"));
            }

            return new Index(name, table, tablespace, type, columns.toArray(new Column[columns.size()]));
        }

    }

}