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
import eu.erasmuswithoutpaper.registryclient.RegistryClient;

import org.joox.Match;
import org.w3c.dom.Document;

/**
 * This constraint prevents the Manifest from introducing insecure or invalid RSA keys.
 */
public abstract class AbstractRsaKeySecurityConstraint implements ManifestConstraint {

  private final int minKeyLength;

  /**
   * @param minKeyLength The minimum required bit length of the public key. All keys weaker than
   *        this will be removed from the manifest.
   */
  public AbstractRsaKeySecurityConstraint(int minKeyLength) {
    this.minKeyLength = minKeyLength;
  }

  @Override
  public List<FailedConstraintNotice> filter(Document doc, RegistryClient registryClient) {
    List<FailedConstraintNotice> notices = new ArrayList<>();

    KeyFactory rsaFactory;
    try {
      rsaFactory = KeyFactory.getInstance("RSA");
    } catch (NoSuchAlgorithmException e) {
      throw new RuntimeException(e);
    }

    Match root = $(doc).namespaces(KnownNamespace.prefixMap());
    Match heis = root.xpath(
        "mf5:host/mf5:institutions-covered/r:hei | " + "mf6:host/mf6:institutions-covered/r:hei");
    // heis contains at most one element (see VerifySingleHost/Hei constraints)
    String heiCovered = heis.size() == 0 ? null : heis.get(0).getAttribute("id");
    Match keyElems = root.xpath(this.getXPath());

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
            "Invalid " + this.getKeyName() + " (" + name + "): " + e.getMessage()));
        continue;
      }

      FailedConstraintNotice notice = verifyKey(publicKey, name, heiCovered, registryClient);
      if (notice != null) {
        if (notice.getSeverity().equals(Severity.ERROR)) {
          keyElem.remove();
        }
        notices.add(notice);
      }
    }
    return notices;
  }

  protected FailedConstraintNotice verifyKey(RSAPublicKey publicKey, String keyNumber,
      String heiCovered, RegistryClient registryClient) {
    if (publicKey.getModulus().bitLength() < this.minKeyLength) {
      return new FailedConstraintNotice(Severity.ERROR,
          "The minimum required length of " + this.getKeyName() + " is " + this.minKeyLength
              + " bits. One of your keys (" + keyNumber + ") " + "uses " + publicKey.getModulus()
              .bitLength() + " bits only. It will not " + "be imported.");
    }
    return null;
  }

  /**
   * @return The name of the key to be used in failure notices. Lower case.
   */
  protected abstract String getKeyName();

  /**
   * @return XPath at which to look for RSA Public Keys to analyze.
   */
  protected abstract String getXPath();
}
