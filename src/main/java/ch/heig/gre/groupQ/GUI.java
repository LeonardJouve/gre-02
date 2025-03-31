package ch.heig.gre.groupQ;

import ch.heig.gre.gui.ChoiceItem;
import ch.heig.gre.gui.MainViewController;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.List;

public class GUI extends Application {
  @Override
  public void start(Stage stage) throws IOException {
    FXMLLoader fxmlLoader = new FXMLLoader(MainViewController.class.getResource("mainView.fxml"));
    Parent parent = fxmlLoader.load();
    Scene scene = new Scene(parent, 1200, 950);
    stage.setTitle("Redrum");
    stage.setScene(scene);

    MainViewController controller = fxmlLoader.getController();
    controller.init(new DfsGenerator(), List.of(
        new ChoiceItem<>("Dijkstra", new AStar(AStar.Heuristic.DIJKSTRA)),
        new ChoiceItem<>("A* - Norme infinie", new AStar(AStar.Heuristic.INFINITY_NORM)),
        new ChoiceItem<>("A* - Norme euclidienne", new AStar(AStar.Heuristic.EUCLIDEAN_NORM)),
        new ChoiceItem<>("A* - Manhattan", new AStar(AStar.Heuristic.MANHATTAN)),
        new ChoiceItem<>("A* - K Manhattan", new AStar(AStar.Heuristic.K_MANHATTAN, 5))
    ));

    stage.show();
  }

  public static void main(String[] args) {
    launch();
  }
}
