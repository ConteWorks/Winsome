import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.RemoteServer;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;

import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import com.google.gson.Gson;




public class WinsomeServer extends RemoteServer implements Runnable, RemoteServerInterface{
	private static int portaRMI = 1234;
	private static String nome_RMI = "RMIServer";
	private static int portaTCP = 1236;
	private ThreadPoolExecutor pool;
	ServerSocket server;
	String filepathUtenti = "./utenti.json";
	File Utenti;
	
	String filepathPost= "./posts.json";
	File PostFile;
	
	ArrayList<User> ListUsers = null;
	ArrayList<User> ListLoggati = null;
	
	ArrayList<Post> Posts = null;

	String filepathWallets = "./wallet.json";
	File WalletFile;
	ArrayList<Wallet> wallets = null;
	
	private static int portaRMIcallback = 1235;
	private static String nome_RMIcallback = "RMICallback";
	
	private ServerCallbackImpl serverCallback;

	private static String AddressUDP = "228.5.6.7";
	private static int portaUDP = 7549;


	private long time = 300 * 1000; //tempo di attesa per avviare il calcolo delle ricompense, in millisecondi
	
	public static void main(String[] args){

		Thread  Server = new Thread(new WinsomeServer());
		Server.start();

	}
	
	//metodo per la registrazione di un nuovo utente
	public  String RegistraUtente(String Username, String Password, String tag1, String tag2, String tag3, String tag4, String tag5) 
			throws RemoteException{
		System.out.println(Thread.currentThread().getName()+" registrando l'utente "+ Username);
		String risposta = null;
		boolean trovato = false;
		int i = 0;
		//verifico se l'username dell'utente risulta già registrato
		while (i < ListUsers.size() && !trovato){
			User u = ListUsers.get(i);
			String name = u.GetNickname();
			//System.out.println(u.GetNickname());
			if(Username.equals(name)) {
				risposta = "Nome utente "+ Username+" già esistente";
				trovato = true;
			}
			i++;
		}
		//registro il nuovo utente, aggiungendo al file "Utenti" la stringa json del nuovo utente,
		if (!trovato) {
			User u = new User (Username, Password, tag1, tag2, tag3, tag4, tag5);
			String s = u.mytoJson();
			try (BufferedWriter out = new BufferedWriter(new FileWriter(Utenti, true));) {
				ListUsers.add(u);		
				out.append(s);
				out.newLine();
				risposta = "utente "+ u.GetNickname()+" registrato con successo";				
			} catch (IOException e) {
				e.printStackTrace();
			}
			
			//creo un nuovo wallet, aggiorno il file json FileWallet e aggiungo il wallet alla lista wallets
			Wallet w = new Wallet(Username);
			String wl = w.mytoJson();
			try (BufferedWriter out = new BufferedWriter(new FileWriter(WalletFile, true));) {
				wallets.add(w);
				out.append(wl);
				out.newLine();			
			}
			catch (IOException e) {
				e.printStackTrace();
			}
		}	
		return risposta;
		
	}
	

