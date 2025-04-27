package ch.heig.gre.groupQ;

import ch.heig.gre.graph.Edge;
import ch.heig.gre.graph.GridGraph2D;
import ch.heig.gre.graph.PositiveWeightFunction;
import ch.heig.gre.graph.VertexLabelling;
import ch.heig.gre.maze.GridMazeSolver;
import jdk.jshell.spi.ExecutionControl;

import java.util.*;


public final class AStar implements GridMazeSolver {
  public record DistPre(int cost, Integer pred) {}
  public record VertexPriority(int vertex, int prio) {}
  public record Coordinate(int x, int y){}
  interface HeuristicFunction {
    int get(int source, int destination, GridGraph2D grid, int minWeight);
  }

  public enum Heuristic {
    DIJKSTRA,
    INFINITY_NORM,
    EUCLIDEAN_NORM,
    MANHATTAN,
    K_MANHATTAN;
  }

  /** Heuristique utilisée pour l'algorithme A*. */
  private final Heuristic heuristic;

  /** Facteur multiplicatif de la distance de Manhattan utilisé par l'heuristique K-Manhattan. */
  private final int kManhattan;

  private final HeuristicFunction heuristicFunction;

  public AStar(Heuristic heuristic) {
    this(heuristic, 1);
  }

  public AStar(Heuristic heuristic, int kManhattan) {
    this.heuristic = heuristic;
    this.kManhattan = kManhattan;

    switch (this.heuristic) {
    case DIJKSTRA:
      this.heuristicFunction = this::djikstra;
      break;
    case INFINITY_NORM:
      this.heuristicFunction = this::infinityNorm;
      break;
    case EUCLIDEAN_NORM:
      this.heuristicFunction = this::euclidianNorm;
      break;
    case MANHATTAN:
      this.heuristicFunction = this::manhattan;
      break;
    case K_MANHATTAN:
      this.heuristicFunction = this::kManhattanHeur;
      break;
    default:
      this.heuristicFunction = this::djikstra;
    }

  }

  int djikstra(int source, int destination, GridGraph2D grid, int minWeight) {
    return 0;
  }

  int infinityNorm(int source, int destination, GridGraph2D grid, int minWeight) {
    Coordinate sourceCoord = getCoordinate(grid, source);
    Coordinate destinationCoord = getCoordinate(grid, destination);
    int deltaX = Math.abs(destinationCoord.x() - sourceCoord.x());
    int deltaY = Math.abs(destinationCoord.y() - sourceCoord.y());

    return minWeight * Math.max(deltaX, deltaY);
  }

  int euclidianNorm(int source, int destination, GridGraph2D grid, int minWeight) {
    Coordinate sourceCoord = getCoordinate(grid, source);
    Coordinate destinationCoord = getCoordinate(grid, destination);
    int x = destinationCoord.x() - sourceCoord.x();
    int y = destinationCoord.y() - sourceCoord.y();

    return minWeight * (int) Math.floor(Math.hypot(x, y));
  }

  int manhattan(int source, int destination, GridGraph2D grid, int minWeight) {
    Coordinate sourceCoord = getCoordinate(grid, source);
    Coordinate destinationCoord = getCoordinate(grid, destination);

    return minWeight * Math.abs(sourceCoord.x() - destinationCoord.x()) + Math.abs(sourceCoord.y() - destinationCoord.y());
  }

  int kManhattanHeur(int source, int destination, GridGraph2D grid, int minWeight) {
    return minWeight * kManhattan * manhattan(source, destination, grid, minWeight);
  }

  private static Coordinate getCoordinate(GridGraph2D grid, int vertex) {
    int width = grid.width();
    return new Coordinate(vertex % width, vertex / width);
  }

  @Override
  public Result solve(GridGraph2D grid,
                      PositiveWeightFunction weights,
                      int source,
                      int destination,
                      VertexLabelling<Boolean> processed) {
    int treated = 0;

    PriorityQueue<VertexPriority> prioQueue = new PriorityQueue<>(1, Comparator.comparingInt(VertexPriority::prio));

    // Initialiser les distPre à maxValue, null sauf pour la source
    // Map ?
    // ArrayList<DistPre> distancesToDest = new ArrayList<>();

    // On prend des maps parce que le but est justement de ne pas traiter tous les sommets
    // TODO: au final on prend tout les sommets
    Map<Integer, DistPre> distancesToDest = new HashMap<>();
    for (int i = 0 ; i < grid.nbVertices(); i++) {
      distancesToDest.put(i, new DistPre(Integer.MAX_VALUE, null));
    }
    distancesToDest.put(source, new DistPre(0, null));

    Map<Integer, Integer> heuristicResults = new HashMap<>();
    heuristicResults.put(source, this.heuristicFunction.get(source, destination, grid, weights.minWeight()));

    prioQueue.add(new VertexPriority(source, heuristicResults.get(source)));

    while (!prioQueue.isEmpty()) {
      int currentVertex = prioQueue.poll().vertex();
      processed.setLabel(currentVertex, true);

      if (currentVertex == destination) {
        List<Integer> path = new LinkedList<>();
        while (currentVertex != source) {
          path.addFirst(currentVertex);
          currentVertex= distancesToDest.get(currentVertex).pred();
        }
        path.addFirst(source);

        return new Result(path, path.size(), treated);
      }

      ++treated;

      // prendre les successeurs
      List<Integer> neighbors = grid.neighbors(currentVertex);
      for (Integer neighborJ : neighbors) {
        int deltaJ = distancesToDest.get(neighborJ).cost();
        int deltaI = distancesToDest.get(currentVertex).cost();
        int weight = weights.get(currentVertex, neighborJ);
        int newDeltaJ = deltaI + weight;

        if (deltaJ > newDeltaJ){
          // ATTENTION OVERFLOW ?
          if (deltaJ == Integer.MAX_VALUE) {
            heuristicResults.put(neighborJ, this.heuristicFunction.get(neighborJ, destination, grid, weights.minWeight()));
          }
          distancesToDest.put(neighborJ, new DistPre(newDeltaJ, currentVertex));

          int priority = newDeltaJ + heuristicResults.get(neighborJ);
          prioQueue.add(new VertexPriority(neighborJ, priority));
        }
      }
    }

    return new Result(Collections.emptyList(), 0, 0);
  }
}