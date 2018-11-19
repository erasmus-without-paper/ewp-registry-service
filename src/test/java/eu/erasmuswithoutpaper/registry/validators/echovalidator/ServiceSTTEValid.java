package eu.erasmuswithoutpaper.registry.validators.echovalidator;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import eu.erasmuswithoutpaper.registry.common.Utils;
import eu.erasmuswithoutpaper.registry.internet.Request;
import eu.erasmuswithoutpaper.registry.internet.Response;
import eu.erasmuswithoutpaper.registry.internet.sec.EwpRsaAesResponseEncoder;
import eu.erasmuswithoutpaper.registry.internet.sec.GzipResponseEncoder;
import eu.erasmuswithoutpaper.registry.internet.sec.Http4xx;
import eu.erasmuswithoutpaper.registryclient.RegistryClient;

public class ServiceSTTEValid extends ServiceSTTTValid {

  public ServiceSTTEValid(String url, RegistryClient registryClient) {
    super(url, registryClient);
  }

  @Override
  public Response handleInternetRequest2(Request request)
      throws IOException, ErrorResponseException {
    Response response = super.handleInternetRequest2(request);

    // Make sure that the client wants us to encrypt the response.

    boolean encryptionRequested = this.wasEwpEncryptionRequested(request);
    if (!encryptionRequested) {
      return this.createErrorResponse(request, 406,
          "This endpoint requires the client to support response encryption.");
    }

    // Apply all codings

    List<String> codings = this.getCodingsToApply(request);
    for (String coding : codings) {
      try {
        this.applyCoding(request, response, coding);
      } catch (Http4xx e) {
        return e.generateEwpErrorResponse();
      }
    }

    return response;
  }

  private boolean wasEwpEncryptionRequested(Request request) {
    for (String coding : Utils.extractAcceptableCodings(request.getHeader("Accept-Encoding"))) {
      if (coding.equalsIgnoreCase("ewp-rsa-aes128gcm")) {
        return true;
      }
    }
    return false;
  }

  protected void applyCoding(Request request, Response response, String coding) throws Http4xx {
    if (coding.equalsIgnoreCase("gzip")) {
      this.applyGzip(response);
    } else if (coding.equalsIgnoreCase("ewp-rsa-aes128gcm")) {
      this.applyEwpEncryption(request, response);
    } else {
      // coding is provided by us, so this shouldn't happen.
      throw new RuntimeException("Unsupported coding: " + coding);
    }
  }

  protected void applyEwpEncryption(Request request, Response response) throws Http4xx {
    // Check for encryption key

    EwpRsaAesResponseEncoder encoder = this.getEwpRsaAesResponseEncoder();
    encoder.encode(request, response);
  }

  protected void applyGzip(Response response) {
    new GzipResponseEncoder().encode(response);
  }

  protected List<String> getCodingsToApply(Request request) {
    List<String> codings = new ArrayList<>();
    boolean acceptGzip = false;
    for (String coding : Utils.extractAcceptableCodings(request.getHeader("Accept-Encoding"))) {
      if (coding.equalsIgnoreCase("gzip")) {
        acceptGzip = true;
      }
    }
    if (acceptGzip) {
      codings.add("gzip");
    }
    codings.add("ewp-rsa-aes128gcm");
    return codings;
  }

  protected EwpRsaAesResponseEncoder getEwpRsaAesResponseEncoder() {
    return new EwpRsaAesResponseEncoder(this.registryClient);
  }
}
