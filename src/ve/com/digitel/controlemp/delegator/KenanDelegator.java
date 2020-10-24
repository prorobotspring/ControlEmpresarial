package ve.com.digitel.controlemp.delegator;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;

import org.apache.log4j.Logger;

import ve.com.digitel.bssint.EnvironmentUtil;
import ve.com.digitel.bssint.ReturnValue;
import ve.com.digitel.bssint.log.BSSIntLogger;

/**
 * Clase KenanDelegator implementada con el Patron Singlenton que se encarga de validar el Contexto de la cuenta.
 * 
 * @author Diego Leal - BSS - CC Areas Tecnicas - Integracion
 * @author Rolando Castaneda - FYCCORP - Consultor de Sistema
 */
public class KenanDelegator {

	private static Logger logger = BSSIntLogger.getBSSIntLogger(KenanDelegator.class);

	private static KenanDelegator rtbs = new KenanDelegator();

	private List vipCodeList;

	private List salesCodes;

	/**
	 * Constructor de la Clase.
	 */
	public KenanDelegator() {
		ResourceBundle rsr = EnvironmentUtil.getResourceAsProperties("config.properties");

		vipCodeList = Arrays.asList(rsr.getString("application.controlemp.kenan.vipcode").trim().split(" "));
		salesCodes = Arrays.asList(rsr.getString("application.controlemp.kenan.salescodes").trim().split(";"));
	}

	/**
	 * Funcion encargada de retornar la instancia de la Clase.
	 * 
	 * @return KenanDelegator Instancia de la Clase.
	 */
	public static KenanDelegator getInstance() {
		return rtbs;
	}

	/**
	 * Metodo encargada de Incializar el Contexto.
	 */
	public void init() {
	}

	/**
	 * Funcion encargada de validar el Estatus.
	 * 
	 * @param parameters Parametros de Entrada.
	 * @return 
	 */
	private boolean isValidStatus(Map parameters){
		if(parameters.get("salesCode") == null ) {
			return true;
		} else if(salesCodes.contains(parameters.get("salesCode")) && Integer.parseInt((String)parameters.get("TotalGSMAfiliados")) != 0  )  {
			return true;
		} else if(!salesCodes.contains(parameters.get("salesCode"))) {
			return true;
		} else {
			return false;
		}
	}


	/**
	 * Funcion encargada de validar si redireccionada.
	 * 
	 * @param parameters Parametros de Entrada.
	 * @return 
	 */
	private boolean isRedirectStatus(Map parameters){
		
		if(((Integer)parameters.get("isRedirect")).intValue() == new Integer(0).intValue() ) {
			return false;
		} else if( ((Integer)parameters.get("isRedirect")).intValue() == new Integer(1).intValue() && 
					(Integer.parseInt((String)parameters.get("TotalGSMAfiliados"))) != 0 )  {
			return false;
		} else {
			return true;
		}
	}
	
	/**
	 * Funcion Encargada de validar el Estatus de la Cuenta.
	 *
	 * @param parameters Parametros de Entrada.
	 * @return Transfer Object donde se almacena los valores retornados en el Objecto HashMap.
	 */
	public ReturnValue validarEstatusCuenta(Map parameters) {
		ReturnValue returnValue = null;

		if (salesCodes.contains(parameters.get("salesCode")) || ((Integer)parameters.get("isRedirect")).intValue() == new Integer(1).intValue()) {
			/*returnValue = new ReturnValue("300", "Esta en PreRecuperadora, Recuperadora o Redireccionada. SALES_CODE: "
					.concat(parameters.get("salesCode").toString()), "", new HashMap());*///gje
			returnValue = new ReturnValue("300", "Esta en PreRecuperadora, Recuperadora o Redireccionada.", "", new HashMap());
	
		} else {
			returnValue = new ReturnValue("0", "Cuenta ".concat(parameters.get("idCuenta").toString())
					.concat(" VALIDA"), "", new HashMap());
		}
		
		return returnValue;
	}
	
	
	/**
	 * Funcion Encargada de validar si la Cuenta esta redireccionada.
	 *
	 * @param parameters Parametros de Entrada.
	 * @return Transfer Object donde se almacena los valores retornados en el Objecto HashMap.
	 */
	public ReturnValue validarRedirectCuenta(Map parameters) {
		ReturnValue returnValue = null;
		if (!isRedirectStatus(parameters)) {
			logger.info("La funcion RedirectStatus fue FALSO ");//gje
			returnValue = new ReturnValue("0", "Cuenta ".concat(parameters.get("idCuenta").toString())
					.concat(" VALIDA"), "", new HashMap());
		} else {
			logger.info("La funcion RedirectStatus fue TRUE ");//gje
			returnValue = new ReturnValue("300", "Esta en Redireccionada.", "", new HashMap());
		}
		
		return returnValue;
	}
	
