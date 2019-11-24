package eu.erasmuswithoutpaper.registry.web;

import static org.joox.JOOX.$;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.KeyStore;
import java.security.PublicKey;
import java.security.cert.CertificateEncodingException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.List;

import eu.erasmuswithoutpaper.registry.Application;
import eu.erasmuswithoutpaper.registry.common.KeyPairAndCertificate;
import eu.erasmuswithoutpaper.registry.common.KeyStoreUtils;
import eu.erasmuswithoutpaper.registry.common.KeyStoreUtilsException;
import eu.erasmuswithoutpaper.registry.documentbuilder.BuildParams;
import eu.erasmuswithoutpaper.registry.documentbuilder.EwpDocBuilder;
import eu.erasmuswithoutpaper.registry.documentbuilder.KnownNamespace;
import eu.erasmuswithoutpaper.registry.validators.ValidatorKeyStore;
import eu.erasmuswithoutpaper.registry.validators.ValidatorKeyStoreSet;
import eu.erasmuswithoutpaper.registry.xmlformatter.XmlFormatter;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Service;

import com.google.common.collect.Lists;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.io.IOUtils;
import org.joox.Match;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * This service provides a Discovery Manifest document for the Registry Service itself.
 *
 * <p>
 * The Registry Service is responsible for distributing the information about various manifests
 * hosted in the EWP Network, but the Registry Service itself <b>also</b> hosts its own manifest
 * (which <b>also</b> is included in the catalogue hosted by the Registry). This class is
 * responsible for generating this manifest.
 * </p>
 */
@Service
public class SelfManifestProvider {

  private static final Logger logger = LoggerFactory.getLogger(SelfManifestProvider.class);

  private static class EncodedCertificateAndKeys {
    private final String certificateEncoded;
    private final String clientPublicKeyEncoded;
    private final String serverPublicKeyEncoded;

    private EncodedCertificateAndKeys(String certificateEncoded,
        String clientPublicKeyEncoded, String serverPublicKeyEncoded) {
      this.certificateEncoded = certificateEncoded;
      this.clientPublicKeyEncoded = clientPublicKeyEncoded;
      this.serverPublicKeyEncoded = serverPublicKeyEncoded;
    }

    public String getCertificateEncoded() {
      return certificateEncoded;
    }

    public String getClientPublicKeyEncoded() {
      return clientPublicKeyEncoded;
    }

    public String getServerPublicKeyEncoded() {
      return serverPublicKeyEncoded;
    }
  }

  private final ResourceLoader res;
  private final EwpDocBuilder docBuilder;
  private final XmlFormatter formatter;
  private final List<String> adminEmails;
  private final List<String> echoTesterHeiIDs;
  private final List<EncodedCertificateAndKeys> certificatesAndKeys = new ArrayList<>();

  private volatile String cached = null;

  /**
   * @param res Needed to fetch the manifest template from application resources.
   * @param docBuilder Needed to build a {@link Document} out of the template.
   * @param formatter Needed to format the end document as XML.
   * @param adminEmails A list of email addresses, separated by commas. These addresses will be
   *        included in the <code>ewp:admin-email</code> elements in the generated manifest file.
   * @param validatorKeyStoreSet Source of public keys and certificates used by validators to be
   *                         published in our manifest.
   */
  @Autowired
  public SelfManifestProvider(
      ResourceLoader res,
      EwpDocBuilder docBuilder,
      XmlFormatter formatter,
      @Value("${app.admin-emails}") List<String> adminEmails,
      ValidatorKeyStoreSet validatorKeyStoreSet,
      @Value("${app.additional-keys-keystore.path:#{null}}") String additionalKeysKeystorePath,
      @Value("${app.additional-keys-keystore.aliases:#{null}}") List<String> aliases,
      @Value("${app.additional-keys-keystore.password:#{null}}") String password)
      throws KeyStoreUtilsException, CertificateEncodingException {
    this.res = res;
    this.docBuilder = docBuilder;
    this.formatter = formatter;
    this.adminEmails = adminEmails;
    ValidatorKeyStore validatorKeyStore = validatorKeyStoreSet.getMainKeyStore();

    this.certificatesAndKeys.add(new EncodedCertificateAndKeys(
        this.encodeCertificate(validatorKeyStore.getTlsClientCertificateInUse()),
        this.encodePublicKey(validatorKeyStore.getClientRsaPublicKeyInUse()),
        this.encodePublicKey(validatorKeyStore.getServerRsaPublicKeyInUse())
    ));

    this.echoTesterHeiIDs = validatorKeyStore.getCoveredHeiIDs();

    if (additionalKeysKeystorePath != null && aliases != null && password != null) {
      readKeysFromKeyStore(additionalKeysKeystorePath, aliases, password);
    }
  }

  private void readKeysFromKeyStore(
      String additionalKeysKeystorePath, List<String> aliases, String password)
      throws KeyStoreUtilsException, CertificateEncodingException {
    char[] charPassword = password.toCharArray();
    logger.debug("Reading keyStore '{}'", additionalKeysKeystorePath);
    KeyStore keyStore = KeyStoreUtils.loadKeyStore(
        additionalKeysKeystorePath, "jks", charPassword
    );

    for (String alias : aliases) {
      logger.debug("Reading keys under alias '{}'", alias);
      KeyPairAndCertificate keyPairAndCertificate =
          KeyStoreUtils.readKeyPairAndCertificateFromKeyStore(keyStore, alias, charPassword);
      certificatesAndKeys.add(
          new EncodedCertificateAndKeys(
              this.encodeCertificate(keyPairAndCertificate.certificate),
              this.encodePublicKey(keyPairAndCertificate.keyPair.getPublic()),
              this.encodePublicKey(keyPairAndCertificate.keyPair.getPublic())
          )
      );
    }
  }

