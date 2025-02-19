package mg.itu.prom16.util;

import java.util.HashMap;
import java.util.List;

public class FormException extends Exception {
    private final String formUrl; // URL of the form
    private final String actionType; // Action type of the form
    private final HashMap<String, List<String>> errors; // Field errors
    private final HashMap<String, Object> submittedValues; // Submitted values

    public FormException(String formUrl, String actionType, HashMap<String, List<String>> errors, HashMap<String, Object> submittedValues) {
        super("Validation errors occurred for the form at: " + formUrl);
        this.formUrl = formUrl;
        this.actionType = actionType; // Initialize action type
        this.errors = errors;
        this.submittedValues = submittedValues;
    }

    public String getFormUrl() {
        return formUrl;
    }

    public String getActionType() { // Getter for action type
        return actionType;
    }

    public HashMap<String, List<String>> getErrors() {
        return errors;
    }

    public HashMap<String, Object> getSubmittedValues() {
        return submittedValues;
    }
} 