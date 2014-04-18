package com.iotsec.nvr.store;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.Socket;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Timer;
import java.util.TimerTask;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FSDataOutputStream;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.IOUtils;
import org.apache.hadoop.io.SequenceFile;
import org.apache.hadoop.io.Text;

import com.iotsec.nvr.parse.*;
import com.iotsec.nvr.security.HE;

public class SocketThread extends Thread {

	private static final Log LOG = LogFactory.getLog(Configuration.class);

	private static Configuration conf = new Configuration();
	private static FileSystem fs;

	private Socket socket;
	// private byte[] buf = new byte[4096];
	private FSDataOutputStream out = null;
	private InputStream in = null;
	private SVCStreamReader ssr = null;
	private Timer reportOff = new Timer(true);
	private Text tsKey = new Text();
	private Text indexValue = new Text();
	private SequenceFile.Writer indexOut = null;
	private Object outFileSynchronized = new Object();

	SocketThread(Socket socket) throws IOException {
		this.socket = socket;
		if (fs == null) {
			conf.addResource("configuration.xml");
			fs = FileSystem.get(conf);
		}
		LOG.info(socket.getRemoteSocketAddress() + " connect to NVR");
	}

	public Date firstUpdateTime() {
		Calendar calendar = new GregorianCalendar();
		Date date = new Date();
		calendar.setTime(date);
		// 系统运行时一天存一次
		calendar.add(Calendar.DAY_OF_MONTH, 1);
		calendar.set(Calendar.HOUR_OF_DAY, 0);
		calendar.set(Calendar.MINUTE, 0);
		calendar.set(Calendar.SECOND, 0);
		calendar.set(Calendar.MILLISECOND, 0);

		// 调试阶段

		/*
		 * calendar.add(Calendar.MINUTE, +(int)
		 * (conf.getLong("video.store.interval", 5184000) / 60000));
		 */
		return calendar.getTime();
	}

	public void updateOutPutStream() {
		try {

			String remoteAddress = socket.getRemoteSocketAddress().toString()
					.replace(':', '_');
			/*
			 * String date = new SimpleDateFormat("yyyyMMddhhmm") .format(new
			 * Date());
			 */

			/*
			 * int portIndex = socket.getRemoteSocketAddress().toString()
			 * .indexOf(":"); String remoteAddress =
			 * socket.getRemoteSocketAddress().toString() .substring(0,
			 * portIndex);
			 */
			String date = new SimpleDateFormat("yyyyMMdd").format(new Date());

			String videoFilePathString = conf.get("videoserver.url")
					+ remoteAddress + "_" + date + ".264";
			String indexFilePathString = conf.get("indexfile.url")
					+ remoteAddress + "_" + date;
			Path videoFilePath = new Path(videoFilePathString);
			Path indexFilePath = new Path(indexFilePathString);

			synchronized (outFileSynchronized) {
				if (fs.exists(videoFilePath))
					out = fs.append(videoFilePath);
				else {
					if (out != null) {
						out.close();
						out = null;
						IOUtils.closeStream(out);
					}
					out = fs.create(videoFilePath, false);
				}
				if (fs.exists(indexFilePath))
					indexOut = SequenceFile.createWriter(fs, conf,
							indexFilePath, tsKey.getClass(),
							indexValue.getClass());
				else {
					if (indexOut != null) {
						indexOut.close();
						indexOut = null;
						IOUtils.closeStream(indexOut);
					}
					indexOut = SequenceFile.createWriter(fs, conf,
							indexFilePath, tsKey.getClass(),
							indexValue.getClass());
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void run() {
		try {
			in = new BufferedInputStream(socket.getInputStream());
			// int bytesRead = in.read(buf, 0, buf.length);
			if (in.read() >= 0) {
				updateOutPutStream();
				reportOff.schedule(new TimerTask() {
					@Override
					public void run() {
						// TODO Auto-generated method stub
						updateOutPutStream();
					}

				}, firstUpdateTime(),
						conf.getLong("video.store.interval", 120000));
			}
			ssr = new SVCStreamReader(in);
			while (!ssr.isFinished) {
				SVCPacket packet = ssr.readPacket();
				if (packet.nal_type == 0) {

					synchronized (outFileSynchronized) {

						tsKey.set(HE.getInstance().en(packet.raw, 5, 6));

						indexValue.set(HE.getInstance().en(out.getPos()));

						indexOut.append(tsKey, indexValue);

						System.out.println(tsKey);
						System.out.println(indexValue);

					}
				} else {
					synchronized (outFileSynchronized) {
						out.write(packet.raw, 0, packet.raw.length);
					}
				}
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} finally {
			try {
				out.close();
				out = null;
				in.close();
				in = null;
				indexOut.close();
				indexOut = null;
				IOUtils.closeStream(indexOut);
				IOUtils.closeStream(out);
				IOUtils.closeStream(in);
				reportOff.cancel();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			IOUtils.closeSocket(socket);
		}
	}
}