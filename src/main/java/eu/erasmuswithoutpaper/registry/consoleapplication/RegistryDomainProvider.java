package eu.erasmuswithoutpaper.registry.consoleapplication;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnNotWebApplication;
import org.springframework.stereotype.Service;

@Service
@ConditionalOnNotWebApplication
public class RegistryDomainProvider {
  private final String registryDomain;

  /**
   * A service that provides domain name of the registry that should be used to fetch the catalogue.
   * It can be changed by setting ${app.registry-domain}.
   * @param registryDomain
   *      Registry domain, without https:// and trailing slash.
   */
  @Autowired
  public RegistryDomainProvider(
      @Value("${app.registry-domain:dev-registry.erasmuswithoutpaper.eu}") String registryDomain) {
    this.registryDomain = registryDomain;
  }

  public String getRegistryDomain() {
    return this.registryDomain;
  }
}
