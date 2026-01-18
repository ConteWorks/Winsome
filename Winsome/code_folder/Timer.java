import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.ArrayList;


import java.io.FileReader;


import com.google.gson.Gson;

//server

public class Timer implements Runnable {

	private long time;
	private ArrayList<User> ListUsers; 
	private File PostFile; 
	private ArrayList<Post> Posts;
	private File WalletFile;
	private ArrayList<Wallet> wallets;
	private InetAddress ia;
	private DatagramSocket ms;
	
	private static int portaUDP = 7549;
	String filePathPostTemp = "./Poststmp.json";
	String filepathWalletstmp = "./wallettmp.json";
	
	//percentuale in decimi della percentuale autori e guadagno curatori
	private double percentuale_autori = 0.7;
	private double percentuale_curatori = 0.3;
	
	public Timer(long time, ArrayList<User> ListUsers, File PostFile, ArrayList<Post> Posts, File WalletFile, ArrayList<Wallet> wallets, InetAddress ia,DatagramSocket ms) {
		this.time = time;
		this.ListUsers = ListUsers;
		this.PostFile = PostFile;
		this.Posts = Posts;	
		this.WalletFile = WalletFile;
		this.wallets = wallets;
		this.ia = ia;
		this.ms = ms;
	}
	
	public synchronized void run() {
	
	
		while (true)
			try {
				Thread.sleep(time);
				System.out.println(Thread.currentThread().getName()+": Timer scaduto.");
				calcoloRicompense(ListUsers, PostFile, Posts, WalletFile, wallets, ia, ms);
				
			} 
			catch (InterruptedException e) {
				e.printStackTrace();
			}
	}
	
