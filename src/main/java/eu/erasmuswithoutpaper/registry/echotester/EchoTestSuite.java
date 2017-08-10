package eu.erasmuswithoutpaper.registry.echotester;

import static org.joox.JOOX.$;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import eu.erasmuswithoutpaper.registry.documentbuilder.BuildError;
import eu.erasmuswithoutpaper.registry.documentbuilder.BuildParams;
import eu.erasmuswithoutpaper.registry.documentbuilder.BuildResult;
import eu.erasmuswithoutpaper.registry.documentbuilder.EwpDocBuilder;
import eu.erasmuswithoutpaper.registry.documentbuilder.KnownElement;
import eu.erasmuswithoutpaper.registry.documentbuilder.KnownNamespace;
import eu.erasmuswithoutpaper.registry.echotester.EchoTest.Failure;
import eu.erasmuswithoutpaper.registry.echotester.EchoTestResult.Status;
import eu.erasmuswithoutpaper.registry.internet.Internet;
import eu.erasmuswithoutpaper.registry.internet.Internet.Request;
import eu.erasmuswithoutpaper.registry.internet.Internet.Response;

import org.joox.Match;

class EchoTestSuite {

  /**
   * This {@link EchoTestResult} is dynamically added when some unexpected runtime exception occurs.
   */
  private static final class OtherErrorTestResult implements EchoTestResult {

    private final RuntimeException cause;

    public OtherErrorTestResult(RuntimeException cause) {
      this.cause = cause;
    }

    @Override
    public String getMessage() {
      return this.cause.getMessage();
    }

    @Override
    public String getName() {
      return "Other error occurred. Please contact the developers.";
    }

    @Override
    public Status getStatus() {
      return Status.ERROR;
    }
  }

  /**
   * Thrown when a test fails so badly, that no other tests are supposed to be run.
   */
  @SuppressWarnings("serial")
  private static class SuiteBroken extends Exception {
  }

  private final EchoTester echoTester;
  private final String urlStr;
  private final List<EchoTestResult> tests;
  private final EwpDocBuilder docBuilder;
  private final Internet internet;

  EchoTestSuite(EchoTester echoTester, EwpDocBuilder docBuilder, Internet internet, String urlStr) {
    this.echoTester = echoTester;
    this.urlStr = urlStr;
    this.tests = new ArrayList<>();
    this.docBuilder = docBuilder;
    this.internet = internet;
  }

  /**
   * Add the test to the public list of tests and run it.
   *
   * @param requireSuccess If true, then a {@link SuiteBroken} exception will be raised on test
   *        failure.
   * @param test The test to be run.
   * @throws SuiteBroken If a test, which was required to succeed, fails.
   */
  private void addAndRun(boolean requireSuccess, EchoTest test) throws SuiteBroken {
    this.tests.add(test);
    Status status = test.run();
    if (requireSuccess && !status.equals(Status.SUCCESS)) {
      throw new SuiteBroken();
    }
  }

