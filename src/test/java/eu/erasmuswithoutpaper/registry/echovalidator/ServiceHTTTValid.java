package eu.erasmuswithoutpaper.registry.echovalidator;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.interfaces.RSAPublicKey;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import eu.erasmuswithoutpaper.registry.common.Utils;
import eu.erasmuswithoutpaper.registry.internet.HttpSigRsaPublicKey;
import eu.erasmuswithoutpaper.registry.internet.Request;
import eu.erasmuswithoutpaper.registry.internet.Response;
import eu.erasmuswithoutpaper.registryclient.RegistryClient;

import net.adamcin.httpsig.api.Algorithm;
import net.adamcin.httpsig.api.Authorization;
import net.adamcin.httpsig.api.Challenge;
import net.adamcin.httpsig.api.DefaultKeychain;
import net.adamcin.httpsig.api.DefaultVerifier;
import net.adamcin.httpsig.api.RequestContent;
import net.adamcin.httpsig.api.VerifyResult;
import org.assertj.core.util.Lists;

/**
 * Internal "fake" implementation of a valid HTTT API endpoint.
 */
public class ServiceHTTTValid extends AbstractEchoV2Service {

  public ServiceHTTTValid(String url, RegistryClient registryClient) {
    super(url, registryClient);
  }

  @Override
  public Response handleInternetRequest2(Request request)
      throws IOException, ErrorResponseException {

    if (!request.getUrl().startsWith(this.myEndpoint)) {
      return null;
    }

    this.verifyHttpMethod(request);
    Authorization authz = this.verifyHttpSignatureAuthorizationHeader(request);
    this.verifyHostHeader(request);
    RSAPublicKey clientKey = this.verifyClientKeyId(request, authz.getKeyId());
    this.verifyDateAndOriginalDateHeaders(request);
    this.verifyRequestIdHeader(request);
    this.verifySignature(request, clientKey, authz);
    this.verifyDigestHeader(request);
    return this.createEchoResponse(request, this.retrieveEchoValues(request),
        this.identifyCoveredHeis(clientKey));
  }

  protected Response createHttpSig401Response(Request request) {
    Response response = this.createErrorResponse(request, 401,
        "This endpoint requires HTTP Signature Authorization, as specified here: "
            + "https://github.com/erasmus-without-paper/ewp-specs-sec-cliauth-httpsig");
    response.putHeader("WWW-Authenticate", "Signature realm=\"EWP\"");
    response.putHeader("Want-Digest", "SHA-256");
    return response;
  }

  protected String extractHost(String url) {
    URL parsed;
    try {
      parsed = new URL(url);
    } catch (MalformedURLException e) {
      throw new RuntimeException(e);
    }
    return parsed.getHost();
  }

  protected String findErrorsInDateHeader(String dateValue) {
    return EchoValidationSuite.findErrorsInDateHeader(dateValue);
  }

  protected List<String> getAcceptedHttpMethods() {
    return Lists.newArrayList("GET", "POST");
  }

  protected List<String> getHeadersThatNeedToBeSignedExcludingDates() {
    return Lists.newArrayList("(request-target)", "host", "digest", "x-request-id");
  }

  protected List<Algorithm> getSupportedHttpsigAlgorithms() {
    return Lists.newArrayList(Algorithm.RSA_SHA256);
  }

  protected Collection<String> identifyCoveredHeis(RSAPublicKey clientKey) {
    return this.registryClient.getHeisCoveredByClientKey(clientKey);
  }

  protected RSAPublicKey verifyClientKeyId(Request request, String keyId)
      throws ErrorResponseException {
    RSAPublicKey clientKey = this.registryClient.findRsaPublicKey(keyId);
    if (clientKey == null || (!this.registryClient.isClientKeyKnown(clientKey))) {
      throw new ErrorResponseException(
          this.createErrorResponse(request, 403, "Unknown client key: " + keyId));
    }
    return clientKey;
  }

  protected void verifyDateAndOriginalDateHeaders(Request request) throws ErrorResponseException {
    List<String> dateHeadersToVerify = new ArrayList<>();
    if (request.getHeader("Date") != null) {
      dateHeadersToVerify.add("Date");
    }
    if (request.getHeader("Original-Date") != null) {
      dateHeadersToVerify.add("Original-Date");
    }
    if (dateHeadersToVerify.size() == 0) {
      throw new ErrorResponseException(this.createErrorResponse(request, 400,
          "This endpoint requires your request to include the \"Date\" "
              + "header or the \"Original-Date\" (or both)."));
    }
    for (String headerName : dateHeadersToVerify) {
      String errorMessage = this.findErrorsInDateHeader(request.getHeader(headerName));
      if (errorMessage != null) {
        throw new ErrorResponseException(this.createErrorResponse(request, 400,
            "The value of the \"" + headerName + "\" failed verification: " + errorMessage));
      }
    }
  }

