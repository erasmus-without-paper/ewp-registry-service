package eu.erasmuswithoutpaper.registry.echovalidator;

import java.io.IOException;
import java.security.KeyPair;
import java.util.Collection;
import java.util.List;

import eu.erasmuswithoutpaper.registry.internet.Request;
import eu.erasmuswithoutpaper.registry.internet.Response;
import eu.erasmuswithoutpaper.registry.internet.sec.ChainingRequestAuthorizer;
import eu.erasmuswithoutpaper.registry.internet.sec.ChainingResponseSigner;
import eu.erasmuswithoutpaper.registry.internet.sec.EwpCertificateRequestAuthorizer;
import eu.erasmuswithoutpaper.registry.internet.sec.EwpClient;
import eu.erasmuswithoutpaper.registry.internet.sec.EwpClientWithCertificate;
import eu.erasmuswithoutpaper.registry.internet.sec.EwpClientWithRsaKey;
import eu.erasmuswithoutpaper.registry.internet.sec.EwpHttpSigRequestAuthorizer;
import eu.erasmuswithoutpaper.registry.internet.sec.EwpHttpSigResponseSigner;
import eu.erasmuswithoutpaper.registry.internet.sec.EwpRsaAesRequestDecoder;
import eu.erasmuswithoutpaper.registry.internet.sec.EwpRsaAesResponseEncoder;
import eu.erasmuswithoutpaper.registry.internet.sec.GzipResponseEncoder;
import eu.erasmuswithoutpaper.registry.internet.sec.Http4xx;
import eu.erasmuswithoutpaper.registry.internet.sec.MultipleCodingsRequestDecoder;
import eu.erasmuswithoutpaper.registry.internet.sec.MultipleCodingsResponseEncoder;
import eu.erasmuswithoutpaper.registry.internet.sec.RequestAuthorizer;
import eu.erasmuswithoutpaper.registry.internet.sec.RequestCodingDecoder;
import eu.erasmuswithoutpaper.registry.internet.sec.RequestDecoder;
import eu.erasmuswithoutpaper.registry.internet.sec.ResponseCodingEncoder;
import eu.erasmuswithoutpaper.registry.internet.sec.ResponseEncoder;
import eu.erasmuswithoutpaper.registry.internet.sec.ResponseSigner;
import eu.erasmuswithoutpaper.registry.internet.sec.TlsResponseSigner;
import eu.erasmuswithoutpaper.registryclient.RegistryClient;

import org.assertj.core.util.Lists;

public class ServiceMMMMValid extends AbstractEchoV2Service {

  public ServiceMMMMValid(String url, RegistryClient registryClient, List<KeyPair> serverKeys) {
    super(url, registryClient, serverKeys);
    if (serverKeys.size() == 0) {
      throw new RuntimeException();
    }
  }

  protected Collection<String> getHeiIds(EwpClient clientId) {
    if (clientId instanceof EwpClientWithCertificate) {
      EwpClientWithCertificate clientWithCert = (EwpClientWithCertificate) clientId;
      return this.registryClient.getHeisCoveredByCertificate(clientWithCert.getCertificate());
    }
    if (clientId instanceof EwpClientWithRsaKey) {
      EwpClientWithRsaKey clientWithRsaKey = (EwpClientWithRsaKey) clientId;
      return this.registryClient.getHeisCoveredByClientKey(clientWithRsaKey.getRsaPublicKey());
    }
    // Unknown client subclass. Shouldn't happen.
    return Lists.emptyList();
  }

  protected RequestAuthorizer getRequestAuthorizer() {
    RequestAuthorizer tlscert = new EwpCertificateRequestAuthorizer(this.registryClient);
    RequestAuthorizer httpsig = new EwpHttpSigRequestAuthorizer(this.registryClient);
    return new ChainingRequestAuthorizer(Lists.newArrayList(tlscert, httpsig), httpsig);
  }

  protected RequestDecoder getRequestDecoder() {
    RequestCodingDecoder ewp = new EwpRsaAesRequestDecoder(this.serverKeys);
    return new MultipleCodingsRequestDecoder(Lists.newArrayList(ewp));
  }

  protected ResponseEncoder getResponseEncoder() {
    ResponseCodingEncoder ewp = new EwpRsaAesResponseEncoder(this.registryClient);
    ResponseCodingEncoder gzip = new GzipResponseEncoder();
    return new MultipleCodingsResponseEncoder(Lists.newArrayList(gzip, ewp), Lists.emptyList());
  }

  protected ResponseSigner getResponseSigner() {
    ResponseSigner httpsig = new EwpHttpSigResponseSigner(this.serverKeys.get(0));
    ResponseSigner tls = new TlsResponseSigner();
    return new ChainingResponseSigner(Lists.newArrayList(httpsig, tls));
  }

  @Override
  protected Response handleInternetRequest2(Request request)
      throws IOException, ErrorResponseException {

    if (!request.getUrl().startsWith(this.myEndpoint)) {
      return null;
    }

    /* Authorize and decode the request. Create valid response. */

    Response response;
    try {
      EwpClient clientId = this.getRequestAuthorizer().authorize(request);
      this.getRequestDecoder().decode(request);
      if (!(request.getMethod().equals("GET") || request.getMethod().equals("POST"))) {
        throw new Http4xx(405, "Unsupported HTTP method. Expecting GET or POST");
      }
      response = this.createEchoResponse(request, this.retrieveEchoValues(request),
          this.getHeiIds(clientId));
    } catch (Http4xx e) {
      /* Problems with authorization or decoding. We will return an error response. */
      response = e.generateEwpErrorResponse();
    }

    /*
     * At this point, response contain either a valid HTTP 200 response, or a HTTP 4xx error
     * response. It doesn't matter which one it is. Regardless, we still want to attempt to encode
     * (e.g. encrypt) and sign such response.
     */

    try {
      this.getResponseEncoder().encode(request, response);
    } catch (Http4xx e) {
      /*
       * Problems with encoding. Replace the previous response (which might have already contained
       * some other error response) with this one. We will still attempt to sign it at least.
       */
      response = e.generateEwpErrorResponse();
    }
    try {
      this.getResponseSigner().sign(request, response);
    } catch (Http4xx e) {
      /*
       * Problems with signing the response. Replace the previous response (which might have already
       * contained some other error response) with this one. We will also TRY to encode it...
       */
      response = e.generateEwpErrorResponse();
      try {
        this.getResponseEncoder().encode(request, response);
      } catch (Http4xx e2) {
        /*
         * So, we have problems with both signing AND encoding. We will ignore this second one, and
         * return the (unencoded) error message about the signing problem.
         */
      }
    }
    return response;
  }

}
