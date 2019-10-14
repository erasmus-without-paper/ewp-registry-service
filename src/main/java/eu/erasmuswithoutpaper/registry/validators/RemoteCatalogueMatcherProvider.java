package eu.erasmuswithoutpaper.registry.validators;


import static org.joox.JOOX.$;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import javax.xml.parsers.DocumentBuilder;

import eu.erasmuswithoutpaper.registry.common.Utils;
import eu.erasmuswithoutpaper.registry.documentbuilder.KnownNamespace;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnNotWebApplication;
import org.springframework.stereotype.Service;

import org.joox.Match;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

@Service
@ConditionalOnNotWebApplication
public class RemoteCatalogueMatcherProvider implements CatalogueMatcherProvider {
  private URL catalogueUrl;

  /**
   * Returns {@link Match}er that can be used to match against contents of the catalogue.
   *
   * @param registryDomain
   *     url where the catalogue can be found, set ${app.registry-domain} to change it.
   */
  @Autowired
  public RemoteCatalogueMatcherProvider(
      @Value("${app.registry-domain:#{null}}") String registryDomain) {
    if (registryDomain == null) {
      registryDomain = "dev-registry.erasmuswithoutpaper.eu";
    }

    try {
      this.catalogueUrl = new URL("https://" + registryDomain + "/catalogue-v1.xml");
    } catch (MalformedURLException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public Match getMatcher() {
    DocumentBuilder docBuilder = Utils.newSecureDocumentBuilder();
    Document doc;

    try (InputStream manifestStream = this.catalogueUrl.openStream()) {
      doc = docBuilder.parse(manifestStream);
    } catch (SAXException | IOException e) {
      throw new RuntimeException(e);
    }

    return $(doc).namespaces(KnownNamespace.prefixMap());
  }
}
