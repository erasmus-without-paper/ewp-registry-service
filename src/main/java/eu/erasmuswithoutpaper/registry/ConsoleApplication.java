package eu.erasmuswithoutpaper.registry;

import java.security.Security;
import java.util.Locale;

import eu.erasmuswithoutpaper.registry.configuration.ConsoleConfiguration;
import eu.erasmuswithoutpaper.registry.configuration.ProductionConfiguration;
import eu.erasmuswithoutpaper.registry.consoleapplication.ConsoleValidator;
import eu.erasmuswithoutpaper.registry.consoleapplication.RegistryDomainProvider;
import eu.erasmuswithoutpaper.registry.documentbuilder.EwpDocBuilder;
import eu.erasmuswithoutpaper.registry.iia.IiaHashService;
import eu.erasmuswithoutpaper.registry.internet.RealInternet;
import eu.erasmuswithoutpaper.registry.updater.ManifestParser;
import eu.erasmuswithoutpaper.registry.validators.ApiValidator;
import eu.erasmuswithoutpaper.registry.validators.ApiValidatorsManager;
import eu.erasmuswithoutpaper.registry.validators.ValidatorKeyStoreSet;
import eu.erasmuswithoutpaper.registry.xmlformatter.XmlFormatter;
import eu.erasmuswithoutpaper.registryclient.RegistryClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.Banner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.condition.ConditionalOnNotWebApplication;
import org.springframework.context.annotation.ComponentScan;

import org.bouncycastle.jce.provider.BouncyCastleProvider;


/**
 * Our secondary Spring application class.
 */
@ComponentScan(basePackageClasses = {
    ApiValidator.class,
    IiaHashService.class,
    ConsoleConfiguration.class,
    ProductionConfiguration.class,
    EwpDocBuilder.class,
    XmlFormatter.class,
    ManifestParser.class,
    RealInternet.class,
    RegistryDomainProvider.class})
@ConditionalOnNotWebApplication
public class ConsoleApplication implements ApplicationRunner {
  @Autowired
  private ApiValidatorsManager apiValidatorsManager;
  @Autowired
  private ValidatorKeyStoreSet validatorKeyStoreSet;
  @Autowired
  private EwpDocBuilder docBuilder;
  @Autowired
  private ManifestParser parser;
  @Autowired
  private XmlFormatter xmlFormatter;
  @Autowired
  private RegistryClient registryClient;
  @Autowired
  private RegistryDomainProvider registryDomainProvider;

  /**
   * Initialize and run Spring application.
   *
   * @param args
   *     Command-line arguments.
   */
  public static void main(String[] args) {
    System.out.println("Starting API Validator..."); // NOPMD
    if (Security.getProvider(BouncyCastleProvider.PROVIDER_NAME) == null) {
      Security.addProvider(new BouncyCastleProvider());
    }

    Locale.setDefault(Locale.US);
    System.setProperty("spring.profiles.active", "console");
    System.setProperty("logging.level.", "ERROR"); // Set logging level in all packages to ERROR

    // --registry-domain is a convenience wrapper around setting environment variable for Spring.
    for (String arg : args) {
      if (arg.matches("^--registry-domain=.+$")) {
        String[] registryUrl = arg.split("=", 2);
        System.setProperty("app.registry-domain", registryUrl[1]);
      }
    }

    SpringApplication app = new SpringApplication(ConsoleApplication.class);
    app.setWebApplicationType(WebApplicationType.NONE);
    app.setBannerMode(Banner.Mode.OFF);
    app.setLogStartupInfo(false);
    app.run(args);
  }

  @Override
  public void run(ApplicationArguments args) {
    ConsoleValidator consoleValidator = new ConsoleValidator();

    consoleValidator.performValidation(args, this.apiValidatorsManager, this.docBuilder,
        this.validatorKeyStoreSet, this.parser, this.xmlFormatter, this.registryClient,
        this.registryDomainProvider.getRegistryDomain());
  }

}
