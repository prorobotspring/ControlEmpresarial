package ve.com.digitel.controlemp.parser;

import java.util.Map;

import org.xml.sax.Attributes;
import org.xml.sax.SAXParseException;
import org.xml.sax.helpers.DefaultHandler;

/**
 * Clase RTBSHandler que implementa el Handler para el analisis de archivos XML.
 * 
 * @author Diego Leal - BSS - CC Areas Tecnicas - Integracion
 */
public class RTBSHandler extends DefaultHandler {

	StringBuffer sb;

	String data;

	private Map map;

	private String errorDescription = "";

	private String errorCode = "";

	private String currentState = "";

	private String cosName = "";

	private String nonVoucherRechargeResult = "";

	/**
	 * Constructor Sobre Cargado.
	 * 
	 * @param mapClass
	 */
	public RTBSHandler(Map mapClass) {
		this.sb = new StringBuffer();
		this.data = "";
		map = mapClass;
	}

	/**
	 * Imprime el Error Generado.
	 */
	public void error(SAXParseException saxpe) {
		saxpe.printStackTrace();
	} 

	/**
	 * Imprime el Error Fatal generado.
	 */
	public void fatalError(SAXParseException saxpe) {
		saxpe.printStackTrace();
	} 

	/**
	 * Inicio de Elemento.
	 */
	public void startElement(String uri, String localName, String qName, Attributes attributes) {
		this.data = "";
	} 

	/**
	 * Fin de Elemento.
	 */
	public void endElement(String uri, String localName, String qName) {

		if (qName.equals("ErrorCode")) {
			errorCode = this.data;
		}
		if (qName.equals("ErrorDescription")) {
			errorDescription = this.data;
		}
		if (qName.equals("CurrentState")) {
			currentState = this.data;
		}
		if (qName.equals("COSName")) {
			cosName = this.data;
		}
		if (qName.equals("NonVoucherRechargeResult")) {
			nonVoucherRechargeResult = this.data;
		}
	}

	/**
	 * (non-Javadoc)
	 * 
	 * @see org.xml.sax.ContentHandler#characters(char[], int, int)
	 */
	public void characters(char[] ch, int start, int length) {
		int i;

		sb.delete(0, sb.capacity());
		sb.append(this.data);

		for (i = start; i < start + length; i++) {
			sb.append(ch[i]);
		} // fin de FOR

		this.data = sb.toString();
	}

	/**
	 * Inicio de Documento.
	 */
	public void startDocument() {
		
	}

	/**
	 * Fin de Documento.
	 */
	public void endDocument() {
		map.put("errorCode", errorCode);
		map.put("errorDescription", errorDescription);
		map.put("currentState", currentState);
		map.put("cosName", cosName);
		map.put("nonVoucherRechargeResult", nonVoucherRechargeResult);
	}
}