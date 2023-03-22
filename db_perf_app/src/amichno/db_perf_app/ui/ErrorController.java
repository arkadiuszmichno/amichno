package amichno.db_perf_app.ui;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.TextArea;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;

public class ErrorController {
    @FXML
    private TextArea errorMessage;

    public void appendErrorText(String text) {
        if (!errorMessage.getText().isEmpty()) {
            errorMessage.appendText("\n--------------------------------------------------\n\n");
        }
        errorMessage.appendText(text);
    }

    @FXML
    public void closeApplication() {
        Platform.exit();
    }

    public static void showError(Thread t, Throwable e) {
        System.err.println("An unexpected [" + e.getClass().getSimpleName() + "] error occurred in " + t);
        if (Platform.isFxApplicationThread()) {
            appendErrorToDialog(e);
        } else {
            Platform.runLater(() -> appendErrorToDialog(e));
        }
    }

    private static ErrorController errorController;

    private static void appendErrorToDialog(Throwable e) {
        if (errorController == null) {
            try {
                showErrorDialog();
            } catch (Throwable exc) {
                System.err.println(
                        "An unexpected error occurred during showing error dialog in " + Thread.currentThread());
                exc.printStackTrace(System.err);
                System.err.println("\nActual error:");
                e.printStackTrace(System.err);
                return;
            }
        }
        StringWriter errorMsg = new StringWriter();
        e.printStackTrace(new PrintWriter(errorMsg));
        errorController.appendErrorText(errorMsg.toString());
    }

    private static void showErrorDialog() throws IOException {
        Stage dialog = new Stage();
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.setTitle("Wystąpił błąd w aplikacji");
        FXMLLoader loader = new FXMLLoader(ErrorController.class.getResource("error.fxml"));
        Parent root = loader.load();
        dialog.setScene(new Scene(root, 600, 480));
        dialog.setOnCloseRequest(event -> ((ErrorController) loader.getController()).closeApplication());
        dialog.show();
        errorController = loader.getController();
    }
}
