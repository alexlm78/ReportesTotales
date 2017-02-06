package gt.com.claro.pisa.reportes;

import java.util.ArrayList;
import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;
import gt.com.claro.pisa.reportes.UT100;
import org.alexlm78.kmail.KMail;
//xml
import java.io.File;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.DocumentBuilder;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException; 

/**
 * Compendio de Reportes Generales cotidianos.
 * 
 * @author Alex Lopez Monzon
 * @category Reportes.
 * @version 0.1
 * {@link http://www.alexlm78.org}
 */
@SuppressWarnings("deprecation")
public class Reportes 
{
	/**
	 * Logger de clase
	 */
	private static Logger log = Logger.getLogger(Reportes.class);
	
	// Variables de control para saber que reporte se debe ejecutar.
	private boolean NIETOS = false;
	private boolean CICLOS_DISTINTOS = false;
	private boolean UT100 = false;
	private boolean SIXBELL = false;
	private boolean XLSX = false;
	private boolean MAIL = false;
	private boolean MENU = false;
	private boolean MSANXELA = false;
	private boolean MALISFACT = false;
	private ArrayList<String> fSixbell = new ArrayList<String>();
	private String sMenu = "";
	private Integer iSixbell = -1;
	ArrayList<String> sTo = new ArrayList<String>();
	ArrayList<String> sCc = new ArrayList<String>();
	ArrayList<String> sCco = new ArrayList<String>();
	String sSubject = "";
	String sAttach = "";
	String sBody = "";
	
	public void parseParams(String[] params) 
	{
		for (String param : params) 
		{
			if ( param.equalsIgnoreCase("--xlsx") || param.equalsIgnoreCase("-x"))
				this.XLSX = true;
			else if (param.equalsIgnoreCase("--mail") || param.equalsIgnoreCase("-m"))
				this.MAIL = true;
			else if ( param.equalsIgnoreCase("--help") || param.equalsIgnoreCase("-h"))
				this.Ayuda();
			else if ( param.equalsIgnoreCase("LISTADO"))
				this.Listado();
			else if ( param.equalsIgnoreCase("NIETOS"))
				this.NIETOS = true;
			else if ( param.equalsIgnoreCase("CICLOS_DISTINTOS"))
				this.CICLOS_DISTINTOS = true;
			else if ( param.equalsIgnoreCase("UT100"))
				this.UT100 = true;
			else if ( param.equalsIgnoreCase("MENU"))
				this.MENU = true;
			else if ( param.equalsIgnoreCase("MSANXELA"))
				this.MSANXELA = true;
			else if ( param.equalsIgnoreCase("MALISFACT"))
				this.MALISFACT = true;			
			else if ( param.equalsIgnoreCase("SIXBELL"))		// Reporte de Sixbell
			{
				this.SIXBELL = true;
				this.iSixbell = 0;
			}
			else if ( SIXBELL && iSixbell == 0 )				// Parametros adicionales para el reporte de Sixbell.
				this.fSixbell.add(param);
			else if ( MENU )									// Menu para generar arbol
				this.sMenu = param;
			else
				log.warn("$>>> El parametro "+ param +" no esta reconocido como valido por lo que es ignorado.");
		}
	}
	
	/**
	 * Muestra el listado de los reportes generables.
	 */
	private void Listado()
	{
		String Listado = "Compendio de reportes generales cotidianos." + 
						 "\n"+
						 "Listado de Reportes:\n"+
						 "\tNIETOS:   Telefonos con sus respectivos hijos y nietos.\n"+
						 "\tCICLOS_DISTINTOS:   Telefonos padres e  hijos que facturan en ciclos distintos.\n"+
						 "\tUT100:   Reporte de regularizacion de bajas para UT100.\n"+
						 "\tSIXBELL:   Reporte para reanudacion de servicio Sixbell\n" +
						 "\tMENU:   Reporte del arbol para menus\n"+
						 "\tMSANXELA:   Consolidado para MSAN OUTDOOR Xela"+
						 "\tMALISFACT:  Reporte de Facturacion ciclos 6 y 15 (Malis)";
		System.out.println(Listado);
	}
	
