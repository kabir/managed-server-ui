package org.wildfly.cli.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class TableOutputter {
    private final List<Integer> columnWidths;
    private final String space;


    private TableOutputter(Builder builder) {
        this.columnWidths = builder.columnWidths;
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < builder.spacing; i++) {
            sb.append(" ");
        }
        this.space = sb.toString();
        outputHeaders(builder.headers);
    }

    public static Builder builder() {
        return new Builder();
    }

    public Row addRow() {
        return new Row();
    }

    private void outputHeaders(List<String> headers) {
        if (headers.size() == 0) {
            return;
        }
        // Output headers
        Row row = addRow();
        for (String header : headers) {
            row.addColumn(header);
        }
        row.output();

        //Output separator
        row = addRow();
        for (int width : columnWidths) {
            char[] arr = new char[width];
            Arrays.fill(arr, '-');
            row.addColumn(new String(arr));
        }
        row.output();
    }



    public class Row{
        private final List<String> columns = new ArrayList<>();

        Row addColumns(String... columns) {
            for (String column : columns) {
                addColumn(column);
            }
            return this;
        }

        Row addColumn(String column) {
            int maxSize = columnWidths.get(columns.size());
            if (column.length() > maxSize) {
                column = column.substring(0, maxSize);
            } else if (column.length() < maxSize) {
                StringBuilder sb = new StringBuilder(column);
                for (int i = column.length() + 1; i <= maxSize; i++) {
                    sb.append(" ");
                }
                column = sb.toString();
            }
            columns.add(column);
            return this;
        }

        public void output() {
            if (columns.size() != columnWidths.size()) {
                throw new IllegalStateException("Not enough columns written");
            }

            for (int i = 0; i < columns.size(); i++) {
                System.out.print(columns.get(i));
                if (i < columns.size() - 1) {
                    System.out.print(space);
                }
            }
            System.out.println();
        }
    }

    public static class Builder {
        private int spacing = 1;
        private final List<Integer> columnWidths = new ArrayList<>();
        private final List<String> headers = new ArrayList<>();

        private Builder() {
        }

        public Builder addColumn(int columnWidth) {
            return addColumn(columnWidth, null);
        }

        public Builder addColumn(int columnWidth, String header) {
            this.columnWidths.add(columnWidth);
            if (header != null) {
                this.headers.add(header);
            }
            return this;
        }

        public Builder setSpacing(int spacing) {
            this.spacing = spacing;
            return this;
        }

        public TableOutputter build() {
            if (headers.size() != columnWidths.size()) {
                throw new IllegalStateException("Either all or no columns need a header");
            }
            return new TableOutputter(this);
        }
    }

    public static void main(String[] args) {
        TableOutputter o = TableOutputter.builder()
                .addColumn(5, "Hello")
                .addColumn(7, "Test")
                .addColumn(3, "THree")
                .setSpacing(3)
                .build();

        o.addRow()
                .addColumn("one")
                .addColumn("ONE")
                .addColumn("ONE")
                .output();
        o.addRow()
                .addColumn("hello")
                .addColumn("SEVENTH")
                .addColumn("ONE")
                .output();
        o.addRow()
                .addColumns("veryverylong", "TRIALANDERROR", "O")
                .output();

    }

}
