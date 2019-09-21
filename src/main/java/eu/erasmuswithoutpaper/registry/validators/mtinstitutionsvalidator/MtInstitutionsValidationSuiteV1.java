package eu.erasmuswithoutpaper.registry.validators.mtinstitutionsvalidator;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;

import eu.erasmuswithoutpaper.registry.validators.AbstractValidationSuite;
import eu.erasmuswithoutpaper.registry.validators.ApiValidator;
import eu.erasmuswithoutpaper.registry.validators.Combination;
import eu.erasmuswithoutpaper.registry.validators.ValidatedApiInfo;
import eu.erasmuswithoutpaper.registry.validators.verifiers.VerifierFactory;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Describes the set of test/steps to be run on an MT Institutions API implementation in order to
 * properly validate it.
 */
class MtInstitutionsValidationSuiteV1 extends AbstractValidationSuite<MtInstitutionsSuiteState> {

  private static final Logger logger =
      LoggerFactory.getLogger(MtInstitutionsValidationSuiteV1.class);

  private static final ValidatedApiInfo apiInfo = new MtInstitutionsValidatedApiInfoV1();

  @Override
  protected Logger getLogger() {
    return logger;
  }

  @Override
  public ValidatedApiInfo getApiInfo() {
    return apiInfo;
  }

  MtInstitutionsValidationSuiteV1(ApiValidator<MtInstitutionsSuiteState> validator,
      MtInstitutionsSuiteState state, ValidationSuiteConfig config) {
    super(validator, state, config);
  }

  //FindBugs is not smart enough to infer that actual type of this.currentState
  //is MtInstitutionsSuiteState not just SuiteState
  @SuppressFBWarnings("BC_UNCONFIRMED_CAST")
  protected void validateCombinationAny(Combination combination)
      throws SuiteBroken {
    final String fakePic = "000000001";
    testParameters200(
        combination,
        "Request with known pic and eche_at_date, expect 200 and non empty response.",
        Arrays.asList(
            new Parameter("pic", this.currentState.selectedPic),
            new Parameter("eche_at_date", this.currentState.selectedEcheAtDate)
        ),
        picVerifierFactory.expectResponseToContainExactly(
            Collections.singletonList(this.currentState.selectedPic))
    );

    testParameters200(
        combination,
        "Request with known pic, eche_at_date and invalid parameter, expect 200.",
        Arrays.asList(
            new Parameter("pic", this.currentState.selectedPic),
            new Parameter("eche_at_date", this.currentState.selectedEcheAtDate),
            new Parameter("pic_param", this.currentState.selectedPic)
        ),
        picVerifierFactory.expectResponseToContainExactly(
            Collections.singletonList(this.currentState.selectedPic))
    );

    if (this.currentState.maxIds > 1) {
      testParameters200(
          combination,
          "Request with correct pic twice, expect 200 and two elements in response.",
          Arrays.asList(
              new Parameter("pic", this.currentState.selectedPic),
              new Parameter("pic", this.currentState.selectedPic),
              new Parameter("eche_at_date", this.currentState.selectedEcheAtDate)
          ),
          picVerifierFactory
              .expectResponseToContainExactly(Collections.nCopies(2, this.currentState.selectedPic))
      );
    }

    testParametersError(
        combination,
        "Request with single incorrect parameter, expect 400.",
        Arrays.asList(new Parameter("pic_param", fakeId)),
        400
    );

    testParameters200(
        combination,
        "Request with unknown pic parameter, expect 200 and empty response.",
        Arrays.asList(
            new Parameter("pic", fakePic),
            new Parameter("eche_at_date", this.currentState.selectedEcheAtDate)
        ),
        picVerifierFactory.expectResponseToBeEmpty()
    );

    testParametersError(
        combination,
        "Request without any parameter, expect 400.",
        new ArrayList<>(),
        400
    );

    testParametersError(
        combination,
        "Request with invalid value of eche_at_date, expect 400.",
        Arrays.asList(
            new Parameter("pic", this.currentState.selectedPic),
            new Parameter("eche_at_date", fakeId)
        ),
        400
    );

    testParametersError(
        combination,
        "Request with eche_at_date being a date with time, expect 400.",
        Arrays.asList(
            new Parameter("pic", this.currentState.selectedPic),
            new Parameter("eche_at_date", "2004-02-12T15:19:21+01:00")
        ),
        400
    );

    testParametersError(
        combination,
        "Request with eche_at_date being a date in wrong format, expect 400.",
        Arrays.asList(
            new Parameter("pic", this.currentState.selectedPic),
            new Parameter("eche_at_date", "05/29/2015")
        ),
        400
    );

    if (this.currentState.maxIds > 1) {
      testParameters200(
          combination,
          "Request one known and one unknown pic, expect 200 and only one pic in response.",
          Arrays.asList(
              new Parameter("pic", this.currentState.selectedPic),
              new Parameter("pic", fakePic),
              new Parameter("eche_at_date", this.currentState.selectedEcheAtDate)
          ),
          picVerifierFactory.expectResponseToContainExactly(
              Collections.singletonList(this.currentState.selectedPic))
      );
    }

    testParametersError(
        combination,
        "Request without pic, expect 400.",
        Arrays.asList(
            new Parameter("eche_at_date", this.currentState.selectedEcheAtDate)
        ),
        400
    );

    testParametersError(
        combination,
        "Request without eche_at_date, expect 400.",
        Arrays.asList(
            new Parameter("pic", this.currentState.selectedPic)
        ),
        400
    );

    testParametersError(
        combination,
        "Request more than <max-ids> known PICs, expect 400.",
        concatArrays(
            Arrays.asList(new Parameter("eche_at_date", this.currentState.selectedEcheAtDate)),
            Collections.nCopies(this.currentState.maxIds + 1,
                new Parameter("pic", this.currentState.selectedPic))
        ),
        400
    );

    testParametersError(
        combination,
        "Request more than <max-ids> unknown PICs, expect 400.",
        concatArrays(
            Arrays.asList(new Parameter("eche_at_date", this.currentState.selectedEcheAtDate)),
            Collections.nCopies(this.currentState.maxIds + 1, new Parameter("pic", fakePic))
        ),
        400
    );

    testParameters200(
        combination,
        "Request exactly <max-ids> known PICs, expect 200 and non-empty response.",
        concatArrays(
            Arrays.asList(new Parameter("eche_at_date", this.currentState.selectedEcheAtDate)),
            Collections.nCopies(this.currentState.maxIds,
                new Parameter("pic", this.currentState.selectedPic))
        ),
        picVerifierFactory.expectResponseToContain(
            Collections.singletonList(this.currentState.selectedPic))
    );
  }

  private VerifierFactory picVerifierFactory = new VerifierFactory(Arrays.asList("hei", "pic"));
}
