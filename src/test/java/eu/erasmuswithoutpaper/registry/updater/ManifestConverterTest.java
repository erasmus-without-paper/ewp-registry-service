package eu.erasmuswithoutpaper.registry.updater;

import static org.assertj.core.api.Assertions.assertThat;

import eu.erasmuswithoutpaper.registry.WRTest;
import eu.erasmuswithoutpaper.registry.documentbuilder.BuildParams;
import eu.erasmuswithoutpaper.registry.documentbuilder.BuildResult;
import eu.erasmuswithoutpaper.registry.documentbuilder.EwpDocBuilder;
import eu.erasmuswithoutpaper.registry.documentbuilder.KnownElement;
import eu.erasmuswithoutpaper.registry.xmlformatter.XmlFormatter;

import org.springframework.beans.factory.annotation.Autowired;

import org.junit.Test;
import org.w3c.dom.Document;

/**
 * Tests for the {@link RegistryUpdaterImpl}.
 */
public class ManifestConverterTest extends WRTest {

  @Autowired
  private EwpDocBuilder docbuilder;

  @Autowired
  private XmlFormatter xmlFormatter;

  @Autowired
  private ManifestConverter converter;

  @Test
  public void testOnFiles() {
    Document v4 = this.loadV4("manifests-v4/all-elems.xml");
    Document v5 = this.converter.convertFromV4ToV5(v4);
    String v5string = this.xmlFormatter.format(v5);
    assertThat(v5string)
        .isEqualTo(this.getFileAsString("manifests-v5/all-elems-converted-from-v4.xml"));
  }

  private Document loadV4(String filename) {
    byte[] input = this.getFile(filename);
    BuildParams params = new BuildParams(input);
    params.setExpectedKnownElement(KnownElement.RESPONSE_MANIFEST_V4);
    BuildResult result = this.docbuilder.build(params);
    assertThat(result.isValid()).isTrue();
    return result.getDocument().get();
  }
}
