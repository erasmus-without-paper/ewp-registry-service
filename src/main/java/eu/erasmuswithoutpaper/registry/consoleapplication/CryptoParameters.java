package eu.erasmuswithoutpaper.registry.consoleapplication;

import static eu.erasmuswithoutpaper.registry.consoleapplication.ApplicationParametersUtils.readParameter;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.UnrecoverableKeyException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.interfaces.RSAPrivateCrtKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.RSAPublicKeySpec;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import org.springframework.boot.ApplicationArguments;

import org.bouncycastle.openssl.PEMParser;
import org.bouncycastle.util.io.pem.PemObject;

public class CryptoParameters {

  /**
   * Returns help text lines for crypto parameters.
   */
  public static List<String> getCryptoParametersHelpText() {
    return Arrays.asList(
        "  TLS private key and certificate:",
        "    --tls-keystore=<path> - path to keystore with private key and certificate.",
        "                            JKS and PKCS #12 key store formats are supported.",
        "                            Format is detected based on extension:",
        "                              .jks, .pkcs12, .p12, .pfx,",
        "                            it can be also specified using --tls-keystore-format.",
        "    --tls-keystore-format=<format> - format of keystore.",
        "    --tls-keystore-alias=<alias> - alias under which private key and cert are stored.",
        "    --tls-keystore-password=<password> - password to keystore and private key.",
        "  HTTP Sig client private key:",
        "    Private key can be read from key store or PKCS #8 PEM file.",
        "    --http-client-keystore=<path> - refer to tls-client-keystore",
        "    --http-client-keystore-format=<format> - refer to tls-client-keystore-format",
        "    --http-client-keystore-alias=<alias> - refer to tls-client-keystore-alias",
        "    --http-client-keystore-password=<password> - refer to tls-client-keystore-password",
        "    --http-client-pem=<path> - path to PKCS #8 .pem file with private key",
        "    --http-client-use-tls-key - Uses same key that was is used by TLS.",
        "  HTTP Sig server private key:",
        "    Private key can be read from key store or PKCS #8 PEM file.",
        "    --http-server-keystore=<path> - refer to tls-client-keystore",
        "    --http-server-keystore-format=<format> - refer to tls-client-keystore-format",
        "    --http-server-keystore-alias=<alias> - refer to tls-client-keystore-alias",
        "    --http-server-keystore-password=<password> - refer to tls-client-keystore-password",
        "    --http-server-pem=<path> - path to PKCS #8 .pem file with private key",
        "    --http-server-use-tls-key - Uses same key that was is used by TLS.",
        "    --http-server-use-client-key - Uses same key that was is used by HTTP Sig Client."
    );
  }

  private static final String jksKeyFormatString = "JKS";
  private static final String jksFileExtension = ".jks";

  private static final String pkcs12KeyFormatString = "PKCS12";
  private static final String pkcs12FileExtension = ".pkcs12";
  private static final String p12FileExtension = ".p12";
  private static final String pfxFileExtension = ".pfx";

  private static final List<String> availableKeyFormats = Arrays.asList(
      jksKeyFormatString,
      pkcs12KeyFormatString
  );

  private static PublicKey getPublicKeyFromPrivateKey(
      RSAPrivateCrtKey rsaPrivateKey) throws ApplicationArgumentException {
    KeyFactory keyFactory = null;
    try {
      keyFactory = KeyFactory.getInstance("RSA");

      RSAPublicKeySpec publicKeySpec = new RSAPublicKeySpec(
          rsaPrivateKey.getModulus(), rsaPrivateKey.getPublicExponent()
      );

      return keyFactory.generatePublic(publicKeySpec);
    } catch (InvalidKeySpecException | NoSuchAlgorithmException e) {
      throw new ApplicationArgumentException("Cannot generate public key from private key: "
          + e.getMessage());
    }
  }

  private static KeyPair readKeyPairFromKeyStoreParameters(ApplicationArguments args,
      String keyStorePath, String keyPairName) throws ApplicationArgumentException {
    String clientKeyStoreFormat = getKeyStoreFormat(args, keyStorePath, keyPairName);
    String keyStoreAlias = readParameter(args, keyPairName + "-keystore-alias", "alias");
    String keyStorePassword = readParameter(args, keyPairName + "-keystore-password", "password",
        false);
    char[] password = keyStorePassword == null ? null : keyStorePassword.toCharArray();

    KeyStore keyStore = loadKeyStore(keyStorePath, clientKeyStoreFormat, password);
    PrivateKey privateKey = readPrivateKeyFromKeyStore(keyStore, keyStoreAlias, password);
    PublicKey publicKey = getPublicKeyFromPrivateKey((RSAPrivateCrtKey) privateKey);
    return new KeyPair(publicKey, privateKey);
  }

