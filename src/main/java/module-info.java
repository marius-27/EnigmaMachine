module com.example.enigmamachine {
    requires javafx.controls;
    requires javafx.fxml;


    opens com.example.enigmamachine to javafx.fxml;
    exports com.example.enigmamachine;
}