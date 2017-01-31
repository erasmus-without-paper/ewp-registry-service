package eu.erasmuswithoutpaper.registry.echotester;

import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.UnrecoverableKeyException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.KeyManager;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;

/**
 * A simple helper for making new client connections to EWP Network endpoints.
 */
public class SimpleEwpClient {

  private static SSLSocketFactory prepareSocketFactory(X509Certificate cert, PrivateKey key) {
    try {
      /* Create an empty keystore (in JKS format). */

      KeyStore keystore = KeyStore.getInstance("JKS");
      keystore.load(null);

      /* Import our certificate and key into the keystore. */

      String keyPassword = "irrelevant";
      keystore.setCertificateEntry("cert", cert);
      keystore.setKeyEntry("key", key, keyPassword.toCharArray(), new Certificate[] { cert });

      /* Create a new SSL key manager with our certificate and key. */

      KeyManagerFactory kmf = KeyManagerFactory.getInstance("SunX509");
      kmf.init(keystore, keyPassword.toCharArray());
      KeyManager[] km = kmf.getKeyManagers();

      /* Return an appropriate SSLSocketFactory initialized with out cert and key. */

      SSLContext context = SSLContext.getInstance("TLS");
      context.init(km, null, null);
      return context.getSocketFactory();

    } catch (KeyStoreException | NoSuchAlgorithmException | CertificateException | IOException
        | UnrecoverableKeyException | KeyManagementException e) {
      throw new RuntimeException(e);
    }
  }

  private final SSLSocketFactory mySocketFactory;

  /**
   * Create a new client for the given client certificate.
   *
   * @param cert The client certificate to be used. If null, then no client certificate will be used
   *        when making new connections.
   * @param key The private key used to generate the client certificate. If null, then no client
   *        certificate will be used when making new connections.
   */
  public SimpleEwpClient(X509Certificate cert, PrivateKey key) {
    if ((cert != null && key != null)) {
      this.mySocketFactory = prepareSocketFactory(cert, key);
    } else {
      this.mySocketFactory = null;
    }
  }

  /**
   * Create a new {@link HttpsURLConnection} instance. This connection will be using the client
   * certificate which has been passed to the {@link SimpleEwpClient} in the constructor.
   *
   * @param url The URL to be opened.
   * @return An {@link HttpsURLConnection} instance (not connected) with proper
   *         {@link SSLSocketFactory} already preconfigured.
   */
  public HttpsURLConnection newConnection(URL url) {
    URLConnection aconn;
    try {
      aconn = url.openConnection();
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
    if (!(aconn instanceof HttpsURLConnection)) {
      throw new RuntimeException("Expecting HTTPS connection, got " + aconn + " instead.");
    }
    HttpsURLConnection conn = (HttpsURLConnection) aconn;
    if (this.mySocketFactory != null) {
      conn.setSSLSocketFactory(this.mySocketFactory);
    }
    return conn;
  }
}
