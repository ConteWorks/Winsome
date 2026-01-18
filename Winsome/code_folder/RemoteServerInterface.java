//stub per il client e per il server
import java.rmi.Remote;
import java.rmi.RemoteException;

public interface RemoteServerInterface extends Remote{
	
	String RegistraUtente(String Username, String Password, String tag1, String tag2, String tag3, String tag4, String tag5) 
			throws RemoteException;
}
