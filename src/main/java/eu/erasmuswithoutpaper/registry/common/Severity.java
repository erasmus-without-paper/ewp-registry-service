package eu.erasmuswithoutpaper.registry.common;

/**
 * An enumeration of severities.
 *
 * <p>
 * It is used as a status for various entities.
 * </p>
 */
public class Severity {

  /**
   * See {@link Severity#isMoreSevereThan(Severity)}.
   */
  @SuppressWarnings("serial")
  public static class OneOfTheValuesIsUndetermined extends Exception {
  }

  /**
   * This value indicates that the severity status has not been determined.
   *
   * <p>
   * Only some entities use this status. If they do, then it usually happens shortly after the
   * entity has been created. (Determining the status of some entities may take some more time.)
   * </p>
   *
   * <p>
   * This severity cannot be compared with other severities via
   * {@link Severity#isMoreSevereThan(Severity)} method.
   * </p>
   */
  public static final Severity UNDETERMINED = new Severity("Undetermined", -1);

  /**
   * Nothing's wrong.
   */
  public static final Severity OK = new Severity("OK", 0);

  /**
   * Warning status. "Someone should be notified."
   */
  public static final Severity WARNING = new Severity("Warning", 5);

  /**
   * Error status. "Someone must do something quickly."
   */
  public static final Severity ERROR = new Severity("Error", 10);

  /**
   * Translate an integer to a {@link Severity}.
   *
   * @param intSeverity integer previously acquired from {@link #getIntegerValue()}.
   * @return One of constant {@link Severity} instances.
   */
  public static Severity fromIntegerValue(int intSeverity) {
    switch (intSeverity) {
      case -1:
        return UNDETERMINED;
      case 0:
        return OK;
      case 5:
        return WARNING;
      case 10:
        return ERROR;
      default:
        throw new RuntimeException();
    }
  }

  private final Integer integerValue;

  private final String name;

  private Severity(String name, int level) {
    this.name = name;
    this.integerValue = level;
  }

  /**
   * @return Serialized value (fit to be stored in a database).
   */
  public int getIntegerValue() {
    return this.integerValue;
  }

  /**
   * Compare the severities of this {@link Severity} against some other. E.g. {@link #WARNING} is
   * more severe than {@link #OK}, and {@link #ERROR} is more severe than {@link #WARNING}.
   *
   * @param other other {@link Severity}.
   * @return <b>true</b> if this {@link Severity} is more severe than the other.
   * @throws OneOfTheValuesIsUndetermined if one of the compared values is {@link #UNDETERMINED}
   *         (this status cannot be compared with any other).
   */
  public boolean isMoreSevereThan(Severity other) throws OneOfTheValuesIsUndetermined {
    if (this.equals(UNDETERMINED) || other.equals(UNDETERMINED)) {
      throw new OneOfTheValuesIsUndetermined();
    }
    return this.integerValue.compareTo(other.integerValue) > 0;
  }

  @Override
  public String toString() {
    return this.name;
  }
}
