package ch.heig.gre.gui;

/**
 * Structure de données pour les éléments d'une liste déroulante.
 *
 * @param name  Nom de l'élément, affiché dans la liste déroulante.
 * @param value Valeur de l'élément, retourné lors de la sélection.
 * @param <T>   Type de la valeur de l'élément.
 */
public record ChoiceItem<T>(String name, T value) {
  @Override
  public String toString() {
    return name;
  }
}
