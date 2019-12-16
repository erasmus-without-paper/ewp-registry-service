package eu.erasmuswithoutpaper.registry.web;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import javax.servlet.http.HttpServletRequest;

import eu.erasmuswithoutpaper.registry.common.Severity;
import eu.erasmuswithoutpaper.registry.notifier.NotifierFlag;
import eu.erasmuswithoutpaper.registry.notifier.NotifierService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.autoconfigure.web.ErrorController;
import org.springframework.core.io.ResourceLoader;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.apache.commons.io.IOUtils;

/**
 * Handles how errors are displayed.
 */
@Controller
@ConditionalOnWebApplication
public class MyErrorController implements ErrorController {

  private final ResourceLoader resLoader;
  private final NotifierFlag http500errorFlag;

  /**
   * @param resLoader
   *     needed to fetch the error XML template from the resources.
   * @param notifier
   *     needed to send error notifications.
   * @param adminEmails
   *     email address to notify on HTTP 500 errors.
   * @param useFlagToNotifyAboutExceptions
   *     is true then NotifierFlag will be used to inform admins
   *     about exceptions.
   */
  @Autowired
  @SuppressFBWarnings("SIC_INNER_SHOULD_BE_STATIC_ANON")
  public MyErrorController(ResourceLoader resLoader, NotifierService notifier,
      @Value("${app.admin-emails}") List<String> adminEmails,
      @Value("${app.use-flag-to-notify-about-exceptions}") boolean useFlagToNotifyAboutExceptions) {
    this.resLoader = resLoader;
    if (useFlagToNotifyAboutExceptions) {
      this.http500errorFlag = new NotifierFlag(adminEmails) {
        @Override
        public String getName() {
          return "Recently recorded runtime errors.";
        }
      };
      this.http500errorFlag.setStatus(Severity.OK);
      notifier.addWatchedFlag(this.http500errorFlag);
    } else {
      this.http500errorFlag = null;
    }
  }

  /**
   * Handle a server error.
   *
   * @param request
   *     request which has caused the error.
   * @return a HTTP 500 response (with EWP error XML).
   */
  @RequestMapping("/error")
  public ResponseEntity<String> error(HttpServletRequest request) {
    if (this.http500errorFlag != null) {
      this.http500errorFlag.setStatus(Severity.WARNING);
    }
    HttpHeaders headers = new HttpHeaders();
    String xml;
    try {
      xml =
          IOUtils.toString(this.resLoader.getResource("classpath:default-500.xml").getInputStream(),
              StandardCharsets.UTF_8);
      headers.setContentType(MediaType.APPLICATION_XML);
    } catch (IOException e) {
      xml = "Internal Server Error";
      headers.setContentType(MediaType.TEXT_PLAIN);
    }
    return new ResponseEntity<String>(xml, headers, HttpStatus.INTERNAL_SERVER_ERROR);
  }

  /**
   * Handle the "not found" error.
   *
   * @return a HTTP 404 response (with EWP error XML).
   */
  @RequestMapping("*")
  public ResponseEntity<String> get404() {
    String xml;
    try {
      xml =
          IOUtils.toString(this.resLoader.getResource("classpath:default-404.xml").getInputStream(),
              StandardCharsets.UTF_8);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_XML);
    return new ResponseEntity<String>(xml, headers, HttpStatus.NOT_FOUND);
  }

  @Override
  public String getErrorPath() {
    return "/error";
  }
}
