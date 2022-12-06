package eu.erasmuswithoutpaper.registry.consoleapplication;

import static eu.erasmuswithoutpaper.registry.consoleapplication.ApplicationParametersUtils.buildApiNameParameter;
import static eu.erasmuswithoutpaper.registry.consoleapplication.ApplicationParametersUtils.readParameter;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import eu.erasmuswithoutpaper.registry.validators.ApiValidator;
import eu.erasmuswithoutpaper.registry.validators.ApiValidatorsManager;
import eu.erasmuswithoutpaper.registry.validators.ValidationParameter;
import eu.erasmuswithoutpaper.registry.validators.ValidationParameters;
import eu.erasmuswithoutpaper.registry.validators.web.ManifestApiEntry;
import org.springframework.boot.ApplicationArguments;

import org.beryx.textio.TextIO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ApiParameters {
  private static final Logger logger = LoggerFactory.getLogger(ApiParameters.class);

  /**
   * Get help text for --api parameter.
   *
   * @param apiValidatorsManager
   *     ApiValidatorsManager that has information about all implemented validators.
   * @return List of String that should be a part of help message.
   */
  public static List<String> getApiParameterHelpText(ApiValidatorsManager apiValidatorsManager) {
    ArrayList<String> apiParameter = new ArrayList<>(Arrays.asList(
        "  APIs:",
        "    --api=<name> - name of the API to test.",
        "                  If not provided, you will be prompted to select one.",
        "                  If there are several API with same name and other parameters are not",
        "                  provided, you will be prompted for more data.",
        "                  Special values:",
        "                      `all` - test all APIs.",
        "                  Examples:",
        "                  --hei_id=uw.edu.pl - use uw.edu.pl as hei_id parameter in all tests",
        "                  --hei_id=uw.edu.pl --ounits:hei_id=up.pt - use uw.edu.pl as hei_id",
        "                    parameter in all tests except for ounits, where up.pt should be used.",
        "                  Possible APIs and their parameters parameters:"
    ));

    Map<ApiValidatorsManager.ApiNameAndEndpoint, ApiValidator<?>> availableValidators =
        apiValidatorsManager.getRegisteredApiValidators();

    for (Map.Entry<ApiValidatorsManager.ApiNameAndEndpoint, ApiValidator<?>> entry :
        availableValidators.entrySet()) {
      ApiValidatorsManager.ApiNameAndEndpoint apiAndEndpoint = entry.getKey();
      ApiValidator<?> apiValidator = entry.getValue();
      String apiName = buildApiNameParameter(apiAndEndpoint.apiName, apiAndEndpoint.endpoint);
      apiParameter.add(
          String.format("                  API: %s", apiName)
      );

      List<ApiValidator.ParameterWithVersion> parameterWithVersions = apiValidator.getParameters();
      for (ApiValidator.ParameterWithVersion parameterWithVersion : parameterWithVersions) {
        ValidationParameter parameter = parameterWithVersion.parameter;

        String description = "";
        if (parameter.getDescription() != null) {
          description = ", " + parameter.getDescription();
        }

        if (parameterWithVersion.maxMajorVersion.equals("inf")) {
          apiParameter.add(
              String.format(
                  "                    - %s - from version %s%s",
                  parameter.getName(),
                  parameterWithVersion.minMajorVersion,
                  description
              )
          );
        } else {
          apiParameter.add(
              String.format(
                  "                    - %s - from version %s - %s%s",
                  parameter.getName(),
                  parameterWithVersion.minMajorVersion,
                  parameterWithVersion.maxMajorVersion,
                  description
              )
          );
        }

        if (!parameter.getDependencies().isEmpty()) {
          String dependencies = String.join(", ", parameter.getDependencies());
          apiParameter.add(
              String.format("                      requires %s", dependencies)
          );
        }

        if (!parameter.getBlockers().isEmpty()) {
          String blockers = String.join(", ", parameter.getBlockers());
          apiParameter.add(
              String.format("                      exclusive with %s", blockers)
          );
        }
      }
    }

    return apiParameter;
  }

  /**
   * Get help text for api parameters.
   *
   * @return List of String that should be a part of help message.
   */
  public static List<String> getApisParametersHelpText() {
    return Arrays.asList(
        "  API parameters:",
        "    --[parameter name]=<arg> - provides value for [parameter name] that will be used for",
        "                               validation APIs. You will be prompted to provide all ",
        "                               parameters you didn't specify. You can provide api-name if",
        "                               you want to provide parameter value for certain API.",
        "                             Format: [api-name:]value",
        "    --use-default-parameters - do not prompt for parameters. Default value will be used ",
        "                               for parameter not provided using --[parameter name]",
        "                               Example:",
        "                               --courses:hei_id=uw.edu.pl --use-default-parameters",
        "                               will try to find default parameters in all tests except",
        "                               for Courses where it will use uw.edu.pl as hei_id",
        "                               and default values for other parameters."
    );
  }

  /**
   * Reads the manifest and returns APIs for which tests are available.
   *
   * @param manifest
   *     Manifest to read.
   * @param apiValidatorsManager
   *     ApiValidatorsManager that has information about all implemented validators.
   * @return List of ManifestApiEntry obtained by parsing the manifest.
   */
  public static List<ManifestApiEntry> readApisFromManifest(String manifest,
      ApiValidatorsManager apiValidatorsManager) {
    return ManifestApiEntry.parseManifest(manifest, apiValidatorsManager)
        .stream().filter(api -> api.available).collect(Collectors.toList());
  }

  /**
   * Reads the manifest from the URL.
   *
   * @param manifestUrl
   *     URL of the manifest to download.
   * @return Downloaded manifest as a String.
   * @throws IOException
   *     When there were problems while downloading the manifest.
   */
  public static String readManifestFromUrl(String manifestUrl) throws IOException {
    try (InputStream manifestStream = new URL(manifestUrl).openStream();
         InputStreamReader reader = new InputStreamReader(manifestStream, StandardCharsets.UTF_8);
         BufferedReader bufferedReader = new BufferedReader(reader)) {
      return bufferedReader.lines().collect(Collectors.joining("\n"));
    }
  }

  /**
   * Returns APIs selected by arguments or asks user to select one.
   *
   * @param apis
   *     List of APIs to select from. Represent APIs implemented by one of manifests.
   * @param args
   *     Arguments passed to the executable.
   * @param console
   *     TextIO representing the console used by the user.
   * @return List of APIs that were selected by the user of specified in arguments.
   */
  public static List<ManifestApiEntry> getSelectedApiEntries(List<ManifestApiEntry> apis,
      ApplicationArguments args, TextIO console) {
    List<String> apiNames = ApplicationParametersUtils.readParameterAllValues(args, "api");

    if (apiNames.isEmpty()) {
      ManifestApiEntry selectedApiEntry = userSelectApi(console, apis);
      if (selectedApiEntry == null) {
        return new ArrayList<>();
      }
      return Collections.singletonList(selectedApiEntry);
    }

    final String allApisParameter = "all";
    if (apiNames.size() == 1 && apiNames.get(0).equals(allApisParameter)) {
      return apis;
    }

    Set<String> allApis = apis.stream().map(api -> api.name).collect(Collectors.toSet());
    for (String apiName : apiNames) {
      if (!allApis.contains(apiName)) {
        logger.error("Unknown API provided: {}", apiName);
      }
    }
    return apis.stream().filter(
        api -> apiNames.contains(buildApiNameParameter(api))
    ).collect(Collectors.toList());
  }

  /**
   * Returns parameters for given API provided as parameters or asks user to provide one.
   *
   * @param entry
   *     Represents an API for which parameters should be returned.
   * @param args
   *     Arguments passed to the executable.
   * @param console
   *     TextIO representing the console used by the user.
   * @return Parameters either read from the arguments or provided by the user.
   * @throws ApplicationArgumentException
   *     Thrown when arguments passed to the executable contain incorrect values.
   */
  public static ValidationParameters getParametersForApi(ManifestApiEntry entry,
      ApplicationArguments args, TextIO console) throws ApplicationArgumentException {
    ValidationParameters result = new ValidationParameters();
    Set<String> enteredParameters = new HashSet<>();

    // We assume that entry.parameters are topologically sorted, that is - if a parameter has
    // a dependency, that dependency appears before that parameter on the list.
    for (ValidationParameter parameter : entry.parameters) {

      if (!enteredParameters.containsAll(parameter.getDependencies())) {
        continue;
      }

      if (parameter.getBlockers().stream().anyMatch(enteredParameters::contains)) {
        continue;
      }


      String apiArgName = buildParameterWithApiArgumentName(entry, parameter);
      String apiArgValue = readParameter(args, apiArgName, "value", false);
      String argValue = readParameter(args, parameter.getName(), "value", false);
      String value = null;
      if (apiArgValue != null) {
        value = apiArgValue;
      } else if (argValue != null) {
        value = argValue;
      } else if (isItAllowedToAskTheUserForParameters(args)) {
        value = console.newStringInputReader()
            .withItemName(parameter.getName())
            .withMinLength(0)
            .read(String.format("%s [enter - leave default]:", parameter.getName()));
      }
      if (value != null && !value.isEmpty()) {
        enteredParameters.add(parameter.getName());
        result.put(parameter.getName(), value);
        console.getTextTerminal().printf("%s <- %s\n", parameter.getName(), value);
      }
    }

    return result;
  }

  private static boolean isItAllowedToAskTheUserForParameters(ApplicationArguments args) {
    return !args.containsOption("use-default-parameters");
  }

  private static String buildParameterWithApiArgumentName(ManifestApiEntry entry,
      ValidationParameter parameter) {
    return String.format("%s:%s", buildApiNameParameter(entry), parameter.getName());
  }

  private static ManifestApiEntry userSelectApi(TextIO console, List<ManifestApiEntry> apis) {
    return UserInput.userSelectList(console, apis, "Select API to test",
        apiEntry -> String.format(
            "%s\t%s\t%s",
            buildApiNameParameter(apiEntry), apiEntry.version, apiEntry.url)
    );
  }

}
