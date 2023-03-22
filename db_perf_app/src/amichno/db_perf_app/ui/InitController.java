package amichno.db_perf_app.ui;

import amichno.db_perf_app.DatabasesManager;
import amichno.db_perf_app.Main;
import amichno.db_perf_app.MySQLServerHandle;
import amichno.db_perf_app.VoldemortServerHandle;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.stage.Stage;

import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;

public class InitController {
    @FXML
    private Label initLabel;

    public void setLabelText(String text) {
        initLabel.setText(text);
    }

    private void setLabelTextLater(String text) {
        Platform.runLater(() -> setLabelText(text));
    }

    public static void performAppInitialization() throws IOException {
        Stage initStage = new Stage();
        initStage.setTitle("Tester wydajności baz danych");
        FXMLLoader loader = new FXMLLoader(InitController.class.getResource("init.fxml"));
        Parent root = loader.load();
        initStage.setScene(new Scene(root, 500, 200));
        initStage.show();

        new Thread(() -> {
            InitController controller = loader.getController();

            try {
                controller.setLabelTextLater("Inicjalizacja aplikacji...");
                initApplication();

                controller.setLabelTextLater("Inicjalizacja baz danych:\nInicjowanie i uruchamianie serwerów MySQL...");
                initMySQLServers();

                controller.setLabelTextLater("Inicjalizacja baz danych:\nKonfigurowanie replikacji MySQL...");
                initMySQLReplication();

                controller.setLabelTextLater("Inicjalizacja baz danych:\nPrzygotowywanie tabel i użytkownika MySQL...");
                createMySQLDatabases();

                controller.setLabelTextLater("Inicjalizacja baz danych:\nŁączenie z bazami MySQL...");
                connectMySQLDatabases();

                controller.setLabelTextLater(
                        "Inicjalizacja baz danych:\nInicjowanie i uruchamianie serwerów Voldemort...");
                initVoldemortServers();

                controller.setLabelTextLater("Inicjalizacja baz danych:\nŁączenie z bazą Voldemort...");
                connectVoldemortServer();

                controller.setLabelTextLater("Inicjalizacja baz danych:\nPobieranie konfiguracji Voldemort...");
                VoldemortServerHandle.ReplicationConfig replicationConfig = fetchVoldemortReplicationConfig();

                Platform.runLater(() -> {
                    initStage.close();
                    try {
                        MainController.startMainInterface(replicationConfig);
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                });
            } catch (RuntimeException exc) {
                throw exc;
            } catch (Throwable exc) {
                throw new RuntimeException(exc);
            }
        }).start();
    }

    private static void initApplication() throws IOException {
        Properties p = new Properties();
        p.load(Main.class.getResourceAsStream("log4j.properties"));
        PropertyConfigurator.configure(p);

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            List<Throwable> exceptions = new LinkedList<>();
            for (MySQLServerHandle server : DatabasesManager.getInstance().getMySQLServers()) {
                try {
                    server.killServerAndClean();
                } catch (Throwable e) {
                    exceptions.add(e);
                }
            }
            for (VoldemortServerHandle server : DatabasesManager.getInstance().getVoldemortServers()) {
                try {
                    server.stopServerAndClean();
                } catch (Throwable e) {
                    exceptions.add(e);
                }
            }
            if (!exceptions.isEmpty()) {
                throw new RuntimeException(
                        "There were [" + exceptions.size() + "] exceptions during application shutdown.",
                        exceptions.get(0));
            }
        }));
    }

    private static void initMySQLServers() throws InterruptedException {
        List<Thread> mySQLInitThreads = new LinkedList<Thread>();
        for (MySQLServerHandle server : DatabasesManager.getInstance().getMySQLServers()) {
            Thread thread = new Thread(() -> {
                try {
                    server.initServer();
                    server.startServer();
                    server.setupServer();
                } catch (Throwable exc) {
                    throw new RuntimeException(exc);
                }
            });
            thread.start();
            mySQLInitThreads.add(thread);
        }

        for (Thread thread : mySQLInitThreads) {
            thread.join();
        }
    }

    private static void initMySQLReplication() throws SQLException {
        DatabasesManager dbManager = DatabasesManager.getInstance();

        String replicationFile = null;
        int replicationPos = 0;
        try (Connection conn = DriverManager.getConnection(dbManager.getMySQLReplicationMaster().getJdbcURL(), "root",
                "zaqwsx")) {
            try (Statement stmt = conn.createStatement()) {
                try (ResultSet rs = stmt.executeQuery("SHOW MASTER STATUS")) {
                    while (rs.next()) {
                        replicationFile = rs.getString("File");
                        replicationPos = rs.getInt("Position");
                    }
                }
            }
        }

        MySQLServerHandle[] mySQLSlaves = {dbManager.getMySQLReplicationSlave1(), dbManager.getMySQLReplicationSlave2()};
        for (MySQLServerHandle slave : mySQLSlaves) {
            try (Connection conn = DriverManager.getConnection(slave.getJdbcURL(), "root", "zaqwsx")) {
                try (Statement stmt = conn.createStatement()) {
                    stmt.execute("STOP REPLICA");
                    stmt.execute("CHANGE REPLICATION SOURCE TO " +
                            "SOURCE_HOST='127.0.0.1', " +
                            "SOURCE_PORT=" + dbManager.getMySQLReplicationMaster().getPort() + ", " +
                            "SOURCE_USER='repuser', " +
                            "SOURCE_PASSWORD='123456', " +
                            "SOURCE_LOG_FILE='" + replicationFile + "', " +
                            "SOURCE_LOG_POS=" + replicationPos);
                    stmt.execute("START REPLICA");
                }
            }
        }
    }

    private static void createMySQLDatabases() throws IOException, InterruptedException {
        for (MySQLServerHandle server : DatabasesManager.getInstance().getMySQLServers()) {
            server.createDatabase();
        }
    }

    private static void connectMySQLDatabases() throws SQLException {
        DatabasesManager.getInstance().getMySQLSingle().getConnection();
        DatabasesManager.getInstance().getMySQLReplicationMaster().getConnection();
    }

    private static void initVoldemortServers() throws InterruptedException {
        for (VoldemortServerHandle server : DatabasesManager.getInstance().getVoldemortServers()) {
            server.startServer();
        }
    }

    private static void connectVoldemortServer() {
        DatabasesManager.getInstance().getVoldemortFirstServer().getStoreClient();
        DatabasesManager.getInstance().getVoldemortFirstServer().getAdminClient();
    }

    private static VoldemortServerHandle.ReplicationConfig fetchVoldemortReplicationConfig() {
        return DatabasesManager.getInstance().getVoldemortFirstServer().getReplicationConfig();
    }
}
