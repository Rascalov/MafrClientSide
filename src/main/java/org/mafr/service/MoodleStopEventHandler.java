package org.mafr.service;

import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.control.Button;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import org.mafr.HomeController;

public class MoodleStopEventHandler implements EventHandler<ActionEvent> {
    private Task taskToHandle;
    public  MoodleStopEventHandler(Task taskToHandle){
        this.taskToHandle = taskToHandle;
    }
    @Override
    public void handle(ActionEvent actionEvent) {
        // the fields need to be re-enabled (now possible with static nodesMap).
        // get string prop to display the cancellation.
        Button source = ((Button) actionEvent.getSource());
        source.setDisable(true);
        String id = source.getId().split("_")[0];
        SimpleStringProperty logMessage = (SimpleStringProperty) HomeController.propertyMap.get(id + HomeController.logLabelMessageSuffix);
        Platform.runLater(new UpdateLabelRunnable(logMessage, "Cancelling..."));
        TextField courseId = (TextField) HomeController.nodesMap.get(id + "_courseIdField");
        ChoiceBox<String> type = (ChoiceBox<String>) HomeController.nodesMap.get(id + "_acquisitionChoiceBox");
        TextField folder = (TextField) HomeController.nodesMap.get(id + "_locationField");
        Button browseButton = (Button) HomeController.nodesMap.get(id + HomeController.browseButtonSuffix);
        if(taskToHandle.cancel(false)){
            System.out.println("Stopped for real");
        }
        else {
            System.out.println("Failed to stop");
        }

        courseId.setDisable(false);
        type.setDisable(false);
        folder.setDisable(false);
        browseButton.setDisable(false);
        source.setOnAction(new MoodleStartEventHandler());
    }
}
