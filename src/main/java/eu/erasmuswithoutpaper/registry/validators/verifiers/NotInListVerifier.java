package eu.erasmuswithoutpaper.registry.validators.verifiers;

import java.util.List;

import eu.erasmuswithoutpaper.registry.internet.Response;
import eu.erasmuswithoutpaper.registry.validators.AbstractValidationSuite;
import eu.erasmuswithoutpaper.registry.validators.InlineValidationStep;
import eu.erasmuswithoutpaper.registry.validators.ValidationStepWithStatus;

import org.joox.Match;

public class NotInListVerifier extends Verifier {
  private final String notWantedValue;

  public NotInListVerifier(String notWantedValue, List<String> selector) {
    super(selector);
    this.notWantedValue = notWantedValue;
  }

  @Override
  protected void verify(AbstractValidationSuite suite, Match root, Response response,
      ValidationStepWithStatus.Status failureStatus)
      throws InlineValidationStep.Failure {
    boolean found = select(root, suite.getApiInfo().getResponsePrefix(), getSelector())
        .stream().anyMatch(x -> x.text().equals(notWantedValue));

    if (found) {
      throw new InlineValidationStep.Failure(
          "The response has proper HTTP status and it passed the schema validation. "
              + "However the set of returned <" + getParamName() + ">s "
              + "doesn't match what we expect. "
              + "It should not contain <" + getParamName() + ">"
              + notWantedValue + "</" + getParamName() + ">, but it does.",
          failureStatus, response
      );
    }
  }
}
