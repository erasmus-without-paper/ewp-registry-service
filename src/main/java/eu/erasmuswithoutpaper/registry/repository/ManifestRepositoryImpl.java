package eu.erasmuswithoutpaper.registry.repository;

import static org.joox.JOOX.$;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import eu.erasmuswithoutpaper.registry.common.Utils;
import eu.erasmuswithoutpaper.registryclient.RegistryClient;
import eu.erasmuswithoutpaper.registryclient.RegistryClient.RefreshFailureException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.apache.commons.codec.digest.DigestUtils;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.RmCommand;
import org.eclipse.jgit.api.Status;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.api.errors.TransportException;
import org.eclipse.jgit.errors.RevisionSyntaxException;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.PersonIdent;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;
import org.joox.Match;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.SAXException;

/**
 * Implementation of {@link ManifestRepository}.
 *
 * <p>
 * It keeps the files in the file system, uses JGit to keeping track of changes, and pushes the
 * changes to git's default "origin" remote. The underlying Git working copy needs to be initiated
 * beforehand.
 * </p>
 */
@Service
@SuppressFBWarnings("SIC_INNER_SHOULD_BE_STATIC_ANON")
public class ManifestRepositoryImpl implements ManifestRepository {

  private static final Logger logger = LoggerFactory.getLogger(ManifestRepositoryImpl.class);

  private final ManifestRepositoryImplProperties repoProperties;
  private final CatalogueDependantCache catcache;
  private RegistryClient client = null;
  private final Git git;

  private final ReentrantReadWriteLock lock;
  private final SortedSet<String> index;

  private volatile String cachedCatalogueContent = null;

  /**
   * @param repoProperties Repository properties to use. These cannot be changed after the object is
   *        instantiated.
   * @param catcache needed because it needs to be notified (cleared) whenever the catalogue
   *        changes.
   */
  @Autowired
  @SuppressFBWarnings("BC_UNCONFIRMED_CAST_OF_RETURN_VALUE")
  public ManifestRepositoryImpl(ManifestRepositoryImplProperties repoProperties,
      CatalogueDependantCache catcache) {
    this.repoProperties = repoProperties;
    this.catcache = catcache;

    FileRepositoryBuilder builder = new FileRepositoryBuilder();
    try {
      this.git = Git.wrap(builder.setMustExist(true)
          .setWorkTree(new File(this.repoProperties.getPath())).readEnvironment().build());
    } catch (IOException e) {
      throw new RuntimeException(e);
    }

    this.lock = new ReentrantReadWriteLock(true);
    Optional<SortedSet<String>> loadedIndex = this.loadIndex();
    if (loadedIndex.isPresent()) {
      this.index = this.loadIndex().get();
    } else {
      this.index = new TreeSet<>();
      this.deleteAll();
      this.flushIndex();
      this.commit("Upgrade repository structure");
    }
  }

  @Override
  public void acquireWriteLock() {
    this.lock.writeLock().lock();
  }

  @Override
  public boolean commit(String message) {
    this.lock.writeLock().lock();
    try {
      this.git.add().addFilepattern(".").call();
      Status status = this.git.status().call();
      if (!status.getMissing().isEmpty() || !status.getRemoved().isEmpty()) {
        RmCommand rm = this.git.rm();
        for (String deletedFile : Iterables.concat(status.getMissing(), status.getRemoved())) {
          rm.addFilepattern(deletedFile);
        }
        rm.call();
      }
      status = this.git.status().call();
      if (status.hasUncommittedChanges()) {
        PersonIdent committer = new PersonIdent(this.repoProperties.getCommitterName(),
            this.repoProperties.getCommitterEmail());
        this.git.commit().setMessage(message).setAuthor(committer).setCommitter(committer).call();
        logger.info("New commit saved: " + message);
        return true;
      } else {
        return false;
      }
    } catch (GitAPIException e) {
      throw new RuntimeException(e);
    } finally {
      this.lock.writeLock().unlock();
    }
  }

