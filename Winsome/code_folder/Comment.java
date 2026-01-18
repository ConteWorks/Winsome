
public class Comment {
	private String text;
	private String autore;
	private boolean iterato; //indica se il commento è già stato utilizzato per il calcolo delle ricompense

	public Comment(String autore, String text) {
		this.autore= autore;
		this.text = text;
		iterato = false;
	}
	
	public String  getautore() {
		return autore;
	}
	
	public String  getText() {
		return text;
	}
	
	public boolean getIterato() {
		return iterato;
	}
	public void setIterato() {
		iterato = true;
	}
}

