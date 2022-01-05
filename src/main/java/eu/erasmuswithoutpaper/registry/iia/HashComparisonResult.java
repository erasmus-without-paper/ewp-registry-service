package eu.erasmuswithoutpaper.registry.iia;

public class HashComparisonResult {

  String hashExtracted;
  String hashExpected;

  public HashComparisonResult(String hashExtracted, String hashExpected) {
    this.hashExtracted = hashExtracted;
    this.hashExpected = hashExpected;
  }

  public String getHashExtracted() {
    return hashExtracted;
  }

  public String getHashExpected() {
    return hashExpected;
  }

  public boolean isCorrect() {
    return hashExtracted.equals(hashExpected);
  }
}
