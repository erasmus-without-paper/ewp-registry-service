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


Running from a Docker Image
---------------------------

### Pull the image

Currently, we are pushing out latest builds to a public Docker registry on GitHub.

```sh
docker pull ghcr.io/erasmus-without-paper/ewp-registry-service/ewp-registry-service:latest
```

(You will need to repeat this step whenever you want to upgrade too.)


### Prepare the data volume

First, you will need to prepare a directory where you will keep the container's
settings and data. Let's say it's `/var/ewp-registry-service`.

```sh
mkdir /var/ewp-registry-service
```


### Prepare the `manifest-sources.xml` file

```sh
vi /var/ewp-registry-service/manifest-sources.xml
```

The application will load the list of manifest locations from this file. The
file has no namespace nor schema, and its format and location **may change in
the future**. Currently, we expect the file to look like this:

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

It needs to be placed in `/root/repo` directory of the container. It's best if
you prepare it via the container's shell (because its SSH client is configured
to work along with the JGit library we use in the app itself):

```sh
docker run --rm -it --entrypoint=bash -v /var/ewp-registry-service:/root \
    docker.usos.edu.pl:5000/ewp-registry-service
```

You will need to either create an empty Git repository (`git init repo`), or
pull an existing *and trusted* one (`git clone URL repo`).

This repository will be used to keep track of all the changes to the remote
manifest files. In production environment, it will also be pushed onto a
publicly-accessible GitHub fork.

If you intend to set `app.repo.enable-pushing` to `true` (see
`application.properties`), then you will need to perform some additional steps:

 * Your repository needs to have the proper `origin` remote preconfigured, and
   local `origin/master` branch must exist.

 * The `master` branch must exist and needs to be checked out.

 * You should perform some test pulls as pushes in order to make sure that the
   `/root/.ssh` directory contains all the keys and credentials needed to
   execute `git push` in a **non-interactive way**.


### Prepare the `application.properties` file

Your `/var/ewp-registry-service/application.properties` file should look
something like this:

```properties
spring.profiles.active=production
app.admin-emails=admin@example.com, another-admin@example.com
spring.mail.host=123.124.125.126
```

In development, you might want to override some more settings, e.g.

```properties
spring.profiles.active=development
spring.datasource.url=jdbc:h2:mem:testdb
app.admin-emails=developer@example.com
app.root-url=http://localhost:8080
app.repo.enable-pushing=false
spring.mail.test-connection=false

# if running outside of docker
app.repo.path=/users/me/ewp-registry-service/var/repo
```

**Things you should now:**

 * Brief descriptions (and default values) of all these properties can be found
   in the `application.properties` file [here][props]. Depending on your
   environment, you might need to set more properties than the ones mentioned
   above.

 * Use the `development` profile if you want to make sure that the application
   won't send any email notifications to anyone.


### Run the application

The container will be serving a HTTP service at port 8080.

In production, you will need to wrap the service up in HTTPS, e.g. by
forwarding it to nginx. You will also probably want to make use of docker's
`--restart=always` argument.

In development, you may start it directly:

```sh
docker run --rm -it -v /var/ewp-registry-service:/root -p 80:8080 \
    docker.usos.edu.pl:5000/ewp-registry-service
```


Running outside of docker
-------------------------

### Build the jar

First, build the application jar. In the root directory of the checked-out
project, do this:

```sh
mvn package
```

If you're having problems then you might try adding `-DskipTests` argument.
Application jar should be stored to `target/ewp-registry-<version>.jar`.
For development purposes you can also save some time by adding
`-Dmaven.javadoc.skip`, which will skip Javadoc generation.


### Prepare the environment

The `application.properties` file should be saved in the `.` directory. You
will needed to fill it out in a similar way as it has been described above for
Docker, but you will need to provide more data (as the defaults are set up for
Docker runs).


### Run the application

Web server is embedded in the jar, so you run it like this:

```sh
java -jar target/ewp-registry-<version>.jar
```


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
[github]: https://github.com
[generate-github-access-token]: https://github.com/settings/tokens
[props]: https://github.com/erasmus-without-paper/ewp-registry-service/blob/master/src/main/resources/application.properties
