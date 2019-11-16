package eu.erasmuswithoutpaper.registry.configuration;

import eu.erasmuswithoutpaper.registry.repository.ManifestRepository;
import eu.erasmuswithoutpaper.registryclient.ClientImpl;
import eu.erasmuswithoutpaper.registryclient.ClientImplOptions;
import eu.erasmuswithoutpaper.registryclient.DefaultCatalogueFetcher;
import eu.erasmuswithoutpaper.registryclient.RegistryClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnNotWebApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

@Configuration
@ConditionalOnNotWebApplication
public class ConsoleConfiguration {
  /**
   * An instance of {@link RegistryClient}.
   *
   * <p>
   * This instance should be kept in sync with the local catalogue copy. Whenever the catalogue is
   * changed, {@link RegistryClient#refresh()} needs to be called explicitly. (This is done by the
   * {@link ManifestRepository} implementations.)
   * </p>
   *
   * @return {@link RegistryClient} connected to the local catalogue copy.
   */
  @Bean
  @SuppressFBWarnings("SIC_INNER_SHOULD_BE_STATIC_ANON")
  public RegistryClient getLocalRegistryClient(
      @Value("${app.registry-domain:#{null}}") String registryDomain)
      throws RegistryClient.RefreshFailureException {
    ClientImplOptions options = new ClientImplOptions();
    if (registryDomain == null) {
      registryDomain = "dev-registry.erasmuswithoutpaper.eu";
    }

    options.setCatalogueFetcher(new DefaultCatalogueFetcher(registryDomain));
    options.setMaxAcceptableStaleness(1000 * 60 * 60 * 24 * 365);
    options.setAutoRefreshing(false);
    RegistryClient client = new ClientImpl(options);
    client.refresh();
    return client;
  }

  @Bean
  public ConsoleEnvInfo getConsoleEnvInfo() {
    return new ConsoleEnvInfo(true);
  }
}
