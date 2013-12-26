package com.iotsec.nvr.parse;

import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;

public class BitInputStream {

	private InputStream iIs;
	private int iBuffer;
	private int iNextBit = 8;

	public BitInputStream(InputStream aIs) {
		iIs = aIs;
	}

	synchronized public int readBits(final int j) throws IOException {
		int value = 0;
		for (int i = j - 1; i >= 0; i--) {
			value |= (readBit() << i);
		}
		return value;
	}

	synchronized public int readBit() throws IOException {
		if (iIs == null)
			throw new IOException("Already closed");

		if (iNextBit == 8) {
			iBuffer = iIs.read();

			if (iBuffer == -1)
				throw new EOFException();

			iNextBit = 0;
		}

		int bit = iBuffer & (1 << 7 - iNextBit);
		iNextBit++;

		bit = (bit == 0) ? 0 : 1;

		return bit;
	}

	public void close() throws IOException {
		iIs.close();
		iIs = null;
	}

	public int readExpGolomb() throws IOException {
		int leadingZeroBits = -1;

		for (byte b = 0; b == 0; leadingZeroBits++) {
			b = (byte) readBit();
		}

		int codeNum = (int) (Math.pow(2, leadingZeroBits)) - 1
				+ readBits(leadingZeroBits);

		return codeNum;
	}
}
