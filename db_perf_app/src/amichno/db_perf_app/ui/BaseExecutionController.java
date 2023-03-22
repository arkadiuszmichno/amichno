package amichno.db_perf_app.ui;

import javafx.application.Platform;
import javafx.event.Event;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.TextArea;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.io.IOException;
import java.net.URL;

public class BaseExecutionController {
    protected final TextArea queryLog;
    protected final Stage processingDialog;

    public BaseExecutionController(TextArea queryLog, URL processingDialogFxmlResource) throws IOException {
        this.queryLog = queryLog;
        FXMLLoader loader = new FXMLLoader(processingDialogFxmlResource);
        processingDialog = loader.load();
        processingDialog.initModality(Modality.APPLICATION_MODAL);
        processingDialog.setOnCloseRequest(Event::consume);
    }

    protected void executeOperation(Runnable operation) {
        processingDialog.show();

        new Thread(() -> {
            operation.run();

            Platform.runLater(processingDialog::hide);
        }).start();
    }

    protected void appendToQueryLog(String text) {
        queryLog.appendText(text);
        queryLog.appendText("\n\n");
    }

    protected void appendExceptionContent(Throwable exc, StringBuilder sb) {
        sb.append("   Treść błędu: ");
        while (exc != null) {
            String message = exc.getLocalizedMessage();
            if (message == null) {
                message = exc.getMessage();
            }
            if (message == null) {
                message = exc.getClass().getSimpleName();
            }
            sb.append(message.replace("\n", " ").replace("\r", ""));
            sb.append(" (").append(exc.getClass().getSimpleName()).append(")");
            exc = exc.getCause();
            if (exc != null) {
                sb.append("\n                -> ");
            }
        }
    }
}
