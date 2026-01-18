//stub per client e server
import java.rmi.*;

public interface NotifyEventInterface extends Remote {

	/* Metodo invocato dal server per notificare un evento ad un client remoto. */
	public void notifyEvent(String value) throws RemoteException;
}
