package eu.erasmuswithoutpaper.registry.configuration;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;

import eu.erasmuswithoutpaper.registry.repository.ManifestRepositoryImplProperties;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.task.SyncTaskExecutor;
import org.springframework.core.task.TaskExecutor;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;

/**
 * As opposed to {@link ProductionConfiguration}, this class provides Spring beans to be autowired
 * during tests.
 */
@Profile("test")
@Configuration
public class TestConfiguration {

  private final FileSystem fs;
  private final String repoPath;

  TestConfiguration() {
    this.fs = FileSystems.getDefault();
    Path dir;
    try {
      dir = Files.createTempDirectory("ewp-registry-tests");
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
    this.repoPath = dir.toAbsolutePath().toString();
    try {
      Git.init().setDirectory(new File(this.repoPath)).call();
    } catch (GitAPIException e) {
      throw new RuntimeException(e);
    }
  }

  @Autowired
  @Bean
  ManifestRepositoryImplProperties getRepoImplProperties(
      @Value("${app.instance-name}") String committerName,
      @Value("${app.reply-to-address}") String committerEmail) {
    return new ManifestRepositoryImplProperties(this.fs, this.repoPath, committerName,
        committerEmail, false);
  }

  /**
   * We won't use an async task executor in unit tests (as we do in production).
   *
   * @return a {@link SyncTaskExecutor} instance.
   */
  @Bean
  TaskExecutor getTaskExecutor() {
    return new SyncTaskExecutor();
  }
}
