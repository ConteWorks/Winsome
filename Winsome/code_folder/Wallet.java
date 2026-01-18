import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

import com.google.gson.Gson;

public class Wallet {
	private String proprietario;
	private double portafoglio;
	
	//REP-INV: i due ArrayList hanno la stessa dimensione
	private ArrayList<Double> portafoglio_storico; //contiene lo storico delle transazioni
	private ArrayList<String> timestamp_storico;	//contiene i timestamp dello storico delle transazioni
	
	public Wallet(String proprietario) {
		this.proprietario = proprietario;
		this.portafoglio = (double) 0;
		this.portafoglio_storico = new ArrayList<Double>();
		portafoglio_storico.add(portafoglio);
		this.timestamp_storico = new ArrayList<String>();
		Long timeStamp = System.currentTimeMillis(); 
	    SimpleDateFormat sdf=new SimpleDateFormat("yyyy-mm-dd HH:mm:ss");
	    String sd = sdf.format(new Date(Long.parseLong(String.valueOf(timeStamp))));
		timestamp_storico.add(sd);
	}
	
    public String mytoJson() {
    	Gson gson = new Gson();
		return gson.toJson(this, Wallet.class);	
    }
    
    public String GetProprietario() {
    	return proprietario;
    }
    
    public Double Portafoglio() {
    	return portafoglio;
    }
    public String getStorico(int i) {
    	if(i<0 || i >= portafoglio_storico.size())
    		return null;
    	return portafoglio_storico.get(i)+ "\t\t"+ timestamp_storico.get(i);
    }
    
    public void aggiornaWallet(double incremento) {
    	portafoglio = portafoglio + incremento;
		portafoglio_storico.add(incremento);
		Long timeStamp = System.currentTimeMillis(); 
	    SimpleDateFormat sdf=new SimpleDateFormat("yyyy-mm-dd HH:mm:ss");
	    String sd = sdf.format(new Date(Long.parseLong(String.valueOf(timeStamp))));
		timestamp_storico.add(sd);
    }
}
