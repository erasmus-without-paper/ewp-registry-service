package eu.erasmuswithoutpaper.registry.documentbuilder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
import static org.joox.JOOX.$;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import eu.erasmuswithoutpaper.registry.WRTest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

import org.junit.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

/**
 * Tests for {@link EwpDocBuilder}.
 */
public class EwpDocBuilderTest extends WRTest {

  @Autowired
  private ResourceLoader resLoader;

  @Autowired
  private EwpDocBuilder builder;

  @Test
  public void checkBuildErrorMessages1() {
    BuildResult result = this.builder.build(new BuildParams("<xml/>"));
    assertThat(result.isValid()).isFalse();
    assertThat(result.getRootNamespaceUri()).isEqualTo(null);
    assertThat(result.getRootLocalName()).isEqualTo("xml");
    assertThat(result.getErrors()).hasSize(1);
    BuildError error = result.getErrors().get(0);
    assertThat(error.getMessage())
        .isEqualTo("cvc-elt.1.a: Cannot find the declaration of element 'xml'.");
  }

  @Test
  public void checkBuildErrorMessages2() {
    StringBuilder sb = new StringBuilder();
    sb.append("<admin-email xmlns='");
    sb.append(KnownNamespace.COMMON_TYPES_V1.getNamespaceUri());
    sb.append("'>invalid</admin-email>");

    BuildParams params = new BuildParams(sb.toString());
    params.setPretty(true);
    BuildResult result = this.builder.build(params);
    assertThat(result.isValid()).isFalse();
    assertThat(result.getErrors()).hasSize(2);
    BuildError error = result.getErrors().get(0);
    assertThat(error.getMessage())
        .isEqualTo("cvc-pattern-valid: Value 'invalid' is not facet-valid with "
            + "respect to pattern '[^@]+@[^\\.]+\\..+' for type 'Email'.");
    assertThat(error.getLineNumber()).isEqualTo(1);
    error = result.getErrors().get(1);
    assertThat(error.getMessage())
        .isEqualTo("cvc-type.3.1.3: The value 'invalid' of element 'admin-email' is not valid.");
  }

  /**
   * This is similar to {@link #checkIfRejectsUnknownElements()}, but from other perspective. If the
   * element is invalid (according to its schema), but this schema is unknown to the validator, then
   * the validator SHOULD accept the invalid content.
   */
  @Test
  public void checkIfAcceptsInvalidAPIentries() {
    BuildParams params =
        new BuildParams(this.getFile("manifests/which-fails-external-api-schema-validation.xml"));
    params.setExpectedKnownElement(KnownElement.RESPONSE_MANIFEST_V4);
    BuildResult result = this.builder.build(params);
    assertThat(result.isValid()).isTrue();
  }

  /**
   * It should accept all global elements from any of EWP schemas (not only the catalogue and
   * manifest elements).
   */
  @Test
  public void checkIfAcceptsKnownElements() {
    BuildParams params = new BuildParams(this.getFile("docbuilder/valid-hei.xml"));
    params.setPretty(true);
    BuildResult result = this.builder.build(params);
    assertThat(result.isValid()).isTrue();
    assertThat(result.getRootNamespaceUri())
        .isEqualTo(KnownNamespace.RESPONSE_REGISTRY_V1.getNamespaceUri());
    assertThat(result.getRootLocalName()).isEqualTo("hei");
    assertThat(result.getErrors()).hasSize(0);
  }


  /**
   * Builder should reject elements from outside of our known schemas, even if they're valid. We do
   * not want the builder to attempt to fetch the schemas "on-demand" from random sources on the
   * web, we want it to fail fast.
   */
  @Test
  public void checkIfRejectsUnknownElements() {
    BuildParams params = new BuildParams(this.getFile("docbuilder/valid-gpx.xml"));
    params.setPretty(true);
    BuildResult result = this.builder.build(params);
    assertThat(result.getRootNamespaceUri()).isEqualTo("http://www.topografix.com/GPX/1/1");
    assertThat(result.getRootLocalName()).isEqualTo("gpx");
    assertThat(result.isValid()).isFalse();
    assertThat(result.getErrors()).hasSize(1);
    BuildError error = result.getErrors().get(0);
    assertThat(error.getMessage())
        .isEqualTo("cvc-elt.1.a: Cannot find the declaration of element 'gpx'.");
    assertThat(error.getLineNumber()).isEqualTo(10);
  }

