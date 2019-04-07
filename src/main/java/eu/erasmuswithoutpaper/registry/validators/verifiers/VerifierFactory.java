package eu.erasmuswithoutpaper.registry.validators.verifiers;

import java.util.List;

public interface VerifierFactory {
  Verifier create(List<String> expectedIds);
}
