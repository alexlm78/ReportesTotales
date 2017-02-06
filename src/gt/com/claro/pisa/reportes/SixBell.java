package gt.com.claro.pisa.reportes;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
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
import org.alexlm78.utils.Configurador;
import org.apache.log4j.Logger;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

/**
 * Generador del reporte de los numeros desbloqueados que deben tener catagoria sixbell y no la tienen.
 * 
 * @author Alejandro Lopez Monzon
 * @version 0.3
 * @category Reportes.
 * {@link http://www.alexlm78.org}
 */
public class SixBell 
{
	// Logger de clase.
	private static Logger log = Logger.getLogger(SixBell.class);
	
	// Array de fechas para contruccion del reporte en el tiempo
	private ArrayList<String> Fechas = new ArrayList<String>();
	private String Hoy;
	// Conexion a la DB
	DBTask db;
		
	/**
	 * Contructor
	 */
	public SixBell()
	{
		Date hoy = Calendar.getInstance().getTime();
		//long tiempoActual = hoy.getTime();
		//long unDia = 1 * 24 * 60 * 60 * 1000;
		//Date ayer = new Date(tiempoActual - unDia);
		DateFormat sdf = new SimpleDateFormat("yyyyMMdd");
		Fechas.add(sdf.format(hoy));
		sdf = new SimpleDateFormat("dd/MM/yy");
		Hoy = sdf.format(hoy);
		Conectar();
	}
	
	/**
	 * Contructor
	 * 
	 * @param fechas Fechas para la ejecucion del reporte.
	 */
	public SixBell ( ArrayList<String> fechas )
	{
		this.Fechas = fechas;
		Date hoy = Calendar.getInstance().getTime();
		DateFormat sdf = new SimpleDateFormat("dd/MM/yy");
		Hoy = sdf.format(hoy);
		Conectar();		
	}
	
