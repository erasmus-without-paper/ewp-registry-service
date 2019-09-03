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
  private final List<String> selector;
  protected String customErrorMessage;
  protected boolean verificationResult = false;

  protected Verifier(List<String> selector) {
    this.selector = selector;
  }

  public void performVerificaion(AbstractValidationSuite suite, Match root, Response response,
      ValidationStepWithStatus.Status failureStatus) throws
      InlineValidationStep.Failure {
    this.verify(suite, root, response, failureStatus);
    verificationResult = true;
  }

  protected abstract void verify(AbstractValidationSuite suite, Match root, Response response,
      ValidationStepWithStatus.Status failureStatus) throws
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
  protected final List<String> getSelector() {
    return this.selector;
  }

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

  public void setCustomErrorMessage(String customErrorMessage) {
    this.customErrorMessage = customErrorMessage;
  }

  //PMD linter forces methods returning boolean to start with 'is', it's not want we want.
  public boolean getVerificationResult() { //NOPMD
    return verificationResult;
  }
}
