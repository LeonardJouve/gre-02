package ch.heig.gre.maze;

import ch.heig.gre.graph.PositiveWeightFunction;
import ch.heig.gre.graph.Graph;
import ch.heig.gre.graph.GridGraph2D;
import ch.heig.gre.graph.VertexLabelling;

import java.util.List;

/**
 * Solver de labyrinthes en forme de grille.
 */
@FunctionalInterface
public interface GridMazeSolver {
  /**
   * Résultat du solveur de labyrinthes.
   *
   * @param path       Chemin trouvé, dans l'ordre, de la source à la destination (toutes deux incluses). La liste peut
   *                   être modifiable ou non.
   * @param length     Longueur du chemin trouvé.
   * @param treatments Nombre de sommets traités (indice de performance).
   */
  record Result(List<Integer> path, int length, int treatments) {}

  /**
   * <p>Détermine un chemin entre deux cellules d'un labyrinthe en forme de grille rectangulaire, dont les arêtes
   * sont pondérées par des poids strictement positifs.</p>
   *
   * <p>Le chemin retourné est la liste ordonnée de tous les sommets parcourus depuis {@code source}
   * jusqu'à {@code destination} (tous deux inclus).</p>
   *
   * <p>Le paramètre d'entrée-sortie {@code processed} est utilisé pour marquer les sommets traités au fur et à mesure
   * de l'avancement de l'algorithme. L'instance fournie est initialisée à {@code false} pour tous les sommets.</p>
   *
   * <p>Des paramètres incohérents (ex. {@code processed} mal initialisé, etc.) peuvent entraîner des comportements
   * indéterminés.</p>
   *
   * @param grid        Un {@link Graph} représentant le labyrinthe.
   * @param weights     Fonction de pondération des arêtes du labyrinthe (poids strictement positifs).
   * @param source      Sommet de départ.
   * @param destination Sommet de destination.
   * @param processed   Distances des sommets à la source ou destination (entrée-sortie).
   * @return Un {@link Result} contenant le chemin trouvé, sa longueur et le nombre de sommets traités.
   *
   * @throws NullPointerException     si {@code grid}, {@code weights} ou {@code processed} sont {@code null}.
   * @throws IllegalArgumentException si {@code source} ou  {@code destination} ne sont pas des sommets de
   *                                  {@code graph}.
   */
  Result solve(GridGraph2D grid,
               PositiveWeightFunction weights,
               int source,
               int destination,
               VertexLabelling<Boolean> processed);
}
