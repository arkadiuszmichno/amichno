package amichno.db_perf_app.ui;

import amichno.db_perf_app.*;
import javafx.beans.value.ObservableValue;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Group;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.CheckBox;
import javafx.scene.control.TextArea;
import javafx.scene.layout.GridPane;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class MainController {
    private static final String BUTTON_LABEL = "button_label";
    private static final String SUCCESS_QUERY_MESSAGE = "query_success";
    private static final String AFFECTED_OBJECTS = "affected_objects";
    private static final String FAILURE_QUERY_MESSAGE = "query_failure";
    private static final String DATABASE_NAME = "database_name";

    private final MySQLQueryExecutor mySQLSingleQueryExecutor;
    private final MySQLQueryExecutor mySQLReplicationQueryExecutor;
    private final VoldemortQueryExecutor voldemortQueryExecutor;
    private VoldemortConfigChangeController voldemortConfigChangeController;

    @FXML
    private GridPane voldemortQueryPane;
    @FXML
    private GridPane mySQLSingleQueryPane;
    @FXML
    private GridPane mySQLReplicationQueryPane;

    @FXML
    private GridPane voldemortCrashPane;
    @FXML
    private GridPane mySQLSingleCrashPane;
    @FXML
    private GridPane mySQLReplicationCrashPane;

    @FXML
    private TextArea queryLog;

    @FXML
    private Group voldemortConfigChangePlaceholder;

    public MainController() {
        DatabasesManager dbManager = DatabasesManager.getInstance();
        mySQLSingleQueryExecutor = new MySQLQueryExecutor(dbManager.getMySQLSingle(), dbManager.getMySQLSingleIdsCache());
        mySQLReplicationQueryExecutor = new MySQLQueryExecutor(dbManager.getMySQLReplicationMaster(), dbManager.getMySQLReplicationIdsCache());
        voldemortQueryExecutor = new VoldemortQueryExecutor(dbManager.getVoldemortFirstServer(), dbManager.getVoldemortKeysManager());
    }

    @FXML
    private void initialize() throws IOException {
        DatabasesManager dbManager = DatabasesManager.getInstance();

        CrashController mySQLSingleCrashController = new CrashController(mySQLSingleQueryExecutor);
        appendCrashPane("MySQL", mySQLSingleCrashPane, 2,
                mySQLSingleCrashController, dbManager.getMySQLSingle());

        CrashController mySQLReplicationCrashController = new CrashController(mySQLReplicationQueryExecutor);
        appendCrashPane("Główny", mySQLReplicationCrashPane, 2,
                mySQLReplicationCrashController, dbManager.getMySQLReplicationMaster());
        appendCrashPane("Replika 1", mySQLReplicationCrashPane, 3,
                mySQLReplicationCrashController, dbManager.getMySQLReplicationSlave1());
        appendCrashPane("Replika 2", mySQLReplicationCrashPane, 4,
                mySQLReplicationCrashController, dbManager.getMySQLReplicationSlave2());

        CrashController voldemortCrashController = new CrashController(voldemortQueryExecutor);
        for (int nodeId = 0; nodeId < 4; nodeId++)
            appendCrashPane("Node " + nodeId, voldemortCrashPane, nodeId + 2,
                    voldemortCrashController, dbManager.getVoldemortServers()[nodeId]);

        initializeQueryPane("Voldemort", voldemortQueryPane,
                voldemortQueryExecutor,
                voldemortCrashController,
                new RecordsCountController(voldemortQueryExecutor, "cache"));
        initializeQueryPane("MySQL bez replikacji", mySQLSingleQueryPane,
                mySQLSingleQueryExecutor, mySQLSingleCrashController,
                new RecordsCountController(mySQLSingleQueryExecutor, "count"));
        initializeQueryPane("MySQL z replikacją", mySQLReplicationQueryPane,
                mySQLReplicationQueryExecutor,
                mySQLReplicationCrashController,
                new RecordsCountController(mySQLReplicationQueryExecutor, "count"));

        voldemortConfigChangeController = new VoldemortConfigChangeController(queryLog, "Voldemort", voldemortQueryExecutor.getServerHandle());
        FXMLLoader configChangeLoader = new FXMLLoader(VoldemortConfigChangeController.class.getResource("voldemortConfigChange.fxml"));
        configChangeLoader.setController(voldemortConfigChangeController);
        voldemortConfigChangePlaceholder.getChildren().add(configChangeLoader.load());
    }

    private void appendCrashPane(String serverName, GridPane pane, int rowIndex, CrashController crashController, ServerHandle serverHandle) {
        CheckBox crashSelected = new CheckBox(serverName);

        DecimalTextField crashDuration = new DecimalTextField("500");
        crashDuration.setMaxWidth(75);
        crashDuration.setDisable(true);
        crashSelected.selectedProperty()
                .addListener((ObservableValue<? extends Boolean> ov, Boolean oldVal, Boolean newVal) -> crashDuration.setDisable(!newVal));

        pane.addRow(rowIndex, crashSelected, crashDuration);

        crashController.addCrashDefinition(crashSelected, crashDuration, serverHandle);
    }

    private void initializeQueryPane(String databaseName, GridPane pane, QueryExecutor queryExecutor, CrashController crashController, RecordsCountController recordsCountController) throws IOException {
        Map<String,String> messagesMap;
        QueryExecutionController executionController;

        messagesMap = new HashMap<>();
        messagesMap.put(BUTTON_LABEL, "Pobierz z bazy");
        messagesMap.put(SUCCESS_QUERY_MESSAGE, "Wykonano zapytanie pobrania %s obiektów z bazy");
        messagesMap.put(AFFECTED_OBJECTS, "Pobranych obiektów");
        messagesMap.put(FAILURE_QUERY_MESSAGE, "Błąd podczas przetwarzania zapytania pobrania %s obiektów z bazy");
        messagesMap.put(DATABASE_NAME, databaseName);
        executionController = new QueryExecutionController(queryLog,
                queryExecutor::executeSelectQuery, crashController, recordsCountController, messagesMap);

        pane.add(executionController.getObjectsCountField(), 0,2);
        pane.add(executionController.getExecuteButton(), 1,2);

        messagesMap = new HashMap<>();
        messagesMap.put(BUTTON_LABEL, "Dodaj do bazy");
        messagesMap.put(SUCCESS_QUERY_MESSAGE, "Wykonano zapytanie dodania %s obiektów do bazy");
        messagesMap.put(AFFECTED_OBJECTS, "Dodanych obiektów");
        messagesMap.put(FAILURE_QUERY_MESSAGE, "Błąd podczas przetwarzania zapytania dodania %s obiektów do bazy");
        messagesMap.put(DATABASE_NAME, databaseName);
        executionController = new QueryExecutionController(queryLog,
                queryExecutor::executeInsertQuery, crashController, recordsCountController, messagesMap);

        pane.add(executionController.getObjectsCountField(), 0,3);
        pane.add(executionController.getExecuteButton(), 1,3);

        messagesMap = new HashMap<>();
        messagesMap.put(BUTTON_LABEL, "Zmień w bazie");
        messagesMap.put(SUCCESS_QUERY_MESSAGE, "Wykonano zapytanie zmiany %s obiektów w bazie");
        messagesMap.put(AFFECTED_OBJECTS, "Zmieniony obiektów");
        messagesMap.put(FAILURE_QUERY_MESSAGE, "Błąd podczas przetwarzania zapytania zmiany %s obiektów w bazie");
        messagesMap.put(DATABASE_NAME, databaseName);
        executionController = new QueryExecutionController(queryLog,
                queryExecutor::executeUpdateQuery, crashController, recordsCountController, messagesMap);

        pane.add(executionController.getObjectsCountField(), 0,4);
        pane.add(executionController.getExecuteButton(), 1,4);

        messagesMap = new HashMap<>();
        messagesMap.put(BUTTON_LABEL, "Usuń z bazy");
        messagesMap.put(SUCCESS_QUERY_MESSAGE, "Wykonano zapytanie usunięcia %s obiektów z bazy");
        messagesMap.put(AFFECTED_OBJECTS, "Usuniętych obiektów");
        messagesMap.put(FAILURE_QUERY_MESSAGE, "Błąd podczas przetwarzania zapytania usunięcia %s obiektów z bazy");
        messagesMap.put(DATABASE_NAME, databaseName);
        executionController = new QueryExecutionController(queryLog,
                queryExecutor::executeDeleteQuery, crashController, recordsCountController, messagesMap);

        pane.add(executionController.getObjectsCountField(), 0,5);
        pane.add(executionController.getExecuteButton(), 1,5);

        pane.add(recordsCountController.getPane(), 0, 6, 2, 1);
    }

    private void setStartupReplicationConfig(VoldemortServerHandle.ReplicationConfig replicationConfig) {
        voldemortConfigChangeController.setReplicationConfig(replicationConfig);
    }

    public static void startMainInterface(VoldemortServerHandle.ReplicationConfig replicationConfig) throws IOException {
        Stage mainStage = new Stage();
        mainStage.setTitle("Tester wydajności baz danych");
        FXMLLoader loader = new FXMLLoader(MainController.class.getResource("main.fxml"));
        Parent root = loader.load();

        ((MainController)loader.getController()).setStartupReplicationConfig(replicationConfig);

        mainStage.setScene(new Scene(root, 1024, 600));
        mainStage.show();
    }
}
