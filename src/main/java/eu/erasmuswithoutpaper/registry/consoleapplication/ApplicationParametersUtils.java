package eu.erasmuswithoutpaper.registry.consoleapplication;

import java.util.ArrayList;
import java.util.List;

import eu.erasmuswithoutpaper.registry.validators.ApiEndpoint;
import eu.erasmuswithoutpaper.registry.validators.web.ManifestApiEntry;
import org.springframework.boot.ApplicationArguments;

public class ApplicationParametersUtils {
  static List<String> readParameterAllValues(ApplicationArguments args, String name) {
    List<String> values = args.getOptionValues(name);
    if (values == null) {
      values = new ArrayList<>();
    }
    return values;
  }

  public static String readParameter(ApplicationArguments args, String name,
      String valuePlaceholder) throws ApplicationArgumentException {
    return readParameter(args, name, valuePlaceholder, true);
  }

  static String readParameter(ApplicationArguments args, String name,
      String valuePlaceholder, boolean required) throws ApplicationArgumentException {
    List<String> values = args.getOptionValues(name);
    boolean hasValue = values != null && values.size() > 0;
    if (!hasValue && !required) {
      return null;
    }
    if (!hasValue) {
      throw new ApplicationArgumentException(
          String.format("missing --%s=<%s>", name, valuePlaceholder)
      );
    }
    if (values.size() > 1) {
      throw new ApplicationArgumentException(
          String.format("expected one --%s=<%s>", name, valuePlaceholder)
      );
    }
    return values.get(0);
  }

  public static String buildApiNameParameter(ManifestApiEntry entry) {
    return buildApiNameParameter(entry.name, entry.endpoint);
  }

  static String buildApiNameParameter(String name, ApiEndpoint endpoint) {
    String apiName;
    if (endpoint != ApiEndpoint.NO_ENDPOINT) {
      apiName = String.format("%s-%s", name, endpoint);
    } else {
      apiName = String.format("%s", name);
    }

    return apiName;
  }
}