  /**
   * Make the request and make sure that the response contains an error.
   *
   * @param request The request to be made.
   * @param status Expected HTTP response status.
   * @throws Failure If HTTP status differs from expected, or if the response body doesn't contain a
   *         proper error response.
   */
  private void assertError(Request request, int status) throws Failure {
    try {
      Response response = this.internet.makeRequest(request);
      if (response.getStatus() != status) {
        int gotFirstDigit = response.getStatus() / 100;
        int expectedFirstDigit = status / 100;
        Status failureStatus =
            (gotFirstDigit == expectedFirstDigit) ? Status.WARNING : Status.FAILURE;
        throw new Failure(
            "HTTP " + status + " expected, but HTTP " + response.getStatus() + " received.",
            failureStatus);
      }
      BuildParams params = new BuildParams(response.getBody());
      params.setExpectedKnownElement(KnownElement.COMMON_ERROR_RESPONSE);
      BuildResult result = this.docBuilder.build(params);
      if (!result.isValid()) {
        throw new Failure(
            "HTTP response status was okay, but the content has failed Schema validation. "
                + "It is recommended to return a proper <error-response> in case of errors. "
                + this.formatDocBuildErrors(result.getErrors()),
            Status.WARNING);
      }
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * Make the request and make sure that the response contains a valid HTTP 200 Echo API response.
   *
   * @param request The request to be made.
   * @param heiIdsExpected The expected contents of the hei-id list.
   * @param echoValuesExpected The expected contents of the echo list.
   * @throws Failure If some expectations are not met.
   */
  private void assertHttp200(Request request, List<String> heiIdsExpected,
      List<String> echoValuesExpected) throws Failure {
    try {
      Response response = this.internet.makeRequest(request);
      if (response.getStatus() != 200) {
        StringBuilder sb = new StringBuilder();
        sb.append("HTTP 200 expected, but HTTP " + response.getStatus() + " received.");
        if (response.getStatus() == 403) {
          sb.append(" Make sure you validate TLS client certificates against a fresh "
              + "Registry catalogue version.");
        }
        throw new Failure(sb.toString());
      }
      BuildParams params = new BuildParams(response.getBody());
      params.setExpectedKnownElement(KnownElement.RESPONSE_ECHO_V1);
      BuildResult result = this.docBuilder.build(params);
      if (!result.isValid()) {
        throw new Failure(
            "HTTP response status was okay, but the content has failed Schema validation. "
                + this.formatDocBuildErrors(result.getErrors()));
      }
      Match root = $(result.getDocument().get()).namespaces(KnownNamespace.prefixMap());
      List<String> heiIdsGot = new ArrayList<>();
      for (Match entry : root.xpath("er1:hei-id").each()) {
        heiIdsGot.add(entry.text());
      }
      for (String heiIdGot : heiIdsGot) {
        if (!heiIdsExpected.contains(heiIdGot)) {
          throw new Failure(
              "The response has proper HTTP status and it passed the schema validation. However, "
                  + "the set of returned hei-ids doesn't match what we expect. It contains <hei-id>"
                  + heiIdGot + "</hei-id>, but it shouldn't. It should contain the following: "
                  + heiIdsExpected);
        }
      }
      for (String heiIdExpected : heiIdsExpected) {
        if (!heiIdsGot.contains(heiIdExpected)) {
          throw new Failure(
              "The response has proper HTTP status and it passed the schema validation. However, "
                  + "the set of returned hei-ids doesn't match what we expect. "
                  + "It should contain the following: " + heiIdsExpected);
        }
      }
      List<String> echoValuesGot = root.xpath("er1:echo").texts();
      if (!echoValuesGot.equals(echoValuesExpected)) {
        throw new Failure(
            "The response has proper HTTP status and it passed the schema validation. "
                + "However, there's something wrong with the echo values produced. "
                + "We expected the response to contain the following echo values: "
                + echoValuesExpected + ", but the following values were found instead: "
                + echoValuesGot);
      }
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  private String formatDocBuildErrors(List<BuildError> errors) {
    StringBuilder sb = new StringBuilder();
    sb.append("Our document parser has reported the following errors:");
    for (int i = 0; i < errors.size(); i++) {
      sb.append("\n" + (i + 1) + ". ");
      sb.append("(Line " + errors.get(i).getLineNumber() + ") " + errors.get(i).getMessage());
    }
    return sb.toString();
  }

  private EchoTest getRegularMethodTest(String method, boolean expectSuccess) {
    return new EchoTest() {

      @Override
      public String getName() {
        if (expectSuccess) {
          return "Running a regular " + method + " request, with a valid (known) TLS "
              + "client certificate, and without any additional parameters. Expecting to "
              + "receive a valid HTTP 200 Echo API response with proper hei-ids, and "
              + "without any echo values.";
        } else {
          return "Running a " + method + " request. Expecting to receive a valid "
              + "HTTP 405 error response.";
        }
      }

      @Override
      protected void innerRun() throws Failure {
        Internet.Request request = new Internet.Request(method, EchoTestSuite.this.urlStr);
        if (method.equals("POST")) {
          request.addHeader("Content-Type: application/x-www-form-urlencoded");
        }
        request.setClientCertificate(EchoTestSuite.this.echoTester.getTlsClientCertificateInUse(),
            EchoTestSuite.this.echoTester.getTlsKeyPairInUse());
        if (expectSuccess) {
          EchoTestSuite.this.assertHttp200(request,
              EchoTestSuite.this.echoTester.getCoveredHeiIDs(), Collections.<String>emptyList());
        } else {
          EchoTestSuite.this.assertError(request, 405);
        }
      }
    };
  }

  List<EchoTestResult> getResults() {
    return this.tests;
  }

  void run() {

    X509Certificate myCert = this.echoTester.getTlsClientCertificateInUse();
    KeyPair myKeyPair = this.echoTester.getTlsKeyPairInUse();

    try {

      this.addAndRun(false, new EchoTest() {

        @Override
        public String getName() {
          return "Check if our TLS client certificate has been served long enough.";
        }

        @Override
        protected void innerRun() throws Failure {
          if (new Date().getTime()
              - EchoTestSuite.this.echoTester.getTlsClientCertificateUsedSince().getTime() < 10 * 60
                  * 1000) {
            throw new Failure(
                "Our TLS client certificate is quite fresh. This means that many Echo APIs will "
                    + "(correctly) return HTTP 403 responses in places where we expect HTTP 200. "
                    + "This notice will disappear once the certificate is 10 minutes old.",
                Status.NOTICE);
          }
        }
      });

      /////////////////////////////////////////

      this.addAndRun(true, new EchoTest() {

        @Override
        public String getName() {
          return "Verifying the format of the URL. Expecting a valid HTTPS-scheme URL.";
        }

        @Override
        protected void innerRun() throws Failure {
          if (!EchoTestSuite.this.urlStr.startsWith("https://")) {
            throw new Failure("It needs to be HTTPS.");
          }
          try {
            new URL(EchoTestSuite.this.urlStr);
          } catch (MalformedURLException e) {
            throw new Failure("Exception while parsing URL format: " + e);
          }
        }
      });

      /////////////////////////////////////////

      this.addAndRun(false, new EchoTest() {

        @Override
        public String getName() {
          return "Accessing your Echo API without any TLS client certificate. "
              + "Expecting to receive a valid HTTP 403 error response.";
        }

        @Override
        protected void innerRun() throws Failure {
          Request request = new Request("GET", EchoTestSuite.this.urlStr);
          EchoTestSuite.this.assertError(request, 403);
        }
      });

      /////////////////////////////////////////

      this.addAndRun(false, new EchoTest() {

        @Override
        public String getName() {
          return "Accessing your Echo API with an unknown TLS client certificate "
              + "(a random one, that has never been published in the Registry). "
              + "Expecting to receive a valid HTTP 403 error response.";
        }

        @Override
        protected void innerRun() throws Failure {
          KeyPair otherKeyPair = EchoTestSuite.this.echoTester.generateKeyPair();
          X509Certificate otherCert =
              EchoTestSuite.this.echoTester.generateCertificate(otherKeyPair);
          Request request = new Request("GET", EchoTestSuite.this.urlStr);
          request.setClientCertificate(otherCert, otherKeyPair);
          EchoTestSuite.this.assertError(request, 403);
        }
      });

      /////////////////////////////////////////

      this.addAndRun(false, this.getRegularMethodTest("GET", true));

      /////////////////////////////////////////

      this.addAndRun(false, this.getRegularMethodTest("POST", true));

      /////////////////////////////////////////

      this.addAndRun(false, this.getRegularMethodTest("PUT", false));

      /////////////////////////////////////////

      this.addAndRun(false, this.getRegularMethodTest("DELETE", false));

      /////////////////////////////////////////

      this.addAndRun(false, new EchoTest() {

        @Override
        public String getName() {
          return "Running a GET request with a list of echo values. Expecting to receive "
              + "a valid HTTP 200 Echo API response, with proper hei-id and matching echo values.";
        }

        @Override
        protected void innerRun() throws Failure {
          Request request = new Request("GET", EchoTestSuite.this.urlStr + "?echo=a&echo=b&echo=a");
          request.setClientCertificate(myCert, myKeyPair);
          ArrayList<String> expectedEchoValues = new ArrayList<>();
          expectedEchoValues.add("a");
          expectedEchoValues.add("b");
          expectedEchoValues.add("a");
          EchoTestSuite.this.assertHttp200(request,
              EchoTestSuite.this.echoTester.getCoveredHeiIDs(), expectedEchoValues);
        }
      });

      /////////////////////////////////////////

      this.addAndRun(false, new EchoTest() {

        @Override
        public String getName() {
          return "Run a POST request with a list of echo values. Expecting the same "
              + "response as above.";
        }

        @Override
        protected void innerRun() throws Failure {
          Request request = new Request("POST", EchoTestSuite.this.urlStr);
          request.addHeader("Content-Type: application/x-www-form-urlencoded");
          request.setBody("echo=a&echo=b&echo=a".getBytes(StandardCharsets.UTF_8));
          request.setClientCertificate(myCert, myKeyPair);
          ArrayList<String> expectedEchoValues = new ArrayList<>();
          expectedEchoValues.add("a");
          expectedEchoValues.add("b");
          expectedEchoValues.add("a");
          EchoTestSuite.this.assertHttp200(request,
              EchoTestSuite.this.echoTester.getCoveredHeiIDs(), expectedEchoValues);
        }
      });

      /////////////////////////////////////////

    } catch (SuiteBroken e) {
      // Ignore.
    } catch (RuntimeException e) {
      this.tests.add(new OtherErrorTestResult(e));
    }
  }

}
