package eu.erasmuswithoutpaper.registry.validators;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.RUNTIME)
public @interface ValidatorTestStep {
  /**
   * @return
   *     a minimal major version a tested API needs to have.
   */
  String minMajorVersion() default "0";

  /**
   * @return
   *     a maximal major version a tested API needs to have.
   */
  String maxMajorVersion() default "inf";
}