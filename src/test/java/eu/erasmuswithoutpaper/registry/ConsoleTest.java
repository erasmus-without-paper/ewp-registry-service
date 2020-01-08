package eu.erasmuswithoutpaper.registry;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE, classes = ConsoleApplication.class)
@ActiveProfiles("console")
public class ConsoleTest {
    @Autowired
    private ConsoleApplication consoleApplication;
    @Test
    public void testConsoleApplicationRun() {
        consoleApplication.run(new DefaultApplicationArguments(new String[]{}));
    }
}
