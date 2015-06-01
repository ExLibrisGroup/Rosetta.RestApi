package com.exlibris.dps.api.rosetta;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;
import java.util.Random;
import java.util.Set;
import java.util.StringTokenizer;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.xml.namespace.NamespaceContext;
import javax.xml.namespace.QName;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.cxf.jaxrs.model.wadl.Description;
import org.apache.cxf.jaxrs.model.wadl.Descriptions;
import org.apache.cxf.jaxrs.model.wadl.DocTarget;
import org.apache.xmlbeans.XmlException;
import org.jboss.resteasy.util.Base64;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.exlibris.core.infra.common.exceptions.logging.ExLogger;
import com.exlibris.core.infra.svc.api.xml.XmlUtil;
import com.exlibris.digitool.deposit.service.xmlbeans.DepositDataDocument;
import com.exlibris.digitool.deposit.service.xmlbeans.DepositDataDocument.DepositData;
import com.exlibris.digitool.deposit.service.xmlbeans.DepositResultDocument;
import com.exlibris.digitool.deposit.service.xmlbeans.DepositResultDocument.DepositResult;
import com.exlibris.dps.DepositWebServices;
import com.exlibris.dps.DepositWebServices_Service;
import com.exlibris.dps.IEWSException_Exception;
import com.exlibris.dps.IEWebServices;
import com.exlibris.dps.IEWebServices_Service;
import com.exlibris.dps.InvalidMIDException_Exception;
import com.exlibris.dps.InvalidXmlException_Exception;
import com.exlibris.dps.LockedIeException_Exception;
import com.exlibris.dps.MetaData;
import com.exlibris.dps.Operation;
import com.exlibris.dps.ProducerWebServices;
import com.exlibris.dps.ProducerWebServices_Service;
import com.exlibris.dps.RepresentationContent;
import com.exlibris.dps.SipStatusInfo;
import com.exlibris.dps.SipWebServices;
import com.exlibris.dps.SipWebServices_Service;
import com.exlibris.dps.UserAuthorizeException;
import com.exlibris.dps.UserAuthorizeException_Exception;
import com.exlibris.dps.sdk.pds.PdsClient;

import eu.scape_project.model.LifecycleState;
import eu.scape_project.model.LifecycleState.State;
@Path("")
public class RosettaRestWS
{
	private static final String CONFIG_PROPERTIES_FILE = "conf.props";
	private static final String IE_PID_PREFIX = "IE";
	private static final long NO_FILES_FLAG = 2L;
	private static final long INCLUDE_FILES_FLAG = 0L;
	private static final String INTERNAL_IDENTIFIER_TYPE = "<key id=\"internalIdentifierType\">SIPID</key>";
	private static final String INTERNAL_IDENTIFIER_VALUE = "<key id=\"internalIdentifierValue\">";
	private static final String DEPOSIT_FOLDER_NAME_PREFIX = "RestAPI";
	private static final String CONTENT_FOLDER_NAME = "content";
	private static final String MD_SUBTYPE_INDICATOR = "MDTYPE";
	public static final String ROSETTA_METS_SCHEMA = "http://www.exlibrisgroup.com/xsd/dps/rosettaMets";
	public static final String METS_SCHEMA = "http://www.loc.gov/METS/";
	public static final String XML_SCHEMA = "http://www.w3.org/2001/XMLSchema-instance";
	public static final String XML_SCHEMA_REPLACEMENT = "http://www.exlibrisgroup.com/XMLSchema-instance";

	private static ExLogger log = ExLogger.getExLogger(RosettaRestWS.class);
	private static Properties properties = null;
	private static SipWebServices sipWebServices = null;
	private static IEWebServices ieWebServices = null;
	private static DepositWebServices depositWebServices = null;
	private static ProducerWebServices producerWebServices = null;
	private static PdsClient pdsClient = null;
	private static int innerDepId = 0;


	@GET
	@Path("file/{entity-id}/{rep-id}/{file-id}/{version-id}")
	@Produces(MediaType.TEXT_XML)
	@Descriptions({
		@Description(value = "Not implemented", title = "Get File", target = DocTarget.METHOD)
	})
	public Response getFile(@PathParam("entity-id") String entityId, @PathParam("rep-id") String repId,
			@PathParam("file-id") String fileId, @PathParam("version-id") String versionId, @HeaderParam("Authorization")String authInfo, @QueryParam("inst")String inst) {
		return Response.status(Status.NOT_IMPLEMENTED).entity("Please omit the version, as only the latest can be fetched").build();
	}

	@GET
	@Path("file/{entity-id}/{rep-id}")
	@Produces(MediaType.TEXT_XML)
	@Descriptions({
		@Description(value = "This Web Service gets a file by Representation id", title = "Get File", target = DocTarget.METHOD)
	})
	public Response getFile(@PathParam("entity-id") String entityId, @PathParam("rep-id") String repId,
			@HeaderParam("Authorization")String authInfo, @QueryParam("inst")String inst) {
		return getFile("", "", repId, authInfo, inst);
	}

	@GET
	@Path("file/{entity-id}")
	@Produces(MediaType.TEXT_XML)
	@Descriptions({
		@Description(value = "This Web Service gets a file by IE id", title = "Get File", target = DocTarget.METHOD)
	})
	public Response getFile(@PathParam("entity-id") String entityId, @HeaderParam("Authorization")String authInfo, @QueryParam("inst")String inst) {
		return getFile("", "", entityId, authInfo, inst);
	}

