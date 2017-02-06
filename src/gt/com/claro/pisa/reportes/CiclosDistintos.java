package gt.com.claro.pisa.reportes;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Properties;
import org.alexlm78.db.DBTask;
import org.alexlm78.utils.Configurador;
import org.apache.log4j.Logger;

/**
 * Generador del reporte de telefonos que tiene padre e hijo en diferente ciclo.
 * 
 * @author Alejandro Lopez Monzon
 * @version 1.0
 * @category Reportes.
 * {@link http://www.alexlm78.org}
 */
public class CiclosDistintos 
{
	// Logger
	private static Logger log = Logger.getLogger(CiclosDistintos.class);
	DBTask db;
	
	/**
	 * Contructor
	 */
	public CiclosDistintos()
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
	 * Lleno CICLOS1
	 * 
	 * @return Exito o fracaso.
	 */
	public boolean ciclosUno()
	{
		boolean bRes = false;
		String query =  "INSERT INTO CICLOS1 " +
						"SELECT C.CONBEX, C.CONBLN, C.CONBRS, C.CONCYC " +
						"  FROM GUAV1.CONTROL C " +
						" INNER JOIN GUAV1.BLCIFAC B ON ( C.CONCYC=B.CICNUM AND B.CICSTS='F' ) " +
						" WHERE C.CONBEX=C.CONMEX " +
						"   AND C.CONBLN=C.CONMLN " +
						"   AND C.CONBRS=C.CONBRS " +
						"   AND C.CONBRS=0 " ;
		try
		{
			Connection con = db.getConexion();
			
			if ( con != null )
			{
				Statement st = con.createStatement();
				st.executeUpdate("DELETE FROM CICLOS1 WHERE 1=1");
				st.executeUpdate(query);
				bRes=true;		
				st.close();
				//log.info("Se completo la generacion del archivo.");
			}
		}catch ( SQLException ex )
		 {
			log.error(ex.getMessage());
		 }
		return bRes;
	}
	
	/**
	 * Lleno CICLOS2
	 * 
	 * @return Exito o fracaso.
	 */
	public boolean ciclosDos()
	{
		boolean bRes = false;
		String query =  "INSERT INTO CICLOS2 " +
						"SELECT C.CONBEX,C.CONBLN,C.CONCYC, B.CONMEX,B.CONMLN,B.CONCYC " +
						"  FROM CICLOS1 C " +
						" INNER JOIN GUAV1.CONTROL B ON (     C.CONBEX  = B.CONBEX " + 
						"                 		          AND C.CONBLN  = B.CONBLN " +
						"  								  AND C.CONBRS  = B.CONBRS " +
						"								  AND C.CONCYC <> B.CONCYC )" ;
		try
		{
			Connection con = db.getConexion();
			
			if ( con != null )
			{
				Statement st = con.createStatement();
				st.executeUpdate("DELETE FROM CICLOS2 WHERE 1=1");
				st.executeUpdate(query);
				bRes=true;
				st.close();
				//log.info("Se completo la generacion del archivo.");
			}
		}catch ( SQLException ex )
		 {
			log.error(ex.getMessage());
		 }
		return bRes;
	}
}
