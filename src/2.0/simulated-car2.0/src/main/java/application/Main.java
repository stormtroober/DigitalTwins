package application;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

/**
 * This class represent the Main class of the JavaFX-based application.
 */
public final class Main extends Application {

    private static final int SCENE_WIDTH = 720;
    private static final int SCENE_HEIGHT = 576;

    @Override
    public void start(final Stage stage) throws Exception {
        final Parent root = FXMLLoader.load(ClassLoader.getSystemResource("layouts/car.fxml"));
        final Scene scene = new Scene(root, SCENE_WIDTH, SCENE_HEIGHT);
        // Stage configuration
        stage.setTitle("Simulated Car");
        stage.setScene(scene);
        stage.show();
    }

    /**
     * 
     * @param args unused
     */
    public static void main(final String[] args) {
        launch();
    }

}
