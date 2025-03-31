package ch.heig.gre.graph;

/**
 * Sujet capable de gérer une collection de {@link GraphObserver}, les notifiant lorsque certains événements
 * se produisent sur un graphe sous-jacent.
 *
 * @see Graph
 * @see GraphObserver
 */
public interface ObservableGraph {
  /**
   * <p>Enregistre un nouvel observateur.</p>
   *
   * <p>Un même observateur enregistré à de multiples reprises peut recevoir plusieurs notifications pour un même
   * événement, selon l'implémentation.</p>
   *
   * <p>Enregistrer {@code null} n'a aucun effet.</p>
   *
   * @param observer Un {@link GraphObserver}.
   */
  void subscribe(GraphObserver observer);

  /**
   * <p>Interrompt l'envoi d'événements à l'observateur donné, si préalablement enregistré (sinon ne fait rien).</p>
   *
   * <p>Si l'observateur a été enregistré à de multiples reprises, il n'est pas garanti que tous les enregistrements
   * soient supprimés.</p>
   *
   * @param observer Un {@link GraphObserver}.
   */
  void unsubscribe(GraphObserver observer);
}
