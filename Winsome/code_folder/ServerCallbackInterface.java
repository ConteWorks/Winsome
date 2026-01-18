//stub callback client-server
import java.rmi.*;
public interface ServerCallbackInterface extends Remote{
		
	/* registrazione per la callback */
	public void registerForCallback (NotifyEventInterface ClientInterface) throws RemoteException;
	
	/* cancella registrazione per la callback */
	public void unregisterForCallback (NotifyEventInterface ClientInterface) throws RemoteException; 
}