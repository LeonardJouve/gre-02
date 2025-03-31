package ch.heig.gre.graph;

/**
 * <p>Fonction de pondération des arêtes d'un graphe.</p>
 *
 * <p>Les poids des arêtes sont des entiers strictement positifs, bornés inférieurement par une valeur minimale
 * retournée par {@link #minWeight()}.</p>
 *
 * <p>La pondération est retournée en temps constant.</p>
 */
public interface PositiveWeightFunction {

  /**
   * <p>Retourne le poids de l'arête reliant les sommets <i>u</i> et <i>v</i>.</p>
   *
   * <p>Si l'arête <i>u</i>-<i>v</i> n'existe pas, le poids retourné est indéfini, mais supérieur ou égal à
   * {@link #minWeight()}.</p>
   *
   * @param u Un sommet.
   * @param v Un autre sommet.
   * @return Le poids de l'arête reliant <i>u</i> et <i>v</i>.
   *
   * @throws IndexOutOfBoundsException si <i>u</i> ou <i>v</i> n'existent pas dans le graphe.
   */
  int get(int u, int v);

  /**
   * Borné inférieur pour les poids des arêtes.
   * @return Un entier strictement positif.
   */
  int minWeight();
}
