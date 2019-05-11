package eu.erasmuswithoutpaper.registry.validators.institutionsvalidator;

import java.util.Arrays;
import java.util.List;

import eu.erasmuswithoutpaper.registry.documentbuilder.KnownElement;
import eu.erasmuswithoutpaper.registry.documentbuilder.KnownNamespace;
import eu.erasmuswithoutpaper.registry.validators.AbstractSetupValidationSuite;
import eu.erasmuswithoutpaper.registry.validators.ApiValidator;
import eu.erasmuswithoutpaper.registry.validators.HttpSecurityDescription;
import eu.erasmuswithoutpaper.registry.validators.ValidationParameter;

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
  private static final String HEI_ID_PARAMETER = "hei_id";

  InstitutionsSetupValidationSuiteV2(ApiValidator<InstitutionsSuiteState> validator,
      InstitutionsSuiteState state, ValidationSuiteConfig config) {
    super(validator, state, config);
  }

  public static List<ValidationParameter> getParameters() {
    return Arrays.asList(new ValidationParameter(HEI_ID_PARAMETER));
  }

  private int getMaxHeiIds() {
    return getMaxIds("hei-ids");
  }

  //FindBugs is not smart enough to infer that actual type of this.currentState
  //is InstitutionsSuiteState not just SuiteState
  @Override
  @SuppressFBWarnings("BC_UNCONFIRMED_CAST")
  protected void runApiSpecificTests(HttpSecurityDescription securityDescription)
      throws SuiteBroken {
    this.currentState.maxHeiIds = getMaxHeiIds();
    if (this.currentState.parameters.contains(HEI_ID_PARAMETER)) {
      this.currentState.selectedHeiId = this.currentState.parameters.get(HEI_ID_PARAMETER);
    } else {
      this.currentState.selectedHeiId = getCoveredHeiIds(this.currentState.url).get(0);
    }
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
  public String getApiPrefix() {
    return "in2";
  }

  @Override
  public String getApiResponsePrefix() {
    return "inr2";
  }
}
