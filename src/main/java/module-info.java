module ch.heig.gre {
  requires javafx.controls;
  requires javafx.fxml;
  requires javafx.graphics;
  requires java.desktop;

  opens ch.heig.gre.gui to javafx.fxml;
  opens ch.heig.gre.groupX to javafx.graphics;
  opens ch.heig.gre.maze.impl;
}
