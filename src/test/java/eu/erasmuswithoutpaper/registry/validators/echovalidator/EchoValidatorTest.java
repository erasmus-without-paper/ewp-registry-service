package eu.erasmuswithoutpaper.registry.validators.echovalidator;

import static org.assertj.core.api.Assertions.assertThat;

import java.security.KeyPair;

import eu.erasmuswithoutpaper.registry.internet.FakeInternetService;
import eu.erasmuswithoutpaper.registry.internet.sec.EwpHttpSigResponseSigner;
import eu.erasmuswithoutpaper.registry.sourceprovider.ManifestSource;
import eu.erasmuswithoutpaper.registry.validators.AbstractApiTest;
import eu.erasmuswithoutpaper.registry.validators.ApiValidator;
import eu.erasmuswithoutpaper.registry.validators.SemanticVersion;

import org.springframework.beans.factory.annotation.Autowired;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.xerces.impl.dv.util.Base64;
import org.assertj.core.util.Lists;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

public class EchoValidatorTest extends AbstractApiTest {
  private static String echoManifestUrl;
  private static String echoV1Url;
  private static String echoUrlTTTT;
  private static String echoUrlSTTT;
  private static String echoUrlHTTT;
  private static String echoUrlMTTT;
  private static String echoUrlMHTT;
  private static String echoUrlMMTT;
  private static String echoUrlSTET;
  private static String echoUrlSTTE;
  private static String echoUrlMMMM;
  /**
   * KeyPair to be used for signing responses of our test services.
   */
  private static KeyPair myKeyPair;
  @Autowired
  private EchoValidator validator;

  @BeforeClass
  public static void setUpClass() {
    selfManifestUrl = "https://registry.example.com/manifest.xml";
    echoManifestUrl = "https://university.example.com/manifest.xml";
    echoV1Url = "https://university.example.com/echo/v1/";
    echoUrlTTTT = "https://university.example.com/echo/TTTT/";
    echoUrlSTTT = "https://university.example.com/echo/STTT/";
    echoUrlHTTT = "https://university.example.com/echo/HTTT/";
    echoUrlMTTT = "https://university.example.com/echo/MTTT/";
    echoUrlMHTT = "https://university.example.com/echo/MHTT/";
    echoUrlMMTT = "https://university.example.com/echo/MMTT/";
    echoUrlSTET = "https://university.example.com/echo/STET/";
    echoUrlSTTE = "https://university.example.com/echo/STTE/";
    echoUrlMMMM = "https://university.example.com/echo/MMMM/";
    needsReinit = true;
  }

  @Before
  public void setUp() {
    if (needsReinit) {
      /*
       * Minimal setup for the services to guarantee that repo contains a valid catalogue,
       * consistent with the certificates returned by the validator.
       */
      this.sourceProvider.clearSources();
      this.repo.deleteAll();
      this.internet.clearAll();

      String myManifest = this.selfManifestProvider.getManifest();
      this.internet.putURL(selfManifestUrl, myManifest);
      this.sourceProvider.addSource(ManifestSource.newTrustedSource(selfManifestUrl));

      String echoManifest = this.getFileAsString("echovalidator/manifest.xml");
      myKeyPair = this.validator.generateKeyPair();
      echoManifest = echoManifest.replace(
          "SERVER-KEY-PLACEHOLDER",
          Base64.encode(myKeyPair.getPublic().getEncoded())
      );
      this.internet.putURL(echoManifestUrl, echoManifest);
      this.sourceProvider
          .addSource(ManifestSource.newRegularSource(echoManifestUrl, Lists.newArrayList()));

      this.registryUpdater.reloadAllManifestSources();
      needsReinit = false;
    }
  }

  @Test
  public void testAgainstServiceHTTTInvalid1() {
    try {
      FakeInternetService service;

      service = new ServiceHTTTInvalid1(echoUrlHTTT, this.client);
      this.internet.addFakeInternetService(service);
      assertThat(this.getValidatorReport(echoUrlHTTT, new SemanticVersion(2, 0, 0), null)).contains(
          "FAILURE",
          "HTTP 200 expected, but HTTP 400 received.",
          "This endpoint requires HTTP Signature to use one of the following algorithms: RSA_SHA512"
      );
      this.internet.removeFakeInternetService(service);

    } finally {
      this.internet.clearAll();
    }
  }