  /**
   * Check if the <code>schemas/__index__.xml</code> file covers all the XML schemas in the
   * <code>schemas</code> directory.
   */
  @Test
  public void checkSchemasConsistentency() {

    // Scan the classpath for all XSD files.

    List<String> localPaths = new ArrayList<>();
    PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
    try {
      for (Resource resource : resolver.getResources("classpath:schemas/**/*.xsd")) {
        localPaths.add(resource.getURI().toString());
      }
    } catch (IOException e) {
      throw new RuntimeException(e);
    }

    // Fetch the same XSD paths from the catalog.

    List<String> catalogPaths = new ArrayList<>();
    Resource xmlCatalog = this.resLoader.getResource("classpath:schemas/__index__.xml");
    try {
      for (Element element : $(xmlCatalog.getInputStream()).find("uri")) {
        catalogPaths.add($(element).attr("uri"));
      }
    } catch (IOException e) {
      throw new RuntimeException(e);
    } catch (SAXException e) {
      throw new RuntimeException(e);
    }

    // Compare the two.

    assertThat(catalogPaths.size()).isEqualTo(localPaths.size());
    for (String cpth : catalogPaths) {
      String match = null;
      for (String lpth : localPaths) {
        if (lpth.endsWith("/" + cpth)) {
          match = lpth;
          break;
        }
      }
      if (match != null) {
        localPaths.remove(match);
      } else {
        fail("No match found for " + cpth);
      }
    }
    assertThat(localPaths).isEmpty();
  }

  /**
   * Make sure it allows external (unknown) APIs entries.
   */
  @Test
  public void testExternalAPIs() {
    BuildParams params = new BuildParams(this.getFile("manifests/a-bit-weird-but-valid.xml"));
    params.setExpectedKnownElement(KnownElement.RESPONSE_MANIFEST_V4);
    BuildResult result = this.builder.build(params);
    assertThat(result.isValid()).isTrue();
  }

  @Test
  public void testManifestExample() {
    BuildParams params = new BuildParams(
        this.getFile("latest-examples/ewp-specs-api-discovery-manifest-example.xml"));
    params.setExpectedKnownElement(KnownElement.RESPONSE_MANIFEST_V4);
    BuildResult result = this.builder.build(params);
    assertThat(result.isValid()).isTrue();
  }

  @Test
  public void testManifestMinimal() {
    BuildParams params = new BuildParams(this.getFile("manifests/tiny-but-valid.xml"));
    params.setExpectedKnownElement(KnownElement.RESPONSE_MANIFEST_V4);
    BuildResult result = this.builder.build(params);
    assertThat(result.isValid()).isTrue();
  }

  /**
   * Test if line numbers are correct in regular (non-pretty) mode.
   */
  @Test
  public void testNonPrettyResult() {
    byte[] input = this.getFile("docbuilder/invalid-manifest.xml");
    BuildResult result = this.builder.build(new BuildParams(input));
    assertThat(result.isValid()).isFalse();
    assertThat(result.getErrors()).hasSize(1);
    BuildError error = result.getErrors().get(0);
    assertThat(error.getMessage())
        .isEqualTo("cvc-complex-type.4: Attribute 'id' must appear on element 'rrrr:hei'.");
    assertThat(error.getLineNumber()).isEqualTo(18);
    assertThat(result.getDocument().get()).isInstanceOf(Document.class);
    assertThat(result.getPrettyLines()).isNotPresent();
  }

  /**
   * Test if line numbers (along with {@link BuildResult#getPrettyLines()} and
   * {@link BuildResult#getPrettyXml()}) are correct in pretty mode.
   */
  @Test
  public void testPrettyResult() {
    byte[] input = this.getFile("docbuilder/invalid-manifest.xml");
    BuildParams params = new BuildParams(input);
    params.setPretty(true);
    BuildResult result = this.builder.build(params);
    assertThat(result.isValid()).isFalse();
    assertThat(result.getErrors()).hasSize(1);
    BuildError error = result.getErrors().get(0);
    assertThat(error.getMessage())
        .isEqualTo("cvc-complex-type.4: Attribute 'id' must appear on element 'rrrr:hei'.");
    assertThat(error.getLineNumber()).isEqualTo(12);
    assertThat(result.getDocument().get()).isInstanceOf(Document.class);
    assertThat(result.getPrettyXml()).isPresent();
    assertThat(result.getPrettyXml().get())
        .isEqualTo(this.getFileAsString("docbuilder/invalid-manifest-pretty.xml"));
    assertThat(result.getPrettyLines()).isPresent();
    assertThat(result.getPrettyLines().get().get(12 - 1)).contains("<rrrr:hei>");
  }
}
