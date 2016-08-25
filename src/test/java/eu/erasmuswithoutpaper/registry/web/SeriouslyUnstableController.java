package eu.erasmuswithoutpaper.registry.web;

import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * For testing {@link RuntimeException}s.
 */
@Controller
@Profile("test")
public class SeriouslyUnstableController {

  /**
   * Throws {@link RuntimeException}, always.
   *
   * @return Nothing.
   */
  @RequestMapping("/throw-exception")
  public ResponseEntity<String> throwException() {
    throw new RuntimeException("This exception has been thrown on purpose.");
  }
}