  @Test
  public void testAgainstServiceHTTTInvalid10() {
    try {
      FakeInternetService service;

      service = new ServiceHTTTInvalid10(echoUrlHTTT, this.client);
      this.internet.addFakeInternetService(service);
      String out = this.getValidatorReport(echoUrlHTTT, new SemanticVersion(2, 0, 0), null);
      assertThat(out).contains("FAILURE: Trying Combination[PHTTT] with an invalid Digest.");
      assertThat(out).contains("WARNING: Trying Combination[PHTTT] with \"SHA\" "
          + "request digest. This algorithm is deprecated, so we are expecting to "
          + "receive a valid HTTP 400 response");
      this.internet.removeFakeInternetService(service);

    } finally {
      this.internet.clearAll();
    }
  }

  @Test
  public void testAgainstServiceHTTTInvalid11() {
    try {
      FakeInternetService service;

      service = new ServiceHTTTInvalid11(echoUrlHTTT, this.client);
      this.internet.addFakeInternetService(service);
      String out = this.getValidatorReport(echoUrlHTTT, new SemanticVersion(2, 0, 0), null);
      assertThat(out).doesNotContain("FAILURE");
      assertThat(out)
          .contains("WARNING: Trying Combination[PHTTT] with non-canonical X-Request-ID");
      this.internet.removeFakeInternetService(service);

    } finally {
      this.internet.clearAll();
    }
  }

  @Test
  public void testAgainstServiceHTTTInvalid12() {
    try {
      FakeInternetService service;

      service = new ServiceHTTTInvalid12(echoUrlHTTT, this.client);
      this.internet.addFakeInternetService(service);
      String out = this.getValidatorReport(echoUrlHTTT, new SemanticVersion(2, 0, 0), null);
      assertThat(out).containsOnlyOnce("FAILURE");
      assertThat(out).contains("FAILURE: Trying Combination[PHTTT] POST request with "
          + "a list of echo values [a, b, a], plus an additional GET echo=c&echo=c parameters");
      assertThat(out).contains("We expected the response to contain the following echo values: "
          + "[a, b, a], but the following values were found instead: [c, c, a, b, a]");
      this.internet.removeFakeInternetService(service);

    } finally {
      this.internet.clearAll();
    }
  }

  @Test
  public void testAgainstServiceHTTTInvalid2() {
    try {
      FakeInternetService service;

      service = new ServiceHTTTInvalid2(echoUrlHTTT, this.client);
      this.internet.addFakeInternetService(service);
      String out = this.getValidatorReport(echoUrlHTTT, new SemanticVersion(2, 0, 0), null);
      assertThat(out)
          .contains("### FAILURE: Trying Combination[PHTTT] with Original-Date (instead of Date). "
              + "Expecting to receive a valid HTTP 200 response.\n\n"
              + "HTTP 200 expected, but HTTP 400 received.\n"
              + "This endpoint requires your request to include the \"Date\" header.");
      this.internet.removeFakeInternetService(service);

    } finally {
      this.internet.clearAll();
    }
  }

  @Test
  public void testAgainstServiceHTTTInvalid3() {
    try {
      FakeInternetService service;

      service = new ServiceHTTTInvalid3(echoUrlHTTT, this.client);
      this.internet.addFakeInternetService(service);
      String out = this.getValidatorReport(echoUrlHTTT, new SemanticVersion(2, 0, 0), null);
      assertThat(out)
          .contains("### FAILURE: Trying Combination[PHTTT] with unsigned x-request-id header. "
              + "Expecting to receive a valid HTTP 400 or HTTP 401 error response.\n\n"
              + "HTTP 400 or HTTP 401 expected, but HTTP 200 received.");
      this.internet.removeFakeInternetService(service);

    } finally {
      this.internet.clearAll();
    }
  }

