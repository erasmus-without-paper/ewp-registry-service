package eu.erasmuswithoutpaper.registry.validators.githubtags;

import java.util.ArrayList;
import java.util.List;

import eu.erasmuswithoutpaper.registry.internet.Internet;
import eu.erasmuswithoutpaper.registry.validators.SemanticVersion;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import org.slf4j.Logger;

@Service
@Profile("test")
public class GitHubTagsGetterTest implements GitHubTagsGetter {
  public List<SemanticVersion> getTags(String apiName, Internet internet, Logger logger) {
    return new ArrayList<>();
  }
}
