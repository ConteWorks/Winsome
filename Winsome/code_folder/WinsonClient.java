
import java.awt.Dimension;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.MulticastSocket;
import java.net.Socket;
import java.rmi.Remote;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
//import java.util.StringTokenizer;

import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;


public class WinsonClient {
	public static int lunghezza_finestra = 800;
	public static int altezza_finestra = 600;
	public static int x;
	public static int y;
	
	private static int portaRMI = 1234;
	private static String nome_RMI = "RMIServer";
	
	private static int portaRMIcallback = 1235;
	private static String nome_RMIcallback = "RMICallback";
	
	private static ServerCallbackInterface stubServerCallback;
	private static NotifyEventInterface stubCallback ;

	
	private static JFrame window; //finestra iniziale di login e register
	private static JFrame window2; //finestra principale 

	private static Socket socket; //socket for TCP
	private static final String ServerAddress = "127.0.0.1"; //indirizzo localhost
	private static int portaTCP = 1236;
	
	private static int portaUDP = 7549;
	private static String AddressUDP = "228.5.6.7";
	
	private static String Username;
	private static String myTags;
	private static ArrayList<String> followers; //utenti che seguo
	private static ArrayList<String> following; //utenti che seguono me
   //penso di aver confuso i nomi follower e following, ma ora è troppo complicato invertire i nomi nel codice 

	private static JLabel infolabel2;
	