  @Test
  public void testAgainstServiceHTTTInvalid4() {
    try {
      FakeInternetService service;

      service = new ServiceHTTTInvalid4(echoUrlHTTT, this.client);
      this.internet.addFakeInternetService(service);
      String out = this.getValidatorReport(echoUrlHTTT, new SemanticVersion(2, 0, 0), null);
      assertThat(out).contains(
          "FAILURE: Trying Combination[PHTTT] with some extra unknown, but properly signed "
              + "headers");
      this.internet.removeFakeInternetService(service);

    } finally {
      this.internet.clearAll();
    }
  }

  @Test
  public void testAgainstServiceHTTTInvalid5() {
    try {
      FakeInternetService service;

      service = new ServiceHTTTInvalid5(echoUrlHTTT, this.client);
      this.internet.addFakeInternetService(service);
      String out = this.getValidatorReport(echoUrlHTTT, new SemanticVersion(2, 0, 0), null);
      assertThat(out).doesNotContain("FAILURE");
      assertThat(out).doesNotContain("ERROR");
      assertThat(out).contains("WARNING");
      assertThat(out).contains(
          "It is RECOMMENDED for HTTP 401 responses to contain a proper Want-Digest header");
      this.internet.removeFakeInternetService(service);

    } finally {
      this.internet.clearAll();
    }
  }

  @Test
  public void testAgainstServiceHTTTInvalid6() {
    try {
      FakeInternetService service;

      service = new ServiceHTTTInvalid6(echoUrlHTTT, this.client);
      this.internet.addFakeInternetService(service);
      String out = this.getValidatorReport(echoUrlHTTT, new SemanticVersion(2, 0, 0), null);
      assertThat(out).doesNotContain("FAILURE");
      assertThat(out).doesNotContain("ERROR");
      assertThat(out).contains("WARNING");
      assertThat(out).contains("If you want to include the \"headers\" property in your "
          + "WWW-Authenticate header, then it should contain at least all required values");
      this.internet.removeFakeInternetService(service);

    } finally {
      this.internet.clearAll();
    }
  }

  @Test
  public void testAgainstServiceHTTTInvalid7() {
    try {
      FakeInternetService service;

      service = new ServiceHTTTInvalid7(echoUrlHTTT, this.client);
      this.internet.addFakeInternetService(service);
      String out = this.getValidatorReport(echoUrlHTTT, new SemanticVersion(2, 0, 0), null);
      assertThat(out).contains(
          "FAILURE: Trying Combination[PHTTT] signed with a server key, instead of a client key. "
              + "Expecting to receive a valid HTTP 403 error response");
      this.internet.removeFakeInternetService(service);

    } finally {
      this.internet.clearAll();
    }
  }

  @Test
  public void testAgainstServiceHTTTInvalid8() {
    try {
      FakeInternetService service;

      service = new ServiceHTTTInvalid8(echoUrlHTTT, this.client);
      this.internet.addFakeInternetService(service);
      String out = this.getValidatorReport(echoUrlHTTT, new SemanticVersion(2, 0, 0), null);
      assertThat(out).doesNotContain("FAILURE");
      assertThat(out).contains("WARNING: Trying Combination[PHTTT] with an unsynchronized clock");
      this.internet.removeFakeInternetService(service);

    } finally {
      this.internet.clearAll();
    }
  }

  @Test
  public void testAgainstServiceHTTTInvalid9() {
    try {
      FakeInternetService service;

      service = new ServiceHTTTInvalid9(echoUrlHTTT, this.client);
      this.internet.addFakeInternetService(service);
      String out = this.getValidatorReport(echoUrlHTTT, new SemanticVersion(2, 0, 0), null);
      assertThat(out).contains(
          "FAILURE: Trying Combination[PHTTT] with a known keyId, but " + "invalid signature");
      assertThat(out).contains("FAILURE: Trying Combination[PHTTT] with missing headers "
          + "that were supposed to be signed");
      this.internet.removeFakeInternetService(service);

    } finally {
      this.internet.clearAll();
    }
  }

  @Test
  public void testAgainstServiceHTTTValid() {
    try {
      FakeInternetService service;

      service = new ServiceHTTTValid(echoUrlHTTT, this.client);
      this.internet.addFakeInternetService(service);
      assertThat(this.getValidatorReport(echoUrlHTTT, new SemanticVersion(2, 0, 0), null))
          .isEqualTo(this.getFileAsString("echovalidator/ServiceHTTTValid.txt"));
      this.internet.removeFakeInternetService(service);

    } finally {
      this.internet.clearAll();
    }
  }

