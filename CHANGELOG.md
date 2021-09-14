Release notes
=============

Note, that this changelog describes only the changes relevant from the
perspective of the Registry API client implementers, and users which view the
public Registry Service site. Most changes made to the Registry Service touch
the *unofficial* services - such as the document validator, or Echo API
validator - but these changes are currently not mentioned in this changelog, to
avoid clutter.

1.8.0
-----

*Released on 2021-09-14*

 * Added missing releases to change log.
 * Updated MT+ APIs to version 1.0.0.
 * Updated Institutions JAXB schema to version 2.2.0.
 * Updated Imobility ToRs validator to version 1.0.0.
 * Updated Factsheet validator to version 1.0.0.
 * Added Omobility LA v6 support to API validator.
 * Added IIAs v6 support to API validator.
 * Added IIAs v4 support to API validator.
 * Added IIAs v4 to Coverage Matrix.
 * Added OMobility LAs validator.
 * Added OMobilities API validator.
 * Added IMobilities API calidator.
 * Added missing CNR sends value for IIAs v3 to Coverage Matrix.
 * Added IIAs v3 to Coverage Matrix.
 * Detect duplicates inside a single host.
 * Excluded echo and discovery APIs from duplicates.
 * Added Factsheet API Validator.
 * Enabled validator for new stable APIs.
 * Updated schema versions.

1.7.1
-----

*Released on 2020-01-12*

 * Added Echo and Discovery to Coverage Matrix.
 * Moved schemas used by JAXB to separate directory.
 * Removed validator host from production manifest.
 * Send emails when duplicates are detected.
 * Added configuration to run the Registry locally.
 * Added README for console validator.
 * Added EWP Validator HOWTO.
 * Disabled validation in production environment.
 * Provided more statistics about APIs and manifests.
 * Retry after uptimerobot.com request failure.
 * Created command line interface.
 * Added erasmus code to Coverage Matrix.
 * List problems with security methods.
 * Institutions Validator fixes.
 * IMobility ToRs fixes.
 * Fail when a request times out.
 * Reformatting and improvements.
 * Added IMobility ToRs validator.

1.7.0
-----

*Released on 2019-08-27*

 * Added MT+ Dictionaries Validator.
 * Added MT+ Institutions Validator.
 * Added MT+ Projects Validator.
 * Ignore invalid API Entries.
 * Added IIAs validator.
 * Added Courses Validator.
 * Added OUnits Validator.
 * Added validation for MT+ schemas.

1.6.1
-----

*Released on 2018-06-27*

 * Removed unnecessary parts from Dockerfile.
 * Added maintainer's email address on index page.
 * Changed Docker registry to private.

1.6.0
-----

*Released on 2018-01-25*

 * Added support for
   [Discovery Manifest API v5](https://github.com/erasmus-without-paper/ewp-specs-api-discovery/tree/stable-v5).
   Previous versions of manifest files (v4) are also still imported.
 * Updated XSDs and XMLs.
 * Refreshed docker dependencies.
 * Many changes in Echo Validator (e.g. validating encryption specs).
 * Many small fixes on the HEI/API coverage page.
 * Small changes in design.


1.5.0
-----

*Released on 2017-11-16*

 * Added an HTML user interface (up to this point, UI was text-only).
 * Added the HEI/API coverage page.
 * `/refresh` endpoint has been removed, but its features are now partially
   available in the GUI. Note, that forcing manifest reloads is currently NOT
   part of the official API (and, as such, can be removed at any time).


1.4.0
-----

*Released on 2017-09-26*

Implemented the [new Registry API v1.3.0
features](https://github.com/erasmus-without-paper/ewp-specs-api-registry/blob/v1.3.0/CHANGELOG.md)
(in particular, the `rsa-public-key` binaries).


1.3.0
-----

*Released on 2017-08-21*

 * The Registry now rejects client certificates which use unsafe signature
   methods (such as MD5 of SHA-1).

 * The Registry now supports
   [Version 2](https://github.com/erasmus-without-paper/ewp-specs-sec-intro/tree/stable-v2)
   of the *Authentication and Security* document. In particular, it reads the
   RSA keys from the manifest files, verifies them, and moves their
   fingerprints to the catalogue response. Currently, keys are required to be
   at least 2048 bits in length.


1.2.1
-----

*Released on 2016-10-11*

 * Admins will now be notified when HTTP 500 errors are recorded.
 * XXE prevention. Manifests are no longer allowed to include DOCTYPEs.


1.2.0
-----

*Released on 2016-09-08*

 * New deployment method (via Docker Hub).
 * Due to the changed deployment method, default values of lots of properties
   were changed. You will need to tweak your settings before this update.


1.1.0
-----

*Released on 2016-08-29*

 * New `/validate` endpoint added (not part of the API, but will be used by the
   official validation tool which will be soon published somewhere on the
   [Developers Site][develhub]).
 * Updated schemas.
 * More prominent Git configuration errors.
 * Enhancements to the installation docs.


1.0.0
-----

*Released on 2016-08-25*

Initial release.


[develhub]: http://developers.erasmuswithoutpaper.eu/
