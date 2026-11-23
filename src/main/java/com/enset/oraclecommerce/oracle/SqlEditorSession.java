package com.enset.oraclecommerce.oracle;

import jakarta.servlet.http.HttpSessionBindingEvent;
import jakarta.servlet.http.HttpSessionBindingListener;

import java.sql.Connection;
import java.util.Map;

/**
 * One dedicated Oracle connection per browser session, with autocommit OFF.
 * It is the "SQL*Plus session" of the editor: its transaction stays open between
 * two HTTP requests until the user types COMMIT or ROLLBACK.
 * When the HTTP session expires, the open transaction is rolled back and the connection closed.
 */
public class SqlEditorSession implements HttpSessionBindingListener {

    public static final String ATTR = "sqlEditorSession";

    private final Connection connection;

    /** Counters at the last COMMIT / ROLLBACK (redo size, db block changes). */
    private volatile Map<String, Long> txBase = Map.of();
    /** Counters at the creation of the session (redo synch writes, user commits). */
    private volatile Map<String, Long> sessionBase = Map.of();

    public SqlEditorSession(Connection connection) {
        this.connection = connection;
    }

    /** Number of DML statements run since the last COMMIT / ROLLBACK. */
    private volatile int pending;
    public int pending() { return pending; }
    public void pendingUp() { pending++; }
    public void pendingReset() { pending = 0; }

    public Connection connection() { return connection; }
    public Map<String, Long> txBase() { return txBase; }
    public Map<String, Long> sessionBase() { return sessionBase; }
    public void txBase(Map<String, Long> m) { this.txBase = m; }
    public void sessionBase(Map<String, Long> m) { this.sessionBase = m; }

    @Override
    public void valueUnbound(HttpSessionBindingEvent event) {
        close();
    }

    public void close() {
        try { connection.rollback(); } catch (Exception ignored) { }
        try { connection.close(); } catch (Exception ignored) { }
    }
}
