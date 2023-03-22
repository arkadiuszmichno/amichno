package amichno.db_perf_app;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

public class MySQLIdsCache {
    private final MySQLServerHandle serverHandle;
    private List<Long> idsList;

    public MySQLIdsCache(MySQLServerHandle serverHandle) {
        this.serverHandle = serverHandle;
        idsList = null;
    }

    public void invalidateCache() {
        idsList = null;
    }

    public long[] getRandomIds(int count) throws SQLException {
        if (idsList == null)
            refreshIdsList();

        Collections.shuffle(idsList);

        if (count > idsList.size())
            count = idsList.size();

        long[] ids = new long[count];
        int i = 0;
        for (Long id : idsList) {
            ids[i] = id;
            i++;
            if (i >= count)
                break;
        }
        return ids;
    }

    private void refreshIdsList() throws SQLException {
        Connection connection = serverHandle.getConnection();
        LinkedList<Long> tempIdsList = new LinkedList<>();
        try (Statement stmt = connection.createStatement()) {
            try (ResultSet resultSet = stmt.executeQuery("SELECT id FROM users")) {
                while (resultSet.next())
                    tempIdsList.add(resultSet.getLong(1));
            }
        }
        idsList = tempIdsList;
    }
}
