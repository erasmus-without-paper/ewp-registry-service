package eu.erasmuswithoutpaper.registry.updater;

import java.util.Optional;

import eu.erasmuswithoutpaper.registry.Application;
import eu.erasmuswithoutpaper.registry.common.Utils;
import eu.erasmuswithoutpaper.registry.notifier.NotifierFlag;
import eu.erasmuswithoutpaper.registry.notifier.NotifierService;
import eu.erasmuswithoutpaper.registry.sourceprovider.ManifestSource;

/**
 * A specialized subclass of {@link NotifierService}'s {@link NotifierFlag} to be used for keeping
 * track of {@link ManifestUpdateStatus}.
 */
public class ManifestUpdateStatusNotifierFlag extends NotifierFlag {

  private final ManifestSource source;

  /**
   * @param source Reference to the {@link ManifestSource} for which this flag has been created
   *        (this is a 1-1 relationship).
   */
  public ManifestUpdateStatusNotifierFlag(ManifestSource source) {
    this.source = source;
  }

  @Override
  public Optional<String> getDetailsUrl() {
    if (Application.getRootUrl() != null) {
      return Optional
          .of(Application.getRootUrl() + "/status?url=" + Utils.urlencode(this.source.getUrl()));
    } else {
      return Optional.empty();
    }
  }

  @Override
  public String getName() {
    return "Manifest import status for '" + this.source.getUrl() + "'.";
  }
}
