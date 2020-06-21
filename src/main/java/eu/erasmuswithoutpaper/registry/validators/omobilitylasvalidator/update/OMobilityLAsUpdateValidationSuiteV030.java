package eu.erasmuswithoutpaper.registry.validators.omobilitylasvalidator.update;

import java.util.Arrays;
import java.util.stream.Collectors;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import eu.erasmuswithoutpaper.registry.validators.AbstractValidationSuite;
import eu.erasmuswithoutpaper.registry.validators.ApiValidator;
import eu.erasmuswithoutpaper.registry.validators.Combination;
import eu.erasmuswithoutpaper.registry.validators.ValidatedApiInfo;
import eu.erasmuswithoutpaper.registry.validators.ValidationStepWithStatus;
import eu.erasmuswithoutpaper.registry.validators.omobilitylasvalidator.OMobilityLAsSuiteState;
import eu.erasmuswithoutpaper.registry.validators.types.ApproveComponentsStudiedProposalV1;
import eu.erasmuswithoutpaper.registry.validators.types.ApprovingParty;
import eu.erasmuswithoutpaper.registry.validators.types.OmobilityLasUpdateRequest;
import eu.erasmuswithoutpaper.registry.validators.types.UpdateComponentsStudiedV1;
import eu.erasmuswithoutpaper.registry.validators.verifiers.VerifierFactory;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

/**
 * Describes the set of test/steps to be run on an OMobilities API index endpoint implementation
 * in order to properly validate it.
 */
