package eu.erasmuswithoutpaper.registry.internet;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.KeyPair;
import java.security.cert.X509Certificate;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.zip.GZIPInputStream;

import eu.erasmuswithoutpaper.rsaaes.BadEwpRsaAesBody;
import eu.erasmuswithoutpaper.rsaaes.EwpRsaAes128GcmDecoder;
import eu.erasmuswithoutpaper.rsaaes.EwpRsaAes128GcmEncoder;
import eu.erasmuswithoutpaper.rsaaes.InvalidRecipient;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import net.adamcin.httpsig.api.Algorithm;
import net.adamcin.httpsig.api.Authorization;
import net.adamcin.httpsig.api.Challenge;
import net.adamcin.httpsig.api.DefaultKeychain;
import net.adamcin.httpsig.api.Key;
import net.adamcin.httpsig.api.RequestContent;
import net.adamcin.httpsig.api.Signer;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.IOUtils;

/**
 * This interface will be used by all the other services for accessing resources over the Internet.
 * This allows to easily replace the Internet for tests.
 */
public interface Internet {

  /**
   * Represents an "advanced" HTTP request.
   *
   * <p>
   * Mutable, not thread-safe.
   * </p>
   */
  class Request {

    private String method;
    private String url;
    private Optional<byte[]> bodyUnencrypted;
    private byte[] bodyEncryptedCache;
    private final Map<String, String> headers;
    private Optional<X509Certificate> clientCertificate;
    private Optional<KeyPair> keyPair;
    private Optional<RSAPublicKey> ewpRsaAesEncryptionKey;

    public Request(String method, String url) {
      this.method = method;
      this.url = url;
      this.bodyUnencrypted = Optional.empty();
      this.headers = new HashMap<>();
      this.clientCertificate = Optional.empty();
      this.keyPair = Optional.empty();
      this.ewpRsaAesEncryptionKey = Optional.empty();
    }

    /**
     * @return Base64-encoded SHA-256 digest of request's body (if there's no body, then it's a
     *         digest of an empty byte-array).
     */
    public String computeBodyDigest() {
      byte[] body;
      if (this.getBody().isPresent()) {
        body = this.getBody().get();
      } else {
        body = new byte[0];
      }
      byte[] binaryDigest = DigestUtils.sha256(body);
      return Base64.getEncoder().encodeToString(binaryDigest);
    }

    public List<String> getAcceptableCodings() {
      List<String> result = new ArrayList<>();
      String headerValue = this.getHeader("Accept-Encoding");
      if (headerValue == null) {
        return result;
      }
      String[] codings = headerValue.split(" *, *");
      for (String entry : codings) {
        String[] params = entry.split(" *; *");
        String coding = params[0];
        boolean acceptable = true;
        for (int i = 1; i < params.length; i++) {
          if (params[i].equals("q=0")) {
            acceptable = false;
          }
        }
        if (acceptable) {
          result.add(coding);
        }
      }
      return result;
    }

    public Optional<byte[]> getBody() {
      if (!this.ewpRsaAesEncryptionKey.isPresent()) {
        return bodyUnencrypted;
      }
      if (this.bodyEncryptedCache == null) {
        byte[] input;
        if (bodyUnencrypted.isPresent()) {
          input = bodyUnencrypted.get();
        } else {
          input = new byte[0];
        }
        EwpRsaAes128GcmEncoder encoder =
            new EwpRsaAes128GcmEncoder(this.ewpRsaAesEncryptionKey.get());
        this.bodyEncryptedCache = encoder.encode(input);
      }
      return Optional.of(this.bodyEncryptedCache);
    }

    public Optional<X509Certificate> getClientCertificate() {
      return clientCertificate;
    }

    public Optional<KeyPair> getClientCertificateKeyPair() {
      return keyPair;
    }

    public String getHeader(String key) {
      return this.headers.get(key.toLowerCase(Locale.US));
    }

    public Map<String, String> getHeaders() {
      return Collections.unmodifiableMap(this.headers);
    }

    public String getMethod() {
      return method;
    }


