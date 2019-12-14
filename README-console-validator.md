This JAR file is a standalone API Validator.
You can use it for testing if APIs implemented by EWP Network participant is compliant with the EWP Specification.

There is an online version of this API Validator, which serves the same purpose. It can be found on the DEV EWP Registry website: https://dev-registry.erasmuswithoutpaper.eu/status

In the online version, everyone can test public APIs, such as Echo or Institutions, however, access to private data available in some APIs is limited.

In the standalone version,  you can perform tests using your HEI's identity and access private data from your partners' APIs.


# How to use the Standalone API Validator?
1) Obtain private key used by your Host which public counterpart is published in the manifest. More on that later, in `Providing Private Keys and Certificates` section.
2) Get URL of the manifest which describes the Host you want to validate, DEV EWP Registry website might be helpful.
3) Read the help message, run the API Validator with the `--help` parameter. It lists all possible arguments that the Validator accepts. It explains how to provide keys and certificates, select APIs to test, security policy to use. We will show some examples later.
4) Run validation tests. A summary will be printed to the console and a full report will be saved to HTML file. A single run can generate more than one file - their names are printed to the console along with the summaries. Filename format is always `test-result-<api>-<version>-<security>-<UTC date and time>.html`. Open the files in the browser.
5) Read the results, all requests and responses are available there for easier reproduction of errors (click `Show/hide details`).
The Validator tries to come up with requests that will give some meaningful results, but it might sometimes fail and you would need to guide it by providing `parameters`. For instance, you can modify the `hei_id` parameter used in requests. All `parameters` are listed in the help message.


# Providing Private Keys and Certificates
For TLS communication you should provide a Key Store with a private key and a corresponding certificate, we support Java `JKS` format and open `PKCS#12` format.
For HTTPSig communication server and client private keys can be provided in a Key Store or PEM file.

You can use keytool or openssl tools to create, browse or change the format of Key Stores. There is also a GUI application available - KeyStore Explorer.
Those tools are available for Linux, macOS, and Windows.
Key Store is always protected by a password, you need to know it.
Keys and Certificates stored in Key Stores are referred to using aliases, you need to know what is an alias of your key/certificate in your Key Store. If you don't know it, then listing its contents should give you some information.


