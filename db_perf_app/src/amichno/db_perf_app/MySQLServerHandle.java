package amichno.db_perf_app;

import java.io.IOException;
import java.net.Socket;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

public class MySQLServerHandle implements ServerHandle {
    private final String directory;
    private final int port;
    private Connection connection;

    public MySQLServerHandle(String directory, int port) {
        this.directory = directory;
        this.port = port;
    }

    public int getPort() {
        return port;
    }

    public String getJdbcURL() {
        return getJdbcURL("");
    }

    public String getJdbcURL(String database) {
        return "jdbc:mysql://localhost:" + port + "/" + database + "?useSSL=false";
    }

    public Connection getConnection() throws SQLException {
        if(connection == null || connection.isClosed() || !connection.isValid(8))
            connection = DriverManager.getConnection(getJdbcURL("user_db"), "user", "123");
        return connection;
    }

    private String scriptPath(String script) {
        return Paths.get(directory + "/" + script).toAbsolutePath().normalize().toString();
    }

    private void runScript(String script) throws IOException, InterruptedException {
        Process p = Runtime.getRuntime().exec(scriptPath(script));
        int exitValue = p.waitFor();
        if (exitValue != 0) {
            throw new RuntimeException("MySQL server [" + script + "] script in directory [" + directory + "] failed with [" + exitValue + "] code.");
        }
    }

    public void initServer() throws IOException, InterruptedException {
        runScript("init_server.sh");
    }

    public void startServer() throws IOException, InterruptedException {
        if (isServerListening())
            throw new IllegalStateException("Cannot start MySQL server in directory [" + directory + "] because port is already taken.");

        if (connection != null) {
            try {
                connection.close();
            } catch (SQLException ignore) {
            } finally {
                connection = null;
            }
        }

        Process process = Runtime.getRuntime().exec(scriptPath("start_server.sh"));

        long startTime = System.currentTimeMillis();
        while (true) {
            process.waitFor(500, TimeUnit.MILLISECONDS);
            if (!process.isAlive()) {
                int exitValue = process.exitValue();
                throw new RuntimeException("MySQL server [start_server.sh] script in directory [" + directory + "] exited with [" + exitValue + "] code.");
            }

            if (isServerListening())
                break;

            long timeElapsed = System.currentTimeMillis() - startTime;
            if (timeElapsed >= 12 * 1000) {
                process.destroyForcibly();
                throw new RuntimeException("MySQL server [start_server.sh] script in directory [" + directory + "] did not start server after [" + String.format("%.1f", timeElapsed / 1000.0) + "] seconds.");
            }
        }
    }

    public void setupServer() throws IOException, InterruptedException {
        runScript("setup_server.sh");
    }

    public void createDatabase() throws IOException, InterruptedException {
        runScript("create_database.sh");
    }

    public void stopServer()  throws IOException, InterruptedException {
        runScript("stop_server.sh");
    }

    public void killServer()  throws IOException, InterruptedException {
        runScript("kill_server.sh");
    }

    public void killServerAndClean()  throws IOException, InterruptedException {
        if(connection != null) {
            try {
                connection.close();
            } catch (SQLException ignore) { }
        }
//        stopServer();
        runScript("clean_up.sh");
    }

    private boolean isServerListening() throws IOException {
        Socket socket = null;
        try {
            socket = new Socket("127.0.0.1", port);
            return true;
        }
        catch (IOException ignore) { }
        finally {
            if (socket != null)
                socket.close();
        }
        return false;
    }
}
