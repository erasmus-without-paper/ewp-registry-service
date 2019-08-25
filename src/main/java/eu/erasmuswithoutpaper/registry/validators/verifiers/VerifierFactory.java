package eu.erasmuswithoutpaper.registry.validators.verifiers;

import java.util.ArrayList;
import java.util.List;

public class VerifierFactory {
  private final List<String> selector;

  public VerifierFactory(List<String> selector) {
    this.selector = selector;
  }

  public Verifier expectCorrectResponse() {
    return new CorrectResponseVerifier();
  }

  public Verifier expectResponseToContain(List<String> expectedIds) {
    return new InListVerifier(expectedIds, selector);
  }

  public Verifier expectResponseToContainExactly(List<String> expectedIds) {
    return new ListEqualVerifier(expectedIds, selector);
  }

  public Verifier expectResponseToNotContain(String notWantedValue) {
    return new NotInListVerifier(notWantedValue, selector);
  }

  public Verifier expectResponseToBeNotEmpty() {
    return new NonEmptyVerifier(selector);
  }

  public Verifier expectResponseToBeEmpty() {
    return new ListEqualVerifier(new ArrayList<>(), selector);
  }
}
