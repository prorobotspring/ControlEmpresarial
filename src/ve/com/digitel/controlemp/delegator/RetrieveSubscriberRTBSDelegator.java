package ve.com.digitel.controlemp.delegator;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.ResourceBundle;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.apache.commons.httpclient.DefaultHttpMethodRetryHandler;
import org.apache.commons.httpclient.Header;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpException;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.methods.StringRequestEntity;
import org.apache.commons.httpclient.params.HttpMethodParams;
import org.apache.log4j.Logger;

import ve.com.digitel.bssint.EnvironmentUtil;
import ve.com.digitel.bssint.ReturnValue;
import ve.com.digitel.bssint.log.BSSIntLogger;
import ve.com.digitel.bssint.log.LogWrapper;
import ve.com.digitel.bssint.messages.ConstantMessageCode;
import ve.com.digitel.bssint.messages.ErrorMessageHandler;
import ve.com.digitel.bssint.util.Fecha;
import ve.com.digitel.bssint.util.NameSustitutionCmdAdapter;
import ve.com.digitel.controlemp.parser.RTBSHandler;

/**
 * Clase RetrieveSubscriberRTBSDelegator implementada con el Patron Singlenton que se encarga de realizar la invocacion al metodo: RetrieveSubscriberWithIdentityNoHistory del Facturador GSM.
 * 
 * @author Diego Leal - BSS - CC Areas Tecnicas - Integracion
 * @author Rolando Castaneda - FYCCORP - Consultor de Sistema
 */
public class RetrieveSubscriberRTBSDelegator {

	private static final String DATE = "date";

	private static final String SUSCRIBER_I_D = "suscriberID";

	private static final String PASSWORD = "password";

	private static final String USERNAME = "username";

	private static Logger logger = BSSIntLogger.getBSSIntLogger(RetrieveSubscriberRTBSDelegator.class);

	private static final String RETRIEVE_SUBSCRIBER = "RetrieveSubscriber.xml";

	private static final String ENCODING = "UTF-8";

	private static final String TEXT_XML = "text/xml";

	private static final String SOAPACTION = "SOAPAction";

	private static final String FORMAT = "yyyy-MM-ddTHH:mm:ssZ";

	private static RetrieveSubscriberRTBSDelegator rtbs = new RetrieveSubscriberRTBSDelegator();

	private NameSustitutionCmdAdapter nameSustitution = new NameSustitutionCmdAdapter();

	private String endPointURL;

	private String user;

	private String password;

	private String SOAPActionValue;

	private String rtbsTimeOut;

	/**
	 * Constructor de la Clase. 
	 */
	private RetrieveSubscriberRTBSDelegator() {
		try {
			ResourceBundle rsr = EnvironmentUtil.getResourceAsProperties("config.properties");

			endPointURL = rsr.getString("application.controlemp.rtbs.endpoint");
			SOAPActionValue = rsr.getString("application.controlemp.rtbs.retrievesubscriber.soapaction");
			user = rsr.getString("application.controlemp.rtbs.user");
			password = rsr.getString("application.controlemp.rtbs.password");
			rtbsTimeOut = rsr.getString("application.controlemp.rtbs.timeout");
			nameSustitution.init(EnvironmentUtil.getStringResource(RETRIEVE_SUBSCRIBER));

		} catch (Exception e) {
			logger.error(LogWrapper.formatMessage(ConstantMessageCode.EXCEPTION, ErrorMessageHandler
					.getMsg(ConstantMessageCode.EXCEPTION)), e);

		}
	}

	/**
	 * Funcion encargada de retornar la instancia de la Clase.
	 * 
	 * @return RetrieveSubscriberRTBSDelegator Instancia de la Clase.
	 */
	public static RetrieveSubscriberRTBSDelegator getInstance() {
		return rtbs;
	}

