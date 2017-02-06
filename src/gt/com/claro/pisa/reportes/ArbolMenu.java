package gt.com.claro.pisa.reportes;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Properties;
import org.alexlm78.db.DBTask;
import org.alexlm78.utils.Configurador;
import org.apache.log4j.Logger;

/**
 * Genera el arbol de un menu en pisa a partir de su origen.
 * 
 * @author Alejandro Lopez Monzon
 * @since Jan 2012
 * @version 0.1
 */
public class ArbolMenu 
{
	private static Logger log = Logger.getLogger(ArbolMenu.class);
	private String rootMenu;
	private DBTask db;
	
	/**
	 * Constuctor
	 * 
	 * @param menu Menu inicial.
	 */
	public ArbolMenu( String menu )
	{
		this.setRootMenu(menu);
		try
		{
			// Obtenemos los datos de acceso a la DB desde su configuracion.
			Properties props = Configurador.getPropiedades("PISA");
			String ipConnection = props.getProperty("IP");
			String usrConn = props.getProperty("User");
			String pasConn = props.getProperty("Pass");
			String schema = props.getProperty("Schema");
			
			String urlConn = "jdbc:as400://"+ ipConnection + "/;libraries="+ schema + ",PASO,GUAV1,GUARDBV1,TFSOBMX1,QTEMP;prompt=false;naming=sql;errors=full";
			db = new DBTask("com.ibm.as400.access.AS400JDBCDriver");
			db.Conectar(urlConn, usrConn, pasConn);
		}catch ( Exception ex )
		{
			log.error(ex.getMessage());
		}
	}
	
	/**
	 * Genera un arbol desde el objeto con el nivel especificado
	 * 
	 * @param menu Menu de Inicio.
	 * @param lvl Nivel jerarquico especificado.
	 * @return Arbol generado.
	 */
	public ArrayList<ArrayList<String>> getArbol( String menu, Integer lvl )
	{
		ArrayList<ArrayList<String>> res = new ArrayList<ArrayList<String>>();
		String query = "SELECT MNUP01,MNUP02,MNUP03,MNUP04,MNUP05,MNUP06,MNUP07,MNUP08,MNUP09,MNUP10,MNUP11,MNUP12,MNUP13,MNUP14,MNUP15,MNUP16,MNUP17,MNUP18,MNUP19,MNUP20,MNUP21,MNUP22,MNUP23,MNUP24 FROM GUAV1.MNMENU2 WHERE MNUNME='"+menu.trim().toUpperCase()+"'";
		
		if ( isMenu( menu ) )
		{
			try		
			{
				Connection con = db.getConexion();
				log.debug(con.toString());
				
				if ( con != null )
				{
					Statement st = con.createStatement();
					ResultSet rs = st.executeQuery(query);
					
					while ( rs.next() )
					{
						for ( int i=1; i<=rs.getMetaData().getColumnCount(); i++)
							if ( rs.getObject(i).toString().trim().length()>0 )
							{
								String slvl="";
								ArrayList<String> all = new ArrayList<String>();
								for ( int k=1; k<=lvl; k++) slvl+="+";
								all.add(slvl);
								all.add(rs.getObject(i).toString());
								all.add(getTipo(rs.getObject(i).toString()));
								all.add(getNom(rs.getObject(i).toString()));
								res.add(all);
								if ( all.get(2).toString().compareTo("M")==0 )
									res.addAll(getArbol(all.get(1).toString(), lvl+1));
							}
					}
					
					st.close();				
				}
				log.info("Generado " + menu.trim().toUpperCase());
			}catch ( SQLException ex )
			 {
				
				log.error(ex.getMessage());
			 }
		}
		
		return res;
	}
	
	/**
	 * Genera un arbol desde el objeto con el nivel default (0)
	 * 
	 * @param menu Menu de Inicio
	 * @return Arbol generado.
	 */
	public ArrayList<ArrayList<String>> getArbol( String menu )
	{
		return getArbol(menu, 0);
	}
	
