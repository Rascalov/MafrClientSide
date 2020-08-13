package org.mafr.service;

import javafx.beans.property.SimpleStringProperty;

public class UpdateLabelRunnable implements Runnable {
    SimpleStringProperty labelProperty;
    String message;
    public UpdateLabelRunnable(SimpleStringProperty property, String message){
        this.labelProperty = property;
        this.message = message;
    }
    @Override
    public void run() {
        labelProperty.setValue(message);
    }
}
