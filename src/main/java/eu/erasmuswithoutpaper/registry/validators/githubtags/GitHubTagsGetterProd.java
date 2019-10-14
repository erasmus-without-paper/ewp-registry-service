package eu.erasmuswithoutpaper.registry.validators.githubtags;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import eu.erasmuswithoutpaper.registry.internet.Internet;
import eu.erasmuswithoutpaper.registry.validators.SemanticVersion;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import com.google.common.base.Charsets;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;

@Service
@Profile({ "production", "development", "console" })
public class GitHubTagsGetterProd implements GitHubTagsGetter {
  /**
   * Sends request to GitHub and parses the response to obtain list of tags available for API.
   * @param apiName API name.
   * @param internet Internet to connect to GitHub.
   * @param logger Logger of class using this method.
   * @return list of available versions.
   */
  public List<SemanticVersion> getTags(String apiName, Internet internet, Logger logger) {
    String url = "https://api.github.com/repos/erasmus-without-paper/ewp-specs-api-";
    url += apiName;
    url += "/tags";

    List<SemanticVersion> result = new ArrayList<>();
    try {
      byte[] data = internet.getUrl(url);
      JSONArray jsonArray = new JSONArray(new String(data, Charsets.UTF_8));
      for (int i = 0; i < jsonArray.length(); i++) {
        JSONObject jsonObject = jsonArray.getJSONObject(i);
        SemanticVersion version = new SemanticVersion(jsonObject.getString("name"));
        result.add(version);
      }
      return result;
    } catch (IOException e) {
      logger.warn("Cannot fetch github tags from url " + url);
    } catch (JSONException e) {
      logger.warn("GitHub api returned invalid JSON from url " + url);
    } catch (SemanticVersion.InvalidVersionString e) {
      logger.warn("GitHub tags response contained invalid name field.");
    }
    return new ArrayList<>();
  }
}

