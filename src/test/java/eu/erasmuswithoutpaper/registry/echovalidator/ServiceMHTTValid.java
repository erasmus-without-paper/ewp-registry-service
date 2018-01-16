package eu.erasmuswithoutpaper.registry.echovalidator;

import java.io.IOException;
import java.security.KeyPair;

import eu.erasmuswithoutpaper.registry.internet.Request;
import eu.erasmuswithoutpaper.registry.internet.Response;
import eu.erasmuswithoutpaper.registry.internet.sec.EwpHttpSigResponseSigner;
import eu.erasmuswithoutpaper.registry.internet.sec.Http4xx;
import eu.erasmuswithoutpaper.registry.internet.sec.TlsResponseSigner;
import eu.erasmuswithoutpaper.registryclient.RegistryClient;

public class ServiceMHTTValid extends ServiceMTTTValid {

  protected final String myKeyId;
  protected final KeyPair myKeyPair;
  private EwpHttpSigResponseSigner mySignerCache;
  private TlsResponseSigner myTlsSignerCache;

  public ServiceMHTTValid(String url, RegistryClient registryClient, String myKeyId,
      KeyPair myKeyPair) {
    super(url, registryClient);
    this.myKeyId = myKeyId;
    this.myKeyPair = myKeyPair;
  }

  @Override
  public Response handleInternetRequest2(Request request)
      throws IOException, ErrorResponseException {
    if (!request.getUrl().startsWith(this.myEndpoint)) {
      return null;
    }
    Response response;
    try {
      response = super.handleInternetRequest2(request);
    } catch (ErrorResponseException e) {
      // We need to sign error responses too.
      response = e.response;
    }
    if (this.decideIfWeWantToSign(request)) {
      this.getHttpSigSigner().sign(request, response);
    } else {
      try {
        this.getTlsSigner().sign(request, response);
      } catch (Http4xx e) {
        return e.generateEwpErrorResponse();
      }
    }
    return response;
  }

  protected boolean decideIfWeWantToSign(Request request) {
    // This implementation always signs.
    return true;
  }

  protected EwpHttpSigResponseSigner getHttpSigSigner() {
    if (this.mySignerCache == null) {
      this.mySignerCache = new EwpHttpSigResponseSigner(this.myKeyId, this.myKeyPair);
    }
    return this.mySignerCache;
  }

  protected TlsResponseSigner getTlsSigner() {
    if (this.myTlsSignerCache == null) {
      this.myTlsSignerCache = new TlsResponseSigner();
    }
    return this.myTlsSignerCache;
  }

}
