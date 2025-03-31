package ch.heig.gre.maze.impl;

import ch.heig.gre.graph.Edge;
import ch.heig.gre.graph.GridGraph;
import ch.heig.gre.graph.GridGraph2D;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.util.Arrays;
import java.util.List;
import java.util.Scanner;

// Format :
//  width
//  height
//  nbEdges
//  u1 v1
//  ...
//  un vn
//  w1
//  ...
//  wn
public final class MazeIO {
  private MazeIO() {
    throw new AssertionError();
  }

  public record WeightedMaze(GridGraph2D maze, int[] weights) {}

  public static void write(GridGraph2D maze, int[] weights, File file) {
    try (var writer = new BufferedWriter(new FileWriter(file))) {
      writer.write(maze.width() + "\n");
      writer.write(maze.height() + "\n");
      List<Edge> edges = maze.edges();
      writer.write(edges.size() + "\n");

      for (var edge : edges) {
        writer.write(edge.u() + " " + edge.v() + "\n");
      }

      for (int weight : weights) {
        writer.write(weight + "\n");
      }
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  public static void writeElevationMap(int width, int height, int[] weights, String filename) {
    // Equivalent JavaFX plus que douteux
    var buffer = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
    var stats = Arrays.stream(weights).summaryStatistics();
    int min = stats.getMin();
    int max = stats.getMax();

    for (int v = 0; v < width * height; v++) {
      int x = v % width;
      int y = v / width;
      // Dégradé de rouge (plus foncé = plus haut)
      int color = 255 - (weights[v] - min) * 255 / (max - min);
      buffer.setRGB(x, y, new Color(255, color, color).getRGB());
    }

    try {
      ImageIO.write(buffer, "png", new File(filename));
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  public static WeightedMaze read(File file) {
    try (var reader = new Scanner(new FileReader(file))) {
      int width = reader.nextInt();
      int height = reader.nextInt();
      GridGraph2D maze = new GridGraph(width, height);
      int nbEdges = reader.nextInt();

      for (int i = 0; i < nbEdges; i++) {
        int u = reader.nextInt();
        int v = reader.nextInt();
        maze.addEdge(u, v);
      }

      int[] weights = new int[maze.nbVertices()];
      for (int i = 0; i < maze.nbVertices(); i++) {
        weights[i] = reader.nextInt();
      }

      return new WeightedMaze(maze, weights);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }
}
