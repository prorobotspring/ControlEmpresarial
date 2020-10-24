package ve.com.digitel.controlemp.delegator;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
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
import ve.com.digitel.bssint.util.NameSustitutionCmdAdapter;

/**
 * Clase ListaNegraDelegator implementada con el Patron Singlenton que se encarga de realizar la invocacion al query para obtener el valor de Lista Negra.
 * 
 * @author Diego Leal - BSS - CC Areas Tecnicas - Integracion
 * @author Rolando Castaneda - FYCCORP - Consultor de Sistema
 */
public class ListaNegraDelegator {

	private static Logger logger = BSSIntLogger.getBSSIntLogger(ListaNegraDelegator.class);

	private static ListaNegraDelegator rtbs = new ListaNegraDelegator();

	NameSustitutionCmdAdapter nameSustitution = new NameSustitutionCmdAdapter();

	private String dataSource = null;

	private String inicialContextFactory = null;

	private String providerUrl = null;

	private Context initial;

	private String listaNegraQuery;

	/**
	 * Constructor de la Clase.
	 */
	public ListaNegraDelegator() {
		try {
			ResourceBundle rsr = EnvironmentUtil.getResourceAsProperties("config.properties");

			inicialContextFactory = rsr.getString("application.initial_context_factory");
			providerUrl = rsr.getString("application.provider_url");
			dataSource = rsr.getString("application.controlemp.datasource");
			listaNegraQuery = rsr.getString("application.controlemp.query.listanegra");

			init();
		} catch (Exception e) {
			logger.error(LogWrapper.formatMessage(ConstantMessageCode.EXCEPTION, ErrorMessageHandler
					.getMsg(ConstantMessageCode.EXCEPTION)), e);

		}
	}

	/**
	 * Funcion encargada de retornar la instancia de la Clase.
	 * 
	 * @return ListaNegraDelegator Instancia de la Clase.
	 */
	public static ListaNegraDelegator getInstance() {
		return rtbs;
	}
	
	/**
	 * Metodo encargada de Incializar el Contexto.
	 */
	private void init() {
		try {
			if (logger.isDebugEnabled())
				logger.debug("Metodo de inicializacion de contexto para CrearBatchDebitoDelegator");

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
		PreparedStatement pstm = null;
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

					pstm = conn.prepareStatement(listaNegraQuery);

					pstm.setString(1, (String) parameters.get("idCuenta"));

					rs = pstm.executeQuery();

					Map map = new HashMap();

					if (rs.next()) {
						map.put("existe", "Y");
					} else {
						map.put("existe", "N");
					}

					returnValue = new ReturnValue("0", "Consulta SIR ejecutado con exito", "", map);

					logger.info("Resultado Validar Cuenta SIR: " + returnValue);
				} catch (Exception e) {
					logger.error("Error ocurrido en el delegador ListaNegraDelegator", e);

					returnValue = new ReturnValue(ConstantMessageCode.EXCEPTION,
							"Error ocurrido en el delegador ListaNegraDelegator", "", new HashMap());
				}
			} else {
				logger.error("Falla Tecnica. Error de comunicacion con el sistema externo");
			}
		} finally {
			if (rs != null)
				rs.close();
			if (pstm != null)
				pstm.close();
			if (conn != null)
				conn.close();
		}
		return returnValue;
	}
}