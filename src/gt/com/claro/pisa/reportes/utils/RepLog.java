package gt.com.claro.pisa.reportes.utils;

import org.alexlm78.utils.Configurador;
import org.alexlm78.db.DBTask;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.Properties;
import org.apache.log4j.Logger;

/**
 * Logs de Reportes.
 * 
 * @author Alejandro Lopez Monzon
 * @version 0.1
 * @category Logs, Reportes.
 */
public class RepLog 
{
	private static Logger log = Logger.getLogger(RepLog.class);
	
	/**
	 * IP de conexion al servidor de log.
	 */
	private static String ipConnection;
	/**
	 * Usuario de conexion al servidor de log.
	 */
	private static String usrConn;
	/**
	 * Password de usuario para conexion al servidor de log.
	 */
	private static String pasConn;
	
	private static void getValores()
	{
		try 
		{
			Properties props = Configurador.getPropiedades("PISA");
		
			// Valores de conexcion a la DB
			ipConnection = props.getProperty("IP");
			usrConn = props.getProperty("User");
			pasConn = props.getProperty("Pass");			
		}catch (Exception e) 
		 {
			log.error(e.getMessage());
		 }
	}
	
	/**
	 * Obtencion del ultimo log para un ID.
	 * 
	 * @param iid ID para el cual se necesita el ultimo log.
	 * @return Estructura del ultimo log (id, ultima fecha, fecha actual). 
	 */
	public static int[] getLastLog( String iid )
	{
		getValores();
		//String ssVal = ss.compareToIgnoreCase("MPP")==0 ? "'P'" : "' '";
		int aVals[] = new int[2];  
		
		try 
		{
			String urlConn = "jdbc:as400://"+ ipConnection + "/;libraries=JL637879,GUAV1,GUARDBV1,TFSOBMX1,QTEMP;prompt=false;naming=sql;errors=full";
			//DBTask db = new DBTask();
			DBTask db = new DBTask("com.ibm.as400.access.AS400JDBCDriver");
			db.Conectar(urlConn, usrConn, pasConn);
			Connection con = db.getConexion();
						
			if (con != null)
			{
				String query = "SELECT DISTINCT CTRREP, "
					 		+ "       CTRDATE, "
					 		+ "       YEAR(DATE(DAYS(CURRENT_DATE)))*10000 + MONTH(DATE(DAYS(CURRENT_DATE)))*100 + DAY(DATE(DAYS(CURRENT_DATE))) Now "
					 		+ "  FROM CTRLREPS A "
					 		+ " WHERE CTRREP='" + iid.toUpperCase() + "'"
					 		+ "   AND CTRSTS = ''" 
							+ "   AND CTRDATE = ( SELECT MAX(CTRDATE) FROM CTRLREPS B "
					 		+ " 	   WHERE A.CTRREP = B.CTRREP AND B.CTRSTS = '' ) ";
				
				Statement st = con.createStatement();
				ResultSet rs = st.executeQuery(query);
					
				if ( rs.next() )
				{
					aVals[0] = rs.getInt(2);
					aVals[1] = rs.getInt(3);						
					//aVals[2] = rs.getInt(4);
				}
				
				rs.close();
			}
		}catch( Exception e)
		{
			log.error(e.getMessage());
		}
	
		return aVals;
	}
	
	/**
	 * Escritura del log
	 * 
	 * @param rep ID Relacionado.
	 * @param fecha Fecha del log
	 * @param flag Estado a aplicar.
	 */
	public static void escribirLog( String rep, int fecha, boolean flag )
	{
		String query="";
		query = "INSERT INTO CTRLREPS " +
				"(CTRSTS,CTRDATE,CTRTIME,CTRREP,CTRRES) " +
				 " values "+
				 "("+ (flag ? "''" : "'E'") +","+ String.valueOf(fecha) +",CURRENT_TIME,'"+ rep.toUpperCase()+"'," + (flag ? "'EXITO'" : "'ERROR'") +")";

		try
		{
			String urlConn = "jdbc:as400://"+ ipConnection + "/;libraries=JL637879,GUAV1,GUARDBV1,TFSOBMX1,QTEMP;prompt=false;naming=sql;errors=full";
			//DBTask db = new DBTask();
			DBTask db = new DBTask("com.ibm.as400.access.AS400JDBCDriver");
			db.Conectar(urlConn, usrConn, pasConn);
			Connection con = db.getConexion();
			
			if ( con != null )
			{
				Statement st = con.createStatement();
				st.executeUpdate(query);
				
				st.close();
				
				log.info("Log entry inserted!");
			}
		
		}catch ( Exception ex )
		{
			log.error(ex.getMessage());
		}
	}
}