	/**
	 * Establece la conexion con la DB para la generacion del reporte de Sixbell.
	 */
	private void Conectar()
	{
		try 
		{
			// Obtenemos los datos de acceso a la DB desde su configuracion.
			Properties props = Configurador.getPropiedades("PISA");
			String ipConnection = props.getProperty("IP");
			String usrConn = props.getProperty("User");
			String pasConn = props.getProperty("Pass");
			String schema = props.getProperty("Schema");
			
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
	 * Llena los archivos con los procesos para la generacion del reporte.
	 * 
	 * @return Exito o fracaso.
	 */
	public Boolean Procesar()
	{
		Boolean bRes = false;
		String Validacion  = ( Fechas.size() == 1 ) ? "B.TELFRE="+Fechas.get(0) : "B.TELFRE BETWEEN "+Fechas.get(0) +" AND " + Fechas.get(1);
				
		try
		{			
			this.Limpiar();
			
			ArrayList<String> querys = new ArrayList<String>();
			querys.add("INSERT INTO JL637879.SIXRECREP1 SELECT B.TELEXC, B.TELLIN, B.TELFRE, B.TELTRE, B.TELHPS FROM GUAV1.BLCTEL B WHERE B.TELSTS='1' AND B.TELHPS IN ('svc','rvc') AND TELNAM NOT IN ( SELECT MCHCEN FROM GUAV1.SVMICEHFC ) AND " + Validacion);
			querys.add("INSERT INTO JL637879.SIXRECREP2 SELECT DISTINCT A.TELEXC, A.TELLIN, A.TELFRE, A.TELTRE, A.TELHPS FROM JL637879.SIXRECREP1 A WHERE A.TELFRE||DIGITS(A.TELTRE) IN ( SELECT MAX(B.TELFRE||DIGITS(B.TELTRE)) FROM JL637879.SIXRECREP1 B WHERE A.TELEXC=B.TELEXC AND A.TELLIN=B.TELLIN) AND A.TELHPS='rvc'");
			querys.add("DELETE FROM JL637879.SIXRECREP2 WHERE TELHPS='svc'");
			querys.add("INSERT INTO JL637879.SIXRECREP3 SELECT * FROM GUARDBV1.SVHISTCIC A WHERE A.SVHISNOS = ( SELECT MAX(B.SVHISNOS) FROM GUARDBV1.SVHISTCIC B WHERE B.SVHISTEL=A.SVHISTEL ) AND SVHISFEC>0");
			querys.add("INSERT INTO JL637879.SIXRECREP4 SELECT TELEXC,TELLIN,TELFRE,TELTRE,TELHPS,SVHISTOS,SVHISNOS,SVHISFEC,SVHISTEL,'' FROM JL637879.SIXRECREP2 R LEFT OUTER JOIN JL637879.SIXRECREP3 S ON ( INTEGER(DIGITS(TELEXC)||DIGITS(TELLIN)) = SVHISTEL )");
			querys.add("DELETE FROM JL637879.SIXRECREP4 WHERE SVHISTOS IS NULL OR SVHISTOS IN ('BA')");
			querys.add("UPDATE JL637879.SIXRECREP4 SET ESPECIAL='BLTSIX' WHERE TELEXC*10000+TELLIN IN ( SELECT GARITA FROM JL637879.GARITAS)");
			querys.add("INSERT INTO JL637879.SIXRECREP5 SELECT * FROM JL637879.SIXRECREP4 WHERE TELEXC*10000+TELLIN NOT IN ( SELECT TELEXC*10000+TELLIN FROM JL637879.SIXRECLOG WHERE FECHA='" + Hoy +"' )");
			
			Connection con = db.getConexion();
			Statement st = con.createStatement();
			
			Integer i = 0;
			while ( i<querys.size())
			{
				log.debug(querys.get(i));
				st.executeUpdate(querys.get(i));
				i++;
			}
			
			bRes = true;
			st.close();			
		}catch( Exception ex )
		{
			log.error(ex.getMessage());
			bRes = false;
		}
		
		return bRes;
	}
	
	/**
	 * Vacia los archivos que se usan como temporales para la generacion del reporte.
	 */
	private void Limpiar()
	{
		try
		{
			log.info("Iniciando limpieza de archivos");
			String query = "";
			Connection con = db.getConexion();
			for( int i=1 ; i<=5 ; i++)
			{
				query = "DELETE FROM JL637879.SIXRECREP"+ String.valueOf(i) + " WHERE 1=1";
				
				if ( con != null )
				{
					Statement st = con.createStatement();
					st.executeUpdate(query);
					st.close();
				}
			}
		}catch ( SQLException sex )
		{
			log.error(sex.getMessage());
		}finally
		{		
			log.info("Limpieza terminada");
		}
	}
	
	/**
	 * Genera el archivo de Sixbell en formato CSV.
	 */
	protected void GenerarArchivo()
	{
		log.info("Generando archivo SIXRECREP");
		try
		{
			FileWriter fArch;
				 		
	 		String sArch = "SIXRECREP.csv";
	 			 			 		
	 		if( new File(sArch).exists() )
	 		{
	 			File aArch = new File(sArch);
	 			aArch.delete();
	 			aArch = null;
	 			fArch = new FileWriter(sArch);
	 		}else
	 			fArch = new FileWriter(sArch);
			
	 		Connection con = db.getConexion();
	 		Statement st = con.createStatement();
	 		ResultSet rs = st.executeQuery("SELECT * FROM JL637879.SIXRECREP5");
	 		
	 		String cadena="";
	 		int cols = rs.getMetaData().getColumnCount();
	 		
			while (rs.next())
			{
				for ( int i=1; i<=cols; i++ )
					cadena += rs.getString(i).trim() + ",";
				
				fArch.write(cadena);
				fArch.write(System.getProperty("line.separator"));
				cadena="";
			}
			fArch.close();
			rs.close();
			st.close();
		}catch (SQLException sqle) 
		{
			log.error(sqle.getMessage());
		}catch (IOException ioe) 
		{
			log.error(ioe.getMessage());
		}catch ( Exception ex )
		{
			log.error(ex.getMessage());
		}
	}
	
	/**
	 * Genera el archivo de Sixbell en formato XLSX (Excel 2007+)
	 */
	protected void GenerarArchivoXlsx() 
	{
		log.info("Generando archivo SIXRECREP");
		String sArch = "SIXRECREP.xlsx";
 		try
 		{
			XSSFWorkbook wb = new XSSFWorkbook();
			XSSFSheet sheet = wb.createSheet();
			                   
			Connection con = db.getConexion();
	 		Statement st = con.createStatement();
	 		ResultSet rs = st.executeQuery("SELECT * FROM JL637879.SIXRECREP5");
	 		
	 		int cols = rs.getMetaData().getColumnCount();
	 		int fila = 0;
	 		
	 		XSSFRow headerRow = sheet.createRow((short) fila);
	 		for ( int i=1; i<=cols; i++ )
	 			headerRow.createCell((short) i-1).setCellValue(rs.getMetaData().getColumnName(i));
			
	 		fila = 1;
	 		
	 		while (rs.next())
			{
	 			XSSFRow F = sheet.createRow((short) fila);
	 			for ( int i=1; i<=cols; i++ )
		 			F.createCell((short) i-1).setCellValue(rs.getString(i));
				
	 			fila++;
			}
	 		
	 		for ( int i=1; i<=cols; i++ )
	 			sheet.autoSizeColumn(i);
	 		
			rs.close();
			st.close();
				
			File ff = new File(sArch);
			FileOutputStream foss = new FileOutputStream(ff);
			wb.write(foss);
 		}catch( Exception ex )
 		 {
 			log.error(ex.getMessage());
 		 }
	}
	
	public Boolean InsertarLog()
	{
		Boolean bRes = false;
				
		try
		{			
			String query = "";
			
			query = "INSERT INTO JL637879.SIXRECLOG (TELEXC,TELLIN,TELFRE,TELTRE,TELHPS,SVHISTOS,SVHISNOS,SVHISFEC,SVHISTEL,ESPECIAL ) SELECT * FROM JL637879.SIXRECREP5";
			
			Connection con = db.getConexion();
			Statement st = con.createStatement();
			
			int rs = st.executeUpdate(query);
			
			if ( rs>0 )
				bRes = true;
			st.close();			
		}catch( Exception ex )
		{
			log.error(ex.getMessage());
			bRes = false;
		}
		
		return bRes;
	}
	
	public Boolean HayRegistros()
	{
		Boolean bRes = false;
				
		try
		{			
			String query = "";
			
			query = "SELECT COUNT(*) FROM JL637879.SIXRECREP5";
			
			Connection con = db.getConexion();
			Statement st = con.createStatement();
			
			ResultSet rs = st.executeQuery(query);
			
			if ( rs.next() )
				bRes = (rs.getInt(1)>0)?true:false;				
						
			st.close();			
		}catch( Exception ex )
		{
			log.error(ex.getMessage());
			bRes = false;
		}
		
		return bRes;
	}
}
