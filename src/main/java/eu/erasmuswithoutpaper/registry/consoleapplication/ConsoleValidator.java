package eu.erasmuswithoutpaper.registry.consoleapplication;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.KeyPair;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;

import eu.erasmuswithoutpaper.registry.common.KeyPairAndCertificate;
import eu.erasmuswithoutpaper.registry.common.KeyStoreUtilsException;
import eu.erasmuswithoutpaper.registry.documentbuilder.EwpDocBuilder;
import eu.erasmuswithoutpaper.registry.updater.ManifestParser;
import eu.erasmuswithoutpaper.registry.validators.AbstractValidationSuite;
import eu.erasmuswithoutpaper.registry.validators.ApiValidatorsManager;
import eu.erasmuswithoutpaper.registry.validators.ExternalValidatorKeyStore;
import eu.erasmuswithoutpaper.registry.validators.HtmlValidationReportFormatter;
import eu.erasmuswithoutpaper.registry.validators.HttpSecurityDescription;
import eu.erasmuswithoutpaper.registry.validators.SemanticVersion;
import eu.erasmuswithoutpaper.registry.validators.ValidationParameters;
import eu.erasmuswithoutpaper.registry.validators.ValidationStepWithStatus;
import eu.erasmuswithoutpaper.registry.validators.ValidatorKeyStoreSet;
import eu.erasmuswithoutpaper.registry.validators.web.ManifestApiEntry;
import eu.erasmuswithoutpaper.registry.xmlformatter.XmlFormatter;
import eu.erasmuswithoutpaper.registryclient.RegistryClient;
import org.springframework.boot.ApplicationArguments;

import org.beryx.textio.TextIO;
import org.beryx.textio.TextIoFactory;
import org.beryx.textio.TextTerminal;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;


public class ConsoleValidator {
  private static final Logger logger = LoggerFactory.getLogger(ConsoleValidator.class);
  private boolean tlsDisabled = false;
  private boolean httpSigDisabled = false;
  private ApiValidatorsManager apiValidatorsManager;
  private ExternalValidatorKeyStore externalValidatorKeyStore;
  private EwpDocBuilder docBuilder;
  private ManifestParser parser;
  private XmlFormatter xmlFormatter;
  private ValidatorKeyStoreSet keyStoreSet;
  private RegistryClient registryClient;

  private static void printHelp(TextTerminal<?> textTerminal,
      ApiValidatorsManager apiValidatorsManager) {
    List<String> helpTextHeaders = Arrays.asList(
        "  The API Validator",
        "  This executable can be used to validate your implementation of EWP APIs with set of",
        "  automatic tests. You should provide your implementation private keys known to",
        "  dev-registry and report will be generated to you. You can run those tests yourself",
        "  against your own implementation, but the main use case is to test another partners.",
        "",
        "  PARAMETERS:",
        "  General:",
        "    -h, --help - print this message and exit.",
        "    --manifest=<arg> - url of manifest to test, required",
        "    --registry-domain=<domain> - domain of registry to use, without https://",
        "                                 and trailing slash.",
        "                                 Default: dev-registry.erasmuswithoutpaper.eu"
    );

    List<String> keysParams = CryptoParameters.getCryptoParametersHelpText();
    List<String> apiParameter = ApiParameters.getApiParameterHelpText(apiValidatorsManager);
    List<String> versionParameter = VersionParameters.getVersionParameterHelpText();
    List<String> securityParam = SecurityParameters.getSecurityParameterHelpText();
    List<String> apisParameters = ApiParameters.getApisParametersHelpText();

    List<String> helpText = AbstractValidationSuite.concatArrays(
        helpTextHeaders,
        keysParams,
        apiParameter,
        versionParameter,
        securityParam,
        apisParameters
    );

    textTerminal.println(helpText);
  }

  private void readKeysFromArguments(ApplicationArguments args,
      TextTerminal<?> textTerminal)
      throws ApplicationArgumentException, KeyStoreUtilsException {
    readTlsKeyAndCertificate(args, textTerminal);
    readHttpSigKeys(args, textTerminal);
    readPermissionKeysAndCertificate(args);
  }

  private void readPermissionKeysAndCertificate(ApplicationArguments args)
      throws KeyStoreUtilsException, ApplicationArgumentException {
    KeyPairAndCertificate keyPairAndCertificate =
        CryptoParameters.readPermissionTlsKeyAndCertificateFromParameters(args);
    if (keyPairAndCertificate == null) {
      return;
    }

    ExternalValidatorKeyStore keyStore = new ExternalValidatorKeyStore(this.registryClient);
    keyStore.setServerRsaKey(keyPairAndCertificate.keyPair);
    keyStore.setClientRsaKey(keyPairAndCertificate.keyPair);
    keyStore.setCertificate(keyPairAndCertificate.keyPair, keyPairAndCertificate.certificate);
    this.keyStoreSet.setSecondaryKeyStore(keyStore);
  }

