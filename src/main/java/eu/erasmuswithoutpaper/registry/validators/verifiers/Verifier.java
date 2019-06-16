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

  /**
   * Returns non empty list of elements' names without namespaces.
   * This list will be joined using '/' to produce a selector in
   * {@link #select(Match, String, List)} method. Selector will be used to extract elements which
   * will be verified.
   * E.g: ["element-0", "element-1"] will select all elements selected by
   * "/response-root/element-0/element-1" xpath selector and perform verification on them.
   *
   * @return non empty list of elements from which selector will be constructed.
   */
  protected abstract List<String> getSelector();

  /**
   * Returns name of verified parameter. It is assumed that this name is last element of
   * the list returned by {@link #getSelector()} method, i.e. the inner-most element that will be
   * selected.
   *
   * @return verified parameter name of empty string if no selector is provided.
   */
  protected String getParamName() {
    List<String> selector = getSelector();
    return selector.get(selector.size() - 1);
  }
}
