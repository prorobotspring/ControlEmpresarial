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

import org.apache.log4j.Logger;

import ve.com.digitel.bssint.EnvironmentUtil;
import ve.com.digitel.bssint.ReturnValue;
import ve.com.digitel.bssint.log.BSSIntLogger;
import ve.com.digitel.bssint.log.LogWrapper;
import ve.com.digitel.bssint.messages.ConstantMessageCode;
import ve.com.digitel.bssint.messages.ErrorMessageHandler;

/**
 * Clase CerrarContratoDelegator implementada con el Patron Singlenton que se encarga de realizar la invocacion al Procedure: PROCESO_PKG.cerrarContratos.
 * 
 * @author Diego Leal - BSS - CC Areas Tecnicas - Integracion
 * @author Rolando Castaneda - FYCCORP - Consultor de Sistema
 */
public class CerrarContratoDelegator {

	private static Logger logger = BSSIntLogger.getBSSIntLogger(CerrarContratoDelegator.class);

	private String dataSource = null;

	private String inicialContextFactory = null;

	private String providerUrl = null;

	private Context initial;

	private String cerrarContratoProcedure;

	private static CerrarContratoDelegator rtbs = new CerrarContratoDelegator();

	/**
	 * Constructor de la Clase.
	 */
	public CerrarContratoDelegator() {
		try {
			ResourceBundle rsr = EnvironmentUtil.getResourceAsProperties("config.properties");

			inicialContextFactory = rsr.getString("application.initial_context_factory");
			providerUrl = rsr.getString("application.provider_url");
			dataSource = rsr.getString("application.controlemp.datasource");
			cerrarContratoProcedure = rsr.getString("application.controlemp.procedure.cerrarcontratos");

			init();
		} catch (Exception e) {
			logger.error(LogWrapper.formatMessage(ConstantMessageCode.EXCEPTION, ErrorMessageHandler
					.getMsg(ConstantMessageCode.EXCEPTION)), e);

		}
	}

	/**
	 * Funcion encargada de retornar la instancia de la Clase.
	 * 
	 * @return ActualizarNotaDebitoDelegator Instancia de la Clase.
	 */
	public static CerrarContratoDelegator getInstance() {
		return rtbs;
	}

	/**
	 * Metodo encargada de Incializar el Contexto.
	 */
	private void init() {
		try {
			if (logger.isDebugEnabled())
				logger.debug("Metodo de inicializacion de contexto para actualizarnotadebito");

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

					cs = conn.prepareCall("{ call ".concat(cerrarContratoProcedure).concat(" }"));

					cs.setString(1, ((Integer) parameters.get("idContrato")).toString());
					cs.execute();

					returnValue = new ReturnValue("0", "Procedure: ".concat(cerrarContratoProcedure).concat(
							" ejecutado con exito"), "", new HashMap());

					logger.info("Resultado CerrarContrato Debito: " + returnValue + " - " + parameters.get("idContrato"));
				} catch (Exception e) {
					logger.error("Error ocurrido en el delegador cerrarContratosDelegator", e);

					returnValue = new ReturnValue(ConstantMessageCode.EXCEPTION,
							"Error ocurrido en el delegador cerrarContratosDelegator", "", new HashMap());
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