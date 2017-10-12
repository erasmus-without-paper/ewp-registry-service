package eu.erasmuswithoutpaper.registry.updater;

import static org.assertj.core.api.Assertions.assertThat;
import static org.joox.JOOX.$;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import eu.erasmuswithoutpaper.registry.WRTest;
import eu.erasmuswithoutpaper.registry.common.Severity;
import eu.erasmuswithoutpaper.registry.common.Utils;
import eu.erasmuswithoutpaper.registry.constraints.RestrictInstitutionsCovered;
import eu.erasmuswithoutpaper.registry.documentbuilder.BuildParams;
import eu.erasmuswithoutpaper.registry.documentbuilder.EwpDocBuilder;
import eu.erasmuswithoutpaper.registry.documentbuilder.KnownNamespace;
import eu.erasmuswithoutpaper.registry.internet.FakeInternet;
import eu.erasmuswithoutpaper.registry.notifier.NotifierService;
import eu.erasmuswithoutpaper.registry.repository.CatalogueNotFound;
import eu.erasmuswithoutpaper.registry.repository.ManifestRepositoryImpl;
import eu.erasmuswithoutpaper.registry.sourceprovider.ManifestSource;
import eu.erasmuswithoutpaper.registry.sourceprovider.TestManifestSourceProvider;
import eu.erasmuswithoutpaper.registry.xmlformatter.XmlFormatter;

import org.springframework.beans.factory.annotation.Autowired;

import org.javatuples.Pair;
import org.joox.Match;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Tests for the {@link RegistryUpdaterImpl}.
 */
public class RegistryUpdaterTest extends WRTest {

  private static String url1;

  private static String url2;

  private static String url3;

  @BeforeClass
  public static void setUpClass() {
    url1 = "https://example.com/manifest.xml";
    url2 = "https://example.com/manifest2.xml";
    url3 = "https://example.com/manifest3.xml";
  }

  @Autowired
  private ManifestRepositoryImpl repo;

  @Autowired
  private FakeInternet internet;

  @Autowired
  private TestManifestSourceProvider sourceProvider;

  @Autowired
  private RegistryUpdaterImpl updater;

  @Autowired
  private ManifestUpdateStatusRepository updateStatuses;

  @Autowired
  private EwpDocBuilder builder;
  @Autowired
  private NotifierService notifier;
  @Autowired
  private EwpDocBuilder docBuilder;

  @Autowired
  private XmlFormatter xmlFormatter;
  private Match lastCatalogue;

  private List<String> lastEmails;

  /**
   * For consistency, we want the catalogue example from the official Registry API specs to be based
   * on a similar manifest example from the official Discovery API specs. This test will verify
   * that.
   */
  @Test
  public void checkIfOfficialExamplesAreConsistent() {
    /*
     * Since the catalogue example contains a Registry API entry, we need to add additional manifest
     * (with the API) to our sources.
     */
    String urlA = "https://registry.erasmuswithoutpaper.eu/manifest.xml";
    String urlB = "https://example.com/manifest.xml";
    this.internet.putURL(urlA, this.getFile("manifests/sample-registry-manifest.xml"));
    this.internet.putURL(urlB,
        this.getFile("latest-examples/ewp-specs-api-discovery-manifest-example.xml"));
    this.sourceProvider.addSource(ManifestSource.newTrustedSource(urlA));
    this.sourceProvider.addSource(ManifestSource.newRegularSource(urlB,
        Arrays.asList(new RestrictInstitutionsCovered(".*\\.pl"))));
    this.timePasses();
    assertThat(this.updateStatuses.findOne(urlA).getLastAccessFlagStatus()).isEqualTo(Severity.OK);
    assertThat(this.updateStatuses.findOne(urlB).getLastAccessFlagStatus()).isEqualTo(Severity.OK);
    try {
      assertThat(
          this.getFileAsString("latest-examples/ewp-specs-api-registry-catalogue-example.xml"))
              .isEqualTo(this.repo.getCatalogue());
    } catch (CatalogueNotFound e) {
      throw new RuntimeException(e);
    }
  }

  @Before
  public void setUp() {
    this.sourceProvider.clearSources();
    this.repo.deleteAll();
    this.internet.clearURLs();
    this.internet.clearEmailsSent();
    this.lastCatalogue = null;
    this.lastEmails = null;
  }

