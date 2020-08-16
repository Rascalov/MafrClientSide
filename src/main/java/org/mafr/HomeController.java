package org.mafr;

import javafx.beans.property.Property;
import javafx.beans.property.SimpleStringProperty;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.stage.DirectoryChooser;
import javafx.stage.Stage;
import org.mafr.model.MoodlePreferences;
import org.mafr.service.MoodleStartEventHandler;

import java.io.File;
import java.util.HashMap;
import java.util.prefs.BackingStoreException;


public class HomeController {
    public static HashMap<String, Node> nodesMap = new HashMap<>();
    public static HashMap<String, Property> propertyMap = new HashMap<>();
    public static final String buttonSuffix = "_startButton";
    public static final String progressIndicatorSuffix = "_progressIndicator";
    public static final String typeSuffix = "_acquisitionChoiceBox";
    public static final String courseIdSuffix = "_courseIdField";
    public static final String logLabelSuffix = "_logLabel";
    public static final String folderLocationSuffix = "_locationField";
    public static final String logLabelMessageSuffix = "_logLabelMessage";
    public static final String courseTitleLabelSuffix = "_courseTitleLabel";
    public static final String browseButtonSuffix = "_browseButton";
    @FXML
    private FlowPane flowPane;
    @FXML
    private TextField serverUrl;
    @FXML
    private PasswordField serverPassword;

    public static String getServerPassword(){
        return ((TextField)nodesMap.get("serverPassword")).getText();
    }
    public static String getServerURL(){
        return ((TextField)nodesMap.get("serverUrl")).getText();
    }

    public void initialize(){
        nodesMap.put("serverUrl", serverUrl);
        nodesMap.put("serverPassword", serverPassword);
        Stage currentStage = App.getStage();
        MoodlePreferences preferences = new MoodlePreferences();
        if(preferences.serverCredentialsExist()){
            var credentials = preferences.getServerCredentials();
            serverUrl.setText(credentials.get("serverURL"));
            serverPassword.setText(credentials.get("serverPassword"));
        }
        try{
            preferences.getSavedCourses().forEach((k, v) ->{
                String[] values = v.split(";");
                AddCoursePanel();
                Pane pane = (Pane) flowPane.getChildren().get(flowPane.getChildren().size()-1);
                String id = pane.getId().split("_")[0];
                System.out.println("Adding for Panel id: " + id);
                System.out.println("Id from previous session: " + k);
                System.out.println("Values: " + v);
                ((TextField)nodesMap.get(id+ courseIdSuffix)).setText(values[0]);
                ((ChoiceBox<String>)nodesMap.get(id+ typeSuffix)).setValue(values[1]);
                ((TextField)nodesMap.get(id+ folderLocationSuffix)).setText(values[2]);

                if( ((ChoiceBox<String>)nodesMap.get(id+ typeSuffix)).getValue().equals("Update")){
                    ((Button) nodesMap.get(id+buttonSuffix)).fire();
                }

            });
        }catch (Exception e){
            System.out.println("Error occurred while retrieving previous session's courses: ");
            e.printStackTrace();
        }

        currentStage.setOnCloseRequest(windowEvent -> {
            try {
                preferences.clearCredentials();
            } catch (BackingStoreException e) {
                e.printStackTrace();
            }
            for (var pane : flowPane.getChildren()){
                String id = pane.getId().split("_")[0];
                String courseId = ((TextField) nodesMap.get(id + courseIdSuffix)).getText();
                String type = (String) ((ChoiceBox) nodesMap.get(id + typeSuffix)).getValue();
                String location = ((TextField) nodesMap.get(id + folderLocationSuffix)).getText();
                preferences.saveCourse(courseId, type, location, id);
                System.out.println("Saved course id:" + id);
            }
            preferences.rememberServerCredentials(serverUrl.getText(), serverPassword.getText());
            System.out.println("closing, saved server credentials");
        });
    }
    @FXML
    private void deleteSelectedCourses(){
        nodesMap.forEach((s, n) -> {
            if(n instanceof CheckBox)
                if(((CheckBox)n).isSelected())
                    removePane(n.getId().split("_")[0]);
        });
    }
    private void removePane(String id) {
        // first if the button is on "Stop", fire the event to cancel the task and then delete it.
        Button target = (Button) nodesMap.get(id+"_startButton");
        if(target.getText().equals("Stop"))
            target.fire();
        flowPane.getChildren().remove(nodesMap.get(id + "_CoursePane"));
    }

    @FXML
    private void AddCoursePanel(){
        int size = flowPane.getChildren().size();
        int id;
        if(size > 0) id = size + 1; else id = 1;
        Pane container = new Pane();
        container.getStyleClass().add("CoursePane");
        container.idProperty().set(id + "_CoursePane");
        container.setPrefWidth(480);
        container.setPrefHeight(240);
        // vbox
        VBox vBoxContainer = new VBox();
        vBoxContainer.setLayoutX(5);
        vBoxContainer.setPrefWidth(474);
        vBoxContainer.setPrefHeight(185);
        // checkbox and title
        vBoxContainer.getChildren().addAll(buildTitleBox(id), buildCourseBox(id), buildTypeInputBox(id), buildLocationBox(id));
        container.getChildren().add(vBoxContainer);
        container.getChildren().add(buildStartBox(id));
        nodesMap.put(id + "_CoursePane", container);
        flowPane.getChildren().add(container);
    }

