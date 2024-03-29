package eu.erasmuswithoutpaper.registry.internet;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;

import eu.erasmuswithoutpaper.registry.internet.FakeInternet.MultipleHandlersConflict;
import eu.erasmuswithoutpaper.registry.internet.sec.EwpHttpSigRequestSigner;
import eu.erasmuswithoutpaper.registry.internet.sec.EwpHttpSigResponseSigner;
import eu.erasmuswithoutpaper.registry.internet.sec.Http4xx;

import org.assertj.core.util.Maps;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link FakeInternet}.
 */
public class FakeInternetTest {

  private static class TestRequestSigner extends EwpHttpSigRequestSigner {

    private final List<String> myHeaders;

    public TestRequestSigner(KeyPair keyPair, String... headers) {
      super(keyPair);
      this.myHeaders = Arrays.asList(headers);
    }

    @Override
    public String getKeyId() {
      return "Test";
    }

    @Override
    protected List<String> getHeadersToSign(Request request) {
      return this.myHeaders;
    }

    @Override
    protected boolean shouldOverrideExistingDigest() {
      return false;
    }
  }

  private static class TestResponseSigner extends EwpHttpSigResponseSigner {

    private final List<String> myHeaders;

    public TestResponseSigner(KeyPair keyPair, String... headers) {
      super(keyPair);
      this.myHeaders = Arrays.asList(headers);
    }

    @Override
    protected List<String> getHeadersToSign(Request request, Response response) {
      return this.myHeaders;
    }

    @Override
    protected String getKeyId() {
      return "Test";
    }

    @Override
    protected boolean shouldOverrideExistingDigest() {
      return false;
    }
  }

  private static final String url1 = "https://example.com/url1.xml";
  private static final String url2 = "https://example.com/url2.xml";

  private FakeInternet internet;

  @BeforeEach
  private void cleanInternet() {
    internet = new FakeInternet();
  }

  @Test
  public void testFakeServices() throws IOException {
    assertThat(this.fetchString(url1)).isNull();
    assertThat(this.fetchString(url2)).isNull();

    FakeInternetService service1 = new FakeInternetService() {
      @Override
      public Response handleInternetRequest(Request request) throws IOException {
        if (request.getUrl().equals(url1)) {
          return new Response(200, "It works!".getBytes(StandardCharsets.UTF_8));
        } else {
          return null;
        }
      }
    };

    this.internet.addFakeInternetService(service1);
    assertThat(this.fetchString(url1)).isEqualTo("It works!");
    assertThat(this.fetchString(url2)).isNull();

    FakeInternetService service2 = new FakeInternetService() {
      @Override
      public Response handleInternetRequest(Request request) throws IOException {
        // This service always responds, it doesn't verify if the request is for its domain.
        return new Response(200, "I'm a bad service!".getBytes(StandardCharsets.UTF_8),
            Maps.newHashMap("Special-Header", "Special Value"));
      }
    };

    this.internet.addFakeInternetService(service2);
    try {
      this.fetchString(url1);
      fail("Exception expected");
    } catch (MultipleHandlersConflict e) {
      // Expected.
    }
    // url2 should still work though, because there's only one service that handles it.
    assertThat(this.fetchString(url2)).isEqualTo("I'm a bad service!");

    // Also make sure that responses are returned correctly.

    Response response = this.internet.makeRequest(new Request("POST", url2));
    assertThat(response.getStatus()).isEqualTo(200);
    assertThat(response.getBody().length)
        .isEqualTo("I'm a bad service!".getBytes(StandardCharsets.UTF_8).length);
    assertThat(response.getHeader("Special-Header")).isEqualTo("Special Value");

    // Adding exactly the same service twice should result in an error.

    try {
      this.internet.addFakeInternetService(service2);
      fail("Exception expected");
    } catch (RuntimeException e) {
      assertThat(e.getMessage()).contains("already been added");
    }

    // Let's put a conflicting url via the putUrl method, and expected the conflict
    // to be detected.

    this.internet.putURL(url2, "Conflicting contents");
    try {
      this.fetchString(url2);
      fail("Exception expected");
    } catch (MultipleHandlersConflict e) {
      // Expected.
    }

    // Remove the service, and expect the conflicts to disappear.

    this.internet.removeFakeInternetService(service2);
    assertThat(this.fetchString(url1)).isEqualTo("It works!");
    assertThat(this.fetchString(url2)).isEqualTo("Conflicting contents");

  }

