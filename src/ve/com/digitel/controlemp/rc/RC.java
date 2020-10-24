package ve.com.digitel.controlemp.rc;

import java.io.IOException;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;

import javax.naming.NamingException;

import org.apache.log4j.Logger;
import org.apache.log4j.MDC;

import ve.com.digitel.bssint.EnvironmentUtil;
import ve.com.digitel.bssint.ReturnValue;
import ve.com.digitel.bssint.log.BSSIntLogger;

/**
 * Clase RequestController para el proyecto, se encarga de canalizar las peticiones de servicios a traves de los delegadores.
 * 
 * @author Diego Leal - BSS - CC Areas Tecnicas - Integracion
 * @author Rolando Castaneda - FYCCORP - Consultor de Sistema
 * 
 */
public class RC {

	private static final String TRACEID = "traceid";

	private static Logger logger = BSSIntLogger.getBSSIntLogger(RC.class);

	private static RC uniqueInstance = new RC();

	private Integer statusParcialmente;

	private Integer statusrechazado;

	private String idTipoProceso;

	private Integer idProductoSpec;

	private List states;

	private String idProductoFicticio;

	private String nombreProductoFicticio;

	private String descripcionProductoFicticio;

	private String costoVigenteProductoFicticio;

	private Map listaProductos;

	private List statusCuentaKenan;

	private List statusGSMPrepago;

	private int cantCuentasKenanProcesar;

	private int cantGSMProcesados;

	private long tiempoEsperaReintento;

	/**
	 * Devuelve la instancia de RC
	 * 
	 * @return objeto RC
	 */
	public static RC getInstance() {
		return uniqueInstance;
	}

	/**
	 * Constructor por Defecto.
	 */
	public RC() {
		ResourceBundle rsr = EnvironmentUtil.getResourceAsProperties("config.properties");

		statusParcialmente = new Integer(rsr.getString("application.controlemp.procedure.crearProcesoContrato.statusejecutadoparcialmente"));
		statusrechazado = new Integer(rsr.getString("application.controlemp.procedure.crearProcesoContrato.statusrechazado"));
		idTipoProceso = rsr.getString("application.controlemp.procedure.crearProceso.idtipoproceso");
		idProductoSpec = new Integer(rsr.getString("application.controlemp.procedure.crearProceso.idproductospec"));
		states = Arrays.asList(rsr.getString("application.controlemp.rtbs.states").trim().split(" "));
		idProductoFicticio = rsr.getString("application.controlemp.procedure.crearnotabatchdebito.idproductoficticio");
		nombreProductoFicticio = rsr.getString("application.controlemp.procedure.crearnotabatchdebito.nombreproductoficticio");
		descripcionProductoFicticio = rsr.getString("application.controlemp.procedure.crearnotabatchdebito.descripcionproductoficticio");
		costoVigenteProductoFicticio = rsr.getString("application.controlemp.procedure.crearnotabatchdebito.costoVigenteproductoficticio");
		tiempoEsperaReintento = new Long(rsr.getString("application.controlemp.nrc.reintento.tiempoespera")).longValue();
		statusCuentaKenan = new ArrayList();
		statusGSMPrepago = new ArrayList();
		cantCuentasKenanProcesar = 0;
		cantGSMProcesados = 0;
	}

