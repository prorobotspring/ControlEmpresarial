package ve.com.digitel.controlemp.rc;

import java.io.IOException;
import java.sql.SQLException;
import java.util.Map;

import javax.naming.NamingException;

import org.apache.commons.httpclient.HttpException;
import org.apache.log4j.Logger;

import ve.com.digitel.bssint.ReturnValue;
import ve.com.digitel.bssint.log.BSSIntLogger;
import ve.com.digitel.controlemp.delegator.ActualizarNotaDebitoDelegator;
import ve.com.digitel.controlemp.delegator.AgregarNRCDelegator;
import ve.com.digitel.controlemp.delegator.CerrarContratoDelegator;
import ve.com.digitel.controlemp.delegator.CrearBatchDebitoDelegator;
import ve.com.digitel.controlemp.delegator.CrearDetalleProcesoDelegator;
import ve.com.digitel.controlemp.delegator.CrearNotaBatchDebitoDelegator;
import ve.com.digitel.controlemp.delegator.CrearProcesoContratoDelegator;
import ve.com.digitel.controlemp.delegator.CrearProcesoDelegator;
import ve.com.digitel.controlemp.delegator.EMailDelegator;
import ve.com.digitel.controlemp.delegator.IsDirectNRCDelegator;
import ve.com.digitel.controlemp.delegator.KenanDelegator;
import ve.com.digitel.controlemp.delegator.ListaNegraDelegator;
import ve.com.digitel.controlemp.delegator.NonVoucherRechargeRTBSDelegator;
import ve.com.digitel.controlemp.delegator.ObtenerBeneficiarioCierreDelegator;
import ve.com.digitel.controlemp.delegator.ObtenerBeneficiarioDelegator;
import ve.com.digitel.controlemp.delegator.ObtenerContratosCerrarDelegator;
import ve.com.digitel.controlemp.delegator.ObtenerContratosPreCerrarDelegator;
import ve.com.digitel.controlemp.delegator.ObtenerGSMProcesarDelegator;
import ve.com.digitel.controlemp.delegator.ObtenerCuentaKenanDelegator;
import ve.com.digitel.controlemp.delegator.ObtenerMontosCargosDelegator;
import ve.com.digitel.controlemp.delegator.ObtenerProductoDelegator;
import ve.com.digitel.controlemp.delegator.RetrieveSubscriberRTBSDelegator;
import ve.com.digitel.controlemp.delegator.ValidarPlanTelefoniaDelegator;

/**
 * Clase RCHelper se encarga de armar el puente entre las invocaciones realizadas en la Clase RC y los delegadores.
 * 
 * @author Diego Leal - BSS - CC Areas Tecnicas - Integracion
 * @author Rolando Castaneda - FYCCORP - Consultor de Sistema
 */
public class RCHelper {

	private static Logger logger = BSSIntLogger.getBSSIntLogger(RCHelper.class);

	private static RCHelper uniqueInstance = new RCHelper();

	/**
	 * Retorna la instancia unica de RCHelper
	 * 
	 * @return Objeto RCHelper
	 */
	public static RCHelper getInstance() {
		return uniqueInstance;
	}

	/**
	 * Funcion encargada de Crear el Proceso.
	 * 
	 * @param parameters Parametros de Entrada.
	 * @return Transfer Object donde se almacena los valores retornados en el Objecto HashMap.
	 * 
	 * @throws IOException
	 * @throws SQLException
	 * @throws NamingException
	 */
	public ReturnValue executeProcCrearProceso(Map parameters) throws IOException, SQLException, NamingException {
		if (logger.isDebugEnabled())
			logger.debug("RCHelper executeProcCrearProceso");

		return CrearProcesoDelegator.getInstance().execute(parameters);
	}

	/**
	 * Funcion encargada de obtener los contratos para realizar el PreCierre.
	 * 
	 * @param parameters Parametros de Entrada.
	 * @return Transfer Object donde se almacena los valores retornados en el Objecto HashMap.
	 * 
	 * @throws IOException
	 * @throws SQLException
	 * @throws NamingException
	 */
	public ReturnValue executeProcObtenerContratosPreCerrar(Map parameters) throws IOException, SQLException, NamingException {
		if (logger.isDebugEnabled())
			logger.debug("RCHelper executeProcObtenerContratosPreCerrar");

		return ObtenerContratosPreCerrarDelegator.getInstance().execute(parameters);
	}

