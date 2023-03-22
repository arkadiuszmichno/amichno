package amichno.db_perf_app;

import voldemort.client.protocol.admin.AdminClient;
import voldemort.utils.ByteArray;

import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

public class VoldemortKeysManager {
    private final VoldemortServerHandle serverHandle;
    private long lastKey;
    private List<String> keysCache;

    public VoldemortKeysManager(VoldemortServerHandle serverHandle) {
        this.serverHandle = serverHandle;
        lastKey = 0;
        keysCache = null;
    }

    public String getNextKey() {
        lastKey++;
        return Long.toHexString(lastKey);
    }

    public void invalidateKeysCache() {
        keysCache = null;
    }

    public String[] getRandomKeys(int count) {
        if (keysCache == null) {
            refreshKeysCache();
        }

        Collections.shuffle(keysCache);

        if (count > keysCache.size()) {
            count = keysCache.size();
        }

        String[] keys = new String[count];
        int i = 0;
        for (String key : keysCache) {
            keys[i] = key;
            i++;
            if (i >= count) {
                break;
            }
        }
        return keys;
    }

    public int getKeysCount() {
        if (keysCache == null) {
            refreshKeysCache();
        }

        return keysCache.size();
    }

    private void refreshKeysCache() {
        AdminClient client = serverHandle.getAdminClient();
        Iterator<ByteArray> keysIterator = client.bulkFetchOps.fetchKeys(0, serverHandle.getStoreName(),
                serverHandle.getPartitionsList(), null, false);
        LinkedList<String> keysList = new LinkedList<>();
        keysIterator.forEachRemaining(key -> keysList.add(new String(key.get())));
        keysCache = keysList;
    }
}
