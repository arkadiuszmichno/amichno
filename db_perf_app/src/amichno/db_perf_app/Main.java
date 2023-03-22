package amichno.db_perf_app;

import amichno.db_perf_app.ui.ErrorController;
import amichno.db_perf_app.ui.InitController;
import javafx.application.Application;
import javafx.stage.Stage;

import java.util.LinkedList;
import java.util.List;


public class Main extends Application {

    @Override
    public void start(Stage primaryStage) throws Exception {
        Thread.setDefaultUncaughtExceptionHandler(ErrorController::showError);

        InitController.performAppInitialization();
    }

    @Override
    public void stop() throws Exception {
        super.stop();
        List<Throwable> exceptions = new LinkedList<>();
        for (VoldemortServerHandle server : DatabasesManager.getInstance().getVoldemortServers()) {
            try {
                server.stopServerAndClean();
            } catch (Throwable e) {
                exceptions.add(e);
            }
        }
        if (!exceptions.isEmpty()) {
            throw new RuntimeException("There were [" + exceptions.size() + "] exceptions during application shutdown.", exceptions.get(0));
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}
