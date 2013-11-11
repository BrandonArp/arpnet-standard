package com.arpnetworking.utils;

import com.jolbox.bonecp.ConnectionHandle;
import com.jolbox.bonecp.StatementHandle;
import com.jolbox.bonecp.hooks.AbstractConnectionHook;
import com.jolbox.bonecp.hooks.AcquireFailConfig;
import com.jolbox.bonecp.hooks.ConnectionHook;
import com.jolbox.bonecp.hooks.ConnectionState;
import play.mvc.Http;

import java.sql.SQLException;
import java.sql.Statement;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * A connection hook that keeps track of metrics
 *
 * @author barp
 */
public class MetricLoggingHook extends AbstractConnectionHook {
    private final ConnectionHook fWrappedHook;
    private final String fDataSourceName;
    private AtomicInteger fOpenConnections = new AtomicInteger(0);
    private AtomicInteger fActiveConnections = new AtomicInteger(0);

    public MetricLoggingHook(ConnectionHook wrappedHook, String dataSourceName) {
        fWrappedHook = wrappedHook;
        fDataSourceName = dataSourceName;
    }

    @Override
    public void onAcquire(final ConnectionHandle connection) {
        fOpenConnections.incrementAndGet();
        fWrappedHook.onAcquire(connection);
    }

    @Override
    public void onCheckIn(final ConnectionHandle connection) {
        fActiveConnections.decrementAndGet();
        fWrappedHook.onCheckIn(connection);
    }

    @Override
    public void onCheckOut(final ConnectionHandle connection) {
        int active = fActiveConnections.incrementAndGet();
        Counter counter = getActiveCounter();
        if (counter != null) {
            counter.recordGauge(fDataSourceName + ".activeConnections", active);
        }
        fWrappedHook.onCheckOut(connection);
    }

    @Override
    public void onDestroy(final ConnectionHandle connection) {
        fOpenConnections.decrementAndGet();
        fWrappedHook.onDestroy(connection);
    }

    @Override
    public boolean onAcquireFail(final Throwable t, final AcquireFailConfig acquireConfig) {
        return fWrappedHook.onAcquireFail(t, acquireConfig);
    }

    @Override
    public void onQueryExecuteTimeLimitExceeded(final ConnectionHandle handle, final Statement statement, final String sql, final Map<Object, Object> logParams, final long timeElapsedInNs) {
        fWrappedHook.onQueryExecuteTimeLimitExceeded(handle, statement, sql, logParams, timeElapsedInNs);
    }

    @SuppressWarnings("deprecation")
    @Override
    public void onQueryExecuteTimeLimitExceeded(final ConnectionHandle handle, final Statement statement, final String sql, final Map<Object, Object> logParams) {
        fWrappedHook.onQueryExecuteTimeLimitExceeded(handle, statement, sql, logParams);
    }

    @SuppressWarnings("deprecation")
    @Override
    public void onQueryExecuteTimeLimitExceeded(final String sql, final Map<Object, Object> logParams) {
        fWrappedHook.onQueryExecuteTimeLimitExceeded(sql, logParams);
    }

    @Override
    public boolean onConnectionException(final ConnectionHandle connection, final String state, final Throwable t) {
        return fWrappedHook.onConnectionException(connection, state, t);
    }

    @Override
    public void onBeforeStatementExecute(final ConnectionHandle conn, final StatementHandle statement, final String sql, final Map<Object, Object> params) {
        fWrappedHook.onBeforeStatementExecute(conn, statement, sql, params);
        Counter counter = getActiveCounter();
        if (counter != null) {
            counter.startTimer(fDataSourceName + ".queryTime");
        }
    }

    @Override
    public void onAfterStatementExecute(final ConnectionHandle conn, final StatementHandle statement, final String sql, final Map<Object, Object> params) {
        Counter counter = getActiveCounter();
        if (counter != null) {
            counter.stopTimer(fDataSourceName + ".queryTime");
            counter.increment(fDataSourceName + ".queriesExecuted");
        }
        fWrappedHook.onAfterStatementExecute(conn, statement, sql, params);
    }

    @Override
    public ConnectionState onMarkPossiblyBroken(final ConnectionHandle connection, final String state, final SQLException e) {
        return fWrappedHook.onMarkPossiblyBroken(connection, state, e);
    }

    private Counter getActiveCounter() {
        Http.Context context = Http.Context.current.get();
        if (context == null) {
            return null;
        }

        Counter counter = (Counter)context.args.get("query-log");
        return counter;
    }
}
