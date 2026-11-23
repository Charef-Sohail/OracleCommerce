package com.enset.oraclecommerce.service;

import com.enset.oraclecommerce.oracle.OracleEvent;
import com.enset.oraclecommerce.oracle.OracleEventBuffer;
import com.enset.oraclecommerce.oracle.RequestTrace;
import com.enset.oraclecommerce.oracle.SqlEditorSession;
import jakarta.servlet.http.HttpSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.sql.*;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Backend of the SQL editor tab.
 *
 * "editor" mode : statements run on the session's dedicated connection (autocommit OFF).
 *                 INSERT / UPDATE / DELETE stay uncommitted until COMMIT.
 * "other" mode  : SELECT only, on a brand-new pooled connection = what ANOTHER user sees.
 *
 * Uses the connected user (SYSTEM) for V$ views, like OracleInternalsService.
 */
@Service
public class SqlEditorService {

    private static final Logger log = LoggerFactory.getLogger(SqlEditorService.class);

    private static final int MAX_ROWS = 200;

    /** DDL commits implicitly in Oracle (it would ruin the demo) and this runs as SYSTEM: blocked. */
    private static final Pattern BLOCKED = Pattern.compile(
            "(?is)^\\s*(drop|truncate|alter|create|grant|revoke|rename|purge|flashback|shutdown|startup|audit|noaudit|comment)\\b");
    private static final Pattern READ_ONLY = Pattern.compile("(?is)^\\s*(select|with)\\b");
    private static final Pattern COMMIT = Pattern.compile("(?is)^\\s*commit(\\s+work)?\\s*$");
    private static final Pattern ROLLBACK = Pattern.compile("(?is)^\\s*rollback(\\s+work)?\\s*$");

    private static final Pattern NEXTVAL = Pattern.compile("(?is)\\b([\\w$]+)\\.nextval\\b");

    private final DataSource dataSource;
    private final OracleEventBuffer events;

    public SqlEditorService(DataSource dataSource, OracleEventBuffer events) {
        this.dataSource = dataSource;
        this.events = events;
    }

    // ------------------------------------------------------------------ DTOs

    public record Request(String sql, String mode) {}

    public record StmtResult(String sql, String kind, boolean ok, String message, long ms,
                             List<String> columns, List<List<String>> rows, boolean truncated) {}

    public record Status(boolean open, String xid, String txStatus, String startTime,
                         long undoBlocks, long undoRecords,
                         long redoBytes, long blockChanges,
                         long redoSynchWrites, long commits, String error) {}

    public record Response(List<StmtResult> results, Status status) {}

    // ------------------------------------------------------------------ API

    public Response execute(HttpSession http, Request req) {
        String script = req.sql() == null ? "" : req.sql();
        boolean other = "other".equalsIgnoreCase(req.mode());
        List<StmtResult> results = new ArrayList<>();

        if (other) {
            try (Connection c = dataSource.getConnection()) {
                for (String sql : split(script)) {
                    StmtResult r = runOther(c, sql);
                    results.add(r);
                    if (!r.ok()) break;
                    publishOther(http, sql, r);
                }
            } catch (SQLException e) {
                results.add(error("", e));
            }
            return new Response(results, status(http));
        }

        SqlEditorSession s = session(http);
        for (String sql : split(script)) {
            StmtResult r = runEditor(s, sql);
            results.add(r);
            if (!r.ok()) break;      // like SQL*Plus with WHENEVER SQLERROR EXIT
        }
        return new Response(results, status(http));
    }

    public Response commit(HttpSession http) {
        return new Response(List.of(runEditor(session(http), "COMMIT")), status(http));
    }

    public Response rollback(HttpSession http) {
        return new Response(List.of(runEditor(session(http), "ROLLBACK")), status(http));
    }

