package eu.erasmuswithoutpaper.registry.consoleapplication;

import static eu.erasmuswithoutpaper.registry.consoleapplication.ApplicationParametersUtils.readParameter;
import static eu.erasmuswithoutpaper.registry.consoleapplication.UserInput.userSelectList;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import eu.erasmuswithoutpaper.registry.validators.AbstractValidationSuite;
import eu.erasmuswithoutpaper.registry.validators.HttpSecurityDescription;
import org.springframework.boot.ApplicationArguments;

import org.beryx.textio.TextIO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SecurityParameters {
  private static final Logger logger = LoggerFactory.getLogger(SecurityParameters.class);

  /**
   * Returns help text lines for --security parameter.
   * @return
   *      List of String that should be a part of help message.
   */
  public static List<String> getSecurityParameterHelpText() {
    List<String> securityParameter = Arrays.asList(
        "  Security protocols:",
        "    --security=<arg> - select security protocol to use. This is string with four chars,",
        "                       defined as follows:"
    );
    List<String> securityParametersDesc = new ArrayList<>();
    for (Map.Entry<String, String> entry : HttpSecurityDescription.getLegend().entrySet()) {
      String description = String
          .format("                       %s - %s", entry.getKey(), entry.getValue());
      securityParametersDesc.add(description);
    }

    List<String> securityParameterExamples = Arrays.asList(
        "                       examples: HTTT, SHTT",
        "                       Wildcard character '*' can be used to select groups of securities,",
        "                       example: H**T will select all available securities with HTTP Sig",
        "                                client authentication and TLS response encryption",
        "                       Special values:",
        "                           `all` - run tests for all available security protocols."
    );

    return AbstractValidationSuite.concatArrays(
        securityParameter, securityParametersDesc, securityParameterExamples);
  }

  private static String userSelectSecurity(TextIO console, List<String> securities,
      String apiName) {
    return userSelectList(console, securities, "Select security method for API " + apiName);
  }

  /**
   * Returns securities provided in parameters or asks user to select one.
   * @param securities
   *      List of Securities to select from. Represent Securities implemented by one of APIs.
   * @param apiName
   *      Name of tested API.
   * @param args
   *      Arguments passed to the executable.
   * @param console
   *      TextIO representing the console used by the user.
   * @param filterHttp
   *      If true, then HTTPSig options won't be returned nor presented to the user.
   * @param filterTls
   *      If true, then TLS options won't be returned nor presented to the user.
   * @return
   *      List of APIs that were selected by the user of specified in arguments.
   * @throws ApplicationArgumentException
   *     Thrown when arguments passed to the executable contain incorrect values.
   */
  public static List<HttpSecurityDescription> getSelectedSecurity(
      List<String> securities,
      String apiName,
      ApplicationArguments args,
      TextIO console,
      boolean filterTls,
      boolean filterHttp) throws ApplicationArgumentException {
    String securityOption = readParameter(args, "security", "security", false);
    securities = filterDisabledSecurities(securities, filterTls, filterHttp);
    List<String> securityDescriptions;

    if (securityOption == null) {
      String security = userSelectSecurity(console, securities, apiName);
      if (security != null) {
        securityDescriptions = Collections.singletonList(security);
      } else {
        securityDescriptions = new ArrayList<>();
      }
    } else if (securityOption.equals("all")) {
      securityDescriptions = securities;
    } else {
      if (securityOption.length() != 4) {
        throw new ApplicationArgumentException(
            "Expected '--security' to be string with exactly four characters, or 'all'."
        );
      }
      String pattern = buildSecurityPattern(securityOption);
      securityDescriptions = securities.stream()
          .filter(security -> Pattern.matches(pattern, security))
          .collect(Collectors.toList());
    }

    List<HttpSecurityDescription> result = new ArrayList<>();
    for (String securityDescription : securityDescriptions) {
      try {
        result.add(new HttpSecurityDescription(securityDescription));
      } catch (HttpSecurityDescription.InvalidDescriptionString invalidDescriptionString) {
        logger.error("Couldn't parse security description '{}'. Skipping this security.",
            securityDescription);
      }
    }
    return result;
  }

  private static List<String> filterDisabledSecurities(List<String> securities, boolean filterTls,
      boolean filterHttpSig) {
    if (filterHttpSig) {
      securities = securities.stream().filter(security -> !security.contains("H")).collect(
          Collectors.toList());
    }
    if (filterTls) {
      securities = securities.stream().filter(security -> !security.contains("T")).collect(
          Collectors.toList());
    }
    return securities;
  }

  private static String buildSecurityPattern(
      String securityOption) throws ApplicationArgumentException {
    final char wildcard = '*';
    final char clientAuthenticationChar = securityOption.charAt(0);
    final char serverAuthenticationChar = securityOption.charAt(1);
    final char requestEncryptionChar = securityOption.charAt(2);
    final char responseEncryptionChar = securityOption.charAt(3);

    StringBuilder securityPatternBuilder = new StringBuilder();
    if (clientAuthenticationChar == wildcard) {
      securityPatternBuilder.append('.');
    } else {
      try {
        HttpSecurityDescription.getClientAuthFromCode(clientAuthenticationChar);
      } catch (HttpSecurityDescription.InvalidDescriptionString invalidDescriptionString) {
        throw new ApplicationArgumentException(
            "Invalid value of '--security', invalid client security."
        );
      }
      securityPatternBuilder.append(clientAuthenticationChar);
    }

    if (serverAuthenticationChar == wildcard) {
      securityPatternBuilder.append('.');
    } else {
      try {
        HttpSecurityDescription.getServerAuthFromCode(serverAuthenticationChar);
      } catch (HttpSecurityDescription.InvalidDescriptionString invalidDescriptionString) {
        throw new ApplicationArgumentException(
            "Invalid value of '--security', invalid server security."
        );
      }
      securityPatternBuilder.append(serverAuthenticationChar);
    }

    if (requestEncryptionChar == wildcard) {
      securityPatternBuilder.append('.');
    } else {
      try {
        HttpSecurityDescription.getRequestEncryptionFromCode(requestEncryptionChar);
      } catch (HttpSecurityDescription.InvalidDescriptionString invalidDescriptionString) {
        throw new ApplicationArgumentException(
            "Invalid value of '--security', invalid request encryption."
        );
      }
      securityPatternBuilder.append(requestEncryptionChar);
    }

    if (responseEncryptionChar == wildcard) {
      securityPatternBuilder.append('.');
    } else {
      try {
        HttpSecurityDescription.getResponseEncryptionFromCode(responseEncryptionChar);
      } catch (HttpSecurityDescription.InvalidDescriptionString invalidDescriptionString) {
        throw new ApplicationArgumentException(
            "Invalid value of '--security', invalid response encryption."
        );
      }
      securityPatternBuilder.append(responseEncryptionChar);
    }
    return securityPatternBuilder.toString();
  }
}
