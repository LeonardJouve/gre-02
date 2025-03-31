package ch.heig.gre.maze.impl;

import ch.heig.gre.graph.PositiveWeightFunction;

import java.util.Objects;

/**
 * <p>Fonction de pondération d'arêtes qui retourne un poids positif pour chaque pair de sommets {u, v} selon les poids
 * respectifs des sommets u et v.</p>
 */
public final class ShenaniganWeightFunction implements PositiveWeightFunction {
  private final int[] vertexWeights;
  private final int min;

  /**
   * <p>Crée une nouvelle fonction de pondération d'arêtes.</p>
   *
   * @param vertexWeights Les poids des sommets.
   * @param min           Borne inférieure strictement positive pour les poids des arêtes.
   * @throws NullPointerException     si vertexWeights est null.
   * @throws IllegalArgumentException si min est inférieur ou égal à 0.
   */
  public ShenaniganWeightFunction(int[] vertexWeights, int min) {
    if (min <= 0) throw new IllegalArgumentException("min must be greater than 0");
    this.vertexWeights = Objects.requireNonNull(vertexWeights);
    this.min = min;
  }

  @Override
  public int get(int u, int v) {
    return Math.max(Math.max(vertexWeights[u], vertexWeights[v]), min);
  }

  @Override
  public int minWeight() {
    return min;
  }
}
