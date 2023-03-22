package amichno.db_perf_app.ui;

import amichno.db_perf_app.QueryExecutor;
import javafx.application.Platform;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;

public class RecordsCountController {
    private final QueryExecutor queryExecutor;
    private final HBox pane;
    private final Label countLabel;

    public RecordsCountController(QueryExecutor queryExecutor, String type) {
        this.queryExecutor = queryExecutor;

        pane = new HBox();
        pane.getChildren().add(new Label("Obiektów w bazie: "));
        countLabel = new Label("b/d");
        countLabel.setStyle("-fx-font-weight: bold;");
        pane.getChildren().add(countLabel);
        pane.getChildren().add(new Label(" (" + type + ")"));
    }

    public HBox getPane() {
        return pane;
    }

    public void updateRecordsCount() {
        Long count = queryExecutor.countRecords();
        Platform.runLater(() -> {
            if (count == null) {
                countLabel.setText("b/d");
            } else {
                countLabel.setText(QueryExecutionController.formatNumber(count));
            }
        });
    }
}