  /**
   * Test if the Registry is properly importing `rsa-public-key` elements, introduced in Discovery
   * API v4.1.0.
   */
  @Test
  public void testPublicClientKeys() {

    /* Test if a valid keys are imported. */

    this.assertManifestStatuses(null, null, null);
    this.internet.putURL(url1, this.getFile("rsa-public-key-tests/manifest1.xml"));
    ManifestSource ms1 = ManifestSource.newRegularSource(url1, Arrays.asList());
    this.sourceProvider.addSource(ms1);
    this.timePasses();
    this.assertManifestStatuses("OK", null, null);
    assertThat(this.lastCatalogue.xpath("/r:catalogue/r:host").size()).isEqualTo(1);
    assertThat(this.lastCatalogue
        .xpath("/r:catalogue/r:host/r:client-credentials-in-use/r:rsa-public-key").size())
            .isEqualTo(1);
    Match keyElem = $(this.lastCatalogue
        .xpath("/r:catalogue/r:host/r:client-credentials-in-use/r:rsa-public-key").get(0));
    assertThat(keyElem.attr("sha-256"))
        .isEqualTo("5531f9a02c44a894d0b706961259fec740ad4ae8a3555871f1a5cd9801285bd4");

    /* Make sure that too-short keys are NOT imported. */

    this.internet.putURL(url1, this.getFile("rsa-public-key-tests/manifest2.xml"));
    this.timePasses();
    this.assertManifestStatuses("Error", null, null);
    this.assertNoticesMatch(url1,
        "(?s).*minimum required length of client public key is 2048 bits.*");
    assertThat(this.lastCatalogue
        .xpath("/r:catalogue/r:host/r:client-credentials-in-use/r:rsa-public-key").size())
            .isEqualTo(0);

    /*
     * Make sure that invalid keys (e.g. invalid encoding) don't break anything. This manifest
     * contains two keys - one invalid and one valid. The valid one should still be imported.
     */

    this.internet.putURL(url1, this.getFile("rsa-public-key-tests/manifest3.xml"));
    this.timePasses();
    this.assertManifestStatuses("Error", null, null);
    this.assertNoticesMatch(url1,
        "(?s).*Invalid client public key \\(1st of 2\\).*premature EOF.*");
    assertThat(this.lastCatalogue
        .xpath("/r:catalogue/r:host/r:client-credentials-in-use/r:rsa-public-key").size())
            .isEqualTo(1);
    keyElem = $(this.lastCatalogue
        .xpath("/r:catalogue/r:host/r:client-credentials-in-use/r:rsa-public-key").get(0));
    assertThat(keyElem.attr("sha-256"))
        .isEqualTo("5531f9a02c44a894d0b706961259fec740ad4ae8a3555871f1a5cd9801285bd4");
  }

