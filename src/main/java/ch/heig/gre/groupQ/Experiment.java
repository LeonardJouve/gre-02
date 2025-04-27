package ch.heig.gre.groupQ;

import ch.heig.gre.graph.GridGraph;
import ch.heig.gre.graph.GridGraph2D;
import ch.heig.gre.graph.PositiveWeightFunction;
import ch.heig.gre.maze.BoolVertexLabelling;
import ch.heig.gre.maze.GridMazeSolver;
import ch.heig.gre.maze.MazeBuilder;
import ch.heig.gre.maze.MazeGenerator;
import ch.heig.gre.maze.impl.GridMazeBuilder;
import ch.heig.gre.maze.impl.MazeTuner;
import ch.heig.gre.maze.impl.ShenaniganWeightFunction;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.function.Function;
import java.util.random.RandomGenerator;

public final class Experiment {
  /** Dimension de la grille (carrée) */
  private static final int SIDE = 1100;

  /** Sommets source et destination pour les expériences */
  private static final int SRC = 550500;
  private static final int DST = 660600;

  /** Nombre de grilles à générer pour chaque expérience */
  private static final int N = 100;

  /** Topologie de la grille */
  private static final GridGraph2D TOPOLOGY;

  static {
    var g = new GridGraph(SIDE);
    GridGraph.bindAll(g);
    TOPOLOGY = g;
  }

  /** Paramètres des expériences à réaliser */
  private static final Params[] PARAMS = {
      new Params(
          "Relief très peu dense, labyrinthe très ouvert",
          new double[]{0, 0.15, 20, 1, 20}),
      new Params(
          "Relief très peu dense, labyrinthe assez ouvert",
          new double[]{0, 0.1, 20, 1, 20}),
      new Params(
          "Relief très peu dense, labyrinthe peu ouvert",
          new double[]{0, 0.01, 20, 1, 20}),
      new Params(
          "Relief dense, labyrinthe moyennement ouvert",
          new double[]{0.25, 0.05, 25, 5, 20}),
      new Params(
          "Relief très dense, labyrinthe moyennement ouvert",
          new double[]{0.5, 0.05, 25, 5, 20}),
      new Params(
          "Relief très dense et fortement pondéré, labyrinthe moyennement ouvert",
          new double[]{0.5, 0.05, 25, 5, 100})
  };

  /**
   * <p>Paramètres d'une expérience, avec une description approximative de leurs effets sur la génération.</p>
   *
   * <p>À passer en paramètre de la méthode {@link #generateGrid} pour générer un labyrinthe.</p>
   *
   * @param description Description de l'expérience
   * @param parameters  Paramètres de l'expérience
   */
  record Params(String description, double[] parameters) {}


  /**
   * Custom function.
   * Computes the average of a list of values.
   * @param values values to computes the average on.
   * @param extract function to extract an integer value from T
   * @return the computed output
   * @param <T> generic type of the values the average will be computed on
   */
  private static <T> double getAverage(List<T> values, Function<T, Integer> extract) {
    int sum = 0;
    for (T value : values) {
      sum += extract.apply(value);
    }

    return (double) sum / values.size();
  }

