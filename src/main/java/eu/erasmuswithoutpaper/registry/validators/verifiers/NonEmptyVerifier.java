package eu.erasmuswithoutpaper.registry.validators.verifiers;

import java.util.ArrayList;
import java.util.List;

import eu.erasmuswithoutpaper.registry.internet.Response;
import eu.erasmuswithoutpaper.registry.validators.AbstractValidationSuite;
import eu.erasmuswithoutpaper.registry.validators.InlineValidationStep;
import eu.erasmuswithoutpaper.registry.validators.ValidationStepWithStatus;

import org.joox.Match;

public final class NonEmptyVerifier extends Verifier {
  public NonEmptyVerifier(List<String> selector) {
    super(selector);
  }

  @Override
  protected void verify(AbstractValidationSuite suite, Match root, Response response,
      ValidationStepWithStatus.Status failureStatus)
      throws InlineValidationStep.Failure {
    List<Match> foundElements =
        new ArrayList<>(select(root, suite.getApiInfo().getResponsePrefix(), getSelector()));

    if (foundElements.isEmpty()) {
      String defaultMessage = "However the set of returned <" + getParamName() + ">s "
              + "doesn't match what we expect. "
              + "It should be non-empty but it is empty";
      String message = defaultMessage;
      if (customErrorMessage != null) {
        message = customErrorMessage;
      }
      throw new InlineValidationStep.Failure(
          "The response has proper HTTP status and it passed the schema validation. "
          + message, failureStatus, response
      );
    }
  }
}
