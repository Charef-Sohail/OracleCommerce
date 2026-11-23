package com.enset.oraclecommerce.service;

import com.enset.oraclecommerce.dto.response.OracleSnapshot;
import com.enset.oraclecommerce.dto.response.OracleSnapshot.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.function.Supplier;

/**
 * Reads the live state of the Oracle instance (SGA, PGA, background processes,
 * redo logs, tablespaces, SQL of this application) from the V$ / DBA_ views.
 *
 * Needs a user allowed to read V$ views (SYSTEM is fine).
 * Each block is isolated: if one view is not readable, the others still work.
 */
@Service
public class OracleInternalsService {

    private static final Logger log = LoggerFactory.getLogger(OracleInternalsService.class);

    private final JdbcTemplate jdbc;

    public OracleInternalsService(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public OracleSnapshot snapshot() {
        return new OracleSnapshot(
                System.currentTimeMillis(),
                safe("instance", this::instance, null),
                safe("sga", this::sga, Map.of()),
                safe("sga components", this::sgaComponents, List.of()),
                safe("pga", this::pga, Map.of()),
                safe("sysstat", this::sysstat, Map.of()),
                safe("sessions", this::sessions, new Sessions(0, 0, 0)),
                safe("background processes", this::backgroundProcesses, List.of()),
                safe("redo logs", this::redoLogs, List.of()),
                safe("tablespaces", this::tablespaces, List.of()),
                safe("parameters", this::parameters, Map.of()),
                safe("top sql", this::topSql, List.of())
        );
    }

    // ---------------------------------------------------------------- blocks

    private Instance instance() {
        return jdbc.queryForObject("""
                SELECT i.instance_name, d.name AS db_name, i.host_name, i.version, i.status,
                       d.log_mode, d.open_mode,
                       ROUND((SYSDATE - i.startup_time) * 86400) AS uptime_s
                FROM v$instance i CROSS JOIN v$database d
                """,
                (rs, n) -> new Instance(
                        rs.getString("instance_name"), rs.getString("db_name"),
                        rs.getString("host_name"), rs.getString("version"),
                        rs.getString("status"), rs.getString("log_mode"),
                        rs.getString("open_mode"), rs.getLong("uptime_s")));
    }

    /** Fixed Size / Variable Size / Database Buffers / Redo Buffers (bytes). */
    private Map<String, Long> sga() {
        Map<String, Long> m = new LinkedHashMap<>();
        jdbc.query("SELECT name, value FROM v$sga", rs -> { m.put(rs.getString(1), rs.getLong(2)); });
        return m;
    }

    private List<Component> sgaComponents() {
        return jdbc.query("""
                SELECT component, current_size FROM v$sga_dynamic_components
                WHERE current_size > 0 ORDER BY current_size DESC
                """, (rs, n) -> new Component(rs.getString(1), rs.getLong(2)));
    }

    private Map<String, Double> pga() {
        Map<String, Double> m = new LinkedHashMap<>();
        jdbc.query("""
                SELECT name, value FROM v$pgastat
                WHERE name IN ('aggregate PGA target parameter', 'total PGA allocated',
                               'total PGA inuse', 'maximum PGA allocated',
                               'total freeable PGA memory', 'cache hit percentage')
                """, rs -> { m.put(rs.getString(1), rs.getDouble(2)); });
        return m;
    }

    /** Cumulative counters: the browser computes per-second rates from two snapshots. */
    private Map<String, Long> sysstat() {
        Map<String, Long> m = new LinkedHashMap<>();
        jdbc.query("""
                SELECT name, value FROM v$sysstat
                WHERE name IN ('user commits', 'user calls', 'redo size', 'redo writes',
                               'physical reads', 'physical writes', 'session logical reads',
                               'parse count (total)', 'parse count (hard)', 'execute count',
                               'db block changes')
                """, rs -> { m.put(rs.getString(1), rs.getLong(2)); });
        return m;
    }

    private Sessions sessions() {
        long processes = Optional.ofNullable(
                jdbc.queryForObject("SELECT COUNT(*) FROM v$process", Long.class)).orElse(0L);
        return jdbc.queryForObject("""
                SELECT COUNT(*) AS total,
                       NVL(SUM(CASE WHEN status = 'ACTIVE' THEN 1 ELSE 0 END), 0) AS active
                FROM v$session WHERE type = 'USER'
                """, (rs, n) -> new Sessions(rs.getLong("total"), rs.getLong("active"), processes));
    }

    private List<BgProcess> backgroundProcesses() {
        return jdbc.query("""
                SELECT name, description FROM v$bgprocess
                WHERE paddr <> HEXTORAW('00') ORDER BY name
                """, (rs, n) -> new BgProcess(rs.getString(1), rs.getString(2)));
    }

    private List<RedoGroup> redoLogs() {
        return jdbc.query("""
                SELECT group# AS grp, status, bytes, members, sequence# AS seq, archived
                FROM v$log ORDER BY group#
                """, (rs, n) -> new RedoGroup(rs.getInt("grp"), rs.getString("status"),
                rs.getLong("bytes"), rs.getInt("members"), rs.getLong("seq"), rs.getString("archived")));
    }

    private List<Tablespace> tablespaces() {
        return jdbc.query("""
                SELECT df.tablespace_name, df.bytes, df.bytes - NVL(fs.bytes, 0) AS used_bytes
                FROM (SELECT tablespace_name, SUM(bytes) AS bytes FROM dba_data_files GROUP BY tablespace_name) df
                LEFT JOIN (SELECT tablespace_name, SUM(bytes) AS bytes FROM dba_free_space GROUP BY tablespace_name) fs
                       ON fs.tablespace_name = df.tablespace_name
                ORDER BY df.bytes DESC
                """, (rs, n) -> new Tablespace(rs.getString(1), rs.getLong(2), rs.getLong(3)));
    }

    private Map<String, String> parameters() {
        Map<String, String> m = new LinkedHashMap<>();
        jdbc.query("""
                SELECT name, value FROM v$parameter
                WHERE name IN ('sga_target', 'sga_max_size', 'pga_aggregate_target',
                               'memory_target', 'db_block_size', 'processes')
                """, rs -> { m.put(rs.getString(1), rs.getString(2)); });
        return m;
    }

    /** The latest statements this application ran against its own tables. */
    private List<SqlStat> topSql() {
        return jdbc.query("""
                SELECT sql_id, SUBSTR(sql_text, 1, 200) AS txt, executions, buffer_gets, disk_reads,
                       ROUND(elapsed_time / 1000) AS ms
                FROM v$sql
                WHERE parsing_schema_name = SYS_CONTEXT('USERENV', 'CURRENT_SCHEMA')
                  AND sql_text NOT LIKE '%v$%' AND sql_text NOT LIKE '%V$%'
                  AND LOWER(sql_text) NOT LIKE '%dba_%'
                  AND REGEXP_LIKE(sql_text, '(CLIENT|CATEGORIE|PRODUIT|COMMANDE|PAIEMENT|MOUVEMENT_STOCK|UTILISATEUR|ROLE)', 'i')
                ORDER BY last_active_time DESC
                FETCH FIRST 8 ROWS ONLY
                """, (rs, n) -> new SqlStat(rs.getString("sql_id"), rs.getString("txt"),
                rs.getLong("executions"), rs.getLong("buffer_gets"), rs.getLong("disk_reads"), rs.getLong("ms")));
    }

    // ---------------------------------------------------------------- util

    private <T> T safe(String what, Supplier<T> block, T fallback) {
        try {
            return block.get();
        } catch (Exception e) {
            log.warn("Oracle internals: '{}' unavailable ({})", what, e.getMessage());
            return fallback;
        }
    }
}
