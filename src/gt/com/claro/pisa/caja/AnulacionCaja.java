package gt.com.claro.pisa.caja;

import java.io.File;
import java.io.FileOutputStream;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Properties;
import org.alexlm78.db.DBTask;
import org.alexlm78.kmail.KMail;
import org.alexlm78.utils.Configurador;
import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import com.ibm.as400.data.XmlException;

/**
 * Reporte de Anulaciones de Cajas (envio automatico a lista de usuarios)
 * 
 * @author Alejandro L�pez Monz�n
 * @version 1.0
 * @category Report Generators
 */
public class AnulacionCaja 
{
	/**
	 * Logger de clase
	 */
	private static Logger log = Logger.getLogger(AnulacionCaja.class);
	// Connections variables
	private DBTask db;
	ArrayList<String> Querys = new ArrayList<String>();	
	
	// Debuger
	Boolean DEBUG=false;	
	
	/**
	 * Constructor
	 */
	public AnulacionCaja()
	{
		try 
		{
			// Obtenemos los datos de acceso a la DB desde su configuracion.
			Properties props = Configurador.getPropiedades("PISA");
			String ipConnection = props.getProperty("IP");
			String usrConn = props.getProperty("User");
			String pasConn = props.getProperty("Pass");
			String schema = props.getProperty("Schema");
			DEBUG = (props.getProperty("Debug")!=null && props.getProperty("Debug").compareToIgnoreCase("true")==0)?true:false;
			
			// Definimos la forma de conexion de la clase
			String urlConn = "jdbc:as400://"+ ipConnection + "/;libraries="+ schema + ",PASO,GUAV1,GUARDBV1,TFSOBMX1,QTEMP;prompt=false;naming=sql;errors=full";
			db = new DBTask("com.ibm.as400.access.AS400JDBCDriver");
			db.Conectar(urlConn, usrConn, pasConn);
		}catch ( Exception ex )
		{
			log.error(ex.getMessage());
		}
	}
	
	/**
	 * Report Generator
	 * 
	 * @return success or fail
	 */
	public boolean Generar()
	{
		boolean bRes = false;
		
		Date hoy = Calendar.getInstance().getTime();
		long tiempoActual = hoy.getTime();
		long unDia = 1 * 24 * 60 * 60 * 1000;
		Date ayer = new Date(tiempoActual - unDia);
		DateFormat sdf = new SimpleDateFormat("yyyyMMdd");
		String sAyer = sdf.format(ayer);
		
		if ( !this.Existe("PASO.ANUCAJAS") )
			Querys.add("CREATE TABLE PASO.ANUCAJAS ( TIENDA CHARACTER (3), CAJA CHARACTER (10), SEC NUMERIC (5), CONCEPTO CHARACTER (11), TPO_PAGO NUMERIC (2), MONTO DECIMAL(11,2), FORMA_PAGO CHARACTER (3), USUARIO CHARACTER (10), NOMBRE VARCHAR (40) )");
		else
			Querys.add("DELETE FROM PASO.ANUCAJAS WHERE 1=1");
		
		Querys.add("INSERT INTO PASO.ANUCAJAS SELECT CJANUOFI TIENDA, CJANUCAJ CAJA, CJANUSEC SEC,CJANUCON CONCEPTO,CJANUTIP  TPO_PAGO, CJANUIMP MONTO, CJANUFOR FORMA_PAGO, CJANUUSU USUARIO, LTRIM(B.USRMH2) NOMBRE FROM GUAV1.CJANULA A LEFT OUTER JOIN GUAV1.MNUSER B ON A.CJANUUSU=B.USRID WHERE CJANUSEC<>0 AND CJANUFEC="+sAyer+" AND CJANUOFI NOT IN ( SELECT COCOD FROM GUAV1.SVCOIC WHERE COCSTI <>'A' ) AND SUBSTR(CJANUCON,1,5)<>'TOTAL' GROUP BY CJANUOFI, CJANUCAJ, CJANUSEC,CJANUCON,CJANUTIP,CJANUIMP,CJANUFOR,CJANUUSU, B.USRMH2 ORDER BY CJANUOFI");
		
		log.info("Procesando...");
		try
		{
			Connection con = db.getConexion();
			if(DEBUG) log.debug(con.toString());
			
			if ( con != null )
			{
				for( String query : Querys )
				{
					if(DEBUG) log.debug("Procesando " + query);
					Statement st = con.createStatement();
					st.executeUpdate(query);
					bRes=true;		
					st.close();
				}
			}
			log.info("Finalizado.");
		}catch ( SQLException ex )
		 {
			bRes=false;
			log.error(ex.getMessage());
		 }
		
		return bRes;
	}
	
