package eu.erasmuswithoutpaper.registry.internet;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
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

  /**
   * Thrown when multiple handlers wanted to handle the same one request.
   */
  public static class MultipleHandlersConflict extends RuntimeException {
    public MultipleHandlersConflict() {
      super("Multiple handlers wanted to handle the same one request");
    }
  }

  public static class NotFound extends IOException {
    public NotFound() {
      super("No such URL in our FakeInternet.");
    }
  }

  private final ConcurrentMap<String, byte[]> map = new ConcurrentHashMap<String, byte[]>();
  private final Set<FakeInternetService> services;
  private final List<String> emailsSent = new ArrayList<>();

  public FakeInternet() {
    this.services = Collections.synchronizedSet(new HashSet<>());
  }

  /**
   * Add a service to this Internet.
   *
   * <p>
   * This is a bit more sophisticated alternative to the {@link #putURL(String, String)} method. It
   * allows the caller to handle more detailed requests and responses. (In particular, this was
   * added for testing the Echo Validator.)
   * </p>
   *
   * @param service The service instance to be added.
   */
  public void addFakeInternetService(FakeInternetService service) {
    if (!this.services.add(service)) {
      throw new RuntimeException("This service has already been added");
    }
  }

  /**
   * Remove everything from this Internet (URLs, emails, services, etc.).
   */
  public void clearAll() {
    this.clearEmailsSent();
    this.clearFakeInternetServices();
    this.clearURLs();
  }

  /**
   * Remove all the emails previously stored via {@link #queueEmail(List, String, String)}.
   */
  public void clearEmailsSent() {
    this.emailsSent.clear();
  }

  /**
   * Remove all services previously added via {@link #addFakeInternetService(FakeInternetService)}.
   */
  public void clearFakeInternetServices() {
    this.services.clear();
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
    Response response = this.makeRequest(new Request("GET", url));
    return response.getBody();
  }

  @Override
  public Response makeRequest(Request request) throws IOException {
    List<Response> responses = new ArrayList<>();
    List<IOException> exceptions = new ArrayList<>();
    byte[] value = this.map.get(request.getUrl());
    if (value != null) {
      // Found this URL in our URL map. Creating a virtual response instance.
      responses.add(new Response(request, 200, value));
    }
    for (FakeInternetService service : this.services) {
      try {
        Response response = service.handleInternetRequest(request);
        if (response != null) {
          responses.add(response);
        }
      } catch (IOException e) {
        exceptions.add(e);
      }
    }
    int sum = responses.size() + exceptions.size();
    if (sum == 0) {
      // None of the handlers responded for this request.
      throw new NotFound();
    } else if (sum == 1) {
      if (responses.size() > 0) {
        return responses.get(0);
      } else {
        throw exceptions.get(0);
      }
    } else {
      // This situation is invalid. Some tests need fixing.
      throw new MultipleHandlersConflict();
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
   * Remove a service previously added via {@link #addFakeInternetService(FakeInternetService)}.
   *
   * @param service Service instance to be removed.
   */
  public void removeFakeInternetService(FakeInternetService service) {
    this.services.remove(service);
  }

  /**
   * @param url The URL to be removed from the internet.
   */
  public void removeURL(String url) {
    this.map.remove(url);
  }

}
