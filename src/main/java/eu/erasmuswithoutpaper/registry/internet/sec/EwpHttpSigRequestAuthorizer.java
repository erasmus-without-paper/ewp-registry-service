package eu.erasmuswithoutpaper.registry.internet.sec;

import java.net.MalformedURLException;
import java.net.URL;
import java.security.interfaces.RSAPublicKey;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import eu.erasmuswithoutpaper.registry.common.Utils;
import eu.erasmuswithoutpaper.registry.internet.Request;
import eu.erasmuswithoutpaper.registryclient.RegistryClient;

import net.adamcin.httpsig.api.Algorithm;
import net.adamcin.httpsig.api.Authorization;
import net.adamcin.httpsig.api.Challenge;
import net.adamcin.httpsig.api.DefaultKeychain;
import net.adamcin.httpsig.api.DefaultVerifier;
import net.adamcin.httpsig.api.RequestContent;
import net.adamcin.httpsig.api.VerifyResult;

/**
 * This particular {@link RequestAuthorizer} expects the request to be signed with HTTP Signature by
 * a client recognized by EWP Registry Service. It will throw {@link Http4xx} exceptions otherwise.
 */
public class EwpHttpSigRequestAuthorizer implements RequestAuthorizer {

  /**
   * The {@link RegistryClient} which this instance uses to verify if the signers' keys were
   * published in the EWP Registry Service.
   */
  protected final RegistryClient registryClient;

  /**
   * @param registryClient Needed to verify if the signers' keys were published in the EWP Registry
   *        Service.
   */
  public EwpHttpSigRequestAuthorizer(RegistryClient registryClient) {
    this.registryClient = registryClient;
  }

  @Override
  public EwpClientWithRsaKey authorize(Request request) throws Http4xx {
    Authorization authz = this.verifyHttpSignatureAuthorizationHeader(request);
    this.verifyHostHeader(request);
    RSAPublicKey clientKey = this.verifyClientKeyId(authz.getKeyId());
    this.verifyDateAndOriginalDateHeaders(request);
    this.verifyRequestIdHeader(request);
    this.verifySignature(request, clientKey, authz);
    this.verifyDigestHeader(request);
    this.removeUnsignedHeaders(request, authz);
    return new EwpClientWithRsaKey(clientKey);
  }

  /**
   * @param url The URL to extract the host from. It MUST be a valid URL.
   * @return The host part of the URL.
   */
  protected String extractHost(String url) {
    URL parsed;
    try {
      parsed = new URL(url);
    } catch (MalformedURLException e) {
      throw new RuntimeException(e);
    }
    return parsed.getHost();
  }

  /**
   * @param dateValue The value taken from the Date header.
   * @return Either String (with the error message), or <code>null</code> (if no errors were found).
   */
  protected String findErrorsInDateHeader(String dateValue) {
    return Utils.findErrorsInHttpSigDateHeader(dateValue);
  }

  /**
   * @return The list of headers that are required to be signed. The list does not include the Date
   *         and Original-Date headers, which are always required.
   */
  protected List<String> getHeadersThatNeedToBeSignedExcludingDates() {
    return new ArrayList<>(Arrays.asList("(request-target)", "host", "digest", "x-request-id"));
  }

  /**
   * @return The list of {@link Algorithm}s which the client is allowed to use.
   */
  protected List<Algorithm> getSupportedHttpsigAlgorithms() {
    return Arrays.asList(Algorithm.RSA_SHA256);
  }

  /**
   * @param clientKey The client key.
   * @return The list of identifiers of HEIs which are covered by this client key.
   */
  protected Collection<String> identifyCoveredHeis(RSAPublicKey clientKey) {
    return this.registryClient.getHeisCoveredByClientKey(clientKey);
  }

  /**
   * @return A {@link Http4xx} exception which will sent to the clients when they don't sign their
   *         request with a HTTP Signature.
   */
  protected Http4xx newHttpSig401() {
    Http4xx error =
        new Http4xx(401, "This endpoint requires HTTP Signature Authorization, as specified here: "
            + "https://github.com/erasmus-without-paper/ewp-specs-sec-cliauth-httpsig");
    error.putEwpErrorResponseHeader("WWW-Authenticate", "Signature realm=\"EWP\"");
    error.putEwpErrorResponseHeader("Want-Digest", "SHA-256");
    return error;
  }

  /**
   * Remove all headers that hadn't been signed.
   *
   * @param request The request to process.
   * @param authz The {@link Authorization} container to get signing information from.
   */
  protected void removeUnsignedHeaders(Request request, Authorization authz) {
    List<String> keys = request.getHeaders().keySet().stream().map(s -> s.toLowerCase(Locale.US))
        .collect(Collectors.toList());
    Set<String> signedKeys =
        authz.getHeaders().stream().map(s -> s.toLowerCase(Locale.US)).collect(Collectors.toSet());
    for (String key : keys) {
      if (!signedKeys.contains(key)) {
        request.removeHeader(key);
        request.addProcessingWarning(key + " header was removed during the authorization process, "
            + "because it has not been signed with HTTP Signature.");
      }
    }
  }

  /**
   * Find a client key by its ID (fingerprint). Only such keys which has been published in the EWP
   * Registry Service in the "client credentials" section are considered valid.
   *
   * @param keyId The SHA-256 fingerprint of the client's public RSA key.
   * @return The actual {@link RSAPublicKey}.
   * @throws Http4xx If the key us unknown, or it is not a client key.
   */
  protected RSAPublicKey verifyClientKeyId(String keyId) throws Http4xx {
    RSAPublicKey clientKey = this.registryClient.findRsaPublicKey(keyId);
    if (clientKey == null || (!this.registryClient.isClientKeyKnown(clientKey))) {
      throw new Http4xx(403, "Unknown client key: " + keyId);
    }
    return clientKey;
  }

