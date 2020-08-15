package org.mafr.service;

import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.concurrent.Task;
import javafx.scene.control.Button;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.TextField;
import org.mafr.HomeController;
import org.mafr.model.MoodleFile;
import org.mafr.model.MoodleFolder;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;

public class MoodleTask extends Task {
    private int totalFiles;
    private int fileCounter = 0;
    private String id;
    private String serverUrl;
    private String serverPassword;
    private TextField courseId;
    private Button browseButton;
    private ChoiceBox<String> type;
    private TextField folder;
    private ProgressIndicator progressIndicator;
    private SimpleStringProperty logMessage;
    private MoodleAutomater automater; // Automater creation

    public MoodleTask(String NodeId, String serverUrl, String serverPassword){
        this.id = NodeId;
        this.serverUrl = serverUrl;
        this.serverPassword = serverPassword;
        courseId = (TextField) HomeController.nodesMap.get(id + "_courseIdField");
        type = (ChoiceBox<String>) HomeController.nodesMap.get(id + "_acquisitionChoiceBox");
        folder = (TextField) HomeController.nodesMap.get(id + "_locationField");
        progressIndicator = (ProgressIndicator) HomeController.nodesMap.get(id + "_progressbar");
        browseButton = (Button) HomeController.nodesMap.get(id+ HomeController.browseButtonSuffix);
        logMessage = (SimpleStringProperty) HomeController.propertyMap.get(id + HomeController.logLabelMessageSuffix);

    }
    @Override
    protected Object call(){
        try {
            automater = new MoodleAutomater(Integer.parseInt(courseId.getText()), serverUrl, serverPassword);
        } catch (NumberFormatException e) {
            Platform.runLater(new UpdateLabelRunnable(logMessage, "Invalid CourseId"));
            stopCourseProcess(false);
            return null;
        }
        try{
            while (true){
                if(isCancelled()){stopCourseProcess(true); return null;}
                MoodleFolder mainFolder = requestStructure();
                if(mainFolder == null){
                    stopCourseProcess(false);
                    return null;
                }
                if(isCancelled()){stopCourseProcess(true); return null;}
                Platform.runLater(new UpdateLabelRunnable(logMessage, "Received structure, creating folders..."));
                Platform.runLater(new UpdateLabelRunnable((SimpleStringProperty) HomeController.propertyMap.get(id + HomeController.courseTitleLabelSuffix), mainFolder.getName()));
                createFolders(mainFolder, folder.getText() + "/");
                Platform.runLater(new UpdateLabelRunnable(logMessage, "Folders created, indexing files..."));
                mainFolder = indexFiles(mainFolder);
                if(isCancelled()){stopCourseProcess(true); return null;}
                switch (type.getValue()){
                    case "Download":
                        totalFiles = getTotalFiles(mainFolder);
                        updateProgress(fileCounter, totalFiles);
                        downloadFiles(mainFolder);
                        stopCourseProcess(true);
                        return null;
                    case "Update":
                        updateFiles(mainFolder);
                        try {
                            Thread.sleep(2000);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                        break;
                }
            }
        }
        catch (RuntimeException e){
            Platform.runLater(new UpdateLabelRunnable(logMessage, e.getMessage()));
            System.out.println("Error message: " + e.getMessage());
            System.out.println("Stacktrace: ");
            e.printStackTrace();
            stopCourseProcess(false);
            return null;
        }
    }

    private void updateFiles(MoodleFolder folder) {
        for(var moodleFile : folder.getMoodleFiles()){
            if(isCancelled())
                return;
            Platform.runLater(new UpdateLabelRunnable(logMessage, "Checking file: " + moodleFile.getName()));
            File compareFile = new File(folder.getPath() + "/" + moodleFile.getName());
            if(compareFile.exists()){
                if(compareFile.lastModified() < moodleFile.getLastModified()){
                    Platform.runLater(new UpdateLabelRunnable(logMessage, "Downloading file: " + moodleFile.getName()));
                    automater.downloadFile(moodleFile, folder.getPath());
                }
            }
            else{
                automater.downloadFile(moodleFile, folder.getPath());
            }
        }
        if(folder.getMoodleFolders().size() > 0){
            for(MoodleFolder subfolder : folder.getMoodleFolders()){
                updateFiles(subfolder);
            }
        }
    }

    private MoodleFolder requestStructure(){
        MoodleFolder mainfolder = null;
        int attempts = 1;
        while (attempts != 20 && mainfolder == null){
            if(isCancelled())
                return null;
            Platform.runLater(new UpdateLabelRunnable(logMessage, "Connecting to server. Attempt " + attempts + "..."));
            try {mainfolder = automater.getCourseStructure();}catch (RuntimeException e){
                if(e.getMessage() != null && e.getMessage().startsWith("Error:")){
                    Platform.runLater(new UpdateLabelRunnable(logMessage, e.getMessage()));
                    break;
                }
                System.out.println("Connect attempt failed: ");
                System.out.println(e.getMessage());
            }
            attempts += (mainfolder == null) ? 1 : 0;
            if(attempts == 20)
                Platform.runLater(new UpdateLabelRunnable(logMessage, "Timeout, try again or contact server"));
        }
        return mainfolder;
    }

    protected void stopCourseProcess(Boolean isCleanExit){
        cancel();
        updateProgress(0, 1);
        if(isCleanExit)
            System.out.println("Clean exit");
        Platform.runLater(()->{
            Button startButton = (Button) HomeController.nodesMap.get(id+ "_startButton");
            startButton.setDisable(false);
            startButton.setText("Start");
            browseButton.setDisable(false);
            courseId.setDisable(false);
            folder.setDisable(false);
            type.setDisable(false);
            startButton.setOnAction(new MoodleStartEventHandler());
            if(isCleanExit)
                logMessage.setValue("Finished.");
        });
    }
    private MoodleFolder indexFiles(MoodleFolder folder){
        Platform.runLater(new UpdateLabelRunnable(logMessage, "Indexing files for: " + folder.getName()));
        int attempts = 1;
        while (attempts != 10){
            if(isCancelled())
                return null;
            try{
                folder = automater.getContentSection(folder);
                if(folder.getMoodleFolders().size() > 0)
                    for (MoodleFolder subFolder : folder.getMoodleFolders()){
                        if(isCancelled())
                            return null;
                        indexFiles(subFolder);
                    }
                break;
            } catch (Exception e){
                if(e instanceof IOException){
                    // Timeout most likely
                    attempts++;
                    Platform.runLater(new UpdateLabelRunnable(logMessage, "Indexing timed out. Attempt: " +attempts));
                    if(attempts == 10)
                        throw new RuntimeException("Error:\n Indexing timed out.");
                }
                else{
                    e.printStackTrace();
                    throw new RuntimeException("Unexpected error: " + e.getMessage());
                }
            }
        }
        return folder;
    }
    private MoodleFolder createFolders(MoodleFolder mainFolder, String path){
        // Creates the folders on the file system and gives each folder its full path.
        File directory = new File(path + mainFolder.getName());
        if(!directory.mkdir() && !directory.exists())
            throw new RuntimeException("Folders could not be created. Admin required.");
        mainFolder.setPath(path + mainFolder.getName());
        if(mainFolder.getMoodleFolders().size() > 0)
            for (MoodleFolder folder : mainFolder.getMoodleFolders()){
                folder = createFolders(folder, path + mainFolder.getName() + "/");
            }
        return mainFolder;
    }
    private void downloadFiles(MoodleFolder folder){
        //int totalFiles = getTotalFiles(folder);
        for(var moodleFile : folder.getMoodleFiles()){
            if(isCancelled())
                return;
            Platform.runLater(new UpdateLabelRunnable(logMessage, "Downloading file: " + moodleFile.getName()));
            automater.downloadFile(moodleFile, folder.getPath());
            updateProgress(fileCounter++, totalFiles);
        }
        if(folder.getMoodleFolders().size() > 0){
            for(MoodleFolder subfolder : folder.getMoodleFolders()){
                if(isCancelled())
                    return;
                downloadFiles(subfolder);
            }
        }
    }

    private int getTotalFiles(MoodleFolder folder) {
        int fileCount = 0;
        fileCount += folder.getMoodleFiles().size();
        if(folder.getMoodleFolders().size() > 0){
            for(MoodleFolder subfolder : folder.getMoodleFolders()){
                fileCount+= getTotalFiles(subfolder);
            }
        }
        return fileCount;
    }

    private void printFolders(MoodleFolder folder){ // test method todo: delete later.
        System.out.println(folder.getName() + ": " + folder.getPath());
        if(folder.getMoodleFolders().size() > 0){
            System.out.println("Subfolder of: " + folder.getName());
            for(MoodleFolder subfolder : folder.getMoodleFolders()){
                printFolders(subfolder);
            }
        }
    }
}