	/**
	 * Genera el archivo en formato XLSX (Excel 2007+)
	 */
	protected void GenerarArchivoXlsx() throws XmlException 
	{
		log.info("Generando archivo xlsx ...");
		String query = "SELECT * FROM PASO.ANUCAJAS";
		String sArch = "AnulacionCajas.xlsx";
 		try
 		{
			XSSFWorkbook wb = new XSSFWorkbook();
			XSSFSheet sheet = wb.createSheet("Data");
			                   
			Connection con = db.getConexion();
	 		Statement st = con.createStatement();
	 		ResultSet rs = st.executeQuery(query);
	 		
	 		int cols = rs.getMetaData().getColumnCount();
	 		int fila = 0;
	 		
	 		// We write the head of the table, columns name.
	 		XSSFRow headerRow = sheet.createRow((short) fila);
	 		for ( int i=1; i<=cols; i++ )
	 			headerRow.createCell((short) i-1).setCellValue(rs.getMetaData().getColumnName(i).trim());
			
	 		fila++;
	 		
	 		// fill the table.
	 		while (rs.next())
			{
	 			XSSFRow F = sheet.createRow((short) fila);
	 			for ( int i=1; i<=cols; i++ )
		 			F.createCell((short) i-1).setCellValue( (rs.getObject(i)==null) ? "" : rs.getObject(i).toString().trim() );
	 							
	 			fila++;
			}
	 		
	 		// try to resize the columns to fit with data.
	 		for ( int i=1; i<sheet.getRow(0).getPhysicalNumberOfCells(); i++)
	 		{
	 			int origColWidth = sheet.getColumnWidth(i);
	 			sheet.autoSizeColumn(i);

	 			// Reset to original width if resized width is smaller than default/original
	 			if ( 12 > sheet.getColumnWidth(i))
	 				sheet.setColumnWidth(i, origColWidth);	 			
	 		}
			rs.close();
			st.close();
				
			File ff = new File(sArch);
			FileOutputStream foss = new FileOutputStream(ff);
			wb.write(foss);
			foss.flush();
			foss.close();
			log.info("Se ha generado el archivo "+sArch+" exitosamente.");
 		}catch( Exception ex )
 		 {
 			log.error(ex.getMessage());
 		 }
	}
	
	public Boolean Existe( String table )
	{
		Boolean Res = false;
		
		String schema = table.substring(0, table.indexOf("."));
		String tabla = table.substring(table.indexOf(".")+1, table.length());
		
		String query = "SELECT * FROM SYSIBM.SQLTABLES WHERE TABLE_SCHEM='"+schema.toUpperCase()+"' AND TABLE_NAME='"+tabla.toUpperCase()+"' AND TABLE_TYPE='TABLE'";
		
		Connection dbConn = db.getConexion();
		
		try
		{
			Statement st = dbConn.createStatement();			
			ResultSet rs = st.executeQuery(query);
			
			if ( rs.next() )
			{
				Res = true;
				if (DEBUG) log.debug("Archivo "+ table + " existe");
			}
			
			rs.close();
			st.close();			
		}catch( Exception ex )
		{
			log.error(ex.getMessage());
			//System.err.println(ex.getMessage());
			Res=false;
		}
		
		return Res;
	}
	
	public void EnviarMail()
	{
		ArrayList<String> mailInfo = new ArrayList<String>();
		mailInfo = org.alexlm78.db.DBConfig.getMailSetting();
		if (DEBUG) log.debug(mailInfo.get(0)+"-"+mailInfo.get(1));
		KMail mail = new KMail(mailInfo.get(0),mailInfo.get(1),mailInfo.get(2),mailInfo.get(3));
		mail.getMailConf("mail.xml");		
		mail.enviar();
		log.info("El archivo fue enviado por mail!");
	}
	
	public static void main(String[] args) 
	{
		PropertyConfigurator.configure("log4j.properties");
		try
		{
			AnulacionCaja ac = new AnulacionCaja();
			if ( ac.Generar() )
			{
				ac.GenerarArchivoXlsx();
				//ac.EnviarMail();
			}
		}catch( XmlException xex)
		{
			log.error(xex.getMessage());
		}				
	}
}
