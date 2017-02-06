package gt.com.claro.pisa.reportes;

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

public class MsanXela 
{
	private static Logger log = Logger.getLogger(Nietos.class);
	private DBTask db;
	private String NomArch = "MSANOUTXELA.csv";
	String Hoy = "";
	
	ArrayList<String> Querys = new ArrayList<String>();		
	
	String qGenArch = "SELECT * FROM PAMBER.REPMSANOUA A where A.OSPRINCIPAL NOT IN ( SELECT B.OSPRINCIPAL FROM PAMBER.REPMSANOUB B )";
	String qGenBit = "INSERT INTO PAMBER.REPMSANOUB SELECT * FROM PAMBER.REPMSANOUA A where A.OSPRINCIPAL NOT IN ( SELECT B.OSPRINCIPAL FROM PAMBER.REPMSANOUB B )";
	
	public MsanXela()
	{
		Date hoy = Calendar.getInstance().getTime();
		DateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");		
		Hoy = sdf.format(hoy);
		Querys.add("DELETE FROM PAMBER.REPMSANOUT WHERE 1=1");	
		Querys.add("DELETE FROM PAMBER.REPMSANOUA WHERE 1=1");
		Querys.add("INSERT INTO PAMBER.REPMSANOUT SELECT SVBHESTAT StaReg, MSOUCD StaOrd, SVBHTELVR Telfono,c.PLTJRACK Rack, c.PLTJTTJA Tipo, trim(d.PLEEQP) Msan,c.PLTJSRAC Frame,c.PLTJTJA Slot, c.PLTJCPTO Puerto,e.GRVGPASS Password,c.PLTJCTL Control, c.PLTJSUB SubControl,c.PLTJSQÑ Sec, PLTJTERP TerPrin, PLTJCABP CabPrin,PLTJPARP ParPrin, PLTJTERS TerSec, PLTJCABS CabSec, PLTJPARS ParSec, MSOTOS TPOS, SVBHORDEN OSPRINCIPAL, msof05 Etapa, SVBHSUBOS SUBORDEN, SVBHTIPOM TipMov, SVBHFEHOE FecHorEnv, SVBHFEHOE FecHorResp, SVBHDESRES CodResp FROM GUAV1.SVGBITHUAW A INNER JOIN GUAV1.SVORD B ON A.SVBHORDEN=B.MSOSOÑ inner join GUAV1.pltjaord c on a.svbhorden=PLTJSOÑ INNER JOIN GUAV1.PLEQMSAN d ON c.PLTJRACK=d.PLEMSAN inner join GUAV1.SVGRTLPWR E ON A.SVBHORDEN=E.GRVORDER WHERE SVBHFEHOE >='"+Hoy+".00.00.00.000000' AND SVBHTIPOM IN('ALTA_VOZ','ALTA_VOZDATOS') and SVBHESTAT='0' AND MSOSTS<>'D' and msoucd <> 2 and  PLTJDIST ='O'"); 
		Querys.add("INSERT INTO PAMBER.REPMSANOUA SELECT STAREG, STAORD, TELFONO, RACK, TIPO, MSAN, FRAME, SLOT, PUERTO, Password, CONTROL, SUBCONTROL, SEC, TERPRIN, CABPRIN, PARPRIN, TERSEC, CABSEC, PARSEC, TPOS, OSPRINCIPAL, ETAPA, SUBORDEN, TIPMOV,FECHORENV, FECHORRESP, CODRESP FROM PAMBER.REPMSANOUT A WHERE A.FECHORENV=( SELECT MAX(B.FECHORENV) FROM PAMBER.REPMSANOUT B WHERE A.OSPRINCIPAL=B.OSPRINCIPAL )");
		
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
		String query = qGenArch;
		log.info("Generando archivo...");
				
		try
		{
			Connection con = db.getConexion();
			
			if ( con != null )
			{
				wSalida = new java.io.BufferedWriter(new java.io.FileWriter(NomArch));
							
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
				st.executeUpdate(qGenBit);
				st.close();
				first = false;

				wSalida.close();				
				log.info("Se ha generado el archivo "+NomArch+" exitosamente.");
			}
		}catch ( Exception ex )
		{
			log.error(ex.getMessage());
		}
	}

}
