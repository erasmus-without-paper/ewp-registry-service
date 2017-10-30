package eu.erasmuswithoutpaper.registry.web;

import static org.assertj.core.api.Assertions.assertThat;

import eu.erasmuswithoutpaper.registry.WRIntegrationTest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import org.junit.Test;


public class UiControllerIntegrationTest extends WRIntegrationTest {

  @Autowired
  private TestRestTemplate template;

  @Test
  public void testCss() {
    ResponseEntity<String> response =
        this.template.getForEntity(this.baseURL + "/style-SOME-SUFFIX.css", String.class);
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response.getBody()).contains("body {");
  }

  @Test
  public void testIndexPage() {
    ResponseEntity<String> response = this.template.getForEntity(this.baseURL + "/", String.class);
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response.getBody()).contains("<body>");
    assertThat(response.getBody()).contains("Registry Service");
  }

}