  /**
   * Test if the Registry is properly importing `rsa-public-key` elements, introduced in Discovery
   * API v4.1.0.
   */
  @Test
  public void testPublicServerKeys() {

    /* Test if a valid keys are imported. */

    this.assertManifestStatuses(null, null, null);
    this.internet.putURL(url1, this.getFile("rsa-public-key-tests/server1.xml"));
    ManifestSource ms1 = ManifestSource.newRegularSource(url1, Arrays.asList());
    this.sourceProvider.addSource(ms1);
    this.timePasses();
    this.assertManifestStatuses("OK", null, null);
    assertThat(this.lastCatalogue.xpath("/r:catalogue/r:host").size()).isEqualTo(1);
    assertThat(this.lastCatalogue
        .xpath("/r:catalogue/r:host/r:server-credentials-in-use/r:rsa-public-key").size())
            .isEqualTo(1);
    Match keyElem = $(this.lastCatalogue
        .xpath("/r:catalogue/r:host/r:server-credentials-in-use/r:rsa-public-key").get(0));
    assertThat(keyElem.attr("sha-256"))
        .isEqualTo("5531f9a02c44a894d0b706961259fec740ad4ae8a3555871f1a5cd9801285bd4");

    /* Make sure that too-short keys are NOT imported. */

    this.internet.putURL(url1, this.getFile("rsa-public-key-tests/server2.xml"));
    this.timePasses();
    this.assertManifestStatuses("Error", null, null);
    this.assertNoticesMatch(url1,
        "(?s).*minimum required length of server public key is 2048 bits.*");
    assertThat(this.lastCatalogue
        .xpath("/r:catalogue/r:host/r:server-credentials-in-use/r:rsa-public-key").size())
            .isEqualTo(0);

    /*
     * Make sure that invalid keys (e.g. invalid encoding) don't break anything. This manifest
     * contains two keys - one invalid and one valid. The valid one should still be imported.
     */

    this.internet.putURL(url1, this.getFile("rsa-public-key-tests/server3.xml"));
    this.timePasses();
    this.assertManifestStatuses("Error", null, null);
    this.assertNoticesMatch(url1,
        "(?s).*Invalid server public key \\(1st of 2\\).*premature EOF.*");
    assertThat(this.lastCatalogue
        .xpath("/r:catalogue/r:host/r:server-credentials-in-use/r:rsa-public-key").size())
            .isEqualTo(1);
    keyElem = $(this.lastCatalogue
        .xpath("/r:catalogue/r:host/r:server-credentials-in-use/r:rsa-public-key").get(0));
    assertThat(keyElem.attr("sha-256"))
        .isEqualTo("5531f9a02c44a894d0b706961259fec740ad4ae8a3555871f1a5cd9801285bd4");
  }

