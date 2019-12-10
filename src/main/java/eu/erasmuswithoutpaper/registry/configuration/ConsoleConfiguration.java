package eu.erasmuswithoutpaper.registry.configuration;

import eu.erasmuswithoutpaper.registry.consoleapplication.RegistryDomainProvider;
import eu.erasmuswithoutpaper.registryclient.ClientImpl;
import eu.erasmuswithoutpaper.registryclient.ClientImplOptions;
import eu.erasmuswithoutpaper.registryclient.DefaultCatalogueFetcher;
import eu.erasmuswithoutpaper.registryclient.RegistryClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnNotWebApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

@Configuration
@ConditionalOnNotWebApplication
public class ConsoleConfiguration {
  /**
   * An instance of {@link RegistryClient}.
   * Client to a registry that is available remotely.
   * It's address is taken from RegistryDomainProvider.
   */
  @Autowired
  @Bean
  @SuppressFBWarnings("SIC_INNER_SHOULD_BE_STATIC_ANON")
  public RegistryClient getRemoteRegistryClient(
      RegistryDomainProvider registryDomainProvider)
      throws RegistryClient.RefreshFailureException {
    ClientImplOptions options = new ClientImplOptions();
    String registryDomain = registryDomainProvider.getRegistryDomain();

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
