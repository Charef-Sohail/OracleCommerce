package com.enset.oraclecommerce.oracle;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Collects, per HTTP request thread, the SQL statements Hibernate sends to Oracle.
 * begin() / end() are called by the interceptor; record() by the Hibernate inspector.
 * Queries of OracleInternalsService (JdbcTemplate) never pass here, so they stay invisible.
 */
public final class RequestTrace {

    private static final ThreadLocal<Map<String, Acc>> CURRENT = new ThreadLocal<>();

    private static final Pattern NEXTVAL = Pattern.compile("(?is)\\b([\\w$]+)\\.nextval\\b");
    private static final Pattern INSERT  = Pattern.compile("(?is)^\\s*insert\\s+into\\s+([\\w$.\"]+)");
    private static final Pattern UPDATE  = Pattern.compile("(?is)^\\s*update\\s+([\\w$.\"]+)");
    private static final Pattern DELETE  = Pattern.compile("(?is)^\\s*delete\\s+from\\s+([\\w$.\"]+)");
    private static final Pattern SELECT  = Pattern.compile("(?is)\\bfrom\\s+([\\w$.\"]+)");

    private static final int MAX_DISTINCT = 12;

    private RequestTrace() {}

    public static void begin() { CURRENT.set(new LinkedHashMap<>()); }

    public static void record(String sql) {
        Map<String, Acc> acc = CURRENT.get();
        if (acc == null || sql == null) return;

        String type, table;
        Matcher m;
        String head = sql.stripLeading().toLowerCase();
        if (head.startsWith("insert")) {
            type = "INSERT"; table = first(INSERT, sql);
        } else if (head.startsWith("update")) {
            type = "UPDATE"; table = first(UPDATE, sql);
        } else if (head.startsWith("delete")) {
            type = "DELETE"; table = first(DELETE, sql);
        } else if (head.startsWith("select") && (m = NEXTVAL.matcher(sql)).find()) {
            type = "SEQ"; table = m.group(1).toUpperCase();
        } else if (head.startsWith("select")) {
            type = "SELECT"; table = first(SELECT, sql);
        } else {
            return;   // DDL, commit, etc.: ignore
        }

        String key = type + ":" + table;
        Acc a = acc.get(key);
        if (a != null) { a.count++; return; }
        if (acc.size() < MAX_DISTINCT) {
            String flat = sql.replaceAll("\\s+", " ").trim();
            acc.put(key, new Acc(type, table, flat.length() > 300 ? flat.substring(0, 300) + "…" : flat));
        }
    }

    public static List<OracleEvent.Stmt> end() {
        Map<String, Acc> acc = CURRENT.get();
        CURRENT.remove();
        List<OracleEvent.Stmt> out = new ArrayList<>();
        if (acc != null) acc.values().forEach(a -> out.add(new OracleEvent.Stmt(a.type, a.table, a.sql, a.count)));
        return out;
    }

    private static String first(Pattern p, String sql) {
        Matcher m = p.matcher(sql);
        return m.find() ? m.group(1).replace("\"", "").toUpperCase() : "?";
    }

    private static final class Acc {
        final String type, table, sql; int count = 1;
        Acc(String type, String table, String sql) { this.type = type; this.table = table; this.sql = sql; }
    }
}