	@GET
	@Path("file/{entity-id}/{rep-id}/{file-id}")
	@Produces(MediaType.TEXT_XML)
	@Descriptions({
		@Description(value = "This Web Service gets a file by File id", title = "Get File", target = DocTarget.METHOD)
	})
	public Response getFile(@PathParam("entity-id") String entityId, @PathParam("rep-id") String repId,
			@PathParam("file-id") String fileId, @HeaderParam("Authorization")String authInfo, @QueryParam("inst")String inst) {
		try {
			String pdsHandle = loginWithPDS(authInfo, inst);
			String delBasePath = getProperties().getProperty("DELIVERY_PATH");
			String delUrl = delBasePath + "/DeliveryManagerServlet?dps_pid="+fileId+"&dps_custom_att_1=staff&change_lng=en&pds_handle="+pdsHandle+"&dps_func=stream";
			URI uri = new URI(delUrl);
			return Response.temporaryRedirect(uri).build(); // build the response
		} catch (UserAuthorizeException_Exception e) {
			log.error(e);
			return Response.status(Status.UNAUTHORIZED).entity(e.getMessage()).build();
		} catch (Exception e) {
			log.error(e);
			return Response.status(Status.NOT_FOUND).entity(e.getMessage()).build();
		}
	}

	@POST
	@Path("representation/{entity-id}")
	@Consumes(MediaType.TEXT_XML)
	@Descriptions({
		@Description(value = "Not implemented", title = "Update Representation", target = DocTarget.METHOD)
	})
	public Response updateRepresentation(String entity,@PathParam("entity-id") String entityId, @HeaderParam("Authorization")String authInfo, @QueryParam("inst")String inst) {
		return Response.status(Status.NOT_IMPLEMENTED).entity("Request must include IE pid and Rperesentation pid.").build();
	}

	@GET
	@Path("representation/{entity-id}")
	@Produces(MediaType.TEXT_XML)
	@Descriptions({
		@Description(value = "This Web Service returns the fileGRPs MD of the selected representation", title = "Get Descriptive MD", target = DocTarget.METHOD)
	})
	public Response getRepresentation(@PathParam("entity-id") String entityId, @HeaderParam("Authorization")String authInfo, @QueryParam("inst")String inst) {
		try
		{
		String pdsHandle = loginWithPDS(authInfo, inst);
		String ieMets = getIEWebServices().getIE(pdsHandle, entityId, INCLUDE_FILES_FLAG);
		Document doc = getXMLDocument(ieMets);
		String fileGrp = getSection(doc, "//mets//fileSec");

		fileGrp = addNS(fileGrp);

		return Response.status(Status.OK).entity(fileGrp).build();

	} catch (UserAuthorizeException_Exception e) {
		log.error(e);
		return Response.status(Status.UNAUTHORIZED).entity(e.getMessage()).build();
	} catch (Exception e) {
		log.error(e);
		return Response.status(Status.INTERNAL_SERVER_ERROR).entity(e.getMessage()).build();
	}

	}

	@GET
	@Path("representation/{entity-id}/{rep-id}")
	@Produces(MediaType.TEXT_XML)
	@Descriptions({
		@Description(value = "This Web Service returns the fileGRP MD of the selected representation", title = "Get Descriptive MD", target = DocTarget.METHOD)
	})
	public Response getRepresentation(@PathParam("entity-id") String entityId, @PathParam("rep-id") String repId, @HeaderParam("Authorization")String authInfo, @QueryParam("inst")String inst) {
		try
		{
		String pdsHandle = loginWithPDS(authInfo, inst);
		String ieMets = getIEWebServices().getIE(pdsHandle, entityId, INCLUDE_FILES_FLAG);
		Document doc = getXMLDocument(ieMets);
		String fileGrp = getSection(doc, "//fileSec//fileGrp[@ID='"+repId+"']");

		fileGrp = fileGrp.replace("<?xml version=\"1.0\" encoding=\"UTF-8\"?>", "<?xml version=\"1.0\" encoding=\"UTF-8\"?> <mets:fileSec>");
		fileGrp += "</mets:fileSec>";

		fileGrp = addNS(fileGrp);

		return Response.status(Status.OK).entity(fileGrp).build();

	} catch (UserAuthorizeException_Exception e) {
		log.error(e);
		return Response.status(Status.UNAUTHORIZED).entity(e.getMessage()).build();
	} catch (Exception e) {
		log.error(e);
		return Response.status(Status.INTERNAL_SERVER_ERROR).entity(e.getMessage()).build();
	}
	}

	@PUT
	@Path("representation/{entity-id}/{representation-id}")
	@Consumes(MediaType.TEXT_XML)
	@Descriptions({
		@Description(value = "This Web Service updates the selected representation ", title = "Update Representation", target = DocTarget.METHOD)
	})
	public Response updateRepresentation(String entity, @PathParam("entity-id") String entityId, @PathParam("representation-id") String repPid, @HeaderParam("Authorization")String authInfo, @QueryParam("inst")String inst) {
		try {
			String pdsHandle = loginWithPDS(authInfo, inst);
			Set<RepresentationContent> newRCs = new HashSet<RepresentationContent>();
			Set<RepresentationContent> oldRCs = new HashSet<RepresentationContent>();

			//use getIeWebServies().getIE() to get IE's full METS
			String ieMets = getIEWebServices().getIE(pdsHandle, entityId, INCLUDE_FILES_FLAG);
			setRCs(oldRCs,ieMets, repPid);

			setRCs(newRCs,entity, repPid);
			setFileOperations(newRCs,oldRCs);

			List<RepresentationContent> rc = new ArrayList<RepresentationContent>(newRCs);

			getIEWebServices().updateRepresentation(pdsHandle, entityId, repPid, "", rc);
			return Response.status(Status.OK).build();
		} catch (UserAuthorizeException_Exception e) {
			log.error(e);
			return Response.status(Status.UNAUTHORIZED).entity(e.getMessage()).build();
		} catch (LockedIeException_Exception e) {
			log.error(e);
			return Response.status(Status.BAD_REQUEST).entity(e.getMessage()).build();
		} catch (InvalidXmlException_Exception e) {
			log.error(e);
			return Response.status(Status.BAD_REQUEST).entity(e.getMessage()).build();
		} catch (Exception e) {
			log.error(e);
			return Response.status(Status.INTERNAL_SERVER_ERROR).entity(e.getMessage()).build();
		}
	}

