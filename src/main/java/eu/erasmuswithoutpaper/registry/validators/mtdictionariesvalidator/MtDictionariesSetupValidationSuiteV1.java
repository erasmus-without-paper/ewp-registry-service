package eu.erasmuswithoutpaper.registry.validators.mtdictionariesvalidator;

import java.util.Arrays;
import java.util.List;

import eu.erasmuswithoutpaper.registry.validators.AbstractSetupValidationSuite;
import eu.erasmuswithoutpaper.registry.validators.ApiValidator;
import eu.erasmuswithoutpaper.registry.validators.HttpSecurityDescription;
import eu.erasmuswithoutpaper.registry.validators.ValidatedApiInfo;
import eu.erasmuswithoutpaper.registry.validators.ValidationParameter;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class MtDictionariesSetupValidationSuiteV1
    extends AbstractSetupValidationSuite<MtDictionariesSuiteState> {

  private static final Logger logger =
      LoggerFactory.getLogger(MtDictionariesSetupValidationSuiteV1.class);

  private static final ValidatedApiInfo apiInfo = new MtDictionariesValidatedApiInfoV1();

  @Override
  protected Logger getLogger() {
    return logger;
  }

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

  MtDictionariesSetupValidationSuiteV1(ApiValidator<MtDictionariesSuiteState> validator,
      MtDictionariesSuiteState state, ValidationSuiteConfig config) {
    super(validator, state, config);
  }

  //FindBugs is not smart enough to infer that actual type of this.currentState
  //is CoursesSuiteState not just SuiteState
  @Override
  @SuppressFBWarnings("BC_UNCONFIRMED_CAST")
  protected void runApiSpecificTests(HttpSecurityDescription securityDescription)
      throws SuiteBroken {
    if (this.currentState.parameters.contains(DICTIONARY_PARAMETER)) {
      this.currentState.selectedDictionary = this.currentState.parameters.get(DICTIONARY_PARAMETER);
    } else {
      this.currentState.selectedDictionary = getDictionaryParameter();
    }

    if (this.currentState.parameters.contains(CALL_YEAR_PARAMETER)) {
      this.currentState.selectedCallYear = this.currentState.parameters.get(CALL_YEAR_PARAMETER);
    } else {
      this.currentState.selectedCallYear = getCallYear();
    }
  }

  private String getCallYear() {
    return Integer.toString(2016);
  }

  private String getDictionaryParameter() {
    return "key_actions";
  }
}
