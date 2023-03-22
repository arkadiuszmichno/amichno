package amichno.db_perf_app;

import java.util.LinkedList;
import java.util.List;

public class CrashPerformer {
    private final List<ServerCrashHandler> crashHandlers = new LinkedList<>();

    public CrashPerformer() {
    }

    public void addCrashHandler(ServerCrashHandler crashHandler) {
        crashHandlers.add(crashHandler);
    }

    public void performCrash() throws CrashException {
        for (ServerCrashHandler handler : crashHandlers)
            handler.crashServer();
        for (ServerCrashHandler handler : crashHandlers)
            handler.restoreServerLater();
    }

    public void forceCrashFinish() throws CrashException {
        for (ServerCrashHandler handler : crashHandlers)
            handler.restoreServerNow();
    }
}