	@PUT
	@Path("metadata/{entity-id}/{metadata-id}")
	@Consumes(MediaType.TEXT_XML)
	@Descriptions({
		@Description(value = "This Web Service updates the Metadata of the selected id ", title = "Update Metadata", target = DocTarget.METHOD)
	})
	public Response updateMetadata(String entity, @PathParam("entity-id") String entityId, @PathParam("metadata-id") String mId, @HeaderParam("Authorization")String authInfo, @QueryParam("inst")String inst) {
		try {
			String pdsHandle = loginWithPDS(authInfo, inst);
			ArrayList <MetaData> mds = new ArrayList<MetaData>();
			Document doc = getXMLDocument(entity);
			MetaData dcMd = getMetadata(entity, mId, doc);
			mds.add(dcMd);
			getIEWebServices().updateMD(pdsHandle, entityId, mds);
			return Response.status(Status.OK).build(); // build the response
		} catch (UserAuthorizeException_Exception e) {
			log.error(e);
			return Response.status(Status.UNAUTHORIZED).entity(e.getMessage()).build();
		} catch (InvalidMIDException_Exception e) {
			log.error(e);
			return Response.status(Status.NOT_FOUND).entity(e.getMessage()).build();
		} catch (Exception e) {
			log.error(e);
			return Response.status(Status.INTERNAL_SERVER_ERROR).entity(e.getMessage()).build();
		}
	}

	@GET
	@Path("metadata/{entity-id}/{rep-id}/{file-id}")
	@Produces(MediaType.TEXT_XML)
	@Descriptions({
		@Description(value = "This Web Service returns the Descriptive MD of the selected file", title = "Get Descriptive MD", target = DocTarget.METHOD)
	})
	public Response getDescriptiveMetadata(@PathParam("entity-id") String entityId, @PathParam("rep-id") String repId,
			@PathParam("file-id") String fileId, @HeaderParam("Authorization")String authInfo, @QueryParam("inst")String inst) {
		return getDescriptiveMetadata(fileId, authInfo, inst);
	}
	@GET
	@Path("metadata/{entity-id}/{sub-id}")
	@Produces(MediaType.TEXT_XML)
	@Descriptions({
		@Description(value = "This Web Service returns the Descriptive MD of the selected id", title = "Get Descriptive MD", target = DocTarget.METHOD)
	})
	public Response getDescriptiveMetadata(@PathParam("entity-id") String entityId,
			@PathParam("sub-id") String subId, @HeaderParam("Authorization")String authInfo,@QueryParam("inst")String inst) {
		return getDescriptiveMetadata(subId, authInfo, inst);
	}

	@GET
	@Path("metadata/{entity-id}")
	@Produces(MediaType.TEXT_XML)
	@Descriptions({
		@Description(value = "This Web Service returns the Descriptive MD of the selected IE", title = "Get Descriptive MD", target = DocTarget.METHOD)
	})
	public Response getDescriptiveMetadata(@PathParam("entity-id") String entityId, @HeaderParam("Authorization")String authInfo, @QueryParam("inst")String inst) {

		try {
			String pdsHandle = loginWithPDS(authInfo, inst);
			String ieMD = getIEWebServices().getMD(pdsHandle, entityId);

			return Response.status(Status.OK).entity(ieMD).build(); // build the response

		} catch (UserAuthorizeException_Exception e) {
			log.error(e);
			return Response.status(Status.UNAUTHORIZED).entity(e.getMessage()).build();
		} catch (Exception e) {
			log.error(e);
			return Response.status(Status.NOT_FOUND).entity(e.getMessage()).build();
		}
	}

	@GET
	@Path("lifecycle/{entity-id}")
	@Produces(MediaType.TEXT_XML)
	@Descriptions({
		@Description(value = "This Web Service returns the Sip status of the selected IE", title = "Get Sip status", target = DocTarget.METHOD)
	})
	public Response getSipStatus(@PathParam("entity-id") String entityId, @HeaderParam("Authorization")String authInfo, @QueryParam("inst")String inst) {

		try {
			String pdsHandle = loginWithPDS(authInfo, inst);
			entityId = convertToSIPPID(entityId, pdsHandle);
			SipStatusInfo innerRosettaStatus = getSipWebServices().getSIPStatusInfo(entityId); // get the rosetta status
			LifecycleState sipStatus = null;

			if ("Finished".equals(innerRosettaStatus.getStage())) {
				sipStatus = new LifecycleState("", State.INGESTED);
			}
			else if ("REJECTED".equals(innerRosettaStatus.getStatus()) ||
					"DECLINED".equals(innerRosettaStatus.getStatus()) ||
					"ERROR".equals(innerRosettaStatus.getStatus())) {
				sipStatus = new LifecycleState(innerRosettaStatus.getStage() + ":" + innerRosettaStatus.getStatus(), State.INGEST_FAILED);
			}

			if (sipStatus == null) {
				sipStatus = new LifecycleState(innerRosettaStatus.getStage() + ":" + innerRosettaStatus.getStatus(), State.OTHER); // default
			}
			return Response.status(Status.OK).entity(sipStatus).build(); // build the response
		} catch (UserAuthorizeException_Exception e) {
			log.error(e);
			return Response.status(Status.UNAUTHORIZED).entity(e.getMessage()).build();
		} catch (Exception e) {
			log.error(e);
			return Response.status(Status.NOT_FOUND).entity(e.getMessage()).build();
		}
	}