  /**
   * Run a complex scenario, involving multiple source and manifest changes. Verify if our
   * {@link RegistryUpdater} and {@link NotifierService} handle everything as expected.
   */
  @Test
  public void testScenario1() {

    /*
     * Initially, there are no manifest sources to consider. So, registry updater has nothing to do.
     */

    this.timePasses();
    assertThat(this.lastEmails).isEmpty();
    this.assertManifestStatuses(null, null, null);

    /*
     * Let's add a single manifest source, but provide no content for it. FakeInternet will throw
     * IOException, so we expect the manifest to be marked with a warning. However, since we do not
     * yet know who is the admin of this manifest, no emails are sent.
     */

    ManifestSource ms1 = ManifestSource.newRegularSource(url1, Arrays.asList());
    this.sourceProvider.addSource(ms1);
    this.timePasses();
    this.assertManifestStatuses("Warning", null, null);
    this.assertNoticesMatch(url1, "(?s).*No such URL in our FakeInternet.*");
    assertThat(this.lastEmails).isEmpty();

    /*
     * Now, let's provide some valid content, with a valid email. This manifest will get imported,
     * but it's not 100% valid. We expect an email too.
     */

    this.internet.putURL(url1, this.getMinimalManifest("admin1@example.com"));
    this.timePasses();
    this.assertManifestStatuses("Warning", null, null);
    this.assertNoticesMatch(url1, "(?s).*inconsistency in your Discovery API.*");
    assertThat(this.lastCatalogue.xpath("r:host")).hasSize(1);
    assertThat(this.lastCatalogue.xpath("r:host/ewp:admin-email").text())
        .isEqualTo("admin1@example.com");
    assertThat(this.lastEmails).hasSize(1);
    assertThat(this.lastEmails.get(0)).contains("To: admin1@example.com");
    assertThat(this.lastEmails.get(0)).contains("status is \"Warning\"");

    /*
     * Replace the semi-valid manifest with a completely invalid one. The catalogue won't change (we
     * will still serve the last valid copy), but the admin will receive another email (because the
     * severity has increased).
     */

    this.internet.putURL(url1, "invalid");
    this.timePasses();
    this.assertManifestStatuses("Error", null, null);
    this.assertNoticesMatch(url1, "(?s).*failed XML Schema validation.*will not be imported.*",
        "(?s).*Content is not allowed in prolog.*");
    assertThat(this.lastCatalogue.xpath("r:host/ewp:admin-email").text())
        .isEqualTo("admin1@example.com");
    assertThat(this.lastEmails).hasSize(1);
    assertThat(this.lastEmails.get(0)).contains("To: admin1@example.com");
    assertThat(this.lastEmails.get(0)).contains("severity status has just *increased*");
    assertThat(this.lastEmails.get(0)).contains("status is \"Error\"");

    /*
     * Let's try a valid XML syntax, but invalid schema. We still get an error (with a different
     * message), but the admin will not receive further "spammy" emails.
     */

    this.internet.putURL(url1, "<xml/>");
    this.timePasses();
    this.assertManifestStatuses("Error", null, null);
    this.assertNoticesMatch(url1, "(?s).*will not be imported.*",
        "(?s).*Cannot find the declaration of element 'xml'.*",
        "(?s).*Expecting .*manifest.* element.*", "(?s).*Expecting element from .* namespace.*");
    assertThat(this.lastCatalogue.xpath("r:host/ewp:admin-email").text())
        .isEqualTo("admin1@example.com");
    assertThat(this.lastEmails).hasSize(0);

    /* Let's add another manifest source. Two problems should be reported now. No emails sent. */

    ManifestSource ms2 = ManifestSource.newRegularSource(url2, Arrays.asList());
    this.sourceProvider.addSource(ms2);
    this.timePasses();
    this.assertManifestStatuses("Error", "Warning", null);
    this.assertNoticesMatch(url2, "(?s).*No such URL in our FakeInternet.*");
    assertThat(this.lastEmails).hasSize(0);

    /*
     * Let's replace the first URL with a completely valid manifest. You might think that the admin
     * should get a notification that everything is back in order, however - while replacing the
     * manifest - admin-email ALSO got replaced. The notifier is assuming that the new recipient is
     * not aware of any previous error, so he won't be notified that the manifest was fixed.
     */

    this.internet.putURL(url1,
        this.getFile("latest-examples/ewp-specs-api-discovery-manifest-example.xml"));
    this.timePasses();
    this.assertManifestStatuses("OK", "Warning", null);
    assertThat(this.lastCatalogue.xpath("r:host/ewp:admin-email").text())
        .isEqualTo("admin-or-developer@example.com"); // new email!
    assertThat(this.lastCatalogue.xpath("r:host/r:apis-implemented/*")).hasSize(2);
    assertThat(this.lastCatalogue.xpath("r:host/r:apis-implemented/d4:discovery")).hasSize(1);
    assertThat(this.lastCatalogue.xpath("r:host/r:apis-implemented/e2:echo")).hasSize(1);
    assertThat(this.lastEmails).hasSize(0);

    /*
     * Let's tweak his manifest a bit an try to host a Registry API. (Only the Registry host is
     * allowed to host this API.)
     */

    Match manifest = this.parseURL(url1);
    manifest.xpath("r:apis-implemented")
        .append("<registry xmlns='" + KnownNamespace.APIENTRY_REGISTRY.getNamespaceUri()
            + "' version='1.0.0'><catalogue-url>https://example.com/catalogue-v1.xml"
            + "</catalogue-url></registry>");
    assertThat(manifest.xpath("r:apis-implemented/*")).hasSize(3);
    assertThat(manifest.xpath("r:apis-implemented/r1:registry")).hasSize(1);
    this.internet.putURL(url1, this.xmlFormatter.format(manifest.document()));
    this.timePasses();
    this.assertManifestStatuses("Warning", "Warning", null);
    assertThat(this.lastCatalogue.xpath("r:host/r:apis-implemented/*")).hasSize(2);
    assertThat(this.lastCatalogue.xpath("r:host/r:apis-implemented/r1:registry")).hasSize(0);
    this.assertNoticesMatch(url1, "(?s).*Registry API entries will not be imported.*");
    assertThat(this.lastEmails).hasSize(1);
    assertThat(this.lastEmails.get(0)).contains("To: admin-or-developer@example.com");
    assertThat(this.lastEmails.get(0)).contains("status is \"Warning\"");

    /*
     * Now, let's say that we want our admin to be responsible for two manifests. The second
     * manifest will contain a warning-level issue, but since he has already been warned about the
     * first manifest, there's no reason to spam him again about the second one.
     */

    this.internet.putURL(url3, this.getMinimalManifest("admin-or-developer@example.com"));
    ManifestSource ms3 = ManifestSource.newRegularSource(url3, Arrays.asList());
    this.sourceProvider.addSource(ms3);
    this.timePasses();
    this.assertManifestStatuses("Warning", "Warning", "Warning");
    this.assertNoticesMatch(url3, "(?s).*inconsistency in your Discovery API.*");
    assertThat(this.lastEmails).hasSize(0);

    /*
     * Let's fix the first manifest. The admin still won't be notified, because the third manifest
     * still has the "Warning" status.
     */

    this.internet.putURL(url1,
        this.getFile("latest-examples/ewp-specs-api-discovery-manifest-example.xml"));
    this.timePasses();
    this.assertManifestStatuses("OK", "Warning", "Warning");
    assertThat(this.lastEmails).hasSize(0);

    /*
     * However, if we remove the last manifest from manifest sources, he will get an email.
     */

    this.sourceProvider.removeSource(ms3);
    this.timePasses();
    this.assertManifestStatuses("OK", "Warning", null);
    assertThat(this.lastEmails).hasSize(1);
    assertThat(this.lastEmails.get(0)).contains("To: admin-or-developer@example.com");
    assertThat(this.lastEmails.get(0)).contains("All problems seem to be resolved now!");
  }

