package eu.erasmuswithoutpaper.registry.validators.githubtags;

import java.util.List;

import eu.erasmuswithoutpaper.registry.internet.Internet;
import eu.erasmuswithoutpaper.registry.validators.SemanticVersion;

import org.slf4j.Logger;

public interface GitHubTagsGetter {
  List<SemanticVersion> getTags(String apiName, Internet internet, Logger logger);
}