	@GET
	@Path("entity/{entity-id}")
	@Produces(MediaType.TEXT_XML)
	@Descriptions({
		@Description(value = "This Web Service returns the Latest Mets selected IE", title = "Get Mets", target = DocTarget.METHOD)
	})
	public Response getLatestMETS(@PathParam("entity-id") String entityId, @HeaderParam("Authorization")String authInfo, @QueryParam("inst")String inst) {

		try {
			String pdsHandle = loginWithPDS(authInfo,inst);
			entityId = convertToIEPID(entityId); // entity id may be IE or SIP
			String ieMETS = getIEWebServices().getIE(pdsHandle, entityId, INCLUDE_FILES_FLAG); // get the METS

			return Response.status(Status.OK).entity(ieMETS).build(); // build the response
		} catch (UserAuthorizeException_Exception e) {
			log.error(e);
			return Response.status(Status.UNAUTHORIZED).entity(e.getMessage()).build();
		} catch (Exception e) {
			log.error(e);
			return Response.status(Status.NOT_FOUND).entity(e.getMessage()).build();
		}
	}

	@POST
	@Path("entity-async")
	@Consumes(MediaType.TEXT_PLAIN)
	@Descriptions({
		@Description(value = "This deposits an Entity asynchrony", title = "Deposit Entity", target = DocTarget.METHOD)
	})
	public Response entityAsync(String entity, @HeaderParam("Authorization")String authInfo, @QueryParam("inst")String inst, @QueryParam("MATERIAL_FLOW_ID")String mfId, @QueryParam("PRODUCER_ID")String producerId, @QueryParam("TRANSFORM_XSL")String transformXslParam) {

		try {
			String pdsHandle = loginWithPDS(authInfo, inst);
			entity = transform(entity, transformXslParam);
			String actualFolder = writeMETS(entity);
			DepositResult depositResult = deposit(authInfo, pdsHandle, actualFolder, mfId, producerId);

			if (depositResult.getIsError()) {
				String err = depositResult.getMessageDesc();
				if (!StringUtils.isEmpty(depositResult.getMessageCode())) {
					err+= "(" + depositResult.getMessageCode() + ")";
				}
				return Response.status(Status.BAD_REQUEST).entity(err).build();
			}
			return Response.status(Status.OK).entity(Long.toString(depositResult.getSipId())).build(); // build the response
		} catch (UserAuthorizeException_Exception e) {
			log.error(e);
			return Response.status(Status.UNAUTHORIZED).entity(e.getMessage()).build();
		} catch (Exception e) {
			log.error(e);
			return Response.status(Status.INTERNAL_SERVER_ERROR).entity(e.getMessage()).build();
		}
	}
	private MetaData getMetadata(String entity, String mId, Document doc) throws Exception {
		String record = "";
		String mdType = "";
		String mdWrap = "";
		String mdSubType = "";
		Document mdWr = null;
		if (mId.contains("-dmd")) { //extract dc record:
			mdWrap = getSection(doc,"//dmdSec//mdWrap");
			mdType = "descriptive";
			mId = null;
			mdWr = getXMLDocument(mdWrap);
			record = getSection(mdWr,"//mdWrap//record");
			mdSubType = "dc";
		} else { //existing sourceMD
			mdType = "source";
			mdWrap = getSection(doc, "//sourceMD[@ID='"+mId+"']//mdWrap");
			mdWr = getXMLDocument(mdWrap);
			record = getSection(mdWr,"//mdWrap//xmlData");
			record = record.replace("<mets:xmlData>", "");
			record = record.replace("</mets:xmlData>", "");
			mdSubType = getAttributeBetweenBrackets(mdWrap, MD_SUBTYPE_INDICATOR);
		}

		//get MdSubType

		MetaData md = new MetaData();
		md.setMid(mId);
		md.setContent(record);
		md.setType(mdType);
		md.setSubType(mdSubType);
		return md;
	}

	private String getSection(Document doc, String expression) throws Exception {
		XPath xPath =  XPathFactory.newInstance().newXPath();
		StreamResult result = new StreamResult(new StringWriter());
		Transformer transformer = TransformerFactory.newInstance().newTransformer();
		transformer.setOutputProperty(OutputKeys.INDENT, "yes");

		Node node= (Node) xPath.compile(expression).evaluate(doc, XPathConstants.NODE);

		DOMSource source = new DOMSource(node);
		transformer.transform(source, result);
		return result.getWriter().toString();
	}

	private Document getXMLDocument(String entity) throws Exception {
		DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
		DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
		InputStream is = new ByteArrayInputStream(entity.getBytes("UTF-8"));
		return dBuilder.parse(is);
	}

	private DepositResult deposit(String authInfo, String pdsHandle, String actualFolder, String mfId, String producerId) throws IOException, XmlException {
		String materialFlowId = StringUtils.isEmpty(mfId) ? getProperties().getProperty("MATERIAL_FLOW_ID") : mfId;
		String prodId = StringUtils.isEmpty(producerId) ? getProperties().getProperty("PRODUCER_ID") : producerId;
		String result = getDepositWebServices().submitDepositActivity(pdsHandle,
				materialFlowId,
				actualFolder,
				getProducerID(getUsernameAndPasswordFromHeader(authInfo)[0], prodId),
				null);
		DepositResultDocument depositResultDocument = DepositResultDocument.Factory.parse(result);
		DepositResult depositResult = depositResultDocument.getDepositResult();
		return depositResult;
	}

	private String writeMETS(String entity) throws IOException {
		String actualFolder = DEPOSIT_FOLDER_NAME_PREFIX + getNextInnerDepId();
		String depositPath = getProperties().getProperty("MATERIAL_FLOW_DEPOSIT_FOLDER") +
				File.separator + actualFolder + File.separator + CONTENT_FOLDER_NAME;
		File depositDirectory = new File(depositPath);
		if (!depositDirectory.exists()) {
			log.info("Making directory for deposit: " + depositPath);
			depositDirectory.mkdirs();
		}
		File file = new File(depositPath + File.separator + "ie.xml");
		FileUtils.write(file, entity);
		return actualFolder;
	}

