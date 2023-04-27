package eu.erasmuswithoutpaper.registry.repository;

import java.util.List;

import org.springframework.beans.factory.DisposableBean;

import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.api.errors.TransportException;

/**
 * Classes implementing this interface allow persistent storage of manifest and catalogue revisions.
 * States of the repository can be committed and pushed to some remote server (for logging
 * purposes).
 *
 * <p>
 * {@link ManifestRepository} keeps two copies of each manifest:
 * </p>
 *
 * <ul>
 * <li>The original one - exactly as it has been last fetched from the remote server.</li>
 * <li>The filtered one - this one is formatted, validated and cleaned up. This version gets
 * imported into the catalogue).</li>
 * </ul>
 *
 * <p>
 * Note, that {@link ManifestRepository} stores the history of all previously committed contents
 * too, but you can retrieve only the latest contents via this interface. All getters will return
 * the current contents kept in the working copy, even if these contents were not yet committed via
 * {@link #commit(String)}. If you want to prevent this, remember to wrap all of your
 * write-transactions with {@link #acquireWriteLock()}.
 * </p>
 */
public interface ManifestRepository extends DisposableBean {

  /** Thrown when repository is misconfigured. */
  @SuppressWarnings("serial")
  class ConfigurationException extends Exception {
    ConfigurationException(String message) {
      super(message);
    }
  }

  /**
   * Acquire a read lock for the current thread.
   *
   * <p>
   * This guarantees that no other thread will read the repository until the lock is
   * released. You MUST release the lock after you make your changes (use try..finally).
   * </p>
   */
  void acquireReadLock();

  /**
   * Acquire a write lock for the current thread.
   *
   * <p>
   * This guarantees that no other thread will read or write to the repository until the lock is
   * released. You MUST release the lock after you make your changes (use try..finally).
   * </p>
   */
  void acquireWriteLock();

  /**
   * Take all the changes made since the previous revision, and commit them with the proper message.
   *
   * <p>
   * You SHOULD wrap your transaction in {@link #acquireWriteLock()} and {@link #releaseWriteLock()}
   * in order to make sure that no other threads will modify the working copy in parallel.
   * </p>
   *
   * @param message The log message to be stored with this commit.
   * @return <b>true</b> if something was indeed committed (changed), <b>false</b> if no changes
   *         since the previous revision were detected in the working copy.
   */
  boolean commit(String message);

  /**
   * Remove a manifest from the repository (both the original and the filtered versions).
   *
   * <p>
   * Remember to {@link #commit(String)} the changes afterwards, for logging purposes.
   * </p>
   *
   * @param urlString URL of the manifest to be deleted.
   * @return <b>true</b> if the manifest has been deleted, <b>false</b> if it didn't exist in the
   *         first place.
   */
  boolean deleteManifest(String urlString);

  /**
   * Retrieve the list of all manifest URLs which are currently stored in the repository. The URLs
   * retrieved should equal the ones previously used in {@link #putOriginalManifest(String, byte[])}
   * or {@link #putFilteredManifest(String, String)} calls.
   *
   * @return A List of URL strings.
   */
  List<String> getAllFilteredManifestUrls();

  /**
   * Retrieve the current catalogue contents from repository's working copy.
   *
   * @return A string with the catalogue contents.
   * @throws CatalogueNotFound when no catalogue has been put in the repository yet.
   */
  String getCatalogue() throws CatalogueNotFound;

  /**
   * Retrieve the <b>filtered</b> manifest contents from repository's working copy.
   *
   * <p>
   * See {@link ManifestRepository} for the difference between filtered and original manifests.
   * </p>
   *
   * @param urlString Unique URL of the manifest.
   * @return A string with the manifest contents, as stored in the repository.
   * @throws ManifestNotFound when no manifest for the given URL was found in the repository.
   */
  String getManifestFiltered(String urlString) throws ManifestNotFound;

  /**
   * Retrieve the <b>original</b> manifest contents from repository's working copy.
   *
   * <p>
   * See {@link ManifestRepository} for the difference between filtered and original manifests.
   * </p>
   *
   * @param urlString Unique URL of the manifest.
   * @return A string with the manifest contents, as stored in the repository.
   * @throws ManifestNotFound when no manifest for the given URL was found in the repository.
   */
  byte[] getManifestOriginal(String urlString) throws ManifestNotFound;

  /**
   * Push all committed changes to the remote repository, for logging purposes.
   *
   * @return <b>true</b> if something was actually pushed, <b>false</b> if there was nothing to
   *         push, or pushing has been disabled.
   * @throws TransportException When network error occurs during the process.
   * @throws GitAPIException When changes cannot be pushed for some other reason (serious).
   * @throws ConfigurationException When pushing is enabled, but not configured properly.
   */
  boolean push() throws TransportException, GitAPIException, ConfigurationException;

  /**
   * Store the new version of the catalogue.
   *
   * <p>
   * Remember to {@link #commit(String)} the changes afterwards, for logging purposes.
   * </p>
   *
   * @param contents A string with the catalogue contents to be stored.
   * @return <b>true</b> if the new content differs from the previous one.
   */
  boolean putCatalogue(String contents);

  /**
   * Store a new filtered version of the manifest.
   *
   * <p>
   * See {@link ManifestRepository} for the difference between filtered and original manifests.
   * Remember to {@link #commit(String)} the changes afterwards, for logging purposes.
   * </p>
   *
   * @param urlString Unique URL of the manifest.
   * @param filteredContents A string with the filtered contents of the manifest.
   * @return <b>true</b> if the new content differs from the previous one.
   */
  boolean putFilteredManifest(String urlString, String filteredContents);

  /**
   * Store a new original version of the manifest.
   *
   * <p>
   * See {@link ManifestRepository} for the difference between filtered and original manifests.
   * Remember to {@link #commit(String)} the changes afterwards, for logging purposes.
   * </p>
   *
   * @param urlString Unique URL of the manifest.
   * @param originalContents A string with the original contents of the manifest.
   * @return <b>true</b> if the new content differs from the previous one.
   */
  boolean putOriginalManifest(String urlString, byte[] originalContents);


  /**
   * Release the lock previously acquired with {@link #acquireReadLock()}.
   */
  void releaseReadLock();

  /**
   * Release the lock previously acquired with {@link #acquireWriteLock()}.
   */
  void releaseWriteLock();
}