# Running the validator
In the following examples, we assume that JAR with the validator is called `ewp-validator.jar`. Bash syntax is used for break-lines (`\`), used parameters are explained below examples.
Please note that every parameter is preceded by a double dash (`--`) and values are assigned using an equality sign (`=`) after the parameter name, without whitespaces.

#### Run validator in an interactive mode - you will be asked to select APIs and securities or to provide parameters.
```
java -jar ewp-validator.jar \
  --tls-keystore=keystore.jks --tls-keystore-alias=alias --tls-keystore-password=pass \
  --http-client-use-tls-key --http-server-use-client-key \
  --manifest=https://ewp.demo.usos.edu.pl/ewp/manifest
```

```
# Select KeyStore with TLS private key and certificate
--tls-keystore=keystore.jks --tls-keystore-alias=alias --tls-keystore-password=pass

# We will use TLS Private Key as HTTPSig Client Key and HTTPSig Client Key as HTTPSig Server Key
--http-client-use-tls-key --http-server-use-client-key

# Address of the manifest which describes the Host you want to test
--manifest=https://ewp.demo.usos.edu.pl/ewp/manifest
```

#### Run validator in an interactive mode with HTTPSig keys read from PEM files.
```
java -jar ewp-validator.jar \
  --http-client-keystore=keystore.jks --http-client-keystore-alias=alias --http-client-keystore-password=pass \
  --http-client-pem=client_private_key.pem --http-server-pem=server_private_key.pem \
  --manifest=https://ewp.demo.usos.edu.pl/ewp/manifest
```

```
# HTTPSig keys will be read from PEM files
--http-client-pem=client_private_key.pem --http-server-pem=server_private_key.pem
```

#### Select APIs to test using parameters - you will be asked to select security and optionally provide parameters.
```
java -jar ewp-validator.jar \
  --tls-keystore=keystore.jks --tls-keystore-alias=alias --tls-keystore-password=pass \
  --http-client-use-tls-key --http-server-use-client-key \
  --manifest=https://ewp.demo.usos.edu.pl/ewp/manifest \
  --api=echo --api=courses
```

```
# Run tests for echo and courses APIs
--api=echo --api=courses
```

#### Run tests for all APIs using HTTT security - you will be asked for parameters.
```
java -jar ewp-validator.jar \
  --tls-keystore=keystore.jks --tls-keystore-alias=alias --tls-keystore-password=pass \
  --http-client-use-tls-key --http-server-use-client-key \
  --manifest=https://ewp.demo.usos.edu.pl/ewp/manifest \
  --api=all \
  --security=HTTT
```

```
# Use HTTT security in all tests
--security=HTTT
```

#### Run tests for all APIs using SHTT security and use default parameters - you won't be asked for anything.

```
java -jar ewp-validator.jar \
  --tls-keystore=keystore.jks --tls-keystore-alias=alias --tls-keystore-password=pass \
  --http-client-use-tls-key --http-server-use-client-key \
  --manifest=https://ewp.demo.usos.edu.pl/ewp/manifest \
  --api=all \
  --security=SHTT \
  --use-default-parameters
```

```
# Default parameters will be used
--use-default-parameters
```

#### Run tests for all APIs using HHTT security and use default parameters, but override hei_id parameter to HEI1 in all tests.

```
java -jar ewp-validator.jar \
  --tls-keystore=keystore.jks --tls-keystore-alias=alias --tls-keystore-password=pass \
  --http-client-use-tls-key --http-server-use-client-key \
  --manifest=https://ewp.demo.usos.edu.pl/ewp/manifest \
  --api=all \
  --security=HHTT \
  --use-default-parameters \
  --hei_id=HEI1
```

```
# Overrides default parameters, uses 'HEI1' as hei_id parameter wherever possible
--hei_id=HEI1
```

#### Run tests for all APIs using HHTT security and use default parameters, but override hei_id parameter to HEI1 in courses tests.

```
java -jar ewp-validator.jar \
  --tls-keystore=keystore.jks --tls-keystore-alias=alias --tls-keystore-password=pass \
  --http-client-use-tls-key --http-server-use-client-key \
  --manifest=https://ewp.demo.usos.edu.pl/ewp/manifest \
  --api=all \
  --security=HHTT \
  --use-default-parameters \
  --courses:hei_id=HEI1
```

```
# Overrides default parameters, uses 'HEI1' as hei_id parameter in courses tests
--courses:hei_id=HEI1
```

#### Run tests for all APIs using HHTT security and use default parameters, but override hei_id parameter to HEI1 in courses and override hei_id to HEI2 in all others

```
java -jar ewp-validator.jar \
  --tls-keystore=keystore.jks --tls-keystore-alias=alias --tls-keystore-password=pass \
  --http-client-use-tls-key --http-server-use-client-key \
  --manifest=https://ewp.demo.usos.edu.pl/ewp/manifest \
  --api=all \
  --security=HHTT \
  --use-default-parameters \
  --courses:hei_id=HEI1 \
  --hei_id=HEI2
```


### Use secondary key to test if the partner obeys confidentiality rules
To run some tests we need an additional private key. We would like to check if the tested host doesn't show private data to unauthorised partner in the network. To do that we need additional private key that is registered in the EWP network, but does not cover data of the requested hei id. Such a key can be generated for you and added to the network by EWP Administrator. You might also create new host entry in your manifest that doesn't cover any hei id and use its private key. The secondary private key must be provided as Key Store.

```
java -jar ewp-validator.jar \
  --tls-keystore=keystore.jks --tls-keystore-alias=alias --tls-keystore-password=pass \
  --http-client-use-tls-key --http-server-use-client-key \
  --manifest=https://ewp.demo.usos.edu.pl/ewp/manifest \
  --api=all --security=HHTT --use-default-parameters \
  --confidentiality-tls-keystore=other_keystore.jks --confidentiality-tls-keystore-alias=other --confidentiality-tls-keystore-password=pass
```

```
# Use other_keystore.jks as a key store with credentials for confidentiality tests
--confidentiality-tls-keystore=other_keystore.jks --confidentiality-tls-keystore-alias=other --confidentiality-tls-keystore-password=pass
```
