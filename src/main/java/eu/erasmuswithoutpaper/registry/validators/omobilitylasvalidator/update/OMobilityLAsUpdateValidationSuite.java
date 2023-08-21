package eu.erasmuswithoutpaper.registry.validators.omobilitylasvalidator.update;

import java.util.Arrays;
import java.util.stream.Collectors;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import eu.erasmuswithoutpaper.registry.validators.AbstractValidationSuite;
import eu.erasmuswithoutpaper.registry.validators.ApiEndpoint;
import eu.erasmuswithoutpaper.registry.validators.ApiValidator;
import eu.erasmuswithoutpaper.registry.validators.Combination;
import eu.erasmuswithoutpaper.registry.validators.ValidatedApiInfo;
import eu.erasmuswithoutpaper.registry.validators.ValidationStepWithStatus;
import eu.erasmuswithoutpaper.registry.validators.omobilitylasvalidator.OMobilityLAsGetValidatedApiInfo;
import eu.erasmuswithoutpaper.registry.validators.omobilitylasvalidator.OMobilityLAsSuiteState;
import eu.erasmuswithoutpaper.registry.validators.types.ApproveProposalV1;
import eu.erasmuswithoutpaper.registry.validators.types.CommentProposalV1;
import eu.erasmuswithoutpaper.registry.validators.types.OmobilityLasUpdateRequest;
import eu.erasmuswithoutpaper.registry.validators.verifiers.VerifierFactory;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import https.github_com.erasmus_without_paper.ewp_specs_api_omobility_las.blob.stable_v1.endpoints.get_response.Signature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

/**
 * Describes the set of test/steps to be run on an OMobilities API index endpoint implementation
 * in order to properly validate it.
 */
