package amichno.db_perf_app;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Collections;

public class MySQLQueryExecutor implements QueryExecutor {
    private final MySQLServerHandle serverHandle;
    private final MySQLIdsCache idsCache;
    private CrashPerformer crashPerformer;

    public MySQLQueryExecutor(MySQLServerHandle serverHandle, MySQLIdsCache idsCache) {
        this.serverHandle = serverHandle;
        this.idsCache = idsCache;
    }

    @Override
    public void setCrashPerformer(CrashPerformer crashPerformer) {
        this.crashPerformer = crashPerformer;
    }

    @Override
    public QueryResult executeInsertQuery(int count) throws SQLException {
        QueryResult queryResult = new QueryResult();
        Connection connection = serverHandle.getConnection();
        connection.setAutoCommit(false);
        try {
            try (PreparedStatement stmt = connection.prepareStatement(
                    "INSERT INTO users (name, email, age, phone) VALUES (?, ?, ?, ?)")) {
                for (int i = 0; i < count; ++i) {
                    stmt.setString(1, UserDataGenerator.getName());
                    stmt.setString(2, UserDataGenerator.getEmail());
                    stmt.setInt(3, UserDataGenerator.getAge());
                    stmt.setString(4, UserDataGenerator.getPhone());
                    stmt.addBatch();
                }

                crashPerformer.performCrash();

                long queryStart = System.currentTimeMillis();
                try {
                    int[] updateCounts = stmt.executeBatch();
                    connection.commit();
                    queryResult.executionTime = System.currentTimeMillis() - queryStart;
                    idsCache.invalidateCache();
                    for (int update : updateCounts) {
                        queryResult.objectsAffected += update;
                    }
                } catch (Throwable exc) {
                    queryResult.error = exc;
                    if (queryStart > 0) {
                        queryResult.executionTime = System.currentTimeMillis() - queryStart;
                    }
                }
            }
        } catch (Throwable exc) {
            try {
                connection.rollback();
            } catch (Throwable ignore) {
            }
            throw exc;
        } finally {
            try {
                connection.setAutoCommit(true);
            } catch (Throwable ignore) {
            }
        }
        return queryResult;
    }

    @Override
    public QueryResult executeUpdateQuery(int count) throws SQLException {
        QueryResult queryResult = new QueryResult();
        Connection connection = serverHandle.getConnection();
        long[] ids = idsCache.getRandomIds(count);
        connection.setAutoCommit(false);
        try {
            try (PreparedStatement stmt = connection.prepareStatement(
                    "UPDATE users SET name = ?, email = ?, age = ?, phone = ? WHERE id = ?")) {
                for (long id : ids) {
                    stmt.setString(1, UserDataGenerator.getName());
                    stmt.setString(2, UserDataGenerator.getEmail());
                    stmt.setInt(3, UserDataGenerator.getAge());
                    stmt.setString(4, UserDataGenerator.getPhone());
                    stmt.setLong(5, id);
                    stmt.addBatch();
                }

                crashPerformer.performCrash();

                long queryStart = System.currentTimeMillis();
                try {
                    int[] updateCounts = stmt.executeBatch();
                    connection.commit();
                    queryResult.executionTime = System.currentTimeMillis() - queryStart;
                    for (int update : updateCounts) {
                        queryResult.objectsAffected += update;
                    }
                } catch (Throwable exc) {
                    queryResult.error = exc;
                    if (queryStart > 0) {
                        queryResult.executionTime = System.currentTimeMillis() - queryStart;
                    }
                }
            }
        } catch (Throwable exc) {
            try {
                connection.rollback();
            } catch (Throwable ignore) {
            }
            throw exc;
        } finally {
            try {
                connection.setAutoCommit(true);
            } catch (Throwable ignore) {
            }
        }
        return queryResult;
    }

    @Override
    public QueryResult executeDeleteQuery(int count) throws SQLException {
        QueryResult queryResult = new QueryResult();
        Connection connection = serverHandle.getConnection();
        long[] ids = idsCache.getRandomIds(count);
        String idsList = ids.length > 0 ? "(" + String.join(",", Collections.nCopies(ids.length, "?")) + ")" : "(NULL)";
        try (PreparedStatement stmt = connection.prepareStatement("DELETE FROM users WHERE id IN " + idsList)) {
            int index = 1;
            for (long id : ids) {
                stmt.setLong(index, id);
                index++;
            }

            crashPerformer.performCrash();

            long queryStart = System.currentTimeMillis();
            try {
                queryResult.objectsAffected = stmt.executeUpdate();
                queryResult.executionTime = System.currentTimeMillis() - queryStart;
                idsCache.invalidateCache();
            } catch (Throwable exc) {
                queryResult.error = exc;
                if (queryStart > 0) {
                    queryResult.executionTime = System.currentTimeMillis() - queryStart;
                }
            }
        }
        return queryResult;
    }

    @Override
    public Long countRecords() {
        try {
            Connection connection = serverHandle.getConnection();
            try (Statement stmt = connection.createStatement()) {
                try (ResultSet resultSet = stmt.executeQuery("SELECT COUNT(1) FROM users")) {
                    if (!resultSet.next()) {
                        throw new SQLException();
                    }
                    return resultSet.getLong(1);
                }
            }
        } catch (Throwable exc) {
            return null;
        }
    }

    @Override
    public QueryResult executeSelectQuery(int count) throws Exception {
        QueryResult queryResult = new QueryResult();
        Connection connection = serverHandle.getConnection();
        long[] ids = idsCache.getRandomIds(count);
        String idsList = ids.length > 0 ? "(" + String.join(",", Collections.nCopies(ids.length, "?")) + ")" : "(NULL)";
        try (PreparedStatement stmt = connection.prepareStatement(
                "SELECT id, name, email, age, phone FROM users WHERE id IN " + idsList)) {
            int index = 1;
            for (long id : ids) {
                stmt.setLong(index, id);
                index++;
            }
            crashPerformer.performCrash();
            long queryStart = System.currentTimeMillis();
            try (ResultSet resultSet = stmt.executeQuery()) {
                queryResult.executionTime = System.currentTimeMillis() - queryStart;
                while (resultSet.next()) {
                    queryResult.objectsAffected += 1;
                }
            } catch (Throwable exc) {
                queryResult.error = exc;
                if (queryStart > 0) {
                    queryResult.executionTime = System.currentTimeMillis() - queryStart;
                }
            }
        }
        return queryResult;
    }
}
