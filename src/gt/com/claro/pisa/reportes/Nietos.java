package gt.com.claro.pisa.reportes;

import java.io.File;
import java.io.FileOutputStream;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Properties;
import org.alexlm78.db.DBTask;
import org.alexlm78.utils.Configurador;
import org.apache.log4j.Logger;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

/**
 * Reporte para generacion de padres, hijos y nietos para procesos de facturacion y post-facturacion.
 * 
 * @author Alejandro Lopez Monzon.
 * @version 1.0
 * @category Reportes.
 * {@link http://www.alexlm78.org/}
 */
public class Nietos 
{
	// Logger
	private static Logger log = Logger.getLogger(Nietos.class);
	private DBTask db;
	
	String[] Querys = {
		"INSERT INTO CTL01 SELECT A.CONBEX, A.CONBLN, A.CONBRS, A.CONMEX, A.CONMLN, A.CONMRS, A.CONCYC FROM GUAV1.CONTROL A INNER JOIN GUAV1.BLCIFAC B ON ( A.CONCYC=B.CICNUM AND B.CICSTS='F' ) WHERE A.CONBEX=A.CONMEX AND A.CONBLN=A.CONMLN AND A.CONBRS=A.CONBRS  AND A.CONBRS=0 ",
		"INSERT INTO CTL02 SELECT A.CONBEX, A.CONBLN, A.CONBRS, A.CONCYC FROM GUAV1.CONTROL A EXCEPTION JOIN CTL01 B ON ( A.CONBEX=B.CONBEX AND A.CONBLN=B.CONBLN AND A.CONBRS=B.CONBRS ) INNER JOIN GUAV1.BLCIFAC C ON ( A.CONCYC=C.CICNUM AND C.CICSTS='F' ) WHERE A.CONBRS=0 GROUP BY A.CONBEX, A.CONBLN, A.CONBRS, A.CONCYC ORDER BY A.CONCYC, A.CONBEX, A.CONBLN, A.CONBRS",
		"INSERT INTO CTL03 SELECT DIGITS(B.CONBEX)||DIGITS(B.CONBLN) AS TEL1, DIGITS(B.CONMEX)||DIGITS(B.CONMLN) AS TEL2, B.CONCYC AS CICLO, B.CONBEX, B.CONBLN, B.CONMEX, B.CONMLN FROM CTL02 A INNER JOIN GUAV1.CONTROL B ON ( A.CONBEX=B.CONMEX AND A.CONBLN=B.CONMLN AND B.CONMRS=0 )",
		"INSERT INTO CTL04 SELECT * FROM CTL02 A EXCEPTION JOIN CTL03 B ON ( A.CONBEX=B.CONMEX AND A.CONBLN=B.CONMLN )",
		"DELETE FROM CTL03 WHERE CONBEX=CONMEX AND CONBLN=CONMLN ",
		"INSERT INTO CTL05 SELECT A.*, B.MSOTOS, B.MSOSO�, B.MSOSDT, B.MSOPST, B.MSOF05, B.MSOCOI, B.MSOUCD FROM CTL03 A INNER JOIN GUAV1.SVORD B ON ( DECIMAL(A.TEL2) = B.MSOPH� ) WHERE B.MSOSTS<>'D' AND B.MSOTOS IN('N1','N3','D1','D8','B1','B2') AND B.MSOSDT >=20080101", 
		"INSERT INTO CTL06 SELECT TEL1, TEL2, CAST(CICLO AS INTEGER) AS CICLO, MSOTOS, MSOSO�, CHAR(MSOSDT), CHAR(MSOPST), MSOF05, MSOCOI, MSOUCD FROM CTL05",
		"INSERT INTO CTL07 SELECT A.TEL1, A.TEL2, CAST(A.CICLO AS INTEGER) AS CICLO FROM CTL03 A EXCEPTION JOIN CTL05 B ON ( A.TEL2=B.TEL2 )",
		"INSERT INTO CTL08 SELECT INTEGER(TEL1) AS PADRE , INTEGER(TEL2) AS HIJO, INTEGER( DIGITS(B.CONMEX)||DIGITS(B.CONMLN) ) AS NIETO, A.CICLO, A.MSOTOS, A.MSOSO�, INTEGER(A.SEL0006) AS FECHACRT, INTEGER(A.SEL0007) AS FECHAPOS, A.MSOF05, A.MSOCOI, CAST(A.MSOUCD AS INTEGER) AS ESTADO FROM CTL06 A INNER JOIN GUAV1.CONTROL B ON ( B.CONBEX = INTEGER(SUBSTR(TEL2,1,6)) AND B.CONBLN = INTEGER(SUBSTR(TEL2,7,4)) AND B.CONMRS = 0 )",
		"INSERT INTO CTL09 SELECT INTEGER( TEL1 ) AS PADRE, INTEGER( TEL2 ) AS HIJO, INTEGER( DIGITS(B.CONMEX)||DIGITS(B.CONMLN) ) AS NIETO, CAST(A.CICLO AS INTEGER) AS CICLO FROM CTL07 A INNER JOIN GUAV1.CONTROL B ON ( B.CONBEX = INTEGER(SUBSTR(TEL2,1,6)) AND B.CONBLN = INTEGER(SUBSTR(TEL2,7,4)) AND B.CONMRS = 0 )",
		"INSERT INTO CTL10 SELECT * FROM CTL08 A WHERE A.FECHAPOS = ( SELECT MAX(Q.FECHAPOS) FROM CTL08 Q WHERE A.PADRE=Q.PADRE AND A.HIJO=Q.HIJO AND A.NIETO=Q.NIETO )",
		"INSERT INTO CTL12 SELECT * FROM CTL10 ",
		"INSERT INTO CTL12 ( PADRE, HIJO, NIETO, CICLO ) SELECT * FROM CTL09",
		"INSERT INTO CTL13 SELECT C.*,S.MSOSO� FROM CTL12 C LEFT OUTER JOIN GUAV1.SVORD S ON ( NIETO=MSOPH� AND MSOUCD=0 AND MSOSTS<>'D' ) "
		};
	
