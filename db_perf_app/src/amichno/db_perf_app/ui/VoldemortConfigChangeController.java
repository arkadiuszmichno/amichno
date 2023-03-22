package amichno.db_perf_app.ui;

import amichno.db_perf_app.DatabasesManager;
import amichno.db_perf_app.VoldemortServerHandle;
import javafx.fxml.FXML;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextFormatter;

import java.io.IOException;
import java.util.function.UnaryOperator;

public class VoldemortConfigChangeController extends BaseExecutionController {
    private final String databaseName;
    private final VoldemortServerHandle serverHandle;

    @FXML
    private DecimalTextField replicationFactor;
    @FXML
    private DecimalTextField requiredReads;
    @FXML
    private DecimalTextField requiredWrites;

    public VoldemortConfigChangeController(
            TextArea queryLog,
            String databaseName,
            VoldemortServerHandle serverHandle) throws IOException {
        super(queryLog, VoldemortConfigChangeController.class.getResource("configChangingDialog.fxml"));
        this.databaseName = databaseName;
        this.serverHandle = serverHandle;
    }

    public void setReplicationConfig(VoldemortServerHandle.ReplicationConfig replicationConfig) {
        replicationFactor.setText(String.valueOf(replicationConfig.replicationFactor));
        requiredReads.setText(String.valueOf(replicationConfig.requiredReads));
        requiredWrites.setText(String.valueOf(replicationConfig.requiredWrites));
    }

    @FXML
    private void updateReplicationConfig() {
        VoldemortServerHandle.ReplicationConfig replicationConfig;
        try {
            replicationConfig = new VoldemortServerHandle.ReplicationConfig(
                    Integer.parseInt(replicationFactor.getText()),
                    Integer.parseInt(requiredReads.getText()),
                    Integer.parseInt(requiredWrites.getText())
            );
        } catch (NumberFormatException ignore) {
            return;
        }

        executeOperation(() -> {
            StringBuilder sb = new StringBuilder();
            sb.append("[").append(databaseName).append("] ");
            try {
                serverHandle.updateReplicationConfig(replicationConfig);
                for (VoldemortServerHandle node : DatabasesManager.getInstance().getVoldemortServers())
                    node.invalidateClients();
            } catch (Throwable exc) {
                sb.append("Błąd podczas rekonfigurania wezłów. Konfiguracja nie została zmieniona.\n");
                appendExceptionContent(exc, sb);
                appendToQueryLog(sb.toString());
                setReplicationConfig(serverHandle.getReplicationConfig());
                return;
            }
            VoldemortServerHandle.ReplicationConfig newReplicationConfig = serverHandle.getReplicationConfig();
            sb.append("Konfiguracja węzłów została zmieniona:\n");
            sb.append("   Ilość replik: ").append(newReplicationConfig.replicationFactor).append("\n");
            sb.append("   Wymagane odczyty: ").append(newReplicationConfig.requiredReads).append("\n");
            sb.append("   Wymagane zapisy: ").append(newReplicationConfig.requiredWrites);
            appendToQueryLog(sb.toString());
            setReplicationConfig(newReplicationConfig);
        });
    }
}
