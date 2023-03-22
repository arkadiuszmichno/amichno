package amichno.db_perf_app;

public interface QueryExecutor {
    void setCrashPerformer(CrashPerformer crashPerformer);
    QueryResult executeSelectQuery(int count) throws Exception;
    QueryResult executeInsertQuery(int count) throws Exception;
    QueryResult executeUpdateQuery(int count) throws Exception;
    QueryResult executeDeleteQuery(int count) throws Exception;
    Long countRecords();
}
