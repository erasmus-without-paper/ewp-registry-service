package eu.erasmuswithoutpaper.registry.validators.githubtags;

import java.util.List;

import eu.erasmuswithoutpaper.registry.internet.Internet;
import eu.erasmuswithoutpaper.registry.validators.SemanticVersion;

public interface GitHubTagsGetter {

  /**
   * Sends request to GitHub and parses the response to obtain list of tags available for API.
   *
   * @param apiName API name.
   * @param internet Internet to connect to GitHub.
   * @return list of available versions.
   */
  List<SemanticVersion> getTags(String apiName, Internet internet);

}
