//client

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.MulticastSocket;

public class UDPreceive implements Runnable{

	MulticastSocket ms;
	DatagramPacket dp;
	
	public UDPreceive(MulticastSocket ms, DatagramPacket dp) {
		this.dp = dp;
		this.ms = ms;
	}
	public void run() {
		String received="";
		while(true) {
			try {
				ms.receive(dp);
				received = new String(dp.getData());
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			WinsonClient.aggiornaInfolable2(received);
		}



	}
}