	private String transform(String entity, String transformXslParam) throws IOException {
		String transfXsl = StringUtils.isEmpty(transformXslParam)? getProperties().getProperty("TRANSFORM_XSL") : transformXslParam;
		if (!StringUtils.isEmpty(transfXsl)) {
			InputStream is = Thread.currentThread().getContextClassLoader().getResourceAsStream(getProperties().getProperty("TRANSFORM_XSL"));
			StringWriter writer = new StringWriter();
			IOUtils.copy(is, writer,  "UTF-8");
			String xslFileContent = writer.toString();
			entity = XmlUtil.runXSL(entity, xslFileContent);
		}
		return entity;
	}

	private String getProducerID(String userName, String producerId) throws IOException, XmlException {
		if(StringUtils.isEmpty(producerId)){
			ProducerWebServices producerWebServices = getProducerWebServices();
			String producerAgentId = producerWebServices.getInternalUserIdByExternalId(userName);
			String xmlReply = producerWebServices.getProducersOfProducerAgent(producerAgentId);
			DepositDataDocument depositDataDocument = DepositDataDocument.Factory.parse(xmlReply);
			DepositData depositData = depositDataDocument.getDepositData();

			producerId = depositData.getDepDataArray(0).getId();
		}
		log.info("Producer Id is: " + producerId);
		return producerId;
	}

	private ProducerWebServices getProducerWebServices() throws IOException {
		if (producerWebServices == null) {
			URL urlProdWS = new URL(getProperties().getProperty("PROD_WS_PATH"));
			producerWebServices = new ProducerWebServices_Service(urlProdWS, new QName("http://dps.exlibris.com/", "ProducerWebServices")).getProducerWebServicesPort();
		}
		return producerWebServices;
	}

	private DepositWebServices getDepositWebServices() throws IOException {
		if (depositWebServices == null) {
			URL urlDepWS = new URL(getProperties().getProperty("DEP_WS_PATH"));
			depositWebServices = new DepositWebServices_Service(urlDepWS, new QName("http://dps.exlibris.com/", "DepositWebServices")).getDepositWebServicesPort();
		}
		return depositWebServices;
	}

	private String loginWithPDS(String authInfo, String inst) throws IOException, UserAuthorizeException_Exception {
		String institution = StringUtils.isBlank(inst) ? getProperties().getProperty("DEFAULT_INST") : inst;
		String pdsHandle;
		String[] usernameAndPasswordFromHeader = getUsernameAndPasswordFromHeader(authInfo);
		try {
			pdsHandle = getPdsClient().login(institution, usernameAndPasswordFromHeader[0], usernameAndPasswordFromHeader[1]);
		} catch (Exception e) {
			UserAuthorizeException userAuthorizeException = new UserAuthorizeException();
			userAuthorizeException.setMessage(e.getMessage());
			throw new UserAuthorizeException_Exception(e.getMessage(), userAuthorizeException, e);
		}
		log.trace("pdsHandle is: " + pdsHandle);
		return pdsHandle;
	}

	private PdsClient getPdsClient() throws IOException {
		if (pdsClient == null) {
			pdsClient = PdsClient.getInstance();
			pdsClient.init(getProperties().getProperty("PDS_PATH"), false);
		}
		return pdsClient;
	}

	private IEWebServices getIEWebServices() throws IOException {
		if (ieWebServices == null) {
			URL urlIEWS = new URL(getProperties().getProperty("IE_WS_PATH"));
			ieWebServices = new IEWebServices_Service(urlIEWS, new QName("http://dps.exlibris.com/", "IEWebServices")).getIEWebServicesPort();
		}
		return ieWebServices;
	}

	private String convertToIEPID(String entityId) throws IOException {
		if (!entityId.startsWith(IE_PID_PREFIX)) {
			String allIEs = getSipWebServices().getSipIEs(entityId);
			final StringTokenizer tokenizer = new StringTokenizer(allIEs, ",");
			entityId = tokenizer.nextToken(); // take first IE
		}
		return entityId;
	}

	private String convertToSIPPID(String entityId, String pdsHandle) throws IOException, IEWSException_Exception, UserAuthorizeException_Exception {
		if (entityId.startsWith(IE_PID_PREFIX)) {
			// yes, this is ugly, but we don't have a better external way of getting this
			String ieMETS = getIEWebServices().getIE(pdsHandle, entityId, NO_FILES_FLAG);
			int startOfSipIDDefinition = ieMETS.indexOf(INTERNAL_IDENTIFIER_TYPE);
			int startOfSipIDValue = ieMETS.indexOf(INTERNAL_IDENTIFIER_VALUE, startOfSipIDDefinition) + INTERNAL_IDENTIFIER_VALUE.length();
			int endOfSipIDValue = ieMETS.indexOf("<", startOfSipIDValue);
			entityId = ieMETS.substring(startOfSipIDValue, endOfSipIDValue);
		}
		return entityId;
	}

	private int getNextInnerDepId() throws IOException {
		if (innerDepId == 0) {
			Random rn = new Random();
			innerDepId = rn.nextInt(Integer.MAX_VALUE/2);
		}
		return innerDepId++; // avoid duplicates
	}

	private SipWebServices getSipWebServices() throws IOException {
		if (sipWebServices == null) {
			URL urlSIPWS = new URL(getProperties().getProperty("SIP_WS_PATH"));
			sipWebServices = new SipWebServices_Service(urlSIPWS, new QName("http://dps.exlibris.com/", "SipWebServices")).getSipWebServicesPort();
		}
		return sipWebServices;
	}

