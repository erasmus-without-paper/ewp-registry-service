Release notes
=============

Note, that this changelog describes only the changes relevant from the
perspective of the Registry API client implementers, and users which view the
public Registry Service site. Most changes made to the Registry Service touch
the *unofficial* services - such as the document validator, or Echo API
validator - but these changes are currently not mentioned in this changelog, to
avoid clutter.


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
