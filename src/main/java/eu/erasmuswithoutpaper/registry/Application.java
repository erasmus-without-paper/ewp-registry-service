package eu.erasmuswithoutpaper.registry;

import java.util.Locale;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.stereotype.Component;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;


/**
 * Our primary Spring application class.
 */
@SpringBootApplication
@EnableScheduling
public class Application {

  private static volatile String rootUrl = null;

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
  @SuppressFBWarnings("UPM_UNCALLED_PRIVATE_METHOD")
  public static String getRootUrl() {
    return rootUrl;
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
    rootUrl = newRootUrl;
    if (rootUrl.endsWith("/")) {
      rootUrl = rootUrl.substring(0, rootUrl.length() - 1);
    }
  }
}