    /**
     * Extract path from request's URL.
     *
     * @return ":path" pseudoheader, as specified in HTTP/2, Section 8.1.2.3
     *         (https://tools.ietf.org/html/rfc7540#section-8.1.2.3).
     */
    public String getPathPseudoHeader() {
      try {
        URL parsed = new URL(this.url);
        StringBuilder sb = new StringBuilder();
        sb.append(parsed.getPath());
        if (parsed.getQuery() != null) {
          sb.append('?');
          sb.append(parsed.getQuery());
        }
        if (sb.length() == 0) {
          sb.append('/');
        }
        return sb.toString();
      } catch (MalformedURLException e) {
        throw new RuntimeException(e);
      }
    }

    public String getUrl() {
      return url;
    }

    public void overrideBodyEncrypted(byte[] body) {
      if (!this.ewpRsaAesEncryptionKey.isPresent()) {
        throw new RuntimeException("This can be done only on encrypted requests.");
      }
      if (!this.bodyUnencrypted.isPresent()) {
        throw new RuntimeException("This can be done only when body is present.");
      }
      this.bodyEncryptedCache = body.clone();
    }

    public void putHeader(String key, String value) {
      this.headers.put(key.toLowerCase(Locale.US), value);
    }

    public void recomputeAndAttachDigestHeader() {
      this.putHeader("Digest", "SHA-256=" + this.computeBodyDigest());
    }

    public void recomputeAndAttachHttpSigAuthorizationHeader(String keyId, KeyPair keyPair) {
      // Simply sign all headers
      List<String> headersToSign = new ArrayList<>(this.getHeaders().keySet());
      headersToSign.add("(request-target)");
      recomputeAndAttachHttpSigAuthorizationHeader(keyId, keyPair, headersToSign);
    }

    public void recomputeAndAttachHttpSigAuthorizationHeader(String keyId, KeyPair keyPair,
        List<String> headersToSign) {

      DefaultKeychain keychain = new DefaultKeychain();
      Key kckey = new HttpSigRsaKeyPair(keyId, keyPair);
      keychain.add(kckey);
      Signer signer = new Signer(keychain);
      List<String> headersSigned = new ArrayList<>(headersToSign);
      if (headersSigned.size() == 0) {
        headersSigned.add("date");
      }
      signer.rotateKeys(
          new Challenge("Not verified", headersSigned, Lists.newArrayList(Algorithm.RSA_SHA256)));

      RequestContent.Builder rcb = new RequestContent.Builder();
      rcb.setRequestTarget(this.getMethod(), this.getPathPseudoHeader());
      for (Map.Entry<String, String> entry : this.headers.entrySet()) {
        rcb.addHeader(entry.getKey(), entry.getValue());
      }
      RequestContent content = rcb.build();

      Authorization authz = signer.sign(content, headersSigned);
      if (authz == null) {
        throw new RuntimeException("Could not sign");
      }
      this.putHeader("Authorization", authz.getHeaderValue());
    }

    public void removeHeader(String key) {
      this.headers.remove(key.toLowerCase(Locale.US));
    }

    /**
     * @param body Optional request body to be sent along the request (in case of POST requests,
     *        this often contains x-www-form-urlencoded set of parameters).
     */
    public void setBodyUnencrypted(byte[] body) {
      this.bodyUnencrypted = Optional.ofNullable(body);
      this.bodyEncryptedCache = null;
    }

    /**
     * @param clientCertificate If given, then the request will be made with the supplied TLS client
     *        certificate (keep in mind that our {@link Internet} allows only HTTPS connections).
     * @param keyPair The key-pair for which the certificate has been generated.
     */
    public void setClientCertificate(X509Certificate clientCertificate, KeyPair keyPair) {
      this.clientCertificate = Optional.ofNullable(clientCertificate);
      this.keyPair = Optional.ofNullable(keyPair);
    }

    /**
     * @param key If given, then the request body will be additionally encrypted for the given key
     *        (and encoded in ewp-rsa-aes128gcm format).
     */
    public void setEwpRsaAesBodyEncryptionKey(RSAPublicKey key) {
      this.ewpRsaAesEncryptionKey = Optional.of(key);
    }

