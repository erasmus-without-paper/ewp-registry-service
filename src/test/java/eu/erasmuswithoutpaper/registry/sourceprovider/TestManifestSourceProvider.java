package eu.erasmuswithoutpaper.registry.sourceprovider;

import java.util.ArrayList;
import java.util.List;

import eu.erasmuswithoutpaper.registry.updater.RegistryUpdaterImpl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

/**
 * Our "test" implementation of {@link ManifestSourceProvider}. This bean will be used during tests
 * (as opposed to {@link ProductionManifestSourceProvider}).
 *
 * <p>
 * Note that this class enables the list of manifest sources to be mutable. It notifies
 * {@link RegistryUpdaterImpl} whenever the list changes.
 * </p>
 */
@Service
@Profile("test")
public class TestManifestSourceProvider implements ManifestSourceProvider {

  private final List<ManifestSource> sources = new ArrayList<>();
  private RegistryUpdaterImpl updater;

  /**
   * @param source {@link ManifestSource} to be added to the list.
   */
  public void addSource(ManifestSource source) {
    this.sources.add(source);
    this.updater.onSourcesUpdated();
  }

  /**
   * Remove all the sources from the list.
   */
  public void clearSources() {
    this.sources.clear();
    this.updater.onSourcesUpdated();
  }

  @Override
  public List<ManifestSource> getAll() {
    return this.sources;
  }

  @Override
  public void update() {
    // Updating manifest sources list is not possible in tests.
  }

  /**
   * @param source {@link ManifestSource} to be removed from the list.
   * @return <b>true</b> if it existed on the list.
   */
  public boolean removeSource(ManifestSource source) {
    boolean sourceExisted = this.sources.remove(source);
    if (sourceExisted) {
      this.updater.onSourcesUpdated();
    }
    return sourceExisted;
  }

  @Autowired
  private void setRegistryUpdater(RegistryUpdaterImpl updater) {
    this.updater = updater;
  }
}
