package ve.com.digitel.controlemp.delegator;

import java.io.IOException;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Properties;
import java.util.ResourceBundle;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.sql.DataSource;

import oracle.jdbc.OracleTypes;

import org.apache.log4j.Logger;

import ve.com.digitel.bssint.EnvironmentUtil;
import ve.com.digitel.bssint.log.BSSIntLogger;
import ve.com.digitel.bssint.log.LogWrapper;
import ve.com.digitel.bssint.messages.ConstantMessageCode;
import ve.com.digitel.bssint.messages.ErrorMessageHandler;

/**
 * 
 * @author dleal
 * 
 */
public class ObtenerGSMProcesarDelegator {

	private static Logger logger = BSSIntLogger.getBSSIntLogger(ObtenerGSMProcesarDelegator.class);

	private String dataSource = null;

	private String inicialContextFactory = null;

	private String providerUrl = null;

	private Context initial;

	private String obtenerGSMProcesarProcedure;

	private static ObtenerGSMProcesarDelegator rtbs = new ObtenerGSMProcesarDelegator();

	public ObtenerGSMProcesarDelegator() {

		try {
			ResourceBundle rsr = EnvironmentUtil.getResourceAsProperties("config.properties");

			inicialContextFactory = rsr.getString("application.initial_context_factory");

			providerUrl = rsr.getString("application.provider_url");

			dataSource = rsr.getString("application.controlemp.datasource");

			obtenerGSMProcesarProcedure = rsr.getString("application.controlemp.procedure.totalgsmprocesados");

			init();

		} catch (Exception e) {
			logger.error(LogWrapper.formatMessage(ConstantMessageCode.EXCEPTION, ErrorMessageHandler
					.getMsg(ConstantMessageCode.EXCEPTION)), e);

		}
	}

	public static ObtenerGSMProcesarDelegator getInstance() {
		return rtbs;
	}

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
	 * 
	 * @param parameters
	 * @return
	 * @throws IOException
	 * @throws SQLException
	 * @throws NamingException
	 */
	public int execute(Integer idProductSpec, String cicloFacturacion) {
		DataSource ds = null;
		Connection conn = null;
		CallableStatement cs = null;
		ResultSet rs = null;

		int totalGSM = 0;

		try {
			// Data Source
			ds = (DataSource) initial.lookup(dataSource);

			// Abriendo Conexión
			conn = ds.getConnection();

			if (conn != null) {
				if (logger.isDebugEnabled())
					logger.debug("Se obtuvo la conexion al DataSource. Accesando a la Base de Datos");

				cs = conn.prepareCall("{ call ".concat(obtenerGSMProcesarProcedure).concat(" }"));

				cs.setInt(1, idProductSpec.intValue());
				cs.setString(2, cicloFacturacion);
				cs.registerOutParameter(3, OracleTypes.NUMBER);
				cs.execute();

				totalGSM = cs.getInt(3);

				logger.info("Obtenidos con exito el total de GSMs a procesar: " + totalGSM);

			} else {
				logger.error("Falla Tecnica. Error de comunicacion con el sistema externo");
			}
		} catch (SQLException e) {
			logger.error("Error ocurrido durante la consulta de GSMs a procesar", e);
		} catch (NamingException e) {
			logger.error("Error ocurrido durante la consulta de GSMs a procesar", e);
		} catch (Exception e) {
			logger.error("Error ocurrido durante la consulta de GSMs a procesar", e);
		} finally {
			try {
				if (rs != null)
					rs.close();
				if (cs != null)
					cs.close();
				if (conn != null)
					conn.close();
			} catch (SQLException e) {
				e.printStackTrace();
			}
		}
		return totalGSM;
	}

}