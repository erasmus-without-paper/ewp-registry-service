package eu.erasmuswithoutpaper.registry.repository;

/**
 * Thrown by {@link ManifestRepositoryImpl#getCatalogue()} when no catalogue contents were found in
 * the repository.
 */
public class CatalogueNotFound extends Exception {
  private static final long serialVersionUID = -7061555206737745664L;

}