  @Test
  public void testAgainstServiceMHTTValid() {
    try {
      FakeInternetService service;

      service = new ServiceMHTTValid(echoUrlMHTT, this.client, myKeyPair);
      this.internet.addFakeInternetService(service);
      String out = this.getValidatorReport(echoUrlMHTT, new SemanticVersion(2, 0, 0), null);
      assertThat(out).doesNotContain("FAILURE");
      assertThat(out).doesNotContain("ERROR");
      assertThat(out).doesNotContain("WARNING");
      assertThat(out).contains("NOTICE");
      assertThat(out).contains(
          "Response contains the Signature header, even though the client didn't ask for it");
      this.internet.removeFakeInternetService(service);

    } finally {
      this.internet.clearAll();
    }
  }

  @Test
  public void testAgainstServiceMMMMValid() {
    try {
      FakeInternetService service;

      service = new ServiceMMMMValid(echoUrlMMMM, this.client, Lists.newArrayList(myKeyPair));
      this.internet.addFakeInternetService(service);
      String out = this.getValidatorReport(echoUrlMMMM, new SemanticVersion(2, 0, 0), null);
      assertThat(out).doesNotContain("FAILURE");
      assertThat(out).doesNotContain("ERROR");
      assertThat(out).doesNotContain("WARNING");
      assertThat(out).containsOnlyOnce("NOTICE");

    } finally {
      this.internet.clearAll();
    }
  }

  @Test
  public void testAgainstServiceMMTTInvalid1() {
    try {
      FakeInternetService service;

      service = new ServiceMMTTInvalid1(echoUrlMMTT, this.client, myKeyPair);
      this.internet.addFakeInternetService(service);
      String out = this.getValidatorReport(echoUrlMMTT, new SemanticVersion(2, 0, 0), null);
      assertThat(out).contains("FAILURE");
      assertThat(out).contains("Expecting the response's Signature to cover the \"date\" header "
          + "or the \"original-date\" header");
      this.internet.removeFakeInternetService(service);

    } finally {
      this.internet.clearAll();
    }
  }

  @Test
  public void testAgainstServiceMMTTInvalid2() {
    try {
      FakeInternetService service;

      service = new ServiceMMTTInvalid2(echoUrlMMTT, this.client, myKeyPair);
      this.internet.addFakeInternetService(service);
      String out = this.getValidatorReport(echoUrlMMTT, new SemanticVersion(2, 0, 0), null);
      assertThat(out).contains("FAILURE");
      assertThat(out).contains("Server/client difference exceeds the maximum allowed threshold");
      this.internet.removeFakeInternetService(service);

    } finally {
      this.internet.clearAll();
    }
  }

  @Test
  public void testAgainstServiceMMTTInvalid3() {
    try {
      FakeInternetService service;

      service = new ServiceMMTTInvalid3(echoUrlMMTT, this.client, myKeyPair);
      this.internet.addFakeInternetService(service);
      String out = this.getValidatorReport(echoUrlMMTT, new SemanticVersion(2, 0, 0), null);
      assertThat(out).contains("FAILURE");
      assertThat(out).contains("Missing SHA-256 digest in Digest header");
      this.internet.removeFakeInternetService(service);

    } finally {
      this.internet.clearAll();
    }
  }

  @Test
  public void testAgainstServiceMMTTInvalid4() {
    try {
      FakeInternetService service;

      service = new ServiceMMTTInvalid4(echoUrlMMTT, this.client, myKeyPair);
      this.internet.addFakeInternetService(service);
      String out = this.getValidatorReport(echoUrlMMTT, new SemanticVersion(2, 0, 0), null);
      assertThat(out).contains("FAILURE");
      assertThat(out).contains("Response SHA-256 digest mismatch");
      this.internet.removeFakeInternetService(service);

    } finally {
      this.internet.clearAll();
    }
  }