class OMobilityLAsUpdateValidationSuiteV030
    extends AbstractValidationSuite<OMobilityLAsSuiteState> {
  private static final Logger logger =
      LoggerFactory.getLogger(
          OMobilityLAsUpdateValidationSuiteV030.class);
  private static final ValidatedApiInfo apiInfo = new OMobilityLAsUpdateValidatedApiInfo();
  private VerifierFactory updateResponseVerifierFactory = new VerifierFactory(Arrays.asList());

  OMobilityLAsUpdateValidationSuiteV030(ApiValidator<OMobilityLAsSuiteState> validator,
      OMobilityLAsSuiteState state, ValidationSuiteConfig config) {
    super(validator, state, config);
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
      UpdateComponentsStudiedV1 updateComponentsStudiedV1) {
    return makeRequest(sendingHeiId, null, updateComponentsStudiedV1);
  }

  private OmobilityLasUpdateRequest makeRequest(String sendingHeiId,
      ApproveComponentsStudiedProposalV1 approveComponentsStudiedProposalV1) {
    return makeRequest(sendingHeiId, approveComponentsStudiedProposalV1, null);
  }

  private OmobilityLasUpdateRequest makeRequest(String sendingHeiId,
      ApproveComponentsStudiedProposalV1 approveComponentsStudiedProposalV1,
      UpdateComponentsStudiedV1 updateComponentsStudiedV1) {
    OmobilityLasUpdateRequest request = new OmobilityLasUpdateRequest();
    request.setSendingHeiId(sendingHeiId);
    request.setApproveComponentsStudiedProposalV1(approveComponentsStudiedProposalV1);
    request.setUpdateComponentsStudiedV1(updateComponentsStudiedV1);
    return request;
  }

  private ApproveComponentsStudiedProposalV1 makeApproveComponentsStudiedProposalV1(
      String omobilityId, String latestProposalId) {
    ApproveComponentsStudiedProposalV1 approveComponentsStudiedProposalV1 =
        new ApproveComponentsStudiedProposalV1();
    approveComponentsStudiedProposalV1.setLatestProposalId(latestProposalId);
    approveComponentsStudiedProposalV1.setOmobilityId(omobilityId);
    approveComponentsStudiedProposalV1.setApprovingParty(ApprovingParty.RECEIVING_HEI);
    return approveComponentsStudiedProposalV1;
  }

  private UpdateComponentsStudiedV1 makeUpdateComponentsStudiedV1(String omobilityId,
      String latestProposalId) {
    UpdateComponentsStudiedV1 updateComponentsStudiedV1 =
        new UpdateComponentsStudiedV1();
    updateComponentsStudiedV1.setLatestProposalId(latestProposalId);
    updateComponentsStudiedV1.setOmobilityId(omobilityId);
    updateComponentsStudiedV1.setComment("test");
    return updateComponentsStudiedV1;
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
        "Send request with unknown omobility-id and latest-proposal-id, without type, expect 400.",
        requestToXmlParameters(makeRequest(this.currentState.sendingHeiId, null, null)),
        400
    );

    testParametersError(combination,
        "Send request with known omobility-id and latest-proposal-id, but without type "
            + "element, expect 400.",
        requestToXmlParameters(makeRequest(
            this.currentState.sendingHeiId,
            null, null
        )),
        400
    );

    testParametersError(combination,
        "Send request with known omobility-id and latest-proposal-id, but with unknown type "
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

    approveComponentsStudiedProposalV1Tests(combination);
    updateComponentsStudiedV1Tests(combination);
  }

  @SuppressFBWarnings("BC_UNCONFIRMED_CAST")
  private void updateComponentsStudiedV1Tests(Combination combination) throws SuiteBroken {
    final boolean supported = this.currentState.supportsUpdateComponentsStudiedV1;
    final String updateTypeName = "update-components-studied-v1";
    final String elementToRemove = "comment";
    RequestFactory requestFactory = (sendingHeiId, omobilityId, latestProposalId) -> makeRequest(
        sendingHeiId,
        makeUpdateComponentsStudiedV1(omobilityId, latestProposalId)
    );
    commonRequestTypeTests(combination, requestFactory, supported, updateTypeName, elementToRemove);
  }

  @SuppressFBWarnings("BC_UNCONFIRMED_CAST")
  private void approveComponentsStudiedProposalV1Tests(Combination combination) throws SuiteBroken {
    final boolean supported = this.currentState.supportsApproveComponentsStudiedProposalV1;
    final String updateTypeName = "approve-components-studied-proposal-v1";
    final String elementToRemove = "approving-party";
    RequestFactory requestFactory = (sendingHeiId, omobilityId, latestProposalId) -> makeRequest(
        sendingHeiId,
        makeApproveComponentsStudiedProposalV1(omobilityId, latestProposalId)
    );
    commonRequestTypeTests(combination, requestFactory, supported, updateTypeName, elementToRemove);
  }

  @SuppressFBWarnings("BC_UNCONFIRMED_CAST")
  private void commonRequestTypeTests(Combination combination, RequestFactory requestFactory,
      boolean supported, String updateTypeName, String elementToRemove)
      throws SuiteBroken {

    final String supportedSkipReason = updateTypeName + " is supported.";
    final String notSupportedSkipReason = updateTypeName + " is not supported.";

    testParametersError(combination,
        String.format(
            "Send %s request, which is unsupported, expect 400.",
            updateTypeName
        ),
        requestToXmlParameters(requestFactory.createRequest(
            this.currentState.sendingHeiId,
            this.currentState.omobilityId,
            this.currentState.latestProposalId
        )),
        400,
        supported,
        supportedSkipReason
    );

    testParameters200(combination,
        String.format(
            "Send %s request with known omobility-id and latest-proposal-id, expect 200.",
            updateTypeName
        ),
        requestToXmlParameters(requestFactory.createRequest(
            this.currentState.sendingHeiId,
            this.currentState.omobilityId,
            this.currentState.latestProposalId
        )),
        updateResponseVerifierFactory.expectCorrectResponse(),
        !supported,
        notSupportedSkipReason
    );

    testParametersError(combination,
        String.format(
            "Send %s request with unknown omobility-id and known latest-proposal-id, expect 400.",
            updateTypeName
        ),
        requestToXmlParameters(requestFactory.createRequest(
            this.currentState.sendingHeiId,
            fakeId,
            this.currentState.latestProposalId
        )),
        400,
        !supported,
        notSupportedSkipReason
    );

    testParametersError(combination,
        String.format(
            "Send %s request with known omobility-id and unknown latest-proposal-id, expect 409.",
            updateTypeName
        ),
        requestToXmlParameters(requestFactory.createRequest(
            this.currentState.sendingHeiId,
            this.currentState.omobilityId,
            fakeId
        )),
        409,
        !supported,
        notSupportedSkipReason
    );

    testParametersError(combination,
        String.format(
            "Send %s request with known omobility-id and latest-proposal-id, but missing request "
                + "elements, expect 400.",
            updateTypeName
        ),
        removeNode(
            requestToXmlParameters(requestFactory.createRequest(
                this.currentState.sendingHeiId,
                this.currentState.omobilityId,
                this.currentState.latestProposalId
            )),
            "omobility-las-update-request", updateTypeName, elementToRemove
        ),
        400,
        !supported,
        notSupportedSkipReason
    );

    testParameters200(combination,
        String.format(
            "Send %s request with known omobility-id and latest-proposal-id and additional "
                + "elements in request, expect 200.",
            updateTypeName
        ),
        addEmptyNode(
            requestToXmlParameters(requestFactory.createRequest(
                this.currentState.sendingHeiId,
                this.currentState.omobilityId,
                this.currentState.latestProposalId
            )),
            "test",
            "omobility-las-update-request", updateTypeName
        ),
        updateResponseVerifierFactory.expectCorrectResponse(),
        !supported,
        notSupportedSkipReason
    );

    testParametersErrorAsOtherEwpParticipant(combination,
        String.format(
            "Send %s request with known omobility-id and latest-proposal-id as other EWP "
                + "participant, expect 400.",
            updateTypeName
        ),
        requestToXmlParameters(requestFactory.createRequest(
            this.currentState.sendingHeiId,
            this.currentState.omobilityId,
            this.currentState.latestProposalId
        )),
        400,
        ValidationStepWithStatus.Status.FAILURE,
        !supported,
        notSupportedSkipReason
    );
  }

  interface RequestFactory {
    OmobilityLasUpdateRequest createRequest(String sendingHeiId, String omobilityId,
        String latestProposalId);
  }

}