    /**
     * @param method Request method to be used (e.g. "GET", "POST", "PUT").
     */
    public void setMethod(String method) {
      this.method = method;
    }

    /**
     * @param url The endpoint to be called.
     */
    public void setUrl(String url) {
      this.url = url;
    }
  }


  /**
   * Represents an "advanced" HTTP response.
   */
  class Response {

    @SuppressWarnings("serial")
    public static class CouldNotDecode extends Exception {

      public CouldNotDecode(String message) {
        super(message);
      }

      public CouldNotDecode(String message, Exception cause) {
        super(message, cause);
      }
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

    private final Request initialRequest;
    private final int status;
    private KeyPair recipientKeyPair;
    private byte[] body;

    private final Map<String, String> headers;

    public Response(Request initialRequest, int status, byte[] body) {
      this(initialRequest, status, body, Maps.newHashMap());
    }

    public Response(Request initialRequest, int status, byte[] body,
        Map<String, String> initialHeaders) {
      this.initialRequest = initialRequest;
      this.status = status;
      this.body = body.clone();
      this.headers = Maps.newHashMap();
      for (Entry<String, String> entry : initialHeaders.entrySet()) {
        this.putHeader(entry.getKey(), entry.getValue());
      }
    }

    public String computeBodyDigest() {
      byte[] binaryDigest = DigestUtils.sha256(this.body);
      return Base64.getEncoder().encodeToString(binaryDigest);
    }

    public byte[] getBodyDecoded() throws CouldNotDecode {
      byte[] data = this.getBodyRaw();
      for (String coding : Lists.reverse(this.getContentCodings())) {
        data = this.decodeCoding(data, coding);
      }
      return data;
    }

    /**
     * @return Raw response body, as returned by the server. This is available regardless of the
     *         response status. In case of error responses, it will contain the error response body.
     *         If you need the decoded body, use {@link #getBodyDecoded()}.
     */
    @SuppressFBWarnings(value = "EI_EXPOSE_REP")
    public byte[] getBodyRaw() {
      return this.body;
    }

    public List<String> getContentCodings() {
      String value = this.getHeader("Content-Encoding");
      List<String> codings = new ArrayList<>();
      if (value == null) {
        // Nothing.
      } else {
        for (String item : value.split(" *, *")) {
          if (item.length() > 0) {
            codings.add(item);
          }
        }
      }
      return codings;
    }

    public String getHeader(String key) {
      return this.headers.get(key.toLowerCase(Locale.US));
    }

    public Map<String, String> getHeaders() {
      return Collections.unmodifiableMap(this.headers);
    }

    /**
     * @return Key pair previously set with {@link #setRecipientKeyPair(KeyPair)} (or null).
     */
    public KeyPair getRecipientKeyPair() {
      return this.recipientKeyPair;
    }

    /**
     * @return The HTTP status returned by the endpoint.
     */
    public int getStatus() {
      return this.status;
    }

    public void putHeader(String key, String value) {
      if (key != null) {
        this.headers.put(key.toLowerCase(Locale.US), value);
      }
    }

    public void recomputeAndAttachDigestHeader() {
      this.putHeader("Digest", "SHA-256=" + this.computeBodyDigest());
    }

    public void recomputeAndAttachSignatureHeader(String keyId, KeyPair keyPair,
        List<String> headersToSign) {

      DefaultKeychain keychain = new DefaultKeychain();
      Key kckey = new HttpSigRsaKeyPair(keyId, keyPair);
      keychain.add(kckey);
      Signer signer = new Signer(keychain);
      List<String> headersSigned = new ArrayList<>(headersToSign);
      if (headersSigned.size() == 0) {
        headersSigned.add("date");
      }
      signer.rotateKeys(
          new Challenge("Not verified", headersSigned, Lists.newArrayList(Algorithm.RSA_SHA256)));

      RequestContent.Builder rcb = new RequestContent.Builder();
      rcb.setRequestTarget(this.initialRequest.getMethod(),
          this.initialRequest.getPathPseudoHeader());
      for (Map.Entry<String, String> entry : this.headers.entrySet()) {
        rcb.addHeader(entry.getKey(), entry.getValue());
      }
      RequestContent content = rcb.build();

      Authorization authz = signer.sign(content, headersSigned);
      if (authz != null) {
        this.putHeader("Signature", getSignatureFromAuthorization(authz.getHeaderValue()));
      }
    }

