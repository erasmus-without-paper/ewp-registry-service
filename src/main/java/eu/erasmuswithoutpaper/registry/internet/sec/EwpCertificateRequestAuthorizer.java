package eu.erasmuswithoutpaper.registry.internet.sec;

import java.security.cert.X509Certificate;

import eu.erasmuswithoutpaper.registry.internet.Request;
import eu.erasmuswithoutpaper.registryclient.RegistryClient;

/**
 * This particular {@link RequestAuthorizer} expects the request to made made with TLS Client
 * Certificates recognized by EWP Registry Service. It will throw {@link Http4xx} exceptions
 * otherwise.
 */
public class EwpCertificateRequestAuthorizer implements RequestAuthorizer {

  private final RegistryClient registryClient;

  /**
   * @param registryClient Needed to verify if the certificates were published in the EWP Registry
   *        Service.
   */
  public EwpCertificateRequestAuthorizer(RegistryClient registryClient) {
    this.registryClient = registryClient;
  }

  @Override
  public EwpClientWithCertificate authorize(Request request) throws Http4xx {
    if (!request.getClientCertificate().isPresent()) {
      throw new UnmatchedRequestAuthorizationMethod(403,
          "Expecting client certificate to be used for TLS transport.");
    }
    X509Certificate cert = request.getClientCertificate().get();
    if (!this.registryClient.isCertificateKnown(cert)) {
      throw new Http4xx(403,
          "Unknown client certificate (could not find it amongst " + "registered EWP members).");
    }
    EwpClientWithCertificate clientId = new EwpClientWithCertificate(cert);
    request.addProcessingNoticeHtml(
        "Request has been successfully authenticated with TLS certificate. Client identified: "
            + clientId);
    return clientId;
  }

}
