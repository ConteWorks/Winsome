
import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.Socket;
import java.net.URL;
import java.net.URLConnection;
import java.net.UnknownHostException;
import java.rmi.RemoteException;

import java.util.ArrayList;

import com.google.gson.Gson;

public class Worker implements Runnable{
	Socket client;
	String filePathTemp = "./utentitmp.json";

	File Utenti;
	ArrayList<User> ListUsers;
	ArrayList<User> ListLoggati;
	ArrayList<String> followers; //utenti che seguo
	ArrayList<String> followings;//utenti che seguono me
	//penso di aver confuso i nomi follower e following, ma ora è troppo complicato invertire i nomi nel codice 

	User u;
	
	File PostFile;
	ArrayList<Post> Posts = null;
	String filePathPostTemp = "./Poststmp.json";

	String filepathWalletstmp = "./wallettmp.json";
	File WalletFile;
	ArrayList<Wallet> wallets = null;
	
	ServerCallbackImpl serverCallback;

	public Worker(Socket client, File Utenti, ArrayList<User> ListUsers, ArrayList<User> ListLoggati, File PostFile, ArrayList<Post> Posts, File WalletFile, ArrayList<Wallet> wallets, ServerCallbackImpl serverCallback) {
		this.client = client;
		this.Utenti = Utenti;
		this.ListUsers = ListUsers;
		this.ListLoggati = ListLoggati;
		this.serverCallback = serverCallback;
		followers = new ArrayList<String>();
		followings = new ArrayList<String>();

		this.PostFile = PostFile;
		this.Posts = Posts;
		
		this.WalletFile = WalletFile;
		this.wallets = wallets;
	}

	public  synchronized String LoginUtente(String request) {
		//toDo GSON
        String[] newStr = request.split("::");
        String Username = newStr[1];
        String password = newStr[2];
        //System.out.println("username per login: "+Username);
        
        String risposta = null;
		boolean iscritto_trovato = false;
		int i = 0;
		u = null;
		String name = null;
		while (i < ListUsers.size() && !iscritto_trovato){
			u = ListUsers.get(i);
			name = u.GetNickname();
			//System.out.println(u.GetNickname());
			if(Username.equals(name)) {
				iscritto_trovato = true;
			}
			i++;
		}
		if (iscritto_trovato) {
			/*if (u.GetEliminato()){
				risposta = "L'account "+Username+" risulta cancellato";
			}
			else {*/
				i = 0;
				User u2;
				boolean loggato_trovato = false;
				
				while (i < ListLoggati.size() && !loggato_trovato) {
					u2 = ListLoggati.get(i);
					name = u2.GetNickname();
					System.out.println("Già loggato "+name);

					if (Username.equals(name))
						loggato_trovato = true;
					i++;
				}
				if(!loggato_trovato) {
					if (u.GetPassword().equals(password)){ 
						ListLoggati.add(u);
						risposta = "login_ok"+"::"+u.GetTags()+"::"+u.GetFollowers()+"::"+u.GetFollowings();
						System.out.println(risposta);
						
						//inserisco i dati dei follower nell'array list followers
						if(u.GetFollowers()!= null) {
							String[] fol = u.GetFollowers().split("--"); 
							for (String f: fol)
								followers.add(f);
						}
						
						//inserisco i dati dei follower nell'array list followings
						if(u.GetFollowings()!= null) {
							String[] fol = u.GetFollowings().split("--"); 
							for (String f: fol)
								followings.add(f);
						}
					}
					else
						risposta = "Error: Password errata!";
				}
				else
					risposta = "Utente "+Username+ " già loggato su una diversa finestra o dispositivo";
			}
		//}
		else
			risposta = "Utente "+Username+ " non trovato. Registrati!";
		return  risposta;	
	}

	public synchronized  String LogoutUtente (String request) {
		String[] newStr = request.split("::");
        String Username = newStr[1];
		String risposta = null;
		int i = 0;
		boolean loggato_trovato = false;
		User u2 = null;
		while (i < ListLoggati.size() && !loggato_trovato) {
			u2 = ListLoggati.get(i);
			if (u2.GetNickname().equals(Username)) {
				loggato_trovato = true;
				ListLoggati.remove(i);
				risposta = "logout_ok";
			}
			else
				i++;
		}
		return risposta;
	}
	
