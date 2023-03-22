package amichno.db_perf_app.ui;

import javafx.scene.control.TextField;
import javafx.scene.control.TextFormatter;

import java.util.function.UnaryOperator;

public class DecimalTextField extends TextField {

    public DecimalTextField(String initText) {
        super(initText);

        UnaryOperator<TextFormatter.Change> filter = change -> {
            String text = change.getText();
            if (text.matches("[0-9]*")) {
                return change;
            }
            return null;
        };
        setTextFormatter(new TextFormatter<>(filter));
    }
}
