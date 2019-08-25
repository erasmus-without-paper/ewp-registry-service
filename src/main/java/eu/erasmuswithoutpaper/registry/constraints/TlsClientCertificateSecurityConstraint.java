package eu.erasmuswithoutpaper.registry.constraints;

import static org.joox.JOOX.$;

import java.io.ByteArrayInputStream;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.security.interfaces.RSAPublicKey;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

import eu.erasmuswithoutpaper.registry.common.Severity;
import eu.erasmuswithoutpaper.registry.common.Utils;
import eu.erasmuswithoutpaper.registry.documentbuilder.KnownNamespace;

import org.joox.Match;
import org.w3c.dom.Document;

/**
 * This constraint prevents the Manifest from introducing insecure TLS client certificates.
 */
public class TlsClientCertificateSecurityConstraint implements ManifestConstraint {

  private final int minKeyLength;

  /**
   * @param minKeyLength The minimum required bit length of the certificate's public key. All
   *        certificates weaker than this will be removed from the manifest.
   */
  public TlsClientCertificateSecurityConstraint(int minKeyLength) {
    this.minKeyLength = minKeyLength;
  }

  @Override
  public List<FailedConstraintNotice> filter(Document doc) {
    List<FailedConstraintNotice> notices = new ArrayList<>();

    CertificateFactory x509factory;
    try {
      x509factory = CertificateFactory.getInstance("X.509");
    } catch (CertificateException e) {
      throw new RuntimeException(e);
    }

    Match root = $(doc).namespaces(KnownNamespace.prefixMap());
    Match certs = root.xpath("mf5:host/mf5:client-credentials-in-use/mf5:certificate");
    for (int i = 0; i < certs.size(); i++) {

      Match certElem = certs.eq(i);
      String name = Utils.ordinal(i + 1) + " of " + certs.size();
      String certStr = certs.get(i).getTextContent().replaceAll("\\s+", "");
      byte[] decoded = Base64.getDecoder().decode(certStr);

      X509Certificate cert;
      try {
        cert = (X509Certificate) x509factory.generateCertificate(new ByteArrayInputStream(decoded));
      } catch (CertificateException e) {
        certElem.remove();
        notices.add(new FailedConstraintNotice(Severity.ERROR,
            "Invalid client certificate (" + name + "): " + e.getMessage()));
        continue;
      }

      if (cert.getSigAlgName().startsWith("MD")) {
        certElem.remove();
        StringBuilder sb = new StringBuilder();
        sb.append("One of your TLS client certificates (").append(name).append(") ");
        sb.append("uses an insecure MD-based signature algorithm ");
        sb.append('(').append(cert.getSigAlgName()).append("). ");
        sb.append("It will not be imported.");
        notices.add(new FailedConstraintNotice(Severity.ERROR, sb.toString()));
        continue;
      }

      RSAPublicKey publicKey = (RSAPublicKey) cert.getPublicKey();
      if (publicKey.getModulus().bitLength() < this.minKeyLength) {
        certElem.remove();
        StringBuilder sb = new StringBuilder();
        sb.append("The minimum required length of TLS client certificate key is ");
        sb.append(this.minKeyLength).append(" bits. One of your TLS client certificates (");
        sb.append(name).append(") ").append("uses ");
        sb.append(publicKey.getModulus().bitLength()).append(" bits only. It will not ");
        sb.append("be imported.");
        notices.add(new FailedConstraintNotice(Severity.ERROR, sb.toString()));
        continue;
      }
    }
    return notices;
  }
}
