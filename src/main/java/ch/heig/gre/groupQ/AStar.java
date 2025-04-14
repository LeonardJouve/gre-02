package ch.heig.gre.groupQ;

import ch.heig.gre.graph.GridGraph2D;
import ch.heig.gre.graph.PositiveWeightFunction;
import ch.heig.gre.graph.VertexLabelling;
import ch.heig.gre.maze.GridMazeSolver;
import jdk.jshell.spi.ExecutionControl;

import java.util.*;


public final class AStar implements GridMazeSolver {
  public record DistPre(int cost, Integer pred) {}
  public record VertexPriority(int vertex, int prio) {}

  interface Implementation {
    int implementation(int source, int destination, GridGraph2D grid);
  }

  public enum Heuristic {
    DIJKSTRA(AStar::djikstra),
    INFINITY_NORM(AStar::infinityNorm),
    EUCLIDEAN_NORM(AStar::euclidianNorm),
    MANHATTAN(AStar::manhattan),
    K_MANHATTAN(AStar::kManhattan),;

    private final Implementation implementation;
    Heuristic(Implementation implementation) {
      this.implementation = implementation;
    }

    Implementation getImplementation() {
      return implementation;
    }
  }

  /** Heuristique utilisée pour l'algorithme A*. */
  private final Heuristic heuristic;

  /** Facteur multiplicatif de la distance de Manhattan utilisé par l'heuristique K-Manhattan. */
  private final int kManhattan;

  public AStar(Heuristic heuristic) {
    this(heuristic, 1);
  }

  public AStar(Heuristic heuristic, int kManhattan) {
    this.heuristic = heuristic;
    this.kManhattan = kManhattan;
  }

  static int djikstra(int source, int destination, GridGraph2D grid) {
    return 0;
  }

  static int infinityNorm(int source, int destination, GridGraph2D grid) {
    throw new UnsupportedOperationException();
  }

  static int euclidianNorm(int source, int destination, GridGraph2D grid) {
    throw new UnsupportedOperationException();
  }

  static int manhattan(int source, int destination, GridGraph2D grid) {
    throw new UnsupportedOperationException();
  }

  static int kManhattan(int source, int destination, GridGraph2D grid) {
    throw new UnsupportedOperationException();
  }

  @Override
  public Result solve(GridGraph2D grid,
                      PositiveWeightFunction weights,
                      int source,
                      int destination,
                      VertexLabelling<Boolean> processed) {
    // TODO vertex labelling
    int treated = 0;
    PriorityQueue<VertexPriority> prioQueue = new PriorityQueue<>(1, Comparator.comparingInt(VertexPriority::prio));

    // Initialiser les distPre à maxValue, null sauf pour la source
    // Map ?
    // ArrayList<DistPre> distancesToDest = new ArrayList<>();

    // On prend des maps parce que le but est justement de ne pas traiter tous les sommets
    Map<Integer, DistPre> distancesToDest = new HashMap<>();

    Map<Integer, Integer> heuristicResults = new HashMap<>();

    for (int i = 0 ; i < grid.nbVertices(); i++) {
      distancesToDest.put(i, new DistPre(Integer.MAX_VALUE, null));
    }
    distancesToDest.put(source, new DistPre(0, null));

    heuristicResults.put(source, this.heuristic.getImplementation().implementation(source, destination, grid));
    prioQueue.add(new VertexPriority(source, heuristicResults.get(source)));

    while (!prioQueue.isEmpty()) {
      ++treated;
      Integer currentVertex = prioQueue.poll().vertex();
      if (currentVertex == destination) {
        List<Integer> path = new LinkedList<>();
        while (currentVertex != source) {
          path.addFirst(currentVertex);
          currentVertex= distancesToDest.get(currentVertex).pred();
        }
        path.addFirst(source);

        return new Result(path, path.size(), treated);
      }

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
            heuristicResults.put(neighborJ, this.heuristic.getImplementation().implementation(neighborJ, destination, grid));
          }
          distancesToDest.put(neighborJ, new DistPre(newDeltaJ, currentVertex));

          int priority = newDeltaJ + heuristicResults.get(neighborJ);
          prioQueue.add(new VertexPriority(neighborJ, priority));
        }
      }
    }

    return null;
  }
}