	/**
	 * Converts a basic Authorization http header to username & password
	 * @param header the authorization http header containing the username & password
	 * @return an array where the first element is a username and the second is a password
	 * @throws IOException if the header isn't decoded correctly
	 */
	private String[] getUsernameAndPasswordFromHeader(String header) throws IOException {
		String[] uNp = new String[2];
		if (header != null)
		{
			header = header.replaceFirst("Basic ", "");
			String usernameAndPassword = new String(Base64.decode(header));

			// split username and password tokens
			final StringTokenizer tokenizer = new StringTokenizer(usernameAndPassword, ":");
			uNp[0] = tokenizer.nextToken(); // username
			uNp[1] = tokenizer.nextToken(); // password
		}
		return uNp;
	}

	private static Properties getProperties() throws IOException {
		if (properties == null) {
			InputStream in = Thread.currentThread().getContextClassLoader().getResourceAsStream(CONFIG_PROPERTIES_FILE);
			Properties readProps = new Properties();
			try {
				readProps.load(in);
				properties = readProps;
			} finally {
				if (in != null) {
					try {
						in.close();
					} catch (IOException e) {
						log.warn(e);
					}
				}
			}
		}
		return properties;
	}

	private static String processFileSecSection(String fileSec) {
		fileSec = fileSec.replace("<?xml version=\"1.0\" encoding=\"UTF-8\"?>", "<mets:mets>");
		fileSec += "</mets:mets>";
		fileSec = fileSec.replaceFirst("<mets:mets>", "<mets:mets  xmlns:mets=\"http://www.loc.gov/METS/\">");
		return fileSec;
	}

	private void setRCs(Set<RepresentationContent> oldRCs,
			String fileSecEntity, String repPid) throws Exception {
		XPath xPath = getXPathMetsNS();
		String fileGrp = getSectionAsString(xPath, getXMLDocument(fileSecEntity), "//mets//fileGrp[@ID='"+repPid+"']");
		fileGrp = processFileSecSection(fileGrp);

		NodeList files = getFileSectionsList(xPath, getXMLDocument(fileGrp), "//fileGrp//file");
		RepresentationContent rc;
		Element eElement;
		String filePath = "";
		String fileID = "";
		for (int i=0; i<files.getLength(); i++) {
			eElement = (Element) files.item(i);
			fileID = eElement.getAttribute("ID");
			eElement = (Element) xPath.compile("//file//FLocat").evaluate(getXMLDocument(getNodeAsString(files.item(i))), XPathConstants.NODE);
			filePath = eElement.getAttribute("xlin:href");
			if (filePath.isEmpty())
			{
				filePath = eElement.getAttribute("xlink:href");
			}

			rc = new RepresentationContent();
			rc.setOldFilePid(fileID);
			rc.setNewFile(filePath);
			oldRCs.add(rc);
		}
	}

	private String getAttributeBetweenBrackets(String section, String attribute) {
		int idInd = section.indexOf(attribute);
		if (idInd>0) {
			int idStart = section.indexOf("\"",idInd);
			int idEnd = section.indexOf("\"",idStart+1);
			return section.substring(idStart+1, idEnd);
		} else
			return "";
	}

	private void setFileOperations(Set<RepresentationContent> rcs,
			Set<RepresentationContent> oldRCs) throws InvalidXmlException_Exception {
		//if there is no pid -> add file
		//if pid exists in given ie mets && filePath is different -> replace file
		//for all pids in given ie mets NOT IN list of pids here -> remove files
		Set<RepresentationContent> toRemove = new HashSet<RepresentationContent>();

		for (RepresentationContent newRc : rcs) {
			if (StringUtils.isEmpty(newRc.getOldFilePid())) {
				newRc.setOperation(Operation.ADD);
			} else {
				RepresentationContent old = setContains(oldRCs,newRc.getOldFilePid());
				if (old!=null) {
					RepresentationContent newFileUrl = setContainsNewFile(oldRCs,newRc.getNewFile());
					if(newFileUrl!=null) // Don't do anything
					{
						toRemove.add(newRc);
					}
					else
					{
					newRc.setOperation(Operation.REPLACE); //replace file
					}
				} else {
					throw new InvalidXmlException_Exception("File element must not contain ID that is not part of the given entity." , null);//TODO else throw input no good
				}
			}
		}

		for (RepresentationContent old : oldRCs) {
			if (setContains(rcs,old.getOldFilePid())==null) {
				old.setOperation(Operation.REMOVE);
				rcs.add(old);
			}
		}

		rcs.removeAll(toRemove);
	}

	private RepresentationContent setContains(Set<RepresentationContent> oldRCs,
			String oldFilePid) {
		for (RepresentationContent rc : oldRCs) {
			if (rc.getOldFilePid().equals(oldFilePid))
				return rc;
		}
		return null;
	}

	private RepresentationContent setContainsNewFile(Set<RepresentationContent> oldRCs,
			String newFile) {
		for (RepresentationContent rc : oldRCs) {
			if (rc.getNewFile().equals(newFile))
				return rc;
		}
		return null;
	}

	private NodeList getFileSectionsList(XPath xPath, Document doc, String expression) throws Exception {
		return (NodeList) xPath.compile(expression).evaluate(doc, XPathConstants.NODESET);
	}
	private String getSectionAsString(XPath xPath, Document doc, String expression) throws Exception {
		return getNodeAsString((Node) xPath.compile(expression).evaluate(doc, XPathConstants.NODE));
	}

