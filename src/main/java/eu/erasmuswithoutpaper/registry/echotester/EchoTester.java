package eu.erasmuswithoutpaper.registry.echotester;

import java.math.BigInteger;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.Security;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.security.interfaces.RSAPrivateKey;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import eu.erasmuswithoutpaper.registry.Application;
import eu.erasmuswithoutpaper.registry.documentbuilder.EwpDocBuilder;
import eu.erasmuswithoutpaper.registry.internet.Internet;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x509.AlgorithmIdentifier;
import org.bouncycastle.asn1.x509.SubjectPublicKeyInfo;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cert.X509v3CertificateBuilder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.crypto.params.RSAKeyParameters;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.DefaultDigestAlgorithmIdentifierFinder;
import org.bouncycastle.operator.DefaultSignatureAlgorithmIdentifierFinder;
import org.bouncycastle.operator.OperatorCreationException;
import org.bouncycastle.operator.bc.BcContentSignerBuilder;
import org.bouncycastle.operator.bc.BcRSAContentSignerBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Provides an Echo API testing feature.
 */
@Service
public class EchoTester {

  private static final Logger logger = LoggerFactory.getLogger(EchoTester.class);

  private final X509Certificate myTlsCertificate;
  private final KeyPair myTlsKeyPair;
  private final List<String> coveredHeiIDs;
  private final Date myTlsCertificateDate;
  private final EwpDocBuilder docBuilder;
  private final Internet internet;

  /**
   * @param docBuilder Needed for validating Echo API responses against the schemas.
   * @param internet Needed to make Echo API requests across the network.
   */
  @Autowired
  public EchoTester(EwpDocBuilder docBuilder, Internet internet) {

    this.docBuilder = docBuilder;
    this.internet = internet;

    if (Security.getProvider(BouncyCastleProvider.PROVIDER_NAME) == null) {
      logger.debug("Registering BouncyCastle security provider");
      Security.addProvider(new BouncyCastleProvider());
    }

    /* Generate the client certificate to be used. */

    this.myTlsKeyPair = this.generateKeyPair();
    this.myTlsCertificate = this.generateCertificate(this.myTlsKeyPair);
    this.myTlsCertificateDate = new Date();

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
   * The TLS client certificate published for the {@link EchoTester} needs to cover a specific set
   * of virtual HEIs (so that the tester can expect Echo APIs to think that the request comes from
   * these HEIs).
   *
   * @return IDs of the HEIs which are to be associated with the TLS client certificate returned in
   *         {@link #getTlsClientCertificateInUse()}.
   */
  public List<String> getCoveredHeiIDs() {
    return Collections.unmodifiableList(this.coveredHeiIDs);
  }

  /**
   * The {@link EchoTester} instance needs to publish its TLS Client Certificate in the Registry in
   * order to be able to test TLS Client Certificate Authentication.
   *
   * @return An {@link X509Certificate} to be published in the Registry.
   */
  public X509Certificate getTlsClientCertificateInUse() {
    return this.myTlsCertificate;
  }

  /**
   * Since it takes some time to propagate information about new TLS Client Certificates, this date
   * may be useful. If it's quite fresh, then it's an indicator that the TLS client certificate
   * might not yet be recognized by all the EWP Hosts.
   *
   * @return The date indicating since when the current TLS client certificate is used.
   */
  public Date getTlsClientCertificateUsedSince() {
    return new Date(this.myTlsCertificateDate.getTime());
  }

  /**
   * Run a suite of tests on the given Echo API URL.
   *
   * @param urlStr HTTPS URL pointing to the Echo API to be tested.
   * @return A list of test results.
   */
  public List<EchoTestResult> runTests(String urlStr) {
    EchoTestSuite suite = new EchoTestSuite(this, this.docBuilder, this.internet, urlStr);
    suite.run();
    return suite.getResults();
  }

  X509Certificate generateCertificate(KeyPair keyPair) {
    try {
      X500Name issuer = new X500Name("CN=EchoTester, OU=None, O=None L=None, C=None");
      BigInteger serial = BigInteger.valueOf(12345);
      Date notBefore = new Date(System.currentTimeMillis() - 1000L * 60 * 60 * 24 * 30);
      Date notAfter = new Date(System.currentTimeMillis() + (1000L * 60 * 60 * 24 * 365 * 10));
      X500Name subject = new X500Name("CN=Dynamically Generated Certificate for testing Echo APIs, "
          + "OU=None, O=None L=None, C=None");
      SubjectPublicKeyInfo publicKeyInfo =
          SubjectPublicKeyInfo.getInstance(keyPair.getPublic().getEncoded());
      X509v3CertificateBuilder certBuilder =
          new X509v3CertificateBuilder(issuer, serial, notBefore, notAfter, subject, publicKeyInfo);
      AlgorithmIdentifier sigAlgId =
          new DefaultSignatureAlgorithmIdentifierFinder().find("SHA256withRSA");
      AlgorithmIdentifier digAlgId = new DefaultDigestAlgorithmIdentifierFinder().find(sigAlgId);
      BcContentSignerBuilder sigGen = new BcRSAContentSignerBuilder(sigAlgId, digAlgId);
      RSAPrivateKey rsaPrivateKey = (RSAPrivateKey) keyPair.getPrivate();
      RSAKeyParameters keyParams = new RSAKeyParameters(true, rsaPrivateKey.getModulus(),
          rsaPrivateKey.getPrivateExponent());
      ContentSigner contentSigner = sigGen.build(keyParams);
      X509CertificateHolder certificateHolder = certBuilder.build(contentSigner);
      JcaX509CertificateConverter certConverter = new JcaX509CertificateConverter();
      return certConverter.getCertificate(certificateHolder);
    } catch (OperatorCreationException | CertificateException e) {
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

  KeyPair getTlsKeyPairInUse() {
    return this.myTlsKeyPair;
  }
}
