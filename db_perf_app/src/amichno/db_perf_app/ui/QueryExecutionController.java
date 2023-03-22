package amichno.db_perf_app.ui;

import amichno.db_perf_app.CrashException;
import amichno.db_perf_app.CrashPerformer;
import amichno.db_perf_app.QueryResult;
import javafx.event.ActionEvent;
import javafx.scene.control.Button;
import javafx.scene.control.TextArea;

import java.io.IOException;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.NumberFormat;
import java.util.Locale;
import java.util.Map;
import java.util.NoSuchElementException;

public class QueryExecutionController extends BaseExecutionController {
    private static final DecimalFormat decimalFormatter;

    static {
        decimalFormatter = (DecimalFormat) NumberFormat.getInstance(Locale.US);
        DecimalFormatSymbols symbols = decimalFormatter.getDecimalFormatSymbols();
        symbols.setGroupingSeparator(' ');
        decimalFormatter.setDecimalFormatSymbols(symbols);
    }

    public static String formatNumber(long number) {
        return decimalFormatter.format(number);
    }

    private final DecimalTextField objectsCountField;
    private final Button executeButton;
    private final QueryExecution queryExecutionHandler;
    private final Map<String, String> messagesMapping;
    private final CrashController crashController;
    private final RecordsCountController recordsCountController;

    public QueryExecutionController(
            TextArea queryLog,
            QueryExecution queryExecutionHandler,
            CrashController crashController,
            RecordsCountController recordsCountController,
            Map<String, String> messagesMapping) throws IOException {
        super(queryLog, QueryExecutionController.class.getResource("queryExecutionDialog.fxml"));
        this.queryExecutionHandler = queryExecutionHandler;
        this.crashController = crashController;
        this.messagesMapping = messagesMapping;
        this.recordsCountController = recordsCountController;

        for (String messageKey : new String[]{"database_name", "query_success", "affected_objects", "query_failure", "button_label"}) {
            if (this.messagesMapping.get(messageKey) == null) {
                throw new NoSuchElementException("Missing [" + messageKey + "] key in messages mapping.");
            }
        }

        objectsCountField = new DecimalTextField("100");
        objectsCountField.setMaxWidth(120);

        executeButton = new Button(messagesMapping.get("button_label"));
        executeButton.setPrefWidth(120);
        executeButton.setOnAction((ActionEvent event) -> this.executeQuery());
    }

    public DecimalTextField getObjectsCountField() {
        return objectsCountField;
    }

    public Button getExecuteButton() {
        return executeButton;
    }

    public void executeQuery() {
        int count;
        try {
            count = Integer.parseInt(objectsCountField.getText());
        } catch (NumberFormatException ignore) {
            return;
        }
        if (count < 1) {
            return;
        }

        CrashPerformer crashPerformer;
        try {
            crashPerformer = crashController.updateCrashPerformer();
        } catch (NumberFormatException ignore) {
            return;
        }

        executeOperation(() -> {
            QueryResult result = null;
            try {
                result = queryExecutionHandler.execute(count);
            } catch (CrashException exc) {
                throw exc;
            } catch (Throwable exc) {
                StringBuilder sb = new StringBuilder();
                sb.append("[").append(messagesMapping.get("database_name")).append("] ");
                sb.append("Wystąpił krytyczny błąd podczas przetwarzania zapytania:\n");
                appendExceptionContent(exc, sb);
                appendToQueryLog(sb.toString());
            }

            if (result != null) {
                StringBuilder sb = new StringBuilder();
                sb.append("[").append(messagesMapping.get("database_name")).append("] ");
                if (result.error == null) {
                    sb.append(String.format(messagesMapping.get("query_success"), formatNumber(count))).append(":\n");
                    sb.append("   Czas wykonania: ").append(formatNumber(result.executionTime)).append(" ms\n");
                    sb.append("   ").append(messagesMapping.get("affected_objects")).append(": ").append(
                            formatNumber(result.objectsAffected)).append(" szt");
                } else {
                    sb.append(String.format(messagesMapping.get("query_failure"), formatNumber(count))).append(":\n");
                    if (result.executionTime > 0) {
                        sb.append("   Czas przetwarzania: ").append(formatNumber(result.executionTime)).append(" ms\n");
                    }
                    if (result.objectsAffected > 0) {
                        sb.append("   ").append(messagesMapping.get("affected_objects")).append(": ").append(
                                formatNumber(result.objectsAffected)).append(" szt\n");
                    }
                    appendExceptionContent(result.error, sb);
                }
                appendToQueryLog(sb.toString());
            }

            crashPerformer.forceCrashFinish();

            recordsCountController.updateRecordsCount();
        });
    }
}