	/**
	 * Muestra una ayuda basica del programa.
	 */
	public void Ayuda()
	{
		String Ayuda =  "Compendio de reportes generales cotidianos." + 
		 				"\n"+
		 				"Uso:\n"+
		 				"\tjava -jar Reportes.jar <REPORTE> \n"+
		 				"\n"+
		 				"Para ver un listado de los reportes generables:" +
		 				"\tjava -jar Reportes.jar LISTADO \n"+
		 				"\n"+
		 				"Para generar en xlsx pasar el parametro --xlsx o -x (no aplica para todos)\n"+
		 				"Para enviar por mail pasar el parametro --mail o -m (no aplica para todos)";
		System.out.println(Ayuda);
	}
	
	public void DoIt()
	{
		if ( this.UT100 )
		{
			log.info("Reporte de regularizacion de bajas para UT100.");
			UT100 ut = new UT100();
			ut.almGestor();
			if ( ut.createRG() && ut.llenarRG() )
			{
				ut.generarArchivo();
				ut.escribirLog(true);  
			}else
				ut.escribirLog(false);
		}else 
		
		if ( this.NIETOS )
		{
			log.info("Telefonos con respectivos hijos y nietos");
			Nietos nt = new Nietos();
			if ( nt.Limpiar() && nt.Procesar() )
				if ( this.XLSX )
					nt.GenerarArchivoXlsx();
				else
					nt.generarArchivo();
			else
				log.error("Occurio un error en el procesamiento de los Nietos (para detalles revisar logs)");
		}
		
		if ( this.MSANXELA )
		{
			log.info("Consolidado para MSAN OUTDORR Xela");
			MsanXela mx = new MsanXela();
			mx.Procesar();
			mx.generarArchivo();
		}
		
		if ( this.MALISFACT )
		{
			log.info("Reporte de Facturacion ciclos 6 y 15 (Malis)");
			MalisFact mf = new MalisFact();
			mf.getDataFile();			
		}
		
		if ( this.CICLOS_DISTINTOS )
		{
			log.info("Telefonos padres e hijos que facturan en ciclos distintos");
			CiclosDistintos cl = new CiclosDistintos();
			if ( cl.ciclosUno() && cl.ciclosDos() )
				log.info("La informacion se encuentra en JL637879/CICLOS2");
			else
				log.error("La informacion no ha podido generarse debido a un error, verifa los logs para mas info.");
		}
		
		if ( this.SIXBELL )
		{
			SixBell sb;
			log.info("Reporte reanudacion de servicio SIXBEL (rvc)");
			if ( fSixbell.size() > 0 )
				sb = new SixBell(this.fSixbell);
			else
				sb = new SixBell();
			
			if ( sb.Procesar() )
			{
				sb.InsertarLog();
				if ( sb.HayRegistros() )
				{
					if ( this.XLSX ) 
						sb.GenerarArchivoXlsx();
					else
						sb.GenerarArchivo();
				
					if ( this.MAIL )
					{
						getMailConf();
						ArrayList<String> mailInfo = new ArrayList<String>();
						mailInfo = org.alexlm78.db.DBConfig.getMailSetting();
						log.debug(mailInfo.get(0)+"-"+mailInfo.get(1));
						KMail mail = new KMail(mailInfo.get(0),mailInfo.get(1),mailInfo.get(2),mailInfo.get(3));
						mail.setAsunto(sSubject);
						mail.addAdjunto(sAttach);
						mail.setTexto(sBody);
						
						//mail.addDestinatario("braulio.tobar@claro.com.gt");
						//mail.addDestinatario("alexlm78@gmail.com");
						for ( int i=0; i<sTo.size(); i++)
							mail.addDestinatario(sTo.get(i));
						
						for ( int i=0; i<sCc.size(); i++)
							mail.addDestinatario(sCc.get(i));
						
						for ( int i=0; i<sCco.size(); i++)
							mail.addDestinatario(sCco.get(i));						
						
						mail.enviar();
						log.info("El archivo SIXRECREP fue enviado por mail!");
					}else
						log.info("Se genero el archivo SIXRECREP@JL637879");
				}else
					log.info("No hay registros para mostrar o enviar.");
			}
			else
				log.error("Error ocurrido en el proceso, favor revisar los logs.");
		}
		
		if ( this.MENU )
		{
			if ( this.sMenu.isEmpty() )
				log.error("Es necesario enviar el menu para el cual se generara el arbol!");
			else
			{
				ArrayList<ArrayList<String>> al = new ArrayList<ArrayList<String>>();
				ArbolMenu ar = new ArbolMenu(this.sMenu);
				al = ar.getArbol(this.sMenu);
				ar.GenerarArchivo(al);
			}
		}
	}
	
