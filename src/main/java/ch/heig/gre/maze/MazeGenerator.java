package ch.heig.gre.maze;

/**
 * Générateur de labyrinthes.
 *
 * @see MazeBuilder
 */
public interface MazeGenerator {
  /**
   * <p>Génère un nouveau labyrinthe en ajoutant ou supprimant des murs à une instance de {@link MazeBuilder} donnée.</p>
   *
   * <p>L'état initial du labyrinthe dépend de la valeur de {@link #requireWalls}. Si cette méthode retourne
   * {@code true}, tous les murs possibles sont déjà placés dans le labyrinthe, sinon aucun mur n'est placé.</p>
   *
   * @param builder Un {@link MazeBuilder} à qui déléguer les modifications de la structure de données.
   * @param from Sommet de départ, si l'algorithme utilisé en nécessite un.
   * @throws NullPointerException si {@code builder} est {@code null}.
   * @throws IllegalArgumentException si {@code from} n'est pas un sommet du graphe sous-jacent de {@code builder}.
   */
  void generate(MazeBuilder builder, int from);

  /**
   * <p>Indique si le générateur nécessite que tous les murs soient placés avant de commencer la génération.</p>
   *
   * <p>Si {@code true}, tous les murs possibles seront placés dans l'état initial du labyrinthe lorsque la méthode
   * {@link #generate} est appelée et le générateur doit supprimer des murs pour générer le labyrinthe.
   * Sinon, aucun mur n'est placé au préalable, et le générateur doit les ajouter lors de la génération.</p>
   *
   * @return {@code true} si tous les murs doivent être placés avant de commencer la génération, {@code false} sinon.
   */
  boolean requireWalls();
}