  private void readTlsKeyAndCertificate(
      ApplicationArguments args, TextTerminal<?> textTerminal)
      throws ApplicationArgumentException, KeyStoreUtilsException {
    KeyPairAndCertificate keyPairAndCertificate =
        CryptoParameters.readTlsKeyAndCertificateFromParameters(args);
    if (keyPairAndCertificate == null) {
      textTerminal.println("TLS KeyStore not provided, TLS protocol disabled.");
      this.tlsDisabled = true;
      return;
    }
    this.externalValidatorKeyStore.setCertificate(
        keyPairAndCertificate.keyPair,
        keyPairAndCertificate.certificate
    );
  }

  private void readHttpSigKeys(
      ApplicationArguments args, TextTerminal<?> textTerminal)
      throws ApplicationArgumentException, KeyStoreUtilsException {
    KeyPair clientKeyPair = CryptoParameters.readHttpSigClientKeyPair(args);
    if (clientKeyPair == null) {
      textTerminal.println(
          "Client HTTPSig KeyStore and PEM file not provided, HTTPSig protocol disabled."
      );
      httpSigDisabled = true;
      return;
    }

    KeyPair serverKeyPair = CryptoParameters.readHttpSigServerKeyPair(args);
    if (serverKeyPair == null) {
      textTerminal.println(
          "Server HTTPSig KeyStore and PEM file not provided, HTTPSig protocol disabled."
      );
      httpSigDisabled = true;
      return;
    }
    this.externalValidatorKeyStore.setClientRsaKey(clientKeyPair);
    this.externalValidatorKeyStore.setServerRsaKey(serverKeyPair);
  }

  private boolean isHelpParameterPresent(ApplicationArguments args) {
    return args.containsOption("help") || args.containsOption("h");
  }

  private void validate(ApplicationArguments args, TextIO console, TextTerminal<?> textTerminal,
      String registryDomain)
      throws ApplicationArgumentException, IOException, KeyStoreUtilsException {
    textTerminal.printf("Using registry located at https://%s/\n", registryDomain);
    final String manifestUrl = ApplicationParametersUtils.readParameter(args, "manifest", "url");

    readKeysFromArguments(args, textTerminal);

    if (this.tlsDisabled && this.httpSigDisabled) {
      textTerminal.println("Keys not provided, exiting. Use '--help' parameter to get help.");
      return;
    }

    String manifest = ApiParameters.readManifestFromUrl(manifestUrl);

    Document doc;
    try {
      doc = this.parser.parseManifest(manifest.getBytes(StandardCharsets.UTF_8), null);
    } catch (ManifestParser.NotValidManifest e) {
      textTerminal.println("This manifest file is not valid, terminating.");
      return;
    }

    String filteredContents = this.xmlFormatter.format(doc);
    List<ManifestApiEntry> apis = ApiParameters
        .readApisFromManifest(filteredContents, apiValidatorsManager);
    if (apis.isEmpty()) {
      textTerminal.println("No API found in provided manifest file, terminating.");
      return;
    }
    List<ManifestApiEntry> selectedApiEntries =
        ApiParameters.getSelectedApiEntries(apis, args, console);
    if (selectedApiEntries.isEmpty()) {
      textTerminal.println("API was not selected, terminating.");
      return;
    }

    List<ManifestApiEntry> filteredApiEntries = VersionParameters
        .filterApiEntriesVersions(selectedApiEntries, args);

    for (ManifestApiEntry entry : filteredApiEntries) {
      List<HttpSecurityDescription> securities = SecurityParameters.getSelectedSecurity(
          entry.securities, ApplicationParametersUtils.buildApiNameParameter(entry), args, console,
          this.tlsDisabled, this.httpSigDisabled
      );
      SemanticVersion semanticVersion;
      try {
        semanticVersion = new SemanticVersion(entry.version);
      } catch (SemanticVersion.InvalidVersionString e) {
        logger.error(
            "Couldn't parse version string '{}' in API '{}'. Skipping.",
            entry.version, ApplicationParametersUtils.buildApiNameParameter(entry)
        );
        continue;
      }

      for (HttpSecurityDescription security : securities) {
        runTestsAndGenerateReport(
            entry, semanticVersion, security, args, registryDomain, textTerminal, console
        );
      }
    }
  }

