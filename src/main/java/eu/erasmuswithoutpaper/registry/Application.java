package eu.erasmuswithoutpaper.registry;

import java.util.Locale;
import java.util.Objects;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.stereotype.Component;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;


/**
 * Our primary Spring application class.
 */
@ConditionalOnWebApplication
@EnableScheduling
@SpringBootApplication
public class Application {

  private static volatile String rootUrl = null;
  private static volatile String productionUrl = null;
  private static volatile String registryRepoBaseUrl = null;
  private static volatile String ewpSchemaBaseUrl = null;

  /**
   * Return the value of <code>app.root-url</code> property, without the trailing slash.
   *
   * <p>
   * <b>Important:</b> This method MAY return <b>null</b> if it is called before Spring finishes
   * initializing its {@link ApplicationContext}. Therefore, you should avoid calling it in
   * {@link Component} constructors (use {@link Autowired} there instead).
   * </p>
   *
   * @return String or null.
   */
  public static String getRootUrl() {
    return rootUrl;
  }

  /**
   * Return the value of <code>app.registry-repo-base-url</code> property, without the trailing
   * slash.
   *
   * @return String or null.
   */
  public static String getRegistryRepoBaseUrl() {
    return registryRepoBaseUrl;
  }

  /**
   * Return the value of <code>app.ewp-schema-location-url</code> property, without the
   * trailing slash.
   *
   * @return String or null.
   */
  public static String getEwpSchemaBaseUrl() {
    return ewpSchemaBaseUrl;
  }

  /**
   * @return True, if this is the official production site.
   */
  public static boolean isProductionSite() {
    return isProductionSite(getRootUrl());
  }

  /**
   * @param urlToCheck URL to check.
   * @return True, if urlToCheck is the official production site.
   */
  public static boolean isProductionSite(String urlToCheck) {
    return Objects.equals(urlToCheck, productionUrl);
  }

  /**
   * @return True, if validator should be available on this website.
   */
  public static boolean isValidationEnabled() {
    return !isProductionSite();
  }

  /**
   * Initialize and run Spring application.
   *
   * @param args Command-line arguments.
   */
  public static void main(String[] args) {
    Locale.setDefault(Locale.US);
    SpringApplication app = new SpringApplication(Application.class);
    app.run(args);
  }

  @Autowired
  @SuppressFBWarnings({ "ST_WRITE_TO_STATIC_FROM_INSTANCE_METHOD", "UPM_UNCALLED_PRIVATE_METHOD" })
  private void setRootUrl(@Value("${app.root-url}") String newRootUrl) { // NOPMD
    rootUrl = removeSlash(newRootUrl);
  }

  @Autowired
  @SuppressFBWarnings({ "ST_WRITE_TO_STATIC_FROM_INSTANCE_METHOD", "UPM_UNCALLED_PRIVATE_METHOD" })
  private void setProductionUrl( // NOPMD
      @Value("${app.registry-production-url}") String newProductionUrl) {
    productionUrl = removeSlash(newProductionUrl);
  }

  @Autowired
  @SuppressFBWarnings({ "ST_WRITE_TO_STATIC_FROM_INSTANCE_METHOD", "UPM_UNCALLED_PRIVATE_METHOD" })
  private void setRepoUrl(@Value("${app.registry-repo-base-url}") String newRepoBaseUrl) { // NOPMD
    registryRepoBaseUrl = removeSlash(newRepoBaseUrl);
  }

  @Autowired
  @SuppressFBWarnings({ "ST_WRITE_TO_STATIC_FROM_INSTANCE_METHOD", "UPM_UNCALLED_PRIVATE_METHOD" })
  private void setSchemaLocationUrl( // NOPMD
      @Value("${app.ewp-schema-base-url}") String newSchemaBaseUrl) {
    ewpSchemaBaseUrl = removeSlash(newSchemaBaseUrl);
  }

  private String removeSlash(String str) {
    if (str.endsWith("/")) {
      return str.substring(0, str.length() - 1);
    }

    return str;
  }
}
