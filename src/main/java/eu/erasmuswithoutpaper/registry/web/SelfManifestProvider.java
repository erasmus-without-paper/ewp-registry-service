package eu.erasmuswithoutpaper.registry.web;

import java.security.KeyStore;
import java.security.cert.CertificateEncodingException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import eu.erasmuswithoutpaper.registry.Application;
import eu.erasmuswithoutpaper.registry.common.EncodedCertificateAndKeys;
import eu.erasmuswithoutpaper.registry.common.KeyPairAndCertificate;
import eu.erasmuswithoutpaper.registry.common.KeyStoreUtils;
import eu.erasmuswithoutpaper.registry.common.KeyStoreUtilsException;
import eu.erasmuswithoutpaper.registry.validators.ValidatorKeyStore;
import eu.erasmuswithoutpaper.registry.validators.ValidatorKeyStoreSet;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.google.common.collect.Lists;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This service provides a Discovery Manifest document for the Registry Service itself.
 *
 * <p>
 * The Registry Service is responsible for distributing the information about various manifests
 * hosted in the EWP Network, but the Registry Service itself <b>also</b> hosts its own manifests
 * (which <b>also</b> are included in the catalogue hosted by the Registry). This class is
 * responsible for generating those manifests.
 * </p>
 */
@Service
@SuppressFBWarnings("SIC_INNER_SHOULD_BE_STATIC_ANON")
public class SelfManifestProvider {

  private static final Logger logger = LoggerFactory.getLogger(SelfManifestProvider.class);

  private final List<String> adminEmails;
  private final List<String> validatorHostCoveredHeiIds;

  /*
   * i-th element is a list of certificates and keys that should be included in
   * the manifest of i-th validator.
   */
  private final List<List<EncodedCertificateAndKeys>> validatorHostCertificatesAndKeys
      = new ArrayList<>();

  private final List<String> secondaryValidatorHostCoveredHeiIds;
  private final List<EncodedCertificateAndKeys> secondaryValidatorHostCertificatesAndKeys;

  private volatile Map<String, String> cached = null;

  /**
   * @param adminEmails
   *     A list of email addresses, separated by commas. These addresses will be
   *     included in the <code>ewp:admin-email</code> elements in the generated manifest file.
   * @param validatorKeyStoreSet
   *     Source of public keys and certificates used by validators to be
   *     published in our manifest.
   * @param additionalKeysKeystorePath
   *     Path where keystore with additional keys, which should be
   *     available as validator heis keys.
   * @param aliases
   *     List of aliases which should be read from `additionalKeysKeystorePath` keystore.
   * @param password
   *     Password to `additionalKeysKeystorePath` keystore.
   * @throws KeyStoreUtilsException
   *     When there was a problem with reading keys from `additionalKeysKeystorePath`.
   * @throws CertificateEncodingException
   *     When there was a problem with encoding a certificate as base64.
   */
  @Autowired
  public SelfManifestProvider(
      @Value("${app.admin-emails}") List<String> adminEmails,
      ValidatorKeyStoreSet validatorKeyStoreSet,
      @Value("${app.additional-keys-keystore.path:#{null}}") String additionalKeysKeystorePath,
      @Value("${app.additional-keys-keystore.aliases:#{null}}") List<String> aliases,
      @Value("${app.additional-keys-keystore.password:#{null}}") String password)
      throws KeyStoreUtilsException, CertificateEncodingException {
    this.adminEmails = adminEmails;

    List<EncodedCertificateAndKeys> additionalCertificateAndKeys = null;
    if (additionalKeysKeystorePath != null && aliases != null && password != null) {
      additionalCertificateAndKeys =
          readCertificatesAndKeysFromKeyStore(additionalKeysKeystorePath, aliases, password);
    }

    this.validatorHostCoveredHeiIds = new ArrayList<>();
    for (ValidatorKeyStore keyStore : validatorKeyStoreSet.getPrimaryKeyStores()) {
      List<EncodedCertificateAndKeys> certificateAndKeys = new ArrayList<>();
      certificateAndKeys.add(new EncodedCertificateAndKeys(keyStore));

      if (additionalCertificateAndKeys != null) {
        certificateAndKeys.addAll(additionalCertificateAndKeys);
      }

      List<String> heiIds = keyStore.getCoveredHeiIDs();
      if (!heiIds.isEmpty()) {
        this.validatorHostCoveredHeiIds.add(heiIds.get(0));
      }

      this.validatorHostCertificatesAndKeys.add(certificateAndKeys);
    }

    ValidatorKeyStore secondaryValidatorHostKeyStore = validatorKeyStoreSet.getSecondaryKeyStore();

    if (secondaryValidatorHostKeyStore != null) {
      this.secondaryValidatorHostCertificatesAndKeys = new ArrayList<>();
      this.secondaryValidatorHostCertificatesAndKeys.add(
          new EncodedCertificateAndKeys(secondaryValidatorHostKeyStore)
      );
      this.secondaryValidatorHostCoveredHeiIds = secondaryValidatorHostKeyStore.getCoveredHeiIDs();
    } else {
      this.secondaryValidatorHostCertificatesAndKeys = null;
      this.secondaryValidatorHostCoveredHeiIds = null;
    }
  }

