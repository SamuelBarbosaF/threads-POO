package br.edu.datacenter;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

/**
 * ## main
**/

public class MainApp extends Application {
    @Override
    public void start(Stage primaryStage) throws Exception {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/br/edu/datacenter/view/MainView.fxml"));
        Scene scene = new Scene(loader.load(), 1024, 768);
        primaryStage.setTitle("Data Center - Escalonador de Tarefas");
        primaryStage.setScene(scene);
        primaryStage.show();
    }
    public static void main(String[] args) { launch(args); }
}