  @Test
  public void testAgainstServiceMMTTInvalid5() {
    try {
      FakeInternetService service;

      service = new ServiceMMTTInvalid5(echoUrlMMTT, this.client, myKeyPair);
      this.internet.addFakeInternetService(service);
      String out = this.getValidatorReport(echoUrlMMTT, new SemanticVersion(2, 0, 0), null);
      assertThat(out).contains("FAILURE");
      assertThat(out).contains("HTTP Signature Server Authentication requires the server "
          + "to include the correlated (and signed) X-Request-Id");
      this.internet.removeFakeInternetService(service);

    } finally {
      this.internet.clearAll();
    }
  }

  @Test
  public void testAgainstServiceMMTTInvalid6() {
    try {
      FakeInternetService service;

      service = new ServiceMMTTInvalid6(echoUrlMMTT, this.client, myKeyPair);
      this.internet.addFakeInternetService(service);
      String out = this.getValidatorReport(echoUrlMMTT, new SemanticVersion(2, 0, 0), null);
      assertThat(out).contains("FAILURE");
      assertThat(out).contains("Missing X-Request-Signature response header");
      this.internet.removeFakeInternetService(service);

    } finally {
      this.internet.clearAll();
    }
  }

  @Test
  public void testAgainstServiceMMTTInvalid7() {
    try {
      FakeInternetService service;

      service = new ServiceMMTTInvalid7(echoUrlMMTT, this.client, myKeyPair);
      this.internet.addFakeInternetService(service);
      String out = this.getValidatorReport(echoUrlMMTT, new SemanticVersion(2, 0, 0), null);
      assertThat(out).contains("FAILURE");
      assertThat(out).contains("X-Request-Signature response header doesn't match the "
          + "actual HTTP Signature of the orginal request");
      this.internet.removeFakeInternetService(service);

    } finally {
      this.internet.clearAll();
    }
  }

  @Test
  public void testAgainstServiceMMTTInvalid8() {
    try {
      FakeInternetService service;

      service = new ServiceMMTTInvalid8(echoUrlMMTT, this.client, myKeyPair);
      this.internet.addFakeInternetService(service);
      String out = this.getValidatorReport(echoUrlMMTT, new SemanticVersion(2, 0, 0), null);
      assertThat(out).contains("FAILURE");
      assertThat(out).contains("Expecting the response's Signature to cover the \"digest\" "
          + "header, but it doesn't.");
      this.internet.removeFakeInternetService(service);

    } finally {
      this.internet.clearAll();
    }
  }


  @Test
  public void testAgainstServiceMMTTInvalid9() {
    try {
      FakeInternetService service;

      service = new ServiceMMTTInvalid9(echoUrlMMTT, this.client, myKeyPair);
      this.internet.addFakeInternetService(service);
      String out = this.getValidatorReport(echoUrlMMTT, new SemanticVersion(2, 0, 0), null);
      assertThat(out).contains("FAILURE");
      assertThat(out).contains("The request didn't contain the X-Request-Id header, "
          + "so the response also shouldn't.");
      this.internet.removeFakeInternetService(service);

    } finally {
      this.internet.clearAll();
    }
  }

  @Test
  public void testAgainstServiceMMTTValid() {
    try {
      FakeInternetService service;

      service = new ServiceMMTTValid(echoUrlMMTT, this.client, myKeyPair);
      this.internet.addFakeInternetService(service);
      String out = this.getValidatorReport(echoUrlMMTT, new SemanticVersion(2, 0, 0), null);
      assertThat(out).isEqualTo(this.getFileAsString("echovalidator/ServiceMMTTValid.txt"));
      assertThat(out).doesNotContain("FAILURE");
      assertThat(out).doesNotContain("ERROR");
      assertThat(out).doesNotContain("WARNING");
      assertThat(out).doesNotContain(
          "Response contains the Signature header, even though the client didn't ask for it");
      // The number of notices, except the ones warning about not supporting gzip, is 1.
      assertThat(StringUtils.countMatches(out, "NOTICE")
          - StringUtils.countMatches(out, "server didn't compress its response")).isEqualTo(1);
      this.internet.removeFakeInternetService(service);

    } finally {
      this.internet.clearAll();
    }
  }

