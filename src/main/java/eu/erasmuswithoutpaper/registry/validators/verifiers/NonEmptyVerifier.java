package eu.erasmuswithoutpaper.registry.validators.verifiers;

import java.util.ArrayList;
import java.util.List;

import eu.erasmuswithoutpaper.registry.internet.Response;
import eu.erasmuswithoutpaper.registry.validators.AbstractValidationSuite;
import eu.erasmuswithoutpaper.registry.validators.InlineValidationStep;
import eu.erasmuswithoutpaper.registry.validators.ValidationStepWithStatus;

import org.joox.Match;

public abstract class NonEmptyVerifier extends Verifier {
  public NonEmptyVerifier(ValidationStepWithStatus.Status status) {
    super(status);
  }

  @Override
  public void verify(AbstractValidationSuite suite, Match root, Response response)
      throws InlineValidationStep.Failure {
    List<Match> foundElements =
        new ArrayList<>(select(root, suite.getApiInfo().getResponsePrefix(), getSelector()));

    if (foundElements.isEmpty()) {
      throw new InlineValidationStep.Failure(
          "The response has proper HTTP status and it passed the schema validation. "
              + "However the set of returned <" + getParamName() + ">s "
              + "doesn't match what we expect. "
              + "It should be non-empty but it is empty",
          this.status, response
      );
    }
  }
}
