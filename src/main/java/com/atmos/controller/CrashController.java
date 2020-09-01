package com.atmos.controller;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

public class CrashController {

    private StringProperty crashCause = new SimpleStringProperty();

    public void setCrashMessage(Exception e) {
        setCrashCause(e.getMessage() != null ? e.toString() : "Couldn't retrieve the message cause sadly :(");
    }

    public String getCrashCause() {
        return crashCause.get();
    }

    public StringProperty crashCauseProperty() {
        return crashCause;
    }

    public void setCrashCause(String crashCause) {
        this.crashCause.set(crashCause);
    }

}