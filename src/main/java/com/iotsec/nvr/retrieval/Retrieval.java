package com.iotsec.nvr.retrieval;

import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URI;
import java.util.Date;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FSDataInputStream;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.IOUtils;
import org.apache.hadoop.io.SequenceFile;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.util.ReflectionUtils;

import com.iotsec.nvr.security.HE;

public class Retrieval {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub

		String indexName = "hdfs://192.168.115.200/index/192.168.115.20020140324";
		String fileName = "hdfs://192.168.115.200/video/192.168.115.20020140324.264";
		String time = "5184102";
		System.out.println(new Date(Long.parseLong(time, 10)));
		Configuration conf = new Configuration();
		try {
			long start = System.currentTimeMillis();
			Text k = HE.getInstance().en(Long.parseLong(time, 10));
			FileOutputStream out = new FileOutputStream("output.264");
			FileSystem fs = FileSystem.get(URI.create(fileName), conf);
			FSDataInputStream in = fs.open(new Path(fileName));
			SequenceFile.Reader reader = new SequenceFile.Reader(fs, new Path(
					indexName), conf);
			Text key = (Text) ReflectionUtils.newInstance(reader.getKeyClass(),
					conf);
			Text value = (Text) ReflectionUtils.newInstance(
					reader.getValueClass(), conf);
			while (reader.next(key, value)) {
				if (HE.getInstance().res(k, key) == 0) {
					long l = HE.getInstance().de(value);
					System.out.println(l);
					in.seek(l);
					break;
				}
			}
			IOUtils.closeStream(reader);
			System.out.println(System.currentTimeMillis() - start);

			IOUtils.copyBytes(in, out, 4096, false);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

}
