package eu.erasmuswithoutpaper.registry.validators.iiavalidator;

import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;

import eu.erasmuswithoutpaper.registry.validators.AbstractValidationSuite;
import eu.erasmuswithoutpaper.registry.validators.ApiEndpoint;
import eu.erasmuswithoutpaper.registry.validators.ApiValidator;
import eu.erasmuswithoutpaper.registry.validators.Combination;
import eu.erasmuswithoutpaper.registry.validators.ValidatedApiInfo;
import eu.erasmuswithoutpaper.registry.validators.ValidationStepWithStatus.Status;
import eu.erasmuswithoutpaper.registry.validators.verifiers.VerifierFactory;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Describes the set of test/steps to be run on an IIAs API index endpoint implementation in order
 * to properly validate it.
 */
public class IiaIndexComplexValidationSuiteV6 extends AbstractValidationSuite<IiaSuiteState> {

  private static final Logger logger =
      LoggerFactory.getLogger(IiaIndexComplexValidationSuiteV6.class);

  private final ValidatedApiInfo apiInfo;

  @Override
  protected Logger getLogger() {
    return logger;
  }

  @Override
  public ValidatedApiInfo getApiInfo() {
    return apiInfo;
  }

  IiaIndexComplexValidationSuiteV6(ApiValidator<IiaSuiteState> validator, IiaSuiteState state,
      ValidationSuiteConfig config, int version) {
    super(validator, state, config);

    this.apiInfo = new IiaValidatedApiInfo(version, ApiEndpoint.INDEX);
  }

  @Override
  //FindBugs is not smart enough to infer that actual type of this.currentState
  //is IiaSuiteState not just SuiteState
  @SuppressFBWarnings("BC_UNCONFIRMED_CAST")
  protected void validateCombinationAny(Combination combination)
      throws SuiteBroken {
    List<String> selectedIiaIdList = Collections.emptyList();
    if (this.currentState.selectedIiaId != null) {
      selectedIiaIdList = Arrays.asList(this.currentState.selectedIiaId);
    }

    testParameters200(
        combination,
        "Request known hei_id and known partner_hei_id, expect 200 OK and "
            + "non-empty response.",
        new ParameterList(
            new Parameter("hei_id", this.currentState.selectedHeiId),
            new Parameter("partner_hei_id", this.currentState.selectedIiaInfo.partnerHeiId)
        ),
        iiaIdVerifierFactory.expectResponseToContain(selectedIiaIdList)
    );

    if (!this.currentState.selectedIiaInfo.receivingAcademicYears.isEmpty()) {
      String knownAcademicYear = this.currentState.selectedIiaInfo.receivingAcademicYears.get(0);
      testParameters200(
          combination,
          "Request with known hei_id and known receiving_academic_year_id parameter, "
              + "expect 200 OK and non-empty response.",
          new ParameterList(
              new Parameter("hei_id", this.currentState.selectedHeiId),
              new Parameter("receiving_academic_year_id", knownAcademicYear)
          ),
          iiaIdVerifierFactory
              .expectResponseToContain(selectedIiaIdList)
      );
    }

    int unknownAcademicYear = 1653; //Arbitrary, but most probably unknown.

    String unknownAcademicYearString =
        String.format("%04d/%04d", unknownAcademicYear, unknownAcademicYear + 1);
    testParameters200(
        combination,
        "Request with known hei_id and unknown receiving_academic_year_id parameter, "
            + "expect 200 OK and empty response.",
        new ParameterList(
            new Parameter("hei_id", this.currentState.selectedHeiId),
            new Parameter("receiving_academic_year_id", unknownAcademicYearString)
        ),
        iiaIdVerifierFactory.expectResponseToBeEmpty()
    );

    int yearInFuture = Calendar.getInstance().get(Calendar.YEAR) + 5;

    testParameters200(
        combination,
        "Request with known hei_id and modified_since in the future, "
            + "expect 200 OK and empty response.",
        new ParameterList(
            new Parameter("hei_id", this.currentState.selectedHeiId),
            new Parameter("modified_since",
                yearInFuture + "-02-12T15:19:21+01:00")
        ),
        iiaIdVerifierFactory.expectResponseToBeEmpty(),
        Status.WARNING
    );

    testParameters200(
        combination,
        "Request with known hei_id and modified_since far in the past, "
            + "expect 200 OK and non-empty response.",
        new ParameterList(
            new Parameter("hei_id", this.currentState.selectedHeiId),
            new Parameter("modified_since", "2000-02-12T15:19:21+01:00")
        ),
        iiaIdVerifierFactory.expectResponseToContain(selectedIiaIdList)
    );
  }

  private VerifierFactory iiaIdVerifierFactory = new VerifierFactory(Arrays.asList("iia-id"));
}
