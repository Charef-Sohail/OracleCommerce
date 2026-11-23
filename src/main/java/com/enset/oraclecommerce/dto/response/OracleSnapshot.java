package com.enset.oraclecommerce.dto.response;

import java.util.List;
import java.util.Map;

/**
 * One photo of the Oracle instance, read from the V$ views.
 * Serialized as JSON for the /oracle page (polled every few seconds).
 */
public record OracleSnapshot(
        long timestamp,
        Instance instance,
        Map<String, Long> sga,              // V$SGA : Fixed Size, Variable Size, Database Buffers, Redo Buffers
        List<Component> sgaComponents,      // V$SGA_DYNAMIC_COMPONENTS
        Map<String, Double> pga,            // V$PGASTAT
        Map<String, Long> stats,            // V$SYSSTAT (cumulative counters)
        Sessions sessions,
        List<BgProcess> backgroundProcesses,
        List<RedoGroup> redoLogs,
        List<Tablespace> tablespaces,
        Map<String, String> parameters,
        List<SqlStat> topSql
) {
    public record Instance(String instanceName, String dbName, String hostName, String version,
                           String status, String logMode, String openMode, long uptimeSeconds) {}

    public record Component(String name, long bytes) {}

    public record Sessions(long total, long active, long processes) {}

    public record BgProcess(String name, String description) {}

    public record RedoGroup(int group, String status, long bytes, int members, long sequence, String archived) {}

    public record Tablespace(String name, long bytes, long usedBytes) {}

    public record SqlStat(String sqlId, String text, long executions, long bufferGets, long diskReads, long elapsedMs) {}
}
