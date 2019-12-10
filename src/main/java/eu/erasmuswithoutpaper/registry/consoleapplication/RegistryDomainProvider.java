package eu.erasmuswithoutpaper.registry.consoleapplication;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnNotWebApplication;
import org.springframework.stereotype.Service;

@Service
@ConditionalOnNotWebApplication
public class RegistryDomainProvider {
  private final String registryDomain;
  private static final String DEFAULT_REGISTRY_DOMAIN = "dev-registry.erasmuswithoutpaper.eu";

  /**
   * A service that provides domain name of the registry that should be used to fetch the catalogue.
   * It can be changed by setting ${app.registry-domain}.
   */
  @Autowired
  public RegistryDomainProvider(
      @Value("${app.registry-domain:#{null}}") String registryDomain) {
    if (registryDomain == null) {
      this.registryDomain = DEFAULT_REGISTRY_DOMAIN;
    } else {
      this.registryDomain = registryDomain;
    }
  }

  public String getRegistryDomain() {
    return this.registryDomain;
  }
}