  @Test
  public void testHttpsigBackend() {
    java.security.Security.addProvider(new org.bouncycastle.jce.provider.BouncyCastleProvider());

    // These tests are taken from HTTPSIG specs, draft 8.

    String publicRsaBase64 = "MIGfMA0GCSqGSIb3DQEBAQUAA4GNADCBiQKBgQDCFENGw33yGihy92pDjZQhl0C3"
        + "6rPJj+CvfSC8+q28hxA161QFNUd13wuCTUcq0Qd2qsBe/2hFyc2DCJJg0h1L78+6"
        + "Z4UMR7EOcpfdUE9Hf3m/hs+FUR45uBJeDK1HSFHD8bHKD6kv8FPGfJTotc+2xjJw"
        + "oYi+1hqp1fIekaxsyQIDAQAB";
    String privateRsaBase64 = "MIICXgIBAAKBgQDCFENGw33yGihy92pDjZQhl0C36rPJj+CvfSC8+q28hxA161QF"
        + "NUd13wuCTUcq0Qd2qsBe/2hFyc2DCJJg0h1L78+6Z4UMR7EOcpfdUE9Hf3m/hs+F"
        + "UR45uBJeDK1HSFHD8bHKD6kv8FPGfJTotc+2xjJwoYi+1hqp1fIekaxsyQIDAQAB"
        + "AoGBAJR8ZkCUvx5kzv+utdl7T5MnordT1TvoXXJGXK7ZZ+UuvMNUCdN2QPc4sBiA"
        + "QWvLw1cSKt5DsKZ8UETpYPy8pPYnnDEz2dDYiaew9+xEpubyeW2oH4Zx71wqBtOK"
        + "kqwrXa/pzdpiucRRjk6vE6YY7EBBs/g7uanVpGibOVAEsqH1AkEA7DkjVH28WDUg"
        + "f1nqvfn2Kj6CT7nIcE3jGJsZZ7zlZmBmHFDONMLUrXR/Zm3pR5m0tCmBqa5RK95u"
        + "412jt1dPIwJBANJT3v8pnkth48bQo/fKel6uEYyboRtA5/uHuHkZ6FQF7OUkGogc"
        + "mSJluOdc5t6hI1VsLn0QZEjQZMEOWr+wKSMCQQCC4kXJEsHAve77oP6HtG/IiEn7"
        + "kpyUXRNvFsDE0czpJJBvL/aRFUJxuRK91jhjC68sA7NsKMGg5OXb5I5Jj36xAkEA"
        + "gIT7aFOYBFwGgQAQkWNKLvySgKbAZRTeLBacpHMuQdl1DfdntvAyqpAZ0lY0RKmW"
        + "G6aFKaqQfOXKCyWoUiVknQJAXrlgySFci/2ueKlIE1QqIiLSZ8V8OlpFLRnb1pzI"
        + "7U1yQXnTAEFYM560yJlzUpOb1V4cScGd365tiSMvxLOvTA==";
    KeyFactory rsaFactory;
    try {
      rsaFactory = KeyFactory.getInstance("RSA");
    } catch (NoSuchAlgorithmException e) {
      throw new RuntimeException(e);
    }
    KeyPair myKeyPair;
    try {
      PrivateKey privateRsa = rsaFactory
          .generatePrivate(new PKCS8EncodedKeySpec(Base64.getDecoder().decode(privateRsaBase64)));
      PublicKey publicRsa = rsaFactory
          .generatePublic(new X509EncodedKeySpec(Base64.getDecoder().decode(publicRsaBase64)));
      myKeyPair = new KeyPair(publicRsa, privateRsa);
    } catch (InvalidKeySpecException e) {
      throw new RuntimeException(e);
    }

    Request request = new Request("POST", "https://example.com/foo?param=value&pet=dog");
    request.putHeader("Host", "example.com");
    request.putHeader("Date", "Sun, 05 Jan 2014 21:31:40 GMT");
    request.putHeader("Content-Type", "application/json");
    request.putHeader("Digest", "SHA-256=X48E9qOokqqrvdts8nOJRJN3OWDUoyWxBf7kbu9DBPE=");
    request.putHeader("Content-Length", "18");
    request.setBody("{\"hello\": \"world\"}".getBytes(StandardCharsets.UTF_8));

    Response response = new Response(200, request.getBody().get());
    response.putHeader("Host", "example.com");
    response.putHeader("Date", "Sun, 05 Jan 2014 21:31:40 GMT");
    response.putHeader("Content-Type", "application/json");
    response.putHeader("Digest", "SHA-256=X48E9qOokqqrvdts8nOJRJN3OWDUoyWxBf7kbu9DBPE=");
    response.putHeader("Content-Length", "18");

    // C.1. Default Test

    /*
     * Note, that our backend explicitly adds the `headers="date"` attribute. This is not required
     * by the specs, but it's still valid.
     */

    TestRequestSigner reqSigner1 = new TestRequestSigner(myKeyPair);
    reqSigner1.sign(request);
    assertThat(request.getHeader("Authorization")).isEqualTo("Signature keyId=\"Test\","
        + "signature=\"SjWJWbWN7i0wzBvtPl8rbASWz5xQW6mcJmn+ibttBqtifLN7Sazz"
        + "6m79cNfwwb8DMJ5cou1s7uEGKKCs+FLEEaDV5lp7q25WqS+lavg7T8hc0GppauB"
        + "6hbgEKTwblDHYGEtbGmtdHgVCk9SuS13F0hZ8FD0k/5OxEPXe5WozsbM=\","
        + "headers=\"date\",algorithm=\"rsa-sha256\"");
    TestResponseSigner resSigner1 = new TestResponseSigner(myKeyPair);
    try {
      resSigner1.sign(request, response);
    } catch (Http4xx e) {
      throw new RuntimeException(e);
    }
    assertThat(response.getHeader("Signature")).isEqualTo(
        "keyId=\"Test\"," + "signature=\"SjWJWbWN7i0wzBvtPl8rbASWz5xQW6mcJmn+ibttBqtifLN7Sazz"
            + "6m79cNfwwb8DMJ5cou1s7uEGKKCs+FLEEaDV5lp7q25WqS+lavg7T8hc0GppauB"
            + "6hbgEKTwblDHYGEtbGmtdHgVCk9SuS13F0hZ8FD0k/5OxEPXe5WozsbM=\","
            + "headers=\"date\",algorithm=\"rsa-sha256\"");

    // C.2. Basic Test

    TestRequestSigner reqSigner2 =
        new TestRequestSigner(myKeyPair, "(request-target)", "host", "date");
    reqSigner2.sign(request);
    assertThat(request.getHeader("Authorization"))
        .isEqualTo("Signature keyId=\"Test\"," + "signature=\"qdx+H7PHHDZgy4"
            + "y/Ahn9Tny9V3GP6YgBPyUXMmoxWtLbHpUnXS2mg2+SbrQDMCJypxBLSPQR2aAjn"
            + "7ndmw2iicw3HMbe8VfEdKFYRqzic+efkb3nndiv/x1xSHDJWeSWkx3ButlYSuBs"
            + "kLu6kd9Fswtemr3lgdDEmn04swr2Os0=\"," + "headers=\"(request-target) host date\","
            + "algorithm=\"rsa-sha256\"");

    // C.3. All Headers Test

    TestRequestSigner reqSigner3 = new TestRequestSigner(myKeyPair, "(request-target)", "host",
        "date", "content-type", "digest", "content-length");
    reqSigner3.sign(request);
    assertThat(request.getHeader("Authorization")).isEqualTo("Signature keyId=\"Test\","
        + "signature=\"vSdrb+dS3EceC9bcwHSo4MlyKS59iFIrhgYkz8+oVLEEzmYZZvRs"
        + "8rgOp+63LEM3v+MFHB32NfpB2bEKBIvB1q52LaEUHFv120V01IL+TAD48XaERZF"
        + "ukWgHoBTLMhYS2Gb51gWxpeIq8knRmPnYePbF5MOkR0Zkly4zKH7s1dE=\","
        + "headers=\"(request-target) host date content-type digest content-length\","
        + "algorithm=\"rsa-sha256\"");
    TestResponseSigner resSigner3 = new TestResponseSigner(myKeyPair, "(request-target)", "host",
        "date", "content-type", "digest", "content-length");
    try {
      resSigner3.sign(request, response);
    } catch (Http4xx e) {
      throw new RuntimeException(e);
    }
    assertThat(response.getHeader("Signature")).isEqualTo(
        "keyId=\"Test\"," + "signature=\"vSdrb+dS3EceC9bcwHSo4MlyKS59iFIrhgYkz8+oVLEEzmYZZvRs"
            + "8rgOp+63LEM3v+MFHB32NfpB2bEKBIvB1q52LaEUHFv120V01IL+TAD48XaERZF"
            + "ukWgHoBTLMhYS2Gb51gWxpeIq8knRmPnYePbF5MOkR0Zkly4zKH7s1dE=\","
            + "headers=\"(request-target) host date content-type digest content-length\","
            + "algorithm=\"rsa-sha256\"");
  }

  /**
   * Some basic tests to verify that our implementation of {@link FakeInternet} is working as
   * expected.
   */
  @Test
  public void testUrlFetching() {

    // Initially, our internet does not have any URLs.

    assertThat(this.fetchString(url1)).isNull();
    assertThat(this.fetchString(url2)).isNull();

    // Put a content for one of the URLs.

    this.internet.putURL(url2, "url2");
    assertThat(this.fetchString(url1)).isNull();
    assertThat(this.fetchString(url2)).isEqualTo("url2");

    // Remove an URL which has never been put.

    this.internet.removeURL(url1);
    assertThat(this.fetchString(url2)).isEqualTo("url2");

    // Remove url2.

    this.internet.removeURL(url2);
    assertThat(this.fetchString(url2)).isNull();
  }

  private String fetchString(String url) {
    try {
      return new String(this.internet.getUrl(url), StandardCharsets.UTF_8);
    } catch (IOException e) {
      return null;
    }
  }
}