	/**
	 * Funcion Encargada de realizar la invocacion al metodo del Facturador.
	 * 
	 * @param parameters Parametros de Entrada.
	 * @return Transfer Object donde se almacena los valores retornados en el Objecto HashMap.
	 */
	public ReturnValue execute(Map parameters) {
		
		String contents = null;
		ReturnValue returnValue = null;

		try {
			HttpClient httpClient = new HttpClient();

			httpClient.getParams().setParameter("http.connection.timeout", new Integer(rtbsTimeOut));

			String soapBody = buildSOAPBody(parameters);

			PostMethod method = new PostMethod(endPointURL);

			method.getParams().setParameter(HttpMethodParams.RETRY_HANDLER, new DefaultHttpMethodRetryHandler(0, false));

			StringRequestEntity str = new StringRequestEntity(soapBody, TEXT_XML, ENCODING);

			method.setRequestEntity(str);

			// Definicion de Action en el header
			method.addRequestHeader(new Header(SOAPACTION, SOAPActionValue));

			// Execute the POST method
			int statusCode = httpClient.executeMethod(method);

			if (statusCode == -1)
				throw new Exception("Error ocurrido durante la consulta en RTBS ".concat(method.getResponseBodyAsString()));
			else
				contents = method.getResponseBodyAsString();

			returnValue = parseResponseRTBS(contents);

			if (logger.isDebugEnabled())
				logger.debug("Retrieve Subscriber ".concat((String) parameters.get("gsm")).concat(" - ").concat(
						returnValue.toString()));

			method.releaseConnection();

		} catch (HttpException e) {
			logger.error("Error ocurrido durante la consulta a RTBS", e);
			returnValue = new ReturnValue("400", "Error ocurrido durante la consulta a RTBS - " + e.getMessage(), "",
					new HashMap());
		} catch (IOException e) {
			logger.error("Error ocurrido durante la consulta a RTBS", e);
			returnValue = new ReturnValue("400", "Error ocurrido durante la consulta a RTBS - " + e.getMessage(), "",
					new HashMap());
		} catch (Exception e) {
			logger.error("Error ocurrido durante la consulta a RTBS", e);
			returnValue = new ReturnValue("400", "Error ocurrido durante la consulta a RTBS - " + e.getMessage(), "",
					new HashMap());
		}
		return returnValue;
	}

	/**
	 * Funcion donde se Construye el Cuerpo del Body del mensaje SOAP.
	 * 
	 * @param parameters Parametros de Entrada.
	 * @return Cuerpo del Mensaje SOAP.
	 */
	private String buildSOAPBody(Map parameters) {

		Map context = new HashMap();
		context.put(USERNAME, user);
		context.put(PASSWORD, password);
		context.put(SUSCRIBER_I_D, parameters.get("gsm"));
		context.put(DATE, Fecha.dateToString(new Date(), FORMAT));

		String assembly = nameSustitution.assembly(context);

		if (logger.isDebugEnabled())
			logger.debug("Comando Ensamblado " + assembly);

		return assembly;
	}

	/**
	 * Funcion Encargada de procesar la respuesta del facturador GSM.
	 * 
	 * @param content Respuesta de la Peticion SOAP.
	 * @return Transfer Object donde se almacena los valores retornados en el Objecto HashMap.
	 * 
	 * @throws Exception
	 */
	private ReturnValue parseResponseRTBS(String content) throws Exception {
		ReturnValue returnValue;

		SAXParserFactory factory;
		SAXParser saxParser;
		factory = SAXParserFactory.newInstance();
		saxParser = factory.newSAXParser();

		factory.setNamespaceAware(true);
		factory.setValidating(true);

		InputStream in = new ByteArrayInputStream(content.getBytes());

		Map map = new HashMap();

		RTBSHandler RTBSHandler = new RTBSHandler(map);

		saxParser.parse(in, RTBSHandler);

		if (map.get("errorCode").toString().equals("")) {
			returnValue = new ReturnValue("0", "Consulta a RTBS ejecuta con exito", "", map);
		} else {
			returnValue = new ReturnValue(map.get("errorCode").toString(), map.get("errorDescription").toString(), "",
					new HashMap());
		}
		return returnValue;
	}
}