    private HBox buildTitleBox(int id){
        String checkBoxName = id+"_checkBox";
        HBox titleHbox = new HBox();
        titleHbox.setPrefHeight(44);
        titleHbox.setPrefWidth(474);
        CheckBox containerCheckBox = new CheckBox();
        containerCheckBox.setPrefWidth(24);
        containerCheckBox.setPrefHeight(43);
        nodesMap.put(checkBoxName, containerCheckBox);
        containerCheckBox.idProperty().set(checkBoxName);
        Label courseLabel = new Label("Course Name");
        courseLabel.setWrapText(true);
        courseLabel.getStyleClass().set(0, "titleLabel");
        courseLabel.setPrefWidth(453);
        courseLabel.setPrefHeight(44);
        propertyMap.put(id + courseTitleLabelSuffix, new SimpleStringProperty("Course Name"));
        courseLabel.textProperty().bind(propertyMap.get(id + courseTitleLabelSuffix));
        titleHbox.getChildren().addAll(containerCheckBox, courseLabel);
        return titleHbox;
    }
    private HBox buildCourseBox(int id){
        HBox courseHbox = new HBox();
        courseHbox.setPrefHeight(29);
        courseHbox.setPrefWidth(476);
        TextField courseIdField = new TextField();
        courseIdField.setPrefWidth(171);
        courseIdField.setPrefHeight(26);
        courseIdField.promptTextProperty().setValue("CourseId...");
        courseIdField.idProperty().setValue(id + "_courseIdField");
        nodesMap.put(id+ courseIdSuffix, courseIdField);

        Label courseIdLabel = new Label("Course ID:");
        courseIdLabel.setPrefHeight(25);
        courseIdLabel.setPrefWidth(122);

        courseIdLabel.getStyleClass().add("inputLabel");
        courseHbox.getChildren().addAll(courseIdLabel, courseIdField);
        return courseHbox;
    }
    private HBox buildTypeInputBox(int id){
        String name = id+"_acquisitionChoiceBox";
        HBox acquisitionHbox = new HBox();
        acquisitionHbox.setPrefHeight(29);
        acquisitionHbox.setPrefWidth(476);
        Label acquisitionLabel = new Label("Acquisition Type:");
        acquisitionLabel.getStyleClass().add("inputLabel");
        acquisitionLabel.setPrefWidth(122);
        acquisitionLabel.setPrefHeight(25);
        ChoiceBox<String> acquisitionChoiceBox = new ChoiceBox<>();
        acquisitionChoiceBox.setPrefHeight(26);
        acquisitionChoiceBox.setPrefWidth(171);
        acquisitionChoiceBox.idProperty().setValue(name);
        nodesMap.put(name, acquisitionChoiceBox);
        acquisitionChoiceBox.getItems().addAll("Download", "Update");
        acquisitionChoiceBox.setValue("Download");
        acquisitionHbox.getChildren().addAll(acquisitionLabel, acquisitionChoiceBox);

        return acquisitionHbox;
    }
    private HBox buildLocationBox(int id){
        String locationName = id + folderLocationSuffix;
        HBox locationBox = new HBox();
        locationBox.setPrefHeight(21);
        locationBox.setPrefWidth(474);
        Label folderLabel = new Label("Folder Location:");
        folderLabel.getStyleClass().add("inputLabel");
        folderLabel.setPrefWidth(122);
        folderLabel.setPrefHeight(25);
        TextField locationField = new TextField();
        locationField.idProperty().set(locationName);
        locationField.promptTextProperty().set("/somewhere/in/your/sys");
        locationField.setPrefWidth(171);
        locationField.setPrefHeight(26);

        nodesMap.put(locationName, locationField);

        Button browseButton = new Button();
        browseButton.setText("Browse...");
        browseButton.prefWidth(79);
        browseButton.idProperty().set(id+"_browseButton");

        browseButton.setOnAction(selectFolder);
        nodesMap.put(id + browseButtonSuffix, browseButton);
        locationBox.getChildren().addAll(folderLabel, locationField, browseButton);
        return locationBox;
    }
    public static HBox buildStartBox(int id){
        String progressbarName = id+"_progressbar";
        String logLabelName = id+"_logLabel";
        String buttonName = id+"_startButton";
        HBox startHbox = new HBox();
        startHbox.setPrefHeight(80);
        startHbox.setPrefWidth(475);
        Button startButton = new Button();
        startButton.setPrefHeight(52);
        startButton.setPrefWidth(132);
        startButton.setText("Start");
        startButton.getStyleClass().add("MoodleButton");
        startButton.idProperty().set(buttonName);
        nodesMap.put(buttonName, startButton);
        startButton.setOnAction(new MoodleStartEventHandler());
        ProgressIndicator indicator = new ProgressIndicator();
        indicator.setProgress(0);
        nodesMap.put(progressbarName, indicator);
        Label logLabel = new Label("");
        logLabel.setWrapText(true);
        logLabel.setAlignment(Pos.TOP_LEFT);
        logLabel.setMaxHeight(52);
        logLabel.setMaxWidth(280);
        propertyMap.put(id + logLabelMessageSuffix, new SimpleStringProperty(""));
        logLabel.textProperty().bind(propertyMap.get(id + logLabelMessageSuffix));
        logLabel.scaleShapeProperty().setValue(false);
        nodesMap.put(logLabelName, logLabel);
        startHbox.getChildren().addAll(startButton, indicator, logLabel);
        startHbox.setLayoutX(0);
        startHbox.setLayoutY(185);
        return  startHbox;
    }
    EventHandler<ActionEvent> selectFolder = actionEvent -> {
        DirectoryChooser directoryChooser = new DirectoryChooser();
        directoryChooser.setTitle("Select Folder");
        final File selectedDirectory =
                directoryChooser.showDialog(App.getStage());
        Button source = ((Button)actionEvent.getSource());
        String id = source.getId().split("_")[0];
        TextField folder = (TextField) nodesMap.get(id+"_locationField");
        if(selectedDirectory != null)
            folder.setText(selectedDirectory.getAbsolutePath());
    };

}
