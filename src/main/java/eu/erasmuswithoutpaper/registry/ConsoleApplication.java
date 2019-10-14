package eu.erasmuswithoutpaper.registry;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringWriter;
import java.io.Writer;
import java.math.BigInteger;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.interfaces.RSAPrivateCrtKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.RSAPrivateCrtKeySpec;
import java.security.spec.RSAPublicKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import eu.erasmuswithoutpaper.registry.documentbuilder.EwpDocBuilder;
import eu.erasmuswithoutpaper.registry.validators.ApiValidatorsManager;
import eu.erasmuswithoutpaper.registry.validators.ExternalValidatorKeyStore;
import eu.erasmuswithoutpaper.registry.validators.HtmlValidationReportFormatter;
import eu.erasmuswithoutpaper.registry.validators.HttpSecurityDescription;
import eu.erasmuswithoutpaper.registry.validators.SemanticVersion;
import eu.erasmuswithoutpaper.registry.validators.ValidationParameter;
import eu.erasmuswithoutpaper.registry.validators.ValidationParameters;
import eu.erasmuswithoutpaper.registry.validators.ValidationStepWithStatus;
import eu.erasmuswithoutpaper.registry.validators.web.ManifestApiEntry;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.Banner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.condition.ConditionalOnNotWebApplication;
import org.springframework.context.ApplicationContext;

import com.mitchellbosecke.pebble.PebbleEngine;
import com.mitchellbosecke.pebble.error.PebbleException;
import com.mitchellbosecke.pebble.loader.ClasspathLoader;
import com.mitchellbosecke.pebble.template.PebbleTemplate;
import org.beryx.textio.TextIO;
import org.beryx.textio.TextIoFactory;
import org.beryx.textio.TextTerminal;
import org.bouncycastle.util.encoders.Base64;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sun.security.util.DerInputStream;
import sun.security.util.DerValue;


/**
 * Our secondary Spring application class.
 */
@SpringBootApplication
@ConditionalOnNotWebApplication
public class ConsoleApplication implements ApplicationRunner {

  @Autowired
  private ApiValidatorsManager apiValidatorsManager;

  @Autowired
  private ExternalValidatorKeyStore externalValidatorKeyStore;

  @Autowired
  ApplicationContext applicationContext;

  @Autowired
  private EwpDocBuilder docBuilder;

  private static final Logger logger = LoggerFactory.getLogger(ConsoleApplication.class);

  private static void disableLoadingDatabaseAndWebAutoconfigurations() {
    List<String> autoconfigurationsToDisable = Arrays.asList(
        "org.springframework.boot.autoconfigure.data.jpa.JpaRepositoriesAutoConfiguration",
        "org.springframework.boot.autoconfigure.flyway.FlywayAutoConfiguration",
        "org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration",
        "org.springframework.boot.autoconfigure.jdbc.DataSourceTransactionManagerAutoConfiguration",
        "org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration",
        "org.springframework.boot.autoconfigure.web.EmbeddedServletContainerAutoConfiguration",
        "org.springframework.boot.autoconfigure.web.WebClientAutoConfiguration",
        "org.springframework.boot.autoconfigure.mail.MailSenderValidatorAutoConfiguration",
        "org.springframework.boot.autoconfigure.web.WebMvcAutoConfiguration",
        "com.mitchellbosecke.pebble.boot.autoconfigure.PebbleAutoConfiguration"
    );

    String excludeAutoconfigurePropertyName = "spring.autoconfigure.exclude";
    System.setProperty(excludeAutoconfigurePropertyName,
        String.join(",", autoconfigurationsToDisable));
  }


  /**
   * Initialize and run Spring application.
   *
   * @param args
   *     Command-line arguments.
   */
  public static void main(String[] args) {
    Locale.setDefault(Locale.US);
    disableLoadingDatabaseAndWebAutoconfigurations();
    System.setProperty("spring.profiles.active", "console");
    SpringApplication app = new SpringApplication(ConsoleApplication.class);
    app.setWebEnvironment(false);
    app.setBannerMode(Banner.Mode.OFF);
    app.setLogStartupInfo(false);
    app.run(args);
  }

  private static <T> T userSelectList(TextIO console, List<T> elements, String prompt) {
    return userSelectList(console, elements, prompt, Object::toString);
  }

  private static <T> T userSelectList(TextIO console, List<T> elements,
      String prompt, Stringifier<T> stringifier) {
    final TextTerminal<?> textTerminal = console.getTextTerminal();
    textTerminal.print("0: EXIT\n");
    for (int i = 0; i < elements.size(); i++) {
      T element = elements.get(i);
      textTerminal.printf("%d: %s\n", i + 1, stringifier.toString(element));
    }
    int selected = console.newIntInputReader()
        .withMinVal(0)
        .withMaxVal(elements.size() + 1)
        .read(prompt);
    if (selected == 0) {
      return null;
    }

    T selectedElement = elements.get(selected - 1);
    textTerminal.printf("Selected %s\n", stringifier.toString(selectedElement));
    return selectedElement;
  }

