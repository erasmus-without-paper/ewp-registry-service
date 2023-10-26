package eu.erasmuswithoutpaper.registry.constraints;

import eu.erasmuswithoutpaper.registry.configuration.Constans;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class ConstraintFactory {

  private String registryRepoBaseUrl = Constans.REGISTRY_REPO_URL;

  @Value("${app.registry-production-url}")
  private String productionUrl;

  public ApiUniqueConstraint getApiUniqueConstraint() {
    return new ApiUniqueConstraint(registryRepoBaseUrl);
  }

  public EndpointUniqueConstraint getEndpointUniqueConstraint() {
    return new EndpointUniqueConstraint(registryRepoBaseUrl);
  }

  public ForbidRegistryImplementations getForbidRegistryImplementations() {
    return new ForbidRegistryImplementations(registryRepoBaseUrl, productionUrl);
  }

  public VerifyApiVersions getVerifyApiVersions() {
    return new VerifyApiVersions(registryRepoBaseUrl);
  }
}