  @Test
  public void testAgainstServiceMMTTValid2() {
    try {
      FakeInternetService service;

      service = new ServiceMMTTValid2(echoUrlMMTT, this.client, myKeyPair);
      this.internet.addFakeInternetService(service);
      String out = this.getValidatorReport(echoUrlMMTT, new SemanticVersion(2, 0, 0), null);
      assertThat(out).doesNotContain("FAILURE");
      assertThat(out).doesNotContain("ERROR");
      assertThat(out).doesNotContain("WARNING");
      assertThat(out).doesNotContain(
          "Response contains the Signature header, even though the client didn't ask for it");
      this.internet.removeFakeInternetService(service);

    } finally {
      this.internet.clearAll();
    }
  }

  @Test
  public void testAgainstServiceMMTTValidWithInvalidKey() {
    try {
      FakeInternetService service;

      KeyPair otherKeyPair = this.validator.getClientRsaKeyPairInUse();
      String keyId = DigestUtils.sha256Hex(otherKeyPair.getPublic().getEncoded());
      service = new ServiceMMTTValid(echoUrlMMTT, this.client, otherKeyPair) {
        @Override
        protected EwpHttpSigResponseSigner getHttpSigSigner() {
          if (this.mySignerCache == null) {
            this.mySignerCache = new EwpHttpSigResponseSigner(this.myKeyPair) {
              @Override
              protected String getKeyId() {
                return keyId;
              }
            };
          }
          return this.mySignerCache;
        }
      };
      this.internet.addFakeInternetService(service);
      String out = this.getValidatorReport(echoUrlMMTT, new SemanticVersion(2, 0, 0), null);
      assertThat(out).contains("FAILURE");
      assertThat(out).contains(
          "keyId extracted from the response's Signature header has been found in the Registry, "
              + "but it doesn't cover the Echo API endpoint which has generated the response");
      this.internet.removeFakeInternetService(service);

    } finally {
      this.internet.clearAll();
    }
  }

  @Test
  public void testAgainstServiceMTTTInvalid1() {
    try {
      FakeInternetService service;

      service = new ServiceMTTTInvalid1(echoUrlMTTT, this.client);
      this.internet.addFakeInternetService(service);
      String out = this.getValidatorReport(echoUrlMTTT, new SemanticVersion(2, 0, 0), null);
      assertThat(out).contains("FAILURE: Trying Combination[PATTT]");
      this.internet.removeFakeInternetService(service);
    } finally {
      this.internet.clearAll();
    }
  }

  @Test
  public void testAgainstServiceMTTTValid() {
    try {
      FakeInternetService service;

      service = new ServiceMTTTValid(echoUrlMTTT, this.client);
      this.internet.addFakeInternetService(service);
      assertThat(this.getValidatorReport(echoUrlMTTT, new SemanticVersion(2, 0, 0), null))
          .isEqualTo(this.getFileAsString("echovalidator/ServiceMTTTValid.txt"));
      this.internet.removeFakeInternetService(service);
    } finally {
      this.internet.clearAll();
    }
  }

  @Test
  public void testAgainstServiceSTETInvalid1() {
    try {
      FakeInternetService service;

      service = new ServiceSTETInvalid1(echoUrlSTET, this.client, Lists.newArrayList(myKeyPair));
      this.internet.addFakeInternetService(service);
      String out = this.getValidatorReport(echoUrlSTET, new SemanticVersion(2, 0, 0), null);
      assertThat(out).containsOnlyOnce("FAILURE");
      assertThat(out).contains("Trying Combination[GSTET] - this is invalid, "
          + "because GET requests are not supported by ewp-rsa-aes128gcm encryption.");
      assertThat(out).contains("HTTP 405 expected, but HTTP 200 received.");

    } finally {
      this.internet.clearAll();
    }
  }

  @Test
  public void testAgainstServiceSTETInvalid2() {
    try {
      FakeInternetService service;

      service = new ServiceSTETInvalid2(echoUrlSTET, this.client, Lists.newArrayList(myKeyPair));
      this.internet.addFakeInternetService(service);
      String out = this.getValidatorReport(echoUrlSTET, new SemanticVersion(2, 0, 0), null);
      // Expect all failures to fail with the same message.
      int failureCount = StringUtils.countMatches(out, "FAILURE");
      int expectedCount =
          StringUtils.countMatches(out, "We cannot decrypt this request. Unknown recipient key.");
      assertThat(expectedCount).isGreaterThan(0);
      assertThat(failureCount).isEqualTo(expectedCount);

    } finally {
      this.internet.clearAll();
    }
  }