  protected void verifyDigestHeader(Request request) throws ErrorResponseException {
    String digestHeader = request.getHeader("Digest");
    if (digestHeader == null) {
      throw new ErrorResponseException(
          this.createErrorResponse(request, 400, "Missing header: Digest"));
    }
    String expectedSha256Digest = Utils.computeDigest(request.getBodyOrEmpty());
    if (!digestHeader.contains("SHA-256=" + expectedSha256Digest)) {
      throw new ErrorResponseException(this.createErrorResponse(request, 400,
          "Digest mismatch. Expected: " + expectedSha256Digest));
    }
  }

  protected void verifyHostHeader(Request request) throws ErrorResponseException {
    if (!this.extractHost(request.getUrl()).equals(request.getHeader("Host"))) {
      throw new ErrorResponseException(this.createErrorResponse(request, 400,
          "Request's \"host\" header is either missing or it doesn't match what we expect."));
    }
  }

  protected void verifyHttpMethod(Request request) throws ErrorResponseException {
    if (!this.getAcceptedHttpMethods().contains(request.getMethod())) {
      throw new ErrorResponseException(this.createErrorResponse(request, 405,
          "Accepted HTTP methods: " + this.getAcceptedHttpMethods()));
    }

  }

  protected Authorization verifyHttpSignatureAuthorizationHeader(Request request)
      throws ErrorResponseException {
    String authHeader = request.getHeader("Authorization");
    if ((authHeader == null) || (!authHeader.toLowerCase().startsWith("signature "))) {
      throw new ErrorResponseException(this.createHttpSig401Response(request));
    }

    Authorization authz = Authorization.parse(request.getHeader("Authorization"));
    if (authz == null) {
      throw new ErrorResponseException(
          this.createErrorResponse(request, 400, "Could not parse the Authorization header"));
    }
    if (!this.getSupportedHttpsigAlgorithms().contains(authz.getAlgorithm())) {
      throw new ErrorResponseException(this.createErrorResponse(request, 400,
          "This endpoint requires HTTP Signature to use one of the following algorithms: "
              + String.join(", ", this.getSupportedHttpsigAlgorithms().stream()
                  .map(Object::toString).collect(Collectors.toList()))));
    }
    for (String requiredHeader : this.getHeadersThatNeedToBeSignedExcludingDates()) {
      if (!authz.getHeaders().contains(requiredHeader)) {
        throw new ErrorResponseException(this.createErrorResponse(request, 400,
            "This endpoint requires your HTTP Signature to cover the \"" + requiredHeader
                + "\" header."));
      }
    }
    if ((!authz.getHeaders().contains("date")) && (!authz.getHeaders().contains("original-date"))) {
      throw new ErrorResponseException(this.createErrorResponse(request, 400,
          "This endpoint requires your HTTP Signature to cover the \"Date\" "
              + "header or the \"Original-Date\" (or both)."));
    }
    return authz;
  }

  protected void verifyRequestIdHeader(Request request) throws ErrorResponseException {
    String value = request.getHeader("X-Request-Id");
    if (value == null) {
      throw new ErrorResponseException(
          this.createErrorResponse(request, 400, "Missing \"X-Request-Id\" header"));
    }
    if (!value.matches("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}")) {
      throw new ErrorResponseException(this.createErrorResponse(request, 400,
          "X-Request-Id must be an UUID formatted in canonical form"));
    }
  }

  protected void verifySignature(Request request, RSAPublicKey expectedClientKey,
      Authorization authz) throws ErrorResponseException {
    DefaultKeychain keychain = new DefaultKeychain();
    keychain.add(new HttpSigRsaPublicKey(expectedClientKey));
    DefaultVerifier verifier = new DefaultVerifier(keychain);

    RequestContent.Builder rcb = new RequestContent.Builder();
    rcb.setRequestTarget(request.getMethod(), request.getPathPseudoHeader());
    for (Map.Entry<String, String> entry : request.getHeaders().entrySet()) {
      rcb.addHeader(entry.getKey(), entry.getValue());
    }
    // The library needs this challenge object in order to verify signature. So,
    // we need to create a "fake" instance just for that.
    Challenge challenge = new Challenge("Not verified",
        this.getHeadersThatNeedToBeSignedExcludingDates(), this.getSupportedHttpsigAlgorithms());
    VerifyResult verifyResult = verifier.verifyWithResult(challenge, rcb.build(), authz);
    if (!verifyResult.equals(VerifyResult.SUCCESS)) {
      throw new ErrorResponseException(this.createErrorResponse(request, 400,
          "Invalid HTTP Signature: " + verifyResult.toString()));
    }
  }

}