	/**
	 * Funcion encargada de ejecutar el PreCierre de las Cuentas Postpago acorde al Ciclo de Facturacion.
	 * 
	 * @param ciclo Los valores para el ciclo pueden ser M03 o M05.
	 * @return Transfer Object donde se almacena los valores retornados en el Objecto HashMap.
	 */
	public ReturnValue procesarPreCierre(String ciclo) {
		putLogTraceId(String.valueOf(System.currentTimeMillis()).concat("_").concat(ciclo));

		logger.info("INICIO de proceso mensual CONTROL EMPRESARIAL");

		ReturnValue returnValue = null;
		
		try {
			Map parameters = new HashMap();
			parameters.put("ciclo", ciclo);
			parameters.put("diaCiclo", ciclo.substring(1));//gje
			parameters.put("diaCorte", String.valueOf(Integer.valueOf(ciclo.substring(1)).intValue() - 1));//gje
			parameters.put("idTipoProceso", idTipoProceso);
			parameters.put("idProductoSpec", idProductoSpec);
	
			returnValue = RCHelper.getInstance().executeProcCrearProceso(parameters);

			parameters.put("idProceso", returnValue.getVto().get("idProceso"));
			logger.info("Id de proceso generado: " + returnValue.getVto().get("idProceso"));

			if (returnValue.getCondCode().equals("0")) {
				
				// FYC:  Se actualizo la logica del Procedure para colocar un parametro adicional donde lineas con estado Recuperadora posee la cantidad de GSM afiliados.
				returnValue = RCHelper.getInstance().executeProcObtenerContratosPreCerrar(parameters);

				if (returnValue.getCondCode().equals("0")) {
					ResultSet rs = (ResultSet) returnValue.getVto().get("cursor");
					CallableStatement cs = (CallableStatement) returnValue.getVto().get("cs");
					Connection conn = (Connection) returnValue.getVto().get("conn");

					if (rs.next()) {
						logger.info("Inicio de procesamiento de contratos por PRE-CERRAR");

						do {
							cantCuentasKenanProcesar++;

							parameters.put("idContrato", new Integer(rs.getInt(1)));
							parameters.put("idCuenta", rs.getString(2));
							parameters.put("cicloProcedure", rs.getString(3));
							parameters.put("salesCode", rs.getString(4));
							parameters.put("vipCode", rs.getString(5));
							parameters.put("noBill", rs.getString(6));
							parameters.put("TotalGSMValido", rs.getString(7));
							parameters.put("TotalGSMAfiliados", rs.getString(8)); // Indica la cantidad de lineas que han sido afiliadas
							
							// FYC: Se agrego la validacion para identificar cuentas con estado Recuperadora	
							returnValue = RCHelper.getInstance().executeValidarCuentaKenan(parameters);
							
							if (returnValue.getCondCode().equals("0")) {

								returnValue = RCHelper.getInstance().executeProcIsDirectNRC(parameters);
								parameters.put("isRedirect", returnValue.getVto().get("isRedirect"));
								
								if (returnValue.getCondCode().equals("0")) {

									// FYC : Se encarga de realizar la validacion de la cuenta.
									returnValue = RCHelper.getInstance().validarRedirectCuenta(parameters);
									
									if (returnValue.getCondCode().equals("0")) {
										
										returnValue = RCHelper.getInstance().executeValidarCuentaSIR(parameters);
										
										if (returnValue.getCondCode().equals("0")) {
											if (returnValue.getVto().get("existe").equals("N")) {
												
												parameters.put("estado", statusParcialmente);
												RCHelper.getInstance().executeProcCrearProcesoContrato(parameters);

												logger.info("La Cuenta: " + parameters.get("idCuenta")
														+ " ha sido actualizada con status Parcialmente Ejecutado");
												
												// FYC : Se encarga de realizar la validacion de la cuenta.
												returnValue = RCHelper.getInstance().executeValidarEstatus(parameters);
												
												if (returnValue.getCondCode().equals("0")) {
												
													returnValue = RCHelper.getInstance().executeProcObtenerBeneficiario(parameters);
													
													ResultSet result = null;
													CallableStatement callable = null;
													Connection connection = null;
													
													if (returnValue.getCondCode().equals("0")) {
														result = (ResultSet) returnValue.getVto().get("cursor");
														callable = (CallableStatement) returnValue.getVto().get("cs");
														connection = (Connection) returnValue.getVto().get("conn");
														
														while (result.next()) {
														
															returnValue = RCHelper.getInstance().executeProcCrearDetalleProceso(parameters, result.getInt(1), result.getString(3));
														}
													} else {
														logger
																.error("Error ocurrido durante la obtencion de beneficiarios para la cuenta: "
																		.concat(parameters.get("idCuenta").toString()));
													}
													if (result != null)
														result.close();
													if (callable != null)
														callable.close();
													if (connection != null)
														connection.close();
												}
												
											} else {
												logger.info("Cuenta: " + parameters.get("idCuenta") + " existe en Lista Negra");

												parameters.put("estado", statusrechazado); // RECHAZADO
												RCHelper.getInstance().executeProcCrearProcesoContrato(parameters);

												Map map = new HashMap();
												map.put("idCuenta", parameters.get("idCuenta"));
												map.put("razon", "Cuenta: " + parameters.get("idCuenta") + " existe en SIR");
												statusCuentaKenan.add(map);
											}
										} else {
											logger.info("Cuenta: " + parameters.get("idCuenta")
													+ ". ERROR consultando SIR. ".concat(returnValue.getReasonCode()));

											parameters.put("estado", statusrechazado); // RECHAZADO
											RCHelper.getInstance().executeProcCrearProcesoContrato(parameters);

											Map map = new HashMap();
											map.put("idCuenta", parameters.get("idCuenta"));
											map.put("razon", returnValue.getReasonCode());
											statusCuentaKenan.add(map);
										}
									} else {
										logger.info("Cuenta: " + parameters.get("idCuenta")
												+ " Redireccionada. ".concat(returnValue.getReasonCode()));

										parameters.put("estado", statusrechazado); // RECHAZADO
										RCHelper.getInstance().executeProcCrearProcesoContrato(parameters);

										Map map = new HashMap();
										map.put("idCuenta", parameters.get("idCuenta"));
										map.put("razon", "Cuenta redireccionada");
										statusCuentaKenan.add(map);
									}
								} else {
									logger.info("Cuenta: " + parameters.get("idCuenta")
											+ ". ERROR consultando Redireccionamiento. ".concat(returnValue.getReasonCode()));

									parameters.put("estado", statusrechazado); // RECHAZADO
									RCHelper.getInstance().executeProcCrearProcesoContrato(parameters);

									Map map = new HashMap();
									map.put("idCuenta", parameters.get("idCuenta"));
									map.put("razon", returnValue.getReasonCode());
									statusCuentaKenan.add(map);
								}
							} else {
								logger.info(returnValue.getReasonCode());

								parameters.put("estado", statusrechazado); // RECHAZADO
								RCHelper.getInstance().executeProcCrearProcesoContrato(parameters);

								Map map = new HashMap();
								map.put("idCuenta", parameters.get("idCuenta"));
								map.put("razon", returnValue.getReasonCode());
								statusCuentaKenan.add(map);
							}

						} while (rs.next());
						returnValue = new ReturnValue("0", "Finalizacion de proceso de PRE-CIERRE", "", new HashMap());
					} else {
						logger.info("No existen cuentas por PRE-CERRAR");
					}

					logger.info("### Resultado Ejecucion PRE-CIERRE: " + returnValue);

					if (rs != null)
						rs.close();
					if (cs != null)
						cs.close();
					if (conn != null)
						conn.close();
				} else {
					logger.info("El proceso terminará debido a un error obteniendo contratos por PRE CERRAR");
				}

				returnValue = RC.getInstance().procesarCierre(parameters.get("ciclo").toString(),
						parameters.get("idProceso").toString());

				logger.info("### Resultado Ejecucion CIERRE: " + returnValue);
			}
		} catch (IOException e) {
			logger.error("Error ocurrido durante la ejecucion del proceso Control Empresarial", e);
			returnValue = new ReturnValue("100", "Error ocurrido durante la ejecucion del proceso Control Empresarial", e
					.getMessage(), new HashMap());

		} catch (SQLException e) {
			logger.error("Error ocurrido durante la ejecucion del proceso Control Empresarial", e);
			returnValue = new ReturnValue("100", "Error ocurrido durante la ejecucion del proceso Control Empresarial", e
					.getMessage(), new HashMap());

		} catch (NamingException e) {
			logger.error("Error ocurrido durante la ejecucion del proceso Control Empresarial", e);
			returnValue = new ReturnValue("100", "Error ocurrido durante la ejecucion del proceso Control Empresarial", e
					.getMessage(), new HashMap());

		} catch (Exception e) {
			logger.error("Error ocurrido durante la ejecucion del proceso Control Empresarial", e);
			returnValue = new ReturnValue("100", "Error ocurrido durante la ejecucion del proceso Control Empresarial", e
					.getMessage(), new HashMap());

		}

		logger.info("FIN de proceso mensual CONTROL EMPRESARIAL");

		return returnValue;
	}

