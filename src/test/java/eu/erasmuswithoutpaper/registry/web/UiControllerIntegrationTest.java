package eu.erasmuswithoutpaper.registry.web;

import static org.assertj.core.api.Assertions.assertThat;

import eu.erasmuswithoutpaper.registry.WRIntegrationTest;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import org.junit.jupiter.api.Test;

public class UiControllerIntegrationTest extends WRIntegrationTest {

  @Test
  public void testCss() {
    ResponseEntity<String> response = this.template.getForEntity("/style-SOME-SUFFIX.css",
        String.class);
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response.getBody()).contains("body {");
  }

  @Test
  public void testIndexPage() {
    ResponseEntity<String> response = this.template.getForEntity("/", String.class);
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response.getBody()).contains("<body>");
    assertThat(response.getBody()).contains("Registry Service");
  }

}
