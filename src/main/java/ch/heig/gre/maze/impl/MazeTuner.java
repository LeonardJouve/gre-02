package ch.heig.gre.maze.impl;

import ch.heig.gre.graph.Graph;
import ch.heig.gre.graph.GridGraph2D;

import java.util.ArrayList;
import java.util.random.RandomGenerator;

/**
 * <p>Classe utilitaire pour ajuster les paramètres d'un labyrinthe.</p>
 *
 * <p>Deux opérations sont disponibles :</p>
 * <ul>
 *   <li>Suppression aléatoire de murs.</li>
 *   <li>Génération de relief (i.e. pondération des cellules de la grille).</li>
 * </ul>
 *
 * <p>La génération du relief est basée sur la génération de massifs montagneux aléatoires. Chaque massif est défini par
 * un ensemble de montagnes séparées par un certain espacement. Chaque montagne est définie par un sommet et un rayon
 * proportionnel à son poids. Les cellules de la grille sont pondérées en fonction de leur distance à chaque sommet de
 * montagne.</p>
 *
 * <p>Sauf mention contraire, aucune vérification n'est effectuée sur les paramètres fournis et certaines valeurs
 * peuvent provoquer des comportements indéterminés (=> trial and error).</p>
 */
public final class MazeTuner {
  /** Probabilité de suppression d'un mur. */
  public static final double DEFAULT_WALL_REMOVAL_PROBABILITY = 0.02;

  /** Poids minimum d'une cellule du relief */
  public static final int DEFAULT_RELIEF_MIN_WEIGHT = 2;

  /** Poids maximum d'un sommet */
  public static final int DEFAULT_RELIEF_MAX_SUMMIT_WEIGHT = 20;

  /** Poids minimum d'un sommet */
  public static final int DEFAULT_RELIEF_MIN_SUMMIT_WEIGHT = DEFAULT_RELIEF_MAX_SUMMIT_WEIGHT / 2;

  /** Nombre de sommets maximal d'un massif */
  public static final int DEFAULT_RELIEF_SUMMITS_PER_RANGE = 5;

  /** Ratio de calcul du rayon des montagnes selon la taille de la grille */
  public static final double DEFAULT_RELIEF_RADIUS_RATIO = 3;

  /** Ratio d'espacement entre les sommets selon la taille de la grille */
  public static final int DEFAULT_RELIEF_SUMMIT_SPACING_RATIO = 8;

  /** Facteur de densité (nombre de massifs) selon la taille de la grille */
  public static final double DEFAULT_RELIEF_DENSITY_FACTOR = 0.05;

  private double wallRemovalProbability = DEFAULT_WALL_REMOVAL_PROBABILITY;
  private int reliefMinWeight = DEFAULT_RELIEF_MIN_WEIGHT;
  private int reliefMinSummitWeight = DEFAULT_RELIEF_MIN_SUMMIT_WEIGHT;
  private int reliefMaxSummitWeight = DEFAULT_RELIEF_MAX_SUMMIT_WEIGHT;
  private int reliefSummitsPerRange = DEFAULT_RELIEF_SUMMITS_PER_RANGE;
  private double reliefRadiusRatio = DEFAULT_RELIEF_RADIUS_RATIO;
  private int reliefSummitSpacingRatio = DEFAULT_RELIEF_SUMMIT_SPACING_RATIO;
  private double reliefDensityFactor = DEFAULT_RELIEF_DENSITY_FACTOR;
  private RandomGenerator randomGenerator = RandomGenerator.getDefault();

  /**
   * <p>Supprime des murs aléatoirement dans un labyrinthe.</p>
   *
   * <p>Les murs sont supprimés avec une probabilité définie par {@link #getWallRemovalProbability()}</p>
   *
   * <p>En cas d'arguments invalides (ex. graphes de tailles différentes, etc), le comportement est indéterminé.</p>
   *
   * @param topology Topologie (i.e. graphe de toutes les arêtes possibles) du labyrinthe
   * @param grid     Grille du labyrinthe
   * @throws NullPointerException si {@code topology} ou {@code grid} est {@code null}
   */
  public void removeWalls(Graph topology, GridGraph2D grid) {
    for (var edge : topology.edges()) {
      if (!grid.areAdjacent(edge.u(), edge.v()) && randomGenerator.nextDouble() < wallRemovalProbability) {
        grid.addEdge(edge.u(), edge.v());
      }
    }
  }

