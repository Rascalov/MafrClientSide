package org.mafr.service;

import javafx.beans.property.SimpleStringProperty;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.control.*;
import org.mafr.HomeController;

public class MoodleStartEventHandler implements EventHandler<ActionEvent> {

    @Override
    public void handle(ActionEvent actionEvent) {
        System.out.println("Start with password: " + HomeController.getServerPassword());
        Button source = ((Button) actionEvent.getSource());
        String id = source.getId().split("_")[0];
        TextField courseId = (TextField) HomeController.nodesMap.get(id + "_courseIdField");
        ChoiceBox<String> type = (ChoiceBox<String>) HomeController.nodesMap.get(id + "_acquisitionChoiceBox");
        TextField folder = (TextField) HomeController.nodesMap.get(id + "_locationField");
        ProgressIndicator progressIndicator = (ProgressIndicator) HomeController.nodesMap.get(id + "_progressbar");
        SimpleStringProperty logMessage = (SimpleStringProperty) HomeController.propertyMap.get(id + HomeController.logLabelMessageSuffix);
        Button browseButton = (Button) HomeController.nodesMap.get(id + HomeController.browseButtonSuffix);
        if(HomeController.getServerURL().isEmpty()){
            logMessage.setValue("Enter a Server to use!");
            return;
        }
        else if(courseId.getText().isEmpty() || type.getValue().isEmpty() || folder.getText().isEmpty()) {
            logMessage.setValue("Please fill in all the fields!");
            return;
        }
        Task task = new MoodleTask(id, HomeController.getServerURL(), HomeController.getServerPassword());
        progressIndicator.progressProperty().bind(task.progressProperty());
        source.setOnAction(new MoodleStopEventHandler(task));
        source.setText("Stop");
        courseId.setDisable(true);
        type.setDisable(true);
        folder.setDisable(true);
        browseButton.setDisable(true);
        System.out.println("Started");
        new Thread(task).start();
    }
}
