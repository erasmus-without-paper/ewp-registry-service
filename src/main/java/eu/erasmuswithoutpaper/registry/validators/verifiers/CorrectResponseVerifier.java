package eu.erasmuswithoutpaper.registry.validators.verifiers;

import eu.erasmuswithoutpaper.registry.internet.Response;
import eu.erasmuswithoutpaper.registry.validators.AbstractValidationSuite;
import eu.erasmuswithoutpaper.registry.validators.InlineValidationStep;
import eu.erasmuswithoutpaper.registry.validators.ValidationStepWithStatus;

import org.joox.Match;

public final class CorrectResponseVerifier extends Verifier {
  public CorrectResponseVerifier() {
    super(null);
  }

  @Override
  protected void verify(AbstractValidationSuite<?> suite, Match root, Response response,
      ValidationStepWithStatus.Status failureStatus)
      throws InlineValidationStep.Failure {
    // Nothing to verify, pass
  }
}
