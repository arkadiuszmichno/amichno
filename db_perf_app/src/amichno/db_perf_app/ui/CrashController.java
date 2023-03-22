package amichno.db_perf_app.ui;

import amichno.db_perf_app.CrashPerformer;
import amichno.db_perf_app.QueryExecutor;
import amichno.db_perf_app.ServerCrashHandler;
import amichno.db_perf_app.ServerHandle;
import javafx.scene.control.CheckBox;
import javafx.scene.control.TextField;

import java.util.LinkedList;
import java.util.List;

public class CrashController {
    public static class CrashDefinition {
        public final CheckBox selected;
        public final TextField durationField;
        public final ServerHandle serverHandle;

        public CrashDefinition(CheckBox selected, TextField durationField, ServerHandle serverHandle) {
            this.selected = selected;
            this.durationField = durationField;
            this.serverHandle = serverHandle;
        }

        public ServerCrashHandler createCrashHandler() throws NumberFormatException {
            if (!selected.isSelected()) {
                return null;
            }
            int duration = Integer.parseInt(durationField.getText());
            if (duration < 1) {
                throw new NumberFormatException("Crash duration less that 1.");
            }
            return new ServerCrashHandler(serverHandle, duration);
        }
    }

    private final QueryExecutor queryExecutor;
    private final List<CrashDefinition> crashDefinitions;

    public CrashController(QueryExecutor queryExecutor) {
        this.queryExecutor = queryExecutor;
        crashDefinitions = new LinkedList<>();
    }

    public void addCrashDefinition(CheckBox selected, TextField durationField, ServerHandle serverHandle) {
        crashDefinitions.add(new CrashDefinition(selected, durationField, serverHandle));
    }

    public CrashPerformer updateCrashPerformer() throws NumberFormatException {
        CrashPerformer crashPerformer = new CrashPerformer();
        for (CrashDefinition definition : crashDefinitions) {
            ServerCrashHandler crashHandler = definition.createCrashHandler();
            if (crashHandler != null) {
                crashPerformer.addCrashHandler(crashHandler);
            }
        }
        queryExecutor.setCrashPerformer(crashPerformer);
        return crashPerformer;
    }
}
