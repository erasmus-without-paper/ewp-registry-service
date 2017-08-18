package eu.erasmuswithoutpaper.registry.constraints;

import static org.joox.JOOX.$;

import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

import eu.erasmuswithoutpaper.registry.common.Severity;
import eu.erasmuswithoutpaper.registry.common.Utils;
import eu.erasmuswithoutpaper.registry.documentbuilder.KnownNamespace;

import org.joox.Match;
import org.w3c.dom.Document;

/**
 * This constraint prevents the Manifest from introducing insecure or invalid RSA keys.
 */
public class ClientKeySecurityConstraint implements ManifestConstraint {

  private final int minKeyLength;

  /**
   * @param minKeyLength The minimum required bit length of the public key. All keys weaker than
   *        this will be removed from the manifest.
   */
  public ClientKeySecurityConstraint(int minKeyLength) {
    this.minKeyLength = minKeyLength;
  }

  @Override
  public List<FailedConstraintNotice> filter(Document doc) {
    List<FailedConstraintNotice> notices = new ArrayList<>();

    KeyFactory rsaFactory;
    try {
      rsaFactory = KeyFactory.getInstance("RSA");
    } catch (NoSuchAlgorithmException e) {
      throw new RuntimeException(e);
    }

    Match root = $(doc).namespaces(KnownNamespace.prefixMap());
    Match keyElems = root.xpath("mf:client-credentials-in-use/mf:rsa-public-key");
    for (int i = 0; i < keyElems.size(); i++) {

      Match keyElem = keyElems.eq(i);
      String name = Utils.ordinal(i + 1) + " of " + keyElems.size();
      String keyStr = keyElems.get(i).getTextContent().replaceAll("\\s+", "");
      byte[] decoded = Base64.getDecoder().decode(keyStr);

      RSAPublicKey publicKey;
      try {
        publicKey = (RSAPublicKey) rsaFactory.generatePublic(new X509EncodedKeySpec(decoded));
      } catch (InvalidKeySpecException e) {
        keyElem.remove();
        notices.add(new FailedConstraintNotice(Severity.ERROR,
            "Invalid client public key (" + name + "): " + e.getMessage()));
        continue;
      }

      if (publicKey.getModulus().bitLength() < this.minKeyLength) {
        keyElem.remove();
        StringBuilder sb = new StringBuilder();
        sb.append("The minimum required length of client keys is ");
        sb.append(this.minKeyLength).append(" bits. One of your keys (");
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
