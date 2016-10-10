package eu.erasmuswithoutpaper.registry.constraints;

import java.util.List;

import eu.erasmuswithoutpaper.registry.sourceprovider.ManifestSource;

import org.w3c.dom.Document;

/**
 * Implementations of this interface can scan and <b>modify</b> a Discovery Manifest document in
 * certain ways, so that the document fit certain constraints.
 *
 * <p>
 * Each {@link ManifestSource} will have a set of constraints bound to it. These constraints will
 * need to be met before the manifest will be imported. {@link ManifestConstraint} implementation
 * need to be able to both detect the problems <b>and fix</b> them.
 * </p>
 */
public interface ManifestConstraint {

  /**
   * Scan the given Manifest document, make sure it fits the constraint, and - if not - try to apply
   * transformations to the document so that it will fit the constraint.
   *
   * <p>
   * Depending on the particular implementation of the {@link ManifestConstraint}, and the nature of
   * the constraint itself, this method may either modify parts of the manifest (in case of serious
   * errors), or simply warn the user of their existence.
   * </p>
   *
   * @param document A document with a valid (in the XML Schema sense) manifest document (all
   *        callers must make sure the document is valid before calling this method).
   * @return A list of {@link FailedConstraintNotice}s which describe the transformations applied
   *         (or, in case of not so serious violations, describe how the user should fix them
   *         himself).
   */
  List<FailedConstraintNotice> filter(Document document);
}
