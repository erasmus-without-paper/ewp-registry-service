package eu.erasmuswithoutpaper.registry.internet.sec;

import java.security.KeyPair;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import eu.erasmuswithoutpaper.registry.common.Utils;
import eu.erasmuswithoutpaper.registry.internet.Request;
import eu.erasmuswithoutpaper.registry.internet.Response;

import com.google.common.collect.Lists;
import net.adamcin.httpsig.api.Algorithm;
import net.adamcin.httpsig.api.Authorization;
import net.adamcin.httpsig.api.Challenge;
import net.adamcin.httpsig.api.DefaultKeychain;
import net.adamcin.httpsig.api.Key;
import net.adamcin.httpsig.api.RequestContent;
import net.adamcin.httpsig.api.Signer;

/**
 * {@link ResponseSigner}, which signs the responses with HTTP Signatures.
 *
 * <p>
 * We need a {@link Request} to properly sign the response. This means that a single instance of
 * {@link HttpSigResponseSigner} can be used only to sign a single {@link Response}.
 * </p>
 */
public class HttpSigResponseSigner implements ResponseSigner {

  /**
   * Compute a proper SHA-256 digest of the given {@link Response}'s body, and attach it to the
   * response as a <code>Digest</code> header.
   *
   * @param response The response to be processed.
   */
  public static void recomputeAndAttachDigestHeader(Response response) {
    response.putHeader("Digest", "SHA-256=" + Utils.computeDigestBase64(response.getBody()));
  }

  private static String getSignatureFromAuthorization(String authz) {
    // We need this helper because the library we use wasn't optimized for
    // server signatures (signature.toString() produces output which is valid
    // for the Authorization header, but not for the Signature header).
    String result = authz;
    String start = "signature ";
    String prefix = result.substring(0, start.length()).toLowerCase(Locale.US);
    if (prefix.equals(start)) {
      result = result.substring(start.length());
    }
    return result.trim();
  }

  private final Request request;
  private final String keyId;
  private final KeyPair keyPair;
  private final List<String> headersToSign;

  /**
   * @param request The request for which we will be signing the response to.
   * @param keyId The SHA-256 fingerprint of the {@link KeyPair} used for signing.
   * @param keyPair {@link KeyPair} to be used for signing.
   * @param headersToSign List of headers to be signed.
   */
  public HttpSigResponseSigner(Request request, String keyId, KeyPair keyPair,
      List<String> headersToSign) {
    this.keyId = keyId;
    this.keyPair = keyPair;
    this.headersToSign = headersToSign;
    this.request = request;
  }

  @Override
  public void sign(Response response) {
    DefaultKeychain keychain = new DefaultKeychain();
    Key kckey = new HttpSigRsaKeyPair(this.keyId, this.keyPair);
    keychain.add(kckey);
    Signer signer = new Signer(keychain);
    List<String> headersSigned = new ArrayList<>(this.headersToSign);
    if (headersSigned.size() == 0) {
      headersSigned.add("date");
    }
    signer.rotateKeys(
        new Challenge("Not verified", headersSigned, Lists.newArrayList(Algorithm.RSA_SHA256)));

    RequestContent.Builder rcb = new RequestContent.Builder();
    rcb.setRequestTarget(this.request.getMethod(), this.request.getPathPseudoHeader());
    for (Map.Entry<String, String> entry : response.getHeaders().entrySet()) {
      rcb.addHeader(entry.getKey(), entry.getValue());
    }
    RequestContent content = rcb.build();

    Authorization authz = signer.sign(content, headersSigned);
    if (authz != null) {
      response.putHeader("Signature", getSignatureFromAuthorization(authz.getHeaderValue()));
    }
  }
}
