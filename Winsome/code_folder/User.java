//server  e client

import java.util.ArrayList;

import com.google.gson.Gson;



public class User {
	private String nickname;
	private String password;
	private String tag1 = null;
	private String tag2 = null;
	private String tag3 = null;
	private String tag4 = null;
	private String tag5 = null;
	//private boolean account_eliminato;
	private ArrayList<String> following; //utenti che seguono me
	private ArrayList<String> followers; //utenti che seguo
	private ArrayList<Integer> feed;  //indica gli id dei post condivisi, ovvero pubblicati sul proprio blog

    public  User(String nickname, String password, String tag1, String tag2, String tag3, String tag4, String tag5){
        this.nickname = nickname;
        this.password = password;
        this.tag1 = tag1;
        this.tag2 = tag2;
        this.tag3 = tag3;
        this.tag4 = tag4;
        this.tag5 = tag5;
        this.feed = new ArrayList<Integer>();
       // this.account_eliminato = false;
        this.following = new ArrayList<String>();
        this.followers = new ArrayList<String>();

    }
    
	

  /*  public  User(String nickname, String password){
        this.nickname = nickname;
        this.password = password;

    }*/
    
    public String GetNickname() {
    	return nickname;
    }
    
    public String GetPassword() {
    	return password;
    }
    
    public String mytoJson() {
    	Gson gson = new Gson();
		return gson.toJson(this, User.class);	
    }
    
 /*   public void SetEliminato() {
    	account_eliminato = false;
    }
    
    public boolean GetEliminato() {
    	return account_eliminato;
    }*/
    //restituisce una stringa con i nomi di tutti i follower, delimitati dal segno --
    public String GetFollowers() {
    	if (followers.isEmpty()) return null;
    	String f = new String();
    	for (String follower : followers)
    		f=follower+"--"+f;
    	return f; 
    }
    
    //restituisce una stringa con i nomi di tutti i folowing, delimitati dal segno --
    public String GetFollowings() {
    	if (following.isEmpty()) return null;
    	String f = new String();
    	for (String follow : following)
    		f=follow+"--"+f;
    	return f; 
    }
    
    public void AddFollower(String f) {
    	if (f!= null)
    		followers.add(f);
    }
    
    public void RemoveFollower(String f) {
    	if (f!= null)
    		followers.remove(f);
    }
    
    public void AddFollowing(String f) {
    	if (f!= null)
    		following.add(f);
    }
    
    public void RemoveFollowing(String f) {
    	if (f!= null)
    		following.remove(f);
    }
    	
    public String GetTags() {
    	String risposta = "";
    	if (tag1 != null)
    		risposta = risposta+tag1+"--";
    	if (tag2 != null)
    		risposta = risposta+tag2+"--";
    	if (tag3 != null)
    		risposta = risposta+tag3+"--";
    	if (tag4 != null)
    		risposta = risposta+tag4+"--";
    	if (tag5 != null)
    		risposta = risposta+tag5+"--";
    	return risposta+"";
    }
    
    public String GetTag1() { 
    	return tag1;
    }
    
    public String GetTag2() {
    	return tag2;
    }
    
    public String GetTag3() {
    	return tag3;
    }
    
    public String GetTag4() {
    	return tag4;
    }
    
    public String GetTag5() {
    	return tag5;
    }
    
    public int getFeed(int i) {
    	if (i<0)
    		return -1;
    	if (i>=feed.size())
    		return -1;
    	return feed.get(i);
    }
    
    public void addFeed(int idpost) {
    	feed.add(idpost);
    }
}
