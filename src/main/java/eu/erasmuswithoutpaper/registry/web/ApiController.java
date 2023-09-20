package eu.erasmuswithoutpaper.registry.web;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import eu.erasmuswithoutpaper.registry.repository.CatalogueNotFound;
import eu.erasmuswithoutpaper.registry.repository.ManifestRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.core.io.ResourceLoader;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import org.apache.commons.io.IOUtils;

/**
 * Controller for all official
 * <a href='https://github.com/erasmus-without-paper/ewp-specs-api-registry'>Registry API</a>
 * endpoints.
 */
@RestController
@ConditionalOnWebApplication
public class ApiController {

  private final ManifestRepository repo;
  private final SelfManifestProvider selfManifestProvider;
  private final ResourceLoader resLoader;

  public static class ManifestNotFoundException extends RuntimeException {
    private static final long serialVersionUID = 4099597392377045829L;

    ManifestNotFoundException(String manifestName) {
      super("Manifest " + manifestName + " does not exist.");
    }
  }

  /**
   * @param repo
   *     Required to fetch the current catalogue contents.
   * @param selfManifestProvider
   *     Required to fetch Registry's own manifest contents.
   * @param resLoader
   *     Needed in order to load XML templates for error responses.
   */
  @Autowired
  public ApiController(ManifestRepository repo, SelfManifestProvider selfManifestProvider,
      ResourceLoader resLoader) {
    this.repo = repo;
    this.selfManifestProvider = selfManifestProvider;
    this.resLoader = resLoader;
  }

  /**
   * @return a HTTP response with the catalogue contents.
   */
  @RequestMapping("/catalogue-v1.xml")
  public ResponseEntity<String> getCatalogue() {
    try {
      HttpHeaders headers = new HttpHeaders();
      headers.setCacheControl("max-age=300, must-revalidate");
      headers.setContentType(MediaType.APPLICATION_XML);
      headers.setExpires(System.currentTimeMillis() + 300_000);
      return new ResponseEntity<String>(this.repo.getCatalogue(), headers, HttpStatus.OK);
    } catch (CatalogueNotFound e) {
      String xml;
      try {
        xml = IOUtils.toString(
            this.resLoader.getResource("classpath:default-503.xml").getInputStream(),
            StandardCharsets.UTF_8);
      } catch (IOException e2) {
        xml = "Internal Server Error";
      }
      HttpHeaders headers = new HttpHeaders();
      headers.setContentType(MediaType.APPLICATION_XML);
      return new ResponseEntity<String>(xml, headers, HttpStatus.SERVICE_UNAVAILABLE);
    }
  }

  /**
   * @return a HTTP response with Registry's own self-manifest.
   */
  @RequestMapping("/manifest-{manifestName}.xml")
  public ResponseEntity<String> getSelfManifest(@PathVariable String manifestName) {
    String manifest = this.selfManifestProvider.getManifests().get(manifestName);

    // Return 404 if manifest name is not known.

    if (manifest == null) {
      throw new ManifestNotFoundException(manifestName);
    }

    HttpHeaders headers = new HttpHeaders();
    headers.setCacheControl("max-age=0, must-revalidate");
    headers.setContentType(MediaType.APPLICATION_XML);
    headers.setExpires(0);
    return new ResponseEntity<String>(manifest, headers, HttpStatus.OK);
  }
}
