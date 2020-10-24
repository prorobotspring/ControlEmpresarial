package ve.com.digitel.controlemp.delegator;

import java.io.IOException;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.ResourceBundle;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.sql.DataSource;

import org.apache.log4j.Logger;

import ve.com.digitel.bssint.EnvironmentUtil;
import ve.com.digitel.bssint.ReturnValue;
import ve.com.digitel.bssint.log.BSSIntLogger;
import ve.com.digitel.bssint.log.LogWrapper;
import ve.com.digitel.bssint.messages.ConstantMessageCode;
import ve.com.digitel.bssint.messages.ErrorMessageHandler;

/**
 * Clase AgregarNRCDelegator implementada con el Patron Singlenton que se encarga de realizar la invocacion al Procedure: PROCESO_PKG.agreganrc.
 * 
 * @author Diego Leal - BSS - CC Areas Tecnicas - Integracion
 * @author Rolando Castaneda - FYCCORP - Consultor de Sistema
 */
public class AgregarNRCDelegator {

	private static Logger logger = BSSIntLogger.getBSSIntLogger(AgregarNRCDelegator.class);

	private String dataSource = null;

	private String inicialContextFactory = null;

	private String providerUrl = null;

	private Context initial;

	private String agregarNRCProcedure;

	private static AgregarNRCDelegator rtbs = new AgregarNRCDelegator();

	private String annotation;

	private String[] mesArr = { "Enero", "Febrero", "Marzo", "Abril", "Mayo", "Junio", "Julio", "Agosto", "Septiembre", "Octubre", "Noviembre", "Diciembre" };

	/**
	 * Constructor de la Clase.
	 */
	public AgregarNRCDelegator() {
		try {
			ResourceBundle rsr = EnvironmentUtil.getResourceAsProperties("config.properties");

			inicialContextFactory = rsr.getString("application.initial_context_factory");
			providerUrl = rsr.getString("application.provider_url");
			dataSource = rsr.getString("application.controlemp.datasource");
			agregarNRCProcedure = rsr.getString("application.controlemp.procedure.agregarnrc");
			annotation = rsr.getString("application.controlemp.kenan.annotation");

			init();
		} catch (Exception e) {
			logger.error(LogWrapper.formatMessage(ConstantMessageCode.EXCEPTION, ErrorMessageHandler
					.getMsg(ConstantMessageCode.EXCEPTION)), e);

		}
	}

	/**
	 * Funcion encargada de retornar la instancia de la Clase.
	 * 
	 * @return AgregarNRCDelegator Instancia de la Clase.
	 */
	public static AgregarNRCDelegator getInstance() {
		return rtbs;
	}

	/**
	 * Metodo encargada de Incializar el Contexto.
	 */
	private void init() {
		try {
			if (logger.isDebugEnabled())
				logger.debug("Metodo de inicializacion de contexto para DataBaseDelegator");

			Properties env = new Properties();

			env.put(Context.INITIAL_CONTEXT_FACTORY, inicialContextFactory);
			env.put(Context.PROVIDER_URL, providerUrl);

			initial = new InitialContext(env);
		} catch (Exception e) {
			logger.error(LogWrapper.formatMessage(ConstantMessageCode.EXCEPTION, ErrorMessageHandler
					.getMsg(ConstantMessageCode.EXCEPTION)), e);

		}
	}

	/**
	 * Funcion Encargada de realizar la invocacion al Procedure.
	 * 
	 * @param parameters Parametros de Entrada.
	 * @return Transfer Object donde se almacena los valores retornados en el Objecto HashMap.
	 * 
	 * @throws IOException
	 * @throws SQLException
	 * @throws NamingException
	 */
	public ReturnValue execute(Map parameters) throws IOException, SQLException, NamingException {
		ReturnValue returnValue = null;
		DataSource ds = null;
		Connection conn = null;
		CallableStatement cs = null;
		ResultSet rs = null;

		// Data Source
		ds = (DataSource) initial.lookup(dataSource);

		try {
			// Abriendo Conexión
			conn = ds.getConnection();

			if (conn != null) {
				if (logger.isDebugEnabled())
					logger.debug("Se obtuvo la conexion al DataSource. Accesando a la Base de Datos");

				try {

					cs = conn.prepareCall("{ call ".concat(agregarNRCProcedure).concat(" }"));

					//java.sql.Date dateSQL = new java.sql.Date(new Date().getTime());
										
					DateFormat df = new SimpleDateFormat("dd/MM/yyyy");
					Calendar cal = Calendar.getInstance();
					
					String dateSP = String.valueOf(parameters.get("diaCorte")).concat("/");
					dateSP = dateSP.concat(String.valueOf(cal.get(Calendar.MONTH) + 1)).concat("/");
					dateSP = dateSP.concat(String.valueOf(cal.get(Calendar.YEAR)));
					
					java.sql.Date dateSQL = new java.sql.Date(((Date)df.parse(dateSP)).getTime());
					
					//java.sql.Date dateSQL = new java.sql.Date(new Date().getTime());
					
					cs.setDate(1, dateSQL);
					cs.setInt(2, Integer.valueOf((String) parameters.get("typeIdNRC")).intValue());
					cs.setInt(3, Integer.valueOf((String) parameters.get("cuentaKenan")).intValue());
					cs.setInt(4, Integer.valueOf((String) parameters.get("montoCargado")).intValue());

					String mesFecha = " ".concat(mesArr[cal.get(Calendar.MONTH)]).concat(" ").concat(
							String.valueOf(cal.get(Calendar.YEAR)));

					if (logger.isDebugEnabled())
						logger.debug("Anotation: " + annotation.concat(mesFecha));

					cs.setString(5, annotation.concat(mesFecha));
					cs.execute();

					returnValue = new ReturnValue("0", "Procedure: ".concat(agregarNRCProcedure).concat(" ejecutado con exito"),
							"", new HashMap());

					logger.info("Resultado agregar NRC: " + returnValue);
				} catch (Exception e) {
					logger.error("Error ocurrido en el delegador AgregarNRCDelegator", e);
					
					returnValue = new ReturnValue(ConstantMessageCode.EXCEPTION, "Error ocurrido en el delegador. "
							+ e.getMessage(), "", new HashMap());
				}
			} else {
				logger.error("Falla Tecnica. Error de comunicacion con el sistema externo");
			}
		} finally {
			if (rs != null)
				rs.close();
			if (cs != null)
				cs.close();
			if (conn != null)
				conn.close();
		}
		return returnValue;
	}
}