	public synchronized void calcoloRicompense(ArrayList<User> ListUsers, File PostFile, ArrayList<Post> Posts, File WalletFile, ArrayList<Wallet> wallets, InetAddress ia, DatagramSocket ms) {

		System.out.println(Thread.currentThread().getName()+": Avvio del calcolo delle ricompense");
		ArrayList<String> ListCuratori = new ArrayList<String>();

		ArrayList<Post> post_valutati = new ArrayList<Post>();
		ArrayList<Voto> voti_valutati = new ArrayList<Voto>();
		ArrayList<Comment> commenti_valutati = new ArrayList<Comment>();

		double guadagno = 0;
		
		//array parallelo a ListUsers, per memorizzare gli incrementi dei guadagni
		double[] incrementi = new double[ListUsers.size()]; 
		int inc = 0;
		while(inc<incrementi.length) {
			incrementi[inc] = 0;
			inc++;
		}

		for(Post p: Posts) {
			String autore = p.GetAutore();
			guadagno = 0;
			int guadagno_voti = 0;
			double guadagno_commenti = 0;
			int n_iterazioni = p.getn_iterazioni();
			n_iterazioni ++;
			
			voti_valutati.clear();
			commenti_valutati.clear();
			
			//recupero il risultato della prima parte della formula
			int lp = 0;
			int i = 0;
			Voto v;
			while((v = p.Getvoti(i))!=null) {
				if (!v.getIterato()) {
					lp = lp + v.getvoto();
					v.setIterato();
					ListCuratori.add(v.getAutore());
				}
				voti_valutati.add(v);
				i++;
			}
			guadagno_voti = (int) Math.log (Math.max(lp, 0)+1);
			
			//recupero il risultato della seconda parte della formula
			Comment c;
			
			int cp; 			//cp indica il numero di commenti totali che la persona ha fatto, dall'inizio della creazione del post
			ArrayList<String> List_autori_commento = new ArrayList<String>(); 	//indica i nomi delle persone che hanno commentato questo post
			while ((c = p.GetCommento(i))!= null) { 							//questo while mi serve per calcolare il valore di cp
				List_autori_commento.add(c.getautore());
				i++;
			}
			
			double rs = 0; //risultato parziale della seconda parte della formula
			i=0;
			while ((c = p.GetCommento(i))!= null) {
				if(!c.getIterato()) {
					c.setIterato();
					if(!ListCuratori.contains(c.getautore()))
						ListCuratori.add(c.getautore());
					cp = 0;
					for(String s: List_autori_commento)
						if(s.equals(c.getautore()))
							cp++;
					rs = rs + (2/(1+(Math.exp(-(cp-1)))));
				}
				commenti_valutati.add(c);
				i++;
			}
			guadagno_commenti= Math.log(rs +1);
			
			guadagno = (guadagno_voti + guadagno_commenti)/n_iterazioni;
			
			
			double guadagno_autore = guadagno * percentuale_autori;
			double guadagno_curatori = 0;
			if (ListCuratori.size()>0)
				guadagno_curatori = (guadagno * percentuale_curatori)/ListCuratori.size();
			
			Post ps = new Post(p.GetIdPost(), p.GetAutore(), p.GetTitolo(), p.getbody(), n_iterazioni, commenti_valutati, voti_valutati);
			post_valutati.add(ps);
			
			//aggiorno l'array locale incrementi[]
			//trovo l'indice della posizione dell'autore del post in ListUsers
			int indice =0;
			boolean trovato = false;
			while(indice < ListUsers.size() && !trovato)
				if (ListUsers.get(indice).GetNickname().equals(autore))
					trovato = true;
				else 
					indice++;
			//aggiorno l'array incrementi[] alla posizione indice
			if (trovato)
				incrementi[indice]= incrementi[indice]+ guadagno_autore;
			
			//faccio la sessa cosa di sopra per ognuno dei curatori

			for (String curatore : ListCuratori) {
				indice =0;
				trovato = false;
				while(indice < ListUsers.size() && !trovato)
					if (ListUsers.get(indice).GetNickname().equals(curatore))
						trovato = true;
					else 
						indice++;
					//aggiorno l'array incrementi[] alla posizione indice
				if(trovato)
					incrementi[indice]= incrementi[indice]+ guadagno_curatori;
			}
		}
		//devo aggiornare le liste Posts e wallets, e i file PostFile e Walletfile
		
		//aggiorno la lista Posts
		Posts = post_valutati;
		
		//aggiorno la lista wallets
		for(Wallet w :wallets) {
			String prop = w.GetProprietario();
			int i = 0;
			boolean trovato = false;
			while(i<ListUsers.size() && !trovato)	
				if(ListUsers.get(i).GetNickname().equals(prop))
					trovato = true;
				else i++;
			if (trovato)
				w.aggiornaWallet(incrementi[i]);
		}
		
		//aggiorno il file Json PostFile
		try {
			File temp = new File(filePathPostTemp);
			FileWriter fw = new FileWriter(temp);
			BufferedWriter bw = new BufferedWriter(fw);
			for (Post pst : post_valutati) {
				String js = pst.myToJson();
				bw.write(js);
				bw.newLine();	
				
			}
			bw.close();
			PostFile.delete();
			temp.renameTo(PostFile);
		}
		catch (IOException e) {
			e.printStackTrace();
		}
		//aggiorno il file Json Wallets
		try {
			File temp = new File(filepathWalletstmp);
			FileWriter fw = new FileWriter(temp);
			BufferedWriter bw = new BufferedWriter(fw);
			for (Wallet w : wallets) {
				String js = w.mytoJson();
				bw.write(js);
				bw.newLine();		
			}
			bw.close();
			WalletFile.delete();
			temp.renameTo(WalletFile);
		}
		catch (IOException e) {
			e.printStackTrace();
		}
		
		//toDo messaggio UDP che avverte che il calcolo è terminato e le ricompense calcolate
		try {
			byte [] data;
			data="Ricompense aggiornate!".getBytes();
			DatagramPacket dp = new DatagramPacket(data,data.length,ia,portaUDP);
			ms.send(dp);
		} catch(IOException ex){ 
			ex.printStackTrace();;
		}
		//System.out.println(Thread.currentThread().getName()+": Fine del calcolo delle ricompense");
	}
}
	
	
	
	
	
	
	
	
	
	
	
	
	
