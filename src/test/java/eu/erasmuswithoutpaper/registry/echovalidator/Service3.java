package eu.erasmuswithoutpaper.registry.echovalidator;

import java.io.IOException;
import java.security.cert.X509Certificate;
import java.util.List;

import eu.erasmuswithoutpaper.registry.internet.Internet.Request;
import eu.erasmuswithoutpaper.registry.internet.Internet.Response;
import eu.erasmuswithoutpaper.registryclient.RegistryClient;

import org.assertj.core.util.Lists;

/**
 * Internal "fake" implementation of an INVALID Version 1 Echo API endpoint.
 *
 * <p>
 * Mistakes: Accepts HTTP methods other than GET and POST. It always returns a hardcoded set of echo
 * values.
 * </p>
 */
public class Service3 extends AbstractEchoV1Service {

  public Service3(String url, RegistryClient registryClient) {
    super(url, registryClient);
  }

  @Override
  public Response handleInternetRequest(Request request) throws IOException {
    if (!request.getUrl().startsWith(this.myEndpoint)) {
      return null;
    }
    if (!request.getClientCertificate().isPresent()) {
      return this.createErrorResponse(403, "Expecting client certificate.");
    }
    X509Certificate cert = request.getClientCertificate().get();
    if (!this.registryClient.isCertificateKnown(cert)) {
      return this.createErrorResponse(403, "Unknown client certificate.");
    }
    List<String> echos = Lists.newArrayList("a", "b");
    return this.createEchoResponse(echos, this.registryClient.getHeisCoveredByCertificate(cert));
  }

}