  /**
   * Delete all contents from the current working copy. Useful during unit tests.
   *
   * <p>
   * Remember to {@link #commit(String)} the changes afterwards, for logging purposes.
   * </p>
   */
  public void deleteAll() {
    this.lock.writeLock().lock();
    try {
      Path path = this.repoProperties.getFileSystem().getPath(this.repoProperties.getPath());
      Files.walkFileTree(path, new FileVisitor<Path>() {

        @Override
        public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
          if (exc == null) {
            // Remove everything except the root directory (with ".git" subdir under).
            if (!dir.equals(path)) {
              Files.delete(dir);
            }
            return FileVisitResult.CONTINUE;
          } else {
            throw exc;
          }
        }

        @Override
        @SuppressFBWarnings("NP_NULL_ON_SOME_PATH_FROM_RETURN_VALUE")
        public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs)
            throws IOException {
          if (dir.getFileName().toString().startsWith(".git")) {
            return FileVisitResult.SKIP_SUBTREE;
          } else {
            return FileVisitResult.CONTINUE;
          }
        }

        @Override
        public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
          Files.delete(file);
          return FileVisitResult.CONTINUE;
        }

        @Override
        public FileVisitResult visitFileFailed(Path file, IOException exc) throws IOException {
          throw exc;
        }
      });
      this.cachedCatalogueContent = null;
      this.index.clear();
      this.flushIndex();
      this.onCatalogueContentChanged();
    } catch (IOException e) {
      throw new RuntimeException(e);
    } finally {
      this.lock.writeLock().unlock();
    }
  }

  @Override
  public boolean deleteManifest(String urlString) {
    this.lock.writeLock().lock();
    try {

      Path path1 = this.getPathForOriginalManifestUrl(urlString);
      Path path2 = this.getPathForFilteredManifestUrl(urlString);
      boolean result = false;
      try {
        if (Files.exists(path1)) {
          Files.delete(path1);
          result = true;
        }
        if (Files.exists(path2)) {
          Files.delete(path2);
          result = true;
        }
      } catch (IOException e) {
        throw new RuntimeException(e);
      }

      if (result == true) {
        this.removeFromIndex(urlString);
      }
      return result;
    } finally {
      this.lock.writeLock().unlock();
    }
  }

  @Override
  public void destroy() {
    this.git.close();
  }

  /**
   * Peek at the current contents of the repository's working copy.
   *
   * @return List of relative paths to all files kept in the working copy (directories are not
   *         included in the results).
   */
  public List<String> getAllFilePaths() {
    this.lock.readLock().lock();
    try {
      List<String> out = Lists.newArrayList();
      Path root = this.repoProperties.getFileSystem().getPath(this.repoProperties.getPath());

      try {
        Files.walkFileTree(root, new SimpleFileVisitor<Path>() {

          @Override
          @SuppressFBWarnings("NP_NULL_ON_SOME_PATH_FROM_RETURN_VALUE")
          public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs)
              throws IOException {
            if (dir.getFileName().toString().startsWith(".")) {
              return FileVisitResult.SKIP_SUBTREE;
            }
            return FileVisitResult.CONTINUE;
          }

          @Override
          public FileVisitResult visitFile(Path file, BasicFileAttributes attrs)
              throws IOException {
            out.add(root.relativize(file).toString().replace("\\", "/"));
            return FileVisitResult.CONTINUE;
          }
        });
      } catch (IOException e) {
        throw new RuntimeException(e);
      }

      return out;
    } finally {
      this.lock.readLock().unlock();
    }
  }

  @Override
  public List<String> getAllFilteredManifestUrls() {
    return Lists.newArrayList(this.index);
  }

  @Override
  public String getCatalogue() throws CatalogueNotFound {
    this.lock.readLock().lock();
    try {

      // Do we have a cached copy?
      if (this.cachedCatalogueContent == null) {

        // Does it exist in our repo?
        Path path = this.getPathForCatalogue();
        if (!Files.exists(path)) {
          throw new CatalogueNotFound();
        }

        // Read it.
        byte[] encoded;
        try {
          encoded = Files.readAllBytes(path);
        } catch (IOException e) {
          throw new RuntimeException(e);
        }

        // Convert it to String.
        this.cachedCatalogueContent = new String(encoded, StandardCharsets.UTF_8);
      }

      return this.cachedCatalogueContent;

    } finally {
      this.lock.readLock().unlock();
    }
  }

  @Override
  public String getManifestFiltered(String urlString) throws ManifestNotFound {
    this.lock.readLock().lock();
    try {

      // Does it exist in our repo?
      Path path = this.getPathForFilteredManifestUrl(urlString);
      if (!Files.exists(path)) {
        throw new ManifestNotFound();
      }

      // Read it.
      byte[] encoded;
      try {
        encoded = Files.readAllBytes(path);
      } catch (IOException e) {
        throw new RuntimeException(e);
      }

      // Convert it to String.
      return new String(encoded, StandardCharsets.UTF_8);

    } finally {
      this.lock.readLock().unlock();
    }
  }

  @Override
  public byte[] getManifestOriginal(String urlString) throws ManifestNotFound {
    this.lock.readLock().lock();
    try {

      // Does it exist in our repo?
      Path path = this.getPathForOriginalManifestUrl(urlString);
      if (!Files.exists(path)) {
        throw new ManifestNotFound();
      }

      try {
        return Files.readAllBytes(path);
      } catch (IOException e) {
        throw new RuntimeException(e);
      }

    } finally {
      this.lock.readLock().unlock();
    }
  }

  @Override
  public boolean push() throws TransportException, GitAPIException, ConfigurationException {
    this.lock.writeLock().lock();
    try {
      if (this.repoProperties.isPushingEnabled()) {
        if (!this.unpushedCommitsExist()) {
          return false;
        }
        try {
          this.git.push().call();
          logger.info("Successfully pushed to origin");
          return true;
        } catch (TransportException e) {
          throw e;
        } catch (GitAPIException e) {
          throw new RuntimeException(e);
        }
      } else {
        return false;
      }
    } finally {
      this.lock.writeLock().unlock();
    }
  }

  @Override
  public boolean putCatalogue(String contents) {
    this.lock.writeLock().lock();
    try {
      boolean changed = this.writeFile(this.getPathForCatalogue(), contents);
      this.cachedCatalogueContent = contents;
      if (changed) {
        this.onCatalogueContentChanged();
      }
      return changed;
    } finally {
      this.lock.writeLock().unlock();
    }
  }

  @Override
  public boolean putFilteredManifest(String urlString, String filteredContents) {
    this.lock.writeLock().lock();
    try {
      boolean changed =
          this.writeFile(this.getPathForFilteredManifestUrl(urlString), filteredContents);
      this.addToIndex(urlString);
      return changed;
    } finally {
      this.lock.writeLock().unlock();
    }
  }

  @Override
  public boolean putOriginalManifest(String urlString, byte[] originalContents) {
    this.lock.writeLock().lock();
    try {
      boolean changed =
          this.writeFile(this.getPathForOriginalManifestUrl(urlString), originalContents);
      this.addToIndex(urlString);
      return changed;
    } finally {
      this.lock.writeLock().unlock();
    }
  }

  @Override
  public void releaseWriteLock() {
    this.lock.writeLock().unlock();
  }

  /**
   * Once set, {@link ManifestRepositoryImpl} will refresh this {@link RegistryClient} whenever the
   * catalogue is changed.
   *
   * @param client The client to be kept in sync.
   */
  @Autowired
  public void setRegistryClient(RegistryClient client) {
    this.client = client;
    this.onCatalogueContentChanged();
  }

  private void addToIndex(String url) {
    this.lock.writeLock().lock();
    try {
      this.index.add(url);
      this.flushIndex();
    } finally {
      this.lock.writeLock().unlock();
    }
  }

  private void flushIndex() {
    StringBuilder sb = new StringBuilder();
    sb.append("<index version=\"1\">\n");
    for (String url : this.index) {
      sb.append("    <manifest-source>\n");
      sb.append("        <url>");
      sb.append(Utils.escapeXml(url));
      sb.append("</url>\n");
      sb.append("        <local-path-prefix>");
      sb.append(Utils.escapeXml(this.getManifestPathPrefix(url)));
      sb.append("</local-path-prefix>\n");
      sb.append("    </manifest-source>\n");
    }
    sb.append("</index>\n");
    this.writeFile(this.getPathForIndex(), sb.toString());
  }

  private Path getPathForCatalogue() {
    return this.repoProperties.getFileSystem()
        .getPath(this.repoProperties.getPath(), "catalogue-v1.xml").toAbsolutePath();
  }

  private Path getPathForFilteredManifestUrl(String urlString) {
    return this.repoProperties.getFileSystem().getPath(this.repoProperties.getPath(),
        this.getManifestPathPrefix(urlString) + "-filtered.xml").toAbsolutePath();
  }

  private Path getPathForIndex() {
    return this.repoProperties.getFileSystem().getPath(this.repoProperties.getPath(), "index.xml")
        .toAbsolutePath();
  }

  private Path getPathForOriginalManifestUrl(String urlString) {
    return this.repoProperties.getFileSystem()
        .getPath(this.repoProperties.getPath(), this.getManifestPathPrefix(urlString) + ".xml")
        .toAbsolutePath();
  }

  private Optional<SortedSet<String>> loadIndex() {

    SortedSet<String> result = new TreeSet<>();

    byte[] encoded;
    try {
      encoded = Files.readAllBytes(this.getPathForIndex());
    } catch (IOException e) {
      /* Index missing. This means that we're upgrading from older format. */
      return Optional.empty();
    }

    Match doc;
    try {
      doc = $(new ByteArrayInputStream(encoded));
    } catch (SAXException | IOException e) {
      throw new RuntimeException(e);
    }
    result.addAll(doc.find("url").texts());

    return Optional.of(result);
  }

  private void onCatalogueContentChanged() {
    this.catcache.clear();
    if (this.client != null) {
      try {
        this.client.refresh();
      } catch (RefreshFailureException e) {
        logger.error("Local registry client refresh failed: " + e);
      }
    }
  }

  private void removeFromIndex(String url) {
    this.lock.writeLock().lock();
    try {
      this.index.remove(url);
      this.flushIndex();
    } finally {
      this.lock.writeLock().unlock();
    }
  }

  private boolean unpushedCommitsExist() throws GitAPIException, ConfigurationException {
    this.lock.writeLock().lock();
    try {
      ObjectId head = this.git.getRepository().resolve("master");
      ObjectId origin = this.git.getRepository().resolve("origin/master");
      if (head == null || origin == null) {
        throw new ConfigurationException(
            "You need to have 'master' and 'origin/master' branches in your repo.");
      }
      Iterable<RevCommit> commits = this.git.log().addRange(origin, head).call();
      return Iterables.size(commits) > 0;
    } catch (RevisionSyntaxException | IOException e) {
      throw new RuntimeException(e);
    } finally {
      this.lock.writeLock().unlock();
    }
  }

  @SuppressFBWarnings("NP_NULL_ON_SOME_PATH_FROM_RETURN_VALUE")
  private boolean writeFile(Path path, byte[] contents) {
    this.lock.writeLock().lock();
    try {

      byte[] previousContents;
      if (!Files.exists(path)) {
        previousContents = null;
      } else {
        try {
          previousContents = Files.readAllBytes(path);
        } catch (IOException e) {
          throw new RuntimeException(e);
        }
      }

      if (previousContents != null && Arrays.equals(previousContents, contents)) {
        return false;
      } else {
        if (path.getParent() != null) {
          Files.createDirectories(path.getParent());
        }
        Files.write(path, contents);
        return true;
      }
    } catch (IOException e) {
      throw new RuntimeException(e);
    } finally {
      this.lock.writeLock().unlock();
    }
  }

  private boolean writeFile(Path path, String contents) {
    return this.writeFile(path, contents.getBytes(StandardCharsets.UTF_8));
  }

  String getManifestPathPrefix(String urlString) {
    ArrayList<String> pathParts = new ArrayList<>();
    pathParts.add("manifests");

    try {
      URL url = new URL(urlString);
      String host = url.getHost();
      String tld = Iterables.getLast(Lists.newArrayList(host.split("\\.")));
      pathParts.add(Utils.urlencode(tld));
      pathParts.add(Utils.urlencode(host));
    } catch (MalformedURLException e) {
      pathParts.add("from-malformed-urls");
    }

    pathParts.add(DigestUtils.sha1Hex(urlString));
    return String.join("/", pathParts);
  }
}