	/**
	 * Metodo en cargado de realizar el Cierre.
	 * 
	 * @param ciclo Los valores para el ciclo pueden ser M03 o M05.
	 * @param idProceso Identificador del Proceso.
	 * @return Transfer Object donde se almacena los valores retornados en el Objecto HashMap.
	 */
	public ReturnValue procesarCierre(String ciclo, String idProceso) {

		ReturnValue returnValue = null;
		Map parameters = new HashMap();
		parameters.put("ciclo", ciclo);
		parameters.put("diaCiclo", ciclo.substring(1));//gje
		parameters.put("diaCorte", String.valueOf(Integer.valueOf(ciclo.substring(1)).intValue() - 1));//gje
		parameters.put("idProductoSpec", idProductoSpec);
		parameters.put("idProceso", idProceso);

		try {
			
			
			// Obteniendo la lista de Productos (Planes)
			returnValue = RCHelper.getInstance().executeProcObtenerProducto(parameters);

			if (returnValue.getCondCode().equals("0")) {
				listaProductos = returnValue.getVto();
			} else {
				return returnValue;
			}

			returnValue = RCHelper.getInstance().executeProcObtenerContratosCerrar(parameters);

			if (returnValue.getCondCode().equals("0")) {
				
				ResultSet rs = (ResultSet) returnValue.getVto().get("cursor");
				CallableStatement cs = (CallableStatement) returnValue.getVto().get("cs");
				Connection conn = (Connection) returnValue.getVto().get("conn");

				if (rs.next()) {
					logger.info("Inicio de procesamiento de contratos por CERRAR");

					do {
						parameters.put("idContrato", new Integer(rs.getInt(1)));

						logger.info("Inicio de procesamiento de contrato: " + rs.getInt(1));

						returnValue = RCHelper.getInstance().executeProcCrearBatchDebito(parameters);

						if (returnValue.getCondCode().equals("0")) {

							parameters.put("idBatchDebito", returnValue.getVto().get("idBatchDebito"));

							returnValue = RCHelper.getInstance().executeProcObtenerBeneficiarioCierre(parameters);
							
							if (returnValue.getCondCode().equals("0")) {
								
								ResultSet result = (ResultSet) returnValue.getVto().get("cursor");
								CallableStatement callable = (CallableStatement) returnValue.getVto().get("cs");
								Connection connection = (Connection) returnValue.getVto().get("conn");

								if (result.next()) {
									do {
										
										if (logger.isDebugEnabled())
											logger.debug("CONTADOR GSM: " + cantGSMProcesados);

										parameters.put("cuentaKenan", result.getString(1));
										parameters.put("gsm", result.getString(2));
										parameters.put("idDetalleProceso", new Integer(result.getInt(3)));

										returnValue = RCHelper.getInstance().executeRetrieveSubscriberRTBS(parameters);

										if (returnValue.getCondCode().equals("0")) {
											
											String plan = (String) returnValue.getVto().get("cosName");
											String estatus = (String) returnValue.getVto().get("currentState");

											//parameters.put("plan", "Plan ".concat(plan)); //gje
											parameters.put("plan", plan);
											parameters.put("estatus", estatus);
											
											//FYC: Se realizo el cambio para la invocacion al Procedure y validar el Plan.
											returnValue = RCHelper.getInstance().executeProcValidarPlanTelefonia(parameters);
											
											if (returnValue.getCondCode().equals("0")) {
											
												if (((Integer)returnValue.getVto().get("validPlan")).intValue() == 1 ) { // Verifica si El Plan Concuerda
													
													parameters.put("producto", listaProductos.get("Plan " + plan));
													
													if (states.contains(estatus)) {
														
														// Aplicando recarga en RTBS
														returnValue = RCHelper.getInstance().executeRechargeSubscriberRTBS(parameters);
														
														if (returnValue.getCondCode().equals("0")) {
															parameters.put("idEstadoCredito", new Integer(4));
															parameters.put("idEstadoDebito", new Integer(2));
			
															// Actualizando nota debito
															RCHelper.getInstance().executeProcCrearNotaBatchDebito(parameters);
			
															cantGSMProcesados++;
														} else {
															parameters.put("idEstadoCredito", new Integer(3));
															parameters.put("idEstadoDebito", new Integer(3));
			
															RCHelper.getInstance().executeProcCrearNotaBatchDebito(parameters);
			
															logger.error("GSM: ".concat("").concat(" no recargado. ").concat(
																	returnValue.getReasonCode()));
			
															Map map = new HashMap();
															map.put("cuentaKenan", result.getString(1));
															map.put("gsm", result.getString(2));
															map.put("razon", "No se aplico beneficio al GSM. ".concat(returnValue.getReasonCode()));
															statusGSMPrepago.add(map);
															
															cantGSMProcesados++;
														}
			
													} else {
														parameters.put("idEstadoCredito", new Integer(3));
														parameters.put("idEstadoDebito", new Integer(3));
			
														RCHelper.getInstance().executeProcCrearNotaBatchDebito(parameters);
			
														logger.error("El GSM: ".concat("gsm").concat(
																" se encuentra en STATUS invalido. State: ").concat(estatus));
			
														Map map = new HashMap();
														map.put("cuentaKenan", result.getString(1));
														map.put("gsm", result.getString(2));
														map.put("razon", "El GSM se encuentra en STATUS invalido. State: "
																.concat(estatus));
														statusGSMPrepago.add(map);
														
														cantGSMProcesados++;
													}
			
												} else {
													parameters.put("idEstadoCredito", new Integer(3));
													parameters.put("idEstadoDebito", new Integer(3));
													Map producto = new HashMap();
													producto.put("idProducto", new Integer(idProductoFicticio));
													producto.put("nombreProducto", nombreProductoFicticio);
													producto.put("descripcionProducto", descripcionProductoFicticio);
													producto.put("costoVigenteProducto", costoVigenteProductoFicticio);
			
													parameters.put("producto", producto);
			
													RCHelper.getInstance().executeProcCrearNotaBatchDebito(parameters);
			
													logger.error("El GSM: ".concat("").concat(" no posee algunos de los productos CE. Plan: ").concat(plan));
			
													Map map = new HashMap();
													map.put("cuentaKenan", result.getString(1));
													map.put("gsm", result.getString(2));
													map.put("razon", "El GSM no posee algunos de los productos CE. Plan: ".concat(plan));
													statusGSMPrepago.add(map);
													
													cantGSMProcesados++;
												}
											} else {
												logger.error("Error validando el Plan Asociado al Tipo de Telefonia. ".concat(returnValue.getReasonCode())
														+ " - IdContrato: " + parameters.get("idContrato"));
											}
										} else {
											parameters.put("idEstadoCredito", new Integer(3));
											parameters.put("idEstadoDebito", new Integer(3));
											Map producto = new HashMap();
											producto.put("idProducto", new Integer(idProductoFicticio));
											producto.put("nombreProducto", nombreProductoFicticio);
											producto.put("descripcionProducto", descripcionProductoFicticio);
											producto.put("costoVigenteProducto", costoVigenteProductoFicticio);

											parameters.put("producto", producto);

											RCHelper.getInstance().executeProcCrearNotaBatchDebito(parameters);

											logger.error("Error consultando el gsm: " + parameters.get("gsm")
													+ " en RTBS. ".concat(returnValue.getReasonCode()));

											Map map = new HashMap();
											map.put("cuentaKenan", result.getString(1));
											map.put("gsm", result.getString(2));
											map.put("razon", returnValue.getReasonCode());
											statusGSMPrepago.add(map);
											
											cantGSMProcesados++;
										}
									} while (result.next());

								} else {
									
									returnValue = RCHelper.getInstance().executeObtenerCuentaKenanDelegator(parameters);
									
									if (returnValue.getCondCode().equals("0")) {
										parameters.put("cuentaKenan", returnValue.getVto().get("cuentaKenan"));
									}
									
									logger.info("No existen Beneficiarios");
								}
								

								if (result != null)
									result.close();
								if (callable != null)
									callable.close();
								if (connection != null)
									connection.close();
									
								// Obteniendo el monto de los cargos a aplicados en RTBS
								returnValue = RCHelper.getInstance().executeProcObtenerMontosCargo(parameters);

								ResultSet res = null;
								CallableStatement callab = null;
								Connection connecti = null;

								if (returnValue.getCondCode().equals("0")) {
									
									res = (ResultSet) returnValue.getVto().get("cursor");
									callab = (CallableStatement) returnValue.getVto().get("cs");
									connecti = (Connection) returnValue.getVto().get("conn");

									if (res.next()) {
										logger.info("INICIANDO cobro en Kenan");

										do {
											parameters.put("montoCargado", res.getString(1));
											parameters.put("idProducto", new Integer(res.getInt(2)));
											parameters.put("typeIdNRC", res.getString(3));

											returnValue = RCHelper.getInstance().executeProcAgregarNRC(parameters);

											if (returnValue.getCondCode().equals("0")) {
												parameters.put("idEstadoDebito", new Integer(4));
												returnValue = RCHelper.getInstance().executeProcActualizarNotaDebito(parameters);
											} else {
												logger.error("Error ocurrido durante la creacion del NRC. "
														+ returnValue.getReasonCode());

												logger.info("Cuenta Kenan: " + parameters.get("cuentaKenan")
														+ ". Transcurriendo " + tiempoEsperaReintento
														+ " mseg para REINTENTAR la creación del NRC... ");

												try {
													Thread.sleep(tiempoEsperaReintento);
												} catch (InterruptedException e) {
													logger.error("Error ocurrido durante el tiempo de espera del reintento",
															e);
												}

												returnValue = RCHelper.getInstance().executeProcAgregarNRC(parameters);

												if (returnValue.getCondCode().equals("0")) {
													parameters.put("idEstadoDebito", new Integer(4));
													returnValue = RCHelper.getInstance().executeProcActualizarNotaDebito(
															parameters);
												} else {
													Map map = new HashMap();
													map.put("idCuenta", parameters.get("cuentaKenan"));
													map.put("razon", returnValue.getReasonCode() + ". Parametros: "
															+ parameters);
													statusCuentaKenan.add(map);
												}
											}
										} while (res.next());
									} else {
										parameters.put("montoCargado", "0");
										parameters.put("idProducto", new Integer(0));
										parameters.put("typeIdNRC", "");
										parameters.put("idEstadoDebito", new Integer(3));
										returnValue = RCHelper.getInstance().executeProcActualizarNotaDebito(parameters);
									}

								} else {
									logger.error("ERROR consultando los montos a cargar en Kenan. ".concat(returnValue
											.toString()));
									Map map = new HashMap();
									map.put("idCuenta", parameters.get("cuentaKenan"));
									map.put("razon", returnValue.getReasonCode());
									statusCuentaKenan.add(map);
								}
								
								if (res != null)
									res.close();
								if (callab != null)
									callab.close();
								if (connecti != null)
									connecti.close();
								
							} else {
								logger.error("Error obteniendo beneficiario cierre. ".concat(returnValue.getReasonCode())
										+ " - IdContrato: " + parameters.get("idContrato"));
							}
						} else {
							logger.error("Error creando Batch Debito. ".concat(returnValue.getReasonCode()) + " - IdContrato: "
									+ parameters.get("idContrato"));
						}

						returnValue = RCHelper.getInstance().executeProcCerrarContrato(parameters);

						logger.info("Cierre de Contratos: ".concat(returnValue.toString()));

					} while (rs.next());
				} else {
					logger.info("No existen Procesos por CERRAR");
					returnValue = new ReturnValue("200", "No existen Procesos por CERRAR", "", new HashMap());
				}

				if (rs != null)
					rs.close();
				if (cs != null)
					cs.close();
				if (conn != null)
					conn.close();

			} else {
				logger.info("El proceso terminara debido a un error obteniendo contratos por PRE CERRAR");
			}
		} catch (IOException e) {
			logger.error("Error ocurrido durante la ejecucion del proceso Control Empresarial", e);
			returnValue = new ReturnValue("100", "Error ocurrido durante la ejecucion del proceso Control Empresarial", e
					.getMessage(), new HashMap());

		} catch (SQLException e) {
			logger.error("Error ocurrido durante la ejecucion del proceso Control Empresarial", e);
			returnValue = new ReturnValue("100", "Error ocurrido durante la ejecucion del proceso Control Empresarial", e
					.getMessage(), new HashMap());

		} catch (NamingException e) {
			logger.error("Error ocurrido durante la ejecucion del proceso Control Empresarial", e);
			returnValue = new ReturnValue("100", "Error ocurrido durante la ejecucion del proceso Control Empresarial", e
					.getMessage(), new HashMap());

		}

		return returnValue;
	}

