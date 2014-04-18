package com.iotsec.nvr.security;

import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.util.Date;
import java.util.Random;

import org.apache.hadoop.io.Text;

public class HE {
	private BigInteger p = new BigInteger(
			"104812793866182138048883244626894518060105735191321171857752153202109453812252742470355591068218402639137418911727854660961028292808036796045745021002822716743748361769998485511261466381290211006841093459984004378860033337390160613420663709015478839579440990130415881415703953337339075514810864387222570974299");
	private BigInteger q = new BigInteger(
			"128576511272352746371519803808388028050051509088371558930585033986048953347956932645718938270700619512529243206109634703960936384753836705636266883025317617677791412925150143812623022407483213314672173061697057577996254979536486285544608471972409616206768580062327480199708540094286238486425373186472456976759");
	private BigInteger n = p.multiply(q);
	private Random ran = new Random(47);
	private ByteBuffer bb = ByteBuffer.allocate(8);
	private Text t = new Text();

	private HE() {

	}

	private static volatile HE he;

	public static HE getInstance() {

		if (he == null) {
			synchronized (HE.class) {
				if (he == null) {
					he = new HE();
				}
			}
		}
		return he;
	}

	public synchronized Text en(byte[] b, int start, int len) {
		bb.put((byte) 0x00);
		bb.put((byte) 0x00);
		bb.put(b, start, len);
		bb.flip();
		long l = bb.asLongBuffer().get(0);
		bb.clear();
		System.out.println(l);
		System.out.println(new Date(l));
		return en(l);
	}

	public synchronized Text en(long l) {
		System.out.println(l);
		BigInteger lb = new BigInteger(new Long(l).toString());
		t.set(p.multiply(
				new BigInteger(new Integer(ran.nextInt(1000)).toString()))
				.add(lb).mod(n).toString());
		return t;
	}

	public synchronized int res(Text t1, Text t2) {
		BigInteger b1 = new BigInteger(t1.toString());
		BigInteger b2 = new BigInteger(t2.toString());
		return (b1.subtract(b2).multiply(q).multiply(new BigInteger(
				new Integer(ran.nextInt(1000)).toString()))).mod(n).intValue();
	}
	
	public synchronized long de(Text t){
		BigInteger b = new BigInteger(t.toString());
		return b.mod(p).longValue();
	}
}
