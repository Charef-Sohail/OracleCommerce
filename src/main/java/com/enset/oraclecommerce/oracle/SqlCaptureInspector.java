package com.enset.oraclecommerce.oracle;

import org.hibernate.resource.jdbc.spi.StatementInspector;

/**
 * Hibernate calls this for every SQL statement it is about to send to Oracle.
 * Registered in application.yml (hibernate.session_factory.statement_inspector).
 * Hibernate instantiates it itself, hence the static RequestTrace.
 */
public class SqlCaptureInspector implements StatementInspector {

    @Override
    public String inspect(String sql) {
        RequestTrace.record(sql);
        return sql;   // never modified
    }
}
