package eu.erasmuswithoutpaper.registry;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;

/**
 * A subclass of {@link WRTest} for integration testing.
 */
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT, classes = Application.class)
public abstract class WRIntegrationTest extends WRTest {

}
