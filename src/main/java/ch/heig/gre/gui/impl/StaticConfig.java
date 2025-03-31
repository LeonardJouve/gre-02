package ch.heig.gre.gui.impl;

import ch.heig.gre.graph.VertexLabelling;
import ch.heig.gre.graph.PositiveWeightFunction;
import javafx.scene.paint.Color;

public final class StaticConfig {
  private StaticConfig(){}

  public static int startPoint(ObservableMaze graph) {
    return 0;
  }

  public static int wallThickness(int cellSide) {
    return Math.max(cellSide / 20, 1);
  }

  public static Color wallColor() {
    return Color.BLACK;
  }

  public static int minWeight() {
    return 5;
  }

  public static int maxWeight() {
    return 25;
  }

  public static Color generatorColor(ObservableMaze maze, int v) {
    return switch(maze.getLabel(v)) {
      case PROCESSED -> Color.WHITE;
      case PENDING -> Color.BLACK;
      case PROCESSING -> Color.RED;
    };
  }

  public static Color tuningColor(int[] weights, int v) {
    double value = (weights[v] - minWeight()) / (double) (maxWeight() - minWeight());
    return Color.WHITE.interpolate(Color.DARKGREEN, value);
  }

  public static Color solverCellColor(int[] weights,
                                      VertexLabelling<Boolean> processed,
                                      int[] pathPredecessors,
                                      PositiveWeightFunction wf,
                                      int v) {

    if (pathPredecessors[v] != -1) {
      double value = (wf.get(pathPredecessors[v], v) - minWeight())
          / (double) (maxWeight() - minWeight());
      return Color.NAVAJOWHITE.interpolate(Color.DARKRED, value);
    }

    if (processed.getLabel(v)) {
      double value = (weights[v] - minWeight()) / (double) (maxWeight() - minWeight());
      return Color.PALETURQUOISE.interpolate(Color.DARKSLATEGRAY, value);
    }

    return StaticConfig.tuningColor(weights, v);
  }
}
