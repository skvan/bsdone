/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.bsball.config.AuditColumnOrderingStrategy
 *  org.hibernate.boot.Metadata
 *  org.hibernate.boot.model.relational.ColumnOrderingStrategy
 *  org.hibernate.dialect.temptable.TemporaryTableColumn
 *  org.hibernate.mapping.Column
 *  org.hibernate.mapping.Constraint
 *  org.hibernate.mapping.Table
 *  org.hibernate.mapping.UserDefinedObjectType
 */
package com.bsball.config;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import org.hibernate.boot.Metadata;
import org.hibernate.boot.model.relational.ColumnOrderingStrategy;
import org.hibernate.dialect.temptable.TemporaryTableColumn;
import org.hibernate.mapping.Column;
import org.hibernate.mapping.Constraint;
import org.hibernate.mapping.Table;
import org.hibernate.mapping.UserDefinedObjectType;

/*
 * Exception performing whole class analysis ignored.
 */
public class AuditColumnOrderingStrategy
implements ColumnOrderingStrategy {
    private static final List<String> AUDIT_ORDER = List.of((Object)"created_by", (Object)"created_at", (Object)"updated_by", (Object)"updated_at", (Object)"deleted_by", (Object)"deleted_at");

    private static String toCanonicalKey(String name) {
        if (name == null) {
            return null;
        }
        String n = name.replace("_", "").replace("\"", "").toLowerCase();
        if (n.equals("createdby")) {
            return "created_by";
        }
        if (n.equals("createdat")) {
            return "created_at";
        }
        if (n.equals("updatedby")) {
            return "updated_by";
        }
        if (n.equals("updatedat")) {
            return "updated_at";
        }
        if (n.equals("deletedby")) {
            return "deleted_by";
        }
        if (n.equals("deletedat")) {
            return "deleted_at";
        }
        return null;
    }

    public List<Column> orderTableColumns(Table table, Metadata metadata) {
        Collection columns = table.getColumns();
        if (columns == null || columns.isEmpty()) {
            return null;
        }
        ArrayList<Column> others = new ArrayList<Column>();
        LinkedHashMap<String, Column> audit = new LinkedHashMap<String, Column>();
        for (String name : AUDIT_ORDER) {
            audit.put(name, null);
        }
        for (Column col : columns) {
            String name = AuditColumnOrderingStrategy.getColumnName((Column)col);
            String key = AuditColumnOrderingStrategy.toCanonicalKey((String)name);
            if (key != null && AUDIT_ORDER.contains(key)) {
                audit.put(key, col);
                continue;
            }
            others.add(col);
        }
        ArrayList<Column> result = new ArrayList<Column>(others);
        for (String name : AUDIT_ORDER) {
            Column c = (Column)audit.get(name);
            if (c == null) continue;
            result.add(c);
        }
        table.reorderColumns(result);
        return result;
    }

    public List<Column> orderConstraintColumns(Constraint constraint, Metadata metadata) {
        return null;
    }

    public void orderTemporaryTableColumns(List<TemporaryTableColumn> temporaryTableColumns, Metadata metadata) {
    }

    public List<Column> orderUserDefinedTypeColumns(UserDefinedObjectType userDefinedObjectType, Metadata metadata) {
        return null;
    }

    private static String getColumnName(Column column) {
        if (column == null) {
            return null;
        }
        try {
            String t = column.getText();
            if (t != null && !t.isEmpty()) {
                return t.replace("\"", "").trim();
            }
        }
        catch (Exception exception) {
            // empty catch block
        }
        return null;
    }
}

