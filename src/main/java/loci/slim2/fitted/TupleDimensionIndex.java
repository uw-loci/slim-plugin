/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package loci.slim2.fitted;

import java.util.List;
import net.imglib2.RandomAccess;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;

/**
 *
 * @author Aivar Grislis
 */
	/**
	 * Class that describes a single index of the tuple dimension.
	 * 
	 * @param <T> type of the values
	 * 
	 * @author Aivar Grislis
	 */
	public class TupleDimensionIndex <T extends RealType<T> & NativeType<T>> {
		private final int POST_XY_INDEX = 2;
		private final String label;
		private final int index;
		private final TupleFormula<T> formula;
		private boolean combined;
		private RandomAccess<T> randomAccess;

		/**
		 * Constructor.
		 * 
		 * @param label name of this index value
		 * @param index index in output (for combined images)
		 * @param formula used to derive index value
		 */
		public TupleDimensionIndex(String label, int index, TupleFormula<T> formula) {
			this.label = label;
			this.index = index;
			this.formula = formula;
		}
		
		public String getLabel() {
			return label;
		}
		
		public int getIndex() {
			return index;
		}
		
		public TupleFormula getFormula() {
			return formula;
		}
		
		public void setCombined(boolean combined) {
			this.combined = combined;
		}
		
		public void setRandomAccess(RandomAccess<T> randomAccess) {
			this.randomAccess = randomAccess;
		}
		
		public void setPixelValue(List<T> values, long[] position, int[] chunkyPixelSize) {
			T value = formula.compute(values);
			long x = position[0];
			long y = position[1];
			for (int i = 0; i < chunkyPixelSize[0]; ++i) {
				for (int j = 0; j < chunkyPixelSize[1]; ++j) {
					position[0] = x + i;
					position[1] = y + j;
					setPixelValue(value, position);
				}
			}
		}
		
		public void setPixelValue(List<T> values, long[] position) {
			T value = formula.compute(values);
			setPixelValue(value, position);
		}
		
		public void setPixelValue(T value, long[] position) {
			if (combined) {
				// adjust position for combined stack
				position = expandPosition(position, POST_XY_INDEX);
				position[POST_XY_INDEX] = index;
			}
			randomAccess.setPosition(position);
			randomAccess.get().set(value);
		}
		
		private long[] expandPosition(long[] position, int index) {
			long[] expandedPosition = new long[position.length + 1];
			for (int i = 0; i < position.length; ++i) {
				if (i < index) {
					expandedPosition[i] = position[i];
				}
				else {
					expandedPosition[i] = position[i - 1];
				}
			}
			return expandedPosition;
		}
	}
