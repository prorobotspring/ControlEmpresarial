package ve.com.digitel.controlemp.delegator;

import java.io.IOException;
import java.io.StringReader;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;

import org.apache.commons.httpclient.DefaultHttpMethodRetryHandler;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpException;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.methods.StringRequestEntity;
import org.apache.commons.httpclient.params.HttpMethodParams;
import org.apache.commons.jxpath.JXPathContext;
import org.apache.log4j.Logger;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.input.SAXBuilder;

import ve.com.digitel.bssint.EnvironmentUtil;
import ve.com.digitel.bssint.ReturnValue;
import ve.com.digitel.bssint.log.BSSIntLogger;
import ve.com.digitel.bssint.log.LogWrapper;
import ve.com.digitel.bssint.messages.ConstantMessageCode;
import ve.com.digitel.bssint.messages.ErrorMessageHandler;
import ve.com.digitel.bssint.util.NameSustitutionCmdAdapter;

/**
 * 
 * @author dleal
 * 
 */
public class CopyOfListaNegraDelegator {

	private static Logger logger = BSSIntLogger.getBSSIntLogger(CopyOfListaNegraDelegator.class);

	private static final String LISTA_NEGRA_XML = "ListaNegra.xml";

	private static final String ENCODING = "UTF-8";

	private static final String TEXT_XML = "text/xml";

	private static CopyOfListaNegraDelegator rtbs = new CopyOfListaNegraDelegator();

	NameSustitutionCmdAdapter nameSustitution = new NameSustitutionCmdAdapter();

	private String endPointURL;

	private String rtbsTimeOut;
//
//	private int maxConnetionPerHost;
//
//	private int maxTotalConnections;
//
//	private HttpClient httpClient;

	private SAXBuilder builder;

	public CopyOfListaNegraDelegator() {

		try {
			ResourceBundle rsr = EnvironmentUtil.getResourceAsProperties("config.properties");

			nameSustitution.init(EnvironmentUtil.getStringResource(LISTA_NEGRA_XML));

			endPointURL = rsr.getString("application.controlemp.listanegra.endpoint");

//			maxConnetionPerHost = Integer.valueOf(rsr.getString("application.controlemp.listanegra.maxconnetionPerHost"))
//					.intValue();
//			//
//			maxTotalConnections = Integer.valueOf(rsr.getString("application.controlemp.listanegra.maxTotalConnections"))
//					.intValue();
//
			rtbsTimeOut = rsr.getString("application.controlemp.listanegra.timeOut");
//
//			MultiThreadedHttpConnectionManager connectionManager = new MultiThreadedHttpConnectionManager();
//			//
//			HttpConnectionManagerParams httpCMParams = new HttpConnectionManagerParams();
//
//			httpCMParams.setDefaultMaxConnectionsPerHost(maxConnetionPerHost);
//			httpCMParams.setMaxTotalConnections(maxTotalConnections);
//
//			connectionManager.setParams(httpCMParams);
//
//			httpClient = new HttpClient(connectionManager);
//
//			httpClient.getParams().setParameter("http.connection.timeout", new Integer(rtbsTimeOut));

		} catch (Exception e) {
			logger.error(LogWrapper.formatMessage(ConstantMessageCode.EXCEPTION, ErrorMessageHandler
					.getMsg(ConstantMessageCode.EXCEPTION)), e);

		}
	}

	public static CopyOfListaNegraDelegator getInstance() {
		return rtbs;
	}

	/**
	 * 
	 * @param parameters
	 * @return
	 * @throws IOException
	 * @throws HttpException
	 */
	public ReturnValue validarCuenta(Map parameters) throws IOException, HttpException {
		String contents = null;
		ReturnValue returnValue = null;

		try {
			HttpClient httpClient = new HttpClient();

			httpClient.getParams().setParameter("http.connection.timeout", new Integer(rtbsTimeOut));

			String body = buildBody(parameters);

			PostMethod method = new PostMethod(endPointURL);

			method.getParams().setParameter(HttpMethodParams.RETRY_HANDLER, new DefaultHttpMethodRetryHandler(0, false));

			StringRequestEntity str = new StringRequestEntity(body, TEXT_XML, ENCODING);

			if (logger.isDebugEnabled())
				logger.debug("str: " + str);

			method.setRequestEntity(str);

			// Execute the POST method
			int status = httpClient.executeMethod(method);

			if (status == -1)
				throw new Exception("Error ocurrido durante la consulta de Lista Negra ".concat(method.getResponseBodyAsString()));
			else
				contents = method.getResponseBodyAsString();

			if (logger.isDebugEnabled())
				logger.debug("Contents " + contents);

			method.releaseConnection();

			Map map;
			if(contents != null && !contents.trim().equals(""))
				map = parsearSalida(contents);
			else{
				map = new HashMap();
				map.put("ERROR_CODE", "200");
				map.put("ERROR_DESCRIPTION", "Error ocurrido durante la consulta a SIR, contents null o vacio");
			}
			
				

			returnValue = new ReturnValue(map.get("ERROR_CODE").toString(), map.get("ERROR_DESCRIPTION").toString(), "", map);

			logger.info("Consulta SIR ejecutada con exito - " + map);

		} catch (Exception e) {
			logger.error("Ocurrio un error enviando la solicitud a SIR", e);
			returnValue = new ReturnValue("200", "Error ocurrido durante la consulta a SIR", "", new HashMap());
		}

		return returnValue;
	}

	/**
	 * 
	 * @param contents
	 * @return
	 * @throws JDOMException
	 * @throws IOException
	 */
	private Map parsearSalida(String contents) throws JDOMException, IOException {

		builder = new SAXBuilder();

		Document doc = builder.build(new StringReader(contents));
		JXPathContext context = JXPathContext.newContext(doc);

		Element headerElement = (Element) context.selectSingleNode("/EAIMessage/IBX_ADP_IN_PARAMETERS");

		List children = headerElement.getChildren();

		Map map = new HashMap();
		for (Iterator iterator = children.iterator(); iterator.hasNext();) {
			Element param = (Element) iterator.next();

			map.put(param.getAttribute("name").getValue(), param.getAttribute("value").getValue());
		}

		return map;
	}

	private String buildBody(Map parameters) {

		String assembly = nameSustitution.assembly(parameters);

		if (logger.isDebugEnabled())
			logger.debug("Comando Ensamblado " + assembly);

		return assembly;
	}

}