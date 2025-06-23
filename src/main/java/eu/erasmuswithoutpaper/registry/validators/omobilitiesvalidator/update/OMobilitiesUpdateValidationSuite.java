package eu.erasmuswithoutpaper.registry.validators.omobilitiesvalidator.update;

import java.util.List;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import eu.erasmuswithoutpaper.registry.validators.AbstractValidationSuite;
import eu.erasmuswithoutpaper.registry.validators.ApiEndpoint;
import eu.erasmuswithoutpaper.registry.validators.ApiValidator;
import eu.erasmuswithoutpaper.registry.validators.Combination;
import eu.erasmuswithoutpaper.registry.validators.ValidatedApiInfo;
import eu.erasmuswithoutpaper.registry.validators.omobilitiesvalidator.OMobilitiesSuiteState;
import eu.erasmuswithoutpaper.registry.validators.omobilitiesvalidator.OMobilitiesValidatedApiInfo;
import eu.erasmuswithoutpaper.registry.validators.verifiers.VerifierFactory;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import https.github_com.erasmus_without_paper.ewp_specs_api_omobilities.blob.stable_v3.endpoints.update_request.ApproveProposalV1;
import https.github_com.erasmus_without_paper.ewp_specs_api_omobilities.blob.stable_v3.endpoints.update_request.OmobilitiesUpdateRequest;
import https.github_com.erasmus_without_paper.ewp_specs_api_omobilities.blob.stable_v3.endpoints.update_request.RejectProposalV1;
import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.Marshaller;
import org.w3c.dom.Document;

class OMobilitiesUpdateValidationSuite extends AbstractValidationSuite<OMobilitiesSuiteState> {
  private final ValidatedApiInfo apiInfo;
  private final VerifierFactory updateResponseVerifierFactory = new VerifierFactory(List.of());

  OMobilitiesUpdateValidationSuite(ApiValidator<OMobilitiesSuiteState> validator,
      OMobilitiesSuiteState state, ValidationSuiteConfig config, int version) {
    super(validator, state, config);

    this.apiInfo = new OMobilitiesValidatedApiInfo(version, ApiEndpoint.UPDATE);
  }

  @Override
  public ValidatedApiInfo getApiInfo() {
    return apiInfo;
  }

  @Override
  protected void validateCombinationGet(Combination combination) throws SuiteBroken {
    this.addAndRun(false,
        this.createHttpMethodValidationStep(combination.withChangedHttpMethod("GET")));
  }

  @Override
  protected void validateCombinationAny(Combination combination) throws SuiteBroken {
    // not used
  }

  private XmlParameters requestToXmlParameters(OmobilitiesUpdateRequest request) {
    return new XmlParameters(requestToDocument(request));
  }

  private Document requestToDocument(OmobilitiesUpdateRequest request) {
    try {
      DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
      DocumentBuilder documentBuilder = documentBuilderFactory.newDocumentBuilder();
      Document document = documentBuilder.newDocument();

      JAXBContext jaxbContext = JAXBContext.newInstance(OmobilitiesUpdateRequest.class);

      Marshaller marshaller = jaxbContext.createMarshaller();
      marshaller.marshal(request, document);
      return document;
    } catch (JAXBException | ParserConfigurationException e) {
      throw new RuntimeException(e);
    }
  }

  private OmobilitiesUpdateRequest makeRequest(RejectProposalV1 rejectProposalV1) {
    return makeRequest(null, rejectProposalV1);
  }

  private OmobilitiesUpdateRequest makeRequest(ApproveProposalV1 approveProposalV1) {
    return makeRequest(approveProposalV1, null);
  }

  private OmobilitiesUpdateRequest makeRequest(ApproveProposalV1 approveProposalV1,
      RejectProposalV1 rejectProposalV1) {
    OmobilitiesUpdateRequest request = new OmobilitiesUpdateRequest();
    request.setApproveProposalV1(approveProposalV1);
    request.setRejectProposalV1(rejectProposalV1);
    return request;
  }

  private ApproveProposalV1 makeApproveProposalV1(String omobilityId, String changesProposalId) {
    ApproveProposalV1 approveProposalV1 = new ApproveProposalV1();
    approveProposalV1.setProposalId(changesProposalId);
    approveProposalV1.setOmobilityId(omobilityId);
    return approveProposalV1;
  }

  private RejectProposalV1 makeRejectProposalV1(String omobilityId, String changesProposalId) {
    RejectProposalV1 rejectProposalV1 = new RejectProposalV1();
    rejectProposalV1.setProposalId(changesProposalId);
    rejectProposalV1.setOmobilityId(omobilityId);
    rejectProposalV1.setComment("test");
    return rejectProposalV1;
  }

  // FindBugs is not smart enough to infer that actual type of this.currentState
  // is OMobilitiesSuiteState not just SuiteState
  @SuppressFBWarnings("BC_UNCONFIRMED_CAST")
  @Override
  protected void validateCombinationPost(Combination combination) throws SuiteBroken {
    approveProposalV1Tests(combination);
    rejectProposalV1Tests(combination);
  }

  @SuppressFBWarnings("BC_UNCONFIRMED_CAST")
  private void rejectProposalV1Tests(Combination combination) throws SuiteBroken {
    RequestFactory requestFactory = (omobilityId,
        changesProposalId) -> makeRequest(makeRejectProposalV1(omobilityId, changesProposalId));
    commonRequestTypeTests(combination, requestFactory, "comment-proposal-v1");
  }

  @SuppressFBWarnings("BC_UNCONFIRMED_CAST")
  private void approveProposalV1Tests(Combination combination) throws SuiteBroken {
    RequestFactory requestFactory = (omobilityId,
        changesProposalId) -> makeRequest(makeApproveProposalV1(omobilityId, changesProposalId));
    commonRequestTypeTests(combination, requestFactory, "approve-proposal-v1");
  }

  @SuppressFBWarnings("BC_UNCONFIRMED_CAST")
  private void commonRequestTypeTests(Combination combination, RequestFactory requestFactory,
      String updateTypeName) throws SuiteBroken {

    testParameters200(combination,
        String.format(
            "Send %s request with known omobility-id and changes-proposal id, expect 200.",
            updateTypeName),
        requestToXmlParameters(requestFactory.createRequest(this.currentState.omobilityId,
            this.currentState.proposalId)),
        updateResponseVerifierFactory.expectCorrectResponse());

    testParametersError(combination,
        String.format(
            "Send %s request with unknown omobility-id and known changes-proposal id, expect 400.",
            updateTypeName),
        requestToXmlParameters(requestFactory.createRequest(FAKE_ID, this.currentState.proposalId)),
        400);

    testParametersError(combination,
        String.format(
            "Send %s request with known omobility-id and unknown changes-proposal id, expect 409.",
            updateTypeName),
        requestToXmlParameters(
            requestFactory.createRequest(this.currentState.omobilityId, FAKE_ID)),
        409);
  }

  interface RequestFactory {
    OmobilitiesUpdateRequest createRequest(String omobilityId, String changesProposalId);
  }
}
