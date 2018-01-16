package eu.erasmuswithoutpaper.registry.internet.sec;

/**
 * Describes a server which has been recognized because the request was made over TLS, and the
 * server's host was properly verified.
 */
public class EwpServerWithCertificate extends EwpServer {

  private final String host;

  /**
   * @param host Hostname of the server.
   */
  public EwpServerWithCertificate(String host) {
    this.host = host;
  }

  /**
   * @return Hostname of the server.
   */
  public String getHost() {
    return this.host;
  }
}