  private List<EncodedCertificateAndKeys> readCertificatesAndKeysFromKeyStore(
      String additionalKeysKeystorePath, List<String> aliases, String password)
      throws KeyStoreUtilsException, CertificateEncodingException {
    char[] charPassword = password.toCharArray();
    logger.debug("Reading keyStore '{}'", additionalKeysKeystorePath);
    KeyStore keyStore = KeyStoreUtils.loadKeyStore(
        additionalKeysKeystorePath, "jks", charPassword
    );

    List<EncodedCertificateAndKeys> readCertificatesAndKeys = new ArrayList<>();
    for (String alias : aliases) {
      logger.debug("Reading keys under alias '{}'", alias);
      KeyPairAndCertificate keyPairAndCertificate =
          KeyStoreUtils.readKeyPairAndCertificateFromKeyStore(keyStore, alias, charPassword);
      readCertificatesAndKeys.add(
          new EncodedCertificateAndKeys(
              EncodedCertificateAndKeys.encodeCertificate(keyPairAndCertificate.certificate),
              EncodedCertificateAndKeys.encodePublicKey(keyPairAndCertificate.keyPair.getPublic()),
              EncodedCertificateAndKeys.encodePublicKey(keyPairAndCertificate.keyPair.getPublic())
          )
      );
    }
    return readCertificatesAndKeys;
  }


  /**
   * Return the manifests formatted in XML.
   *
   * @return
   *     A map with names and contents of XMLs.
   */
  public Map<String, String> getManifests() {
    if (this.cached == null) {
      this.cached = this.generateManifests();
    }
    return this.cached;
  }

  private Map<String, String> generateManifests() {
    // Create registry manifest

    SelfManifestBuilder registryBuilder = new SelfManifestBuilder();
    registryBuilder.addAdminEmails(Lists.reverse(this.adminEmails))
        .setAdminNotes("This host handles the EWP Registry Service.")
        .addApi("discovery", "6.0.0",
            "https://github.com/erasmus-without-paper/ewp-specs-api-discovery/tree/stable-v6/manifest-entry.xsd",
            Collections.singletonMap("url", Application.getRootUrl() + "/manifest-registry.xml"))
        .addApi("registry", "1.3.0",
            "https://github.com/erasmus-without-paper/ewp-specs-api-registry/blob/stable-v1/manifest-entry.xsd",
            Collections.singletonMap("catalogue-url",
                Application.getRootUrl() + "/catalogue-v1.xml"));

    Map<String, String> manifests = new HashMap<>();
    manifests.put("registry", registryBuilder.buildXml());

    // Create validator manifests

    if (Application.isValidationEnabled()) {
      // Create primary validator manifests

      manifests.putAll(createValidatorManifests("primaryValidator",
          this.validatorHostCoveredHeiIds, this.validatorHostCertificatesAndKeys));

      if (this.secondaryValidatorHostCoveredHeiIds != null) {
        // Create secondary validator manifests

        manifests.putAll(createValidatorManifests("secondaryValidator",
            this.secondaryValidatorHostCoveredHeiIds,
            Arrays.asList(this.secondaryValidatorHostCertificatesAndKeys)));
      }
    }

    return manifests;
  }

  private Map<String, String> createValidatorManifests(String baseName,
      List<String> validatorHostCoveredHeiIds,
      List<List<EncodedCertificateAndKeys>> validatorHostCertificatesAndKeys) {

    Map<String, String> manifests = new HashMap<>();
    String fakeApiXmlns = "https://github.com/erasmus-without-paper/ewp-registry-service/issues/8";

    if (validatorHostCoveredHeiIds.size() == 0) {
      // Add necessary info and return if there are no HEIs.

      SelfManifestBuilder validatorBuilder = new SelfManifestBuilder();
      validatorBuilder.addAdminEmails(this.adminEmails)
          .setAdminNotes("This host is needed for the API Validator.")
          .addApi("fake-api-without-a-version", null, fakeApiXmlns, new HashMap<>())
          .addClientCertificates(validatorHostCertificatesAndKeys.get(0));

      manifests.put(baseName, validatorBuilder.buildXml());
    } else {
      // Otherwise, create a different XML for each HEI.

      for (int i = 0; i < validatorHostCoveredHeiIds.size(); i++) {
        String hei = validatorHostCoveredHeiIds.get(i);
        String name = baseName + "Hei" + Integer.toString(i + 1);
        SelfManifestBuilder validatorBuilder = new SelfManifestBuilder();

        validatorBuilder.addAdminEmails(this.adminEmails)
            .setAdminNotes("This host is needed for the API Validator.")
            .addApi("fake-api-without-a-version", null, fakeApiXmlns, new HashMap<>())
            .setHei(hei, "Artificial HEI for testing APIs")
            .addClientCertificates(validatorHostCertificatesAndKeys.get(i));

        manifests.put(name, validatorBuilder.buildXml());
      }
    }

    return manifests;
  }
}
