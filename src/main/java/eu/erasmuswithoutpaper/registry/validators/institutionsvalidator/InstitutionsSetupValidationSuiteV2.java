package eu.erasmuswithoutpaper.registry.validators.institutionsvalidator;

import eu.erasmuswithoutpaper.registry.documentbuilder.KnownElement;
import eu.erasmuswithoutpaper.registry.documentbuilder.KnownNamespace;
import eu.erasmuswithoutpaper.registry.validators.AbstractSetupValidationSuite;
import eu.erasmuswithoutpaper.registry.validators.ApiValidator;
import eu.erasmuswithoutpaper.registry.validators.HttpSecurityDescription;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Describes the set of test/steps to be run on an Institutions API implementation in order to
 * properly validate it.
 */
class InstitutionsSetupValidationSuiteV2
    extends AbstractSetupValidationSuite<InstitutionsSuiteState> {

  private static final Logger logger =
      LoggerFactory.getLogger(InstitutionsSetupValidationSuiteV2.class);

  InstitutionsSetupValidationSuiteV2(ApiValidator<InstitutionsSuiteState> validator,
      InstitutionsSuiteState state, ValidationSuiteConfig config) {
    super(validator, state, config);
  }

  private int getMaxHeiIds() {
    return getMaxIds("hei-ids");
  }

  //FindBugs is not smart enough to infer that actual type of this.currentState
  //is InstitutionsSuiteState not just SuiteState
  @Override
  @SuppressFBWarnings("BC_UNCONFIRMED_CAST")
  protected void runApiSpecificTests(HttpSecurityDescription securityDescription) {
    this.currentState.maxHeiIds = getMaxHeiIds();
  }

  @Override
  protected Logger getLogger() {
    return logger;
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
