package eu.erasmuswithoutpaper.registry.validators.verifiers;

import java.util.List;
import java.util.stream.Collectors;

import eu.erasmuswithoutpaper.registry.internet.Response;
import eu.erasmuswithoutpaper.registry.validators.AbstractValidationSuite;
import eu.erasmuswithoutpaper.registry.validators.InlineValidationStep;
import eu.erasmuswithoutpaper.registry.validators.ValidationStepWithStatus;

import org.joox.Match;

public abstract class InListVerifier extends Verifier {
  private final List<String> wantedValue;

  public InListVerifier(List<String> wantedValue, ValidationStepWithStatus.Status status) {
    super(status);
    this.wantedValue = wantedValue;
  }

  @Override
  public void verify(AbstractValidationSuite suite, Match root, Response response)
      throws InlineValidationStep.Failure {
    List<String> foundElements =
        select(root, suite.getApiInfo().getApiResponsePrefix(), getSelector())
        .stream().map(Match::text).collect(Collectors.toList());

    if (!foundElements.containsAll(wantedValue)) {
      throw new InlineValidationStep.Failure(
          "The response has proper HTTP status and it passed the schema validation. "
              + "However the set of returned <" + getParamName() + ">s "
              + "doesn't match what we expect. "
              + "It should contain <" + getParamName() + ">"
              + wantedValue + "</" + getParamName() + ">, but it doesn't.",
          this.status, response
      );
    }
  }
}
