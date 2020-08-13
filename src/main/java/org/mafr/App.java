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

    public static void main(String[] args) {
        // todo: mafr Server seems to return a decode-able unicode (given you add % per u), but client gives ?????'s all the time.
        System.out.println("u0627u0635u0648u0644 u0627u0644u062du062fu064au062b");
        System.out.println("<a href=\"https://moodle.inholland.nl/pluginfile.php/303016/mod_page/intro/u0627u0635u0648u0644 u0627u0644u062du062fu064au062b.docx\" target=\"_blank\" alt=\"\">u0627u0635u0648u0644 u0627u0644u062du062fu064au062b.docx</a>");
        Document document = Jsoup.parse("<a href=\"https://moodle.inholland.nl/pluginfile.php/303016/mod_page/intro/u0627u0635u0648u0644 u0627u0644u062du062fu064au062b.docx\" target=\"_blank\" alt=\"\">u0627u0635u0648u0644 u0627u0644u062du062fu064au062b.docx</a>");
        System.out.println(document.html());
        document = Jsoup.parse("<a href=\\\"https:\\/\\/moodle.inholland.nl\\/pluginfile.php\\/303016\\/mod_page\\/intro\\/\\u0627\\u0635\\u0648\\u0644 \\u0627\\u0644\\u062d\\u062f\\u064a\\u062b.docx\\\" target=\\\"_blank\\\" alt=\\\"\\\">\\u0627\\u0635\\u0648\\u0644 \\u0627\\u0644\\u062d\\u062f\\u064a\\u062b.docx<\\/a>");
        System.out.println(document.html());
        System.out.println("اصول الحديث");
        MoodleAutomater automater = new MoodleAutomater(6161, "https://damp-lowlands-42720.herokuapp.com", "inhollandFree");
        try {
            automater.getContentSection(new MoodleFolder("", 11));
        } catch (IOException e) {
            e.printStackTrace();
        }

        launch();
    }
}