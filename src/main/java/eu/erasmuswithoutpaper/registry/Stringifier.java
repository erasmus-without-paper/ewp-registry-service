package eu.erasmuswithoutpaper.registry;

public interface Stringifier<T> {
  String toString(T value);
}