	/**
	 * Funcion encargada de Crear un Proceso por Contrato.
	 * 
	 * @param parameters Parametros de Entrada.
	 * @return Transfer Object donde se almacena los valores retornados en el Objecto HashMap.
	 * 
	 * @throws IOException
	 * @throws SQLException
	 * @throws NamingException
	 */
	public ReturnValue executeProcCrearProcesoContrato(Map parameters) throws IOException, SQLException, NamingException {
		if (logger.isDebugEnabled())
			logger.debug("RCHelper executeProcCrearProcesoContrato");

		return CrearProcesoContratoDelegator.getInstance().execute(parameters);
	}

	/**
	 * Funcion encargada de Obtener la Lista de Benificiarios.
	 * 
	 * @param parameters Parametros de Entrada.
	 * @return Transfer Object donde se almacena los valores retornados en el Objecto HashMap.
	 * 
	 * @throws IOException
	 * @throws SQLException
	 * @throws NamingException
	 */
	public ReturnValue executeProcObtenerBeneficiario(Map parameters) throws IOException, SQLException, NamingException {
		if (logger.isDebugEnabled())
			logger.debug("RCHelper executeProcobtenerBeneficiario");

		return ObtenerBeneficiarioDelegator.getInstance().execute(parameters);
	}

	/**
	 * Funcion encargada de crear Detalle del Proceso.
	 * 
	 * @param parameters Parametros de Entrada.
	 * @param idContrato Identificador del contrato.
	 * @param idCuentaDebito Identificador de de la cuenta Debito. 
	 * @return Transfer Object donde se almacena los valores retornados en el Objecto HashMap.
	 * 
	 * @throws IOException
	 * @throws SQLException
	 * @throws NamingException
	 */
	public ReturnValue executeProcCrearDetalleProceso(Map parameters, int idContrato, String idCuentaDebito) throws IOException,
			SQLException, NamingException {
		if (logger.isDebugEnabled())
			logger.debug("RCHelper executeProcCrearDetalleProceso");

		return CrearDetalleProcesoDelegator.getInstance().execute(parameters, idContrato, idCuentaDebito);
	}

	/**
	 * Funcion Encargada de Obtener Lista de Contratos a Cerrar.
	 * 
	 * @param parameters Parametros de Entrada.
	 * @return Transfer Object donde se almacena los valores retornados en el Objecto HashMap.
	 * 
	 * @throws IOException
	 * @throws SQLException
	 * @throws NamingException
	 */
	public ReturnValue executeProcObtenerContratosCerrar(Map parameters) throws IOException, SQLException, NamingException {
		if (logger.isDebugEnabled())
			logger.debug("RCHelper executeProcObtenerContratosCerrar");

		return ObtenerContratosCerrarDelegator.getInstance().execute(parameters);
	}

	/**
	 * Funcion encargada de crer registro en Batch Debito.
	 * 
	 * @param parameters Parametros de Entrada.
	 * @return Transfer Object donde se almacena los valores retornados en el Objecto HashMap.
	 * 
	 * @throws IOException
	 * @throws SQLException
	 * @throws NamingException
	 */
	public ReturnValue executeProcCrearBatchDebito(Map parameters) throws IOException, SQLException, NamingException {
		if (logger.isDebugEnabled())
			logger.debug("RCHelper executeProcCrearBatchDebito");

		return CrearBatchDebitoDelegator.getInstance().execute(parameters);
	}

	/**
	 * Funcion encargada de Obtener los Planes Permitidos.
	 * 
	 * @param parameters Parametros de Entrada.
	 * @return Transfer Object donde se almacena los valores retornados en el Objecto HashMap.
	 * 
	 * @throws IOException
	 * @throws SQLException
	 * @throws NamingException
	 */
	public ReturnValue executeProcObtenerProducto(Map parameters) throws IOException, SQLException, NamingException {
		if (logger.isDebugEnabled())
			logger.debug("RCHelper executeProcObtenerProducto");

		return ObtenerProductoDelegator.getInstance().execute(parameters);
	}

	/**
	 * Funcion encargada de Obtener la informacion del Subscriptor.
	 * 
	 * @param parameters Parametros de Entrada.
	 * @return Transfer Object donde se almacena los valores retornados en el Objecto HashMap.
	 * 
	 */
	public ReturnValue executeRetrieveSubscriberRTBS(Map parameters) {
		if (logger.isDebugEnabled())
			logger.debug("RCHelper executeRetrieveSubscriberRTBS");

		return RetrieveSubscriberRTBSDelegator.getInstance().execute(parameters);
	}

