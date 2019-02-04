package eu.erasmuswithoutpaper.registry.validators.institutionsvalidator;

import eu.erasmuswithoutpaper.registry.documentbuilder.EwpDocBuilder;
import eu.erasmuswithoutpaper.registry.documentbuilder.KnownElement;
import eu.erasmuswithoutpaper.registry.documentbuilder.KnownNamespace;
import eu.erasmuswithoutpaper.registry.internet.Internet;
import eu.erasmuswithoutpaper.registry.repository.ManifestRepository;
import eu.erasmuswithoutpaper.registry.validators.AbstractValidationSuite;
import eu.erasmuswithoutpaper.registry.validators.ApiValidator;
import eu.erasmuswithoutpaper.registryclient.RegistryClient;

public abstract class InstitutionsValidationSuiteBase
    extends AbstractValidationSuite<InstitutionsSuiteState> {
  protected InstitutionsValidationSuiteBase(ApiValidator<InstitutionsSuiteState> echoValidator,
      EwpDocBuilder docBuilder, Internet internet, String urlStr, RegistryClient regClient,
      ManifestRepository repo, InstitutionsSuiteState state) {
    super(echoValidator, docBuilder, internet, urlStr, regClient, repo, state);
  }

  @Override
  protected KnownElement getKnownElement() {
    return KnownElement.RESPONSE_INSTITUTIONS_V2;
  }

  @Override
  protected String getApiNamespace() {
    return KnownNamespace.APIENTRY_INSTITUTIONS_V2.getNamespaceUri();
  }

  @Override
  protected String getApiName() {
    return "institutions";
  }

  @Override
  protected String getApiVersion() {
    return "2.0.0";
  }

  @Override
  public String getApiPrefix() {
    return "in2";
  }

  @Override
  public String getApiResponsePrefix() {
    return "inr2";
  }
}
