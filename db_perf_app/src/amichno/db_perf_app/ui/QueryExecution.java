package amichno.db_perf_app.ui;

import amichno.db_perf_app.QueryResult;

public interface QueryExecution {
    QueryResult execute(int count) throws Exception;
}
