package eu.erasmuswithoutpaper.registry.validators;

import static org.joox.JOOX.$;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import javax.xml.parsers.DocumentBuilder;

import eu.erasmuswithoutpaper.registry.common.Utils;
import eu.erasmuswithoutpaper.registry.documentbuilder.KnownNamespace;
import eu.erasmuswithoutpaper.registry.repository.CatalogueNotFound;
import eu.erasmuswithoutpaper.registry.repository.ManifestRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.stereotype.Service;

import org.joox.Match;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

@Service
@ConditionalOnWebApplication
public class RegistryCatalogueMatcherProvider implements CatalogueMatcherProvider {
  private final ManifestRepository repo;

  @Autowired
  public RegistryCatalogueMatcherProvider(ManifestRepository repo) {
    this.repo = repo;
  }

  @Override
  public Match getMatcher() {
    DocumentBuilder docBuilder = Utils.newSecureDocumentBuilder();
    Document doc;
    try {
      doc = docBuilder.parse(
          new ByteArrayInputStream(
              this.repo.getCatalogue().getBytes(StandardCharsets.UTF_8)
          )
      );
    } catch (SAXException | IOException | CatalogueNotFound e) {
      throw new RuntimeException(e);
    }

    return $(doc).namespaces(KnownNamespace.prefixMap());
  }
}