    /** Live state of the editor transaction, read from V$TRANSACTION and V$MYSTAT. */
    public Status status(HttpSession http) {
        SqlEditorSession s = session(http);
        try {
            Connection c = s.connection();
            Map<String, Long> now = stats(c);

            String xid = null, txStatus = null, start = null;
            long ublk = 0, urec = 0;
            boolean open = false;
            try (Statement st = c.createStatement();
                 ResultSet rs = st.executeQuery("""
                         SELECT t.xidusn || '.' || t.xidslot || '.' || t.xidsqn AS xid, t.status,
                                t.start_time, t.used_ublk, t.used_urec
                         FROM v$transaction t JOIN v$session se ON se.taddr = t.addr
                         WHERE se.sid = (SELECT sid FROM v$mystat WHERE ROWNUM = 1)
                         """)) {
                if (rs.next()) {
                    open = true;
                    xid = rs.getString(1); txStatus = rs.getString(2); start = rs.getString(3);
                    ublk = rs.getLong(4); urec = rs.getLong(5);
                }
            }

            long redo = delta(now, s.txBase(), "redo size");
            long blocks = delta(now, s.txBase(), "db block changes");
            long synch = delta(now, s.sessionBase(), "redo synch writes");
            long commits = delta(now, s.sessionBase(), "user commits");
            return new Status(open, xid, txStatus, start, ublk, urec,
                    open ? redo : 0, open ? blocks : 0, synch, commits, null);
        } catch (SQLException e) {
            log.warn("SQL editor status unavailable: {}", e.getMessage());
            return new Status(false, null, null, null, 0, 0, 0, 0, 0, 0, e.getMessage());
        }
    }

    // ------------------------------------------------------------------ internals

    private synchronized SqlEditorSession session(HttpSession http) {
        Object o = http.getAttribute(SqlEditorSession.ATTR);
        if (o instanceof SqlEditorSession s) {
            try { if (!s.connection().isClosed()) return s; } catch (SQLException ignored) { }
        }
        try {
            Connection c = dataSource.getConnection();
            c.setAutoCommit(false);
            SqlEditorSession s = new SqlEditorSession(c);
            try {
                Map<String, Long> base = stats(c);
                s.txBase(base);
                s.sessionBase(base);
            } catch (SQLException e) {
                log.warn("SQL editor: baseline stats unavailable: {}", e.getMessage());
            }
            http.setAttribute(SqlEditorSession.ATTR, s);
            return s;
        } catch (SQLException e) {
            throw new IllegalStateException("Impossible d'ouvrir la session Oracle de l'éditeur : " + e.getMessage(), e);
        }
    }

    /**
     * Runs one statement on the editor session AND tells the Oracle page what really happened:
     * V$MYSTAT / V$TRANSACTION are read just before and just after, so the animation shows
     * measured redo bytes, block changes, undo usage and forced log writes (nothing is invented).
     */
    private StmtResult runEditor(SqlEditorSession s, String sql) {
        Map<String, Long> baseBefore = s.txBase();
        Snap before = snap(s.connection());
        StmtResult r = runEditorInner(s, sql);
        if (r.ok()) {
            try { publishEditor(s, sql, r, baseBefore, before); }
            catch (Exception e) { log.warn("SQL editor: event not published ({})", e.getMessage()); }
        }
        return r;
    }

    private StmtResult runEditorInner(SqlEditorSession s, String sql) {
        long t0 = System.nanoTime();
        Connection c = s.connection();
        try {
            if (BLOCKED.matcher(sql).find()) return blocked(sql);

            if (COMMIT.matcher(sql).matches()) {
                c.commit();
                s.txBase(stats(c));
                return ok(sql, "COMMIT", "Validation effectuée (COMMIT). Le LGWR a écrit le redo sur disque.", t0);
            }
            if (ROLLBACK.matcher(sql).matches()) {
                c.rollback();
                s.txBase(stats(c));
                return ok(sql, "ROLLBACK", "Annulation effectuée (ROLLBACK). Les modifications sont défaites avec l'undo.", t0);
            }
            return run(c, sql, t0);
        } catch (SQLException e) {
            return error(sql, e);
        }
    }

    private StmtResult runOther(Connection c, String sql) {
        long t0 = System.nanoTime();
        try {
            if (!READ_ONLY.matcher(sql).find()) {
                return new StmtResult(sql, "ERROR", false,
                        "Cette vue « autre session » est en lecture seule : seules les requêtes SELECT sont acceptées.",
                        0, List.of(), List.of(), false);
            }
            return run(c, sql, t0);
        } catch (SQLException e) {
            return error(sql, e);
        }
    }

