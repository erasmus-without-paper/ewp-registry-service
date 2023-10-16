Release notes
=============

1.11.1
-----

*Released on 2023-10-17*

* Change java's runtime in docker (11 -> 17)
* Spring Boot update (2.1.18 -> 2.7.16)
* Pretty XML won't end with empty line
* Disable spring.jpa.open-in-view
* Stop using Apache HttpClient outside tests
* Use Maven Enforcer Plugin (require Maven >= 6.3.0)
* Tests not only extends one class

* Updated other dependencies:
    * Pebble (3.1.0 -> 3.1.5)
    * text-io (3.3.0 -> 3.4.1)
    * commons-io (2.13.0 -> 2.14.0)
    * PrettyTime (4.0.6 -> 5.0.7)
    * Postcss (8.4.29 -> 8.4.31)
    * Frontend Maven Plugin (1.13.4 -> 1.14.0)
    * Spotbugs Maven Plugin (4.7.3.5 -> 4.7.3.6)

1.11.0
-----

*Released on 2023-10-03*

* Require & use Java 11
* Coverage Matrix:
    * Optimize Coverage CSS for smaller HTML size (about 77% reduction)
    * Improve styles for API versions containing warnings
    * Fix concurrency issue
    * Add hard spaces in Erasmus code
* Remove support for client TLS authentication
* Adding build info to the main page
* "How to join" on the main page
* Replace HEI Search Page with link to EWP Stats Portal (on Production)
* Change Docker base image to eclipse-temurin:11-jre-jammy and enable JVM args on start
* Maven profile 'skipValidation' for faster builds
* Support multiple manifest sources files
* Speed up updates by removing XPath calls
* Many, many fixes of concurrency issues and api versions compatibility
* Add info about APIs that lack stats endpoint implementation
* Corrected RFC 2616 date format in validator (fixes
  [#28](https://github.com/erasmus-without-paper/ewp-registry-service/issues/28))
* Add "How to join" and contact emails to status page
* Update registry's manifest to version 6.0.0
* Check public key uniqueness in manifests
* Add support for admin provider field to Manifests Overview page
* Update schema versions

* Updated dependencies:
    * Spring Boot (1.5.22 -> 2.1.18)
    * JGit (4.11.9 -> 6.7.0)
    * Guava (30.1.1 -> 32.1.2)
    * commons-io (2.7 -> 2.13.0)
    * ewp-registry-client (to 1.9.1)
    * JUnit 4 to 5
    * jQuery (3.2.1 -> 3.7.1)
    * node (8.11.1 -> 20.6.1)
    * Vue (2.6.10  -> 3.3.4)
    * pebble (2.6.2 -> 3.1.0)
    * All explicitly used maven plugins are updated to the newest versions (Docker,
    Jar, Compiler, Surefire, Frontend, PMD, Jaxb2, Findbugs to the newest Spotbugs)

1.10.0
-----

*Released on 2022-06-23*

* Updated catalogue to version 1.5.0.
* Dropped support for Discovery 4.
* Added support for Discovery 6.0.0.
* Updated frontend-maven-plugin (1.6 -> 1.12.1).
* Updated Spring Boot Starter parent (1.5.6 -> 1.5.22).

1.9.0
-----

*Released on 2022-06-01*

* AbstractValidationSuite: added serialVersionUID to InvalidNumberOfApiEntries.
* Updated Jgit dependency (4.4.0 -> 4.11.9).
* Updated BouncyCastle dependency (1.57 -> 1.70).
* Small update of prettytime dependency (4.0.1 -> 4.0.6).
* Updated ewp-registry-client dependecy (1.7.0 -> 1.8.0).
* Removed javatuples test dependency.
* Updated AssertJ dependency (3.5.1 -> 3.22).
* Removed org.json usage.
* Bumped xercesImpl from 2.12.0 to 2.12.2.
* Updated uptimerobot API.
* Changed pages order on index page.
* Show string that was hashed in IIA validator.
* Validated all IIAs in hash validator.
* Added IIA hash validator.
* Fixed schema validator page title.
* Formatted JavaDoc.
* Fixed comment to schema validator view.
* Renamed README-local-registry.md to README.md.
* Fixed #21 minor issues in README-local-registry.md.
* Repo permission unnecessary for Docker access token.
* Fixed typo in the GitHub Docker access token.
* IIA validator should properly handle absent IIA codes.
* IIA validator should expect HTTP 400 for unknown HEI.
* Prevented validator exception when no IIA code.
* Keep HEI filter on coverage matrix.
* Added filtering to coverage matrix page.
* Added link to EUF HEI information page.
* Added HEI search results count.
* Search HEI by SCHAC fragment.
* Added search pattern explanation + minor changes.
* Fixed label for search input.
* Added autofocus to search input.
* Changed workspace for Maven npm plugin.
* Added HEI search page.
* Reformatted UiController according to code style.
* Do not call commit for unchanged manifests.
* Removed locking when retrieving catalogue from repo.
* Use newly released EWP registry client version.
* Made coverage matrix header sticky.
* Removed CNR sends and LA update types from coverage.

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
