package eu.erasmuswithoutpaper.registry.validators.iiavalidator.index;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;

import eu.erasmuswithoutpaper.registry.validators.AbstractValidationSuite;
import eu.erasmuswithoutpaper.registry.validators.ApiValidator;
import eu.erasmuswithoutpaper.registry.validators.Combination;
import eu.erasmuswithoutpaper.registry.validators.ValidatedApiInfo;
import eu.erasmuswithoutpaper.registry.validators.ValidationStepWithStatus.Status;
import eu.erasmuswithoutpaper.registry.validators.iiavalidator.IiaSuiteState;
import eu.erasmuswithoutpaper.registry.validators.verifiers.InListVerifier;
import eu.erasmuswithoutpaper.registry.validators.verifiers.ListEqualVerifier;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Describes the set of test/steps to be run on an IIAs API index endpoint implementation in order
 * to properly validate it.
 */
class IiaIndexComplexValidationSuiteV2 extends AbstractValidationSuite<IiaSuiteState> {

  private static final Logger logger =
      LoggerFactory.getLogger(IiaIndexComplexValidationSuiteV2.class);

  private static final ValidatedApiInfo apiInfo = new IiaIndexValidatedApiInfo();

  @Override
  protected Logger getLogger() {
    return logger;
  }

  @Override
  public ValidatedApiInfo getApiInfo() {
    return apiInfo;
  }

  IiaIndexComplexValidationSuiteV2(ApiValidator<IiaSuiteState> validator,
      IiaSuiteState state, ValidationSuiteConfig config) {
    super(validator, state, config);
  }

  //FindBugs is not smart enough to infer that actual type of this.currentState
  //is IiaSuiteState not just SuiteState
  @SuppressFBWarnings("BC_UNCONFIRMED_CAST")
  protected void validateCombinationAny(Combination combination)
      throws SuiteBroken {
    testParameters200(
        combination,
        "Request known hei_id and known partner_hei_id, expect 200 OK and "
            + "non-empty response.",
        Arrays.asList(
            new Parameter("hei_id", this.currentState.selectedHeiId),
            new Parameter("partner_hei_id", this.currentState.selectedIiaInfo.partnerHeiId)
        ),
        new IiaIndexVerifier(Arrays.asList(this.currentState.selectedIiaId))
    );

    String knownAcademicYear = this.currentState.selectedIiaInfo.receivingAcademicYears.get(0);
    testParameters200(
        combination,
        "Request with known hei_id and known receiving_academic_year_id parameter, "
            + "expect 200 OK and non-empty response.",
        Arrays.asList(
            new Parameter("hei_id", this.currentState.selectedHeiId),
            new Parameter("receiving_academic_year_id", knownAcademicYear)
        ),
        new IiaIndexVerifier(Arrays.asList(this.currentState.selectedIiaId))
    );

    int unknownAcademicYear = this.currentState.selectedIiaInfo.receivingAcademicYears.stream()
        .map(x -> Integer.valueOf(x.split("/")[0]))
        .min(Integer::compareTo).orElse(1980) - 1;

    String unknownAcademicYearString =
        String.format("%04d/%04d", unknownAcademicYear, unknownAcademicYear + 1);
    testParameters200(
        combination,
        "Request with known hei_id and unknown receiving_academic_year_id parameter, "
            + "expect 200 OK and empty response.",
        Arrays.asList(
            new Parameter("hei_id", this.currentState.selectedHeiId),
            new Parameter("receiving_academic_year_id", unknownAcademicYearString)
        ),
        new IndexEmptyListVerifier()
    );

    int yearInFuture = Calendar.getInstance().get(Calendar.YEAR) + 5;

    testParameters200(
        combination,
        "Request with known hei_id and modified_since in the future, "
            + "expect 200 OK and empty response.",
        Arrays.asList(
            new Parameter("hei_id", this.currentState.selectedHeiId),
            new Parameter("modified_since",
                yearInFuture + "-02-12T15:19:21+01:00")
        ),
        new IndexEmptyListVerifier(Status.WARNING)
    );

    testParameters200(
        combination,
        "Request with known hei_id and modified_since far in the past, "
            + "expect 200 OK and non-empty response.",
        Arrays.asList(
            new Parameter("hei_id", this.currentState.selectedHeiId),
            new Parameter("modified_since", "1970-02-12T15:19:21+01:00")
        ),
        new IiaIndexVerifier(Arrays.asList(this.currentState.selectedIiaId))
    );
  }

  private static class IiaIndexVerifier extends InListVerifier {
    IiaIndexVerifier(List<String> expected, Status status) {
      super(expected, status);
    }

    IiaIndexVerifier(List<String> expected) {
      this(expected, Status.FAILURE);
    }

    @Override
    protected List<String> getSelector() {
      return Arrays.asList("iia-id");
    }
  }

  private static class IndexEmptyListVerifier extends ListEqualVerifier {
    IndexEmptyListVerifier(Status status) {
      super(new ArrayList<>(), status);
    }

    IndexEmptyListVerifier() {
      this(Status.FAILURE);
    }

    @Override
    protected List<String> getSelector() {
      return Arrays.asList("iia-id");
    }
  }
}