	/**
	 * Funcion encargada de validar si el Plan actual con el tipo de telefonia es valido.
	 * 
	 * @param parameters Parametros de Entrada.
	 * @return Transfer Object donde se almacena los valores retornados en el Objecto HashMap.
	 * 
	 * @throws IOException
	 * @throws SQLException
	 * @throws NamingException
	 */
	public ReturnValue executeProcValidarPlanTelefonia(Map parameters) throws IOException, SQLException, NamingException {
		if (logger.isDebugEnabled())
			logger.debug("RCHelper executeProcValidarPlan");

		return ValidarPlanTelefoniaDelegator.getInstance().execute(parameters);
	}
	
	/**
	 * Funcion encargada de Obtener la Lista de Beneficiarios a Cerrar.
	 * 
	 * @param parameters Parametros de Entrada.
	 * @return Transfer Object donde se almacena los valores retornados en el Objecto HashMap.
	 * 
	 * @throws IOException
	 * @throws SQLException
	 * @throws NamingException
	 */
	public ReturnValue executeProcObtenerBeneficiarioCierre(Map parameters) throws IOException, SQLException, NamingException {
		if (logger.isDebugEnabled())
			logger.debug("RCHelper executeObtenerBeneficiarioCierre");

		return ObtenerBeneficiarioCierreDelegator.getInstance().execute(parameters);
	}

	/**
	 * Funcion encargada de Crear Nota Batch Debito.
	 * 
	 * @param parameters Parametros de Entrada.
	 * @return Transfer Object donde se almacena los valores retornados en el Objecto HashMap.
	 * 
	 * @throws IOException
	 * @throws SQLException
	 * @throws NamingException
	 */
	public ReturnValue executeProcCrearNotaBatchDebito(Map parameters) throws IOException, SQLException, NamingException {
		if (logger.isDebugEnabled())
			logger.debug("RCHelper executeProcCrearNotaBatchDebito");

		return CrearNotaBatchDebitoDelegator.getInstance().execute(parameters);
	}

	/**
	 * Funcion encargada de Obtener los Montos a Debitar.
	 * 
	 * @param parameters Parametros de Entrada.
	 * @return Transfer Object donde se almacena los valores retornados en el Objecto HashMap.
	 * 
	 * @throws IOException
	 * @throws SQLException
	 * @throws NamingException
	 */
	public ReturnValue executeProcObtenerMontosCargo(Map parameters) throws IOException, SQLException, NamingException {
		if (logger.isDebugEnabled())
			logger.debug("RCHelper executeProcObtenerMontosCargo");

		return ObtenerMontosCargosDelegator.getInstance().execute(parameters);
	}

	/**
	 * Funcion encargada de Actualizar la Nota de Debito.
	 * 
	 * @param parameters Parametros de Entrada.
	 * @return Transfer Object donde se almacena los valores retornados en el Objecto HashMap.
	 * 
	 * @throws IOException
	 * @throws SQLException
	 * @throws NamingException
	 */
	public ReturnValue executeProcActualizarNotaDebito(Map parameters) throws IOException, SQLException, NamingException {
		if (logger.isDebugEnabled())
			logger.debug("RCHelper executeProcActualizarNotaDebito");

		return ActualizarNotaDebitoDelegator.getInstance().execute(parameters);
	}

	/**
	 * Funcion encargada de Cerrar el Contrato.
	 * 
	 * @param parameters Parametros de Entrada.
	 * @return Transfer Object donde se almacena los valores retornados en el Objecto HashMap.
	 * 
	 * @throws IOException
	 * @throws SQLException
	 * @throws NamingException
	 */
	public ReturnValue executeProcCerrarContrato(Map parameters) throws IOException, SQLException, NamingException {
		if (logger.isDebugEnabled())
			logger.debug("RCHelper executeProcCerrarContrato");

		return CerrarContratoDelegator.getInstance().execute(parameters);
	}
	/**
	 * Funcion encargada de validar si una cuenta ha sido rediccionada.
	 * 
	 * @param parameters Parametros de Entrada.
	 * @return Transfer Object donde se almacena los valores retornados en el Objecto HashMap.
	 * 
	 * @throws IOException
	 * @throws SQLException
	 * @throws NamingException
	 */
	public ReturnValue executeProcIsDirectNRC(Map parameters) throws IOException, SQLException, NamingException {
		if (logger.isDebugEnabled())
			logger.debug("RCHelper executeProcIsDirectNRC");

		return IsDirectNRCDelegator.getInstance().execute(parameters);
	}