  @Test
  public void testAgainstServiceSTETValid() {
    try {
      FakeInternetService service;

      service = new ServiceSTETValid(echoUrlSTET, this.client, Lists.newArrayList(myKeyPair));
      this.internet.addFakeInternetService(service);
      String out = this.getValidatorReport(echoUrlSTET, new SemanticVersion(2, 0, 0), null);
      assertThat(out).doesNotContain("FAILURE");
      assertThat(out).doesNotContain("ERROR");
      assertThat(out).containsOnlyOnce("WARNING");
      assertThat(out).contains("It is RECOMMENDED for all EWP server endpoints to support "
          + "HTTP Signature Client Authentication");

    } finally {
      this.internet.clearAll();
    }
  }

  @Test
  public void testAgainstServiceSTTEInvalid1() {
    try {
      FakeInternetService service;

      service = new ServiceSTTEInvalid1(echoUrlSTTE, this.client);
      this.internet.addFakeInternetService(service);
      String out = this.getValidatorReport(echoUrlSTTE, new SemanticVersion(2, 0, 0), null);
      assertThat(out).doesNotContain("FAILURE");
      assertThat(out).doesNotContain("ERROR");
      assertThat(out).contains("WARNING: Querying for supported security methods");
      assertThat(out)
          .containsPattern("WARNING: Trying Combination....... with additional \"gzip\"");
      assertThat(out).contains("Your response was first encrypted, and gzipped later");
      assertThat(out).containsOnlyOnce("NOTICE");

    } finally {
      this.internet.clearAll();
    }
  }

  @Test
  public void testAgainstServiceSTTEInvalid2() {
    try {
      FakeInternetService service;

      service = new ServiceSTTEInvalid2(echoUrlSTTE, this.client);
      this.internet.addFakeInternetService(service);
      String out = this.getValidatorReport(echoUrlSTTE, new SemanticVersion(2, 0, 0), null);
      assertThat(out).contains("FAILURE");
      assertThat(out).contains("The response was (successfully) encoded with the 'gzip' "
          + "coding, but the client didn't declare this encoding as acceptable");

    } finally {
      this.internet.clearAll();
    }
  }

  @Test
  public void testAgainstServiceSTTEInvalid3() {
    try {
      FakeInternetService service;

      service = new ServiceSTTEInvalid3(echoUrlSTTE, this.client);
      this.internet.addFakeInternetService(service);
      String out = this.getValidatorReport(echoUrlSTTE, new SemanticVersion(2, 0, 0), null);
      assertThat(out).contains("FAILURE");
      assertThat(out).contains("Expecting the response to be encoded with ewp-rsa-aes128gcm");

    } finally {
      this.internet.clearAll();
    }
  }

  @Test
  public void testAgainstServiceSTTEValid() {
    try {
      FakeInternetService service;

      service = new ServiceSTTEValid(echoUrlSTTE, this.client);
      this.internet.addFakeInternetService(service);
      String out = this.getValidatorReport(echoUrlSTTE, new SemanticVersion(2, 0, 0), null);
      assertThat(out).doesNotContain("FAILURE");
      assertThat(out).doesNotContain("ERROR");
      assertThat(out).containsOnlyOnce("WARNING");
      assertThat(out).contains("WARNING: Querying for supported security methods");
      assertThat(out).containsOnlyOnce("NOTICE");

    } finally {
      this.internet.clearAll();
    }
  }

  @Test
  public void testAgainstServiceSTTTInvalid1() {
    try {
      FakeInternetService service;

      service = new ServiceSTTTInvalid1(echoUrlSTTT, this.client);
      this.internet.addFakeInternetService(service);
      assertThat(this.getValidatorReport(echoUrlSTTT, new SemanticVersion(2, 0, 0), null))
          .isEqualTo(this.getFileAsString("echovalidator/ServiceSTTTInvalid1.txt"));
      this.internet.removeFakeInternetService(service);

    } finally {
      this.internet.clearAll();
    }
  }