  private static PrivateKey readPkcs1Key(byte[] keyBytes,
      KeyFactory keyFactory) throws IOException, InvalidKeySpecException {
    DerInputStream derReader = new DerInputStream(keyBytes);
    DerValue[] seq = derReader.getSequence(0);
    // skip version seq[0];
    BigInteger modulus = seq[1].getBigInteger();
    BigInteger publicExp = seq[2].getBigInteger();
    BigInteger privateExp = seq[3].getBigInteger();
    BigInteger prime1 = seq[4].getBigInteger();
    BigInteger prime2 = seq[5].getBigInteger();
    BigInteger exp1 = seq[6].getBigInteger();
    BigInteger exp2 = seq[7].getBigInteger();
    BigInteger crtCoef = seq[8].getBigInteger();

    RSAPrivateCrtKeySpec keySpec = new RSAPrivateCrtKeySpec(
        modulus, publicExp, privateExp, prime1, prime2, exp1, exp2, crtCoef);
    return keyFactory.generatePrivate(keySpec);
  }

  private static PrivateKey readPkcs7Key(byte[] keyBytes,
      KeyFactory keyFactory) throws InvalidKeySpecException {
    X509EncodedKeySpec spec = new X509EncodedKeySpec(keyBytes);
    return keyFactory.generatePrivate(spec);
  }

  private static KeyPair readKey(
      String filePath) throws IOException, InvalidKeySpecException, NoSuchAlgorithmException {
    String key = String.join("\n", Files.readAllLines(Paths.get(filePath)));
    KeyFactory keyFactory = KeyFactory.getInstance("RSA");

    boolean isPkcs1;

    if (key.contains("-----BEGIN RSA PRIVATE KEY-----")) {
      isPkcs1 = true;
    } else if (key.contains("-----BEGIN PRIVATE KEY-----")) {
      isPkcs1 = false;
    } else {
      return null;
    }

    key = key.replace("-----BEGIN RSA PRIVATE KEY-----", "");
    key = key.replace("-----END RSA PRIVATE KEY-----", "");
    key = key.replace("-----BEGIN PRIVATE KEY-----", "");
    key = key.replace("-----END PRIVATE KEY-----", "");
    key = key.replaceAll("\n", "");

    byte[] rsaKeyBase64Bytes = key.getBytes(StandardCharsets.UTF_8);
    byte[] keyBytes = Base64.decode(rsaKeyBase64Bytes);

    PrivateKey privateKey;
    if (isPkcs1) {
      privateKey = readPkcs1Key(keyBytes, keyFactory);
    } else {
      privateKey = readPkcs7Key(keyBytes, keyFactory);
    }

    RSAPrivateCrtKey rsaPrivateKey = (RSAPrivateCrtKey) privateKey;

    RSAPublicKeySpec publicKeySpec = new RSAPublicKeySpec(
        rsaPrivateKey.getModulus(), rsaPrivateKey.getPublicExponent()
    );

    PublicKey publicKey;
    try {
      publicKey = keyFactory.generatePublic(publicKeySpec);
    } catch (InvalidKeySpecException e) {
      logger.error("Pubilc key spec invalid");
      return null;
    }

    return new KeyPair(publicKey, rsaPrivateKey);
  }

  public static class ApplicationArgumentException extends Exception {
    public ApplicationArgumentException(String message) {
      super(message);
    }
  }

