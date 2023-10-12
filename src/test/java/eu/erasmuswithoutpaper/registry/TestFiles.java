package eu.erasmuswithoutpaper.registry;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import org.apache.commons.io.IOUtils;

public abstract class TestFiles {

  public static InputStream getFileAsStream(String filename) throws IOException {
    return TestFiles.class.getClassLoader().getResourceAsStream("test-files/" + filename);
  }

  public static byte[] getFile(String filename) throws IOException {
    return IOUtils.toByteArray(getFileAsStream(filename));
  }

  public static String getFileAsUtf8String(String filename) throws IOException {
    byte[] bytes = getFile(filename);
    return new String(bytes, StandardCharsets.UTF_8);
  }

}
