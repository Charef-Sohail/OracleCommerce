package com.enset.oraclecommerce.oracle;

import java.util.List;

/**
 * One user request that actually reached Oracle (through Hibernate),
 * with the SQL statements it triggered.
 */
public record OracleEvent(
        long id,
        long timestamp,
        String method,        // GET / POST
        String path,          // /produits, /categories/3/edit ...
        String operation,     // Ajout / Modification / Suppression / Consultation
        String entity,        // produit, catégorie, client ...
        String label,         // "Ajout produit"
        boolean write,        // true if an INSERT / UPDATE / DELETE ran
        int status,
        long durationMs,
        List<Stmt> statements,
        String source,        // "app" (Hibernate request) | "sql" (SQL editor, own session) | "sql-other" (editor, other session)
        boolean committed,    // false for editor DML: the change is NOT validated yet
        Tx tx                 // real transaction figures (V$MYSTAT / V$TRANSACTION), editor events only
) {
    /** A statement type + table, collapsed: 12 identical SELECTs = one Stmt with count 12. */
    public record Stmt(String type, String table, String sql, int count) {}

    /**
     * Real numbers measured on the editor session around one statement.
     * tx* = whole open transaction, stmt* = this statement only.
     */
    public record Tx(boolean open, String xid, long undoBlocks, long undoRecords,
                     long txRedoBytes, long txBlockChanges,
                     long stmtRedoBytes, long stmtBlockChanges,
                     long redoSynchWrites, int pending) {}

    public record Page(long last, List<OracleEvent> events) {}
}
