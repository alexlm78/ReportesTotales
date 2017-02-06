package gt.com.claro.pisa.reportes;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;

public class MalisFact 
{
	private Connection dbConn = null;
	
	public MalisFact()
	{
		try 
		{
			Class.forName("oracle.jdbc.driver.OracleDriver"); 
			dbConn = DriverManager.getConnection("jdbc:oracle:thin:@192.168.4.59:1524:BSCS", "READ", "READ");
		}catch ( Exception ex )
		{
			ex.printStackTrace();
		}
	}
	
	public void getDataFile()
	{
		Statement st;
		ResultSet rs;
		java.io.Writer wSalida;
		boolean first= true;
		
		try 
		{
			st = dbConn.createStatement();
			rs = st.executeQuery(getQuery());
			
			wSalida = new java.io.BufferedWriter(new java.io.FileWriter("Ciclos_facturacion_06_15.csv"));
			int cols = rs.getMetaData().getColumnCount();
			
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
			
		} catch (Exception e) 
		{			
			e.printStackTrace();
		}		
	}
	
	public String getQuery()
	{
		return "SELECT ca.customer_id, cus.custcode, cus.billcycle, ca.co_id, ca.plcode, dn.dn_num, sm.sm_serialnum, pr.port_num, "+
				"       DECODE (ch.ch_status, "+
				"               'a', 'ACTIVO', "+
				"               'd', 'DESACTIVADO', "+
				"               's', 'SUSPENDIDO', "+
				"               'o', 'ESPERA', "+
				"               'DESCONOCIDO' "+
				"              ) ch_status, "+
				"       ca.co_installed, ca.co_activated, csc.cs_deactiv_date, ca.co_expir_date, cd.cd_deactiv_date, ca.tmcode, "+
				"       rtp.des tmdes, ca.co_userlastmod, ccont.ccname, ccont.ccfname, ccont.cclname "+
				"  FROM port pr, "+
				"       contr_devices cd, "+
				"       storage_medium sm, "+
				"       contract_all ca, "+
				"       rateplan rtp, "+
				"       directory_number dn, "+
				"       contract_history ch, "+
				"       contr_services_cap csc, "+
				"       customer_all cus, "+
				"       ccontact_all ccont "+
				" WHERE rtp.tmcode = ca.tmcode "+
				"   AND pr.port_id = cd.port_id "+
				"   AND ca.co_id = cd.co_id "+
				"   AND csc.co_id = ca.co_id "+
				"   AND dn.dn_id = csc.dn_id "+
				"   AND sm.sm_id = pr.sm_id "+
				"   AND ca.co_id = ch.co_id "+
				"   AND csc.co_id = ch.co_id "+
				"   AND ca.customer_id = cus.customer_id "+
				"   AND ccont.customer_id = cus.customer_id "+
				"   AND ccont.customer_id = ca.customer_id "+
				"   AND ch.ch_seqno = (SELECT MAX (ch_seqno) "+
				"                        FROM contract_history "+
				"                       WHERE co_id = ca.co_id) "+
				"   AND cd.cd_seqno = (SELECT MAX (cd_seqno) "+
				"                        FROM contr_devices "+
				"                       WHERE co_id = ca.co_id) "+
				"   AND csc.seqno = (SELECT MAX (seqno) "+
				"                      FROM contr_services_cap "+
				"                     WHERE co_id = ca.co_id) "+
				"   AND ccont.ccbill = 'X' "+
				"   AND ch.ch_status IN ('a', 's') "+
				"   AND cus.billcycle IN ('06', '15') ";
	}
}
