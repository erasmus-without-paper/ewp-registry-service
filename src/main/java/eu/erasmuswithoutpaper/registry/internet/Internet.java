package eu.erasmuswithoutpaper.registry.internet;

import java.io.IOException;
import java.util.List;

/**
 * This interface will be used by all the other services for accessing resources over the Internet.
 * This allows to easily replace the Internet for tests.
 */
public interface Internet {

  /**
   * Fetch the contents of the given URL.
   *
   * @param url The URL at which the contents can be found.
   * @return The contents of the URL, fully loaded to the memory.
   * @throws IOException Numerous reasons, e.g.
   *         <ul>
   *         <li>there was a problem with the transport,</li>
   *         <li>the URL was invalid,</li>
   *         <li>the server responded with a status other than HTTP 200,</li>
   *         <li>if the URL uses a HTTPS scheme and server certificate has expired,</li>
   *         <li>etc.</li>
   *         </ul>
   */
  byte[] getUrl(String url) throws IOException;

  /**
   * Make an "advanced" request at the given URL. This method allows you to send and retrieve much
   * more data that the {@link #getUrl(String)} method does.
   *
   * @param request Description of the request to be made.
   * @return Description of the response returned by the server.
   * @throws IOException When the request is invalid, or the server could not be reached.
   */
  Response makeRequest(Request request) throws IOException;

  Response makeRequest(Request request, Integer timeout) throws IOException;

  /**
   * Enqueue an email for sending from the Registry Service to the given recipients. This method
   * should return immediately and never throw any exceptions.
   *
   * @param recipients A list of email addresses. These will be put into the "To" header of the sent
   *        email.
   * @param subject This will be put into the "Subject" header of the email.
   * @param contents The contents of the email. Plain-text.
   */
  void queueEmail(List<String> recipients, String subject, String contents);
}
