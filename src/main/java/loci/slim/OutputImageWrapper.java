/*
 * #%L
 * SLIM Curve plugin for combined spectral-lifetime image analysis.
 * %%
 * Copyright (C) 2010 - 2015 Board of Regents of the University of
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

import loci.slim.fitting.IFittedImage;
import net.imagej.ImgPlus;
import net.imglib2.Cursor;
import net.imglib2.RandomAccess;
import net.imglib2.img.planar.PlanarImgs;
import net.imglib2.type.numeric.real.DoubleType;

/**
 * This class wraps an image that is being used as output from a fit.
 *
 * @author Aivar Grislis
 */
public class OutputImageWrapper implements IFittedImage {

	private final ImgPlus<DoubleType> _image;
	private final int _width;
	private final int _height;
	private final int _channels;
	private final int _parameters;
	private final int _parameterIndex;
	private final RandomAccess<DoubleType> _cursor;
	private final int[] _location;

	/**
	 * Creates a wrapper for an output image and initial image.
	 *
	 */
	public OutputImageWrapper(final String title, final String fitTitle,
		final int width, final int height, final int channels, final int parameters)
	{
		_width = width;
		_height = height;
		_channels = channels;
		_parameters = parameters;

		final long[] dimensions =
			new long[] { width, height, channels, parameters };
		_parameterIndex = 3;
		_location = new int[dimensions.length];

		_image = new ImgPlus<DoubleType>(PlanarImgs.doubles(dimensions));
		_image.setName(title + " Fitted " + fitTitle);

		// fill image with NaNs
		final Cursor<DoubleType> cursor = _image.cursor();
		while (cursor.hasNext()) {
			cursor.fwd();
			cursor.get().set(Double.NaN);
		}

		_cursor = _image.randomAccess();
	}

	/**
	 * Gets width of image.
	 *
	 */
	@Override
	public int getWidth() {
		return _width;
	}

	/**
	 * Gets height of image.
	 *
	 */
	@Override
	public int getHeight() {
		return _height;
	}

	/**
	 * Gets number of channels of image.
	 *
	 */
	@Override
	public int getChannels() {
		return _channels;
	}

	/**
	 * Gets number of parameters of image.
	 *
	 */
	@Override
	public int getParameters() {
		return _parameters;
	}

	@Override
	public int[] getDimension() {
		final int[] dimension =
			new int[] { _width, _height, _channels, _parameters };
		return dimension;
	}

	@Override
	public double[] getPixel(final int[] location) {
		for (int i = 0; i < location.length; ++i) {
			_location[i] = location[i];
		}
		final double[] parameters = new double[_parameters];
		for (int i = 0; i < _parameters; ++i) {
			_location[_parameterIndex] = i;
			_cursor.setPosition(_location);
			parameters[i] = _cursor.get().getRealFloat();
		}
		return parameters;
	}

	@Override
	public void setPixel(final int[] location, final double[] value) {
		for (int i = 0; i < location.length; ++i) {
			_location[i] = location[i];
		}
		for (int i = 0; i < _parameters; ++i) {
			_location[_parameterIndex] = i;
			_cursor.setPosition(_location);
			// a pixel with an error fitting will have null value
			_cursor.get().set(null == value ? Double.NaN : value[i]);
		}
	}

	/**
	 * Gets associated image.
	 *
	 */
	@Override
	public ImgPlus<DoubleType> getImage() {
		return _image;
	}
}
