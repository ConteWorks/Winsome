
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JPanel;

public class MyButton {
	JButton button;
	private static int lunghezza_button = 180;
	private static int altezza_button = 30;
	public MyButton(String nome, int x, int y) {
		button = new JButton(nome);
		button.setEnabled(true);
		button.setBounds(x,y,lunghezza_button,altezza_button);
	}
	public void add(JPanel jp) {
		jp.add(button);
	}
	public void addActionListener(ActionListener listener) {
		button.addActionListener(listener);
	}
}
