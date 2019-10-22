package eu.erasmuswithoutpaper.registry.consoleapplication;

public interface Stringifier<T> {
  String toString(T value);
}