	public void getMailConf()
	{
		try
		{
			DocumentBuilderFactory docBuilderFactory = DocumentBuilderFactory.newInstance();
	        DocumentBuilder docBuilder = docBuilderFactory.newDocumentBuilder();
	        Document doc = docBuilder.parse (new File("mail.xml"));
	        
	        doc.getDocumentElement ().normalize ();
            //System.out.println ("Root element of the doc is " + doc.getDocumentElement().getNodeName());
            
            NodeList nList = doc.getElementsByTagName("MAIL");
            for (int temp = 0; temp < nList.getLength(); temp++) 
            {
            	Node nNode = nList.item(temp);
            	//System.out.println(nNode.toString());
            	if (nNode.getNodeType() == Node.ELEMENT_NODE) 
            	{
            		Element eElement = (Element) nNode;
            		
            		sSubject = getTagValue("SUBJECT", eElement);
            		sAttach = getTagValue("ATTACH", eElement);
            		sBody = getTagValue("BODY", eElement);
           		}
            }
            
            NodeList nToList = doc.getElementsByTagName("TO");

            for(int temp = 0 ; temp <nToList.getLength(); temp++)
            {
            	Node nNode = nToList.item(temp);
	            Element eElement = (Element) nNode;
	            NodeList childList = eElement.getChildNodes();
	            
	            for(int i = 0; i < childList.getLength(); i++)
	            {
	            	Node childNode = childList.item(i);
	            	if ( !childNode.getTextContent().trim().isEmpty() )
	            		sTo.add(childNode.getTextContent());
	            }
            }
            
            NodeList nCcList = doc.getElementsByTagName("CC");

            for(int temp = 0 ; temp <nCcList.getLength(); temp++)
            {
            	Node nNode = nCcList.item(temp);
	            Element eElement = (Element) nNode;
	            NodeList childList = eElement.getChildNodes();
	            
	            for(int i = 0; i < childList.getLength(); i++)
	            {
	            	Node childNode = childList.item(i);
	            	if ( !childNode.getTextContent().trim().isEmpty() )
	            		sCc.add(childNode.getTextContent());
	            }
            }
            
            NodeList nCcoList = doc.getElementsByTagName("CCO");

            for(int temp = 0 ; temp <nCcoList.getLength(); temp++)
            {
            	Node nNode = nCcoList.item(temp);
	            Element eElement = (Element) nNode;
	            NodeList childList = eElement.getChildNodes();
	            
	            for(int i = 0; i < childList.getLength(); i++)
	            {
	            	Node childNode = childList.item(i);
	            	if ( !childNode.getTextContent().trim().isEmpty() )
	            		sCco.add(childNode.getTextContent());
	            }
            }
          
		}catch (SAXParseException err) 
		{
			System.out.println ("** Parsing error" + ", line " + err.getLineNumber () + ", uri " + err.getSystemId ());
			System.out.println(" " + err.getMessage ());
		}catch (SAXException e) 
		{
			Exception x = e.getException ();
			((x == null) ? e : x).printStackTrace ();
		}catch (Throwable t) 
		{
			t.printStackTrace ();
		}
	}
	
	private String getTagValue(String sTag, Element eElement) 
	{
	    NodeList nlList = eElement.getElementsByTagName(sTag).item(0).getChildNodes();
	    Node nValue = (Node) nlList.item(0);
	    return nValue.getNodeValue();
	}
	
	/**
	 * Generacion de reportes cotidianos.
	 * 
	 * @param args Parametros de inicio.
	 */
	public static void main(String[] args) 
	{
		PropertyConfigurator.configure("log4j.properties");
		
		Reportes Rep = new Reportes();
		if (args.length > 0)
			Rep.parseParams(args);
		else
			Rep.Ayuda();
		
		Rep.DoIt();		
	}
}