	public  void run() {
		pool = new ThreadPoolExecutor( 0, 1000, 60L, TimeUnit.SECONDS, new SynchronousQueue<Runnable>());
		
		//caricare i dati degli Utenti dal file json
		Utenti = new File(filepathUtenti);
		ListUsers = new ArrayList<User>();
		ListLoggati = new ArrayList<User>();
		if (!Utenti.exists())
			try {
				Utenti.createNewFile();
			} catch (IOException e1) {
				e1.printStackTrace();
				return;
			}
		String json = null;
		try (BufferedReader in = new BufferedReader(new FileReader(Utenti));) {
			System.out.println("File Utenti");
			json = in.readLine();
			while (json != null) {
				System.out.println(json);
				Gson gson = new Gson();
				User p = gson.fromJson(json, User.class);
				ListUsers.add(p);
				json = in.readLine();
			}
		} catch (FileNotFoundException e1) {
			e1.printStackTrace();
			return;
		} catch (IOException e1) {
			e1.printStackTrace();
			return;
		}
		
		//caricare i dati dei Post dal file json
		PostFile = new File(filepathPost);
		Posts = new ArrayList<Post>();
		if (!PostFile.exists())
			try {
				PostFile.createNewFile();
			} catch (IOException e1) {
				e1.printStackTrace();
				return;
			}
		String jsonpost = null;
		try (BufferedReader in = new BufferedReader(new FileReader(PostFile));) {
			System.out.println("File Post");
			jsonpost = in.readLine();
			while (jsonpost != null) {
				System.out.println(jsonpost);
				Gson gson = new Gson();
				Post ps = gson.fromJson(jsonpost, Post.class);
				Posts.add(ps);
				jsonpost = in.readLine();
			}
		} catch (FileNotFoundException e1) {
			e1.printStackTrace();
			return;
		} catch (IOException e1) {
			e1.printStackTrace();
			return;
		}
		
		//caricare i dati dei wallets dal file json
		//toDo
		WalletFile = new File(filepathWallets);
		wallets = new ArrayList<Wallet>();
		if(!WalletFile.exists())
			try {
				WalletFile.createNewFile();
			} catch (IOException e1) {
				e1.printStackTrace();
				return;
			}
		String wlltstr = null;
		try (BufferedReader in = new BufferedReader(new FileReader(WalletFile));) {
			System.out.println("File Wallet");
			wlltstr = in.readLine();
			while (wlltstr != null) {
				System.out.println(wlltstr);
				Gson gson = new Gson();
				Wallet wl = gson.fromJson(wlltstr, Wallet.class);
				wallets.add(wl);
				wlltstr = in.readLine();
			}
		} catch (FileNotFoundException e1) {
			e1.printStackTrace();
			return;
		} catch (IOException e1) {
			e1.printStackTrace();
			return;
		}

		
		//Setup RMI		
		try {
			RemoteServerInterface stub = (RemoteServerInterface) this;
			UnicastRemoteObject.exportObject(stub, 0);
			// Creazione di un registry sulla portaRMI
			LocateRegistry.createRegistry(portaRMI);
			Registry r = LocateRegistry.getRegistry(portaRMI);
			// Pubblicazione dello stub nel registry 
			r.rebind(nome_RMI, stub);			
		}
		catch(RemoteException e) {
			System.out.println("Communication error " + e.toString());
		}
		//Setup RMIcallback
		Registry registryCallback = null;
		try{ 
			/*registrazione presso il registry */
			serverCallback = new ServerCallbackImpl( );
			ServerCallbackInterface stubCallback=(ServerCallbackInterface)UnicastRemoteObject.exportObject (serverCallback,39000);
			LocateRegistry.createRegistry(portaRMIcallback);
			registryCallback=LocateRegistry.getRegistry(portaRMIcallback);
			registryCallback.bind(nome_RMIcallback, stubCallback);
		} catch (Exception e) { System.out.println("Eccezione" +e);}
		
		//setup UDP
		InetAddress ia = null;
		MulticastSocket ms = null;
		try {
			ia =InetAddress.getByName(AddressUDP);
			try {
				ms = new MulticastSocket(portaUDP);
			} catch (SocketException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

		} catch (UnknownHostException e2) {
			// TODO Auto-generated catch block
			e2.printStackTrace();
		}
		
		//Avvio il timer per il calcolo delle ricompense
		Timer timer = new Timer(time, ListUsers, PostFile, Posts, WalletFile, wallets, ia, ms);
		Thread t= new Thread(timer, "Thread_Timer");
		t.setDaemon(true);	
		t.start();

		//Setup TCP

		server = null;
		try {
			server = new ServerSocket(portaTCP);	
			System.out.println("WinsomServer ready"); 
	
			while (true) {
				Socket client = null;
		        try {
		        	client = server.accept();
		        }
		        catch (IOException e) {
		        	e.printStackTrace();
		        }
		        //new Thread(new Worker(client, Utenti, ListUsers, ListLoggati, PostFile, Posts, WalletFile, wallets, serverCallback)).start();
				pool.execute(new Worker(client, Utenti, ListUsers, ListLoggati, PostFile, Posts, WalletFile, wallets, serverCallback));

			}
			
		}
		catch(IOException e) {  		
			System.out.println("Error occur in WinsomServer Setup");
			e.printStackTrace();
			System.exit(0);
		}
		finally {
			try {
				if (!server.isClosed())
					server.close();
			} catch (IOException e1) {
				e1.printStackTrace();
			}
		}
	}
}	
	

