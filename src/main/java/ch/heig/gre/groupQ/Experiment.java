package ch.heig.gre.groupQ;

import ch.heig.gre.graph.GridGraph;
import ch.heig.gre.graph.GridGraph2D;
import ch.heig.gre.graph.PositiveWeightFunction;
import ch.heig.gre.maze.MazeBuilder;
import ch.heig.gre.maze.MazeGenerator;
import ch.heig.gre.maze.impl.GridMazeBuilder;
import ch.heig.gre.maze.impl.MazeTuner;
import ch.heig.gre.maze.impl.ShenaniganWeightFunction;

import java.util.random.RandomGenerator;

// TODO: renommer le package (Shift + F6) selon la lettre attribuée à votre groupe

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

  public static void main(String[] args) {
    // TODO
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
