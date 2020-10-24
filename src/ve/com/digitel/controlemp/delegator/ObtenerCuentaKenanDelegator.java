package ve.com.digitel.controlemp.delegator;

import java.io.IOException;
import java.sql.CallableStatement;
import java.sql.Connection;
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

import oracle.jdbc.OracleTypes;

import org.apache.log4j.Logger;

import ve.com.digitel.bssint.EnvironmentUtil;
import ve.com.digitel.bssint.ReturnValue;
import ve.com.digitel.bssint.log.BSSIntLogger;
import ve.com.digitel.bssint.log.LogWrapper;
import ve.com.digitel.bssint.messages.ConstantMessageCode;
import ve.com.digitel.bssint.messages.ErrorMessageHandler;

/**
 * Clase ObtenerInfoGSMDelegator implementada con el Patron Singlenton que se encarga de realizar la invocacion al SP: .
 * 
 * @author Guillermo Espejo - FYCCORP - Consultor de Sistema
 */

public class ObtenerCuentaKenanDelegator {

	private static Logger logger = BSSIntLogger.getBSSIntLogger(ObtenerCuentaKenanDelegator.class);

	private String dataSource = null;

	private String inicialContextFactory = null;

	private String providerUrl = null;

	private Context initial;

	private String obtenerCuentaKenanProcedure;

	private static ObtenerCuentaKenanDelegator instance = new ObtenerCuentaKenanDelegator();

	public ObtenerCuentaKenanDelegator() {

		try {
			ResourceBundle rsr = EnvironmentUtil.getResourceAsProperties("config.properties");

			inicialContextFactory = rsr.getString("application.initial_context_factory");

			providerUrl = rsr.getString("application.provider_url");

			dataSource = rsr.getString("application.controlemp.datasource");

			obtenerCuentaKenanProcedure = rsr.getString("application.controlemp.procedure.obtenercuentakenan");

			init();

		} catch (Exception e) {
			logger.error(LogWrapper.formatMessage(ConstantMessageCode.EXCEPTION, ErrorMessageHandler
					.getMsg(ConstantMessageCode.EXCEPTION)), e);

		}
	}

	public static ObtenerCuentaKenanDelegator getInstance() {
		return instance;
	}

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
	 * 
	 * @param parameters
	 * @return
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

		try {
			// Data Source
			ds = (DataSource) initial.lookup(dataSource);

			// Abriendo Conexión
			conn = ds.getConnection();

			if (conn != null) {
				if (logger.isDebugEnabled())
					logger.debug("Se obtuvo la conexion al DataSource. Accesando a la Base de Datos");
				
				
				try {
					cs = conn.prepareCall("{ call ? := ".concat(obtenerCuentaKenanProcedure).concat(" }"));

					cs.registerOutParameter(1, OracleTypes.VARCHAR);
					cs.setInt(2, ((Integer) parameters.get("idContrato")).intValue());
					cs.execute();
				
					Map map = new HashMap();
					map.put("cuentaKenan", cs.getString(1));
					
					returnValue = new ReturnValue("0", "Procedure: ".concat(obtenerCuentaKenanProcedure).concat(" ejecutado con exito"), "", map);

					logger.info("Resultado Obtener Informacion de Cuenta Kenan: " + returnValue.getCondCode());
				
				} catch (Exception e) {
					logger.error("Error ocurrido en el delegador ObtenerCuentaKenanDelegator", e);

					returnValue = new ReturnValue(ConstantMessageCode.EXCEPTION, "Error ocurrido en el delegador", "",
							new HashMap());
				}
			} else {
				logger.error("Falla Tecnica. Error de comunicacion con el sistema externo");
			}
		} catch (Exception e) {
			logger.error("Error ocurrido durante la consulta de Informacion de Cuenta Kenan", e);
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