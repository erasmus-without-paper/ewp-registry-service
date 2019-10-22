package eu.erasmuswithoutpaper.registry.configuration;

public class ConsoleEnvInfo {
  private final boolean console;

  public ConsoleEnvInfo(boolean isConsole) {
    this.console = isConsole;
  }

  public boolean isConsole() {
    return console;
  }
}
