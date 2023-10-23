package eu.erasmuswithoutpaper.registry.consoleapplication;

import static eu.erasmuswithoutpaper.registry.consoleapplication.ApplicationParametersUtils.readParameter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyStore;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.interfaces.RSAPrivateCrtKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import eu.erasmuswithoutpaper.registry.common.KeyPairAndCertificate;
import eu.erasmuswithoutpaper.registry.common.KeyStoreUtils;
import eu.erasmuswithoutpaper.registry.common.KeyStoreUtilsException;
import org.springframework.boot.ApplicationArguments;

import org.bouncycastle.openssl.PEMParser;
import org.bouncycastle.util.io.pem.PemObject;

public class CryptoParameters {

  /**
   * Returns help text lines for crypto parameters.
   *
   * @return List of String that should be a part of help message.
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
        "    --http-server-use-client-key - Uses same key that was is used by HTTP Sig Client.",
        "  Keys for permission tests, provided by EWP Administrators:",
        "    --confidentiality-tls-keystore=<path>",
        "    --confidentiality-tls-keystore-alias=<alias>",
        "    --confidentiality-tls-keystore-password=<password>"
    );
  }

  private static final String JKS_KEY_FORMAT = "JKS";
  private static final String JKS_FILE_EXT = ".jks";

  private static final String PKCS12_KEY_FORMAT = "PKCS12";
  private static final String PKCS12_FILE_EXT = ".pkcs12";
  private static final String P12_FILE_EXT = ".p12";
  private static final String PFX_FILE_EXT = ".pfx";

  private static final List<String> KEY_FORMATS = Arrays.asList(
      JKS_KEY_FORMAT,
      PKCS12_KEY_FORMAT
  );

  private static KeyPair readKeyPairFromKeyStoreParameters(
      ApplicationArguments args,
      String keyStorePath,
      String keyPairName)
      throws ApplicationArgumentException, KeyStoreUtilsException {
    String clientKeyStoreFormat = getKeyStoreFormat(args, keyStorePath, keyPairName);
    String keyStoreAlias = readParameter(args, keyPairName + "-keystore-alias", "alias");
    String keyStorePassword = readParameter(args, keyPairName + "-keystore-password", "password",
        false);
    char[] password = keyStorePassword == null ? null : keyStorePassword.toCharArray();

    KeyStore keyStore = KeyStoreUtils.loadKeyStore(keyStorePath, clientKeyStoreFormat, password);
    PrivateKey privateKey = KeyStoreUtils
        .readPrivateKeyFromKeyStore(keyStore, keyStoreAlias, password);
    PublicKey publicKey = KeyStoreUtils.getPublicKeyFromPrivateKey((RSAPrivateCrtKey) privateKey);
    return new KeyPair(publicKey, privateKey);
  }

  private static KeyPair readHttpSigKeyFromParameters(ApplicationArguments args, String keyName)
      throws ApplicationArgumentException, KeyStoreUtilsException {
    String keyStorePathParameter = "http-" + keyName + "-keystore";
    String pemPathParameter = "http-" + keyName + "-pem";
    String keyStorePath = readParameter(args, keyStorePathParameter, "file-path", false);
    String pemPath = readParameter(args, pemPathParameter, "file-path", false);
    if (keyStorePath != null && pemPath != null) {
      throw new ApplicationArgumentException(
          "You should provide only one http-" + keyName + " private key path."
      );
    }

    KeyPair keyPair;
    if (keyStorePath != null) {
      keyPair = readKeyPairFromKeyStoreParameters(args, keyStorePath, "http-" + keyName);
    } else if (pemPath != null) {
      keyPair = readPkcs8KeyFromFile(pemPath);
    } else {
      return null;
    }
    return keyPair;
  }

  /**
   * Read Client HTTP Sig KeyPair from file given in parameters.
   *
   * @param args
   *     Arguments passed to the executable.
   * @return KeyPair loaded from path found in arguments.
   * @throws ApplicationArgumentException
   *     Thrown when arguments passed to the executable contain incorrect values.
   * @throws KeyStoreUtilsException
   *     Thrown when there is a problem with reading keys from the keystore specified in arguments.
   */
  public static KeyPair readHttpSigClientKeyPair(ApplicationArguments args)
      throws ApplicationArgumentException, KeyStoreUtilsException {
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
   *
   * @param args
   *     Arguments passed to the executable.
   * @return KeyPair loaded from path found in arguments.
   * @throws ApplicationArgumentException
   *     Thrown when arguments passed to the executable contain incorrect values.
   * @throws KeyStoreUtilsException
   *     Thrown when there is a problem with reading keys from the keystore specified in arguments.
   */
  public static KeyPair readHttpSigServerKeyPair(ApplicationArguments args)
      throws ApplicationArgumentException, KeyStoreUtilsException {
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

  private static KeyPair readPkcs8KeyFromFile(String clientPemPath)
      throws ApplicationArgumentException, KeyStoreUtilsException {
    PemObject pemObject;
    try (PEMParser pemParser = new PEMParser(
        Files.newBufferedReader(Paths.get(clientPemPath), StandardCharsets.UTF_8)
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
      PublicKey publicKey = KeyStoreUtils.getPublicKeyFromPrivateKey((RSAPrivateCrtKey) privateKey);
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
   * Read TLS PrivateKey and Certificate from KeyStore given in parameters for permission testing.
   *
   * @param args
   *     Arguments passed to the executable.
   * @return KeyPair and Certificate loaded from path found in arguments.
   * @throws ApplicationArgumentException
   *     Thrown when arguments passed to the executable contain incorrect values.
   * @throws KeyStoreUtilsException
   *     Thrown when there is a problem with reading keys from the keystore specified in arguments.
   */
  public static KeyPairAndCertificate readPermissionTlsKeyAndCertificateFromParameters(
      ApplicationArguments args)
      throws ApplicationArgumentException, KeyStoreUtilsException {
    return readTlsKeyAndCertificateFromParametersWithPrefix(args, "confidentiality-");
  }

  /**
   * Read TLS PrivateKey and Certificate from file given in parameters.
   *
   * @param args
   *     Arguments passed to the executable.
   * @return KeyPair and Certificate loaded from path found in arguments.
   * @throws ApplicationArgumentException
   *     Thrown when arguments passed to the executable contain incorrect values.
   * @throws KeyStoreUtilsException
   *     Thrown when there is a problem with reading keys from the keystore specified in arguments.
   */
  public static KeyPairAndCertificate readTlsKeyAndCertificateFromParameters(
      ApplicationArguments args)
      throws ApplicationArgumentException, KeyStoreUtilsException {
    return readTlsKeyAndCertificateFromParametersWithPrefix(args, "");
  }

  private static KeyPairAndCertificate readTlsKeyAndCertificateFromParametersWithPrefix(
      ApplicationArguments args, String prefix)
      throws ApplicationArgumentException, KeyStoreUtilsException {
    String clientKeyStorePath = readParameter(args, prefix + "tls-keystore", "file-path", false);
    if (clientKeyStorePath == null) {
      return null;
    }
    String clientKeyStoreFormat = getKeyStoreFormat(args, clientKeyStorePath, "tls");
    String keyStoreAlias = readParameter(args, prefix + "tls-keystore-alias", "alias");
    String keyStorePassword = readParameter(
        args, prefix + "tls-keystore-password", "password", false);
    char[] password = keyStorePassword == null ? null : keyStorePassword.toCharArray();

    KeyStore keyStore = KeyStoreUtils
        .loadKeyStore(clientKeyStorePath, clientKeyStoreFormat, password);
    return KeyStoreUtils.readKeyPairAndCertificateFromKeyStore(
        keyStore,
        keyStoreAlias,
        password
    );
  }

  private static String getKeyStoreFormat(
      ApplicationArguments args,
      String clientKeyStorePath,
      String securityProtocolPrefix) throws ApplicationArgumentException {
    String parameter = securityProtocolPrefix + "-keystore-format";
    String clientKeyStoreFormat = readParameter(args, parameter, "key store format",
        false);

    if (clientKeyStoreFormat == null) {
      String lowerCasePath = clientKeyStorePath.toLowerCase(Locale.getDefault());
      if (lowerCasePath.endsWith(JKS_FILE_EXT)) {
        return JKS_KEY_FORMAT;
      } else if (lowerCasePath.endsWith(PKCS12_FILE_EXT)
          || lowerCasePath.endsWith(PFX_FILE_EXT)
          || lowerCasePath.endsWith(P12_FILE_EXT)) {
        return PKCS12_KEY_FORMAT;
      } else {
        throw new ApplicationArgumentException(
            "Cannot infer KeyStore format from filename, please provide " + parameter
            + " parameter.");
      }
    }

    if (!KEY_FORMATS.contains(clientKeyStoreFormat)) {
      throw new ApplicationArgumentException(
          "Invalid key format, available key formats are " + KEY_FORMATS.toString());
    }
    return clientKeyStoreFormat;
  }

}
