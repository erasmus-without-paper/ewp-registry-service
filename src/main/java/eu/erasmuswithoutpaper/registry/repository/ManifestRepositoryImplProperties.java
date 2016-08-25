package eu.erasmuswithoutpaper.registry.repository;

import java.nio.file.FileSystem;

/**
 * A set of properties required for {@link ManifestRepositoryImpl} to be instantiated.
 */
public class ManifestRepositoryImplProperties {

  private final FileSystem fileSystem;
  private final String path;
  private final String committerName;
  private final String committerEmail;
  private final boolean enablePushing;

  /**
   * @param fileSystem value for {@link #getFileSystem()}.
   * @param path value for {@link #getPath()}.
   * @param committerName value for {@link #getCommitterName()}.
   * @param committerEmail value for {@link #getCommitterEmail()}.
   * @param enablePushing value for {@link #getEnablePushing()}.
   */
  public ManifestRepositoryImplProperties(FileSystem fileSystem, String path, String committerName,
      String committerEmail, boolean enablePushing) {
    this.fileSystem = fileSystem;
    this.path = path;
    this.committerName = committerName;
    this.committerEmail = committerEmail;
    this.enablePushing = enablePushing;
  }

  /**
   * @return Email address to be used in Git commits.
   */
  public String getCommitterEmail() {
    return this.committerEmail;
  }

  /**
   * @return Name to be used in Git commits.
   */
  public String getCommitterName() {
    return this.committerName;
  }

  /**
   * @return <b>true</b> to enable pushing. If <b>false</b> is given, then
   *         {@link ManifestRepository#push()} will do nothing.
   */
  public boolean getEnablePushing() {
    return this.enablePushing;
  }

  /**
   * @return {@link FileSystem} implementation to be used for storage. Due to the limits of the
   *         underlying JGit library this needs to be a real file system.
   */
  public FileSystem getFileSystem() {
    return this.fileSystem;
  }

  /**
   * @return Path to the Git working copy to use. It needs to be on the same file system as the one
   *         returned by {@link #getFileSystem()}, and it needs to be created beforehand.
   */
  public String getPath() {
    return this.path;
  }
}
