package eu.erasmuswithoutpaper.registry.web;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import javax.servlet.http.HttpServletRequest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.web.ErrorController;
import org.springframework.core.io.ResourceLoader;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

import org.apache.commons.io.IOUtils;

/**
 * Handles how errors are displayed.
 */
@Controller
public class MyErrorController implements ErrorController {

  private final ResourceLoader resLoader;

  /**
   * @param resLoader needed to fetch the error XML template from the resources.
   */
  @Autowired
  public MyErrorController(ResourceLoader resLoader) {
    this.resLoader = resLoader;
  }

  /**
   * Handle a server error.
   *
   * @param request request which has caused the error.
   * @return a HTTP 500 response (with EWP error XML).
   */
  @RequestMapping("/error")
  public ResponseEntity<String> error(HttpServletRequest request) {
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