    private StmtResult run(Connection c, String sql, long t0) throws SQLException {
        try (Statement st = c.createStatement()) {
            st.setMaxRows(MAX_ROWS + 1);
            boolean hasRs = st.execute(sql);
            if (!hasRs) {
                int n = st.getUpdateCount();
                String kind = firstWord(sql).toUpperCase();
                boolean dml = kind.equals("INSERT") || kind.equals("UPDATE") || kind.equals("DELETE") || kind.equals("MERGE");
                String msg = n + (n > 1 ? " lignes affectées" : " ligne affectée")
                        + (dml ? " — non validée : visible uniquement dans cette session jusqu'au COMMIT." : ".");
                return ok(sql, kind, msg, t0);
            }
            try (ResultSet rs = st.getResultSet()) {
                ResultSetMetaData md = rs.getMetaData();
                int cols = md.getColumnCount();
                List<String> names = new ArrayList<>();
                for (int i = 1; i <= cols; i++) names.add(md.getColumnLabel(i));
                List<List<String>> rows = new ArrayList<>();
                boolean truncated = false;
                while (rs.next()) {
                    if (rows.size() == MAX_ROWS) { truncated = true; break; }
                    List<String> row = new ArrayList<>(cols);
                    for (int i = 1; i <= cols; i++) row.add(rs.getString(i));
                    rows.add(row);
                }
                long ms = (System.nanoTime() - t0) / 1_000_000;
                String msg = rows.size() + (rows.size() > 1 ? " lignes" : " ligne")
                        + (truncated ? " (affichage limité à " + MAX_ROWS + ")" : "");
                return new StmtResult(sql, "SELECT", true, msg, ms, names, rows, truncated);
            }
        }
    }

    private Map<String, Long> stats(Connection c) throws SQLException {
        Map<String, Long> m = new HashMap<>();
        try (Statement st = c.createStatement();
             ResultSet rs = st.executeQuery("""
                     SELECT n.name, m.value
                     FROM v$mystat m JOIN v$statname n ON n.statistic# = m.statistic#
                     WHERE n.name IN ('redo size', 'db block changes', 'redo synch writes', 'user commits')
                     """)) {
            while (rs.next()) m.put(rs.getString(1), rs.getLong(2));
        }
        return m;
    }

    private static long delta(Map<String, Long> now, Map<String, Long> base, String key) {
        return Math.max(0, now.getOrDefault(key, 0L) - base.getOrDefault(key, 0L));
    }

    // ------------------------------------------------------------------ events for the Oracle page

    private record TxRow(boolean open, String xid, long ublk, long urec) {}
    private record Snap(Map<String, Long> stats, TxRow tx) {}

    private Snap snap(Connection c) {
        try { return new Snap(stats(c), txRow(c)); }
        catch (SQLException e) { return new Snap(Map.of(), new TxRow(false, null, 0, 0)); }
    }

    private TxRow txRow(Connection c) throws SQLException {
        try (Statement st = c.createStatement();
             ResultSet rs = st.executeQuery("""
                     SELECT t.xidusn || '.' || t.xidslot || '.' || t.xidsqn, t.used_ublk, t.used_urec
                     FROM v$transaction t JOIN v$session se ON se.taddr = t.addr
                     WHERE se.sid = (SELECT sid FROM v$mystat WHERE ROWNUM = 1)
                     """)) {
            return rs.next() ? new TxRow(true, rs.getString(1), rs.getLong(2), rs.getLong(3))
                             : new TxRow(false, null, 0, 0);
        }
    }

    private void publishEditor(SqlEditorSession s, String sql, StmtResult r,
                               Map<String, Long> baseBefore, Snap before) throws SQLException {
        String type = r.kind();
        boolean terminal = type.equals("COMMIT") || type.equals("ROLLBACK");
        boolean dml = type.equals("INSERT") || type.equals("UPDATE") || type.equals("DELETE") || type.equals("MERGE");

        Connection c = s.connection();
        Snap after = new Snap(stats(c), txRow(c));

        int pending = s.pending();
        if (dml) { s.pendingUp(); pending = s.pending(); }
        if (terminal) s.pendingReset();

        // For COMMIT / ROLLBACK the transaction is gone afterwards: report what it held just before.
        Map<String, Long> ref = terminal ? before.stats() : after.stats();
        TxRow row = terminal ? before.tx() : after.tx();

        OracleEvent.Tx tx = new OracleEvent.Tx(
                after.tx().open(), row.xid(), row.ublk(), row.urec(),
                delta(ref, baseBefore, "redo size"), delta(ref, baseBefore, "db block changes"),
                delta(after.stats(), before.stats(), "redo size"), delta(after.stats(), before.stats(), "db block changes"),
                delta(after.stats(), before.stats(), "redo synch writes"), pending);

        events.publishSql("sql", type, tableOf(sql, type), statementsOf(sql, type), dml, r.ms(), tx);
    }

