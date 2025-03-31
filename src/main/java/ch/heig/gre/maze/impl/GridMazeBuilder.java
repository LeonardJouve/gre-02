package ch.heig.gre.maze.impl;

import ch.heig.gre.graph.Graph;
import ch.heig.gre.graph.GridGraph2D;
import ch.heig.gre.graph.VertexLabelling;
import ch.heig.gre.maze.MazeBuilder;
import ch.heig.gre.maze.Progression;

import java.util.Arrays;

/**
 * Implémentation basique de {@link MazeBuilder} pour un labyrinthe en forme de grille.
 */
public final class GridMazeBuilder implements MazeBuilder {
  private final Graph topology;
  private final GridGraph2D maze;
  private final ProgressionLabelling progressions;

  public GridMazeBuilder(Graph topology, GridGraph2D maze) {
    this.topology = topology;
    this.maze = maze;
    this.progressions = new ProgressionLabelling(topology.nbVertices());
  }

  @Override
  public Graph topology() {
    return topology;
  }

  @Override
  public VertexLabelling<Progression> progressions() {
    return progressions;
  }

  @Override
  public void addWall(int u, int v) {
    maze.removeEdge(u, v);
  }

  @Override
  public void removeWall(int u, int v) {
    maze.addEdge(u, v);
  }

  private static final class ProgressionLabelling implements VertexLabelling<Progression> {
    private final Progression[] labels;

    public ProgressionLabelling(int nbVertices) {
      labels = new Progression[nbVertices];
      Arrays.fill(labels, Progression.PENDING);
    }

    @Override
    public Progression getLabel(int vertex) {
      return labels[vertex];
    }

    @Override
    public void setLabel(int vertex, Progression label) {
      labels[vertex] = label;
    }
  }
}
