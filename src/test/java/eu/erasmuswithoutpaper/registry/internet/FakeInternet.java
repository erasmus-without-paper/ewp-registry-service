package eu.erasmuswithoutpaper.registry.internet;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import com.google.common.base.Joiner;

/**
 * Implementation of {@link Internet} for unit tests.
 *
 * @see RealInternet
 */
@Service
@Profile("test")
public class FakeInternet implements Internet {

  private final ConcurrentMap<String, byte[]> map = new ConcurrentHashMap<String, byte[]>();
  private final List<String> emailsSent = new ArrayList<>();

  /**
   * Remove all the emails previously stored via {@link #queueEmail(List, String, String)}.
   */
  public void clearEmailsSent() {
    this.emailsSent.clear();
  }

  /**
   * Remove all stored URLs.
   */
  public void clearURLs() {
    this.map.clear();
  }

  /**
   * This implementation of {@link Internet#getUrl(String)} fetches contents from our storage
   * instead of from the net.
   */
  @Override
  public byte[] getUrl(String url) throws IOException {
    if (this.map.containsKey(url)) {
      return this.map.get(url);
    } else {
      throw new IOException("No such URL in our FakeInternet.");
    }
  }

  /**
   * Fetch the list of emails sent, and clear the list.
   *
   * @return The list emails sent, formatted as Strings, for easy testing.
   */
  public List<String> popEmailsSent() {
    List<String> copy = new ArrayList<>(this.emailsSent);
    this.emailsSent.clear();
    return copy;
  }

  /**
   * Put URL contents in the internet. These contents will now be accessible via
   * {@link #getUrl(String)} to all the other components which use an autowired {@link Internet}.
   *
   * @param url The URL to put the contents at.
   * @param contents The contents to be stored.
   */
  public void putURL(String url, byte[] contents) {
    this.map.put(url, contents);
  }

  /**
   * Same as {@link #putURL(String, byte[])}, but String is accepted.
   *
   * @param url The URL to put the contents at.
   * @param contents The contents to be stored (will be converted to UTF-8 bytes).
   */
  public void putURL(String url, String contents) {
    this.putURL(url, contents.getBytes(StandardCharsets.UTF_8));
  }

  /**
   * This implementation of {@link Internet#queueEmail(List, String, String)} stores the email on a
   * list (to be later retrieved via {@link #popEmailsSent()}), instead of actually sending it.
   */
  @Override
  public void queueEmail(List<String> recipients, String subject, String contents) {
    StringBuilder sb = new StringBuilder();
    sb.append("To: ");
    sb.append(Joiner.on(", ").join(recipients));
    sb.append("\n");
    sb.append("Subject: " + subject + "\n\n");
    sb.append(contents);
    this.emailsSent.add(sb.toString());
  }

  /**
   * @param url The URL to be removed from the internet.
   */
  public void removeURL(String url) {
    this.map.remove(url);
  }

}