    public void removeHeader(String key) {
      this.headers.remove(key.toLowerCase(Locale.US));
    }

    @SuppressFBWarnings(value = "EI_EXPOSE_REP2")
    public void setBody(byte[] changed) {
      this.body = changed;
    }

    /**
     * This should be set whenever we expect the response to be encrypted. You SHOULD NOT set it if
     * you don't expect the response to be encrypted.
     *
     * @param keyPair The RSA key pair of the recipient.
     */
    public void setRecipientKeyPair(KeyPair keyPair) {
      this.recipientKeyPair = keyPair;
    }

    private byte[] decodeCoding(byte[] data, String coding) throws CouldNotDecode {
      if (coding.equalsIgnoreCase("ewp-rsa-aes128gcm")) {
        return this.decodeEwpEncryption(data);
      } else if (coding.equalsIgnoreCase("gzip")) {
        return this.decodeGzip(data);
      } else {
        throw new CouldNotDecode("Unknown Content-Encoding, could not decode: " + coding);
      }
    }

    private byte[] decodeEwpEncryption(byte[] data) throws CouldNotDecode {
      if (this.recipientKeyPair == null) {
        // Should not happen.
        throw new CouldNotDecode("We didn't expect an encrypted response here!");
      }
      EwpRsaAes128GcmDecoder decoder =
          new EwpRsaAes128GcmDecoder((RSAPublicKey) this.recipientKeyPair.getPublic(),
              (RSAPrivateKey) this.recipientKeyPair.getPrivate());
      try {
        return decoder.decode(data);
      } catch (BadEwpRsaAesBody e) {
        throw new CouldNotDecode(
            "Could not decode the response body. " + "Broken encryption algorithm?", e);
      } catch (InvalidRecipient e) {
        throw new CouldNotDecode("This response seems to be encrypted to a wrong recipient. "
            + "We don't have a proper key pair needed to decrypt it.");
      }
    }

    private byte[] decodeGzip(byte[] data) throws CouldNotDecode {
      try {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        GZIPInputStream gunzipped = new GZIPInputStream(new ByteArrayInputStream(data));
        IOUtils.copy(gunzipped, output);
        return output.toByteArray();
      } catch (IOException e) {
        throw new CouldNotDecode("Error occurred while trying to decode gzipped contents", e);
      }
    }
  }

  /**
   * Fetch the contents of the given URL.
   *
   * @param url The URL at which the contents can be found.
   * @return The contents of the URL, fully loaded to the memory.
   * @throws IOException Numerous reasons, e.g.
   *         <ul>
   *         <li>there was a problem with the transport,</li>
   *         <li>the URL was invalid,</li>
   *         <li>the server responded with a status other than HTTP 200,</li>
   *         <li>if the URL uses a HTTPS scheme and server certificate has expired,</li>
   *         <li>etc.</li>
   *         </ul>
   */
  byte[] getUrl(String url) throws IOException;

  /**
   * Make an "advanced" request at the given URL. This method allows you to send and retrieve much
   * more data that the {@link #getUrl(String)} method does.
   *
   * @param request Description of the request to be made.
   * @return Description of the response returned by the server.
   * @throws IOException When the request is invalid, or the server could not be reached.
   */
  Response makeRequest(Request request) throws IOException;

  /**
   * Enqueue an email for sending from the Registry Service to the given recipients. This method
   * should return immediately and never throw any exceptions.
   *
   * @param recipients A list of email addresses. These will be put into the "To" header of the sent
   *        email.
   * @param subject This will be put into the "Subject" header of the email.
   * @param contents The contents of the email. Plain-text.
   */
  void queueEmail(List<String> recipients, String subject, String contents);
}
