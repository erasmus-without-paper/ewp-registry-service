package eu.erasmuswithoutpaper.registry.sourceprovider;

import static org.joox.JOOX.$;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import javax.xml.parsers.DocumentBuilder;

import eu.erasmuswithoutpaper.registry.common.Utils;
import eu.erasmuswithoutpaper.registry.constraints.ManifestConstraint;
import eu.erasmuswithoutpaper.registry.constraints.RestrictInstitutionsCovered;
import eu.erasmuswithoutpaper.registry.web.SelfManifestProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.annotation.Profile;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Service;

import com.google.common.collect.Lists;
import org.joox.Match;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

/**
 * Official list of manifest sources, to be used in production environment.
 *
 * <p>
 * Currently, this list is hardcoded (this will probably change in the future).
 * </p>
 */
@Service
@Profile({"production", "development"})
@ConditionalOnWebApplication
public class ProductionManifestSourceProvider extends ManifestSourceProvider {

  private static final Logger logger =
      LoggerFactory.getLogger(ProductionManifestSourceProvider.class);

  private final String rootUrl;
  private final List<String> manifestSourcesUrls;
  private final ResourceLoader resourceLoader;
  private final SelfManifestProvider selfManifestProvider;
  private final ManifestSourceFactory manifestFactory;

  private List<ManifestSource> sources;

  /**
   * @param rootUrl
   *     needed to construct a proper URL to Registry's own self-manifest.
   * @param manifestSourcesUrls
   *     location of the manifest-sources.xml files.
   * @param resourceLoader
   *     needed to load the manifest-sources.xml file.
   * @param selfManifestProvider
   *     needed to get self manifests.
   */
  @Autowired
  public ProductionManifestSourceProvider(@Value("${app.root-url}") String rootUrl,
      @Value("${app.manifest-sources.urls}") List<String> manifestSourcesUrls,
      ResourceLoader resourceLoader, SelfManifestProvider selfManifestProvider,
      ManifestSourceFactory manifestFactory) {
    this.rootUrl = rootUrl;
    this.manifestSourcesUrls = manifestSourcesUrls;
    this.resourceLoader = resourceLoader;
    this.selfManifestProvider = selfManifestProvider;
    this.manifestFactory = manifestFactory;
    this.update();
  }

  @Override
  public List<ManifestSource> getAll() {
    return this.sources;
  }

  @Override
  public final void update() {
    // The first manifest source is our own.

    ArrayList<ManifestSource> lst = new ArrayList<>();
    if (rootUrl.startsWith("https://")) {
      Set<String> manifests = this.selfManifestProvider.getManifests().keySet();

      for (String manifest : manifests) {
        String url = rootUrl + "/manifest-" + manifest + ".xml";
        ManifestSource manifestSource = manifestFactory.newTrustedSource(url);
        logger.info("Adding self-manifest source: " + manifestSource);
        lst.add(manifestSource);
      }
    }

    /*
     * The rest of the manifest sources will be loaded from the manifest-sources.xml files.
     */

    for (String manifestSourcesUrl : manifestSourcesUrls) {
      Resource resource = resourceLoader.getResource(manifestSourcesUrl);
      if (!resource.exists()) {
        throw new RuntimeException("Resource does not exist: " + resource);
      }
      logger.info("Loading manifest sources from " + resource);
      Match sources;
      try {
        DocumentBuilder docBuilder = Utils.newSecureDocumentBuilder();
        Document doc = docBuilder.parse(resource.getInputStream());
        sources = $(doc).find("source");
      } catch (SAXException | IOException e) {
        throw new RuntimeException(e);
      }
      for (Element source : sources) {
        String location = $(source).find("location").text();
        List<ManifestConstraint> constraints = Lists.newArrayList();
        for (String regex : $(source).find("hei-regex").texts()) {
          constraints.add(new RestrictInstitutionsCovered(regex));
        }
        ManifestSource manifestSource = manifestFactory.newRegularSource(location, constraints);
        logger.info("Adding new manifest source: " + manifestSource);
        lst.add(manifestSource);
      }
    }

    this.sources = Collections.unmodifiableList(lst);
  }
}
