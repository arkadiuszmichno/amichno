package amichno.db_perf_app;

public class ServerCrashHandler {
    private final ServerHandle serverHandle;
    private final int duration;
    private boolean willRestore;
    Thread restoringThread;

    public ServerCrashHandler(ServerHandle serverHandle, int duration) {
        this.serverHandle = serverHandle;
        this.duration = duration;
        willRestore = false;
        restoringThread = null;
    }

    public void crashServer() throws CrashException {
        synchronized (this) {
            if (willRestore)
                throw new CrashException("Server already marked as crashed.");
            try {
                serverHandle.killServer();
            } catch (Throwable e) {
                throw new CrashException(e);
            }
            willRestore = true;
        }
    }

    private void restoreServer() {
        if (!willRestore)
            return;
        try {
            serverHandle.startServer();
        } catch (Throwable exc) {
            throw new CrashException(exc);
        }
        willRestore = false;
    }

    public void restoreServerNow() {
        synchronized (this) {
            if (restoringThread != null) {
                restoringThread.interrupt();
                restoringThread = null;
            }
            restoreServer();
        }
    }

    public void restoreServerLater() {
        synchronized (this) {
            ServerCrashHandler self = this;
            restoringThread = new Thread(() -> {
                try {
                    Thread.sleep(duration);
                } catch (InterruptedException e) {
                    synchronized (self) {
                        if (restoringThread == Thread.currentThread())
                            restoringThread = null;
                        return;
                    }
                }
                synchronized (self) {
                    restoringThread = null;
                    restoreServer();
                }
            });
            restoringThread.start();
        }
    }
}
