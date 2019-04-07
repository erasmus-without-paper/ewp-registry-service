package eu.erasmuswithoutpaper.registry.validators.verifiers;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import eu.erasmuswithoutpaper.registry.internet.Response;
import eu.erasmuswithoutpaper.registry.validators.AbstractValidationSuite;
import eu.erasmuswithoutpaper.registry.validators.InlineValidationStep;
import eu.erasmuswithoutpaper.registry.validators.ValidationStepWithStatus;

import org.joox.Match;

public abstract class Verifier {
  protected final ValidationStepWithStatus.Status status;

  protected Verifier(ValidationStepWithStatus.Status status) {
    this.status = status;
  }

  public abstract void verify(AbstractValidationSuite suite, Match root, Response response) throws
      InlineValidationStep.Failure;

  List<Match> select(Match root, String nsPrefix, String... pathElements) {
    return select(root, nsPrefix, Arrays.asList(pathElements));
  }

  List<Match> select(Match root, String nsPrefix, List<String> pathElements) {
    String selector = String.join(
        "/",
        pathElements.stream().map(s -> nsPrefix + ":" + s).collect(Collectors.toList())
    );
    return root.xpath(selector).each();
  }

  protected abstract List<String> getSelector();

  protected abstract String getParamName();
}
