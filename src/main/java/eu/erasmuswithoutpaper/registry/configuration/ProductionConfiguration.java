package eu.erasmuswithoutpaper.registry.configuration;

import java.nio.file.FileSystems;

import eu.erasmuswithoutpaper.registry.repository.ManifestRepositoryImplProperties;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

/**
 * Spring beans to be used when running an actual application server.
 */
@Profile({ "production", "development" })
@Configuration
public class ProductionConfiguration {

  public ProductionConfiguration() {
    // Allow manual setting of "Content-Length" header in requests
    allowSettingRestrictedHeaders();
  }

  private void allowSettingRestrictedHeaders() {
    System.setProperty("sun.net.http.allowRestrictedHeaders", "true");
  }

  /**
   * Get {@link ManifestRepositoryImplProperties} to be used in production environment.
   *
   * <p>
   * When running the actual application server, {@link ManifestRepositoryImplProperties} will be
   * initialized based on the values in the <code>application.properties</code> file.
   * </p>
   *
   * @param path Path to the Git repository folder.
   * @param committerName Name to be used when committing a Git change.
   * @param committerEmail Email to be used when committing a Git change.
   * @param enablePushing whether to enable pushing Git changes to a remote repository or not.
   * @return {@link ManifestRepositoryImplProperties} instance.
   */
  @Autowired
  @Bean
  public ManifestRepositoryImplProperties getRepoImplProperties(
      @Value("${app.repo.path}") String path, @Value("${app.instance-name}") String committerName,
      @Value("${app.reply-to-address}") String committerEmail,
      @Value("${app.repo.enable-pushing}") boolean enablePushing) {
    if (path.length() == 0) {
      throw new RuntimeException("Missing app.repo.path property");
    }
    return new ManifestRepositoryImplProperties(FileSystems.getDefault(), path, committerName,
        committerEmail, enablePushing);
  }

  /**
   * Get {@link ThreadPoolTaskExecutor} to be used in production environment.
   *
   * <p>
   * When running the actual application server, a {@link ThreadPoolTaskExecutor} will be used for
   * handling async runnables.
   * </p>
   *
   * @return {@link ThreadPoolTaskExecutor} instance.
   */
  @Bean
  public TaskExecutor getTaskExecutor() {
    return new ThreadPoolTaskExecutor();
  }
}
