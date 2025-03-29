module org.example.fina {
    requires javafx.controls;
    requires javafx.fxml;
    requires java.sql;
    requires mysql.connector.j;


    opens org.example.fina to javafx.fxml;
    exports org.example.fina;
}