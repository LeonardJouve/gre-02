package ch.heig.gre.groupQ;

import ch.heig.gre.graph.GridGraph2D;
import ch.heig.gre.graph.PositiveWeightFunction;
import ch.heig.gre.graph.VertexLabelling;
import ch.heig.gre.maze.GridMazeSolver;

import java.util.*;

public final class AStar implements GridMazeSolver {
  // Pair of cost and predecessor
  public record DistPre(int cost, Integer pred) {}

  // Used as a priority queue entry to store the priority and the vertex
  public record VertexPriority(int vertex, int prio) {}

  // Pair of 2D coordinates
  public record Coordinate(int x, int y){}

  // Interface used to generalize heuristic implementations usage
  interface HeuristicFunction {
    int get(int source, int destination, GridGraph2D grid, int minWeight);
  }

  public enum Heuristic {
    DIJKSTRA,
    INFINITY_NORM,
    EUCLIDEAN_NORM,
    MANHATTAN,
    K_MANHATTAN
  }

  /** Heuristique utilisée pour l'algorithme A*. */
  private final Heuristic heuristic;

  /** Facteur multiplicatif de la distance de Manhattan utilisé par l'heuristique K-Manhattan. */
  private final int kManhattan;

  // Function used to get the heuristic result
  private final HeuristicFunction heuristicFunction;

  public AStar(Heuristic heuristic) {
    this(heuristic, 1);
  }

  public AStar(Heuristic heuristic, int kManhattan) {
    this.heuristic = heuristic;
    this.kManhattan = kManhattan;

    // Set appropriate heuristic implementation
    switch (this.heuristic) {
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
    return kManhattan * manhattan(source, destination, grid, minWeight);
  }

  // Convert a vertex index to a pair of 2D coordinates
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
    // Total number of vertex treated
    int treated = 0;

    // Priority queue used by AStar using VertexPriority::prio as priority
    PriorityQueue<VertexPriority> prioQueue = new PriorityQueue<>(1, Comparator.comparingInt(VertexPriority::prio));

    // Since we have to initialize a distance for each vertex anyway, we store an array.
    ArrayList<DistPre> distancesToDest = new ArrayList<>();
    for (int i = 0 ; i < grid.nbVertices(); i++) {
      // Initialize distances to infinity
      distancesToDest.add(new DistPre(Integer.MAX_VALUE, null));
    }

    // Set source distance to 0
    distancesToDest.set(source, new DistPre(0, null));

    // Use a map as we should not calculate the heuristic for every vertices
    Map<Integer, Integer> heuristicResults = new HashMap<>();

    // Calculate source heuristic
    heuristicResults.put(source, this.heuristicFunction.get(source, destination, grid, weights.minWeight()));

    // Add the source to the priority queue
    prioQueue.add(new VertexPriority(source, heuristicResults.get(source)));

    // Treat each "I" vertex
    while (!prioQueue.isEmpty()) {
      // Poll vertex the with highest priority
      int currentVertex = prioQueue.poll().vertex();
      processed.setLabel(currentVertex, true);

      // If we found the destination,
      if (currentVertex == destination) {
        // we start to recreate the path from the destination
        List<Integer> path = new LinkedList<>();
        while (currentVertex != source) {
          // while we're not at the source, we add the current vertex at beginning of the path, so it's in good order
          path.addFirst(currentVertex);
          currentVertex = distancesToDest.get(currentVertex).pred();
        }
        // We end by adding the source to the path, by returning a result object
        path.addFirst(source);

        return new Result(path, path.size(), treated);
      }

      // We are starting to treat a new vertex, so increment total vertices treated
      ++treated;

      // Getting the currently treated vertex's neighbors
      List<Integer> neighbors = grid.neighbors(currentVertex);

      // For each neighbor "J"
      for (Integer neighborJ : neighbors) {
        // current cost to "J"
        int deltaJ = distancesToDest.get(neighborJ).cost();

        // current cost to "I"
        int deltaI = distancesToDest.get(currentVertex).cost();

        // weight of the edge from "I" to "J"
        int weight = weights.get(currentVertex, neighborJ);

        // calculate potential new weight from "I" to "J"
        int newDeltaJ = deltaI + weight;

        // if the weight improved
        if (newDeltaJ < deltaJ) {
          // if the heuristic result has not been calculated yet from "J"
          if (deltaJ == Integer.MAX_VALUE) {
            // calculate it
            heuristicResults.put(neighborJ, this.heuristicFunction.get(neighborJ, destination, grid, weights.minWeight()));
          }

          // update "J"'s distance and predecessor
          distancesToDest.set(neighborJ, new DistPre(newDeltaJ, currentVertex));

          int priority = newDeltaJ + heuristicResults.get(neighborJ);

          // If the vertex is already in the priority queue, remove it, since it will be added
          prioQueue.removeIf(vertexPriority -> vertexPriority.vertex == neighborJ);

          // insert neighbor to priority queue.
          prioQueue.add(new VertexPriority(neighborJ, priority));
        }
      }
    }
    // If we treated every vertex available from the source, but didn't find the destination, it means that no
    // path to destination is available from the source.
    return new Result(Collections.emptyList(), 0, 0);
  }
}