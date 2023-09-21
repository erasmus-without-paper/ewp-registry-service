package eu.erasmuswithoutpaper.registry.common;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.Key;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.UnrecoverableKeyException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.security.interfaces.RSAPrivateCrtKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.RSAPublicKeySpec;

public class KeyStoreUtils {

  /**
   * Reads KeyPairAndCertificate from keyStore.
   * @param keyStore keyStore to read from.
   * @param keyStoreAlias alias of certificate to read.
   * @param password password to the keyStore
   * @return KeyPairAndCertificate read from keyStore
   * @throws KeyStoreUtilsException when problems with reading certificate appear.
   */
  public static KeyPairAndCertificate readKeyPairAndCertificateFromKeyStore(
      KeyStore keyStore,
      String keyStoreAlias,
      char[] password
  ) throws KeyStoreUtilsException {
    try {
      PrivateKey key = readPrivateKeyFromKeyStore(keyStore, keyStoreAlias, password);
      KeyPair keyPair;
      Certificate certificate = keyStore.getCertificate(keyStoreAlias);
      if (!(certificate instanceof X509Certificate)) {
        throw new KeyStoreUtilsException(
            String.format("Certificate '%s' is not X509Certificate.", keyStoreAlias)
        );
      }
      PublicKey publicKey = certificate.getPublicKey();
      keyPair = new KeyPair(publicKey, key);
      return new KeyPairAndCertificate(keyPair, (X509Certificate) certificate);
    } catch (KeyStoreException e) {
      throw new KeyStoreUtilsException(
          String.format(
              "Problem while reading certificate from provided keystore: %s",
              e.getMessage()
          )
      );
    }
  }

  /**
   * Reads PrivateKey from keyStore.
   * @param keyStore keyStore to read from.
   * @param keyStoreAlias alias of private key to read.
   * @param password password to the keyStore
   * @return PrivateKey read from keyStore
   * @throws KeyStoreUtilsException when problems with reading private key appear.
   */
  public static PrivateKey readPrivateKeyFromKeyStore(
      KeyStore keyStore,
      String keyStoreAlias,
      char[] password
  ) throws KeyStoreUtilsException {
    Key key;
    try {
      key = keyStore.getKey(keyStoreAlias, password);
    } catch (UnrecoverableKeyException | NoSuchAlgorithmException | KeyStoreException e) {
      throw new KeyStoreUtilsException(
          String.format(
              "Cannot recover key from keystore. Alias: \"%s\", %s",
              keyStoreAlias, e.getMessage()
          )
      );
    }

    if (key == null) {
      throw new KeyStoreUtilsException(
          String.format(
              "Cannot recover key, invalid alias: \"%s\"",
              keyStoreAlias
          )
      );
    }

    if (key instanceof PrivateKey) {
      return (PrivateKey) key;
    }
    throw new KeyStoreUtilsException(
        "There is no PrivateKey instance under provided alias."
    );
  }

  /**
   * Generates Public Key from RSA Private Key.
   * @param rsaPrivateKey input RSA Private Key.
   * @return PublicKey generated from input Private Key.
   * @throws KeyStoreUtilsException when public key cannot be generated.
   */
  public static PublicKey getPublicKeyFromPrivateKey(RSAPrivateCrtKey rsaPrivateKey)
      throws KeyStoreUtilsException {
    KeyFactory keyFactory = null;
    try {
      keyFactory = KeyFactory.getInstance("RSA");

      RSAPublicKeySpec publicKeySpec = new RSAPublicKeySpec(
          rsaPrivateKey.getModulus(), rsaPrivateKey.getPublicExponent()
      );

      return keyFactory.generatePublic(publicKeySpec);
    } catch (InvalidKeySpecException | NoSuchAlgorithmException e) {
      throw new KeyStoreUtilsException("Cannot generate public key from private key: "
          + e.getMessage());
    }
  }

  /**
   * Loads KeyStore from file.
   * @param path path of the file to read.
   * @param format format of KeyStore.
   * @param password password to KeyStore.
   * @return read KeyStore.
   * @throws KeyStoreUtilsException If KeyStore cannot be loaded.
   */
  public static KeyStore loadKeyStore(String path, String format, char[] password)
      throws KeyStoreUtilsException {
    try {
      KeyStore keyStore = KeyStore.getInstance(format);
      try (InputStream is = Files.newInputStream(Paths.get(path))) {
        keyStore.load(is, password);
      }
      return keyStore;
    } catch (IOException | CertificateException | NoSuchAlgorithmException | KeyStoreException e) {
      throw new KeyStoreUtilsException(
          String.format(
              "Cannot load KeyStore, %s",
              e.getMessage()
          )
      );
    }
  }

}
