package eu.erasmuswithoutpaper.registry.internet.sec;

import java.security.interfaces.RSAPublicKey;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Collectors;

import eu.erasmuswithoutpaper.registry.common.Utils;
import eu.erasmuswithoutpaper.registry.internet.Request;
import eu.erasmuswithoutpaper.registry.internet.Response;
import eu.erasmuswithoutpaper.registryclient.ApiSearchConditions;
import eu.erasmuswithoutpaper.registryclient.RegistryClient;

import com.google.common.collect.Lists;
import net.adamcin.httpsig.api.Algorithm;
import net.adamcin.httpsig.api.Authorization;
import net.adamcin.httpsig.api.Challenge;
import net.adamcin.httpsig.api.DefaultKeychain;
import net.adamcin.httpsig.api.DefaultVerifier;
import net.adamcin.httpsig.api.RequestContent;
import net.adamcin.httpsig.api.VerifyResult;
import org.w3c.dom.Element;

/**
 * This {@link ResponseAuthorizer} authorizes responses using the EWP HTTP Signatures method.
 *
 * <p>
 * This particular implementation requires that a matched API entry will be provided on construction
 * (provided by the registry client when the API is searched for). This means that it can be reused
 * only for making requests at this single API endpoint.
 * </p>
 */
public class EwpHttpSigResponseAuthorizer extends CommonResponseAuthorizer {

  /**
   * The Registry Client used for validating server keys.
   */
  protected final RegistryClient regClient;

  /**
   * The API entry identifying the endpoint at which the request was made. Received from the
   * {@link RegistryClient#findApi(ApiSearchConditions)} method.
   */
  protected final Element matchedApiEntry;

  /**
   * @param regClient Needed for validating server keys of the received responses.
   * @param matchedApiEntry The API entry identifying the endpoint at which the request was made.
   *        This MUST be the element received from the
   *        {@link RegistryClient#findApi(ApiSearchConditions)} method.
   */
  public EwpHttpSigResponseAuthorizer(RegistryClient regClient, Element matchedApiEntry) {
    this.regClient = regClient;
    this.matchedApiEntry = matchedApiEntry;
  }

  @Override
  public EwpServer authorize(Request request, Response response) throws InvalidResponseError {
    this.verifyRequestId(request, response);
    Authorization authz = this.parseSignatureHeader(response);
    this.verifySignatureAlgorithm(authz);
    this.verifyRequiredHeadersAreSigned(response, authz);
    this.verifyRequestSignatureMatches(request, response);
    this.verifyDates(response);
    RSAPublicKey serverKey = this.lookupServerKey(authz.getKeyId());
    this.verifySignature(serverKey, request, response, authz);
    this.verifyDigest(response);
    EwpServerWithRsaKey serverId = new EwpServerWithRsaKey(serverKey);
    request.addProcessingNoticeHtml(
        "Response has been successfully authenticated with HttpSig. Server identified: "
            + serverId);
    this.removeUnsignedHeaders(response, authz);
    return serverId;
  }

  /**
   * Find a server key for the given keyId.
   *
   * @param keyId SHA-256 hex fingerprint of the public key.
   * @return The matched {@link RSAPublicKey} as found in the Registry Service.
   * @throws InvalidResponseError If the key cannot be found, or the {@link #matchedApiEntry} is not
   *         covered by this key.
   */
  protected RSAPublicKey lookupServerKey(String keyId) throws InvalidResponseError {
    RSAPublicKey serverKey = this.regClient.findRsaPublicKey(keyId);
    if (serverKey == null) {
      throw new InvalidResponseError("The keyId extracted from the response's Signature header "
          + "doesn't match any of the keys published in the Registry");
    }
    if (!this.regClient.isApiCoveredByServerKey(this.matchedApiEntry, serverKey)) {
      throw new InvalidResponseError("The keyId extracted from the response's Signature header "
          + "has been found in the Registry, but it doesn't cover the Echo API "
          + "endpoint which has generated the response. Make sure that you have "
          + "included your key in a proper manifest section.");
    }
    return serverKey;
  }

  /**
   * Parse the value of the Digest header.
   *
   * @param value The value of the Digest header.
   * @return The map of all digests included in the header. Keys contain the name of the digest
   *         function, values contain the bound digest value. The map MUST be case-insensitive.
   */
  protected Map<String, String> parseDigestHeaderValue(String value) {
    Map<String, String> result = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
    for (String pair : value.split(",")) {
      String[] parts = pair.split("=", 2);
      if (parts.length == 2) {
        result.put(parts[0].trim(), parts[1].trim());
      }
    }
    return result;
  }

