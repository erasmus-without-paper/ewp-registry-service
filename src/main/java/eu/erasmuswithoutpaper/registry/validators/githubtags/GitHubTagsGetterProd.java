package eu.erasmuswithoutpaper.registry.validators.githubtags;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import eu.erasmuswithoutpaper.registry.configuration.Constans;
import eu.erasmuswithoutpaper.registry.internet.Internet;
import eu.erasmuswithoutpaper.registry.validators.SemanticVersion;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
@Profile({ "production", "development", "console" })
public class GitHubTagsGetterProd implements GitHubTagsGetter {

  private static final Logger logger = LoggerFactory.getLogger(GitHubTagsGetterProd.class);

  @Override
  public List<SemanticVersion> getTags(String apiName, Internet internet) {
    String url = Constans.GITHUB_API_URL + "/ewp-specs-api-";
    url += apiName;
    url += "/tags";

    List<SemanticVersion> result = new ArrayList<>();
    try {
      byte[] data = internet.getUrl(url);
      Gson gson = new Gson();
      JsonArray jsonArray = gson.fromJson(new String(data, StandardCharsets.UTF_8),
          JsonArray.class);
      for (JsonElement jsonElement : jsonArray) {
        JsonObject jsonObject = jsonElement.getAsJsonObject();
        SemanticVersion version = new SemanticVersion(jsonObject.get("name").getAsString());
        result.add(version);
      }
      return result;
    } catch (IOException e) {
      logger.warn("Cannot fetch github tags from url " + url);
    } catch (JsonSyntaxException e) {
      logger.warn("GitHub api returned invalid JSON from url " + url);
    } catch (SemanticVersion.InvalidVersionString e) {
      logger.warn("GitHub tags response contained invalid name field.");
    }
    return new ArrayList<>();
  }
}