	/**
	 * Funcion encargada de Enviar Email.
	 * 
	 * @param procesarPreCierre Transfer Object donde se almacena los valores retornados en el Objecto HashMap.
	 * @param cicloFacturacion
	 * 
	 */
	private void enviarCorreoResultado(ReturnValue procesarPreCierre, String cicloFacturacion) {
		StringBuffer sb = new StringBuffer();

		sb.append("\tRESULTADO DE EJECUCION DE LOS PROCESOS DE PRECIERRE Y CIERRE - CICLO: ");
		sb.append(cicloFacturacion);
		sb.append("\n\n");
		sb.append("\tCantidad de cuentas postpago procesadas: ");
		sb.append(cantCuentasKenanProcesar);
		sb.append("\n");
		sb.append("\tCantidad de cuentas postpago fallidas: ");
		sb.append(statusCuentaKenan.size());
		sb.append("\n");
		sb.append("\tCantidad de gsm prepago procesados: ");
		sb.append(cantGSMProcesados);
		sb.append("\n");
		sb.append("\tCantidad de gsm prepago fallidos: ");
		sb.append(statusGSMPrepago.size());
		sb.append("\n\n");

		if (statusCuentaKenan.size() > 0) {
			sb.append("\tREPORTE DE CUENTAS KENAN FALLIDAS\n\n");
			sb.append("\tCUENTA\tRAZON\n");
			for (int i = 0; i < statusCuentaKenan.size(); i++) {
				sb.append("\t");
				sb.append(((Map) statusCuentaKenan.get(i)).get("idCuenta"));
				sb.append("\t");
				sb.append(((Map) statusCuentaKenan.get(i)).get("razon"));
				sb.append("\n");
			}
		}

		if (statusGSMPrepago.size() > 0) {
			sb.append("\n\tREPORTE DE GSMs PREPAGO FALLIDOS\n\n");
			sb.append("\tCUENTA\tGSM         \tRAZON\n");
			for (int i = 0; i < statusGSMPrepago.size(); i++) {
				sb.append("\t");
				sb.append(((Map) statusGSMPrepago.get(i)).get("cuentaKenan"));
				sb.append("\t");
				sb.append(((Map) statusGSMPrepago.get(i)).get("gsm"));
				sb.append("\t");
				sb.append(((Map) statusGSMPrepago.get(i)).get("razon"));
				sb.append("\n");
			}
		}

		sb.append("\nInformacion Tecnica sobre ultimo status de la aplicacion: ");
		sb.append(procesarPreCierre.toString());

		RCHelper.getInstance().executeSendMail(sb.toString(), cicloFacturacion);
	}