  /**
   * Parse the Signature header of the response.
   *
   * @param response The response to process.
   * @return The parsed header value. This comes in the {@link Authorization} format, for
   *         compatibility with the library we use.
   * @throws InvalidResponseError If the header is missing, or could not be parsed.
   */
  protected Authorization parseSignatureHeader(Response response) throws InvalidResponseError {
    String sigHeader = response.getHeader("Signature");
    if (sigHeader == null) {
      throw new InvalidResponseError("Expecting the response to contain the Signature header");
    }
    /*
     * Our library parses only the Authorization header (not the Signature header), so we will
     * reformat the value to match.
     */
    Authorization authz = Authorization.parse("Signature " + sigHeader);
    if (authz == null) {
      throw new InvalidResponseError(
          "Could not parse response's Signature header, make sure it's in a proper format");
    }
    return authz;
  }

  /**
   * Remove all headers that hadn't been signed.
   *
   * @param response The response to process.
   * @param authz The {@link Authorization} container to get signing information from.
   */
  protected void removeUnsignedHeaders(Response response, Authorization authz) {
    List<String> keys = response.getHeaders().keySet().stream().map(s -> s.toLowerCase(Locale.US))
        .collect(Collectors.toList());
    Set<String> signedKeys =
        authz.getHeaders().stream().map(s -> s.toLowerCase(Locale.US)).collect(Collectors.toSet());
    List<String> removedHeaders = new ArrayList<>();
    for (String key : keys) {
      if (!signedKeys.contains(key)) {
        response.removeHeader(key);
        removedHeaders.add(key);
      }
    }
    if (removedHeaders.size() > 0) {
      response.addProcessingNoticeHtml("The following headers were removed, "
          + "because they weren't covered by HTTP Signature: <code>"
          + removedHeaders.stream().map(s -> Utils.escapeHtml(Utils.formatHeaderName(s)))
              .collect(Collectors.joining("</code>, <code>"))
          + "</code>.");
    }
  }

  /**
   * Verify if the response contains proper Date (or Original-Date) values, as expected in the EWP
   * specs.
   *
   * @param response The response to process.
   * @throws InvalidResponseError If headers are missing, or the values don't meet the spec's
   *         requirements.
   */
  protected void verifyDates(Response response) throws InvalidResponseError {
    List<String> dateHeadersToVerify = new ArrayList<>();
    if (response.getHeader("Date") != null) {
      dateHeadersToVerify.add("Date");
    }
    if (response.getHeader("Original-Date") != null) {
      dateHeadersToVerify.add("Original-Date");
    }
    if (dateHeadersToVerify.size() == 0) {
      throw new InvalidResponseError("Expecting the response to contain the \"Date\" "
          + "header or the \"Original-Date\" (or both).");
    }
    for (String headerName : dateHeadersToVerify) {
      String errorMessage = Utils.findErrorsInHttpSigDateHeader(response.getHeader(headerName));
      if (errorMessage != null) {
        throw new InvalidResponseError("The value of response's \"" + headerName
            + "\" header failed verification: " + errorMessage);
      }
    }
  }

  /**
   * Verify the response's digest.
   *
   * @param response The response to process.
   * @throws InvalidResponseError If the Digest header is missing, invalid or it doesn't meet the
   *         spec's criteria.
   */
  protected void verifyDigest(Response response) throws InvalidResponseError {
    String digestHeader = response.getHeader("Digest");
    if (digestHeader == null) {
      throw new InvalidResponseError("Missing response header: Digest");
    }
    String expectedSha256Digest = Utils.computeDigestBase64(response.getBody());
    Map<String, String> attrs = this.parseDigestHeaderValue(digestHeader);
    if (!attrs.containsKey("SHA-256")) {
      throw new InvalidResponseError("Missing SHA-256 digest in Digest header");
    }
    String got = attrs.get("SHA-256");
    if (!got.equals(expectedSha256Digest)) {
      throw new InvalidResponseError(
          "Response SHA-256 digest mismatch. Expected: " + expectedSha256Digest);
    }
  }

  @Override
  protected void verifyRequestId(Request request, Response response) throws InvalidResponseError {
    super.verifyRequestId(request, response);
    if ((request.getHeader("X-Request-Id") != null)
        && (response.getHeader("X-Request-Id") == null)) {
      throw new InvalidResponseError("HTTP Signature Server Authentication requires the server to "
          + "include the correlated (and signed) X-Request-Id, whenever it has been included "
          + "in the request.");
    }
  }

