package eu.erasmuswithoutpaper.registry.validators.verifiers;

import java.util.List;

import eu.erasmuswithoutpaper.registry.internet.Response;
import eu.erasmuswithoutpaper.registry.validators.AbstractValidationSuite;
import eu.erasmuswithoutpaper.registry.validators.InlineValidationStep;
import eu.erasmuswithoutpaper.registry.validators.ValidationStepWithStatus;

import org.joox.Match;

public class NoopVerifier extends Verifier {
  public NoopVerifier() {
    super(ValidationStepWithStatus.Status.FAILURE);
  }

  @Override
  public void verify(AbstractValidationSuite suite, Match root, Response response)
      throws InlineValidationStep.Failure {
    // Nothing to verify, pass
  }

  @Override
  protected List<String> getSelector() {
    return null;
  }
}
