package eu.erasmuswithoutpaper.registry.updater;

import eu.erasmuswithoutpaper.registry.repository.ManifestRepository;
import eu.erasmuswithoutpaper.registry.sourceprovider.ManifestSource;
import eu.erasmuswithoutpaper.registry.sourceprovider.ManifestSourceProvider;

/**
 * Registry updater is responsible for fetching new content of the manifests and importing this new
 * content into the catalogue.
 */
public interface RegistryUpdater {

  /**
   * Reload all {@link ManifestSource}s provided by the {@link ManifestSourceProvider}. Return only
   * after all sources have been reloaded.
   *
   * <p>
   * See {@link #reloadManifestSource(ManifestSource)} for details on the process.
   * </p>
   */
  void reloadAllManifestSources();

  /**
   * Reload a single {@link ManifestSource}. The following actions are applied:
   *
   * <ul>
   * <li>The new content of the manifest is fetched and validated.</li>
   * <li>If there are any warnings or errors, manifest's maintainers are notified. BTW, the list of
   * manifest maintainers is also being updated, based on the <code>&lt;ewp:admin-email/&gt;</code>
   * elements found in the manifest itself.</li>
   * <li>If there weren't any errors, then the manifest (after being filtered) gets imported into
   * the catalogue.</li>
   * <li>All changes to the manifests (both original and filtered), and the catalogue are committed
   * to the {@link ManifestRepository}. The new content of the manifests and the catalogue can be
   * acquired directly from there.</li>
   * </ul>
   *
   * @param manifestSource {@link ManifestSource} to be reloaded. It SHOULD be one of the sources
   *        from the {@link ManifestSourceProvider#getAll()} list!
   */
  void reloadManifestSource(ManifestSource manifestSource);
}