  private String encodeCertificate(X509Certificate certificate)
      throws CertificateEncodingException {
    return encodeAsBase64(certificate.getEncoded());
  }

  private String encodePublicKey(PublicKey key) {
    return encodeAsBase64(key.getEncoded());
  }

  private String encodeAsBase64(byte[] bytes) {
    return new String(
        Base64.encodeBase64(bytes),
        StandardCharsets.US_ASCII
    );

  }

  /**
   * Return the manifest formatted in XML.
   *
   * @return A String with XML contents.
   */
  public String getManifest() {
    if (this.cached == null) {
      this.cached = this.generateManifest();
    }
    return this.cached;
  }

  private String generateManifest() {

    // Fetch the template.

    InputStream inputStream;
    try {
      inputStream = this.res.getResource("classpath:self-manifest-base.xml").getInputStream();
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
    Document doc;
    try {
      doc = this.docBuilder.build(new BuildParams(IOUtils.toByteArray(inputStream))).getDocument()
          .get();
    } catch (IOException e) {
      throw new RuntimeException(e);
    }

    // Prepend <ewp:admin-email> elements.

    List<Match> hosts =
        $(doc.getDocumentElement()).namespaces(KnownNamespace.prefixMap()).xpath("mf5:host").each();
    for (String email : Lists.reverse(this.adminEmails)) {
      for (Match host : hosts) {
        Element elem = doc.createElementNS(KnownNamespace.COMMON_TYPES_V1.getNamespaceUri(),
            "ewp:admin-email");
        elem.setTextContent(email);
        host.get(0).insertBefore(elem, host.get(0).getFirstChild());
      }
    }

    // Fix URLs.

    hosts.get(0).xpath("r:apis-implemented/d5:discovery/d5:url")
        .text(Application.getRootUrl() + "/manifest.xml");
    hosts.get(0).xpath("r:apis-implemented/r1:registry/r1:catalogue-url")
        .text(Application.getRootUrl() + "/catalogue-v1.xml");

    // Add covered HEIs.

    Element heisCoveredElem = hosts.get(1).xpath("mf5:institutions-covered").get(0);
    for (String heiId : this.echoTesterHeiIDs) {
      Element heiElem =
          doc.createElementNS(KnownNamespace.RESPONSE_REGISTRY_V1.getNamespaceUri(), "hei");
      heiElem.setAttribute("id", heiId);
      Element nameElem =
          doc.createElementNS(KnownNamespace.RESPONSE_REGISTRY_V1.getNamespaceUri(), "name");
      nameElem.setTextContent("Artificial HEI for testing APIs");
      heiElem.appendChild(nameElem);
      heisCoveredElem.appendChild(heiElem);
    }

    // Add client certificates in use.
    for (EncodedCertificateAndKeys encodedCertificateAndKeys : this.certificatesAndKeys) {
      this.addClientCertificate(hosts, doc, encodedCertificateAndKeys.getCertificateEncoded());
    }

    // Add client keys in use.
    for (EncodedCertificateAndKeys encodedCertificateAndKeys : this.certificatesAndKeys) {
      this.addClientRsaPublicKey(hosts, doc, encodedCertificateAndKeys.getClientPublicKeyEncoded());
    }

    // Add server credentials in use.
    for (EncodedCertificateAndKeys encodedCertificateAndKeys : this.certificatesAndKeys) {
      this.addServerCredentials(hosts, doc, encodedCertificateAndKeys.getServerPublicKeyEncoded());
    }

    // Reformat.

    return this.formatter.format(doc);
  }

  private void addClientCertificate(List<Match> hosts, Document doc, String certEncoded) {
    Element cliCredentialsElem = hosts.get(1).xpath("mf5:client-credentials-in-use").get(0);
    Element certElem =
        doc.createElementNS(KnownNamespace.RESPONSE_MANIFEST_V5.getNamespaceUri(), "certificate");
    certElem.setTextContent(certEncoded);
    cliCredentialsElem.appendChild(certElem);
  }

  private void addClientRsaPublicKey(List<Match> hosts, Document doc, String rsaPublicKeyEncoded) {
    Element cliCredentialsElem = hosts.get(1).xpath("mf5:client-credentials-in-use").get(0);
    Element rsaKeyElem = doc.createElementNS(KnownNamespace.RESPONSE_MANIFEST_V5.getNamespaceUri(),
        "rsa-public-key");
    rsaKeyElem.setTextContent(rsaPublicKeyEncoded);
    cliCredentialsElem.appendChild(rsaKeyElem);
  }

  private void addServerCredentials(List<Match> hosts, Document doc,
      String rsaPublicKeyEncoded) {
    Element srvCredentialsElem = hosts.get(1).xpath("mf5:server-credentials-in-use").get(0);
    Element srvKeyElem = doc.createElementNS(KnownNamespace.RESPONSE_MANIFEST_V5.getNamespaceUri(),
        "rsa-public-key");
    srvKeyElem.setTextContent(rsaPublicKeyEncoded);
    srvCredentialsElem.appendChild(srvKeyElem);
  }
}
