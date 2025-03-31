package ch.heig.gre.maze;

import ch.heig.gre.graph.VertexLabelling;

/**
 * <p>Implémentation basique de {@link VertexLabelling} pour des étiquettes de type booléen.</p>
 */
public final class BoolVertexLabelling implements VertexLabelling<Boolean> {
  private final boolean[] labels;

  /**
   * <p>Crée un nouvel étiquetage de sommets.</p>
   * @param size Taille de l'étiquetage.
   */
  public BoolVertexLabelling(int size) {
    this.labels = new boolean[size];
  }

  @Override
  public Boolean getLabel(int v) {
    assertVertexExists(v);
    return labels[v];
  }

  @Override
  public void setLabel(int v, Boolean label) {
    assertVertexExists(v);
    labels[v] = label;
  }

  private void assertVertexExists(int v) {
    if (v < 0 || v >= labels.length) {
      throw new IndexOutOfBoundsException("Vertex " + v + " is out of bounds");
    }
  }
}
