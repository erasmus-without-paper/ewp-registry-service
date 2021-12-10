## How to run the Registry locally?
We've created bash scripts to help you. You will need docker and git.

You need to create an [GitHub][github] account, [generate access token][generate-github-access-token] with `read:packages` scopes, and then login to docker.pkg.github.com in docker.
```bash
docker login docker.pkg.github.com --username <your username>
<enter your GitHub access token when prompted for password>
```

Then, to pull image use:
```bash
docker pull docker.pkg.github.com/erasmus-without-paper/ewp-registry-service/ewp-registry-service:latest
```
Then, call `./setup.sh <https URL of your manifest file>` script, it will create required directories and files.
Next, run `./run_local_docker.sh`. It will start local registry on `localhost:8080`.

If you are not using a Linux machine then consult [Running from a Docker Image section of our registry's README.md][running-from-docker], you can use application.properties file found in this directory.


## Test scenarios
We present two possible scenarios how you can test your implementation using the local registry.

The validator will pretend that it is one of your partners and it will send certain requests to your installation. It will check how you handle them and give you feedback if there's anything you could improve.


### Prerequirements
You need to have some data in your database, you can e.g. copy them from your production environment or generate them.
If you want to test Institutions API then add some institutions. If you want to test IIAs API then add some agreements to you system.

Please note that your manifests and your APIs should be available using *HTTPS protocol*, that is required by the specification.
If you don't have a place where you could host your development manifests you can consider using websites such as [pastebin.com][pastebin] or [GitHub gists][gists]. If you use these websites always provide links for *raw* version of files.


#### What to do when you don't have HTTPS support?
If you don't have HTTPS support, e.g. because you are running on a local machine, you can try using [stunnel][stunnel] or [openssl s\_server][openssl_s_server] or other available tools to proxy requests. You might also check if your framework supports SSL configuration, Spring, for instance, does.
You will have to generate a self-signed certificate (for `localhost`). In keys directory you can find key and certificate for `localhost` used by the local registry, you can use them. They are provided as a PKCS #12 KeyStore with the default password (`changeit`) and as separate .pem files for certificate and private key without password.
Use it for your SSL proxy (e.g. stunnel).
Then you have to pass it through to local registry - copy .pem file with a certificate to `./registry_data/keys/certs_to_trust` directory.


This is an example command that you can use on Linux machine, file names and paths may differ in your case.
```bash
# Generate a certificate file.
# When prompted for Common Name use "localhost" (without quotes).
# If you access your installation using a different domain name, then use it instead of localhost.
# Do not include the port number.
# Other parameters do not matter.
openssl req -x509 -newkey rsa:4096 -keyout my-private-key.pem -out my-certificate.pem -days 365

# Copy my-certificate.pem file to a directory from which the registry will be able to read it.
# It'll be added to list of registry's trusted certificates automatically.
cp my-certificate.pem ./registry_data/keys/certs_to_trust

# Run the registry using docker.
./run_local_docker.sh
```

You should also add the registry's certificate to your trusted certs.
It depends on system and distribution where you should copy ./keys/cert.pem file and how to add it to trusted hosts.

If you want to use Java tools (e.g. standalone validator) then you should add it to your Java's trust store.
This command should work on most Linux installations.
```
# keytool and cacerts file come with Java, you have to install it.
keytool -import -file ./keys/cert.pem -keystore $JAVA_HOME/jre/lib/security/cacerts -alias my-local-registry-key
```

#### Changing registry port
Registry uses port 8080 by default, but you can change it in `application.properties` file. Just change `server.port` property to any other free port number. Remember to use it consistently in all scenarios.
If you've already run `./setup.sh` then you'll need to run it again.
If you've already run `./setup.sh` then you might want to change that value in `./registry_data/application.properties` file. You won't need to run `./setup.sh` again, but beware that changes in that file will be overridden on next `./setup.sh` run.


### Scenario 1: Using console validator
1. Select a partner that the validator will pretend to be. If you want to test private APIs then you should have some data that this partner can access. We strongly recommend to select a real partner and use real (preferably anonymized) data, they will be more meaningful than artificially generated data. Let's call this partner *P*.
2. Generate key pairs for *P*, they will be used by the console validator to make requests as *P*.
3. Create a new manifest file for *P*.
   It should contain:
   - public keys from key pairs generated in step 2;
   - *P* on `institutions covered` list.
   You don't need to specify any APIs.
   Host it in a place where it will be available to the local registry and which supports *HTTPS* connection.
4. Create your manifest file. If you are testing publicly available installation then you can use its manifest. If you are testing local installation then use `localhost:port` as your installation address. Remember that *HTTPS* communication should be available on `localhost:port` and a registry should trust its certificate.
5. Configure your local installation to communicate with the local registry, it's available on localhost:\<registry-port\>.
6. Run `./setup.sh <URL of your manifest> <URL of manifest for P>`.
7. Run `./run_local_docker.sh`.
8. Use the console validator. Pass keys and certs generated in step 2. Also, use `--registry-domain` parameter to point to your local registry: `--registry-domain=localhost:<registry-port>`.


### Scenario 2: Using web validator
1. Select a partner that the validator will pretend to be. If you want to test private APIs then you should have some data that this partner can access. We strongly recommend to select a real partner and use real (preferably anonymized) data, they will be more meaningful than artificially generated data. Let's call this partner *P*.
2. Add `app.local-registry.additional-hei-ids=*P*` to `./application.properties`. This can be a comma-separated list or a single hei\_id.
3. Run `./setup.sh <URL of your manifest>`.
4. Run `./run_local_docker.sh`.
5. Configure your local installation to communicate with the local registry, it's available on localhost:\<registry-port\>.
6. Use registry webpage `https://localhost:<registry-port>` to run your tests. You will need to add ./keys/cert.pem to list of trusted certificates or add browser exception.


[github]: https://github.com
[generate-github-access-token]: https://github.com/settings/tokens
[running-from-docker]: https://github.com/erasmus-without-paper/ewp-registry-service/blob/master/README.md#running-from-a-docker-image
[pastebin]: https://pastebin.com/
[gists]: https://gist.github.com/
[stunnel]: https://www.stunnel.org/
[openssl_s_server]: https://www.openssl.org/docs/man1.0.2/man1/openssl-s_server.html
