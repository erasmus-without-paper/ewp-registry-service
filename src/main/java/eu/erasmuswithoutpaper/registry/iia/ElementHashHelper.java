package eu.erasmuswithoutpaper.registry.iia;

import java.io.ByteArrayOutputStream;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.xml.security.Init;
import org.apache.xml.security.c14n.Canonicalizer;
import org.apache.xml.security.exceptions.XMLSecurityException;
import org.w3c.dom.Node;

public class ElementHashHelper {

  static String getXmlHash(Node element) throws ElementHashException {
    try {
      Init.init();
      Canonicalizer canon =
          Canonicalizer.getInstance(Canonicalizer.ALGO_ID_C14N_EXCL_OMIT_COMMENTS);
      ByteArrayOutputStream canonicalWriter = new ByteArrayOutputStream();
      canon.canonicalizeSubtree(element, canonicalWriter);

      return DigestUtils.sha256Hex(canonicalWriter.toByteArray());
    } catch (XMLSecurityException cause) {
      throw new ElementHashException(cause);
    }
  }
}
