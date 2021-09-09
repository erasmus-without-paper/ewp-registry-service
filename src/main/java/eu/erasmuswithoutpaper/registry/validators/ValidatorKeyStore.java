package eu.erasmuswithoutpaper.registry.validators;

import java.math.BigInteger;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import eu.erasmuswithoutpaper.registry.web.SelfManifestProvider;

import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x509.AlgorithmIdentifier;
import org.bouncycastle.asn1.x509.SubjectPublicKeyInfo;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cert.X509v3CertificateBuilder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.crypto.params.RSAKeyParameters;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.DefaultDigestAlgorithmIdentifierFinder;
import org.bouncycastle.operator.DefaultSignatureAlgorithmIdentifierFinder;
import org.bouncycastle.operator.OperatorCreationException;
import org.bouncycastle.operator.bc.BcContentSignerBuilder;
import org.bouncycastle.operator.bc.BcRSAContentSignerBuilder;


/**
 * Keeps keys and certificates used by {@link ApiValidator}s.
 */
public class ValidatorKeyStore {
  protected KeyPair myClientRsaKeyPair;
  protected KeyPair myServerRsaKeyPair;
  protected KeyPair myTlsKeyPair;
  protected KeyPair myUnregisteredKeyPair;
  protected X509Certificate myTlsCertificate;

  protected Date myCredentialsDate;

  protected List<String> myCoveredHeiIDs;

  /**
   * Generates credentials and certificates to be used by validators and published in the manifest.
   */
  public ValidatorKeyStore() {
    this(null);
  }

  /**
   * Generates credentials and certificates to be used by validators and published in the manifest.
   * Provided HEI IDs will be present in covered-institutions list of the manifest.
   *
   * @param coveredHeiIds HEI IDs to be added as covered-institutions in the manifest.
   */
  public ValidatorKeyStore(List<String> coveredHeiIds) {
    this.myCredentialsDate = new Date();
    this.myClientRsaKeyPair = this.generateKeyPair();
    this.myServerRsaKeyPair = this.generateKeyPair();
    this.myTlsKeyPair = this.generateKeyPair();
    this.myTlsCertificate = this.generateCertificate(this.myTlsKeyPair);
    this.myUnregisteredKeyPair = this.generateKeyPair();

    this.myCoveredHeiIDs = new ArrayList<>();
    if (coveredHeiIds != null) {
      this.myCoveredHeiIDs.addAll(coveredHeiIds);
    }
  }

  /**
   * Similar to {@link #getTlsClientCertificateInUse()}, but for HTTPSIG keys.
   *
   * @return A {@link KeyPair} to be published in the Registry.
   */
  public RSAPublicKey getClientRsaPublicKeyInUse() {
    return (RSAPublicKey) this.myClientRsaKeyPair.getPublic();
  }

  /**
   * The TLS client certificate published for the {@link ApiValidator} needs to cover a specific
   * set of virtual HEIs (so that the tester can expect APIs to think that the request comes
   * from these HEIs). This method allows other services (in particular, the
   * {@link SelfManifestProvider}) to fetch these HEIs from us.
   *
   * @return IDs of the HEIs which are to be associated with the TLS client certificate returned in
   *     {@link #getTlsClientCertificateInUse()}.
   */
  public List<String> getCoveredHeiIDs() {
    return Collections.unmodifiableList(this.myCoveredHeiIDs);
  }

  /**
   * Since it takes some time to propagate information about new client credentials, this date may
   * be useful. If it's quite fresh, then it's an indicator that the clients might not yet be
   * recognized by all the EWP Hosts.
   *
   * @return The date indicating since when the current credentials are used.
   */
  public Date getCredentialsGenerationDate() {
    if (this.myCredentialsDate == null) {
      return null;
    }
    return new Date(this.myCredentialsDate.getTime());
  }

  /**
   * @return A {@link KeyPair} to be published in the Registry.
   */
  public RSAPublicKey getServerRsaPublicKeyInUse() {
    return (RSAPublicKey) this.myServerRsaKeyPair.getPublic();
  }

  /**
   * The {@link ApiValidator} instance needs to publish its TLS Client Certificate in the Registry
   * in order to be able to test TLS Client Certificate Authentication. This method allows other
   * services (in particular, the {@link SelfManifestProvider}) to fetch this information from us.
   *
   * @return An {@link X509Certificate} to be published in the Registry.
   */
  public X509Certificate getTlsClientCertificateInUse() {
    return this.myTlsCertificate;
  }

  /**
   * Generate a certificate for given KeyPair.
   *
   * @param keyPair
   *     Base KeyPair.
   * @return Certificate
   */
  public X509Certificate generateCertificate(KeyPair keyPair) {
    try {
      X500Name issuer = new X500Name("CN=EchoTester, OU=None, O=None L=None, C=None");
      BigInteger serial = BigInteger.valueOf(12345);
      Date notBefore = new Date(System.currentTimeMillis() - 1000L * 60 * 60 * 24 * 30);
      Date notAfter = new Date(System.currentTimeMillis() + (1000L * 60 * 60 * 24 * 365 * 10));
      X500Name subject = new X500Name("CN=Dynamically Generated Certificate for testing APIs, "
          + "OU=None, O=None L=None, C=None");
      SubjectPublicKeyInfo publicKeyInfo =
          SubjectPublicKeyInfo.getInstance(keyPair.getPublic().getEncoded());
      X509v3CertificateBuilder certBuilder =
          new X509v3CertificateBuilder(
              issuer, serial, notBefore, notAfter, subject, publicKeyInfo
          );
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

  /**
   * Generates RSA KeyPair.
   *
   * @return Generated KeyPair.
   */
  public KeyPair generateKeyPair() {
    KeyPairGenerator generator;
    try {
      generator = KeyPairGenerator.getInstance("RSA");
    } catch (NoSuchAlgorithmException e) {
      throw new RuntimeException(e);
    }
    generator.initialize(2048);
    return generator.generateKeyPair();
  }

  public KeyPair getClientRsaKeyPairInUse() {
    return this.myClientRsaKeyPair;
  }

  public KeyPair getServerRsaKeyPairInUse() {
    return this.myServerRsaKeyPair;
  }

  public KeyPair getTlsKeyPairInUse() {
    return this.myTlsKeyPair;
  }

  public KeyPair getUnregisteredKeyPair() {
    return this.myUnregisteredKeyPair;
  }
}