	private String getNodeAsString(Node node) throws Exception {
		StreamResult result = new StreamResult(new StringWriter());
		Transformer transformer = TransformerFactory.newInstance().newTransformer();
		transformer.setOutputProperty(OutputKeys.INDENT, "yes");

		DOMSource source = new DOMSource(node);
		transformer.transform(source, result);
		return result.getWriter().toString();

	}
	private XPath getXPathMetsNS() {
		XPath xPath = XPathFactory.newInstance().newXPath();
		xPath.setNamespaceContext(new NamespaceContext() {
			public String getNamespaceURI(String prefix) {
				return prefix.equals("mets") ? "http://www.loc.gov/METS/" : null;
			}

			public Iterator<?> getPrefixes(String val) {
				return null;
			}

			public String getPrefix(String uri) {
				return null;
			}
		});
		return xPath;
	}

	private String addNS (String entity) {
		entity = entity.replace("<?xml version=\"1.0\" encoding=\"UTF-8\"?>", "<?xml version=\"1.0\" encoding=\"UTF-8\"?> <mets:mets  xmlns:mets=\"http://www.loc.gov/METS/\">");
		entity += "</mets:mets>";
		return entity;

	}

	//=====Unimplemented Methods=====//

	@POST
	@Path("representation")
	@Consumes(MediaType.TEXT_PLAIN)
	@Descriptions({
		@Description(value = "Not implemented", title = "Update Representation", target = DocTarget.METHOD)
	})
	public Response updateRepresentation(String entity) {
		return Response.status(Status.NOT_IMPLEMENTED).entity("Request must include Representation ID.").build();
	}
	@POST
	@Path("metadata")
	@Consumes(MediaType.TEXT_PLAIN)
	@Descriptions({
		@Description(value = "Not implemented", title = "Update Metadata", target = DocTarget.METHOD)
	})
	public Response updateMetadata(String entity, @HeaderParam("Authorization")String authInfo, @QueryParam("inst")String inst) {
		return Response.status(Status.NOT_IMPLEMENTED).entity("Request must include Entity and Metadata IDs.").build();
	}
	@POST
	@Path("metadata/{entity-id}")
	@Consumes(MediaType.TEXT_PLAIN)
	@Descriptions({
		@Description(value = "Not implemented", title = "Update Metadata", target = DocTarget.METHOD)
	})
	public Response updateMetadata(String entity, @PathParam("entity-id") String entityId, @HeaderParam("Authorization")String authInfo, @QueryParam("inst")String inst) {
		return Response.status(Status.NOT_IMPLEMENTED).entity("Request must include Metadata ID.").build();
	}
	@GET
	@Path("metadata/{entity-id}/{rep-id}/{file-id}/{bitstream-id}/{version-id}/{md-id}")
	@Produces("application/xml")
	@Descriptions({
		@Description(value = "Not implemented", title = "Get Descriptive Metadata", target = DocTarget.METHOD)
	})
	public Response getDescriptiveMetadata(@PathParam("entity-id") String entityId, @PathParam("rep-id") String repId,
			@PathParam("file-id") String fileId, @PathParam("bitstream-id") String bitstream,
			@PathParam("version-id") String version, @PathParam("md-id") String mdid, @HeaderParam("Authorization")String authInfo, @QueryParam("inst")String inst) {
		return Response.status(Status.NOT_IMPLEMENTED).entity("Please omit the MdId, as only the entire METS file can be fetched").build();
	}
	@GET
	@Path("metadata/{entity-id}/{rep-id}/{file-id}/{bitstream-id}/{version-id}")
	@Produces("application/xml")
	@Descriptions({
		@Description(value = "Not implemented", title = "Get Descriptive Metadata", target = DocTarget.METHOD)
	})
	public Response getDescriptiveMetadata(@PathParam("entity-id") String entityId, @PathParam("rep-id") String repId,
			@PathParam("file-id") String fileId, @PathParam("bitstream-id") String bitstream,
			@PathParam("version-id") String version, @HeaderParam("Authorization")String authInfo, @QueryParam("inst")String inst) {
		return Response.status(Status.NOT_IMPLEMENTED).entity("Please omit the version, as only the latest can be fetched").build();
	}
	@GET
	@Path("metadata/{entity-id}/{rep-id}/{file-id}/{bitstream-id}")
	@Produces("application/xml")
	@Descriptions({
		@Description(value = "Not implemented", title = "Get Descriptive Metadata", target = DocTarget.METHOD)
	})
	public Response getDescriptiveMetadata(@PathParam("entity-id") String entityId, @PathParam("rep-id") String repId,
			@PathParam("file-id") String fileId, @PathParam("bitstream-id") String bitstream, @HeaderParam("Authorization")String authInfo, @QueryParam("inst")String inst) {
		return Response.status(Status.NOT_IMPLEMENTED).entity("Please omit the bitstream-id, as fetching is done only according to PIDs.").build();
	}
	@POST
	@Path("entity-list")
	@Consumes(MediaType.TEXT_PLAIN)
	@Descriptions({
		@Description(value = "Not implemented", title = "Get List of Entities", target = DocTarget.METHOD)
	})
	public Response getEntityList(@HeaderParam("Authorization")String authInfo, @QueryParam("inst")String inst) {
		return Response.status(Status.NOT_IMPLEMENTED).entity("This method is not implemented.").build();
	}
	@POST
	@Path("entity")
	@Consumes(MediaType.TEXT_PLAIN)
	@Descriptions({
		@Description(value = "Not implemented", title = "Ingest IE", target = DocTarget.METHOD)
	})
	public Response ingestIE(@HeaderParam("Authorization")String authInfo, @QueryParam("inst")String inst) {
		return Response.status(Status.NOT_IMPLEMENTED).entity("This method is not implemented.").build();
	}
	@PUT
	@Path("entity/{id}")
	@Consumes(MediaType.TEXT_PLAIN)
	@Descriptions({
		@Description(value = "Not implemented", title = "Update IE", target = DocTarget.METHOD)
	})
	public Response updateIE(@PathParam("id") String entityId, @HeaderParam("Authorization")String authInfo, @QueryParam("inst")String inst) {
		return Response.status(Status.NOT_IMPLEMENTED).entity("This method is not implemented.").build();
	}
	@GET
	@Path("entity-version-list/{entity-id}")
	@Produces("application/xml")
	@Descriptions({
		@Description(value = "Not implemented", title = "Get IE List by Version", target = DocTarget.METHOD)
	})
	public Response getIEVersionList(@PathParam("entity-id") String entityId) {
		return Response.status(Status.NOT_IMPLEMENTED).entity("This method is not implemented.").build();
	}
	@GET
	@Path("bitstream/{entity-id}/{rep-id}/{file-id}/{bitstream-id}/{version-id}")
	@Produces("application/xml")
	@Descriptions({
		@Description(value = "Not implemented", title = "Get Names of Bit Streams", target = DocTarget.METHOD)
	})
	public Response getNamesBitStreams(@PathParam("entity-id") String entityId3,@PathParam("entity-id") String entityId2,
			@PathParam("entity-id") String entityId4,@PathParam("entity-id") String entityId5,@PathParam("entity-id") String entityId6) {
		return Response.status(Status.NOT_IMPLEMENTED).entity("This method is not implemented.").build();
	}
	@GET
	@Path("bitstream/{entity-id}/{rep-id}/{file-id}/{bitstream-id}")
	@Produces("application/xml")
	@Descriptions({
		@Description(value = "Not implemented", title = "Get Names of Bit Streams", target = DocTarget.METHOD)
	})
	public Response getNamesBitStreams(@PathParam("entity-id") String entityId,@PathParam("entity-id") String entityId2,
			@PathParam("entity-id") String entityId3,@PathParam("entity-id") String entityId4) {
		return Response.status(Status.NOT_IMPLEMENTED).entity("This method is not implemented.").build();
	}
	@GET
	@Path("bitstream/{entity-id}/{rep-id}/{file-id}")
	@Produces("application/xml")
	@Descriptions({
		@Description(value = "Not implemented", title = "Get Names of Bit Streams", target = DocTarget.METHOD)
	})
	public Response getNamesBitStreams(@PathParam("entity-id") String entityId,@PathParam("entity-id") String entityId2,
			@PathParam("entity-id") String entityId3) {
		return Response.status(Status.NOT_IMPLEMENTED).entity("This method is not implemented.").build();
	}
	@GET
	@Path("bitstream/{entity-id}/{rep-id}")
	@Produces("application/xml")
	@Descriptions({
		@Description(value = "Not implemented", title = "Get Names of Bit Streams", target = DocTarget.METHOD)
	})
	public Response getNamesBitStreams(@PathParam("entity-id") String entityId,@PathParam("entity-id") String entityId2) {
		return Response.status(Status.NOT_IMPLEMENTED).entity("This method is not implemented.").build();
	}
	@GET
	@Path("bitstream/{entity-id}")
	@Produces("application/xml")
	@Descriptions({
		@Description(value = "Not implemented", title = "Get Names of Bit Streams", target = DocTarget.METHOD)
	})
	public Response getNamesBitStreams(@PathParam("entity-id") String entityId) {
		return Response.status(Status.NOT_IMPLEMENTED).entity("This method is not implemented.").build();
	}
	@GET
	@Path("bitstream")
	@Produces("application/xml")
	@Descriptions({
		@Description(value = "Not implemented", title = "Get Names of Bit Streams", target = DocTarget.METHOD)
	})
	public Response getNamesBitStreams() {
		return Response.status(Status.NOT_IMPLEMENTED).entity("This method is not implemented.").build();
	}
	@GET
	@Path("sru/entities")
	@Produces("application/xml")
	@Descriptions({
		@Description(value = "Not implemented", title = "Search IEs In Collection", target = DocTarget.METHOD)
	})
	public Response searchIEsInCollection() {
		return Response.status(Status.NOT_IMPLEMENTED).entity("This method is not implemented.").build();
	}
	@GET
	@Path("sru/representations")
	@Produces("application/xml")
	@Descriptions({
		@Description(value = "Not implemented", title = "Search REPs In Collection", target = DocTarget.METHOD)
	})
	public Response searchREPsInCollection() {
		return Response.status(Status.NOT_IMPLEMENTED).entity("This method is not implemented.").build();
	}
	@GET
	@Path("sru/files")
	@Produces("application/xml")
	@Descriptions({
		@Description(value = "Not implemented", title = "Search Files In Collection", target = DocTarget.METHOD)
	})
	public Response searchFilesInCollection() {
		return Response.status(Status.NOT_IMPLEMENTED).entity("This method is not implemented.").build();
	}
	@GET
	@Path("lifecycle")
	@Produces("application/xml")
	@Descriptions({
		@Description(value = "Not implemented", title = "Get Sip Statuso", target = DocTarget.METHOD)
	})
	public Response getSipStatus() {
		return Response.status(Status.NOT_IMPLEMENTED).entity("This method is not implemented.").build();
	}
	@GET
	@Path("representation")
	@Produces("application/xml")
	@Descriptions({
		@Description(value = "Not implemented", title = "Get Representation", target = DocTarget.METHOD)
	})
	public Response getRepresentation() {
		return Response.status(Status.NOT_IMPLEMENTED).entity("This method is not implemented.").build();
	}

	@GET
	@Path("entity/{entity-id}/{version-id}")
	@Produces("application/xml")
	@Descriptions({
		@Description(value = "Not implemented", title = "Get METS with Version", target = DocTarget.METHOD)
	})
	public Response getMETSOfVersion(@PathParam("entity-id") String entityId, @PathParam("version-id") int version) {
		return Response.status(Status.NOT_IMPLEMENTED).entity("Please omit the version, as only the latest can be fetched").build();
	}
}