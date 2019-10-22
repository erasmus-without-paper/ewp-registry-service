package eu.erasmuswithoutpaper.registry.consoleapplication;

import static eu.erasmuswithoutpaper.registry.consoleapplication.ApplicationParametersUtils.buildApiNameParameter;
import static eu.erasmuswithoutpaper.registry.consoleapplication.ApplicationParametersUtils.readParameter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import eu.erasmuswithoutpaper.registry.validators.SemanticVersion;
import eu.erasmuswithoutpaper.registry.validators.web.ManifestApiEntry;
import org.springframework.boot.ApplicationArguments;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class VersionParameters {
  private static final Logger logger = LoggerFactory.getLogger(VersionParameters.class);

  /**
   * Returns help text lines for --version parameter.
   */
  public static List<String> getVersionParameterHelpText() {
    return Arrays.asList(
        "  API Versions:",
        "    --version=<version> - version of the API to test.",
        "                      Possible values:",
        "                          latest - selects newest implemented version of the API.",
        "                          all - runs tests for all implemented versions.",
        "                      Default: latest"
    );
  }

  /**
   * Filters given APIs to those matching selected versions.
   */
  public static List<ManifestApiEntry> filterApiEntriesVersions(
      List<ManifestApiEntry> selectedApiEntries,
      ApplicationArguments args) throws ApplicationArgumentException {

    LinkedHashMap<String, List<SemanticVersion>> availableVersions = new LinkedHashMap<>();

    for (ManifestApiEntry apiEntry : selectedApiEntries) {
      try {
        List<SemanticVersion> versions = availableVersions.getOrDefault(
            buildApiNameParameter(apiEntry), new ArrayList<>()
        );
        versions.add(new SemanticVersion(apiEntry.version));
        availableVersions.put(buildApiNameParameter(apiEntry), versions);
      } catch (SemanticVersion.InvalidVersionString invalidVersionString) {
        logger.error("Invalid version string encountered for API {}, '{}'",
            buildApiNameParameter(apiEntry), apiEntry.version);
      }
    }

    Map<String, List<String>> selectedVersions = new LinkedHashMap<>();

    for (Map.Entry<String, List<SemanticVersion>> entry : availableVersions.entrySet()) {
      String apiName = entry.getKey();
      List<SemanticVersion> versions = entry.getValue();
      if (versions.size() == 1) {
        selectedVersions.put(
            apiName,
            Collections.singletonList(versions.get(0).toString())
        );
      }
      String versionParameter = readParameter(args, "version", "version", false);
      if (versionParameter == null || versionParameter.equals("latest")) {
        Optional<SemanticVersion> latest = versions.stream().max(SemanticVersion::compareTo);
        if (latest.isPresent()) {
          selectedVersions.put(apiName, Collections.singletonList(latest.get().toString()));
        }
      } else if (versionParameter.equals("all")) {
        selectedVersions.put(
            apiName,
            versions.stream().map(SemanticVersion::toString).collect(Collectors.toList())
        );
      }
    }

    return selectedApiEntries.stream().filter(
        api -> selectedVersions.getOrDefault(
            buildApiNameParameter(api), new ArrayList<>()
        ).contains(api.version)
    ).collect(
        Collectors.toList()
    );
  }

}