  /**
   * Check if the request contains a Date or Original-Date header, compliant with EWP HTTP Signature
   * Authentication specs.
   *
   * @param request The request to verify.
   * @throws Http4xx If the verification fails.
   */
  protected void verifyDateAndOriginalDateHeaders(Request request) throws Http4xx {
    List<String> dateHeadersToVerify = new ArrayList<>();
    if (request.getHeader("Date") != null) {
      dateHeadersToVerify.add("Date");
    }
    if (request.getHeader("Original-Date") != null) {
      dateHeadersToVerify.add("Original-Date");
    }
    if (dateHeadersToVerify.size() == 0) {
      throw new Http4xx(400, "This endpoint requires your request to include the \"Date\" "
          + "header or the \"Original-Date\" (or both).");
    }
    for (String headerName : dateHeadersToVerify) {
      String errorMessage = this.findErrorsInDateHeader(request.getHeader(headerName));
      if (errorMessage != null) {
        throw new Http4xx(400,
            "The value of the \"" + headerName + "\" failed verification: " + errorMessage);
      }
    }
  }

  /**
   * Check if the Digest header is correct.
   *
   * @param request The request to verify.
   * @throws Http4xx If the digest header is missing or incorrect.
   */
  protected void verifyDigestHeader(Request request) throws Http4xx {
    String digestHeader = request.getHeader("Digest");
    if (digestHeader == null) {
      throw new Http4xx(400, "Missing header: Digest");
    }
    String expectedSha256Digest = Utils.computeDigestBase64(request.getBodyOrEmpty());
    if (!digestHeader.contains("SHA-256=" + expectedSha256Digest)) {
      throw new Http4xx(400, "Digest mismatch. Expected: " + expectedSha256Digest);
    }
  }

  /**
   * Check if the request's Host header matches the expected host of the service.
   *
   * @param request The request to verify.
   * @throws Http4xx If the Host header is missing or doesn't match what we expect.
   */
  protected void verifyHostHeader(Request request) throws Http4xx {
    if (!this.extractHost(request.getUrl()).equals(request.getHeader("Host"))) {
      throw new Http4xx(400,
          "Request's \"host\" header is either missing or it doesn't match what we expect.");
    }
  }

  /**
   * Check the Authorization header. Make sure that the client uses the HTTP Signature
   * authentication with proper algorithms and proper headers being signed. Do not check the actual
   * signature - this is performed in {@link #verifySignature(Request, RSAPublicKey, Authorization)}
   * .
   *
   * @param request The request to verify.
   * @return The parsed {@link Authorization} header.
   * @throws Http4xx If the Authorization header is missing, invalid, or otherwise different from
   *         what we expect.
   */
  protected Authorization verifyHttpSignatureAuthorizationHeader(Request request) throws Http4xx {
    String authHeader = request.getHeader("Authorization");
    if ((authHeader == null) || (!authHeader.toLowerCase(Locale.US).startsWith("signature "))) {
      throw this.newHttpSig401();
    }

    Authorization authz = Authorization.parse(request.getHeader("Authorization"));
    if (authz == null) {
      throw new Http4xx(400, "Could not parse the Authorization header");
    }
    if (!this.getSupportedHttpsigAlgorithms().contains(authz.getAlgorithm())) {
      throw new Http4xx(400,
          "This endpoint requires HTTP Signature to use one of the following algorithms: "
              + String.join(", ", this.getSupportedHttpsigAlgorithms().stream()
                  .map(Object::toString).collect(Collectors.toList())));
    }
    for (String requiredHeader : this.getHeadersThatNeedToBeSignedExcludingDates()) {
      if (!authz.getHeaders().contains(requiredHeader)) {
        throw new Http4xx(400, "This endpoint requires your HTTP Signature to cover the \""
            + requiredHeader + "\" header.");
      }
    }
    if ((!authz.getHeaders().contains("date")) && (!authz.getHeaders().contains("original-date"))) {
      throw new Http4xx(400, "This endpoint requires your HTTP Signature to cover the \"Date\" "
          + "header or the \"Original-Date\" (or both).");
    }
    return authz;
  }

  /**
   * Check the X-Request-Id header. Make sure it exists and is in a proper format.
   *
   * @param request The request to verify.
   * @throws Http4xx If X-Request-Id is missing or incorrect.
   */
  protected void verifyRequestIdHeader(Request request) throws Http4xx {
    String value = request.getHeader("X-Request-Id");
    if (value == null) {
      throw new Http4xx(400, "Missing \"X-Request-Id\" header");
    }
    if (!value.matches("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}")) {
      throw new Http4xx(400, "X-Request-Id must be an UUID formatted in canonical form");
    }
  }

  /**
   * Check the request's signature.
   *
   * @param request The request to verify.
   * @param expectedClientKey The key of the client which has signed the request (already extracted
   *        from the EWP Registry Service based on the request's Authorization header).
   * @param authz The parsed Authorization header of the request.
   * @throws Http4xx If the signature is incorrect.
   */
  protected void verifySignature(Request request, RSAPublicKey expectedClientKey,
      Authorization authz) throws Http4xx {
    DefaultKeychain keychain = new DefaultKeychain();
    keychain.add(new MyHttpSigRsaPublicKey(expectedClientKey));
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
      throw new Http4xx(400, "Invalid HTTP Signature: " + verifyResult.toString());
    }
  }

}
