package com.iotsec.nvr.store;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class WriteIntoHadoop {
	public static void main(String[] args) {

		ServerSocket s;
		try {
			s = new ServerSocket(10000);
			while (true) {
				Socket socket = s.accept();
				new SocketThread(socket).start();
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

}