package com.iotsec.nvr.parse;

import java.io.IOException;
import java.io.InputStream;


public class SVCStreamReader {

	SVCPacket packet = new SVCPacket();

	byte[] buffer = new byte[1000000];

	int pointer = -1;
	int packet_end = -1; // excluding packet index
	boolean isValidPacket = false;

	InputStream instream;
	public boolean isFinished = false;

	public SVCStreamReader(InputStream in) {
		instream = in;
	}

	public SVCPacket readPacket() throws IOException {
		if (isFinished) {
			return null;
		}

		boolean nextPacket = false;
		while (true) {
			if (pointer >= 3 && SVCPacket.isNewNalUnit(buffer, pointer - 3)) {
				if (!isValidPacket) { // 是一个完整的包
					isValidPacket = true;
					System.arraycopy(buffer, pointer - 3, buffer, 0, 4);
					pointer = 3;
				} else if (pointer >= 4) {
					packet_end = pointer - 3;
					nextPacket = true;
					break;
				}
			}

			int t = instream.read();

			if (t == -1) // if we are end of file
			{
				isFinished = true;
				break;
			}

			buffer[++pointer] = (byte) t;

			if (pointer == 4) {
				packet.init(buffer, 0, 5);
				if (packet.isEnd()) {
					pointer = -1;
					return packet.clone();
				}
			}
		}

		if (isFinished) {
			packet_end = pointer + 1;
		}

		packet.init(buffer, 0, packet_end);

		if (nextPacket) {
			System.arraycopy(buffer, packet_end, buffer, 0, 4);
			pointer = 3;
			packet_end = -1;
			nextPacket = false;
		}

		return packet.clone();
	}
}
