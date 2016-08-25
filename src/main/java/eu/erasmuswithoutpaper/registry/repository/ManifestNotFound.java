package eu.erasmuswithoutpaper.registry.repository;

/**
 * Thrown by {@link ManifestRepositoryImpl#getManifestFiltered(String)} when URL of the manifest was
 * not found in the repository.
 */
@SuppressWarnings("serial")
public class ManifestNotFound extends Exception {

}