  private static KeyPair readHttpSigKeyFromParameters(ApplicationArguments args,
      String keyName) throws ApplicationArgumentException {
    String keyStorePathParameter = String.format("http-%s-keystore", keyName);
    String pemPathParameter = String.format("http-%s-pem", keyName);
    String keyStorePath = readParameter(args, keyStorePathParameter, "file-path", false);
    String pemPath = readParameter(args, pemPathParameter, "file-path", false);
    if (keyStorePath != null && pemPath != null) {
      throw new ApplicationArgumentException(
          String.format(
              "You should provide only one http-%s private key path.",
              keyName
          )
      );
    }

    KeyPair keyPair;
    if (keyStorePath != null) {
      keyPair = readKeyPairFromKeyStoreParameters(args, keyStorePath,
          String.format("http-%s", keyName));
    } else if (pemPath != null) {
      keyPair = readPkcs8KeyFromFile(pemPath);
    } else {
      return null;
    }
    return keyPair;
  }

  /**
   * Read Client HTTP Sig KeyPair from file given in parameters.
   */
  public static KeyPair readHttpSigClientKeyPair(
      ApplicationArguments args) throws ApplicationArgumentException {
    if (args.containsOption("http-client-use-tls-key")) {
      KeyPairAndCertificate keyPairAndCertificate = readTlsKeyAndCertificateFromParameters(args);
      if (keyPairAndCertificate == null) {
        return null;
      }
      return keyPairAndCertificate.keyPair;
    } else {
      return readHttpSigKeyFromParameters(args, "client");
    }
  }

  /**
   * Read Server HTTP Sig KeyPair from file given in parameters.
   */
  public static KeyPair readHttpSigServerKeyPair(
      ApplicationArguments args) throws ApplicationArgumentException {
    if (args.containsOption("http-server-use-tls-key")) {
      KeyPairAndCertificate keyPairAndCertificate = readTlsKeyAndCertificateFromParameters(args);
      if (keyPairAndCertificate == null) {
        return null;
      }
      return keyPairAndCertificate.keyPair;
    } else if (args.containsOption("http-server-use-client-key")) {
      return readHttpSigClientKeyPair(args);
    } else {
      return readHttpSigKeyFromParameters(args, "server");
    }
  }

  private static KeyPair readPkcs8KeyFromFile(
      String clientPemPath) throws ApplicationArgumentException {
    PemObject pemObject;
    try (PEMParser pemParser = new PEMParser(
        new InputStreamReader(
            new FileInputStream(clientPemPath),
            StandardCharsets.UTF_8
        )
    )) {
      pemObject = pemParser.readPemObject();
    } catch (IOException e) {
      throw new ApplicationArgumentException("Cannot read client PEM file. " + e.getMessage());
    }

    if (pemObject == null) {
      throw new ApplicationArgumentException("Cannot read client PEM file.");
    }

    PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(pemObject.getContent());
    try {
      KeyFactory factory = KeyFactory.getInstance("RSA", "BC");
      PrivateKey privateKey = factory.generatePrivate(keySpec);
      PublicKey publicKey = getPublicKeyFromPrivateKey((RSAPrivateCrtKey) privateKey);
      if (publicKey == null) {
        throw new ApplicationArgumentException(
            "Cannot generate public key from private key.");
      }
      return new KeyPair(publicKey, privateKey);
    } catch (NoSuchAlgorithmException | NoSuchProviderException e) {
      throw new ApplicationArgumentException(
          "Application cannot create KeyFactory. " + e.getMessage());
    } catch (InvalidKeySpecException e) {
      throw new ApplicationArgumentException(
          "Application cannot read keys from provided PEM file. " + e.getMessage());
    }
  }

