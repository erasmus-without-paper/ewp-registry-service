package eu.erasmuswithoutpaper.registry.validators;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

import eu.erasmuswithoutpaper.registry.WRTest;

import org.junit.jupiter.api.Test;

public class SemanticVersionTest extends WRTest {
  @Test
  public void testParse() throws SemanticVersion.InvalidVersionString {
    SemanticVersion v200 = new SemanticVersion(2, 0, 0);
    SemanticVersion v200str1 = new SemanticVersion("2.0.0");
    SemanticVersion v200str2 = new SemanticVersion("v2.0.0");
    assertThat(v200).isEqualTo(v200str1);
    assertThat(v200).isEqualTo(v200str2);
    assertThat(v200str1).isEqualTo(v200str2);

    SemanticVersion v200rc1 = new SemanticVersion(2, 0, 0, 1);
    SemanticVersion v200rc1str1 = new SemanticVersion("2.0.0-rc1");
    SemanticVersion v200rc1str2 = new SemanticVersion("v2.0.0-rc1");
    assertThat(v200rc1).isEqualTo(v200rc1str1);
    assertThat(v200rc1).isEqualTo(v200rc1str2);
    assertThat(v200rc1str1).isEqualTo(v200rc1str2);

    Throwable thrown;
    thrown = catchThrowable(() -> new SemanticVersion("v2.0.0.0"));
    assertThat(thrown).isInstanceOf(SemanticVersion.InvalidVersionString.class);

    thrown = catchThrowable(() -> new SemanticVersion(""));
    assertThat(thrown).isInstanceOf(SemanticVersion.InvalidVersionString.class);

    thrown = catchThrowable(() -> new SemanticVersion("-1.0.0"));
    assertThat(thrown).isInstanceOf(SemanticVersion.InvalidVersionString.class);

    thrown = catchThrowable(() -> new SemanticVersion("1.0.0-rc"));
    assertThat(thrown).isInstanceOf(SemanticVersion.InvalidVersionString.class);

    thrown = catchThrowable(() -> new SemanticVersion("1.0.0-rc-1"));
    assertThat(thrown).isInstanceOf(SemanticVersion.InvalidVersionString.class);
  }

  @Test
  public void testCompatible() {
    SemanticVersion semanticVersion200 = new SemanticVersion(2, 0, 0);
    SemanticVersion semanticVersion210 = new SemanticVersion(2, 1, 0);
    SemanticVersion semanticVersion211 = new SemanticVersion(2, 1, 1);
    assertThat(semanticVersion210.isCompatible(semanticVersion200)).isTrue();
    assertThat(semanticVersion200.isCompatible(semanticVersion200)).isTrue();
    assertThat(semanticVersion200.isCompatible(semanticVersion210)).isFalse();

    assertThat(semanticVersion211.isCompatible(semanticVersion200)).isTrue();
    assertThat(semanticVersion211.isCompatible(semanticVersion210)).isTrue();
    assertThat(semanticVersion211.isCompatible(semanticVersion211)).isTrue();
    assertThat(semanticVersion200.isCompatible(semanticVersion211)).isFalse();
    assertThat(semanticVersion210.isCompatible(semanticVersion211)).isFalse();
  }

  @Test
  public void testReleaseCandidates() {
    SemanticVersion semanticVersion200 = new SemanticVersion(2, 0, 0);
    SemanticVersion semanticVersion210rc1 = new SemanticVersion(2, 1, 0, 1);
    SemanticVersion semanticVersion210rc2 = new SemanticVersion(2, 1, 0, 2);
    SemanticVersion semanticVersion210rc3 = new SemanticVersion(2, 1, 0, 3);
    SemanticVersion semanticVersion210 = new SemanticVersion(2, 1, 0);

    assertThat(semanticVersion200.compareTo(semanticVersion210rc1) < 0).isTrue();
    assertThat(semanticVersion210rc1.compareTo(semanticVersion210rc2) < 0).isTrue();
    assertThat(semanticVersion210rc2.compareTo(semanticVersion210rc3) < 0).isTrue();
    assertThat(semanticVersion210rc3.compareTo(semanticVersion210) < 0).isTrue();

    assertThat(semanticVersion210rc3.compareTo(semanticVersion210) < 0).isTrue();
    assertThat(semanticVersion210rc2.compareTo(semanticVersion210rc3) < 0).isTrue();
    assertThat(semanticVersion210rc1.compareTo(semanticVersion210rc2) < 0).isTrue();
    assertThat(semanticVersion200.compareTo(semanticVersion210rc1) < 0).isTrue();
  }
}
