package eu.erasmuswithoutpaper.registry.sourceprovider;

import java.util.List;
import java.util.Optional;

/**
 * Provides a list of {@link ManifestSource ManifestSources}.
 *
 * <p>
 * This component is used to feed the Registry Service with the list of {@link ManifestSource
 * ManifestSources} which should be used for generating the catalogue.
 * </p>
 */
public interface ManifestSourceProvider {

  /**
   * @return A full list of all {@link ManifestSource ManifestSources} to be used.
   */
  List<ManifestSource> getAll();

  /**
   * Return a {@link ManifestSource} for given URL.
   *
   * @param url A value which should match one of our {@link ManifestSource#getUrl()} values.
   * @return An optional with {@link ManifestSource}, if found.
   */
  default Optional<ManifestSource> getOne(String url) {
    return getAll().stream().filter(source -> source.getUrl().equals(url)).findFirst();
  }

  /**
   * Update the list of {@link ManifestSource ManifestSources}.
   */
  void update();

}
