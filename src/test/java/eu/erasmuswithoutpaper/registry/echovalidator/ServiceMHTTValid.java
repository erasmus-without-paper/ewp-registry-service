package eu.erasmuswithoutpaper.registry.echovalidator;

import java.io.IOException;
import java.security.KeyPair;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

import eu.erasmuswithoutpaper.registry.internet.Internet.Request;
import eu.erasmuswithoutpaper.registry.internet.Internet.Response;
import eu.erasmuswithoutpaper.registryclient.RegistryClient;

import net.adamcin.httpsig.api.Authorization;
import org.assertj.core.util.Lists;

public class ServiceMHTTValid extends ServiceMTTTValid {

  private final String myKeyId;
  private final KeyPair myKeyPair;

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
      this.includeProperHeaders(request, response);
    }
    return response;
  }

  protected boolean decideIfWeWantToSign(Request request) {
    // This implementation always signs.
    return true;
  }

  protected ZonedDateTime getCurrentTime() {
    return ZonedDateTime.now(ZoneId.of("UTC"));
  }

  protected List<String> getHeaderCandidatesToSign() {
    return Lists.newArrayList("date", "original-date", "digest", "x-request-id",
        "x-request-signature");
  }

  protected void includeDateHeaders(Response response, boolean date, boolean originalDate) {
    String now = DateTimeFormatter.RFC_1123_DATE_TIME.format(this.getCurrentTime());
    if (date) {
      response.putHeader("Date", now);
    }
    if (originalDate) {
      response.putHeader("Original-Date", now);
    }
  }

  protected void includeDigestHeader(Response response) {
    response.recomputeAndAttachDigestHeader();
    // Also add a digest in an unknown algorithm. This is valid, and shouldn't "break"
    // the validator.
    response.putHeader("Digest", response.getHeader("Digest") + ", Unknown-Algorithm=Value");
  }

  protected void includeProperHeaders(Request request, Response response) {
    this.includeDateHeaders(response, true, false);
    this.includeDigestHeader(response);
    this.includeXRequestIdHeader(request, response);
    this.includeXRequestSignature(request, response);
    this.includeSignatureHeader(response);
  }

  protected void includeSignatureHeader(Response response) {
    List<String> headersToSign = new ArrayList<>();
    for (String headerName : this.getHeaderCandidatesToSign()) {
      if (response.getHeader(headerName) != null) {
        headersToSign.add(headerName);
      }
    }
    response.recomputeAndAttachSignatureHeader(this.myKeyId, this.myKeyPair, headersToSign);
  }

  protected void includeXRequestIdHeader(Request request, Response response) {
    String reqId = request.getHeader("X-Request-Id");
    if (reqId != null) {
      response.putHeader("X-Request-Id", reqId);
    }
  }

  protected void includeXRequestSignature(Request request, Response response) {
    String authzString = request.getHeader("Authorization");
    if (authzString == null) {
      return;
    }
    Authorization authz = Authorization.parse(authzString);
    if (authz == null) {
      return;
    }
    if (authz.getSignature() == null) {
      return;
    }
    response.putHeader("X-Request-Signature", authz.getSignature());
  }

}
