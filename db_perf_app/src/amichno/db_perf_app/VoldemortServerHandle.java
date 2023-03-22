package amichno.db_perf_app;

import voldemort.client.ClientConfig;
import voldemort.client.SocketStoreClientFactory;
import voldemort.client.StoreClient;
import voldemort.client.protocol.admin.AdminClient;
import voldemort.server.VoldemortConfig;
import voldemort.server.VoldemortServer;
import voldemort.store.StoreDefinition;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class VoldemortServerHandle implements ServerHandle {
    public static class ReplicationConfig {
        public final int replicationFactor;
        public final int requiredReads;
        public final int requiredWrites;

        public ReplicationConfig(int replicationFactor, int requiredReads, int requiredWrites) {
            this.replicationFactor = replicationFactor;
            this.requiredReads = requiredReads;
            this.requiredWrites = requiredWrites;
        }
    }

    private final String directory;
    private VoldemortConfig config;
    private VoldemortServer server;
    private final int port;
    private StoreClient<String, Map<String, Object>> storeClient;
    private AdminClient adminClient;
    private final List<Integer> partitionsList;
    private final String storeName;

    public VoldemortServerHandle(String directory, int port) {
        this.directory = directory;
        this.port = port;
        partitionsList = Arrays.asList(0, 1, 2, 3, 4, 5, 6, 7);
        storeName = "users";
    }

    public int getPort() {
        return port;
    }

    public String getDirectory() {
        return directory;
    }

    public String getBootstrapUrl() {
        return "tcp://localhost:" + port;
    }

    public List<Integer> getPartitionsList() {
        return partitionsList;
    }

    public String getStoreName() {
        return storeName;
    }

    public void invalidateClients() {
        storeClient = null;
        adminClient = null;
    }

    public StoreClient<String, Map<String, Object>> getStoreClient() {
        if (storeClient == null) {
            storeClient = (new SocketStoreClientFactory(new ClientConfig().setBootstrapUrls(getBootstrapUrl())))
                    .getStoreClient(getStoreName());
        }
        return storeClient;
    }

    public AdminClient getAdminClient() {
        if (adminClient == null) {
            adminClient = new AdminClient(getBootstrapUrl());
        }
        return adminClient;
    }

    public ReplicationConfig getReplicationConfig() {
        List<StoreDefinition> storeDefinitions = getAdminClient().metadataMgmtOps.getRemoteStoreDefList().getValue();
        for (StoreDefinition storeDef : storeDefinitions) {
            if (storeDef.getName().equals(getStoreName())) {
                return new ReplicationConfig(storeDef.getReplicationFactor(), storeDef.getRequiredReads(),
                        storeDef.getPreferredWrites());
            }
        }
        throw new RuntimeException("Store [" + getStoreName() + "] not found in definitions.");
    }

    public void updateReplicationConfig(ReplicationConfig replicationConfig) {
        List<StoreDefinition> storeDefinitions = getAdminClient().metadataMgmtOps.getRemoteStoreDefList().getValue();
        StoreDefinition sd = null;
        int index = 0;
        for (StoreDefinition storeDef : storeDefinitions) {
            if (storeDef.getName().equals(getStoreName())) {
                sd = storeDef;
                break;
            }
            index++;
        }
        if (sd == null) {
            throw new RuntimeException("Store [" + getStoreName() + "] not found in definitions.");
        }

        StoreDefinition newStoreDefinition = new StoreDefinition(
                sd.getName(),
                sd.getType(),
                sd.getDescription(),
                sd.getKeySerializer(),
                sd.getValueSerializer(),
                sd.getTransformsSerializer(),
                sd.getRoutingPolicy(),
                sd.getRoutingStrategyType(),
                replicationConfig.replicationFactor,
                null,
                replicationConfig.requiredReads,
                null,
                replicationConfig.requiredWrites,
                sd.getViewTargetStoreName(),
                sd.getValueTransformation(),
                sd.getZoneReplicationFactor(),
                sd.getZoneCountReads(),
                sd.getZoneCountWrites(),
                sd.getRetentionDays(),
                sd.getRetentionScanThrottleRate(),
                sd.getRetentionFrequencyDays(),
                sd.getSerializerFactory(),
                sd.getHintedHandoffStrategyType(),
                sd.getHintPrefListSize(),
                sd.getOwners(),
                sd.getMemoryFootprintMB()
        );
        storeDefinitions.set(index, newStoreDefinition);

        getAdminClient().metadataMgmtOps.fetchAndUpdateRemoteStores(storeDefinitions);

        adminClient = null;
    }

    public void startServer() {
        if (server == null) {
            config = VoldemortConfig.loadFromVoldemortHome(directory);
            server = new VoldemortServer(config);
        }
        invalidateClients();
        server.start();
    }

    public void killServer() {
        stopServer();
    }

    public void stopServer() {
        if (server == null) {
            return;
        }
        server.stop();
        server = null;
    }

    public void stopServerAndClean() throws IOException, InterruptedException {
        stopServer();
        runScript("clean_up.sh");
    }

    private void runScript(String script) throws IOException, InterruptedException {
        Process p = Runtime.getRuntime().exec(
                Paths.get(directory + "/" + script).toAbsolutePath().normalize().toString());
        int exitValue = p.waitFor();
        if (exitValue != 0) {
            throw new RuntimeException(
                    "Voldemort node [" + script + "] script in directory [" + directory + "] failed with [" + exitValue + "] code.");
        }
    }
}
