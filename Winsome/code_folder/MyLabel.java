import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

public class MyLabel{
	JLabel label;
	JTextField InputArea;
	private static int lunghezza_lable = 110;
	private static int altezza_lable = 30;
	public MyLabel(String nome, int x, int y){
		label =new JLabel(nome);
		label.setBounds(x,y,lunghezza_lable, altezza_lable);
		InputArea = new JTextField();
		InputArea.setBounds(x+100,y,lunghezza_lable,altezza_lable);
		InputArea.setText(null);
		InputArea.setEditable(true);	
	}
	
	public void add (JPanel jp) {
		jp.add(label);
		jp.add(InputArea);		
	}
	public String getText() {
		return InputArea.getText();
	}
	
	public void reset() {
		InputArea.setText("");
	}
}
