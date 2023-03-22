package amichno.db_perf_app;

import voldemort.client.StoreClient;
import voldemort.versioning.Versioned;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class VoldemortQueryExecutor implements QueryExecutor {
    private final VoldemortServerHandle serverHandle;
    private final VoldemortKeysManager keysManager;
    private CrashPerformer crashPerformer;

    public VoldemortQueryExecutor(VoldemortServerHandle serverHandle, VoldemortKeysManager keysManager) {
        this.serverHandle = serverHandle;
        this.keysManager = keysManager;
    }

    @Override
    public void setCrashPerformer(CrashPerformer crashPerformer) {
        this.crashPerformer = crashPerformer;
    }

    public VoldemortServerHandle getServerHandle() {
        return serverHandle;
    }

    @Override
    public QueryResult executeInsertQuery(int count) {
        QueryResult queryResult = new QueryResult();
        long queryStart;
        StoreClient<String, Map<String, Object>> client = serverHandle.getStoreClient();

        List<Map<String, Object>> inputRecords = new ArrayList<>();

        for (int i = 0; i < count; ++i) {
            inputRecords.add(generateRecord());
        }

        crashPerformer.performCrash();

        keysManager.invalidateKeysCache();

        queryStart = System.currentTimeMillis();
        try {
            for (Map<String, Object> inputRecord : inputRecords) {
                client.put(keysManager.getNextKey(), inputRecord);
                queryResult.objectsAffected++;
            }
            queryResult.executionTime = System.currentTimeMillis() - queryStart;
        } catch (Throwable exc) {
            queryResult.error = exc;
            if (queryStart > 0) {
                queryResult.executionTime = System.currentTimeMillis() - queryStart;
            }
        }

        return queryResult;
    }

    @Override
    public QueryResult executeSelectQuery(int count) {
        List<String> keys = Arrays.asList(keysManager.getRandomKeys(count));
        StoreClient<String, Map<String, Object>> client = serverHandle.getStoreClient();

        QueryResult queryResult = new QueryResult();
        long queryStart;

        crashPerformer.performCrash();

        queryStart = System.currentTimeMillis();
        try {
            Map<String, Versioned<Map<String, Object>>> records = client.getAll(keys);
            queryResult.executionTime = System.currentTimeMillis() - queryStart;
            queryResult.objectsAffected = records.size();
        } catch (Throwable exc) {
            queryResult.error = exc;
            if (queryStart > 0) {
                queryResult.executionTime = System.currentTimeMillis() - queryStart;
            }
        }
        return queryResult;
    }

    @Override
    public QueryResult executeUpdateQuery(int count) {
        String[] keys = keysManager.getRandomKeys(count);
        StoreClient<String, Map<String, Object>> client = serverHandle.getStoreClient();

        QueryResult queryResult = new QueryResult();
        long queryStart;

        crashPerformer.performCrash();

        queryStart = System.currentTimeMillis();
        try {
            for (String key : keys) {
                client.put(key, generateRecord());
                queryResult.objectsAffected++;
            }
            queryResult.executionTime = System.currentTimeMillis() - queryStart;
        } catch (Throwable exc) {
            queryResult.error = exc;
            if (queryStart > 0) {
                queryResult.executionTime = System.currentTimeMillis() - queryStart;
            }
        }

        return queryResult;
    }

    @Override
    public QueryResult executeDeleteQuery(int count) {
        String[] keys = keysManager.getRandomKeys(count);
        StoreClient<String, Map<String, Object>> client = serverHandle.getStoreClient();

        QueryResult queryResult = new QueryResult();
        long queryStart;

        crashPerformer.performCrash();

        keysManager.invalidateKeysCache();

        queryStart = System.currentTimeMillis();
        try {
            for (String key : keys) {
                client.delete(key);
                queryResult.objectsAffected++;
            }
            queryResult.executionTime = System.currentTimeMillis() - queryStart;
        } catch (Throwable exc) {
            queryResult.error = exc;
            if (queryStart > 0) {
                queryResult.executionTime = System.currentTimeMillis() - queryStart;
            }
        }
        return queryResult;
    }

    @Override
    public Long countRecords() {
        try {
            return (long) keysManager.getKeysCount();
        } catch (Throwable ignore) {
            return null;
        }
    }

    private Map<String, Object> generateRecord() {
        HashMap<String, Object> inputRecord = new HashMap<>();
        inputRecord.put("name", UserDataGenerator.getName());
        inputRecord.put("email", UserDataGenerator.getEmail());
        inputRecord.put("age", (short) UserDataGenerator.getAge());
        inputRecord.put("phone", UserDataGenerator.getPhone());
        return inputRecord;
    }
}