    /** Another session reading committed data only: still a real request to the instance. */
    private void publishOther(HttpSession http, String sql, StmtResult r) {
        try {
            Status st = status(http);
            OracleEvent.Tx tx = new OracleEvent.Tx(st.open(), st.xid(), st.undoBlocks(), st.undoRecords(),
                    st.redoBytes(), st.blockChanges(), 0, 0, 0, st.open() ? 1 : 0);
            events.publishSql("sql-other", "SELECT", tableOf(sql, "SELECT"), statementsOf(sql, "SELECT"), false, r.ms(), tx);
        } catch (Exception e) {
            log.warn("SQL editor: event not published ({})", e.getMessage());
        }
    }

    private static List<OracleEvent.Stmt> statementsOf(String sql, String type) {
        if (type.equals("COMMIT") || type.equals("ROLLBACK")) return List.of();
        List<OracleEvent.Stmt> out = new ArrayList<>();
        Matcher m = NEXTVAL.matcher(sql);
        if (m.find()) out.add(new OracleEvent.Stmt("SEQ", m.group(1).toUpperCase(), m.group(0), 1));
        RequestTrace.begin();
        RequestTrace.record(sql);
        out.addAll(RequestTrace.end());
        return out;
    }

    private static String tableOf(String sql, String type) {
        for (OracleEvent.Stmt st : statementsOf(sql, type)) if (!st.type().equals("SEQ")) return st.table();
        return null;
    }

    // ------------------------------------------------------------------ helpers

    /** Splits a script on ';' outside of 'quotes', dropping -- and block comments. PL/SQL blocks are not supported. */
    static List<String> split(String script) {
        List<String> out = new ArrayList<>();
        StringBuilder cur = new StringBuilder();
        boolean quote = false;
        int n = script.length();
        for (int i = 0; i < n; i++) {
            char ch = script.charAt(i);
            if (!quote && ch == '-' && i + 1 < n && script.charAt(i + 1) == '-') {
                while (i < n && script.charAt(i) != '\n') i++;
                cur.append('\n');
                continue;
            }
            if (!quote && ch == '/' && i + 1 < n && script.charAt(i + 1) == '*') {
                int e = script.indexOf("*/", i + 2);
                i = e < 0 ? n : e + 1;
                cur.append(' ');
                continue;
            }
            if (ch == '\'') quote = !quote;
            if (ch == ';' && !quote) { add(out, cur); cur.setLength(0); continue; }
            cur.append(ch);
        }
        add(out, cur);
        return out;
    }

    private static void add(List<String> out, StringBuilder sb) {
        String s = sb.toString().strip();
        if (!s.isEmpty()) out.add(s);
    }

    private static String firstWord(String sql) {
        String s = sql.stripLeading();
        int i = 0;
        while (i < s.length() && Character.isLetter(s.charAt(i))) i++;
        return s.substring(0, i);
    }

    private static StmtResult ok(String sql, String kind, String msg, long t0) {
        return new StmtResult(sql, kind, true, msg, (System.nanoTime() - t0) / 1_000_000, List.of(), List.of(), false);
    }

    private static StmtResult error(String sql, SQLException e) {
        String m = e.getMessage() == null ? "Erreur SQL" : e.getMessage().strip();
        return new StmtResult(sql, "ERROR", false, m, 0, List.of(), List.of(), false);
    }

    private static StmtResult blocked(String sql) {
        return new StmtResult(sql, "ERROR", false,
                "Instruction DDL / administration bloquée : en Oracle un DDL fait un COMMIT implicite, "
                        + "ce qui détruirait la démonstration (et l'éditeur tourne avec l'utilisateur SYSTEM).",
                0, List.of(), List.of(), false);
    }
}
