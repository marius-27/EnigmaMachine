module com.example.enigmamachine {
    requires com.fasterxml.jackson.databind;
    requires javafx.controls;

    exports com.example.enigmamachine;

    // Jackson reads and writes the saved file format by reflection.
    opens com.example.enigmamachine.persistence to com.fasterxml.jackson.databind;
}