	/**
	 * Genera un TraceId por cada solicitud
	 * 
	 * @param traceId
	 */
	private void putLogTraceId(String traceId) {
		MDC.put(TRACEID, traceId);
	}

	/**
	 * Metodo Principal para dar inicio al Proceso de Control Empresarial.
	 * 
	 * @param args
	 */
	public static void main(String[] args) {
		try {
			logger.info("################## CONTROL EMPRESARIAL ##################");
			if (args != null) {
				logger.info("Cantidad de parametros recibidos: " + args.length);
				for (int i = 0; i < args.length; i++) {
					logger.info("args[" + i + "] = " + args[i]);
				}
			} else {
				logger.info("No se recibieron parametros de entrada");
				System.exit(1);
			}

			if (args.length >= 2) {
				if (args[0].equalsIgnoreCase("Cierre") || args[0].equalsIgnoreCase("PreCierre")) {
					if (args[1].equals("M03") || args[1].equals("M05")) {
						if (args[0].equalsIgnoreCase("Cierre")) {
							if (args.length == 3) {
								ReturnValue procesarCierre = RC.getInstance().procesarCierre(args[1], args[2]);
								logger.info(procesarCierre);

								RC.getInstance().enviarCorreoResultado(procesarCierre, args[1]);

								if (procesarCierre.getCondCode().equals("0"))
									System.exit(0);
								else
									System.exit(1);
							} else {
								logger.error("Para ejecutar el cierre debe ingresar el ID proceso asociado");
							}
						} else {
							ReturnValue procesarPreCierre = RC.getInstance().procesarPreCierre(args[1]);
							logger.info(procesarPreCierre);

							RC.getInstance().enviarCorreoResultado(procesarPreCierre, args[1]);

							if (procesarPreCierre.getCondCode().equals("0"))
								System.exit(0);
							else
								System.exit(1);
						}
					} else {
						logger.error("Los valores para el ciclo deben ser M03 o M05");
					}
				} else {
					logger.error("Debe ingresar el proceso a ejecutar (PreCierre o Cierre) como primer parametro de ejecucion");
				}
			} else {
				logger.error("Debe ingresar el tipo de Proceso y el CICLO (M03 - M05) como argumentos para la ejecucion");
			}
		} catch (Exception e) {
			logger.error("Error ocurrido durante la ejecución de los procesos de PreCierre y Cierre", e);
			System.exit(1);
		}
	}
}