	public static void window1() {
		// finestra della pagina iniziale di login e register
		//label per username
		x= lunghezza_finestra/3;
		y= altezza_finestra/16;
		MyLabel label1= new MyLabel("Username: ", x, y);

		//label per password
		y= y + altezza_finestra/16;
		MyLabel label2= new MyLabel("Password: ", x, y);
				
		JPanel jp = new JPanel();
				
		//label per i tag
		ArrayList<MyLabel> tags = new ArrayList<MyLabel>(); 
		int j;
		for(int i= 0; i< 5; i++) {
			y= y + altezza_finestra/16;
			j=i+1;
			tags.add(i, new MyLabel("tag "+ j +":", x, y));
			tags.get(i).add(jp);
		}
				
		//botton per iscrizione
		y= y + altezza_finestra/16;
		MyButton button_iscrizione = new MyButton("Iscriviti",x,y);
				
		//botton per login
		y= y + altezza_finestra/16;
		MyButton button_login = new MyButton("Login",x,y);
				
		//label per messaggi di sistema e messaggi di errore
		JLabel infolabel = new JLabel("");
		y= y + altezza_finestra/16;
		infolabel.setBounds(x,y,3000, 30);

		jp.add(infolabel);
				
				
		label1.add(jp);
		label2.add(jp);
				
		button_iscrizione.add(jp);
		button_login.add(jp);
				
				
		RemoteServerInterface stub;
		Remote RemoteObject;
				
		socket= null;

		try {
			//Setup RMI
			Registry r = LocateRegistry.getRegistry(portaRMI);
			RemoteObject = r.lookup(nome_RMI);
			stub = (RemoteServerInterface) RemoteObject;
					
			//listener register
			ActionListener listener = new MyActionListener(stub, label1, label2, tags, infolabel);
			button_iscrizione.addActionListener(listener);
					
					
			//Setup TPC
					
			//al logout va messo
			//socket.close();
					
			//listener login
			if (socket == null)
					socket = new Socket(ServerAddress, portaTCP);
			System.out.println(socket);
			
			button_login.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent event) {
					infolabel.setText(null);	
					if (label1.getText()!= null && !label1.getText().equals("") && label2.getText() != null && !label2.getText().equals("")) {
						//Connessione al server per Login, usando TCP
						try {
							OutputStream output = socket.getOutputStream();
							PrintWriter writer = new PrintWriter(output, true);
							String string = new String ("login::"+label1.getText()+"::"+label2.getText());
							writer.println(string);	
							
							InputStream input = socket.getInputStream();
							BufferedReader reader = new BufferedReader(new InputStreamReader(input));
							String answer = reader.readLine();	// reads a line of text
							
							String[] newStr = answer.split("::");
					        String ans = newStr[0];
							//System.out.println(ans);
					        
							if (ans.equals("login_ok")) {
								myTags = newStr[1];
								followers = new ArrayList<String>();
								following = new ArrayList<String>();
								//aggiungo i follower alla lista locale 
								if (newStr[2]!= null) {
									String[] fol = newStr[2].split("--"); 
									for (String f: fol)
										followers.add(f);
								}
								//aggiungo i following alla lista locale 
								if (newStr[3]!= null) {
									String[] fol = newStr[3].split("--"); 
									for (String f: fol)
										following.add(f);
								}
								
								Username = label1.getText();
								window2();
								open_window2();
								close_window1();
								try {
									System.out.println("Cerco il Server");
									Registry registry = LocateRegistry.getRegistry(portaRMIcallback);
									stubServerCallback = (ServerCallbackInterface) registry.lookup(nome_RMIcallback);
									/* si registra per la callback */
									System.out.println("Registering for callback");
									NotifyEventInterface callbackObj = new NotifyEventImpl();
									stubCallback = (NotifyEventInterface)UnicastRemoteObject.exportObject(callbackObj, 0);
									stubServerCallback.registerForCallback(stubCallback);
								}
								catch (Exception e){ System.err.println("Client exception:"+ e.getMessage( ));}
								
							}
							else 
								infolabel.setText(answer);	
							//output.close();
							//input.close();
						} catch (IOException e1) {
							e1.printStackTrace();
						}
						
						
					}
					else 
						infolabel.setText("Error: Username o Password mancante");	
				}
			});
		}
		catch(Exception e) {
			System.out.println("Error in invoking object method " +
			e.toString() + e.getMessage());
		}
				
		window = new JFrame("Winsome");
		window.setSize(lunghezza_finestra,altezza_finestra);
		jp.setLayout(null);
		window.setLocation(0,0);
		window.setContentPane(jp);
		window.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		open_window1();
	}


	public static void close_window1() {
		window.setVisible(false);
	}	
	
	public static void open_window1() {
		window.setVisible(true);
	}
	
	public static void window2() {

		window2 = new JFrame("Winsome");
		Dimension screenSize = Toolkit.getDefaultToolkit( ).getScreenSize( );
		
		lunghezza_finestra = (int) screenSize.getWidth();
		altezza_finestra = (int) screenSize.getHeight();

		window2.setSize(lunghezza_finestra,altezza_finestra);
		window2.setLocation(0,0);
		window2.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
		
		
	
		//exit
		//quando clicco x invoco il logout
		//finestra piccola popup presa da https://www.iprogrammatori.it/forum-programmazione/java/login-logout-cambio-dei-pannelli-t24926.html
		window2.addWindowListener(new WindowAdapter() {
	            @Override
	            public void windowClosing(WindowEvent e) {
	                int conferma = JOptionPane.showConfirmDialog(null,"Sicuro Di Voler Uscire Dal Programma?","Esci", JOptionPane.YES_OPTION);
	                if (conferma == JOptionPane.YES_OPTION) {
	                	OutputStream output;
						try {
							output = socket.getOutputStream();
							PrintWriter writer = new PrintWriter(output, true);
		    				String string = new String ("exit::"+Username);
		    				writer.println(string);	
		    				
		    				InputStream input = socket.getInputStream();
		    				BufferedReader reader = new BufferedReader(new InputStreamReader(input));
		    				String answer = reader.readLine();	// reads a line of text
		    				output.close();
		    				input.close();
		    				if (answer.equals("logout_ok")) {
		    					if (!socket.isClosed())
		    						socket.close();
		    					
		    					//al logout invoco anche l'unregister for callback
		    					System.out.println("Unregistering for callback");
		    					stubServerCallback.unregisterForCallback(stubCallback);
		    					
		    					System.out.println("Uscita dall'Applicazione Avvenuta con Successo\n"+""+this.getClass().getName()+"\n");
		    					System.exit(0);
		    				}
		    				else {
		    					System.out.println("Errore durante l'uscita dell'applicazione\n"+""+this.getClass().getName()+"\n");

		    				}
						} catch (IOException e1) {
							e1.printStackTrace();
						}
	    				
	                }
	            }
		});				
		JPanel jp2 = new JPanel();
		jp2.setLayout(null);

		try {
			MulticastSocket ms = new MulticastSocket(portaUDP);
			InetAddress ia=InetAddress.getByName(AddressUDP);
			ms.joinGroup(ia);
			byte[] buf = new byte[24];
			DatagramPacket dp = new DatagramPacket (buf,buf.length);
			UDPreceive x = new UDPreceive(ms, dp);
			
			Thread t= new Thread(x, "UDP_receiver");
			t.setDaemon(true);	
			t.start();
		}
		catch (IOException ex) {ex.printStackTrace();; }
		
		x= 8;

		//label per messaggi di sistema e messaggi di errore
		infolabel2 = new JLabel("");
		y= altezza_finestra -  (altezza_finestra/4);
		infolabel2.setBounds(x,y,3000, 30);
		jp2.add(infolabel2);
		
		y=2;
		//label che mostra l'username dell'utente loggato
		JLabel NameUser =new JLabel("User: "+Username);
		NameUser.setBounds(x,y,320, 30);
		jp2.add(NameUser);
		
		x= x+224;
		//label che mostra i tags dell'utente loggato
		String[] listtags = myTags.split("--");
		String tags="";
		for(String t: listtags)
			tags=t+"; "+tags;
		JLabel mytags2 =new JLabel("Tags: "+tags);
		mytags2.setBounds(x,y,640, 30);
		jp2.add(mytags2);
		
		//bottone per logout
		x= lunghezza_finestra - lunghezza_finestra/3;
		MyButton button_logout = new MyButton("Logout",x,y);
		button_logout.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent event) {
				System.out.println("button logout cliccato");
				OutputStream output;
				try {
					output = socket.getOutputStream();
					PrintWriter writer = new PrintWriter(output, true);
    				String string = new String ("logout::"+Username);
    				writer.println(string);	
    				
    				InputStream input = socket.getInputStream();
    				BufferedReader reader = new BufferedReader(new InputStreamReader(input));
    				String answer = reader.readLine();	// reads a line of text
    				//output.close();
    				//input.close();
    				if (answer.equals("logout_ok")) {
    					//al logout resetto la lista followers
    					followers.clear();
    					
    					//al logout resetto la lista following
    					following.clear();
    					
    					//distruggo la finestra2 e ripristino la finestra 1
    					window.setVisible(true);
    					window2.dispose();
    					System.out.println("logout avvenuto con Successo\n"+""+this.getClass().getName()+"\n");
    				}
    				else {
    					System.out.println("Errore durante il logout\n"+""+this.getClass().getName()+"\n");

    				}
				} catch (IOException e1) {
					e1.printStackTrace();
				}
			}
		});
		button_logout.add(jp2);	

		//textarea per lettura
		x=8;
		y= 383;
		JTextArea ta2 = new JTextArea();
		ta2.setEditable(false);
		ta2.setLineWrap(true); //accapo automatico
		JScrollPane scroll2 = new JScrollPane(ta2);
		scroll2.setBounds(x,y,670,350);
		jp2.add(scroll2);
		
		y=2;
		//bottone per visualizzare la lista di users che hanno almeno 1 tag in comune
		x = x + 800;
		MyButton button_listUsers = new MyButton("VieWListUsers",x,y);
		button_listUsers.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent event) {
				ta2.setText("");
				infolabel2.setText("");
				try {
					OutputStream output = socket.getOutputStream();
					PrintWriter writer = new PrintWriter(output, true);
					String string = new String ("viewlistusers");
					writer.println(string);	
					
					InputStream input = socket.getInputStream();
					BufferedReader reader = new BufferedReader(new InputStreamReader(input));
					String answer;
					while(!(answer = reader.readLine()).equals("/nnull")) {
						ta2.append(answer+"\n");

					}
				}
				catch (IOException e1) {
					e1.printStackTrace();
				}
			}
		});
		button_listUsers.add(jp2);
		
		x= 8;
		//bottone label per ricerca idutente
		y= y + altezza_finestra/16;
		MyLabel label_ricerca = new MyLabel("Nome Utente: ", x, y);
		MyButton button_ricerca = new MyButton("Cerca",x+224,y);
		button_ricerca.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent event) {
				infolabel2.setText("");
				ta2.setText("");

				if (label_ricerca.getText().equals(Username)) {
					infolabel2.setText("Non puoi cercare te stesso");
				} else 
					if (label_ricerca.getText().equals(""))
						infolabel2.setText("Campo NomeUtente vuoto");

					else {
						try {
							OutputStream output = socket.getOutputStream();
							PrintWriter writer = new PrintWriter(output, true);
							String string = new String ("cerca::"+label_ricerca.getText());
							writer.println(string);	
							
							InputStream input = socket.getInputStream();
							BufferedReader reader = new BufferedReader(new InputStreamReader(input));
							String answer = reader.readLine();	// reads a line of text
							String[] ans = answer.split("::"); 
							if (ans[0].equals("utente_trovato")) {
								String[] listtags = ans[1].split("--");
								String tags="";
								for(String t: listtags)
									tags=t+"; "+tags;
								ta2.setText("nome: "+label_ricerca.getText()+" - tags: "+tags);
							}
							else 
								infolabel2.setText("Utente "+ label_ricerca.getText()+ " non trovato");
							//output.close();
							//writer.close();
						}
						catch (IOException e) {
							e.printStackTrace();
						}	
				}
			}
		});
		label_ricerca.add(jp2);	
		button_ricerca.add(jp2);
		
		
		//bottone per follow utente
		x = x + 420;
		MyButton button_follow = new MyButton("Follow",x,y);
		button_follow.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent event) {
				infolabel2.setText("");
				ta2.setText("");
				if (label_ricerca.getText().equals(Username)) {
					infolabel2.setText("Non puoi seguire te stesso");
				} else 
					if (label_ricerca.getText().equals(""))
						infolabel2.setText("Campo NomeUtente vuoto");

					else {
						try {
							OutputStream output = socket.getOutputStream();
							PrintWriter writer = new PrintWriter(output, true);
							String string = new String ("follow::"+label_ricerca.getText());
							writer.println(string);	
							
							InputStream input = socket.getInputStream();
							BufferedReader reader = new BufferedReader(new InputStreamReader(input));
							String answer = reader.readLine();	// reads a line of text
							
							if (answer.equals("follow ok"))
								followers.add(label_ricerca.getText());
							infolabel2.setText(answer);
							//output.close();
							//writer.close();
						}
						catch (IOException e) {
							e.printStackTrace();
						}	
				}
			}
		});
		button_follow.add(jp2);

		//bottone per unfollow utente
		x = x + 200;
		MyButton button_unfollow = new MyButton("Unfollow",x,y);
		button_unfollow.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent event) {
				infolabel2.setText("");
				ta2.setText("");
				if (label_ricerca.getText().equals(Username)) {
					infolabel2.setText("Non puoi non seguire te stesso");
				}
				else 
					if (label_ricerca.getText().equals(""))
						infolabel2.setText("Campo NomeUtente vuoto");

					else {
						try {
							OutputStream output = socket.getOutputStream();
							PrintWriter writer = new PrintWriter(output, true);
							String string = new String ("unfollow::"+label_ricerca.getText());
							writer.println(string);	
							
							InputStream input = socket.getInputStream();
							BufferedReader reader = new BufferedReader(new InputStreamReader(input));
							String answer = reader.readLine();	// reads a line of text
							
							if (answer.equals("unfollow ok"))
								followers.remove(label_ricerca.getText());
							infolabel2.setText(answer);
							//output.close();
							//writer.close();
						}
						catch (IOException e) {
							e.printStackTrace();
						}	
				}
			}
		});
		button_unfollow.add(jp2);
		
		//bottone per visualizzare lista delle persone che seguo (followers)
		x = x + 200;
		MyButton button_followers = new MyButton("UtentiCheSeguo",x,y);
		button_followers.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent event) {
				ta2.setText("");
				infolabel2.setText("");
				if(!followers.isEmpty() )//&& followers.get(0)!=null && !followers.get(0).equals("null"))
					for (String follower: followers) {
						if(!follower.equals("null"))
							ta2.append(follower+"\n");
					}
				else 
					ta2.append("Non segui nessun utente");
			}
		});
		button_followers.add(jp2);
		
		//bottone per visualizzare lista delle persone che seguono ME (following)
		x = x + 224;
		MyButton button_following = new MyButton("UtentiCheSeguoME",x,y);
		button_following.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent event) {
				ta2.setText("");
				infolabel2.setText("");
				if(!following.isEmpty())// && following.get(0)!=null && !following.get(0).equals("null") ) 
					for (String follower: following) {
						if(!follower.equals("null"))
							ta2.append(follower+"\n");
					}
				else 
					ta2.append("Non sei seguito da nessun utente");
			}
		});
		button_following.add(jp2);
		
		x=8;
		y= y + altezza_finestra/16;
		
		//label per titolo post
		JTextField titolo;
		titolo = new JTextField();
		titolo.setBounds(x,y,670,30);
		titolo.setText(null);
		titolo.setEditable(true);
		jp2.add(titolo);
		
		//textarea per scrittura
		y=y+30;
		JTextArea ta = new JTextArea();
	    ta.setEditable(true);
		ta.setLineWrap(true); //accapo automatico
		JScrollPane scroll = new JScrollPane(ta);
		scroll.setBounds(x,y,670,150);
		jp2.add(scroll);
		
		//bottone per creare un nuovo post
		/*y= y+150;
		MyButton button_post_create = new MyButton("Crea post",x,y);
		button_post_create.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent event) {
				//verifico la lunghezza del titolo (max 20 parole) e del testo (max 500 parole)
	        	infolabel2.setText("");
		        StringTokenizer stringTokenizer = new StringTokenizer(titolo.getText());
		        int tokensCount = stringTokenizer.countTokens();
		        if (tokensCount > 20) 
		        	infolabel2.setText("Il titolo è troppo lungo, lunghezza massima consentita 20 parole, attualmente sono "+tokensCount+" parole");
		        else 
			        if (tokensCount == 0)
			        	infolabel2.setText("Il titolo non può essere vuoto");
			        else {
				        stringTokenizer = new StringTokenizer(ta.getText());
				        tokensCount = stringTokenizer.countTokens();
				        if (tokensCount == 0) 
				        	infolabel2.setText("Il testo non può essere vuoto");		        
				        else
					        if (tokensCount > 500)
					        	infolabel2.setText("Il testo è troppo lungo, lunghezza massima consentita 500 parole, attualmente sono "+tokensCount+" parole");
					        else {
					        	//toDo crea nuovoPost
					        }
			        }
			}
				
		});
		button_post_create.add(jp2);	*/
		
		y= y+150;					
		//bottone per cercare un post
		//prendo l'id del post da cercare dal campo titolo
		MyButton button_CercaPost = new MyButton("CercaPost",x,y);
		button_CercaPost.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent event) {
				infolabel2.setText("");
				ta2.setText("");
				if (titolo.getText().length()==0) 
					infolabel2.setText("Il campo IdPost è vuoto");
				else {
					int idpost;
					try{
						idpost = Integer.parseInt(titolo.getText());
						try {						
							OutputStream output = socket.getOutputStream();
							PrintWriter writer = new PrintWriter(output, true);
							String string = new String ("CercaPost::"+idpost);
							writer.println(string);	
													
							InputStream input = socket.getInputStream();
							BufferedReader reader = new BufferedReader(new InputStreamReader(input));
							String answer;
							while(!(answer = reader.readLine()).equals("/nnull")) {
								ta2.append(answer+"\n");
							}							
						}
						catch (IOException e1) { 
							e1.printStackTrace();
						}
					}
					catch (NumberFormatException ex){
						infolabel2.setText("Il campo IdPost non è un id");
						ex.printStackTrace();
					}
				}
								
			}
		});
		button_CercaPost.add(jp2);
		
		
		
		//bottone per creare un nuovo post
		x = x + 200;
		MyButton button_post_create = new MyButton("Crea post",x,y);
		button_post_create.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent event) {
				//verifico la lunghezza del titolo (max 20 parole) e del testo (max 500 parole)
			       infolabel2.setText(""); 
				   if (titolo.getText().length() > 20) 
				        infolabel2.setText("Il titolo è troppo lungo, lunghezza massima consentita 20 caratteri, attualmente sono "+titolo.getText().length()+" caratteri");
				   else 
					   if (titolo.getText().length() == 0)
					        infolabel2.setText("Il titolo non può essere vuoto");
					   else {
	
						   if (ta.getText().length() == 0) 
						        infolabel2.setText("Il testo non può essere vuoto");		        
						    else
							    if (ta.getText().length() > 500)
							       infolabel2.setText("Il testo è troppo lungo, lunghezza massima consentita 500 caratteri, attualmente sono "+ta.getText().length()+" caratteri");
							    else {
							    	try {
										OutputStream output = socket.getOutputStream();
										PrintWriter writer = new PrintWriter(output, true);
										String string = new String ("CreaPost::"+titolo.getText()+"::"+ta.getText());
										writer.println(string);	
											
										InputStream input = socket.getInputStream();
										BufferedReader reader = new BufferedReader(new InputStreamReader(input));
										String answer = reader.readLine();	// reads a line of text

										infolabel2.setText(answer);
										titolo.setText("");
										ta.setText("");
										//output.close();
										//writer.close();
									}
									catch (IOException e) {
										e.printStackTrace();
									}
							    }
					        }
					}
						
		});
		button_post_create.add(jp2);
		
		//bottone per eliminare un post
		//prendo l'id del post da eliminare dal campo titolo
		x = x + 200;
		MyButton button_EliminaPost = new MyButton("EliminaPost",x,y);
		button_EliminaPost.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent event) {
				infolabel2.setText("");
				ta2.setText("");
				if (titolo.getText().length()==0) 
					infolabel2.setText("Il campo IdPost è vuoto");
				else {
					int idpost;
					try{
						idpost = Integer.parseInt(titolo.getText());
						try {						
							OutputStream output = socket.getOutputStream();
							PrintWriter writer = new PrintWriter(output, true);
							String string = new String ("EliminaPost::"+idpost);
							writer.println(string);	
															
							InputStream input = socket.getInputStream();
							BufferedReader reader = new BufferedReader(new InputStreamReader(input));
							String answer = reader.readLine();	// reads a line of text

							infolabel2.setText(answer);						
						}
						catch (IOException e1) { 
							e1.printStackTrace();
						}
					}
					catch (NumberFormatException ex){
						infolabel2.setText("Il campo IdPost non è un id");
						ex.printStackTrace();
					}
				}
										
			}
		});
		button_EliminaPost.add(jp2);
		
		x = x + 200;
		//bottone per aggiungere un commento ad un post
		//prendo l'id del post dal campo titolo, e il commento dal campo ta
		MyButton button_comment = new MyButton("AddComment",x,y);
		button_comment.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent event) {
				infolabel2.setText("");
				ta2.setText("");
				if (titolo.getText().length()==0) 
					infolabel2.setText("Il campo IdPost è vuoto");
				else {
					int idpost;
					try{
						idpost = Integer.parseInt(titolo.getText());
						if (ta.getText().length()== 0) {
								infolabel2.setText("Il campo Commento è vuoto");
						}
						else {
							try {						
								OutputStream output = socket.getOutputStream();
								PrintWriter writer = new PrintWriter(output, true);
								String string = new String ("AddComment::"+idpost+"::"+ta.getText());
								writer.println(string);	
													
								InputStream input = socket.getInputStream();
								BufferedReader reader = new BufferedReader(new InputStreamReader(input));
								String answer = reader.readLine();
								infolabel2.setText(answer);	
								ta.setText("");
								titolo.setText("");
								}
								catch (IOException e1) { 
									e1.printStackTrace();
								}
						}
					}
					catch (NumberFormatException ex){
						infolabel2.setText("Il campo IdPost non è un id");
							ex.printStackTrace();
					}
				}
								
			}
		});
		button_comment.add(jp2);
				
		//bottone per dare un voto positivo ad un post
		//prendo l'id del post da votare dal campo titolo
		x = x + 200;
		MyButton button_AggiungiVotoPositivo = new MyButton("Voto +1",x,y);
		button_AggiungiVotoPositivo.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent event) {
				infolabel2.setText("");
				ta2.setText("");
				if (titolo.getText().length()==0) 
					infolabel2.setText("Il campo IdPost è vuoto");
				else {
					int idpost;
					try{
						idpost = Integer.parseInt(titolo.getText());
						try {						
							OutputStream output = socket.getOutputStream();
							PrintWriter writer = new PrintWriter(output, true);
							String string = new String ("AggiungiVoto::"+idpost+"::"+1);
							writer.println(string);	
															
							InputStream input = socket.getInputStream();
							BufferedReader reader = new BufferedReader(new InputStreamReader(input));
							String answer = reader.readLine();	// reads a line of text

							infolabel2.setText(answer);						
						}
						catch (IOException e1) { 
							e1.printStackTrace();
						}
					}
					catch (NumberFormatException ex){
						infolabel2.setText("Il campo IdPost non è un id");
						ex.printStackTrace();
					}
				}
										
			}
		});
		button_AggiungiVotoPositivo.add(jp2);
		
		//bottone per dare un voto negativo ad un post
		//prendo l'id del post da votare dal campo titolo
		x = x + 200;
		MyButton button_AggiungiVotoNegativo = new MyButton("Voto -1",x,y);
		button_AggiungiVotoNegativo.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent event) {
				infolabel2.setText("");
				ta2.setText("");
				if (titolo.getText().length()==0) 
					infolabel2.setText("Il campo IdPost è vuoto");
				else {
					int idpost;
					try{
						idpost = Integer.parseInt(titolo.getText());
						try {	
							OutputStream output = socket.getOutputStream();
							PrintWriter writer = new PrintWriter(output, true);
							String string = new String ("AggiungiVoto::"+idpost+"::"+-1);
							writer.println(string);	
																	
							InputStream input = socket.getInputStream();
							BufferedReader reader = new BufferedReader(new InputStreamReader(input));
							String answer = reader.readLine();	// reads a line of text

							infolabel2.setText(answer);						
						}
						catch (IOException e1) { 
							e1.printStackTrace();
						}
					}
					catch (NumberFormatException ex){
						infolabel2.setText("Il campo IdPost non è un id");
						ex.printStackTrace();
					}
				}
												
			}
		});
		button_AggiungiVotoNegativo.add(jp2);
		
		//bottone per Condividere un Post, Rewin(post), aggiunge un post del feed al proprio blog
		//aggiorna la lista user.feed
		x = x + 200;
		MyButton button_RewinPost = new MyButton("RewinPost",x,y);
		button_RewinPost.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent event) {
				infolabel2.setText("");
				ta2.setText("");
				if (titolo.getText().length()==0) 
					infolabel2.setText("Il campo IdPost è vuoto");
				else {
					int idpost;
					try{
						idpost = Integer.parseInt(titolo.getText());
						try {	
							OutputStream output = socket.getOutputStream();
							PrintWriter writer = new PrintWriter(output, true);
							String string = new String ("RewinPost::"+idpost);
							writer.println(string);	
																	
							InputStream input = socket.getInputStream();
							BufferedReader reader = new BufferedReader(new InputStreamReader(input));
							String answer = reader.readLine();	// reads a line of text

							infolabel2.setText(answer);						
						}
						catch (IOException e1) { 
							e1.printStackTrace();
						}
					}
					catch (NumberFormatException ex){
						infolabel2.setText("Il campo IdPost non è un id");
						ex.printStackTrace();
					}
				}
												
			}
		});
		button_RewinPost.add(jp2);
		
		x = x -400;
		y = y+100;
		//bottone che mostra il wallet
		MyButton view_wallet = new MyButton("ViewWallet",x,y);
		view_wallet.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent event) {
				ta2.setText("");
				infolabel2.setText("");
				try {
					OutputStream output = socket.getOutputStream();
					PrintWriter writer = new PrintWriter(output, true);
					String string = new String ("ViewWallet");
					writer.println(string);	
					
					InputStream input = socket.getInputStream();
					BufferedReader reader = new BufferedReader(new InputStreamReader(input));
					String answer;
					while(!(answer = reader.readLine()).equals("/nnull")) {
						ta2.append(answer+"\n");
					}
				}
				catch (IOException e1) {
					e1.printStackTrace();
				}
			}
		});
		view_wallet.add(jp2);
		
		y = y+100;
		//bottone che mostra il valore in bitcoin del proprio wallet
		MyButton button_wallet_bitcoin = new MyButton("WalletInBicoin",x,y);
		button_wallet_bitcoin.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent event) {
				infolabel2.setText("");
				ta2.setText("Sto calcolando . . .");
				try {
					OutputStream output = socket.getOutputStream();
					PrintWriter writer = new PrintWriter(output, true);
					String string = new String ("WalletInBicoin");
					writer.println(string);	
					
					InputStream input = socket.getInputStream();
					BufferedReader reader = new BufferedReader(new InputStreamReader(input));
					String answer = reader.readLine();
					ta2.setText(answer);
				}
				catch (IOException e1) {
					e1.printStackTrace();
				}
			}
		});
		button_wallet_bitcoin.add(jp2);
		
		
		//bottone per visualizzazione blog personale(ovvero i post di cui l'utente è autore)
		//e i post che ho condiviso
		y=y-200;
		x=8;
		y= y + altezza_finestra/16+350;
		MyButton button_blog = new MyButton("ViewBlog",x,y);
		button_blog.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent event) {
				ta2.setText("");
				infolabel2.setText("");
				try {
					OutputStream output = socket.getOutputStream();
					PrintWriter writer = new PrintWriter(output, true);
					String string = new String ("ViewBlog");
					writer.println(string);	
					
					InputStream input = socket.getInputStream();
					BufferedReader reader = new BufferedReader(new InputStreamReader(input));
					String answer;
					while(!(answer = reader.readLine()).equals("/nnull")) {
						ta2.append(answer+"\n");
					}
				}
				catch (IOException e1) {
					e1.printStackTrace();
				}
			}
		});
		button_blog.add(jp2);
		
		//bottone per la visualizzazione del proprio feed (ovvero i post che sono degli utenti che segue)
		//e i post che loro hanno condiviso
		x=x+200;
		MyButton button_show_feed = new MyButton("ViewFeed",x,y);
		button_show_feed.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent event) {
				ta2.setText("");
				infolabel2.setText("");
				try {
					OutputStream output = socket.getOutputStream();
					PrintWriter writer = new PrintWriter(output, true);
					String string = new String ("ViewFeed");
					writer.println(string);	
					
					InputStream input = socket.getInputStream();
					BufferedReader reader = new BufferedReader(new InputStreamReader(input));
					String answer;
					while(!(answer = reader.readLine()).equals("/nnull")) {
						ta2.append(answer+"\n");
					}
				}
				catch (IOException e1) {
					e1.printStackTrace();
				}
			}
		});
		button_show_feed.add(jp2);
		
		//bottone per pulire tutti i campi di testo
		x=8;
		y= y+50;
		MyButton button_pulisci = new MyButton("Pulisci",x,y);
		button_pulisci.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent event) {
				infolabel2.setText("");
				titolo.setText("");
				ta.setText("");
				ta2.setText("");
				label_ricerca.reset();
			}
		});
		button_pulisci.add(jp2);
		
		window2.setContentPane(jp2);


		System.out.println(Username);

	}
	
	public static void close_window2() {
		window2.setVisible(false);
	}	
	
	public static void open_window2() {
		window2.setVisible(true);
	}
	
	public synchronized static void NewEventFollowing(String request) {
		String[] r = request.split("::");
		if (r[1].equals(Username)) {
			if (r[0].equals("following")){
				following.add(r[2]);
				infolabel2.setText("L'utente "+r[2]+" ha deciso di seguirti");

			}
			else {
				following.remove(r[2]);
				infolabel2.setText("L'utente "+r[1]+" ha deciso di non seguirti più");
			}

		}
	}
	
	public static void main (String args[]) {
		Username = null;
		myTags = null;
		window1();
	}
	
	public synchronized static void aggiornaInfolable2(String text) {
		infolabel2.setText(text);

	}
}