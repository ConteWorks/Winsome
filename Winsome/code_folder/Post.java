//server
import java.util.ArrayList;

import com.google.gson.Gson;

public class Post {
	private int IdPost;
	private String autore;
	private int n_iterazioni;
	private String titolo;
	private String Textbody;
	private ArrayList <Comment> commenti; 
	private ArrayList<Voto> voti;
	
	public Post(int IdPost, String autore, String titolo, String body) {
		this.IdPost = IdPost;
		this.autore = autore;
		this.titolo = titolo;
		this.Textbody = body;
		this.n_iterazioni = 0;
		commenti = new ArrayList<Comment>();
		voti = new ArrayList <Voto>();
	}
	
	public Post(int IdPost, String autore, String titolo, String body, int n_iterazioni,  ArrayList <Comment> cm, ArrayList<Voto> vt) {
		this.IdPost = IdPost;
		this.autore = autore;
		this.titolo = titolo;
		this.Textbody = body;
		this.n_iterazioni = n_iterazioni;
		commenti = cm;
		voti = vt;
	}
	
	public int GetIdPost() {
		return IdPost;
	}
	
	public String GetTitolo() {
		return titolo;
	}
	
	public String getbody() {
		return Textbody;
	}
	public String GetAutore() {
		return autore;
	}
	
	public int getVotiPositivi(){
		int i=0;
		for (Voto v: voti) {
			if (v.getvoto() == 1)
				i++;
		}
		return i;	
	}
	
	public int getVotiNegativi(){
		int i=0;
		for (Voto v: voti) {
			if (v.getvoto() == -1)
				i++;
		}
		return i;	
	}
	
	public String getComments() {
		String risposta="";
		for (Comment c: commenti) {
			risposta = risposta+c.getautore()+": "+c.getText()+"\n\t";
		}
		return risposta;
	}
	
	public Comment GetCommento(int i) {
		if(i>=0 && i<commenti.size())
			return commenti.get(i);
		else
			return null;
	}
	
	
	public Voto Getvoti(int i) {
		if (i>=0 && i< voti.size())
			return voti.get(i);
		else 
			return null;
	}
	
	
	public void AddVoto(Voto v) {
		voti.add(v);
	}
	
	public String myToJson(){
    	Gson gson = new Gson();
		return gson.toJson(this, Post.class);	
    }
	
	public void AddComment(String autoreCommento, String bodyCommento) {
		commenti.add(new Comment(autoreCommento, bodyCommento));
	}
	
	public int getn_iterazioni() {
		return n_iterazioni;
	}
	
	public void addIterazione() {
		n_iterazioni ++;
	}
}


