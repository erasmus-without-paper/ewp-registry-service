package eu.erasmuswithoutpaper.registry.updater;

import eu.erasmuswithoutpaper.registry.common.Severity;

import com.google.gson.Gson;
import com.google.gson.annotations.SerializedName;

/**
 * Represents a single notice reported during the update process.
 */
public class UpdateNotice {

  @SerializedName("severity")
  private final int severity;

  @SerializedName("messageHtml")
  private final String messageHtml;

  /**
   * @param severity see {@link #getSeverity()}.
   * @param messageHtml see {@link #getMessageHtml()}.
   */
  public UpdateNotice(Severity severity, String messageHtml) {
    this.severity = severity.getIntegerValue();
    this.messageHtml = messageHtml;
  }

  /**
   * This seems to be unused, but it is actually used by {@link Gson} deserializer.
   *
   * <p>
   * See <a href='http://stackoverflow.com/a/18645370/1010931'>here</a>.
   * </p>
   */
  @SuppressWarnings("unused")
  private UpdateNotice() {
    this.severity = Severity.UNDETERMINED.getIntegerValue();
    this.messageHtml = null;
  }

  /**
   * A message with the description of the notice, formatted in HTML.
   *
   * @return HTML String.
   */
  public String getMessageHtml() {
    return this.messageHtml;
  }

  /**
   * The severity of the notice. May influence numerous things, such as the notifications being sent
   * to the people responsible.
   *
   * @return A {@link Severity}.
   */
  public Severity getSeverity() {
    return Severity.fromIntegerValue(this.severity);
  }

  @Override
  public String toString() {
    return this.getSeverity().toString() + ": " + this.getMessageHtml();
  }
}
