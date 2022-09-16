package eu.erasmuswithoutpaper.registry.sourceprovider;

import java.util.List;
import java.util.Optional;

/**
 * Provides a list of {@link ManifestSource}s.
 *
 * <p>
 * This component is used to feed the Registry Service with the list of {@link ManifestSource}s
 * which should be used for generating the catalogue.
 * </p>
 */
public abstract class ManifestSourceProvider {

  /**
   * @return A full list of all {@link ManifestSource}s to be used.
   */
  public abstract List<ManifestSource> getAll();

  /**
   * Return a {@link ManifestSource} for given URL.
   *
   * @param url A value which should match one of our {@link ManifestSource#getUrl()} values.
   * @return An optional with {@link ManifestSource}, if found.
   */
  public Optional<ManifestSource> getOne(String url) {
    for (ManifestSource source : this.getAll()) {
      if (source.getUrl().equals(url)) {
        return Optional.of(source);
      }
    }
    return Optional.empty();
  }

  /**
   * Update the list of {@link ManifestSource}s.
   */
  public abstract void update();
}
