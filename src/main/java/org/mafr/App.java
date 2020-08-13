package org.mafr;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.mafr.model.MoodleFolder;
import org.mafr.service.MoodleAutomater;

import java.io.IOException;

/**
 * JavaFX App
 */
public class App extends Application {

    private static Stage stage;
    private static Scene scene;

    public static Stage getStage() {
        return stage;
    }
    @Override
    public void start(Stage stage) throws IOException {
        this.stage = stage;
        stage.getIcons().add(new Image(getClass().getClassLoader().getResourceAsStream("icon.png")));
        scene = new Scene(loadFXML("Home"));
        stage.setScene(scene);
        stage.show();
    }

    static void setRoot(String fxml) throws IOException {
        scene.setRoot(loadFXML(fxml));
    }

    private static Parent loadFXML(String fxml) throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(App.class.getResource(fxml + ".fxml"));
        return fxmlLoader.load();
    }

    public static void main(String[] args) { launch(); }
}