package eu.erasmuswithoutpaper.registry.validators.verifiers;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import eu.erasmuswithoutpaper.registry.internet.Response;
import eu.erasmuswithoutpaper.registry.validators.AbstractValidationSuite;
import eu.erasmuswithoutpaper.registry.validators.InlineValidationStep;
import eu.erasmuswithoutpaper.registry.validators.ValidationStepWithStatus;

import org.joox.Match;

public abstract class ListEqualVerifier extends Verifier {
  private final List<String> expected;

  protected ListEqualVerifier(List<String> expected, ValidationStepWithStatus.Status status) {
    super(status);
    this.expected = expected;
  }

  @Override
  public void verify(AbstractValidationSuite suite, Match root, Response response)
      throws InlineValidationStep.Failure {
    List<String> actual =
        select(root, suite.getApiInfo().getApiResponsePrefix(), getSelector())
            .stream().map(Match::text).collect(Collectors.toList());

    ArrayList<String> unexpectedValues = new ArrayList<>(actual);
    for (String oneExpected : expected) {
      unexpectedValues.remove(oneExpected);
    }

    ArrayList<String> notReceivedValues = new ArrayList<>(expected);
    for (String oneActual : actual) {
      notReceivedValues.remove(oneActual);
    }

    if (!unexpectedValues.isEmpty()) {
      throw new InlineValidationStep.Failure(
          "The response has proper HTTP status and it passed the schema validation. However"
              + "the set of returned " + getParamName() + "s doesn't match what we expect. "
              + "It contains those unexpected values: " + unexpectedValues + " "
              + "It should contain the following values: " + expected,
          this.status, response
      );
    }

    if (!notReceivedValues.isEmpty()) {
      throw new InlineValidationStep.Failure(
          "The response has proper HTTP status and it passed the schema validation. However, "
              + "the set of returned " + getParamName() + "s doesn't match what we expect. "
              + "It does not contain those expected values: " + notReceivedValues + " "
              + "It should contain the following values: " + expected,
          this.status, response
      );
    }
  }
}
