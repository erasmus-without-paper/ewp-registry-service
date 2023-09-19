package eu.erasmuswithoutpaper.registry.repository;

/**
 * Thrown by {@link ManifestRepositoryImpl#getManifestFiltered(String)} when URL of the manifest was
 * not found in the repository.
 */
public class ManifestNotFound extends Exception {
  private static final long serialVersionUID = 58112075609118799L;

}
