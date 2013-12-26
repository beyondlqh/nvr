package com.iotsec.nvr.parse;

import java.io.ByteArrayInputStream;
import java.io.IOException;

import com.iotsec.nvr.parse.BitInputStream;

public class SVCPacket implements Cloneable
{
	// normal nal data
	int nal_ref_idc;
	public int nal_type;

	// normal nal header data in scalable context
	public boolean idr_flag;
	int priority_id;
	boolean no_inter_layer;

	public int dependency_id;
	int quality_id;
	int temporal_id;

	boolean use_ref_base;
	boolean discardable;
	boolean output;

	// slice data
	int first_mb_in_slice;
	int slice_type;
	// int pic_parameter_set_id;
	// int colour_plane_id;

	// int frame_num;

	// the whole packet
	public byte[] raw;

	public static boolean isNewNalUnit(byte[] buf, int i)
	{
		return buf[i + 0] == 0 && buf[i + 1] == 0 && buf[i + 2] == 0
				&& buf[i + 3] == 1;
	}

	public static void print_header()
	{
		System.out.println("sizeB  nal  Lid  Tid  Qid  IDR  BIP");
		System.out.println("=====  ===  ===  ===  ===  ===  ===");
	}

	public void print()
	{
		System.out.printf("%5d  %3d  %3s  %3s  %3s  %3s  %3s\n", raw.length,
				nal_type, dependency_id, temporal_id, quality_id, idr_flag
						? "x" : "", hasSliceHeader() ? slice_type : "");
	}

	// THIS IS INCOMPLETE!! CHANGE WHEN NEEDED
	public SVCPacket clone()
	{
		SVCPacket packet = new SVCPacket();
		packet.nal_type = nal_type;
		packet.dependency_id = dependency_id;
		packet.idr_flag = idr_flag;
		packet.temporal_id = temporal_id;
		packet.quality_id = quality_id;
		packet.slice_type = slice_type;
		packet.discardable = discardable;
		packet.raw = raw; // because of this, do not overwrite raw
		return packet;
	}

	public void init(byte[] buf, int i, int e)
	{
		init(buf, i, e, true);
	}

	// including i, excluding e
	public void init(byte[] buf, int i, int e, boolean copyRawData)
	{
		if (copyRawData)
		{
			raw = new byte[e - i];
			System.arraycopy(buf, i, raw, 0, e - i);
		}
		else
		{
			raw = buf;
		}

		assert (raw.length >= 5);

		ByteArrayInputStream bais =
				new ByteArrayInputStream(raw, 4, raw.length - 4);
		BitInputStream bis = new BitInputStream(bais);

		try
		{
			bis.readBit(); // forbidden zero bit
			nal_ref_idc = bis.readBits(2);
			nal_type = bis.readBits(5);

			if (isEnd())
			{
				dependency_id = 0;
			}
			
			// only these have SVC nal unit header information
			if (raw.length >= 9 && hasSVCHeader())
			{
				bis.readBit(); // reserved bit
				idr_flag = bis.readBit() == 1;
				priority_id = bis.readBits(6);

				no_inter_layer = bis.readBit() == 1;
				dependency_id = bis.readBits(3);
				quality_id = bis.readBits(4);

				temporal_id = bis.readBits(3);
				use_ref_base = bis.readBit() == 1;
				discardable = bis.readBit() == 1;
				output = bis.readBit() == 1;
				bis.readBits(2); // reversed bits
			}

			if (hasSliceHeader())
			{
				// might not work
				// first_mb_in_slice = bis.readExpGolomb();
				// slice_type = bis.readExpGolomb();
				// pic_parameter_set_id = bis.readExpGolomb();
				// frame_num = bis.readBits(?); // would require ParameterSet
				// readout
			}
		}
		catch (IOException ex)
		{
			ex.printStackTrace();
			System.exit(0);
		}
	}

	// maybe incomplete
	boolean isParameterSet()
	{
		// 7: sequence parameter set
		// 8: picture parameter set
		// 15: subset sequence parameter set (used by SVC extension)
		return nal_type == 7 || nal_type == 8 || nal_type == 15;
	}

	// SEI is the header for the stream
	boolean isStreamHeader()
	{
		return nal_type == 6;
	}

	boolean isHeader()
	{
		return isStreamHeader() || isParameterSet();
	}

	boolean hasSVCHeader()
	{
		return nal_type == 20 || nal_type == 14;
	}

	boolean hasSliceHeader()
	{
		return nal_type >= 1 && nal_type <= 5;
	}

	boolean isSequenceEnd()
	{
		return nal_type == 10;
	}

	boolean isStreamEnd()
	{
		return nal_type == 11;
	}

	boolean isEnd()
	{
		return isStreamEnd() || isSequenceEnd();
	}

	// for sending
	// type 10 = end of sequence (next nal packet shall be IDR / nal type 5)
	// type 11 = end of stream nothing comes afterwards
	public void setNalType(int type)
	{
		assert (type >= 0 && type <= 31);

		byte[] tmp = new byte[5];
		tmp[0] = 0;
		tmp[1] = 0;
		tmp[2] = 0;
		tmp[3] = 1;
		tmp[4] = (byte) type;

		init(tmp, 0, 5, false);
	}

	public void setStreamEnd()
	{
		setNalType(11);
	}

	public void setSequenceEnd()
	{
		setNalType(10);
	}
}
