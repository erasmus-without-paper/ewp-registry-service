package eu.erasmuswithoutpaper.registry.iia;

public class HashComparisonResult {

  private final String hashExtracted;
  private final String hashExpected;
  private final String hashedString;

  HashComparisonResult(String hashExtracted, String hashExpected, String hashedString) {
    this.hashExtracted = hashExtracted;
    this.hashExpected = hashExpected;
    this.hashedString = hashedString;
  }

  public String getHashExtracted() {
    return hashExtracted;
  }

  public String getHashExpected() {
    return hashExpected;
  }

  public String getHashedString() {
    return hashedString;
  }

  public boolean isCorrect() {
    return hashExtracted.equals(hashExpected);
  }
}