  /**
   * Verify that the response's X-Request-Signature header matches Signature sent (or NOT sent) in
   * the request.
   *
   * @param request The request for which the response was received for.
   * @param response The response to verify.
   * @throws InvalidResponseError If something was wrong with the X-Request-Signature header (e.g.
   *         it was missing when it should have been present). Consult the specs for details.
   */
  protected void verifyRequestSignatureMatches(Request request, Response response)
      throws InvalidResponseError {
    String reqAuthHeader = request.getHeader("Authorization");
    Authorization reqAuthz = Authorization.parse(reqAuthHeader);
    if (response.getHeader("X-Request-Signature") != null) {
      if (reqAuthz == null) {
        throw new InvalidResponseError("X-Request-Signature response header should be present only "
            + "when HTTP Signature Client Authentication has been used in the request.");
      }
      if (!response.getHeader("X-Request-Signature").equals(reqAuthz.getSignature())) {
        throw new InvalidResponseError(
            "X-Request-Signature response header doesn't match the actual "
                + "HTTP Signature of the orginal request");
      }
    } else if (reqAuthz != null) {
      throw new InvalidResponseError("Missing X-Request-Signature response header.");
    }
  }

  /**
   * Verify that the signature covers are headers that we want to be signed.
   *
   * @param response The response from which the {@link Authorization} headers has been extracted.
   * @param authz The {@link Authorization} container which describes which headers which the
   *        Signature covers.
   * @throws InvalidResponseError If some of the required headers are not covered by the Signature.
   */
  protected void verifyRequiredHeadersAreSigned(Response response, Authorization authz)
      throws InvalidResponseError {
    Set<String> signedHeaders = new HashSet<>(authz.getHeaders());
    List<String> headersThatShouldBeSigned = Lists.newArrayList("digest");
    if (response.getHeader("X-Request-Id") != null) {
      headersThatShouldBeSigned.add("x-request-id");
    }
    if (response.getHeader("X-Request-Signature") != null) {
      headersThatShouldBeSigned.add("x-request-signature");
    }
    for (String headerName : headersThatShouldBeSigned) {
      if (!signedHeaders.contains(headerName)) {
        throw new InvalidResponseError("Expecting the response's Signature to cover the \""
            + headerName + "\" header, but it doesn't.");
      }
    }
    if (signedHeaders.contains("date") || signedHeaders.contains("original-date")) {
      // Okay!
    } else {
      throw new InvalidResponseError(
          "Expecting the response's Signature to cover the \"date\" header "
              + "or the \"original-date\" header (or both), but it doesn't cover any of them.");
    }
  }

  /**
   * Verify if the Signature is correct.
   *
   * @param serverKey The server key matched for Signature's <code>keyId</code>.
   * @param request The request for which the response has been received for.
   * @param response The response to process.
   * @param authz The {@link Authorization} container which represents the response's parsed
   *        <code>Signature</code> header.
   * @throws InvalidResponseError If the signature turned out to be invalid.
   */
  protected void verifySignature(RSAPublicKey serverKey, Request request, Response response,
      Authorization authz) throws InvalidResponseError {
    DefaultKeychain keychain = new DefaultKeychain();
    keychain.add(new MyHttpSigRsaPublicKey(serverKey));
    DefaultVerifier verifier = new DefaultVerifier(keychain);

    RequestContent.Builder rcb = new RequestContent.Builder();
    rcb.setRequestTarget(request.getMethod(), request.getPathPseudoHeader());
    for (Map.Entry<String, String> entry : response.getHeaders().entrySet()) {
      rcb.addHeader(entry.getKey(), entry.getValue());
    }
    // The library needs this challenge object in order to verify signature. So,
    // we need to create a "fake" instance just for that.
    Challenge challenge = new Challenge("Not verified", Lists.newArrayList("digest"),
        Lists.newArrayList(Algorithm.RSA_SHA256));
    VerifyResult verifyResult = verifier.verifyWithResult(challenge, rcb.build(), authz);
    if (!verifyResult.equals(VerifyResult.SUCCESS)) {
      throw new InvalidResponseError(
          "Invalid HTTP Signature in response: " + verifyResult.toString());
    }
  }

  /**
   * Verify that the algorithm used for signing is trusted.
   *
   * @param authz The {@link Authorization} container with the parsed values of the
   *        <code>Signature</code> header.
   * @throws InvalidResponseError If the signing algorithm cannot be trusted, or otherwise does not
   *         conform to the specs.
   */
  protected void verifySignatureAlgorithm(Authorization authz) throws InvalidResponseError {
    if (!authz.getAlgorithm().equals(Algorithm.RSA_SHA256)) {
      throw new InvalidResponseError(
          "Expecting the response's Signature to use the rsa-sha256 algorithm, " + "but "
              + authz.getAlgorithm().getName() + " found instead.");
    }
  }
}
