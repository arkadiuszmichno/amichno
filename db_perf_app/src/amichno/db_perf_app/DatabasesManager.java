package amichno.db_perf_app;

public final class DatabasesManager {
    private static DatabasesManager instance;

    private final MySQLServerHandle[] mySQLServers = {
            new MySQLServerHandle("./mysql/single", 13306),
            new MySQLServerHandle("./mysql/replication-master", 13316),
            new MySQLServerHandle("./mysql/replication-slave-1", 13326),
            new MySQLServerHandle("./mysql/replication-slave-2", 13336),
    };

    private final VoldemortServerHandle[] voldemortServers = {
            new VoldemortServerHandle("./voldemort/node_0", 6666),
            new VoldemortServerHandle("./voldemort/node_1", 6670),
            new VoldemortServerHandle("./voldemort/node_2", 6674),
            new VoldemortServerHandle("./voldemort/node_3", 6678),
    };

    private final MySQLIdsCache mySQLSingleIdsCache = new MySQLIdsCache(mySQLServers[0]);
    private final MySQLIdsCache mySQLReplicationIdsCache = new MySQLIdsCache(mySQLServers[1]);
    private final VoldemortKeysManager voldemortKeysManager = new VoldemortKeysManager(voldemortServers[0]);

    private DatabasesManager() {
    }

    public static DatabasesManager getInstance() {
        if (instance == null) {
            instance = new DatabasesManager();
        }
        return instance;
    }

    public MySQLServerHandle[] getMySQLServers() {
        return mySQLServers;
    }

    public MySQLServerHandle getMySQLSingle() { return mySQLServers[0]; }
    public MySQLServerHandle getMySQLReplicationMaster() { return mySQLServers[1]; }
    public MySQLServerHandle getMySQLReplicationSlave1() { return mySQLServers[2]; }
    public MySQLServerHandle getMySQLReplicationSlave2() { return mySQLServers[3]; }
    public MySQLIdsCache getMySQLSingleIdsCache() { return mySQLSingleIdsCache; }
    public MySQLIdsCache getMySQLReplicationIdsCache() { return mySQLReplicationIdsCache; }

    public VoldemortServerHandle[] getVoldemortServers() { return voldemortServers; }
    public VoldemortServerHandle getVoldemortFirstServer() { return voldemortServers[0]; }
    public VoldemortKeysManager getVoldemortKeysManager() { return voldemortKeysManager; }

}
