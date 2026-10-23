package eu.erasmuswithoutpaper.registry;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureTestRestTemplate;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;

/**
 * A subclass of {@link WRTest} for integration testing.
 */
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT, classes = Application.class)
@AutoConfigureTestRestTemplate
public abstract class WRIntegrationTest extends WRTest {

  @Autowired
  protected TestRestTemplate template;

}