  private void runTestsAndGenerateReport(ManifestApiEntry entry, SemanticVersion semanticVersion,
      HttpSecurityDescription security, ApplicationArguments args, String registryDomain,
      TextTerminal<?> textTerminal, TextIO console)
      throws ApplicationArgumentException, IOException {

    textTerminal.printf("Testing API %s, version %s, security %s\n",
        ApplicationParametersUtils.buildApiNameParameter(entry), semanticVersion.toString(),
        security.toString()
    );
    ValidationParameters requestParameters = ApiParameters.getParametersForApi(
        entry, args, console
    );

    Date validationStartedDate = new Date();

    textTerminal.printf("Started tests at %s\n", getUtcDateString(validationStartedDate));
    List<ValidationStepWithStatus> report = runTests(console, entry, security,
        semanticVersion, requestParameters);

    HtmlValidationReportFormatter.ValidationInfoParameters validationInfoParameters =
        new HtmlValidationReportFormatter.ValidationInfoParameters(
            entry.name, entry.url, semanticVersion.toString(), security, validationStartedDate, null
        );

    textTerminal.printf("Finished tests at %s\n", getUtcDateString(new Date()));

    String htmlReport = ReportUtils.generateHtmlReport(
        report, validationInfoParameters, docBuilder, registryDomain
    );
    String reportSummary = ReportUtils.generateReportSummary(report);
    textTerminal.println(reportSummary);

    String resultFileName = String.format("test-result-%s-%s-%s-%s.html",
        ApplicationParametersUtils.buildApiNameParameter(entry), semanticVersion, security,
        getUtcDateString(validationStartedDate)
    );

    Path file = Paths.get(resultFileName);
    Files.write(file, Collections.singletonList(htmlReport), StandardCharsets.UTF_8);
    textTerminal.printf("Full report was saved to %s\n\n\n", resultFileName);
  }

  private List<ValidationStepWithStatus> runTests(TextIO console, ManifestApiEntry entry,
      HttpSecurityDescription securityDescription, SemanticVersion semanticVersion,
      ValidationParameters userParameters) {
    console.getTextTerminal().print("Running tests...\n");

    return this.apiValidatorsManager
        .getApiValidator(entry.name, entry.endpoint)
        .runTests(entry.url, semanticVersion, securityDescription, userParameters);
  }

  private static String getUtcDateString(Date date) {
    TimeZone tz = TimeZone.getTimeZone("UTC");
    DateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH-mm-ss'Z'");
    df.setTimeZone(tz);
    return df.format(date);
  }

  /**
   * Reads the parameters, asks the user about missing values and performs tests.
   * @param args
   *      Arguments passed to the executable.
   * @param apiValidatorsManager
   *      ApiValidatorsManager that has information about all implemented validators.
   * @param docBuilder
   *      needed to validate responses.
   * @param parser
   *      needed parse manifests.
   * @param xmlFormatter
   *      used to format manifest which contents should be validated.
   * @param keyStoreSet
   *      ValidatorKeyStoreSet that holds keys used by the validators.
   * @param registryClient
   *      Used to fetch data from the registry.
   * @param registryDomain
   *      Domain (without https:// and trailing slash) where the registry can be found.
   */
  public void performValidation(
      ApplicationArguments args,
      ApiValidatorsManager apiValidatorsManager,
      EwpDocBuilder docBuilder,
      ValidatorKeyStoreSet keyStoreSet,
      ManifestParser parser,
      XmlFormatter xmlFormatter,
      RegistryClient registryClient,
      String registryDomain) {
    final TextIO console = TextIoFactory.getTextIO();
    final TextTerminal<?> textTerminal = console.getTextTerminal();
    this.apiValidatorsManager = apiValidatorsManager;
    this.docBuilder = docBuilder;
    this.registryClient = registryClient;
    this.parser = parser;
    this.xmlFormatter = xmlFormatter;

    this.externalValidatorKeyStore = new ExternalValidatorKeyStore(this.registryClient);
    keyStoreSet.setMainKeyStore(this.externalValidatorKeyStore);
    keyStoreSet.setSecondaryKeyStore(null);
    this.keyStoreSet = keyStoreSet;

    if (isHelpParameterPresent(args)) {
      printHelp(textTerminal, apiValidatorsManager);
      return;
    }

    try {
      validate(args, console, textTerminal, registryDomain);
    } catch (ApplicationArgumentException | IOException | KeyStoreUtilsException e) {
      textTerminal.println(e.getMessage());
      textTerminal.println("\nUse --help parameter to get help.");
    }
  }
}