	/**
	 * Funcion Encargada de validar el Contexto de la Cuenta con los parametros de Configuracion del properties.
	 *
	 * @param parameters Parametros de Entrada.
	 * @return Transfer Object donde se almacena los valores retornados en el Objecto HashMap.
	 */
	public ReturnValue validarCuenta(Map parameters) {
		ReturnValue returnValue = null;

		logger.info("Ejecutando validarCuenta en KenanDelegator"); //gje
		
		if (parameters.get("salesCode") != null) {
			logger.info("");
		}

		try {
			if (parameters.get("ciclo").equals(parameters.get("cicloProcedure"))) {

				if (vipCodeList.contains(parameters.get("vipCode"))) {

					// if (parameters.get("salesCode") == null || !salesCodes.contains(parameters.get("salesCode"))) {
					
					// FYC: Se valida si la linea en estado Recuperadora posee GSM afliados.
					if (isValidStatus(parameters)) {
						if (parameters.get("noBill").equals("0")) {

							if (!parameters.get("TotalGSMValido").equals("0")) {
								returnValue = new ReturnValue("0", "Cuenta ".concat(parameters.get("idCuenta").toString())
										.concat(" VALIDA"), "", new HashMap());
							} else {
								if (logger.isDebugEnabled())
									logger.debug("Cuenta ".concat(parameters.get("idCuenta").toString()).concat(
											" Invalida. Cuenta Invalida. La cuenta posee solo planes de Suspension o Robo"));

								returnValue = new ReturnValue("300", "Cuenta Invalida. La cuenta posee solo planes de Suspension o Robo", "",
										new HashMap());
							}

						} else {
							if (logger.isDebugEnabled())
								logger.debug("Cuenta ".concat(parameters.get("idCuenta").toString()).concat(
										" Invalida. NO_BILL = ").concat((String) parameters.get("noBill")));

							returnValue = new ReturnValue("300", "Cuenta Invalida. NO_BILL = ".concat((String) parameters
									.get("noBill")), "", new HashMap());
						}
					} else {
						if (logger.isDebugEnabled())
							logger.debug("Cuenta ".concat(parameters.get("idCuenta").toString()).concat(
									" esta en PreRecuperadora o Recuperadora. SALES_CODE: ").concat(
									parameters.get("salesCode").toString()));

						returnValue = new ReturnValue("300", "Esta en PreRecuperadora o Recuperadora. SALES_CODE: "
								.concat(parameters.get("salesCode").toString()), "", new HashMap());
					}
				} else {
					if (logger.isDebugEnabled())
						logger.debug("Cuenta ".concat(parameters.get("idCuenta").toString()).concat(
								" no pertenece a Corporativo/Gubernamental. VIP_CODE: ").concat(
								parameters.get("vipCode").toString()));

					returnValue = new ReturnValue("300", "No pertenece a Corporativo/Gubernamental. VIP_CODE: ".concat(parameters
							.get("vipCode").toString()), "", new HashMap());
				}
			} else {
				if (logger.isDebugEnabled())
					logger.debug("Cuenta ".concat(parameters.get("idCuenta").toString()).concat(" no pertenece al ciclo: ")
							.concat(parameters.get("ciclo").toString()));

				returnValue = new ReturnValue("300", "No pertenece al ciclo: ".concat(parameters.get("ciclo").toString()), "",
						new HashMap());
			}
		} catch (Exception e) {
			logger.error("Error ocurrido durante la validacion en Kenan de la cuenta ".concat(parameters.get("idCuenta")
					.toString()), e);

			returnValue = new ReturnValue("300", "Error ocurrido durante la validacion en Kenan de la cuenta ".concat(parameters
					.get("idCuenta").toString()), "", new HashMap());
		}

		return returnValue;
	}
}