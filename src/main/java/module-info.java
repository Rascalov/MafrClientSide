module org.example {
    requires javafx.controls;
    requires javafx.fxml;
    requires java.logging;
    requires org.jsoup;
    requires java.prefs;

    opens org.mafr to javafx.fxml;
    exports org.mafr;
}