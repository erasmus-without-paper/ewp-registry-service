package eu.erasmuswithoutpaper.registry;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import org.junit.jupiter.api.Test;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE, classes = ConsoleApplication.class)
@ActiveProfiles("console")
public class ConsoleTest {
    @Autowired
    private ConsoleApplication consoleApplication;
    @Test
    public void testConsoleApplicationRun() {
      consoleApplication.run(new DefaultApplicationArguments());
    }
}
