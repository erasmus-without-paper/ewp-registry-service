package eu.erasmuswithoutpaper.registry.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

import eu.erasmuswithoutpaper.registry.WRTest;

import org.springframework.beans.factory.annotation.Autowired;

import org.junit.After;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Tests for {@link ManifestRepositoryImpl}.
 */
public class ManifestRepositoryTest extends WRTest {

  private static String manifestUrl1;
  private static String manifestUrl2;

  @BeforeClass
  public static void setUpClass() {
    manifestUrl1 = "https://example.com/manifest1.xml";
    manifestUrl2 = "https://example.com/manifest2.xml";
  }

  @Autowired
  private ManifestRepositoryImpl repo;

  @Autowired
  private CatalogueDependantCache catcache;

  @After
  public void tearDown() {
    this.repo.deleteAll();
  }

  @Test
  public void testCatalogueDependantCache() {
    this.repo.deleteAll();

    this.repo.putCatalogue("1");
    this.catcache.putCoverageMatrixHtml("1");
    assertThat(this.catcache.getCoverageMatrixHtml()).isEqualTo("1");

    this.repo.putCatalogue("1");
    assertThat(this.catcache.getCoverageMatrixHtml()).isEqualTo("1");

    this.repo.putCatalogue("2");
    assertThat(this.catcache.getCoverageMatrixHtml()).isNull();
    this.catcache.putCoverageMatrixHtml("2");
    assertThat(this.catcache.getCoverageMatrixHtml()).isEqualTo("2");

    this.repo.putCatalogue("1");
    assertThat(this.catcache.getCoverageMatrixHtml()).isNull();
  }

  /**
   * Similar to {@link #testManifestStorage()}, but for catalogue this time.
   */
  @Test
  public void testCatalogueStorage() {

    this.repo.deleteAll();

    // No catalogue should exist before this test is run.

    String expectedCatalogueFileName = "catalogue-v1.xml";
    assertThat(this.repo.getAllFilePaths()).doesNotContain(expectedCatalogueFileName);

    // Attempt to get a nonexistent catalogue-v1.xml from the repo.

    String catalogueXml;
    try {
      catalogueXml = this.repo.getCatalogue();
      fail("Exception was expected");
    } catch (CatalogueNotFound e1) {
      // expected
    }

    // Put a catalogue-v1.xml in the repo. As with the manifests, repository
    // should accept any string here.

    this.repo.commit("commit things which were changed before");
    this.repo.putCatalogue("a string with the catalogue");
    boolean r = this.repo.commit("commit the catalogue");
    assertThat(r).isTrue();
    this.repo.putCatalogue("a string with the catalogue");
    r = this.repo.commit("try to commit unchanged catalogue");
    assertThat(r).isFalse();

    // Retrieve the previously set catalogue and validate its contents.

    try {
      catalogueXml = this.repo.getCatalogue();
    } catch (CatalogueNotFound e) {
      throw new RuntimeException(e);
    }
    assertThat(catalogueXml).isEqualTo("a string with the catalogue");
    assertThat(this.repo.getAllFilePaths()).contains(expectedCatalogueFileName);
  }

  /**
   * Test if {@link ManifestRepositoryImpl#deleteAll()} deletes all files, even if the "pairs" are
   * inconsistent.
   */
  public void testDeleteAll() {
    this.repo.deleteAll();
    assertThat(this.repo.getAllFilePaths()).containsExactly("index.xml");
    this.repo.putOriginalManifest(manifestUrl1, "some string".getBytes());
    this.repo.putFilteredManifest(manifestUrl2, "some string");
    assertThat(this.repo.getAllFilePaths()).containsExactlyInAnyOrder("index.xml",
        "manifests/com/example.com/51d9ca82ce863381a7648647ab688b966f3b2260-filtered.xml",
        "manifests/com/example.com/bb937788ce84767ff64935e70c3856bd8c7bd16d.xml");
    this.repo.deleteAll();
    assertThat(this.repo.getAllFilePaths()).containsExactly("index.xml");
  }

  /**
   * Test manifest storage and retrieval methods. Verify if the contents are saved in the file
   * system and they are properly named.
   */
  @Test
  public void testManifestStorage() {

    this.repo.deleteAll();

    // Attempt to get a nonexistent manifest from the repo.

    try {
      this.repo.getManifestFiltered(manifestUrl1);
      fail("getManifest SHOULD throw an exception here");
    } catch (ManifestNotFound e) {
      // expected
    }

    assertThat(this.repo.getAllFilteredManifestUrls()).isEmpty();

    // Put a manifest in the repo. Note, that the repository component should
    // accept any string as a valid manifest. It's not the repository's duty to
    // verify the manifests.

    this.repo.putOriginalManifest(manifestUrl1, "some string".getBytes());
    this.repo.putFilteredManifest(manifestUrl1, "some filtered string");
    assertThat(this.repo.getAllFilteredManifestUrls()).containsExactly(manifestUrl1);

    // Retrieve the previously put manifest and validate its contents.

    byte[] ro;
    String rf;
    try {
      ro = this.repo.getManifestOriginal(manifestUrl1);
      rf = this.repo.getManifestFiltered(manifestUrl1);
    } catch (ManifestNotFound e) {
      throw new RuntimeException(e);
    }
    assertThat(ro).isEqualTo("some string".getBytes());
    assertThat(rf).isEqualTo("some filtered string");

    // Verify that the file exists in the fileSystem.

    assertThat(this.repo.getAllFilePaths()).containsExactlyInAnyOrder("index.xml",
        "manifests/com/example.com/bb937788ce84767ff64935e70c3856bd8c7bd16d-filtered.xml",
        "manifests/com/example.com/bb937788ce84767ff64935e70c3856bd8c7bd16d.xml");

    // Call setManifest again, with exactly the same content. Repository should
    // ignore such calls silently.

    this.repo.putOriginalManifest(manifestUrl1, "some string".getBytes());
    this.repo.putFilteredManifest(manifestUrl1, "some filtered string");

    // Delete the manifest from the repo.

    assertThat(this.repo.getAllFilteredManifestUrls()).containsExactly(manifestUrl1);
    assertThat(this.repo.deleteManifest(manifestUrl1)).isTrue();
    assertThat(this.repo.getAllFilteredManifestUrls()).isEmpty();

    // Attempt to retrieve the deleted manifest.

    try {
      this.repo.getManifestFiltered(manifestUrl1);
      fail("getManifest SHOULD throw an exception here");
    } catch (ManifestNotFound e) {
    }

    // Make sure the file is no longer present.

    assertThat(this.repo.getAllFilePaths()).containsExactlyInAnyOrder("index.xml");
  }

  /**
   * Run a couple of tests on the {@link ManifestRepositoryImpl#getManifestPathPrefix(String)}
   * method to determine if the names are as we expect them to be.
   */
  @Test
  public void testPaths() {
    this.testPath("https://example.com/manifest.xml",
        "manifests/com/example.com/ce6fe21a5362373066a48db353c61cee9ff25fda");
    this.testPath("http://my.dev.host/file",
        "manifests/host/my.dev.host/700f15f06866cee166107f617d4e197e8778b394");
    this.testPath("http://bźdźiągwa.pl/manifest",
        "manifests/pl/b%C5%BAd%C5%BAi%C4%85gwa.pl/d1a3a550edb437de22b59feac93f04a32e0729da");
  }

  private void testPath(String urlstring, String expectedPathPrefix) {
    assertThat(this.repo.getManifestPathPrefix(urlstring)).isEqualTo(expectedPathPrefix);
  }
}