  @Test
  public void testAgainstServiceSTTTInvalid2() {
    try {
      FakeInternetService service;

      service = new ServiceSTTTInvalid2(echoUrlSTTT, this.client);
      this.internet.addFakeInternetService(service);
      assertThat(this.getValidatorReport(echoUrlSTTT, new SemanticVersion(2, 0, 0), null))
          .isEqualTo(this.getFileAsString("echovalidator/ServiceSTTTInvalid2.txt"));
      this.internet.removeFakeInternetService(service);

    } finally {
      this.internet.clearAll();
    }
  }

  @Test
  public void testAgainstServiceSTTTInvalid3() {
    try {
      FakeInternetService service;

      service = new ServiceSTTTInvalid3(echoUrlSTTT, this.client);
      this.internet.addFakeInternetService(service);
      assertThat(this.getValidatorReport(echoUrlSTTT, new SemanticVersion(2, 0, 0), null))
          .isEqualTo(this.getFileAsString("echovalidator/ServiceSTTTInvalid3.txt"));
      this.internet.removeFakeInternetService(service);

    } finally {
      this.internet.clearAll();
    }
  }

  @Test
  public void testAgainstServiceSTTTValid() {
    try {
      FakeInternetService service;

      service = new ServiceSTTTValid(echoUrlSTTT, this.client);
      this.internet.addFakeInternetService(service);
      String out = this.getValidatorReport(echoUrlSTTT, new SemanticVersion(2, 0, 0), null);
      assertThat(out).containsOnlyOnce("WARNING");
      assertThat(out).contains("It is RECOMMENDED for all EWP server endpoints to support "
          + "HTTP Signature Client Authentication");
      this.internet.removeFakeInternetService(service);

    } finally {
      this.internet.clearAll();
    }
  }

  @Test
  public void testAgainstServiceTTTTDummy() {
    try {
      FakeInternetService service;

      service = new ServiceTTTTDummy(echoUrlTTTT, this.client);
      this.internet.addFakeInternetService(service);
      assertThat(this.getValidatorReport(echoUrlTTTT, new SemanticVersion(2, 0, 0), null))
          .isEqualTo(this.getFileAsString("echovalidator/ServiceTTTTDummy.txt"));
      this.internet.removeFakeInternetService(service);

    } finally {
      this.internet.clearAll();
    }
  }

  @Test
  public void testAgainstServiceV1Invalid1() {
    try {
      FakeInternetService service;

      service = new ServiceV1Invalid1(echoV1Url, this.client);
      this.internet.addFakeInternetService(service);
      assertThat(this.getValidatorReport(echoV1Url, new SemanticVersion(1, 1, 1), null))
          .isEqualTo(this.getFileAsString("echovalidator/ServiceV1Invalid1.txt"));
      this.internet.removeFakeInternetService(service);

    } finally {
      this.internet.clearAll();
    }
  }

  @Test
  public void testAgainstServiceV1Invalid2() {
    try {
      FakeInternetService service;

      service = new ServiceV1Invalid2(echoV1Url, this.client);
      this.internet.addFakeInternetService(service);
      assertThat(this.getValidatorReport(echoV1Url, new SemanticVersion(1, 1, 1), null))
          .isEqualTo(this.getFileAsString("echovalidator/ServiceV1Invalid2.txt"));
      this.internet.removeFakeInternetService(service);

    } finally {
      this.internet.clearAll();
    }
  }

  @Test
  public void testAgainstServiceV1Valid() {
    try {
      FakeInternetService service;

      service = new ServiceV1Valid(echoV1Url, this.client);
      this.internet.addFakeInternetService(service);
      assertThat(this.getValidatorReport(echoV1Url, new SemanticVersion(1, 1, 1), null))
          .isEqualTo(this.getFileAsString("echovalidator/ServiceV1Valid.txt"));
      this.internet.removeFakeInternetService(service);

    } finally {
      this.internet.clearAll();
    }
  }

  @Override
  protected ApiValidator<EchoSuiteState> GetValidator() {
    return validator;
  }
}
