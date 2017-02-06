package gt.com.claro.pisa.reportes;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.Properties;
import gt.com.claro.pisa.reportes.utils.RepLog;
import org.alexlm78.db.DBTask;
import org.alexlm78.utils.Configurador;
import org.apache.log4j.Logger;

/**
 * Generador del repote de numero de tecnologia ITALTEL (UT100) para su baja en centrales.
 * 
 * @author Alejandro Lopez Monzon
 * @version 0.3
 * @category Reportes.
 * @deprecated [2010] Por la automatizacion de las centrales a travez del gestor.
 * {@link http://www.alexlm78.org}
 */
public class UT100 
{
	private static Logger log = Logger.getLogger(UT100.class);
	private int fActual;
	int[] vlog; 
	private DBTask db;
	
	public UT100()
	{
		this.vlog = RepLog.getLastLog("UT100");
		this.fActual = vlog[1];
		
		try 
		{
			// Obtenemos los datos de acceso a la DB desde su configuracion.
			Properties props = Configurador.getPropiedades("PISA");
			String ipConnection = props.getProperty("IP");
			String usrConn = props.getProperty("User");
			String pasConn = props.getProperty("Pass");
			String schema = props.getProperty("Schema");
			
			// Definimos la forma de conexion de la clase
			db = new DBTask("com.ibm.as400.access.AS400JDBCDriver");
			String urlConn = "jdbc:as400://"+ ipConnection + "/;libraries="+ schema + ",PASO,GUAV1,GUARDBV1,TFSOBMX1,QTEMP;prompt=false;naming=sql;errors=full";
			db.Conectar(urlConn, usrConn, pasConn);
			
		}catch ( Exception ex )
		{
			log.error(ex.getMessage());
		}		
	}
	
	public int almGestor()
	{
		int Valor=-1;
		String query="";
				
		query = "INSERT INTO ALMGESTOR " +
				"SELECT TELNAM,TELSTS,TELEXC,TELLIN,TELFEC, "+ 
				"       TELTEN,TELFRE,TELTRE,TELRCD,TELDS2, "+
		 		"       TELDS3,MSOTOS,MSOSDT,MSOPST,MSOCOI, "+
		 		" 	    MSOF05,MSOUCD "+
		 		"  FROM GUAV1.BLCTEL B "+
		 		" INNER JOIN GUAV1.SVORD S ON ( DIGITS(B.TELDS2)=S.MSOSOÑ ) "+
		 		" WHERE B.TELHPS='BGT' "+
		 		"   AND B.TELSTS IN ( '1','3' ) "+
		 		"   AND B.TELFEC>=" + String.valueOf(vlog[0]) +
		 		"   AND B.TELNAM IN ( 'QTG', 'REU', 'SEP', 'SEZA', 'CMT' ) "+
		 		"   AND S.MSOF05 NOT IN ( 'CC','CO' ) "+
		 		"   AND S.MSOSTS <> 'D' ";
		try
		{
			Connection con = db.getConexion();
			
			if ( con != null )
			{
				Statement st = con.createStatement();
				st.executeUpdate("DELETE FROM ALMGESTOR WHERE 1=1");
				Valor = st.executeUpdate(query);
								
				st.close();
				
				log.info("ALMGESTOR generado y completo!");
			}
		}catch ( Exception ex )
		{
			log.error(ex.getMessage());
		}
		
		return Valor;
	}
	
	public boolean createRG()
	{
		String query="";
		boolean bRes = false;
		query = "CREATE TABLE PALMBLGU01.RG"+ String.valueOf(this.fActual) + " ( " +
				"TELNAM CHAR(4), TELSTS CHAR(2), TELEXC INT, " +
				"TELLIN INT, TELFEC INT, TELTEN INT, TELFRE INT, " +
				"TELTRE INT, TELRCD INT, TELDS2 INT, TELDS3 CHAR(25), " +
				"MSOTOS CHAR(2), MSOSDT INT, MSOPST INT, " +
				"MSOCOI CHAR(3), MSOF05 CHAR(2), MSOUCD INT ) ";
		
		try
		{
			Connection con = db.getConexion();
			
			if ( con != null )
			{
				Statement st = con.createStatement();
				st.executeUpdate(query); 
				bRes=true;
								
				st.close();
				log.info("CREATE TABLE COMPLETE!");
			}
		}catch ( Exception ex )
		{
			log.error(ex.getMessage());
			bRes=false;
		}
		
		return bRes;
	}

	public boolean llenarRG()
	{
		boolean bRes = false;
		String query="";
		query = "INSERT INTO PALMBLGU01.RG" + String.valueOf(this.vlog[1]) + " " +
				"SELECT * " +
				"  FROM JL637879.ALMGESTOR " +
				" WHERE TELEXC||TELLIN NOT IN ( " +
				"              SELECT TELEXC||TELLIN " +
				"                FROM PALMBLGU01.RG"+ String.valueOf(this.vlog[0]) +" )";
		
		try
		{
			Connection con = db.getConexion();
			
			if ( con != null )
			{
				Statement st = con.createStatement();
				if ( st.executeUpdate(query) > 0 );
					bRes=true;
								
				st.close();
				log.info("Se completo la generacion del archivo RG" + String.valueOf(this.vlog[1]) + " en PALMBLGU01." );
			}
		}catch ( Exception ex )
		{
			log.error(ex.getMessage());
		}
		
		return bRes;
	}

	public void escribirLog( boolean flag )
	{
		RepLog.escribirLog("UT100", this.vlog[1], flag);
	}
	
	public void generarArchivo()
	{
		java.io.Writer wSalida;
		String query="";
		query = "SELECT * FROM PALMBLGU01.RG" + String.valueOf(this.vlog[1]);
		
		try
		{
			Connection con = db.getConexion();
			
			if ( con != null )
			{
				wSalida = new java.io.BufferedWriter(new java.io.FileWriter("RG" + String.valueOf(this.vlog[1]) + ".csv"));
				
				Statement st = con.createStatement();
				ResultSet rs = st.executeQuery(query);
				
				int cols = rs.getMetaData().getColumnCount();
				
				for ( int i=1; i<=cols; i++ )
					wSalida.write("\""+rs.getMetaData().getColumnLabel(i)+"\",");
				wSalida.write(System.getProperty("line.separator"));
				
				while ( rs.next() )
				{
					for ( int i=1; i<=cols; i++ )
						wSalida.write("\""+rs.getObject(i).toString()+"\",");
					
					wSalida.write(System.getProperty("line.separator"));
				}
				
				wSalida.close();
				rs.close();
				st.close();
				log.info("Se ha generado el archivo RG" + String.valueOf(this.vlog[1])+ ".csv");
			}
		}catch ( Exception ex )
		{
			log.error(ex.getMessage());
		}
	}
}
