package eu.erasmuswithoutpaper.registry.validators.mtdictionariesvalidator;

import java.util.Arrays;
import java.util.List;

import eu.erasmuswithoutpaper.registry.validators.AbstractSetupValidationSuite;
import eu.erasmuswithoutpaper.registry.validators.ApiEndpoint;
import eu.erasmuswithoutpaper.registry.validators.ApiValidator;
import eu.erasmuswithoutpaper.registry.validators.HttpSecurityDescription;
import eu.erasmuswithoutpaper.registry.validators.ValidatedApiInfo;
import eu.erasmuswithoutpaper.registry.validators.ValidationParameter;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

class MtDictionariesSetupValidationSuite
    extends AbstractSetupValidationSuite<MtDictionariesSuiteState> {

  private final ValidatedApiInfo apiInfo;

  @Override
  public ValidatedApiInfo getApiInfo() {
    return apiInfo;
  }

  private static final String DICTIONARY_PARAMETER = "dictionary";
  private static final String CALL_YEAR_PARAMETER = "call_year";

  public static List<ValidationParameter> getParameters() {
    return Arrays.asList(
        new ValidationParameter(DICTIONARY_PARAMETER),
        new ValidationParameter(CALL_YEAR_PARAMETER)
    );
  }

  MtDictionariesSetupValidationSuite(ApiValidator<MtDictionariesSuiteState> validator,
      MtDictionariesSuiteState state, ValidationSuiteConfig config, int version) {
    super(validator, state, config, false);

    this.apiInfo = new MtDictionariesValidatedApiInfo(version, ApiEndpoint.NO_ENDPOINT);
  }

  //FindBugs is not smart enough to infer that actual type of this.currentState
  //is CoursesSuiteState not just SuiteState
  @Override
  @SuppressFBWarnings("BC_UNCONFIRMED_CAST")
  protected void runApiSpecificTests(HttpSecurityDescription securityDescription)
      throws SuiteBroken {
    this.currentState.selectedDictionary = getParameterValue(DICTIONARY_PARAMETER,
        this::getDictionaryParameter);
    this.currentState.selectedCallYear = getParameterValue(CALL_YEAR_PARAMETER, this::getCallYear);
  }

  private String getCallYear() {
    return Integer.toString(2016);
  }

  private String getDictionaryParameter() {
    return "key_actions";
  }
}
