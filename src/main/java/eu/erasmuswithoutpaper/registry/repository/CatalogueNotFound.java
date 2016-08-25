package eu.erasmuswithoutpaper.registry.repository;

/**
 * Thrown by {@link ManifestRepositoryImpl#getCatalogue()} when no catalogue contents were found in
 * the repository.
 */
@SuppressWarnings("serial")
public class CatalogueNotFound extends Exception {

}
