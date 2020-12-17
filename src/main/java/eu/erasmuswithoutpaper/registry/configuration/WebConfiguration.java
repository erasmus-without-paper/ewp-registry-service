package eu.erasmuswithoutpaper.registry.configuration;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import eu.erasmuswithoutpaper.registry.documentbuilder.KnownNamespace;
import eu.erasmuswithoutpaper.registry.repository.CatalogueNotFound;
import eu.erasmuswithoutpaper.registry.repository.ManifestRepository;
import eu.erasmuswithoutpaper.registryclient.CatalogueFetcher;
import eu.erasmuswithoutpaper.registryclient.ClientImpl;
import eu.erasmuswithoutpaper.registryclient.ClientImplOptions;
import eu.erasmuswithoutpaper.registryclient.RegistryClient;
import eu.erasmuswithoutpaper.registryclient.RegistryClient.RefreshFailureException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/**
 * Spring beans to be used when running both the actual application server AND unit tests.
 */
@Configuration
@ConditionalOnWebApplication
public class WebConfiguration {
  /**
   * An instance of {@link RegistryClient}.
   *
   * <p>
   * This instance should be kept in sync with the local catalogue copy. Whenever the catalogue is
   * changed, {@link RegistryClient#refresh()} needs to be called explicitly. (This is done by the
   * {@link ManifestRepository} implementations.)
   * </p>
   *
   * @param repo
   *     Needed to feed the client with the current copy of the catalogue.
   * @return {@link RegistryClient} connected to the local catalogue copy.
   */
  @Autowired
  @Bean
  @SuppressFBWarnings("SIC_INNER_SHOULD_BE_STATIC_ANON")
  public RegistryClient getLocalRegistryClient(ManifestRepository repo) {
    ClientImplOptions options = new ClientImplOptions();
    options.setCatalogueFetcher(new CatalogueFetcher() {

      @Override
      public RegistryResponse fetchCatalogue(String etag) throws IOException {
        try {
          return new Http200RegistryResponse(repo.getCatalogue().getBytes(StandardCharsets.UTF_8),
              null, null);
        } catch (CatalogueNotFound e) {
          // We want our local client to never throw UnacceptableStaleness errors.
          String xml = "<catalogue xmlns=\"" + KnownNamespace.RESPONSE_REGISTRY_V1.getNamespaceUri()
              + "\"><institutions/></catalogue>";
          return new Http200RegistryResponse(xml.getBytes(StandardCharsets.UTF_8), null, null);
        }
      }
    });
    // Refresh will be called whenever catalogue is changed, so we don't need to worry
    // about staleness.
    options.setMaxAcceptableStaleness(1000L * 60 * 60 * 24 * 365);
    RegistryClient client = new ClientImpl(options);
    try {
      client.refresh();
    } catch (RefreshFailureException e) {
      // Ignore.
    }
    return client;
  }

  @Bean
  public ConsoleEnvInfo getConsoleEnvInfo() {
    return new ConsoleEnvInfo(false);
  }
}
