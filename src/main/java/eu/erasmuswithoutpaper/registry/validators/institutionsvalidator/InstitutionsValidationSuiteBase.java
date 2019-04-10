package eu.erasmuswithoutpaper.registry.validators.institutionsvalidator;

import eu.erasmuswithoutpaper.registry.documentbuilder.KnownElement;
import eu.erasmuswithoutpaper.registry.documentbuilder.KnownNamespace;
import eu.erasmuswithoutpaper.registry.validators.AbstractValidationSuite;
import eu.erasmuswithoutpaper.registry.validators.ApiValidator;

public abstract class InstitutionsValidationSuiteBase
    extends AbstractValidationSuite<InstitutionsSuiteState> {
  protected InstitutionsValidationSuiteBase(ApiValidator<InstitutionsSuiteState> echoValidator,
      InstitutionsSuiteState state, ValidationSuiteConfig config) {
    super(echoValidator, state, config);
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
  public String getApiPrefix() {
    return "in2";
  }

  @Override
  public String getApiResponsePrefix() {
    return "inr2";
  }
}
