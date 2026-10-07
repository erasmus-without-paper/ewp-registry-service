package eu.erasmuswithoutpaper.registry.common;

import java.io.Serial;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public abstract class AcademicYearUtils {
  private static final String ACADEMIC_YEAR_SEPARATOR = "/";
  private static final Pattern ACADEMIC_YEAR_ID_PATTERN =
      Pattern.compile("^2[0-9]{3}" + ACADEMIC_YEAR_SEPARATOR + "2[0-9]{3}$");

  /**
   * @param academicYearId academic year ID to check
   * @return true if academicYearId is in YYYY/YYYY format with endYear = startYear + 1
   */
  public static boolean isValidAcademicYearId(String academicYearId) {
    if (academicYearId == null) {
      return false;
    }
    Matcher matcher = ACADEMIC_YEAR_ID_PATTERN.matcher(academicYearId);
    if (!matcher.matches()) {
      return false;
    }
    String[] years = academicYearId.split(ACADEMIC_YEAR_SEPARATOR);
    return Integer.parseInt(years[1]) == Integer.parseInt(years[0]) + 1;
  }

  /**
   * @param firstAcademicYearId first academic year ID
   * @param lastAcademicYearId last academic year ID
   * @return list of academic year IDs between first and last
   * @throws IncorrectAcademicYearException when academic year ID cannot be parsed
   */
  public static Collection<String> getAcademicYearsBetween(String firstAcademicYearId,
      String lastAcademicYearId) throws IncorrectAcademicYearException {
    int lastStartYear = getStartYear(lastAcademicYearId);
    List<String> academicYears = new ArrayList<>();
    for (int startYear =
        getStartYear(firstAcademicYearId); startYear < lastStartYear; startYear++) {
      academicYears.add(getAcademicYear(startYear));
    }
    return academicYears;
  }

  private static int getStartYear(String academicYearId) throws IncorrectAcademicYearException {
    Matcher matcher = ACADEMIC_YEAR_ID_PATTERN.matcher(academicYearId);
    if (!matcher.matches()) {
      throw new IncorrectAcademicYearException(academicYearId);
    }

    String[] years = academicYearId.split(ACADEMIC_YEAR_SEPARATOR);
    int startYear = Integer.parseInt(years[0]);
    int endYear = Integer.parseInt(years[1]);

    if (endYear != startYear + 1) {
      throw new IncorrectAcademicYearException(academicYearId);
    }

    return startYear;
  }

  private static String getAcademicYear(int startYear) {
    return startYear + ACADEMIC_YEAR_SEPARATOR + (startYear + 1);
  }

  public static class IncorrectAcademicYearException extends RuntimeException {
    @Serial
    private static final long serialVersionUID = 865089606806701466L;

    public IncorrectAcademicYearException(String academicYearId) {
      super("Wrong academic year format: " + academicYearId);
    }
  }
}