	/**
	 * Funcion encargada de agregar NRC.
	 * 
	 * @param parameters Parametros de Entrada.
	 * @return Transfer Object donde se almacena los valores retornados en el Objecto HashMap.
	 */
	public ReturnValue executeProcAgregarNRC(Map parameters) throws IOException, SQLException, NamingException {
		if (logger.isDebugEnabled())
			logger.debug("RCHelper executeProcAgregarNRC");

		return AgregarNRCDelegator.getInstance().execute(parameters);
	}

	/**
	 * Funcion encargada de Validar la Cuenta posee un contexto valido.
	 * 
	 * @param parameters Parametros de Entrada.
	 * @return Transfer Object donde se almacena los valores retornados en el Objecto HashMap.
	 */
	public ReturnValue executeValidarCuentaKenan(Map parameters) {
		if (logger.isDebugEnabled())
			logger.debug("RCHelper executeValidarCuentaKenan");
		
		return KenanDelegator.getInstance().validarCuenta(parameters);
	}

	
	
	/**
	 * Funcion encargada de Validar el Estatus de la Cuenta.
	 * 
	 * @param parameters Parametros de Entrada.
	 * @return Transfer Object donde se almacena los valores retornados en el Objecto HashMap.
	 */
	public ReturnValue executeValidarEstatus(Map parameters) {
		if (logger.isDebugEnabled())
			logger.debug("RCHelper executeValidarEstatus");
		
		return KenanDelegator.getInstance().validarEstatusCuenta(parameters);
	}
	
	/**
	 * Funcion encargada de Validar si la cuenta esta redireccionada.
	 * 
	 * @param parameters Parametros de Entrada.
	 * @return Transfer Object donde se almacena los valores retornados en el Objecto HashMap.
	 */
	public ReturnValue validarRedirectCuenta(Map parameters) {
		if (logger.isDebugEnabled())
			logger.debug("RCHelper validarRedirectCuenta");
		
		return KenanDelegator.getInstance().validarRedirectCuenta(parameters);
	}
	
	
	/**
	 * Funcion encargada de Validar la Cuenta en Lista Negra.
	 * 
	 * @param parameters Parametros de Entrada.
	 * @return Transfer Object donde se almacena los valores retornados en el Objecto HashMap.
	 * 
	 * @throws IOException
	 * @throws SQLException
	 * @throws NamingException
	 */
	public ReturnValue executeValidarCuentaSIR(Map parameters) throws IOException, SQLException, NamingException {
		if (logger.isDebugEnabled())
			logger.debug("RCHelper executeValidarCuentaSIR");
		
		return ListaNegraDelegator.getInstance().execute(parameters);
	}

	/**
	 * Funcion encargada de realizar la recarga de un GSM.
	 * 
	 * @param parameters Parametros de Entrada.
	 * @return Transfer Object donde se almacena los valores retornados en el Objecto HashMap.
	 * 
	 * @throws HttpException
	 * @throws IOException
	 */
	public ReturnValue executeRechargeSubscriberRTBS(Map parameters) throws HttpException, IOException {
		if (logger.isDebugEnabled())
			logger.debug("RCHelper executeRechargeSubscriberRTBS");
		
		return NonVoucherRechargeRTBSDelegator.getInstance().execute(parameters);
	}

	/**
	 * Funcion encargada de Enviar Email.
	 * 
	 * @param body Cuerpo del Correo Electronico.
	 * @param cicloFacturacion Ciclo de la Facturacion.
	 */
	public void executeSendMail(String body, String cicloFacturacion) {
		if (logger.isDebugEnabled())
			logger.debug("RCHelper executeSendMail");
		
		EMailDelegator.getInstance().sendMail(body, cicloFacturacion);
	}

	public int obtenerTotalGSMProcesar(Integer idProductoSpec, String cicloFacturacion) {
		if (logger.isDebugEnabled())
			logger.debug("RCHelper obtenerTotalGSMProcesar");
		
		return ObtenerGSMProcesarDelegator.getInstance().execute(idProductoSpec, cicloFacturacion);
	}
	
	
	/**
	 * Funcion encargada de Obtener el Identificador de la cuenta kenan basado en el idContrato.
	 * 
	 * @param parameters Parametros de Entrada.
	 * @return Transfer Object donde se almacena los valores retornados en el Objecto HashMap.
	 * 
	 * @throws IOException
	 * @throws SQLException
	 * @throws NamingException
	 */
	public ReturnValue executeObtenerCuentaKenanDelegator(Map parameters) throws IOException, SQLException, NamingException {
		if (logger.isDebugEnabled())
			logger.debug("RCHelper executeObtenerCuentaKenanDelegator");

		return ObtenerCuentaKenanDelegator.getInstance().execute(parameters);
	}
}