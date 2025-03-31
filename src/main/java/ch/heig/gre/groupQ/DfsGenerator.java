package ch.heig.gre.groupQ;

import ch.heig.gre.maze.MazeBuilder;
import ch.heig.gre.maze.MazeGenerator;
import ch.heig.gre.maze.Progression;

import java.util.*;

public final class DfsGenerator implements MazeGenerator {
  @Override
  public void generate(MazeBuilder builder, int from) {
    // We verify the arguments
    if (builder == null) throw new NullPointerException("Builder missing");
    if (!builder.topology().vertexExists(from)) throw new IllegalArgumentException("Non-existent source node");

    // initiate a random number generator
    Random rand = new Random();
    Stack<Integer> stack = new Stack<>();
    // visitedVertices will represent for each vertex whether it was already (or not) visited by the DFS algorithm.
    List<Boolean> visitedVertices = new ArrayList<>(Collections.nCopies(builder.topology().nbVertices(), false));

    // we add the starting vertex to the stack, and count it as visited to enter the while loop
    stack.push(from);
    visitedVertices.set(from, true);

    while (!stack.isEmpty()) {
      // we get the first available vertex on the stack, and mark that it's being processed
      Integer currentVertex = stack.peek();
      builder.progressions().setLabel(currentVertex, Progression.PROCESSING);

      // we get a list of the neighbours of the currently treated vertex, and remove the already visited vertices from it
      List<Integer> neighbours = builder.topology().neighbors(currentVertex);
      neighbours.removeIf(visitedVertices::get);

      if (neighbours.isEmpty()) {
        // if after filtering, the neighbors list is empty, it means that every neighbor of the vertex was already treated
        // therefore, we can mark that processing on the vertex is finished, and remove it from the stack
        stack.pop();
        builder.progressions().setLabel(currentVertex, Progression.PROCESSED);
        continue;
      }
      // after filtering the list, we randomly choose one of it's neighbors, and push it on the stack to process it
      // afterward
      Integer neighbour = neighbours.get(rand.nextInt(neighbours.size()));
      stack.push(neighbour);
      // we break the wall between the current vertex and it's chosen neighbor, and mark the neighbor as visited to
      // prevent breaking another wall toward it
      builder.removeWall(currentVertex, neighbour);
      visitedVertices.set(neighbour, true);
    }
  }

  @Override
  public boolean requireWalls() {
      return true;
  }
}