  public int[] generateRelief(int width, int height) {
    final int size = width * height;
    final int density = Math.max((int) (width * reliefDensityFactor), 1);

    // Facteur de calcul du rayon de la montagne en fonction de la taille du labyrinthe et du poids maximal
    final double radiusFactor = Math.max(width / reliefRadiusRatio, 1) / reliefMaxSummitWeight;

    // Espacement entre les sommets des massifs
    final int summitSpacing = Math.max(width / reliefSummitSpacingRatio, 1);

    ArrayList<Integer> summitX = new ArrayList<>(density);
    ArrayList<Integer> summitY = new ArrayList<>(density);
    for (int i = 0; i < density; i++) {
      int x = randomGenerator.nextInt(width);
      int y = randomGenerator.nextInt(height);
      summitX.add(x);
      summitY.add(y);

      // Nombre de sommets à ajouter au massif
      int nbSummits = randomGenerator.nextInt(reliefSummitsPerRange);

      // Espace entre chaque sommet du massif. Tant pis pour le cas dx=dy=0
      int dx = randomGenerator.nextInt(summitSpacing) - summitSpacing / 2;
      int dy = randomGenerator.nextInt(summitSpacing) - summitSpacing / 2;

      // Générer les sommets du massif
      for (int j = 0; j < nbSummits; j++) {
        int x2 = x + dx;
        int y2 = y + dy;

        summitX.add(x2);
        summitY.add(y2);
        x = x2;
        y = y2;

        // Autorise un débordement par massif (sinon les bords peuvent manquer de relief)
        if (x2 < 0 || x2 >= width || y2 < 0 || y2 >= height) break;
      }
    }

    int[] summitWeights = randomGenerator.ints(
        summitX.size(),
        reliefMinSummitWeight,
        reliefMaxSummitWeight - reliefMinWeight + 1).toArray();

    // Attribution des poids de toutes les cellules de la grille
    int[] weights = new int[size];
    for (int v = 0; v < size; v++) {
      // Poids de base de la cellule
      weights[v] = reliefMinWeight;

      // Coordonnées de la cellule
      int x = v % width;
      int y = v / width;

      // Si la cellule est proche d'un ou plusieurs sommets, on lui attribue un poids en fonction de sa distance
      for (int summit = 0; summit < summitX.size(); summit++) {
        // Coordonnées du sommet de la montagne
        int sx = summitX.get(summit);
        int sy = summitY.get(summit);

        // Carré de la distance euclidienne entre la cellule et le sommet de la montagne
        int d = (x - sx) * (x - sx) + (y - sy) * (y - sy);
        int r = (int) Math.round(summitWeights[summit] * radiusFactor);
        if (d < r * r) {
          // Poids de la cellule en fonction de la distance au sommet de la montagne
          int value = reliefMinWeight + (int) Math.round((1 - Math.sqrt(d) / r) * summitWeights[summit]);
          weights[v] = Math.max(weights[v], value);
        }
      }
    }

    return weights;
  }

  // Fluent setters

  /**
   * @see #DEFAULT_WALL_REMOVAL_PROBABILITY
   */
  public MazeTuner setWallRemovalProbability(double wallRemovalProbability) {
    this.wallRemovalProbability = wallRemovalProbability;
    return this;
  }

  /**
   * @see #DEFAULT_RELIEF_MIN_WEIGHT
   */
  public MazeTuner setReliefMinWeight(int reliefMinWeight) {
    this.reliefMinWeight = reliefMinWeight;
    return this;
  }

  /**
   * @see #DEFAULT_RELIEF_MIN_SUMMIT_WEIGHT
   */
  public MazeTuner setReliefMinSummitWeight(int reliefMinSummitWeight) {
    this.reliefMinSummitWeight = reliefMinSummitWeight;
    return this;
  }

  /**
   * @see #DEFAULT_RELIEF_MAX_SUMMIT_WEIGHT
   */
  public MazeTuner setReliefMaxSummitWeight(int reliefMaxSummitWeight) {
    this.reliefMaxSummitWeight = reliefMaxSummitWeight;
    return this;
  }

  /**
   * @see #DEFAULT_RELIEF_SUMMITS_PER_RANGE
   */
  public MazeTuner setReliefSummitsPerRange(int reliefSummitsPerRange) {
    this.reliefSummitsPerRange = reliefSummitsPerRange;
    return this;
  }

  /**
   * @see #DEFAULT_RELIEF_RADIUS_RATIO
   */
  public MazeTuner setReliefRadiusRatio(double reliefRadiusRatio) {
    this.reliefRadiusRatio = reliefRadiusRatio;
    return this;
  }

  /**
   * @see #DEFAULT_RELIEF_SUMMIT_SPACING_RATIO
   */
  public MazeTuner setReliefSummitSpacingRatio(int reliefSummitSpacingRatio) {
    this.reliefSummitSpacingRatio = reliefSummitSpacingRatio;
    return this;
  }

  /**
   * @see #DEFAULT_RELIEF_DENSITY_FACTOR
   */
  public MazeTuner setReliefDensityFactor(double reliefDensityFactor) {
    this.reliefDensityFactor = reliefDensityFactor;
    return this;
  }

  /**
   * Définit le générateur de nombres aléatoires.
   * @param randomGenerator Générateur de nombres aléatoires
   * @return this
   */
  public MazeTuner setRandomGenerator(RandomGenerator randomGenerator) {
    this.randomGenerator = randomGenerator;
    return this;
  }

  // Getters

  /**
   * @see #DEFAULT_WALL_REMOVAL_PROBABILITY
   */
  public double getWallRemovalProbability() {
    return wallRemovalProbability;
  }

  /**
   * @see #DEFAULT_RELIEF_MIN_WEIGHT
   */
  public int getReliefMinWeight() {
    return reliefMinWeight;
  }

  /**
   * @see #DEFAULT_RELIEF_MIN_SUMMIT_WEIGHT
   */
  public int getReliefMinSummitWeight() {
    return reliefMinSummitWeight;
  }

  /**
   * @see #DEFAULT_RELIEF_MAX_SUMMIT_WEIGHT
   */
  public int getReliefMaxSummitWeight() {
    return reliefMaxSummitWeight;
  }

  /**
   * @see #DEFAULT_RELIEF_SUMMITS_PER_RANGE
   */
  public int getReliefSummitsPerRange() {
    return reliefSummitsPerRange;
  }

  /**
   * @see #DEFAULT_RELIEF_RADIUS_RATIO
   */
  public double getReliefRadiusRatio() {
    return reliefRadiusRatio;
  }

  /**
   * @see #DEFAULT_RELIEF_SUMMIT_SPACING_RATIO
   */
  public int getReliefSummitSpacingRatio() {
    return reliefSummitSpacingRatio;
  }

  /**
   * @see #DEFAULT_RELIEF_DENSITY_FACTOR
   */
  public double getReliefDensityFactor() {
    return reliefDensityFactor;
  }
}