  /**
   * Read TLS PrivateKey and Certificate from file given in parameters.
   */
  public static KeyPairAndCertificate readTlsKeyAndCertificateFromParameters(
      ApplicationArguments args) throws ApplicationArgumentException {
    String clientKeyStorePath = readParameter(args, "tls-keystore", "file-path", false);
    if (clientKeyStorePath == null) {
      return null;
    }
    String clientKeyStoreFormat = getKeyStoreFormat(args, clientKeyStorePath, "tls");
    String keyStoreAlias = readParameter(args, "tls-keystore-alias", "alias");
    String keyStorePassword = readParameter(args, "tls-keystore-password", "password", false);
    char[] password = keyStorePassword == null ? null : keyStorePassword.toCharArray();

    KeyStore keyStore = loadKeyStore(clientKeyStorePath, clientKeyStoreFormat, password);
    return readKeyPairAndCertificateFromKeyStore(
        keyStore,
        keyStoreAlias,
        password
    );
  }

  private static String getKeyStoreFormat(ApplicationArguments args,
      String clientKeyStorePath,
      String securityProtocolPrefix) throws ApplicationArgumentException {
    String parameter = securityProtocolPrefix + "-keystore-format";
    String clientKeyStoreFormat = readParameter(args, parameter, "key store format",
        false);

    if (clientKeyStoreFormat == null) {
      String lowerCasePath = clientKeyStorePath.toLowerCase(Locale.getDefault());
      if (lowerCasePath.endsWith(jksFileExtension)) {
        return jksKeyFormatString;
      } else if (lowerCasePath.endsWith(pkcs12FileExtension)
          || lowerCasePath.endsWith(pfxFileExtension)
          || lowerCasePath.endsWith(p12FileExtension)) {
        return pkcs12KeyFormatString;
      } else {
        throw new ApplicationArgumentException(
            String.format(
                "Cannot infer KeyStore format from filename, please provide %s parameter.",
                parameter
            )
        );
      }
    }

    if (!availableKeyFormats.contains(clientKeyStoreFormat)) {
      throw new ApplicationArgumentException(
          "Invalid key format, available key formats are " + availableKeyFormats.toString());
    }
    return clientKeyStoreFormat;
  }

  private static KeyStore loadKeyStore(String path, String format,
      char[] password) throws ApplicationArgumentException {
    try {
      KeyStore keyStore = KeyStore.getInstance(format);
      try (FileInputStream is = new FileInputStream(path)) {
        keyStore.load(is, password);
      }
      return keyStore;
    } catch (IOException | CertificateException | NoSuchAlgorithmException | KeyStoreException e) {
      throw new ApplicationArgumentException(
          String.format(
              "Cannot load KeyStore, %s",
              e.getMessage()
          )
      );
    }
  }

  private static PrivateKey readPrivateKeyFromKeyStore(
      KeyStore keyStore,
      String keyStoreAlias,
      char[] password
  ) throws ApplicationArgumentException {
    Key key;
    try {
      key = keyStore.getKey(keyStoreAlias, password);
    } catch (UnrecoverableKeyException | NoSuchAlgorithmException | KeyStoreException e) {
      throw new ApplicationArgumentException(
          String.format(
              "Cannot recover key from keystore. Alias: \"%s\", %s",
              keyStoreAlias, e.getMessage()
          )
      );
    }

    if (key == null) {
      throw new ApplicationArgumentException(
          String.format(
              "Cannot recover key, invalid alias: \"%s\"",
              keyStoreAlias
          )
      );
    }

    if (key instanceof PrivateKey) {
      return (PrivateKey) key;
    }
    throw new ApplicationArgumentException(
        "There is no PrivateKey instance under provided alias."
    );
  }

  private static KeyPairAndCertificate readKeyPairAndCertificateFromKeyStore(
      KeyStore keyStore,
      String keyStoreAlias,
      char[] password
  ) throws ApplicationArgumentException {
    try {
      PrivateKey key = readPrivateKeyFromKeyStore(keyStore, keyStoreAlias, password);
      KeyPair keyPair;
      Certificate certificate;
      certificate = keyStore.getCertificate(keyStoreAlias);
      PublicKey publicKey = certificate.getPublicKey();
      keyPair = new KeyPair(publicKey, key);
      return new KeyPairAndCertificate(keyPair, certificate);
    } catch (KeyStoreException e) {
      throw new ApplicationArgumentException(
          String.format(
              "Problem while reading certificate from provided keystore: %s",
              e.getMessage()
          )
      );
    }
  }

}
