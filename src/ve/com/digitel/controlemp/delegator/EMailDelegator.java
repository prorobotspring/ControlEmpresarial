package ve.com.digitel.controlemp.delegator;

import java.util.Properties;
import java.util.ResourceBundle;

import javax.mail.Message;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

import org.apache.log4j.Logger;

import ve.com.digitel.bssint.EnvironmentUtil;
import ve.com.digitel.bssint.log.BSSIntLogger;

/**
 * Clase EMailDelegator implementada con el Patron Singlenton que se encarga de realizar el envio de Correos electronicos.
 * 
 * @author Diego Leal - BSS - CC Areas Tecnicas - Integracion
 * @author Rolando Castaneda - FYCCORP - Consultor de Sistema
 */
public class EMailDelegator {

	private static Logger logger = BSSIntLogger.getBSSIntLogger(EMailDelegator.class);

	private static EMailDelegator emd = new EMailDelegator();

	private String mailHost;

	private String from;

	private String to;

	private String subject;

	/**
	 * Constructor de la Clase.
	 */
	private EMailDelegator() {
		ResourceBundle rsr = EnvironmentUtil.getResourceAsProperties("config.properties");

		mailHost = rsr.getString("application.controlemp.mail.mailhost");
		from = rsr.getString("application.controlemp.mail.from");
		to = rsr.getString("application.controlemp.mail.to");
		subject = rsr.getString("application.controlemp.mail.subject");
	}

	/**
	 * Funcion encargada de retornar la instancia de la Clase.
	 * 
	 * @return EMailDelegator Instancia de la Clase.
	 */
	public static EMailDelegator getInstance() {
		return emd;
	}

	/**
	 * Funcion Encargada del envio de Email.
	 * 
	 * @param body Cuerpo del Correo Electronico.
	 * @param cicloFacturacion Ciclo de Facturacion.
	 */
	public void sendMail(String body, String cicloFacturacion) {
		if (body != null) {
			try {
				Properties props = System.getProperties();

				props.put("mail.smtp.host", mailHost);

				Session session = Session.getInstance(props, null);

				Message message = new MimeMessage(session);
				message.setFrom(new InternetAddress(from));

				String[] split = to.split(";");

				InternetAddress[] ia = new InternetAddress[split.length];
				for (int i = 0; i < split.length; i++) {
					ia[i] = new InternetAddress(split[i]);
				}

				message.setRecipients(Message.RecipientType.TO, ia);
				message.setSubject(subject.concat(" ").concat(cicloFacturacion));
				message.setContent(body, "text/plain");

				Transport.send(message);

				logger.info("Correo enviado con exito");

			} catch (Throwable t) {
				logger.error("Error durante el envion del email", t);
			}
		} else {
			logger.info("No se tiene un mensaje para enviar");
		}
	}
}