  /**
   * Read manifest statuses for {@link #url1}, {@link #url2} and {@link #url3} and check if they are
   * equal to the ones passed.
   */
  private void assertManifestStatuses(String forUrl1, String forUrl2, String forUrl3) {
    List<Pair<String, String>> pairs = new ArrayList<>();
    pairs.add(new Pair<>(url1, forUrl1));
    pairs.add(new Pair<>(url2, forUrl2));
    pairs.add(new Pair<>(url3, forUrl3));

    for (Pair<String, String> pair : pairs) {
      String url = pair.getValue0();
      ManifestUpdateStatus status = this.updateStatuses.findOne(url);
      String expectedSeverityString = pair.getValue1();
      if (expectedSeverityString == null) {
        assertThat(status).as("status of %s", url).isNull();
      } else {
        assertThat(status).as("status of %s", url).isNotNull();
        assertThat(status.getLastAccessFlagStatus().toString()).as("status of %s", url)
            .isEqualTo(expectedSeverityString);
      }
    }
  }

  /**
   * Read all the notices reported for the URL and check if they meet the given regexps.
   */
  private void assertNoticesMatch(String url, String... regexps) {
    ManifestUpdateStatus status = this.updateStatuses.findOne(url);
    List<UpdateNotice> notices = status.getLastAccessNotices();
    assertThat(notices).hasSameSizeAs(regexps);
    for (int i = 0; i < regexps.length; i++) {
      UpdateNotice notice = notices.get(i);
      assertThat(notice.getMessageHtml()).as("notice #%s", i).matches(regexps[i]);
    }
  }

  /**
   * Generate a minimal manifest, with a single admin-email entry and nothing more.
   */
  private String getMinimalManifest(String email) {
    StringBuilder sb = new StringBuilder();
    sb.append("<manifest xmlns='");
    sb.append("https://github.com/erasmus-without-paper/ewp-specs-api-discovery/tree/stable-v4");
    sb.append("'><admin-email xmlns='");
    sb.append(
        "https://github.com/erasmus-without-paper/ewp-specs-architecture/blob/stable-v1/common-types.xsd");
    sb.append("'>");
    sb.append(Utils.escapeXml(email));
    sb.append("</admin-email></manifest>");
    return sb.toString();
  }

  private Match parseURL(String url) {
    byte[] contents;
    try {
      contents = this.internet.getUrl(url);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
    return $(this.docBuilder.build(new BuildParams(contents)).getDocument().get())
        .namespaces(KnownNamespace.prefixMap());
  }

  /**
   * Simulate the passage of time. All manifests are reloaded, ale notifications are sent. Also fill
   * the {@link #lastCatalogue} and {@link #lastEmails} fields with proper values.
   */
  private void timePasses() {
    this.internet.clearEmailsSent();
    this.updater.reloadAllManifestSources();
    this.notifier.sendNotifications();
    try {
      this.lastCatalogue =
          $(this.builder.build(new BuildParams(this.repo.getCatalogue())).getDocument().get())
              .namespaces(KnownNamespace.prefixMap());
    } catch (CatalogueNotFound e) {
      this.lastCatalogue = null;
    }
    this.lastEmails = this.internet.popEmailsSent();
  }
}
