package eu.erasmuswithoutpaper.registry.echotester;

import java.math.BigInteger;
import java.security.InvalidKeyException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.Security;
import java.security.SignatureException;
import java.security.cert.CertificateEncodingException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import eu.erasmuswithoutpaper.registry.Application;
import eu.erasmuswithoutpaper.registry.documentbuilder.EwpDocBuilder;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import org.bouncycastle.jce.X509Principal;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.x509.X509V3CertificateGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Provides an Echo API testing feature.
 */
@Service
public class EchoTester {

  private static final Logger logger = LoggerFactory.getLogger(EchoTester.class);

  private final X509Certificate myCertificate;
  private final KeyPair myKeyPair;
  private final List<String> coveredHeiIDs;
  private final Date myCertificateDate;
  private final EwpDocBuilder docBuilder;

  /**
   * @param docBuilder Needed for validating Echo API responses against the schemas.
   */
  @Autowired
  public EchoTester(EwpDocBuilder docBuilder) {

    this.docBuilder = docBuilder;
    if (Security.getProvider(BouncyCastleProvider.PROVIDER_NAME) == null) {
      logger.debug("Registering BouncyCastle security provider");
      Security.addProvider(new BouncyCastleProvider());
    }

    /* Generate the client certificate to be used. */

    this.myKeyPair = this.generateKeyPair();
    this.myCertificate = this.generateCertificate(this.myKeyPair);
    this.myCertificateDate = new Date();

    /* Generate the IDs of the covered HEIs. */

    this.coveredHeiIDs = new ArrayList<>();
    if (Application.getRootUrl().equals("https://registry.erasmuswithoutpaper.eu")) {
      // We will never introduce artificial HEIs in the official registry.
    } else {
      for (int i = 1; i <= 2; i++) {
        this.coveredHeiIDs.add("hei0" + i + ".developers.erasmuswithoutpaper.eu");
      }
    }
  }

  /**
   * The {@link EchoTester} instance needs to publish its client certificate in the Registry,
   * (because it needs all the Echo APIs to be aware of it).
   *
   * @return An {@link X509Certificate} to be published in the Registry.
   */
  public X509Certificate getClientCertificateInUse() {
    return this.myCertificate;
  }

  /**
   * Since it takes some time to propagate information about new client certificates, this date may
   * be useful. If it's quite fresh, then it's an indicator that the client certificate might not
   * yet be recognized by all the EWP Hosts.
   *
   * @return The date indicating since when the current client certificate was used.
   */
  public Date getClientCertificateUsedSince() {
    return new Date(this.myCertificateDate.getTime());
  }

  /**
   * The certificate published for the {@link EchoTester} needs to cover a specific set of virtual
   * HEIs (so that the tester can expect Echo APIs to think that the request comes from these HEIs).
   *
   * @return IDs of the HEIs which are to be associated with the certificate returned in
   *         {@link #getClientCertificateInUse()}.
   */
  public List<String> getCoveredHeiIDs() {
    return Collections.unmodifiableList(this.coveredHeiIDs);
  }

  /**
   * Run a suite of tests on the given Echo API URL.
   *
   * @param urlStr HTTPS URL pointing to the Echo API to be tested.
   * @return A list of test results.
   */
  public List<EchoTestResult> runTests(String urlStr) {
    EchoTestSuite suite = new EchoTestSuite(this, this.docBuilder, urlStr);
    suite.run();
    return suite.getResults();
  }

  X509Certificate generateCertificate(KeyPair keyPair) {
    X509V3CertificateGenerator cert = new X509V3CertificateGenerator();
    cert.setSerialNumber(BigInteger.valueOf(12345));
    cert.setIssuerDN(new X509Principal("CN=EchoTester, OU=None, O=None L=None, C=None"));
    cert.setNotBefore(new Date(System.currentTimeMillis() - 1000L * 60 * 60 * 24 * 30));
    cert.setNotAfter(new Date(System.currentTimeMillis() + (1000L * 60 * 60 * 24 * 365 * 10)));
    cert.setSubjectDN(
        new X509Principal("CN=Dynamically Generated Certificate for testing Echo APIs, "
            + "OU=None, O=None L=None, C=None"));
    cert.setPublicKey(keyPair.getPublic());
    cert.setSignatureAlgorithm("MD5WithRSAEncryption");
    try {
      return cert.generate(keyPair.getPrivate(), "BC");
    } catch (CertificateEncodingException | InvalidKeyException | IllegalStateException
        | NoSuchProviderException | NoSuchAlgorithmException | SignatureException e) {
      throw new RuntimeException(e);
    }
  }

  KeyPair generateKeyPair() {
    KeyPairGenerator generator;
    try {
      generator = KeyPairGenerator.getInstance("RSA");
    } catch (NoSuchAlgorithmException e) {
      throw new RuntimeException(e);
    }
    generator.initialize(2048);
    return generator.generateKeyPair();
  }

  KeyPair getKeyPairInUse() {
    return this.myKeyPair;
  }
}
