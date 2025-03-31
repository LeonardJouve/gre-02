package ch.heig.gre.gui.impl;

import ch.heig.gre.graph.GridGraph2D;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;
import javafx.scene.image.PixelBuffer;
import javafx.scene.image.PixelFormat;
import javafx.scene.image.WritableImage;
import javafx.scene.paint.Color;

import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import java.nio.IntBuffer;
import java.util.function.IntFunction;

public final class MazePainter {

  private final GraphicsContext context;
  private GridGraph2D maze;
  private IntFunction<Color> cellColorF = v -> Color.WHITE;

  // Techniquement épaisseur d'un demi mur
  private int wallThickness = 1;
  private int cellSide = 100;
  private Color wallColor = Color.BLACK;

  public MazePainter(GridGraph2D maze, GraphicsContext context) {
    this.maze = maze;
    this.context = context;
  }

  public void drawWall(int u, int v) {
    drawWall(u, v, null);
  }

  private void drawWall(int u, int v, BufferedImage buffer) {
    if (! maze.areAdjacent(u, v)) {
      drawWall(u, v, wallColor, wallColor, buffer);
    } else {
      drawWall(u, v, cellColorF.apply(u), cellColorF.apply(v), buffer);
    }
  }

  private void drawWall(int u, int v, Color uColor, Color vColor, BufferedImage buffer) {
    int xu = col(u);
    int yu = row(u);
    int xv = col(v);
    int yv = row(v);

    // Swap des couleurs pour correspondre à l'ordre d'affichage (haut -> bas ou gauche -> droite)
    if (xv < xu || yv < yu) {
      Color tmp = uColor;
      uColor = vColor;
      vColor = tmp;
    }

    // Coordonnées du bord supérieur gauche du rectangle formée par les deux cellules
    int x = cellOffset(Math.min(xu, xv));
    int y = cellOffset(Math.min(yu, yv));

    if (xu == xv) {
      // Séparation horizontale
      drawRect(x, y + cellSide, cellSide, wallThickness, uColor, buffer);
      drawRect(x, y + cellSide + wallThickness, cellSide, wallThickness, vColor, buffer);
    } else {
      // Séparation verticale
      drawRect(x + cellSide, y, wallThickness, cellSide, uColor, buffer);
      drawRect(x + cellSide + wallThickness, y, wallThickness, cellSide, vColor, buffer);
    }
  }

  private void drawRect(int x, int y, int width, int height, Color color, BufferedImage buffer) {
    if (buffer == null) {
      context.setFill(color);
      context.fillRect(x, y, width, height);
      return;
    }

    var g = buffer.getGraphics();
    g.setColor(toAwtColor(color));
    g.fillRect(x, y, width, height);
  }

  public void drawCell(int v) {
    drawCell(v, null);
  }

  private void drawCell(int v, BufferedImage buffer) {
    drawRect(cellOffset(col(v)), cellOffset(row(v)), cellSide, cellSide, cellColorF.apply(v), buffer);

    for(int u : maze.neighbors(v)) {
      drawWall(u, v, buffer);
    }
  }

  public void repaint() {
    // Hack pour contourner le problème de dessin direct sur le canvas, qui peut se révéler très lent
    BufferedImage buffer = new BufferedImage(
        (int) context.getCanvas().getWidth(),
        (int) context.getCanvas().getHeight(),
        BufferedImage.TYPE_INT_ARGB_PRE);

    // Arrière-plan
    var g = buffer.getGraphics();
    g.setColor(toAwtColor(wallColor));
    g.fillRect(0, 0, buffer.getWidth(), buffer.getHeight());

    // Cases et murs
    for (int u = 0; u < maze.nbVertices(); ++u) {
      drawCell(u, buffer);
      for (int v : maze.neighbors(u)) {
        if (u > v)
          drawWall(u, v, buffer);
      }
    }

    context.drawImage(toFXImage(buffer), 0, 0);
  }

  // Getters

  public int getCellSide() {
    return cellSide;
  }

  public int getWallThickness() {
    return wallThickness;
  }

  // Fluent setters

  public MazePainter setMaze(GridGraph2D maze) {
    this.maze = maze;
    return this;
  }

  public MazePainter setCellColorF(IntFunction<Color> cellColorF) {
    this.cellColorF = cellColorF;
    return this;
  }

  public MazePainter setWallColor(Color wallColor) {
    this.wallColor = wallColor;
    return this;
  }

  public MazePainter setWallThickness(int wallThickness) {
    this.wallThickness = wallThickness;
    return this;
  }

  public MazePainter setCellSide(int cellSide) {
    this.cellSide = cellSide;
    return this;
  }

  // Helpers

  public int cellOffset(int pos) {
    return 2 * wallThickness + pos * (cellSide + 2 * wallThickness);
  }

  private int row(int v) {
    return v / maze.width();
  }

  private int col(int v) {
    return v % maze.width();
  }

  private static java.awt.Color toAwtColor(Color color) {
    return new java.awt.Color((float) color.getRed(), (float) color.getGreen(), (float) color.getBlue(), (float) color.getOpacity());
  }

  // https://stackoverflow.com/a/75703543
  private static Image toFXImage(BufferedImage img) {
    //converting the BufferedImage to an IntBuffer
    int[] type_int_agrb = ((DataBufferInt) img.getRaster().getDataBuffer()).getData();
    IntBuffer buffer = IntBuffer.wrap(type_int_agrb);

    //converting the IntBuffer to an Image, read more about it here: https://openjfx.io/javadoc/13/javafx.graphics/javafx/scene/image/PixelBuffer.html
    PixelFormat<IntBuffer> pixelFormat = PixelFormat.getIntArgbPreInstance();
    PixelBuffer<IntBuffer> pixelBuffer = new PixelBuffer(img.getWidth(), img.getHeight(), buffer, pixelFormat);
    return new WritableImage(pixelBuffer);
  }
}


