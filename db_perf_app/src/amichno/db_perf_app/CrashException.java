package amichno.db_perf_app;

public class CrashException extends RuntimeException {
    public CrashException(String message) {
        super(message);
    }

    public CrashException(Throwable e) {
        super(e);
    }
}
