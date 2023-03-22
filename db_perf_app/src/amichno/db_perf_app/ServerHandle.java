package amichno.db_perf_app;

public interface ServerHandle {
    void startServer() throws Exception;
    void killServer() throws Exception;
}
