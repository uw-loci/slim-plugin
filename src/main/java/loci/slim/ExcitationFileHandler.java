//
// ExcitationFileHandler.java
//

/*
SLIMPlugin for combined spectral-lifetime image analysis.

Copyright (c) 2010, UW-Madison LOCI
All rights reserved.

Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions are met:
    * Redistributions of source code must retain the above copyright
      notice, this list of conditions and the following disclaimer.
    * Redistributions in binary form must reproduce the above copyright
      notice, this list of conditions and the following disclaimer in the
      documentation and/or other materials provided with the distribution.
    * Neither the name of the UW-Madison LOCI nor the
      names of its contributors may be used to endorse or promote products
      derived from this software without specific prior written permission.

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDERS OR CONTRIBUTORS BE
LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
POSSIBILITY OF SUCH DAMAGE.
*/

package loci.slim;

import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Scanner;

import loci.formats.FormatException;
import loci.formats.in.ICSReader;
import loci.formats.meta.MetadataRetrieve;
import loci.formats.meta.MetadataStore;
import loci.formats.out.ICSWriter;

import mpicbg.imglib.container.planar.PlanarContainerFactory;
import mpicbg.imglib.cursor.Cursor;
import mpicbg.imglib.cursor.LocalizableByDimCursor;
import mpicbg.imglib.image.Image;
import mpicbg.imglib.image.ImageFactory;
import mpicbg.imglib.io.ImageOpener;
import mpicbg.imglib.type.numeric.RealType;
import mpicbg.imglib.type.numeric.real.DoubleType;

/**
 * TODO
 *
 * <dl><dt><b>Source code:</b></dt>
 * <dd><a href="http://dev.loci.wisc.edu/trac/software/browser/trunk/projects/slim-plugin/src/main/java/loci/slim/ExcitationFileHandler.java">Trac</a>,
 * <a href="http://dev.loci.wisc.edu/svn/software/trunk/projects/slim-plugin/src/main/java/loci/slim/ExcitationFileHandler.java">SVN</a></dd></dl>
 *
 * @author Aivar Grislis
 */
public class ExcitationFileHandler <T extends RealType<T>> {
    private static final String ICS = ".ics";
    private static final String IRF = ".irf";
    private static ExcitationFileHandler s_instance = null;

    public static synchronized ExcitationFileHandler getInstance() {
        if (null == s_instance) {
            s_instance = new ExcitationFileHandler();
        }
        return s_instance;
    }

    public Excitation loadExcitation(String fileName, float timeInc) {
        Excitation excitation = null;
        float values[] = null;
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

    public Excitation createExcitation(String fileName, float[] values, float timeInc) {
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

    private float[] loadICSExcitationFile(String fileName) {
        float[] results = null;
        ICSReader icsReader = new ICSReader();
        try {
            icsReader.setId(fileName);
            //System.out.println(" is single file " + icsReader.isSingleFile(fileName));
            String domains[] = icsReader.getDomains();
            int lengths[] = icsReader.getChannelDimLengths();
            //System.out.print("lengths");
            //for (int i : lengths) {
            //    System.out.print(" " + i);
            //}
            //System.out.println();
            String types[] = icsReader.getChannelDimTypes();
            int sizeX = icsReader.getSizeX();
            int sizeY = icsReader.getSizeY();
            int sizeZ = icsReader.getSizeZ();
            int sizeT = icsReader.getSizeT();
            int sizeC = icsReader.getSizeC();
            int bpp = icsReader.getBitsPerPixel();
            String dimOrder = icsReader.getDimensionOrder();
            int pixelType = icsReader.getPixelType();
            int effSizeC = icsReader.getEffectiveSizeC();
            boolean littleEndian = icsReader.isLittleEndian();
            //System.out.println("size X Y Z T C " + sizeX + " " + sizeY + " " + sizeZ + " " + sizeT + " " + sizeC + " ");
            //System.out.println("bpp " + bpp + " dim order " + dimOrder + " pixelTYpe + " + pixelType + " effsizec " + effSizeC + " littleendian " + littleEndian);

            System.out.println(icsReader.getFormat());
            int bins = icsReader.getSizeC();
            results = new float[bins];
            byte bytes[] = new byte[4];
            for (int bin = 0; bin < bins; ++bin) {
                bytes = icsReader.openBytes(bin);
                //for (byte b : bytes) {
                //    System.out.print(" " + b);
                //}
                //System.out.println();
                results[bin] = convertBytesToFloat(bytes);
            }
            icsReader.close();
        }
        catch (IOException e) {
            System.out.println("IOException " + e.getMessage());
        }
        catch (FormatException e) {
            System.out.println("FormatException " + e.getMessage());
        }
        return results;
    }

    //TODO doesn't work; needed to interoperate with TRI2
    private boolean saveICSExcitationFile(String fileName, float[] values) {
        boolean success = false;
        ICSWriter icsWriter = new ICSWriter();
        MetadataRetrieve meta = null;
//        icsWriter.setMetadataRetrieve(meta);
        try {
            for (int bin = 0; bin < values.length; ++bin) {
                icsWriter.saveBytes(bin, convertFloatToBytes(values[bin]));
            }
            success = true;
        }
        catch (IOException e) {
            System.out.println("IOException " + e.getMessage());
        }
        catch (FormatException e) {
            System.out.println("FormatException " + e.getMessage());
        }        
        return success;
    }

    private float[] loadIRFExcitationFile(String fileName) {
        float[] values = null;
        try {
            ArrayList<Float> valuesArrayList = new ArrayList<Float>();
            Scanner scanner = new Scanner(new FileReader(fileName));
            String line = null;
            while (scanner.hasNextLine()) {
                line = scanner.nextLine();
                valuesArrayList.add(Float.parseFloat(line));
            }
            values = new float[valuesArrayList.size()];
            for (int i = 0; i < valuesArrayList.size(); ++i) {
                values[i] = valuesArrayList.get(i);
            }
        }
        catch (Exception e) {
            System.out.println("Exception " + e.getMessage());
        }
        return values;
    }

    private boolean saveIRFExcitationFile(String fileName, float[] values) {
        boolean success = false;
        try {
            FileWriter writer = new FileWriter(fileName);
            for (int i = 0; i < values.length; ++i) {
                if (i > 0) {
                    writer.append('\n');
                }
                writer.append(Float.toString(values[i]));
            }
            writer.flush();
            writer.close();
            success = true;
        }
        catch (IOException e) {
            System.out.println("IOException " + e.getMessage());
        }
        return success;
    }

    private byte[] convertFloatToBytes(float f) {
        int rawIntBits = Float.floatToRawIntBits(f);
        byte[] result = new byte[4];
        for (int i = 0; i < 4; ++i) {
            int offset = 8 * i;
            result[i] = (byte) ((rawIntBits >>> offset) & 0xff);
        }
        return result;
    }

    /**
     * Converts a little-endian four byte array to a float.
     *
     * @param bytes
     * @return
     */
    private float convertBytesToFloat(byte[] bytes) {
        int i = 0;
        i |= bytes[3] & 0xff;
        i <<= 8;
        i |= bytes[2] & 0xff;
        i <<= 8;
        i |= bytes[1] & 0xff;
        i <<= 8;
        i |= bytes[0] & 0xff;
        return Float.intBitsToFloat(i);
    }
}