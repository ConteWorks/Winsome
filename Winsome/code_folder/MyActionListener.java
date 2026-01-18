//client
//ActionListener del botton "Register"

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.StringTokenizer;

import javax.swing.JLabel;


public class MyActionListener implements ActionListener {
	MyLabel label1;
	MyLabel label2;
	ArrayList<MyLabel> tags;
	RemoteServerInterface stub;
	JLabel infolable;

	public MyActionListener(RemoteServerInterface stub, MyLabel label1, MyLabel label2, ArrayList<MyLabel> tags, JLabel infolable) {
		this.stub = stub;
		this.label1 = label1;
		this.label2 = label2;
		this.tags = tags;
		this.infolable = infolable;
		

	}
		
	public void actionPerformed(ActionEvent e){
		String tag1, tag2, tag3, tag4, tag5;
		tag1 = tags.get(0).getText().toLowerCase();
		tag2 = tags.get(1).getText().toLowerCase();
		tag3 = tags.get(2).getText().toLowerCase();
		tag4 = tags.get(3).getText().toLowerCase();
		tag5 = tags.get(4).getText().toLowerCase();
		
		infolable.setText(null);	
		
		if (label1.getText()!= null && !label1.getText().equals("") && label2.getText() != null && !label2.getText().equals("")) {
			try {
				String risposta = null;
				risposta =  stub.RegistraUtente(label1.getText(), label2.getText(), tag1, tag2, tag3, tag4, tag5);
					
				infolable.setText(risposta);	
	
					
			} catch (RemoteException e1) {
				e1.printStackTrace();
			}
		}
		else 
			infolable.setText("Error: Username o Password mancante");	
    }
}
