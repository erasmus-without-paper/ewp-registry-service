EWP Registry Service
====================

[This repository][self] (link to self) contains the **implementation** of the
[EWP Registry Service][registry-service]. This means **the server part**.

We invite everyone to inspect the code, but we expect that most developers will
rather want to **use** the Registry Service, and won't be interested in how it
is implemented. If you are one of them, then you should visit one the following
pages instead:

 * [EWP Registry Client][registry-client],
 * [Registry API specification][registry-api].


Running the Registry Service
----------------------------

### Build the jar

First, build the application jar. In the root directory of the checked-out
project, do this:

```sh
mvn package
```

If you're having problems then you might try adding `-DskipTests` argument.
Application jar should be stored to `target/ewp-registry-<version>.jar`.


### Prepare the `manifest-sources.xml` file

The application will load the list of manifest locations from this file. The
file has no namespace nor schema, and its format **will change in the future**.
Currently, we expect the file to look like this:

```xml
<manifest-sources>
    <source>
        <location>https://example.com/manifest1.xml</location>
        <hei-regex>^uw\.edu\.pl$</hei-regex>
    </source>
    <source>
        <location>https://example.com/manifest2.xml</location>
        <hei-regex>^.+\.se$</hei-regex> <!-- this constraint is optional -->
    </source>
    ...
</manifest-sources>
```


### Prepare the Git repository

You will need to create an empty (`git init`) repository somewhere (e.g. in
`/var/ewp-registry-service/repo` directory). This repository will be used to
keep track of all the changes to the remote manifest files. In production
environment, it will also be pushed onto a publicly-accessible GitHub fork.

If you intend to set `app.repo.enable-pushing` to `true`, then your repository
should have `master` and `origin/master` branches, and your `.ssh` directory
should contain keys which should enable you to execute `git push` in a
non-interactive way.


### Prepare the `application.properties` file

There are [many ways][spring-config] to pass parameters to the application, but
putting them into the `application.properties` file seems the easiest. The file
should reside in the same directory in which you will be executing your jar.

Your `application.properties` file will look like this:

```properties
spring.profile.active=production (OR development)

app.repo.path=/var/ewp-registry-service/repo
app.manifest-sources.url=file:///var/ewp-registry-service/manifest-sources.xml
spring.datasource.url=jdbc:h2:/var/ewp-registry-service/db

app.admin-emails=email,email,email
...
```

**Things you should now:**

 * Brief descriptions (and default values) of all these properties can be found
   in the `application.properties` file [here][props]. Note, that these default
   values can get [overridden][spring-config] by *other* "less default" values,
   based on the value of `spring.profiles.active` property - if you set it to
   `development`, then default values of other properties will be loaded from
   [application-development.properties][devel-props] instead of
   [application.properties][props].

 * You might need to set more properties than the ones above, depending on your
   environment. The opposite is also true, for example, it is not required to
   set `spring.datasource.url` nor `app.admin-emails` if
   `spring.profiles.active` is set to `development`.

 * Use the `development` profile if you want to make sure that the application
   won't send any email notifications to anyone.


### Run the application

```sh
java -jar target/ewp-registry-<version>.jar
```

The `application.properties` file should be saved in the `.` directory.


Versioning strategy
-------------------

As opposed to the [Registry Client][registry-client], this project is *not*
a library, and is not supposed to be *included* in other projects, so this
section is only informative.

We will use [semantic versioning](http://semver.org/) (`MAJOR.MINOR.PATCH`) in
our releases:

 * **Major version** will be incremented if we decide to
   break backward compatibility in the supported APIs, e.g. drop support for a
   deprecated version of the Registry API. (We do not intend for this to
   happen, especially after the Registry Service is started to be used in
   production.)

 * **Minor version** will be incremented when new API features are added (e.g.
   support for a new - either major or minor - version of the Registry API is
   added), or important GUI features are either added or dropped.

 * **Patch version** will be incremented on bug fixes, and other minor changes.


[self]: https://github.com/erasmus-without-paper/ewp-registry-service
[registry-service]: https://registry.erasmuswithoutpaper.eu/
[registry-api]: https://github.com/erasmus-without-paper/ewp-specs-api-registry
[registry-client]: https://github.com/erasmus-without-paper/ewp-registry-client
[props]: https://github.com/erasmus-without-paper/ewp-registry-service/blob/master/src/main/resources/application.properties
[devel-props]: https://github.com/erasmus-without-paper/ewp-registry-service/blob/master/src/main/resources/application-development.properties
[spring-config]: http://docs.spring.io/spring-boot/docs/current/reference/html/boot-features-external-config.html
