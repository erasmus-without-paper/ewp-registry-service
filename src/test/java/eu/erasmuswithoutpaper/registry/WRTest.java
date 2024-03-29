package eu.erasmuswithoutpaper.registry;

import java.io.IOException;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/**
 * A common test class to extend. Should be used for all unit tests.
 */
@SpringBootTest(classes = Application.class)
@ActiveProfiles("test")
public abstract class WRTest {

  /**
   * Quick way of fetching files from resources.
   *
   * @param filename A path relative to "test-files" directory. The file must exist.
   * @return The contents of the file.
   */
  protected byte[] getFile(String filename) {
    try {
      return TestFiles.getFile(filename);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * Same as {@link #getFile(String)}, but converts the file to String.
   *
   * @param filename as in {@link #getFile(String)}.
   * @return Contents transformed to a string (with UTF-8 encoding).
   */
  protected String getFileAsString(String filename) {
    try {
      return TestFiles.getFileAsUtf8String(filename);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

}