  /**
   * Writes headers in a csv file
   * @param filename file name of the file to write in.
   * @param headers every header to write in the file.
   */
  public static void writeCsvHeaders(String filename, String... headers) {
    try (FileWriter fileWriter = new FileWriter(filename, true);
         PrintWriter printWriter = new PrintWriter(fileWriter)) {
      for (int i = 0; i < headers.length; i++) {
        if (i > 0) {
          printWriter.print(",");
        }
        printWriter.print("\"" + headers[i] + "\"");
      }
      printWriter.print('\n');
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  /**
   * Add a csv line to a csv file.
   * @param filename the name of the file.
   * @param line the already formatted line as a string.
   */
  public static void addCsvLine(String filename, String line) {
    try (FileWriter fileWriter = new FileWriter(filename, true);
         PrintWriter printWriter = new PrintWriter(fileWriter)) {
      printWriter.println(line);
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  public static void main(String[] args) {
    MazeGenerator mazeGenerator = new DfsGenerator();
    Random randomGenerator = new Random();

    // Setting a seed, so that it always give the same output
    randomGenerator.setSeed(2012);

    try {
      // Remove and create a csv to save results
      Files.deleteIfExists(Paths.get("stats.csv"));
      writeCsvHeaders("stats.csv", "Experiment name", "Heuristic", "Average length", "Average treatment", "Average Treatment Percentage Decrease");

      // Remove and create a csv to save detailed k-manhattan results
      Files.deleteIfExists(Paths.get("k_manhattan_stats.csv"));
      writeCsvHeaders("k_manhattan_stats.csv", "Experiment name", "Kvalue", "Optimal solution percentage", "Min error treatments", "Max error treatments", "Mean error treatments", "Min error length", "Max error length", "Mean error length", "Gain moyen");
    } catch (IOException e) {
      e.printStackTrace();
    }

    // Loop on each experiment
    for (Params p : PARAMS) {
      System.out.println("\n\n\nNOUVELLE EXPERIENCE : " + p.description + "\n");

      // Initialize the results array for each heuristic except k-manhattan
      ArrayList<LinkedList<GridMazeSolver.Result>> results = new ArrayList<>();
      for (int i = 0; i < AStar.Heuristic.values().length - 1; ++i) {
        results.add(new LinkedList<>());
      }

      Map<Integer, LinkedList<GridMazeSolver.Result>> kManhattanResults = new HashMap<>();

      // Loop N times to get a result sample
      for (int i = 0; i < N; ++i) {
        System.out.println("N = " + i + ":");

        // Generate a grid
        GenerationResult generationResult = generateGrid(mazeGenerator, p.parameters(), randomGenerator);

        // Loop on each heuristic
        for (AStar.Heuristic heuristic : AStar.Heuristic.values()) {
          if (heuristic == AStar.Heuristic.K_MANHATTAN) {
            // Loop on different K values for k-manhattan
            for (int k = 2 ; k <= 8; ++k) {
              // Create an AStar maze solver instance with specific K
              AStar aStar = new AStar(heuristic, k);

              GridMazeSolver.Result result = aStar.solve(generationResult.maze(), generationResult.weights(), SRC, DST, new BoolVertexLabelling(TOPOLOGY.nbVertices()));

              // Insert K if the current k is not in the map, add it. Doing this here prevents us from adding a loop at the beginning
              if (!kManhattanResults.containsKey(k)) {
                kManhattanResults.put(k, new LinkedList<>());
              }
              // Store results
              kManhattanResults.get(k).add(result);

              System.out.println(heuristic.name() + " [" + k + "]: " + result.treatments() + " / " + result.length());
            }
          } else {
            // Creating a new AStar object
            AStar aStar = new AStar(heuristic);

            // Solving the given configuration
            GridMazeSolver.Result result = aStar.solve(generationResult.maze(), generationResult.weights(), SRC, DST, new BoolVertexLabelling(TOPOLOGY.nbVertices()));

            // Store results
            results.get(heuristic.ordinal()).add(result);

            System.out.println(heuristic.name() + ": " + result.treatments() + " / " + result.length());
          }
        }
      }

      System.out.println("\n\nRésultats de l'expérience \"" + p.description + "\":");

      List<GridMazeSolver.Result> djikstraResults = results.get(AStar.Heuristic.DIJKSTRA.ordinal());
      double djikstraAverageTreatment = getAverage(djikstraResults, GridMazeSolver.Result::treatments);

      // Loop on heuristic results
      for (int i = 0; i < results.size(); ++i) {
        AStar.Heuristic heuristic = AStar.Heuristic.values()[i];
        System.out.println(heuristic.name() + ":");

        List<GridMazeSolver.Result> heuristicResults = results.get(i);

        double averageLength = getAverage(heuristicResults, GridMazeSolver.Result::length);
        System.out.printf("Longueur moyenne du chemin trouvé: %.2f\n", averageLength);

        double averageTreatment = getAverage(heuristicResults, GridMazeSolver.Result::treatments);
        System.out.printf("Nombre moyen de sommets traités: %.2f\n", averageTreatment);

        // Computing the treatment percentage decrease compared with Dijkstra
        double averageTreatmentPercentageDecrease = (djikstraAverageTreatment - averageTreatment) / (djikstraAverageTreatment / 100);
        System.out.printf("Diminution en pourcentage du nombre moyen de sommets traités par rapport à Djikstra: %.2f\n", averageTreatmentPercentageDecrease);

        // Adding the global results of this heuristic in the csv file.

        addCsvLine("stats.csv", String.format(Locale.US, "\"%s\",\"%s\",%.2f,%.2f,%.2f", p.description(), heuristic, averageLength, averageTreatment, averageTreatmentPercentageDecrease));
      }

      System.out.println("\nRésultats détaillés K-Manhattan:");

      // Loop on K values
      for (Integer k : kManhattanResults.keySet()) {
        System.out.println("K = " + k + ":");

        List<GridMazeSolver.Result> kKManhattanResults = kManhattanResults.get(k);
        // Since we were not totally confident on whether we were supposed to compute the minimum, mean, and maximum relative
        // error according to the number of treated vertices, or to the length of the path to destination, we did it
        // for both.
        double minErrorTreatments = Double.MAX_VALUE;
        double maxErrorTreatments = Double.MIN_VALUE;
        double errorSumTreatments = 0;
        double minErrorLength = Double.MAX_VALUE;
        double maxErrorLength = Double.MIN_VALUE;
        double errorSumLength = 0;
        double treatmentsPercentageGain = 0;
        int optimalResults = 0;

        // Loop over each result and compare it to the Dijkstra equivalent
        for (int i = 0; i < kKManhattanResults.size(); ++i) {
          GridMazeSolver.Result kKManhattanResult = kKManhattanResults.get(i);
          GridMazeSolver.Result djikstraResult = djikstraResults.get(i);
          if (kKManhattanResult.length() == djikstraResult.length()) {
            ++optimalResults;
          }

          treatmentsPercentageGain += (double) (djikstraResult.treatments() - kKManhattanResult.treatments()) / djikstraResult.treatments() * 100;

          double relativeErrorTreatments = (double) (kKManhattanResult.treatments() - djikstraResult.treatments()) / djikstraResult.treatments() * 100;
          errorSumTreatments += relativeErrorTreatments;

          minErrorTreatments = Math.min(minErrorTreatments, relativeErrorTreatments);
          maxErrorTreatments = Math.max(maxErrorTreatments, relativeErrorTreatments);

          double relativeErrorLength = (double) (kKManhattanResult.length() - djikstraResult.length()) / djikstraResult.length() * 100;
          errorSumLength += relativeErrorLength;

          minErrorLength = Math.min(minErrorLength, relativeErrorLength);
          maxErrorLength = Math.max(maxErrorLength, relativeErrorLength);
        }

        double optimalResultsPercentage = (double) optimalResults / djikstraResults.size() * 100;
        System.out.printf("Pourcentage de solutions optimales: %.2f\n", optimalResultsPercentage);

        System.out.printf("Nombre de sommets traités erreur relative minimale: %.2f\n", minErrorTreatments);

        System.out.printf("Nombre de sommets traités erreur relative maximale: %.2f\n", maxErrorTreatments);

        double errorMeanTreatments = errorSumTreatments / djikstraResults.size();
        System.out.printf("Nombre de sommets traités erreur relative moyenne: %.2f\n", errorMeanTreatments);

        System.out.printf("Longueur du chemin trouvé erreur relative minimale: %.2f\n", minErrorLength);

        System.out.printf("Longueur du chemin trouvé erreur relative maximale: %.2f\n", maxErrorLength);

        double errorMeanLength = errorSumLength / djikstraResults.size();
        System.out.printf("Longueur du chemin trouvé erreur relative moyenne: %.2f\n", errorMeanLength);

        double averageTreatmentsGain = treatmentsPercentageGain / djikstraResults.size();
        System.out.printf("Gain moyen: %.2f\n", averageTreatmentsGain);
        
        addCsvLine("k_manhattan_stats.csv", String.format(Locale.US, "\"%s\",%d,%.2f,%.2f,%.2f,%.2f,%.2f,%.2f,%.2f,%.2f", p.description(), k,
                optimalResultsPercentage, minErrorTreatments, maxErrorTreatments, errorMeanTreatments, minErrorLength, maxErrorLength, errorMeanLength, averageTreatmentsGain));
      }
    }
  }


  /**
   * Résultat de la méthode {@link #generateGrid}, fournit un labyrinthe et une fonction de pondération des arêtes
   * associée.
   *
   * @param maze    labyrinthe généré
   * @param weights Fonction de pondération associée
   */
  private record GenerationResult(GridGraph2D maze, PositiveWeightFunction weights) {}

  /**
   * Génère un labyrinthe en forme de grille avec un générateur donné et des réglages spécifiques pour le relief et
   * l'ouverture (i.e. densité de murs) du labyrinthe.
   *
   * @param generator      Générateur de labyrinthe.
   * @param tuneParameters Paramètres de réglage du relief et de l'ouverture du labyrinthe.
   * @param rng            Générateur de nombres aléatoires.
   * @return Un {@link GenerationResult} contenant le labyrinthe et la fonction de pondération associée.
   */
  private static GenerationResult generateGrid(MazeGenerator generator, double[] tuneParameters, RandomGenerator rng) {
    GridGraph maze = new GridGraph(SIDE);
    if (!generator.requireWalls())
      GridGraph.bindAll(maze);

    MazeBuilder builder = new GridMazeBuilder(TOPOLOGY, maze);
    generator.generate(builder, 0);

    MazeTuner tuner = new MazeTuner()
        .setRandomGenerator(rng)
        .setReliefDensityFactor(tuneParameters[0])
        .setWallRemovalProbability(tuneParameters[1])
        .setReliefRadiusRatio(tuneParameters[2])
        .setReliefSummitsPerRange((int) tuneParameters[3])
        .setReliefMaxSummitWeight((int) tuneParameters[4]);

    tuner.removeWalls(TOPOLOGY, maze);
    int[] weights = tuner.generateRelief(SIDE, SIDE);
    PositiveWeightFunction wf = new ShenaniganWeightFunction(weights, tuner.getReliefMinWeight());

    return new GenerationResult(maze, wf);
  }
}
