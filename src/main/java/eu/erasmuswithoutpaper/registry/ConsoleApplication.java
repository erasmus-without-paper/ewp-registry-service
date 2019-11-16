package eu.erasmuswithoutpaper.registry;

import java.security.Security;
import java.util.Locale;

import eu.erasmuswithoutpaper.registry.configuration.ConsoleConfiguration;
import eu.erasmuswithoutpaper.registry.configuration.ProductionConfiguration;
import eu.erasmuswithoutpaper.registry.consoleapplication.ConsoleValidator;
import eu.erasmuswithoutpaper.registry.documentbuilder.EwpDocBuilder;
import eu.erasmuswithoutpaper.registry.internet.RealInternet;
import eu.erasmuswithoutpaper.registry.updater.ManifestConverter;
import eu.erasmuswithoutpaper.registry.validators.ApiValidator;
import eu.erasmuswithoutpaper.registry.validators.ApiValidatorsManager;
import eu.erasmuswithoutpaper.registry.validators.ExternalValidatorKeyStore;
import eu.erasmuswithoutpaper.registry.xmlformatter.XmlFormatter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.Banner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.condition.ConditionalOnNotWebApplication;
import org.springframework.context.annotation.ComponentScan;

import org.bouncycastle.jce.provider.BouncyCastleProvider;


/**
 * Our secondary Spring application class.
 */
@ComponentScan(basePackageClasses = {
    ApiValidator.class,
    ConsoleConfiguration.class,
    ProductionConfiguration.class,
    EwpDocBuilder.class,
    XmlFormatter.class,
    ManifestConverter.class,
    RealInternet.class})
@ConditionalOnNotWebApplication
public class ConsoleApplication implements ApplicationRunner {
  @Autowired
  private ApiValidatorsManager apiValidatorsManager;
  @Autowired
  private ExternalValidatorKeyStore externalValidatorKeyStore;
  @Autowired
  private EwpDocBuilder docBuilder;
  @Autowired
  private ManifestConverter converter;
  @Autowired
  private XmlFormatter xmlFormatter;

  /**
   * Initialize and run Spring application.
   *
   * @param args
   *     Command-line arguments.
   */
  public static void main(String[] args) {
    if (Security.getProvider(BouncyCastleProvider.PROVIDER_NAME) == null) {
      Security.addProvider(new BouncyCastleProvider());
    }

    Locale.setDefault(Locale.US);
    System.setProperty("spring.profiles.active", "console");
    System.setProperty("logging.level.", "ERROR"); // Set logging level in all packages to ERROR
    SpringApplication app = new SpringApplication(ConsoleApplication.class);
    app.setWebEnvironment(false);
    app.setBannerMode(Banner.Mode.OFF);
    app.setLogStartupInfo(false);
    app.run(args);
  }

  @Override
  public void run(ApplicationArguments args) {
    ConsoleValidator consoleValidator = new ConsoleValidator();

    consoleValidator.performValidation(
        args, this.apiValidatorsManager, this.docBuilder, this.externalValidatorKeyStore,
        this.converter, this.xmlFormatter
    );
  }

}
