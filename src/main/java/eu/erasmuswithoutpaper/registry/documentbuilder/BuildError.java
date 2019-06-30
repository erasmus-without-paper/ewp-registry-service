package eu.erasmuswithoutpaper.registry.documentbuilder;

import javax.xml.bind.ValidationEvent;

import org.xml.sax.SAXParseException;

/**
 * Describes a single error found during {@link EwpDocBuilder#build(BuildParams)} call.
 */
public class BuildError {

  private final int lineNumber;
  private final String message;

  /**
   * Used when the BuildError was caused by an error during XML parsing or validation.
   *
   * @param ex The {@link SAXParseException} that caused the build error.
   */
  BuildError(SAXParseException ex) {
    this.lineNumber = ex.getLineNumber();
    this.message = ex.getMessage();
  }

  /**
   * Used when some other kind of build error occurred. Such errors will be "pinned" to the first
   * line of the file.
   *
   * @param message Error message (plain text).
   */
  BuildError(String message) {
    this.lineNumber = 1;
    this.message = message;
  }

  public BuildError(ValidationEvent validationEvent) {
    this.lineNumber = validationEvent.getLocator().getLineNumber();
    this.message = validationEvent.getMessage();
  }

  /**
   * Get the line number at which the error has occurred.
   *
   * @return Integer. The first line is 1 (not 0!).
   */
  public int getLineNumber() {
    return this.lineNumber;
  }

  /**
   * @return Error message reported (plain text).
   */
  public String getMessage() {
    return this.message;
  }

  @Override
  public String toString() {
    return this.lineNumber + ": " + this.message;
  }
}