	/**
	 * Contrutor.
	 */
	public Nietos()
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
	 * Se ejecutan las querys que generan los datos para lo padres, hijos y nietos de lineas.
	 * 
	 * @return Exito o fracaso.
	 */
	public boolean Procesar()
	{
		boolean bRes = false;
		log.info("Procesando...");
		try
		{
			Connection con = db.getConexion();
			log.debug(con.toString());
			
			if ( con != null )
			{
				Statement st = con.createStatement();
				for( String query : Querys )
				{
					log.debug("Procesando " + query);					
					st.executeUpdate(query);
					bRes=true;					
				}
				st.close();
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
	 * Genera un archivo con el contenido de los procesos resultantes.
	 */
	public void generarArchivo()
	{
		java.io.Writer wSalida;
		boolean first= true;
		String query = "SELECT * FROM CTL13";
		log.info("Generando archivo...");
				
		try
		{
			Connection con = db.getConexion();
			
			if ( con != null )
			{
				wSalida = new java.io.BufferedWriter(new java.io.FileWriter("Nietos.csv"));
							
				Statement st = con.createStatement();
				ResultSet rs = st.executeQuery(query);
				
				int cols = rs.getMetaData().getColumnCount();
				log.debug("Total de columnas: "+ cols);
								
				if( first )
				{
					for ( int i=1; i<=cols; i++ )
						wSalida.write("\""+rs.getMetaData().getColumnLabel(i)+"\",");
					
					wSalida.write(System.getProperty("line.separator"));
				}
				
				while ( rs.next() )
				{  
					//rs.getO
					for ( int i=1; i<=cols; i++ )   
						wSalida.write( (rs.getObject(i)==null) ? "" : "\""+rs.getObject(i).toString()+"\",");
											
					wSalida.write(System.getProperty("line.separator"));
				}
				
				rs.close();
				st.close();
				first = false;

				wSalida.close();
				log.info("Se ha generado el archivo Nietos.csv exitosamente.");
			}
		}catch ( Exception ex )
		{
			log.error(ex.getMessage());
		}
	}
	
	/**
	 * Genera el archivo en formato XLSX (Excel 2007+)
	 */
	protected void GenerarArchivoXlsx() 
	{
		log.info("Generando archivo xlsx ...");
		String query = "SELECT * FROM CTL13";
		String sArch = "Nietos.xlsx";
 		try
 		{
			XSSFWorkbook wb = new XSSFWorkbook();
			XSSFSheet sheet = wb.createSheet();
			                   
			Connection con = db.getConexion();
	 		Statement st = con.createStatement();
	 		ResultSet rs = st.executeQuery(query);
	 		
	 		int cols = rs.getMetaData().getColumnCount();
	 		int fila = 0;
	 		
	 		XSSFRow headerRow = sheet.createRow((short) fila);
	 		for ( int i=1; i<=cols; i++ )
	 			headerRow.createCell((short) i-1).setCellValue(rs.getMetaData().getColumnName(i));
			
	 		fila++;
	 		
	 		while (rs.next())
			{
	 			XSSFRow F = sheet.createRow((short) fila);
	 			for ( int i=1; i<=cols; i++ )
		 			F.createCell((short) i-1).setCellValue( (rs.getObject(i)==null) ? "" : rs.getObject(i).toString() );
	 							
	 			fila++;
			}
	 		
	 		for ( int i=1; i<=cols; i++ )
	 			sheet.autoSizeColumn(i);
	 		
			rs.close();
			st.close();
				
			File ff = new File(sArch);
			FileOutputStream foss = new FileOutputStream(ff);
			wb.write(foss);
			log.info("Se ha generado el archivo Nietos.xlsx exitosamente.");
 		}catch( Exception ex )
 		 {
 			log.error(ex.getMessage());
 		 }
	}
	
	/**
	 * Inicializa los archivos para el proceso de estos datos.
	 * 
	 * @return Exito o fracaso.
	 */
	public boolean Limpiar()
	{
		boolean bRes = false;
		String num="";
		
		log.info("Limpiando archivos temporales...");
		
		try
		{
			Connection con = db.getConexion();
			
			if ( con != null )
			{
				for ( int i=1; i<=13; i++)
				{
					Statement st = con.createStatement();
					num = ( i < 10 ) ? "0"+String.valueOf(i) : String.valueOf(i);
					log.debug("Cleaning CTL" + num );
					st.executeUpdate("DELETE FROM CTL" + num + " WHERE 1=1");
					bRes=true;		
					st.close();
				}
			}
		}catch ( SQLException ex )
		 {
			bRes=false;
			log.error(ex.getMessage());
		 }
		
		return bRes;
	}
}
