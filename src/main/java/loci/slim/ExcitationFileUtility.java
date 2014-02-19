/*
 * #%L
 * SLIM plugin for combined spectral-lifetime image analysis.
 * %%
 * Copyright (C) 2010 - 2014 Board of Regents of the University of
 * Wisconsin-Madison.
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/gpl-3.0.html>.
 * #L%
 */

package loci.slim;

import ij.IJ;
import io.scif.ByteArrayPlane;
import io.scif.FormatException;
import io.scif.ImageMetadata;
import io.scif.SCIFIO;
import io.scif.Writer;
import io.scif.common.DataTools;
import io.scif.filters.ReaderFilter;
import io.scif.img.axes.SCIFIOAxes;

import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Scanner;

/**
 * Loads and saves excitation files.
 *
 * @author Aivar Grislis
 */
public class ExcitationFileUtility {
	private static final String ICS = ".ics";
	private static final String IRF = ".irf";

	public static Excitation loadExcitation(String fileName, double timeInc) {
		Excitation excitation = null;
		double values[] = null;
		if (fileName.toLowerCase().endsWith(ICS)) {
			values = loadICSExcitationFile(fileName);
		}
		else {
			if (!fileName.toLowerCase().endsWith(IRF)) {
				fileName += IRF;
			}
			values = loadIRFExcitationFile(fileName);
		}
		if (null != values) {
			excitation = new Excitation(fileName, values, timeInc);
		}
		return excitation;
	}

	public static Excitation createExcitation(String fileName, double[] values, double timeInc) {
		Excitation excitation = null;
		boolean success = false;
		if (fileName.endsWith(ICS)) {
			success = saveICSExcitationFile(fileName, values);
		}
		else {
			if (!fileName.endsWith(IRF)) {
				fileName += IRF;
			}
			success = saveIRFExcitationFile(fileName, values);
		}
		if (success) {
			excitation = new Excitation(fileName, values, timeInc);
		}
		return excitation;
	}

	private static double[] loadICSExcitationFile(String fileName) {
		double[] results = null;
		try {
			final SCIFIO scifio = new SCIFIO();
			final ReaderFilter reader = scifio.initializer().initializeReader(fileName);
			final ImageMetadata meta = reader.getMetadata().get(0);
			final int bitsPerPixel = meta.getBitsPerPixel();
			final int bytesPerPixel = bitsPerPixel / 8;
			final boolean littleEndian = meta.isLittleEndian();
			final boolean interleaved = false; // meta.isInterleaved(); // CTR FIXME use ChannelSeparator to prevent interleaved
			long lifetimeLength = meta.getAxisLength(SCIFIOAxes.LIFETIME);
			if (lifetimeLength > Integer.MAX_VALUE) {
				throw new IllegalArgumentException("Lifetime dimension too large: " + lifetimeLength);
			}
			int bins = (int) lifetimeLength;
			results = new double[bins];
			byte bytes[];
			if (interleaved) { //TODO ARG interleaved does not read the whole thing; was 130K, now 32767
				// this returns the whole thing
				bytes = reader.openPlane(0, 0).getBytes();
				IJ.log("INTERLEAVED reads # bytes: " + bytes.length);
				for (int bin = 0; bin < bins; ++bin) {
					results[bin] = convertBytesToDouble(littleEndian, bitsPerPixel, bytes, bytesPerPixel * bin);
				}
			}
			else {
				for (int bin = 0; bin < bins; ++bin) {
					bytes = reader.openPlane(0, bin).getBytes();
					results[bin] = convertBytesToDouble(littleEndian, bitsPerPixel, bytes, 0);
				}
			}
			reader.close();
		}
		catch (IOException e) {
			IJ.log("IOException " + e.getMessage());
		}
		catch (FormatException e) {
			IJ.log("FormatException " + e.getMessage());
		}
		return results;
	}

	//TODO doesn't work; needed to interoperate with TRI2
	private static boolean saveICSExcitationFile(String fileName, double[] values) {
		boolean success = false;
		final SCIFIO scifio = new SCIFIO();
		// NB: Use a fake string as a shorthand for metadata values.
		final String source =
			"pixelType=uint16&lengths=1,1," + values.length +
				"&axes=X,Y,Lifetime.fake";
		try {
			final Writer writer =
					scifio.initializer().initializeWriter(source, fileName);
			// TODO: Writer may require bytes to be structured according to a
			// particular endianness. But at this point, is it possible yet to
			// interrogate the writer to ask for its desired endianness?
			final boolean little = true;
			final ByteArrayPlane plane = new ByteArrayPlane(scifio.getContext());
			for (int bin = 0; bin < values.length; ++bin) {
				plane.setData(DataTools.doubleToBytes(values[bin], little));
				writer.savePlane(0, bin, plane);
			}
			success = true;
		}
		catch (IOException e) {
			IJ.log("IOException " + e.getMessage());
		}
		catch (FormatException e) {
			IJ.log("FormatException " + e.getMessage());
		}
		return success;
	}

	private static double[] loadIRFExcitationFile(String fileName) {
		double[] values = null;
		try {
			ArrayList<Float> valuesArrayList = new ArrayList<Float>();
			Scanner scanner = new Scanner(new FileReader(fileName));
			String line = null;
			while (scanner.hasNextLine()) {
				line = scanner.nextLine();
				valuesArrayList.add(Float.parseFloat(line));
			}
			values = new double[valuesArrayList.size()];
			for (int i = 0; i < valuesArrayList.size(); ++i) {
				values[i] = valuesArrayList.get(i);
			}
		}
		catch (Exception e) {
			IJ.log("Exception " + e.getMessage());
		}
		return values;
	}

	private static boolean saveIRFExcitationFile(String fileName, double[] values) {
		boolean success = false;
		try {
			FileWriter writer = new FileWriter(fileName);
			for (int i = 0; i < values.length; ++i) {
				if (i > 0) {
					writer.append('\n');
				}
				writer.append(Double.toString(values[i]));
			}
			writer.flush();
			writer.close();
			success = true;
		}
		catch (IOException e) {
			IJ.log("IOException " + e.getMessage());
		}
		return success;
	}

	/**
	 * Converts a four- or eight-byte array to a double.
	 *
	 * @param littleEndian byte order
	 * @param bitsPerPixel
	 * @param bytes
	 * @param index
	 * @return
	 */
	private static double convertBytesToDouble(boolean littleEndian,
		int bitsPerPixel, byte[] bytes, int index)
	{
		if (bitsPerPixel == 32) {
			return DataTools.bytesToFloat(bytes, index, littleEndian);
		}
		if (bitsPerPixel == 64) {
			return DataTools.bytesToDouble(bytes, index, littleEndian);
		}
		throw new IllegalArgumentException("Invalid bits per pixel: " +
			bitsPerPixel);
	}
}