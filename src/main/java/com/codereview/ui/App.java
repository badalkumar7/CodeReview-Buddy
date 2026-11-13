package com.codereview.ui;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;
/** Starts the CodeReview Buddy desktop application. */
public class App extends Application {
    /** Creates the main dashboard window. */
    @Override public void start(Stage stage){MainDashboardView view=new MainDashboardView(stage);stage.setTitle("CodeReview Buddy");stage.setMinWidth(1050);stage.setMinHeight(680);stage.setScene(new Scene(view,1250,800));stage.show();}
    /** Launches JavaFX. */
    public static void main(String[] args){launch(args);}
}