	//se l'user vuole seguire l'utente richiesto aggiorno la lista locale followers, aggiorno la lista locale ListUsers e aggiorno il file json utenti
	public synchronized String FollowUser(String request) {
		String[] fol = request.split("::"); 
		String fl = fol[1];
		boolean trovato = false;
		//if (followers!= null) {
		int i = 0;
		if (!followers.isEmpty()) {
			for (String f : followers) {
				if(f.equals(fl))
					trovato = true;
			}
		}
		if (trovato)
			return  "L'utente "+ u.GetNickname()+" segue già l'utente "+fl;
		else {
			boolean user_trovato= false;
			String js2="";
			String name="";
			if (!ListUsers.isEmpty()) {
				i = 0;
				while (i< ListUsers.size()&& !user_trovato)
					if(ListUsers.get(i).GetNickname().equals(fl)) {
						user_trovato = true;
						ListUsers.get(i).AddFollowing(u.GetNickname());
						js2= ListUsers.get(i).mytoJson();
						name = ListUsers.get(i).GetNickname();
					}
					else 
						i++;
				}
			if (user_trovato) {
				followers.add(fl);
				u.AddFollower(fl);
				String js = u.mytoJson();
				try {
					String linea;
					User p;
					File temp = new File(filePathTemp);
					FileReader fr = new FileReader(Utenti);
					BufferedReader br = new BufferedReader(fr);
					FileWriter fw = new FileWriter(temp);
					BufferedWriter bw = new BufferedWriter(fw);
					while ((linea = br.readLine())!= null) {
						Gson gson = new Gson();
						p = gson.fromJson(linea, User.class);
						if (!p.GetNickname().equals(u.GetNickname()) && !p.GetNickname().equals(name)) {
							//codice necessario per modificare la linea
							bw.write(linea);
							bw.newLine();
						}
					}
					bw.write(js);
					bw.newLine();
					bw.write(js2);
					bw.newLine();
					br.close();
					bw.close();
					Utenti.delete();
					temp.renameTo(Utenti);
					
					try {
						//callback, questo per avvertire l'utente che ha un nuovo follower
						//following::utente_da_avvertire::follower
						serverCallback.update("following::"+fl+"::"+u.GetNickname());
					} catch (RemoteException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
				catch (IOException e) {
					e.printStackTrace();
				}
				return "follow ok";
			}
			else 
				return "L'utente "+fl+" non esiste";
		}
	}
	
	//se l'user non vuole seguire più l'utente richiesto aggiorno la lista locale followers, aggiorno la lista locale ListUsers e aggiorno il file json utenti
	public synchronized String UnFollowUser(String request) {
		String risposta= "";
		String[] fol = request.split("::"); 
		String fl = fol[1];
		boolean trovato = false;
		if (!followers.isEmpty()) {
			int i = 0;
			while(i< followers.size() && !trovato){
				if(followers.get(i).equals(fl)) {
					trovato = true;
				}
				else i++;
			}
			if (!trovato)
				risposta =   "L'utente "+ u.GetNickname()+"non segue già l'utente "+fl;
			else {
				followers.remove(fl);
				u.RemoveFollower(fl);
				String js = u.mytoJson();
				String js2="";
				String name="";
				i = 0;
				boolean user_trovato = false;
				while(i < ListUsers.size() && !user_trovato)
					if(ListUsers.get(i).GetNickname().equals(fl)) {
						ListUsers.get(i).RemoveFollowing(u.GetNickname());
						js2 = ListUsers.get(i).mytoJson();
						name = ListUsers.get(i).GetNickname();
						user_trovato = true;
					}
					else
						i++;
				try {
					String linea;
					User p;
					File temp = new File(filePathTemp);
					FileReader fr = new FileReader(Utenti);
					BufferedReader br = new BufferedReader(fr);
					FileWriter fw = new FileWriter(temp);
					BufferedWriter bw = new BufferedWriter(fw);
					while ((linea = br.readLine())!= null) {
						Gson gson = new Gson();
						p = gson.fromJson(linea, User.class);
						if (!p.GetNickname().equals(u.GetNickname()) && !p.GetNickname().equals(name)) {
							//codice necessario per modificare la linea
							bw.write(linea);
							bw.newLine();
						}
					}
					bw.write(js);
					bw.newLine();
					bw.write(js2);
					bw.newLine();
					br.close();
					bw.close();
					Utenti.delete();
					temp.renameTo(Utenti);
					risposta = "unfollow ok";
				}
				catch (IOException e) {
					e.printStackTrace();
				}
				try {
					//callback, questo per avvertire l'utente che ha perso un follower
					//unfollowing::utente_da_avvertire::follower
					serverCallback.update("unfollowing::"+fl+"::"+u.GetNickname());
				} catch (RemoteException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				
			}
		}
		return risposta;
	}
		
		
	public  synchronized String CercaUser(String request) {
		String risposta = null;
		String[] users = request.split("::"); 
		String u = users[1];
		int i = 0;
		boolean trovato = false;
		while (i < ListUsers.size() && !trovato) {
			if(ListUsers.get(i).GetNickname().equals(u)) {
				trovato = true;
				risposta = "utente_trovato::"+ListUsers.get(i).GetTags();
			}
			else
				i++;
		}
		if (!trovato)
			risposta = "Utente "+ u+ "non trovato";
		return risposta;
	}
	
	public synchronized String ViewListUsers() {
		String risposta = "";
		ArrayList<String> users_trovati = new ArrayList<String>(); 
		users_trovati.add(u.GetNickname());
		for (User us: ListUsers) {
			if (!users_trovati.contains(us.GetNickname())) {
				String tag = u.GetTag1();
				if(tag!=null && !tag.equals(""))
					if (tag.equals(us.GetTag1())||tag.equals(us.GetTag2())||tag.equals(us.GetTag3())||tag.equals(us.GetTag4())||tag.equals(us.GetTag5())) {
						risposta = risposta + "nome: "+ us.GetNickname()+" tags: "+ us.GetTags()+"\n";
						users_trovati.add(us.GetNickname());
					}
					else
						if (!users_trovati.contains(us.GetNickname())) {
						tag = u.GetTag2();
						if(tag!=null && !tag.equals(""))
							if (tag.equals(us.GetTag1())||tag.equals(us.GetTag2())||tag.equals(us.GetTag3())||tag.equals(us.GetTag4())||tag.equals(us.GetTag5())) {
								risposta = risposta + "nome: "+ us.GetNickname()+" tags: "+ us.GetTags()+"\n";
								users_trovati.add(us.GetNickname());
							}
							else
								if (!users_trovati.contains(us.GetNickname())) {

								tag = u.GetTag3();
								if(tag!=null && !tag.equals(""))
									if (tag.equals(us.GetTag1())||tag.equals(us.GetTag2())||tag.equals(us.GetTag3())||tag.equals(us.GetTag4())||tag.equals(us.GetTag5())) {
										risposta = risposta + "nome: "+ us.GetNickname()+" tags: "+ us.GetTags()+"\n";
										users_trovati.add(us.GetNickname());
									}
									else
										if (!users_trovati.contains(us.GetNickname())) {
										tag = u.GetTag4();
										if(tag!=null && !tag.equals(""))
											if (tag.equals(us.GetTag1())||tag.equals(us.GetTag2())||tag.equals(us.GetTag3())||tag.equals(us.GetTag4())||tag.equals(us.GetTag5())) {
												risposta = risposta + "nome: "+ us.GetNickname()+" tags: "+ us.GetTags()+"\n";
												users_trovati.add(us.GetNickname());
											}
											else
												if (!users_trovati.contains(us.GetNickname())) {
												tag = u.GetTag5();
												if(tag!=null && !tag.equals(""))
													if (tag.equals(us.GetTag1())||tag.equals(us.GetTag2())||tag.equals(us.GetTag3())||tag.equals(us.GetTag4())||tag.equals(us.GetTag5())) {
														risposta = risposta + "nome: "+ us.GetNickname()+" tags: "+ us.GetTags()+"\n";
														users_trovati.add(us.GetNickname());
													}
												}
										}
								}
						}
				}
		}
			
		
		if (risposta.equals(""))
			risposta = "non ci sono altri utenti con un tag in comune al tuo\n";
		return risposta;
	}
	
	//crea nuovo post, aggiorno la lista Posts e il file PostFile
	//l'id del nuovo post creato è il valore id dell'ultimo post +1
	public synchronized String CreaPost(String request) {
		String risposta = "";
		int IdPost;
		if (Posts.isEmpty())
			IdPost = 0;
		else {
			int n = Posts.size()-1;
			IdPost = Posts.get(n).GetIdPost();
			IdPost++;
		}
		String[] ps = request.split("::");	
		String titolo = ps[1];
		String textbody = ps[2];
		Post p = new Post(IdPost, u.GetNickname(), titolo, textbody);
		String json = p.myToJson();
		Posts.add(p); //aggiorno la lista Posts
		try (BufferedWriter out = new BufferedWriter(new FileWriter(PostFile, true));) {
			out.append(json);
			out.newLine();
			risposta = "Post "+ IdPost+" registrato con successo";
		} catch (IOException e) {
			e.printStackTrace();
		}
		return risposta;
	}
	
	//restituisce il Blog dell'utente
	public synchronized String ViewBlog() {
		String risposta ="";
		boolean trovato = false;//se non sono stati trovati post restituisce la stringa "non hai scritto nessun post"
		for(Post p: Posts) {
			String autore = p.GetAutore();
			//trova i post di cui sono autore
			if (u.GetNickname().equals(autore)) {
				trovato = true;
				risposta = risposta+"IdPost:" + p.GetIdPost()+"\n"+"autore: "+autore+"\n"+"titolo: "+p.GetTitolo()+"\n"+"Contenuto: "+p.getbody()+"\n"
						+"voti positivi: "+p.getVotiPositivi()+"; voti negativi: "+p.getVotiNegativi()+"\n"
								+"Commenti:"+"\n\t"+p.getComments()+"\n"+"------------------------"+"\n";
						
			}
		}
		//trova i post che ho condiviso, ovvero i cui idpost sono presenti nella mia lista feed
		int i =0;
		while(u.getFeed(i)!= -1) {
			for(Post ps : Posts) {
				if(ps.GetIdPost() == u.getFeed(i)) {
					trovato = true;
					risposta = risposta+"IdPost:" + ps.GetIdPost()+"\n"+"autore: "+ps.GetAutore()+"\n"+"titolo: "+ps.GetTitolo()+"\n"+"Contenuto: "+ps.getbody()+"\n"
						+"voti positivi: "+ps.getVotiPositivi()+"; voti negativi: "+ps.getVotiNegativi()+"\n"
								+"Commenti:"+"\n\t"+ps.getComments()+"\n"+"------------------------"+"\n";
				}
			}
			i++;
		}
			
		if(!trovato)
			risposta = "non hai scritto o condiviso nessun post \n";
		return risposta;
	}
	//toDoBetter
	//aggiunge un commento al post, aggiorna il file PostFile e l'array posts
	public synchronized String NewComment(String request) {
		String risposta="";
		String[] req = request.split("::");
		int k= Integer.parseInt(req[1]);
		boolean trovato = false;
		int i = 0;
		while (i<Posts.size() && !trovato) {
			if(Posts.get(i).GetIdPost() == k) {
				trovato= true;
			}
			else 
				i++;
		}
		if (trovato) {
			//verifico che non sia un post che ho creato io
			if(Posts.get(i).GetAutore().equals(u.GetNickname())){
				risposta = "Non puoi commentare un tuo stesso post";
			}
			else {
				//toDo
				//verifico che il post sia nel mio feed, ovvero che il creatore del post sia un utente che seguo, 
				//oppure che uno di loro abbia condiviso il post
				
				boolean post_trovato = false;
				//verifico che il post sia di un utente che seguo
				if(!followers.isEmpty()) {
					for (String nome: followers)
						for(Post p: Posts) {
							String autore = p.GetAutore();
							if (nome.equals(autore)) {
								post_trovato = true;
							}
						}
				}
				if(!post_trovato) {
					//verifico che il post sia stato condiviso da una delle persone che seguo
					for(String nome: followers)
						for(User us: ListUsers)
							if(us.GetNickname().equals(nome)) {
								int j = 0;
								while(us.getFeed(j)!= -1) {
									for(Post p: Posts)
										if(p.GetIdPost() == us.getFeed(j)) {
											post_trovato = true;
										}
								}
							}
				}
				if(post_trovato) {
					//Aggiorno la lista Posts
					Posts.get(i).AddComment(u.GetNickname(), req[2]);
					
					//Aggiorno il file json PostFile
					String js = Posts.get(i).myToJson();
					int  id = Posts.get(i).GetIdPost();
					try {
						String linea;
						Post p;
						File temp = new File(filePathPostTemp);
						FileReader fr = new FileReader(PostFile);
						BufferedReader br = new BufferedReader(fr);
						FileWriter fw = new FileWriter(temp);
						BufferedWriter bw = new BufferedWriter(fw);
						while ((linea = br.readLine())!= null) {
							Gson gson = new Gson();
							p = gson.fromJson(linea, Post.class);
							if (p.GetIdPost() != id) {
								//codice necessario per modificare la linea
								bw.write(linea);
								bw.newLine();
							}
							else {
								bw.write(js);
								bw.newLine();
							}
						}
						br.close();
						bw.close();
						PostFile.delete();
						temp.renameTo(PostFile);
						risposta = "commento aggiunto con successo!";
					}
					catch (IOException e) {
						e.printStackTrace();
					}
				}
				else
					risposta = "non puoi commentare questo post, perchè il post non è presente nel tuo feed";
			}
		}
		return risposta;
	}
	//restituisce il Feed dell'utente, ovvero i post creati dalle persone che seguo e i post che loro hanno condiviso nel proprio blog
	public synchronized String ViewFeed() {
		String risposta ="";
		boolean trovato = false;//se non sono stati trovati post restituisce la stringa "gli utenti che segui non hanno scritto nessun post"
		//questi sono i post delle persone che seguo
		if(!followers.isEmpty()) {
			for (String nome: followers)
				for(Post p: Posts) {
					String autore = p.GetAutore();
					if (nome.equals(autore)) {
						trovato = true;
						risposta = risposta+"IdPost:" + p.GetIdPost()+"\n"+"autore: "+nome+"\n"+"titolo: "+p.GetTitolo()+"\n"+"Contenuto: "+p.getbody()+"\n"
								+"voti positivi: "+p.getVotiPositivi()+"; voti negativi: "+p.getVotiNegativi()+"\n"
										+"Commenti:"+"\n\t"+p.getComments()+"\n"+"------------------------"+"\n";
								
					}
				}
		//questi sono i post che hanno condiviso le persone che seguo
			for(String nome: followers)
				for(User us: ListUsers)
					if(us.GetNickname().equals(nome)) {
						int j = 0;
						while(us.getFeed(j)!= -1) {
							for(Post p: Posts)
								if(p.GetIdPost() == us.getFeed(j)) {
									trovato = true;
									risposta = risposta+"IdPost:" + p.GetIdPost()+"\n"+"autore: "+nome+"\n"+"titolo: "+p.GetTitolo()+"\n"+"Contenuto: "+p.getbody()+"\n"
											+"voti positivi: "+p.getVotiPositivi()+"; voti negativi: "+p.getVotiNegativi()+"\n"
													+"Commenti:"+"\n\t"+p.getComments()+"\n"+"------------------------"+"\n";
								}
						}
							
					}
						
		}
		if(!trovato)
			risposta = "gli utenti che segui non hanno scritto o condiviso nessun post\n";
		else risposta=risposta+"\n";
		return risposta;
	}
	
	//metodo per cercare un post conoscendo il suo id
	public synchronized String CercaPost(String request) {
		String risposta="";
		String[] req= request.split("::");
		int id= Integer.parseInt(req[1]);
		boolean trovato = false;
		int i=0;
		while(i<Posts.size() && !trovato) {
			if(Posts.get(i).GetIdPost()==id)
				trovato=true;
			else 
				i++;
		}
		if (trovato) {
			Post p= Posts.get(i);
			risposta = risposta+"IdPost:" + p.GetIdPost()+"\n"+"autore: "+p.GetAutore()+"\n"+"titolo: "+p.GetTitolo()+"\n"+"Contenuto: "+p.getbody()+"\n"
					+"voti positivi: "+p.getVotiPositivi()+"; voti negativi: "+p.getVotiNegativi()+"\n"
							+"Commenti:"+"\n\t"+p.getComments()+"\n"+"------------------------"+"\n";
		}
		else risposta = "Post non trovato\n";
		return risposta;
	}
	
	//metodo per cancellare un post, conoscendo il suo id, se l'utente che ne fa richiesta è l'autore del post
	public synchronized String EliminaPost(String request) {
		String risposta="";
		String[] req= request.split("::");
		int id= Integer.parseInt(req[1]);
		boolean trovato = false;
		int i=0;
		while(i<Posts.size() && !trovato) {
			if(Posts.get(i).GetIdPost()==id)
				trovato=true;
			else 
				i++;
		}
		if (trovato) {
			Post ps= Posts.get(i);
			if(ps.GetAutore().equals(u.GetNickname())) {
				//elimino il post dalla lista locale Posts, ed aggiorno il file json PostFile
				Posts.remove(i);
				
				try {
					String linea;
					Post p;
					File temp = new File(filePathPostTemp);
					FileReader fr = new FileReader(PostFile);
					BufferedReader br = new BufferedReader(fr);
					FileWriter fw = new FileWriter(temp);
					BufferedWriter bw = new BufferedWriter(fw);
					while ((linea = br.readLine())!= null) {
						Gson gson = new Gson();
						p = gson.fromJson(linea, Post.class);
						if (p.GetIdPost() != id) {
							//codice necessario per modificare la linea
							bw.write(linea);
							bw.newLine();
						}
					}
					br.close();
					bw.close();
					PostFile.delete();
					temp.renameTo(PostFile);
					risposta = "Post "+ id+" cancellato";
				}
				catch (IOException e) {
					e.printStackTrace();
				}
			}
			else
				risposta = "Non sei l'autore del post, perciò non puoi eliminarlo";
		}
		else risposta = "Post non trovato";
		return risposta;
	}

	//metodo per votare un post, conoscendo il suo id
	public synchronized String AggiungiVotoPositivo(String request) {
		String risposta="";
		String[] req= request.split("::");
		int id= Integer.parseInt(req[1]);
		int vt = Integer.parseInt(req[2]); //voto da assegnare al post
		boolean trovato = false;
		int i=0;
		while(i<Posts.size() && !trovato) {
			if(Posts.get(i).GetIdPost()==id)
				trovato=true;
			else 
				i++;
		}
		if (trovato) {
			Post ps= Posts.get(i);
			if(ps.GetAutore().equals(u.GetNickname())) {
				//l'autore non può votare il proprio post
				risposta = "Sei l'autore del post, perciò non puoi votarlo";
			}
			else {
				int j=0;
				trovato = false;
				while(ps.Getvoti(j)!= null && !trovato)
					if (ps.Getvoti(j).getAutore().equals(u.GetNickname())) {
						trovato = true;
					}
					else
						j++;
				if(!trovato) {
					
					Voto v = new Voto(u.GetNickname(), vt);
					//aggiorno la lista locale Posts e aggiorno il file json PostFile
					Posts.get(i).AddVoto(v);
					String js = Posts.get(i).myToJson();
					
					try {
						String linea;
						Post p;
						File temp = new File(filePathPostTemp);
						FileReader fr = new FileReader(PostFile);
						BufferedReader br = new BufferedReader(fr);
						FileWriter fw = new FileWriter(temp);
						BufferedWriter bw = new BufferedWriter(fw);
						while ((linea = br.readLine())!= null) {
							Gson gson = new Gson();
							p = gson.fromJson(linea, Post.class);
							if (p.GetIdPost() != id) {
								//codice necessario per modificare la linea
								bw.write(linea);
								bw.newLine();
							}
							else {
								bw.write(js);
								bw.newLine();
							}
						}
						br.close();
						bw.close();
						PostFile.delete();
						temp.renameTo(PostFile);
						risposta = "Post "+ id+" votato";
					}
					catch (IOException e) {
						e.printStackTrace();
					}
				}
				else {
					risposta = "Hai già votato il post, non puoi votarlo di nuovo";
				}
			}
		}
		else risposta = "Post "+id+" non trovato";
		return risposta;
	}
	
	//metodo che effettua il rewin di un post presente nel feed
	public synchronized String RewinPost(String request) {
		String risposta="";
		String[] req= request.split("::");
		int id= Integer.parseInt(req[1]);
		boolean trovato = false;
		int i=0;
		while(i<Posts.size() && !trovato) {
			if(Posts.get(i).GetIdPost()==id)
				trovato=true;
			else 
				i++;
		}
		if (trovato) {
			Post p= Posts.get(i);
			//verifico se sono l'autore del post
			if (p.GetAutore().equals(u.GetNickname()))
					risposta = "Sei l'autore del post, rewin non consentito";
			else {
				//verifico se il post è già nel suo blog
				int j = 0;
				trovato = false;
				while(u.getFeed(j)!= -1 && !trovato) {
					if(u.getFeed(j) == id)
						trovato = true;
					else
						j++;
				}
				if (trovato) {
					risposta = "Il Post è già presente nel tuo blog";
				}
				else {
					//verifico se l'autore del post è presente tra le proprie persone che segue
					trovato = false;
					for (String f: followers) 
						if (f.equals(p.GetAutore()))
							trovato = true;
					if (!trovato)
						risposta = "L'autore del post non è una delle persone che segui, perciò non puoi condividere il post";
					else {
						//Posso condividere il post
						//aggiorno il file json Utenti, aggiungendo al campo feed l'id del post
						//e aggiorno l'arrayList locale ListUsers
						try {
							u.addFeed(id);
							String js = u.mytoJson();
							int k = 0;
							trovato = false;
							while(k < ListUsers.size()&& !trovato) {
								if(ListUsers.get(k).GetNickname().equals(u.GetNickname()))
									trovato = true;
								else
									k++;
							}
							if (trovato) {
								ListUsers.get(k).addFeed(id);;
								String linea;
								User ps;
								File temp = new File(filePathTemp);
								FileReader fr = new FileReader(Utenti);
								BufferedReader br = new BufferedReader(fr);
								FileWriter fw = new FileWriter(temp);
								BufferedWriter bw = new BufferedWriter(fw);
								while ((linea = br.readLine())!= null) {
									Gson gson = new Gson();
									ps = gson.fromJson(linea, User.class);
									if (!ps.GetNickname().equals(u.GetNickname()) ) {
										bw.write(linea);
										bw.newLine();
									}
									else {
										bw.write(js);
										bw.newLine();
									}
								}
								bw.write(js);
								bw.newLine();
								br.close();
								bw.close();
								Utenti.delete();
								temp.renameTo(Utenti);
								risposta = "Post condiviso con successo";	
							}
							else
								risposta = "Errore";	

						}
						catch (IOException e) {
							e.printStackTrace();
						
						}
					}
				}
			}
		}
		else risposta = "Post non trovato";
		return risposta;
	}

	//metodo che restituisce il valore e lo storico del wallet
	public synchronized String ViewWallet() {
		String risposta="";
		boolean trovato = false;
		int i=0;
		while(i<wallets.size() && !trovato) {
			if(wallets.get(i).GetProprietario().equals(u.GetNickname()))
				trovato=true;
			else 
				i++;
		}
		if (trovato) {
			Wallet wl = wallets.get(i);
			risposta ="Proprietario:" +wl.GetProprietario()+"\t"+"portafoglio: "+wl.Portafoglio()+"\n\nincremento\t\ttimestamp\n";
			int j = 0;
			while(wl.getStorico(j)!= null) {
				risposta = risposta+wl.getStorico(j)+"\n";
				j++;
			}
		}
		else risposta = "Wallet non trovato\n";
		return risposta;
	}
	
	//metodo che restituisce il valore in bitcoin del proprio wallet
	//in realtà questo metodo prende dal sito internet random.org un numero calcolato casualmente
	public synchronized String WalletInBicoin() {
		String risposta = "";
		//calcolo il valore del mio portafoglio
		boolean trovato = false;
		int i=0;
		while(i<wallets.size() && !trovato) {
			if(wallets.get(i).GetProprietario().equals(u.GetNickname()))
				trovato=true;
			else 
				i++;
		}
		if (trovato) {
			Wallet wl = wallets.get(i);
			double pf = wl.Portafoglio();
			if (pf == 0)
				risposta = "il valore in bitcoin del tuo portafoglio vale: 0";
			else {
//--------------------
				URL url = null;
				try {
					url = new URL("https://www.random.org/decimal-fractions/?num=1&dec=10&col=1&format=html&rnd=new");
				}
				catch (MalformedURLException e) { e.printStackTrace(); }
				String inputLine = "", s = "";
				try (BufferedReader in = new BufferedReader(new InputStreamReader(url.openStream()))) {
					while ((inputLine = in.readLine()) != null) {
						s = s + inputLine;
					}
					//la stringa s di risposta è molto lunga, spezzo la stringa varie volte fino a quando non mi resta solo il numero
					String[] pars = s.split("</p><pre");
					//for (String p : pars)
					//	System.out.println(p);
					String[] pars2 = pars[1].split("</pre><p>");
					//for (String p : pars2)
					//	System.out.println(p);
					String[] pars3 = pars2[0].split(">");
					//	System.out.println(pars3[1]);
					risposta ="Il valore in bitcoin del tuo portafoglio vale: "+pars3[1];
					

				}
				catch (UnknownHostException e) {
					risposta = "Errore connessione internet, non  è possibile raggiongere il sito per effettuare la conversione";
				}
				catch (IOException e) { e.printStackTrace(); }
//------------------------
		/*		try {
					// set default encoding
					String encoding = "ISO-8859-1";
					URL u = new URL("https://www.random.org/decimal-fractions/?num=1&dec=10&col=1&format=html&rnd=new");
					URLConnection uc = u.openConnection();
					String contentType = uc.getContentType();
					System.out.println("contenttype"+contentType);
					int encodingStart = contentType.indexOf("charset=");
					if (encodingStart != -1) {
					encoding = contentType.substring(encodingStart + 8);
					}
					System.out.println("encoding"+encoding); 
					InputStream in = new BufferedInputStream(uc.getInputStream());
					Reader r = new InputStreamReader(in, encoding);
					int c;
					while ((c = r.read()) != -1) {
						System.out.print((char) c);
					}
					r.close();
					} catch (MalformedURLException ex) {
						System.err.println("https://www.random.org/decimal-fractions/?num=1&dec=10&col=1&format=html&rnd=new" + " is not a parseable URL");
					} catch (UnsupportedEncodingException ex) {
						System.err.println(
								"Server sent an encoding Java does not support: " +
										ex.getMessage());
					} catch (IOException ex) {
						System.err.println(ex);
					}*/
//-----------------------				
			}
		}
		else risposta = "Wallet non trovato\n";
		return risposta;
	}
	
	public  void run() {
		System.out.println("Connected to client: " + client);
		boolean done =false;
		while (!done) {
			System.out.println(client+" waiting");
			InputStream input;
			OutputStream output;
			try {
				input = client.getInputStream();
				BufferedReader reader = new BufferedReader(new InputStreamReader(input));
				output = client.getOutputStream();	
				PrintWriter answer = new PrintWriter(output, true);
				String  risposta = null;
				String request = reader.readLine();	// reads a line of text
				System.out.println("request: "+request);
				
				//nel caso in cui chiudo la pagina iniziale del client senza effettuare operazioni
				if (request == null)
					done = true;
				
				if (request !=null) {
			        String[] newStr = request.split("::");
			        switch (newStr[0]) {
			        	case "login":
			    			risposta = LoginUtente(request);
			    			break;
			        	case "logout":
			        		risposta = LogoutUtente(request);
			        		break;
			        	case "exit":
			        		risposta = LogoutUtente(request);
			        		done= true;
			        		break;
			        	case "follow":
			        		risposta = FollowUser(request);
			        		break;
			        	case "unfollow":
			        		risposta = UnFollowUser(request);
			        		break;
			        	case "cerca":
			        		risposta = CercaUser(request);
			        		break;
			        	case "viewlistusers":
			        		risposta = ViewListUsers();
			        		risposta = risposta+"/nnull";//carattere speciale, che al lato client indica la terminazione della lettura
			        		break;
			        	case "CreaPost":
			        		risposta = CreaPost(request);
			        		break;
			        	case "ViewBlog":
			        		risposta = ViewBlog();
			        		risposta = risposta+"/nnull";
			        		break;
			        	case "AddComment":
			        		risposta = NewComment(request);
			        		break;
			        	case "ViewFeed":
			        		risposta = ViewFeed();
			        		risposta = risposta+"/nnull";
			        		break;
			        	case "CercaPost":
			        		risposta = CercaPost(request);
			        		risposta = risposta+"/nnull";
			        		break;
			        	case "EliminaPost":
			        		risposta = EliminaPost(request);
			        		break;
			        	case "AggiungiVoto":
			        		risposta = AggiungiVotoPositivo(request);
			        		break;
			        	case "RewinPost":
			        		risposta = RewinPost(request);
			        		break;
			        	case "ViewWallet":
			        		risposta = ViewWallet();
			        		risposta = risposta+"/nnull";
			        		break;
			        	case "WalletInBicoin":
			        		risposta = WalletInBicoin();
			        		break;
			        	default:
			        		risposta = "azione non riconosciuta\n/nnull";
			        		break;
			        }
					answer.println(risposta);	
				}
				request = null;
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		try {
			//sono uscito dal ciclo dopo aver effettuato il logout, perciò chiudo il socket
			if (!client.isClosed()) {
				String s= client.toString();
				System.out.println("Socket " + client+" is closing");
				client.close();
				System.out.println("Socket " + s+" is closed");

			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		
	}
}