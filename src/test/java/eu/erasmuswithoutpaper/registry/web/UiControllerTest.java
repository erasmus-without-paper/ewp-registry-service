package eu.erasmuswithoutpaper.registry.web;

import static org.assertj.core.api.Assertions.assertThat;

import eu.erasmuswithoutpaper.registry.WRTest;
import eu.erasmuswithoutpaper.registry.documentbuilder.EwpDocBuilder;
import eu.erasmuswithoutpaper.registry.documentbuilder.KnownNamespace;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import org.junit.Test;

/**
 * Tests for {@link ApiController}.
 */
public class UiControllerTest extends WRTest {

  @Autowired
  private UiController uiController;

  /**
   * Make sure that the <code>/validate</code> endpoint is properly connected to the validator
   * exposed by {@link EwpDocBuilder} class.
   */
  @Test
  public void testXmlValidator() {
    StringBuilder sb = new StringBuilder();
    sb.append("<echo\n\nxmlns='");
    sb.append(KnownNamespace.APIENTRY_ECHO_V1.getNamespaceUri());
    sb.append("' someOtherAttribute='value'>\n<url>http://example.com/</url>\n</echo>");

    ResponseEntity<String> result = this.uiController.validateXml(sb.toString());
    assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(result.getHeaders().getContentType())
        .isEqualByComparingTo(MediaType.APPLICATION_JSON_UTF8);
    assertThat(result.getHeaders().getAccessControlAllowOrigin())
        .isEqualTo("http://developers.erasmuswithoutpaper.eu");
    String body = result.getBody();
    JsonElement aRoot = new JsonParser().parse(body);
    assertThat(aRoot.isJsonObject()).isTrue();
    assertThat(body).isEqualTo(this.getFileAsString("validatorResults/result1.json"));
  }
}