  private String readParameter(ApplicationArguments args, String name,
      String valuePlaceholder) throws ApplicationArgumentException {
    if (!args.containsOption(name)) {
      throw new ApplicationArgumentException(
          String.format("missing --%s=<%s>", name, valuePlaceholder)
      );
    }
    List<String> values = args.getOptionValues(name);
    if (values.size() == 0) {
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

  @Override
  public void run(ApplicationArguments args) throws Exception {
    final TextIO console = TextIoFactory.getTextIO();
    final TextTerminal<?> textTerminal = console.getTextTerminal();

    String manifestUrl = readParameter(args, "manifest", "url");

    String clientKeyPath = readParameter(args, "client-key", "file-path");
    this.externalValidatorKeyStore.setClientRsaKey(readKey(clientKeyPath));

    String serverKeyPath = readParameter(args, "server-key", "file-path");
    this.externalValidatorKeyStore.setServerRsaKey(readKey(serverKeyPath));

    String manifest = readManifestFromUrl(manifestUrl);
    List<ManifestApiEntry> apis = readApisFromManifest(manifest);
    ManifestApiEntry entry = userSelectApi(console, apis);
    if (entry == null) {
      return;
    }
    String security = userSelectSecurity(console, entry.securities);

    HttpSecurityDescription securityDescription;
    SemanticVersion semanticVersion;
    try {
      securityDescription = new HttpSecurityDescription(security);
      semanticVersion = new SemanticVersion(entry.version);
    } catch (HttpSecurityDescription.InvalidDescriptionString
        | IllegalArgumentException
        | SemanticVersion.InvalidVersionString ignored) {
      // TODO log
      return;
    }

    ValidationParameters requestParameters = userEnterParameters(console, entry.parameters);

    printUsedParameters(console, requestParameters);

    Date validationStartedDate = new Date();

    textTerminal.printf("Started tests at %s\n", validationStartedDate.toString());
    List<ValidationStepWithStatus> report = runTests(console, entry, securityDescription,
        semanticVersion, requestParameters);

    HtmlValidationReportFormatter.ValidationInfoParameters validationInfoParameters =
        new HtmlValidationReportFormatter.ValidationInfoParameters(
            entry.name,
            security,
            entry.url,
            semanticVersion.toString(),
            securityDescription,
            validationStartedDate,
            null
        );

    String htmlReport = generateHtmlReport(report, validationInfoParameters);
    String reportSummary = generateReportSummary(report);
    textTerminal.print(reportSummary);

    Path file = Paths.get("result.html");
    Files.write(file, Collections.singletonList(htmlReport), StandardCharsets.UTF_8);
  }

  private String generateReportSummary(List<ValidationStepWithStatus> report) {
    return String.format("SUMMARY, run %s tests", report.size()); //TODO
  }

  private String generateHtmlReport(List<ValidationStepWithStatus> report,
      HtmlValidationReportFormatter.ValidationInfoParameters validationInfoParameters) {

    HtmlValidationReportFormatter htmlValidationReportFormatter =
        new HtmlValidationReportFormatter(docBuilder);

    Map<String, Object> pebbleContext = htmlValidationReportFormatter.getPebbleContext(
        report,
        validationInfoParameters
    );

    String registryDomain = "https://dev-registry.erasmuswithoutpaper.eu";
    pebbleContext.put("baseUrl", registryDomain);
    pebbleContext.put("isUsingDevDesign", true);

    ClasspathLoader classpathLoader = new ClasspathLoader();
    classpathLoader.setPrefix("templates");
    classpathLoader.setSuffix(".pebble");
    PebbleEngine engine = new PebbleEngine.Builder().loader(classpathLoader).build();
    PebbleTemplate compiledTemplate = null;
    try {
      compiledTemplate = engine.getTemplate("validationResult");
    } catch (PebbleException e) {
      logger.error("getTemplate exception", e);
      // TODO log
      return "ERROR";
    }

    Writer writer = new StringWriter();
    try {
      compiledTemplate.evaluate(writer, pebbleContext);
    } catch (PebbleException | IOException e) {
      logger.error("getTemplate exception", e);
      // TODO log
      return "ERROR";
    }

    return writer.toString();
  }

  private List<ValidationStepWithStatus> runTests(TextIO console, ManifestApiEntry entry,
      HttpSecurityDescription securityDescription, SemanticVersion semanticVersion,
      ValidationParameters userParameters) {
    console.getTextTerminal().print("Running tests...\n");

    return this.apiValidatorsManager
        .getApiValidator(entry.name, entry.endpoint)
        .runTests(entry.url, semanticVersion, securityDescription, userParameters);
  }

  private void printUsedParameters(TextIO console, ValidationParameters requestParameters) {
    TextTerminal<?> textTerminal = console.getTextTerminal();
    textTerminal.print("Using parameters:\n");
    for (Map.Entry<String, String> parameter : requestParameters.getParameters()) {
      textTerminal.printf("%s:\t%s\n", parameter.getKey(), parameter.getValue());
    }
  }

  private ValidationParameters userEnterParameters(TextIO console,
      List<ValidationParameter> parameters) {
    ValidationParameters userParameters = new ValidationParameters();
    Set<String> enteredParameters = new HashSet<>();
    for (ValidationParameter parameter : parameters) {
      if (!enteredParameters.containsAll(parameter.getDependencies())) {
        continue;
      }

      if (parameter.getBlockers().stream().anyMatch(enteredParameters::contains)) {
        continue;
      }


      String value = console.newStringInputReader()
          .withItemName(parameter.getName())
          .withMinLength(0)
          .read(String.format("%s [enter - leave default]:", parameter.getName()));
      if (!value.isEmpty()) {
        enteredParameters.add(parameter.getName());
        userParameters.put(parameter.getName(), value);
      }
    }

    return userParameters;
  }

  private String userSelectSecurity(TextIO console, List<String> securities) {
    return userSelectList(console, securities, "Select security method");
  }

  private ManifestApiEntry userSelectApi(TextIO console, List<ManifestApiEntry> apis) {
    return userSelectList(console, apis, "Select API to test",
        apiEntry -> String.format("%s\t%s\t%s", apiEntry.name, apiEntry.version, apiEntry.url));
  }

  private List<ManifestApiEntry> readApisFromManifest(String manifest) {
    return ManifestApiEntry.parseManifest(manifest, apiValidatorsManager)
        .stream().filter(api -> api.available).collect(Collectors.toList());
  }

  private String readManifestFromUrl(String manifestUrl) throws IOException {
    try (InputStream manifestStream = new URL(manifestUrl).openStream();
         InputStreamReader reader = new InputStreamReader(manifestStream, StandardCharsets.UTF_8);
         BufferedReader bufferedReader = new BufferedReader(reader)) {
      return bufferedReader.lines().collect(Collectors.joining("\n"));
    }
  }
}