class OMobilityLAsUpdateValidationSuite
    extends AbstractValidationSuite<OMobilityLAsSuiteState> {
  private static final Logger logger =
      LoggerFactory.getLogger(
          OMobilityLAsUpdateValidationSuite.class);
  private final ValidatedApiInfo apiInfo;
  private VerifierFactory updateResponseVerifierFactory = new VerifierFactory(Arrays.asList());

  OMobilityLAsUpdateValidationSuite(ApiValidator<OMobilityLAsSuiteState> validator,
                                      OMobilityLAsSuiteState state, ValidationSuiteConfig config,
                                      int version) {
    super(validator, state, config);

    this.apiInfo = new OMobilityLAsGetValidatedApiInfo(version, ApiEndpoint.Update);
  }

  @Override
  protected Logger getLogger() {
    return logger;
  }

  @Override
  public ValidatedApiInfo getApiInfo() {
    return apiInfo;
  }

  @Override
  protected void validateCombinationGet(Combination combination)
      throws SuiteBroken {
    this.addAndRun(
        false,
        this.createHttpMethodValidationStep(combination.withChangedHttpMethod("GET"))
    );
  }

  @Override
  protected void validateCombinationAny(Combination combination) throws SuiteBroken {
    // not used
  }

  private XmlParameters requestToXmlParameters(OmobilityLasUpdateRequest request) {
    return new XmlParameters(requestToDocument(request));
  }

  private Document requestToDocument(OmobilityLasUpdateRequest request) {
    try {
      // Create the Document
      DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
      DocumentBuilder documentBuilder = documentBuilderFactory.newDocumentBuilder();
      Document document = documentBuilder.newDocument();

      JAXBContext jaxbContext = JAXBContext.newInstance(OmobilityLasUpdateRequest.class);

      // Marshal the Object to a Document
      Marshaller marshaller = jaxbContext.createMarshaller();
      marshaller.marshal(request, document);
      return document;
    } catch (JAXBException | ParserConfigurationException e) {
      throw new RuntimeException(e);
    }
  }

  private OmobilityLasUpdateRequest makeRequest(String sendingHeiId,
      CommentProposalV1 commentProposalV1) {
    return makeRequest(sendingHeiId, null, commentProposalV1);
  }

  private OmobilityLasUpdateRequest makeRequest(String sendingHeiId,
      ApproveProposalV1 approveProposalV1) {
    return makeRequest(sendingHeiId, approveProposalV1, null);
  }

  private OmobilityLasUpdateRequest makeRequest(String sendingHeiId,
      ApproveProposalV1 approveProposalV1,
      CommentProposalV1 commentProposalV1) {
    OmobilityLasUpdateRequest request = new OmobilityLasUpdateRequest();
    request.setSendingHeiId(sendingHeiId);
    request.setApproveProposalV1(approveProposalV1);
    request.setCommentProposalV1(commentProposalV1);
    return request;
  }

  private ApproveProposalV1 makeApproveProposalV1(
      String omobilityId, String changesProposalId) {
    ApproveProposalV1 approveProposalV1 =
        new ApproveProposalV1();
    approveProposalV1.setChangesProposalId(changesProposalId);
    approveProposalV1.setOmobilityId(omobilityId);
    approveProposalV1.setSignature(getSignature());
    return approveProposalV1;
  }

  private Signature getSignature() {
    Signature signature = new Signature();
    signature.setSignerName("Paweł Tomasz Kowalski");
    signature.setSignerPosition("Mobility coordinator");
    signature.setSignerEmail("pawel.kowalski@example.com");
    try {
      signature.setTimestamp(DatatypeFactory.newInstance()
          .newXMLGregorianCalendar(2000, 1, 1, 0, 0, 0, 0, 0));
    } catch (DatatypeConfigurationException e) {
      // Shouldn't happen
      assert false;
      return null;
    }
    signature.setSignerApp("USOS");
    return signature;
  }

  private CommentProposalV1 makeCommentProposalV1(String omobilityId,
      String changesProposalId) {
    CommentProposalV1 commentProposalV1 =
        new CommentProposalV1();
    commentProposalV1.setChangesProposalId(changesProposalId);
    commentProposalV1.setOmobilityId(omobilityId);
    commentProposalV1.setSignature(getSignature());
    commentProposalV1.setComment("test");
    return commentProposalV1;
  }

  private Document cloneDocument(Document document) {
    DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
    DocumentBuilder db = null;
    try {
      db = dbf.newDocumentBuilder();
    } catch (ParserConfigurationException e) {
      throw new RuntimeException(e);
    }

    Document copiedDocument = db.newDocument();
    Node copiedRoot = copiedDocument.importNode(document.getDocumentElement(), true);
    copiedDocument.appendChild(copiedRoot);
    return copiedDocument;
  }

  private Node selectNode(Document document, String[] xpathSelectorParts) {
    String selector = "/" + Arrays.stream(xpathSelectorParts)
        .map(s -> String.format("*[local-name() = '%s']", s))
        .collect(Collectors.joining("/"));
    XPathFactory xpathFactory = XPathFactory.newInstance();
    XPath xpath = xpathFactory.newXPath();
    try {
      return (Node) xpath.evaluate(selector, document, XPathConstants.NODE);
    } catch (XPathExpressionException e) {
      throw new RuntimeException(e);
    }
  }

  @SuppressFBWarnings("UPM_UNCALLED_PRIVATE_METHOD")
  private XmlParameters addEmptyNode(XmlParameters parameters, String name,
      String... xpathSelector) {
    Document copiedDocument = cloneDocument(parameters.getXmlBody());
    Node selectedNode = selectNode(copiedDocument, xpathSelector);
    selectedNode.appendChild(copiedDocument.createElement(name));
    return new XmlParameters(copiedDocument);
  }

  private XmlParameters removeNode(XmlParameters parameters, String... xpathSelector) {
    Document copiedDocument = cloneDocument(parameters.getXmlBody());
    Node selectedNode = selectNode(copiedDocument, xpathSelector);
    selectedNode.getParentNode().removeChild(selectedNode);
    return new XmlParameters(copiedDocument);
  }

  //FindBugs is not smart enough to infer that actual type of this.currentState
  //is OMobilityLAsSuiteState not just SuiteState
  @SuppressFBWarnings("BC_UNCONFIRMED_CAST")
  @Override
  protected void validateCombinationPost(Combination combination)
      throws SuiteBroken {
    testParametersError(combination,
        "Send request with unknown omobility-id and changes-proposal id, without type, expect 400.",
        requestToXmlParameters(makeRequest(this.currentState.sendingHeiId, null, null)),
        400
    );

    testParametersError(combination,
        "Send request with known omobility-id and changes-proposal id, but without type "
            + "element, expect 400.",
        requestToXmlParameters(makeRequest(
            this.currentState.sendingHeiId,
            null, null
        )),
        400
    );

    testParametersError(combination,
        "Send request with known omobility-id and changes-proposal id, but with unknown type "
            + "element, expect 400.",
        addEmptyNode(
            requestToXmlParameters(makeRequest(
                this.currentState.sendingHeiId,
                null, null
            )),
            "test",
            "omobility-las-update-request"
        ),
        400
    );

    approveProposalV1Tests(combination);
    commentProposalV1Tests(combination);
  }

  @SuppressFBWarnings("BC_UNCONFIRMED_CAST")
  private void commentProposalV1Tests(Combination combination) throws SuiteBroken {
    RequestFactory requestFactory = (sendingHeiId, omobilityId, changesProposalId) -> makeRequest(
        sendingHeiId,
        makeCommentProposalV1(omobilityId, changesProposalId)
    );
    commonRequestTypeTests(combination, requestFactory, "comment-proposal-v1");
  }

  @SuppressFBWarnings("BC_UNCONFIRMED_CAST")
  private void approveProposalV1Tests(Combination combination) throws SuiteBroken {
    RequestFactory requestFactory = (sendingHeiId, omobilityId, changesProposalId) -> makeRequest(
        sendingHeiId,
        makeApproveProposalV1(omobilityId, changesProposalId)
    );
    commonRequestTypeTests(combination, requestFactory, "approve-proposal-v1");
  }

  @SuppressFBWarnings("BC_UNCONFIRMED_CAST")
  private void commonRequestTypeTests(Combination combination, RequestFactory requestFactory,
      String updateTypeName) throws SuiteBroken {

    testParameters200(combination,
        String.format(
            "Send %s request with known omobility-id and changes-proposal id, expect 200.",
            updateTypeName
        ),
        requestToXmlParameters(requestFactory.createRequest(
            this.currentState.sendingHeiId,
            this.currentState.omobilityId,
            this.currentState.changesProposalId
        )),
        updateResponseVerifierFactory.expectCorrectResponse()
    );

    testParametersError(combination,
        String.format(
            "Send %s request with unknown omobility-id and known changes-proposal id, expect 400.",
            updateTypeName
        ),
        requestToXmlParameters(requestFactory.createRequest(
            this.currentState.sendingHeiId,
            fakeId,
            this.currentState.changesProposalId
        )),
        400
    );

    testParametersError(combination,
        String.format(
            "Send %s request with known omobility-id and unknown changes-proposal id, expect 409.",
            updateTypeName
        ),
        requestToXmlParameters(requestFactory.createRequest(
            this.currentState.sendingHeiId,
            this.currentState.omobilityId,
            fakeId
        )),
        409
    );

    testParametersError(combination,
        String.format(
            "Send %s request with known omobility-id and changes-proposal id, but missing"
                    + " signature, expect 400.",
            updateTypeName
        ),
        removeNode(
            requestToXmlParameters(requestFactory.createRequest(
                this.currentState.sendingHeiId,
                this.currentState.omobilityId,
                this.currentState.changesProposalId
            )),
            "omobility-las-update-request", updateTypeName, "signature"
        ),
        400
    );

    testParameters200(combination,
        String.format(
            "Send %s request with known omobility-id and changes-proposal id and additional "
                + "elements in request, expect 200.",
            updateTypeName
        ),
        addEmptyNode(
            requestToXmlParameters(requestFactory.createRequest(
                this.currentState.sendingHeiId,
                this.currentState.omobilityId,
                this.currentState.changesProposalId
            )),
            "test",
            "omobility-las-update-request", updateTypeName
        ),
        updateResponseVerifierFactory.expectCorrectResponse()
    );

    testParametersErrorAsOtherEwpParticipant(combination,
        String.format(
            "Send %s request with known omobility-id and changes-proposal id as other EWP "
                + "participant, expect 400.",
            updateTypeName
        ),
        requestToXmlParameters(requestFactory.createRequest(
            this.currentState.sendingHeiId,
            this.currentState.omobilityId,
            this.currentState.changesProposalId
        )),
        400,
        ValidationStepWithStatus.Status.FAILURE
    );
  }

  interface RequestFactory {
    OmobilityLasUpdateRequest createRequest(String sendingHeiId, String omobilityId,
        String changesProposalId);
  }

}
