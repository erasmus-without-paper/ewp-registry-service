package eu.erasmuswithoutpaper.registry.validators.githubtags;

import java.util.ArrayList;
import java.util.List;

import eu.erasmuswithoutpaper.registry.internet.Internet;
import eu.erasmuswithoutpaper.registry.validators.SemanticVersion;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

@Service
@Profile("test")
public class GitHubTagsGetterTest implements GitHubTagsGetter {

  @Override
  public List<SemanticVersion> getTags(String apiName, Internet internet) {
    return new ArrayList<>();
  }

}