	/**
	 * Genera en un archivo el arbol generado.
	 * 
	 * @param arbol Arbol para generar el archivo
	 */
	protected void GenerarArchivo( ArrayList<ArrayList<String>> arbol)
	{
		log.info("Generando arbol para "+this.rootMenu);
		try
		{
			FileWriter fArch;
				 		
	 		String sArch = "Arbol_"+ this.rootMenu +".txt";
	 			 			 		
	 		if( new File(sArch).exists() )
	 		{
	 			File aArch = new File(sArch);
	 			aArch.delete();
	 			aArch = null;
	 			fArch = new FileWriter(sArch);
	 		}else
	 			fArch = new FileWriter(sArch);
			
	 		fArch.write("Arbol: " + this.rootMenu);
	 		fArch.write(System.getProperty("line.separator"));
	 		
	 		for( ArrayList<String> val : arbol )
	 		{
	 			String cadena = val.get(0) + val.get(1) + " [" + val.get(2)+ "]  " + val.get(3); 
	 			fArch.write(cadena);
	 			fArch.write(System.getProperty("line.separator"));
	 		}
	 		
			fArch.close();
			log.info("Se genero en el archivo "+sArch);
		}catch (IOException ioe) 
		{
			log.error(ioe.getMessage());
		}catch ( Exception ex )
		{
			log.error(ex.getMessage());
		}
	}
	
	/**
	 * Establece si una opcion es o no un menu
	 * 
	 * @param menu Objeto a verificar.
	 * @return True si es menu.
	 */
	public Boolean isMenu( String menu )
	{
		String query="SELECT * FROM GUAV1.MNOPT2 WHERE OPTTYP='M' AND OPTNME='"+menu.trim().toUpperCase()+"'";
		Boolean bRes = false;
		
		try
		{
			Connection con = db.getConexion();
			
			if ( con != null )
			{
				Statement st = con.createStatement();
				ResultSet rs = st.executeQuery(query);
				
				if( rs.next() )
					bRes=true;
				
				rs.close();
				st.close();
			}
		}catch ( SQLException ex )
		 {			
			log.error(ex.getMessage());
		 }
		
		return bRes;
	}
	
	/**
	 * Obtiene el tipo de opcion de una dada.
	 * 
	 * @param obj Objeto del cual se necesita el tipo
	 * @return Tipo de opcion para un objeto de menu.
	 */
	public String getTipo( String obj )
	{
		String query="SELECT OPTTYP,OPTTTL FROM GUAV1.MNOPT2 WHERE OPTNME='"+obj.trim().toUpperCase()+"'";
		String bRes = "P";
		try
		{
			Connection con = db.getConexion();
			
			if ( con != null )
			{
				Statement st = con.createStatement();
				ResultSet rs = st.executeQuery(query);
				
				if( rs.next() )
					bRes = rs.getObject(1).toString().trim();
				
				rs.close();
				st.close();
			}
		}catch ( SQLException ex )
		 {			
			log.error(ex.getMessage());
		 }
		
		return bRes;
	}
	
	/**
	 * Obtiene el nombre descriptivo del objeto.
	 * 
	 * @param obj Objeto para el cual obtendremos el nombre
	 * @return Nombre descriptivo del Objeto
	 */
	public String getNom( String obj )
	{
		String query="SELECT OPTTTL FROM GUAV1.MNOPT2 WHERE OPTNME='"+obj.trim().toUpperCase()+"'";
		String bRes = "P";
		try
		{
			Connection con = db.getConexion();
			
			if ( con != null )
			{
				Statement st = con.createStatement();
				ResultSet rs = st.executeQuery(query);
				
				if( rs.next() )
					bRes = rs.getObject(1).toString().trim();
				
				rs.close();
				st.close();
			}
		}catch ( SQLException ex )
		 {			
			log.error(ex.getMessage());
		 }
		
		return bRes;
	}

	/**
	 * Obtiene el menu raiz para el arbol
	 * 
	 * @return Menu raiz.
	 */
	public String getRootMenu() 
	{
		return rootMenu;
	}

	/**
	 * Establece el menu raiz para el arbol
	 * 
	 * @param rootMenu Menu raiz para la creacion del arbol.
	 */
	public void setRootMenu(String rootMenu) 
	{
		this.rootMenu = rootMenu.toUpperCase();
	}	

}
