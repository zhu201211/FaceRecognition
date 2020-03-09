package application;
	
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.stage.Stage;
import javafx.scene.Parent;
import javafx.scene.Scene;


public class Main extends Application {
	@Override
	public void start(Stage primaryStage) {
		try {
		    	FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("myscene.fxml"));
			Parent root = fxmlLoader.load();
			primaryStage.setScene(new Scene(root));
			primaryStage.setTitle("课程设计-图像处理");
			primaryStage.show();
		} catch(Exception e) {
			e.printStackTrace();
		}
	}
	
	public static void main(String[] args) {
		launch(args);
	}
}
