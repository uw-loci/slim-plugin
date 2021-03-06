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

package loci.slim.ui;

import ij.IJ;
import ij.Prefs;
import ij.io.OpenDialog;
import ij.io.SaveDialog;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Dimension;
import java.awt.ItemSelectable;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.ArrayList;
import java.util.List;
import java.util.prefs.Preferences;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;
import javax.swing.SpinnerModel;
import javax.swing.SpinnerNumberModel;
import javax.swing.SpringLayout;
import javax.swing.SwingConstants;
import javax.swing.border.Border;
import javax.swing.border.EmptyBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import loci.curvefitter.ICurveFitter.FitAlgorithm;
import loci.curvefitter.ICurveFitter.FitFunction;
import loci.curvefitter.ICurveFitter.FitRegion;
import loci.curvefitter.ICurveFitter.NoiseModel;
import loci.curvefitter.IFitterEstimator;
import loci.slim.IThresholdUpdate;
import loci.slim.SLIMProcessor;
import loci.slim.paramSetMacro;
import loci.slim.fitting.cursor.FittingCursorHelper;
import loci.slim.fitting.cursor.IFittingCursorUI;

/**
 * Main user interface panel for the fit.
 *
 * @author Aivar Grislis
 */

public class UserInterfacePanel implements IUserInterfacePanel,
	IFittingCursorUI
{

	private static final String TITLE = "SLIM Curve";

	// Unicode special characters
	private static final Character CHI = '\u03c7', SQUARE = '\u00b2',
			TAU = '\u03c4', LAMBDA = '\u03bb', SIGMA = '\u03c3', SUB_1 = '\u2081',
			SUB_2 = '\u2082', SUB_3 = '\u2083', SUB_M = '\u2098', // Unicode 6.0.0
																														// (October 2010)
			SUB_R = '\u1d63';

	private static final String SUM_REGION = "Sum All Pixels",
			ROIS_REGION = "Sum Each ROI", PIXEL_REGION = "Single Pixel",
			ALL_REGION = "Images";

	private static final String JAOLHO_LMA_ALGORITHM = "Jaolho LMA",
			SLIM_CURVE_RLD_ALGORITHM = "SLIMCurve RLD",
			SLIM_CURVE_LMA_ALGORITHM = "SLIMCurve LMA",
			SLIM_CURVE_RLD_LMA_ALGORITHM = "SLIMCurve RLD+LMA";

	private static final String SINGLE_EXPONENTIAL = "Single Exponential",
			DOUBLE_EXPONENTIAL = "Double Exponential",
			TRIPLE_EXPONENTIAL = "Triple Exponential",
			STRETCHED_EXPONENTIAL = "Stretched Exponential";

	private static final String GAUSSIAN_FIT = "Gaussian Fit",
			POISSON_FIT = "Poisson Fit", POISSON_DATA = "Poisson Data",
			MAXIMUM_LIKELIHOOD = "Max. Likelihood Est.";

	private static final String CHI_SQ_TARGET = "" + CHI + SQUARE + SUB_R +
		" Target";

	private static final String EXCITATION_NONE = "None",
			EXCITATION_FILE = "Load from File",
			EXCITATION_CREATE = "Create from current X Y",
			EXCITATION_ESTIMATE = "Estimate from current X Y",
			EXCITATION_GAUSSIAN = "Gaussian",
			LOAD_DEFAULT = "Use default Excitation",
			SET_AS_DEFAULT = "Set as default";
	// /add something to add default excitation
	// /option:save as default //save as drop down box
	// /option: load default //check box would be fine I guess

	// /keys for excitation storign and retrieving

	public static final String KEY_EXCITATION = "key.excitation";
	public static final String KEY_BASE_PROMPT = "key.basePrompt";
	public static final String KEY_DELAY_TIME = "key.delaytime";
	public static final String KEY_WIDTH = "key.width";

	int countForExcitationCheck = 0;
	private static final String FIT_IMAGE = "Fit Images",
			FIT_PIXEL = "Fit Pixel", FIT_SUMMED_PIXELS = "Fit Summed Pixels",
			FIT_SUMMED_ROIS = "Fit Summed ROIs", CANCEL_FIT = "Cancel Fit";

	private static final Border EMPTY_BORDER = BorderFactory.createEmptyBorder(
		10, 10, 10, 10), ETCHED_BORDER = BorderFactory.createEtchedBorder();

	// TODO ARG fitting a series of ROIs is broken, so omit that possibility, for
	// now:
	private static final String REGION_ITEMS[] = { SUM_REGION, /*ROIS_REGION,*/
		PIXEL_REGION, ALL_REGION }, ALGORITHM_ITEMS[] = { JAOLHO_LMA_ALGORITHM,
		SLIM_CURVE_RLD_ALGORITHM, SLIM_CURVE_LMA_ALGORITHM,
		SLIM_CURVE_RLD_LMA_ALGORITHM }, FUNCTION_ITEMS[] = { SINGLE_EXPONENTIAL,
		DOUBLE_EXPONENTIAL, TRIPLE_EXPONENTIAL, STRETCHED_EXPONENTIAL },
			NOISE_MODEL_ITEMS[] = { GAUSSIAN_FIT, POISSON_FIT, POISSON_DATA,
				MAXIMUM_LIKELIHOOD };
	private static final String meanSingleStrings[]={"A",Character.toString(TAU),"Z",Character.toString(CHI)};
	private static final String meanDoubleStrings[]={"A1","A2",Character.toString(TAU)+"1",Character.toString(TAU)+"2","Z",Character.toString(CHI)};
	private static final String	meanTripleStrings[]={"A1","A2","A3",Character.toString(TAU)+"1",Character.toString(TAU)+"2",Character.toString(TAU)+"3","Z",Character.toString(CHI)};
	private static final String strechedExpo[]={"Z",Character.toString(CHI)};
	private static final String A_T_Z_X2 = "A " + TAU + " Z " + CHI + SQUARE,
			A_T_X2 = "A " + TAU + " " + CHI + SQUARE, A_T = "A " + TAU,
			F_UPPER_T_Z_X2 = "F " + TAU + " Z " + CHI + SQUARE, F_UPPER_T_X2 = "F " +
				TAU + " " + CHI + SQUARE, F_UPPER_T = "F " + TAU,
			F_LOWER_T_Z_X2 = "f " + TAU + " Z " + CHI + SQUARE, F_LOWER_T_X2 = "f " +
				TAU + " " + CHI + SQUARE, F_LOWER_T = "f " + TAU, T_X2 = TAU + " " +
				CHI + SQUARE, T = "" + TAU, TAU_MEAN_X2 = TAU + "m " + CHI + SQUARE,
			TAU_MEAN = TAU + "m", A_T_H_Z_X2 = "A " + TAU + " H Z " + CHI + SQUARE,
			A_T_H_X2 = "A " + TAU + " H " + CHI + SQUARE, A_T_H = "A " + TAU + " H",
			T_H_X2 = TAU + " H " + CHI + SQUARE, T_H = TAU + " H", NONE = " ";

	private static final String FITTING_ERROR = "Fitting Error", NO_FIT = "--";

	public static final String SINGLE_FITTED_IMAGE_ITEMS[] = { A_T_Z_X2, A_T_X2,
		A_T, T_X2, T, NONE };

	public static final String DOUBLE_FITTED_IMAGE_ITEMS[] = { A_T_Z_X2, A_T_X2,
		A_T, F_UPPER_T_Z_X2, F_UPPER_T_X2, F_UPPER_T, F_LOWER_T_Z_X2, F_LOWER_T_X2,
		F_LOWER_T, T_X2, T, TAU_MEAN_X2, TAU_MEAN, NONE };

	public static final String TRIPLE_FITTED_IMAGE_ITEMS[] = { A_T_Z_X2, A_T_X2,
		A_T, F_UPPER_T_Z_X2, F_UPPER_T_X2, F_UPPER_T, F_LOWER_T_Z_X2, F_LOWER_T_X2,
		F_LOWER_T, T_X2, T, TAU_MEAN_X2, TAU_MEAN, NONE };

	public static final String STRETCHED_FITTED_IMAGE_ITEMS[] = { A_T_H_Z_X2,
		A_T_H_X2, A_T_H, T_H_X2, T_H, T, NONE };

	private static final String EXCITATION_ITEMS[] = { EXCITATION_NONE,
		LOAD_DEFAULT, SET_AS_DEFAULT, EXCITATION_FILE, EXCITATION_CREATE,
		EXCITATION_ESTIMATE, EXCITATION_GAUSSIAN };

	private static final int X_VALUE = 0, X_MAX = 64000, X_MIN = 0, X_INC = 1;

	private static final int Y_VALUE = 0, Y_MAX = 64000, Y_MIN = 0, Y_INC = 1;

	private static final int THRESH_VALUE = 0, THRESH_MAX = 64000,
			THRESH_MIN = 0, THRESH_INC = 1;

	private static final double CHISQ_VALUE = 1.5, CHISQ_MAX = 5.0,
			CHISQ_MIN = 1.0, CHISQ_INC = 0.25;

	private static final double SCATTER_VALUE = 0.0, SCATTER_MAX = 2.0,
			SCATTER_MIN = 0.0, SCATTER_INC = 0.001;

	private final Preferences userPreferences = Preferences
		.userNodeForPackage(this.getClass());
	private static final String SCATTER = "scatter";

	private final FittingCursorHelper _fittingCursorHelper;
	private final IFitterEstimator _fitterEstimator;
	private final int _maxBin;
	private final double _xInc;

	private IUserInterfacePanelListener _listener;
	private IThresholdUpdate _thresholdListener;
	int _fittedParameterCount = 0;

	// UI panel
	JPanel _COMPONENT;
	JFrame _frame;
	JPanel _cardPanel;

	private boolean flagFunctionComboBox = false;

	public static JComboBox _regionComboBox;
	public static JComboBox _algorithmComboBox;
	public static JComboBox _functionComboBox;
	public static JComboBox _binningComboBox;
	public static JComboBox _fittedImagesComboBox;
	public static JComboBox _meanValueComboBox;
	public static JComboBox _noiseModelComboBox;

	JCheckBox _colorizeGrayScale;
	public static JCheckBox[] _analysisCheckBoxList;
	JCheckBox _fitAllChannels;

	JTextField meanValueTextField;
	// cursor settings
	JTextField _promptBaselineField;
	// JTextField _transientStartField;
	// JTextField _dataStartField;
	// JTextField _transientStopField;
	// JTextField _promptDelayField;
	// JTextField _promptWidthField;
	JCheckBox _showBins;
	public static JComboBox _promptComboBox;
	JButton _estimateCursorsButton;

	// cursor settings
	SpinnerNumberModel _promptBaselineModel;
	JSpinner _promptBaselineSpinner;
	SpinnerNumberModel _transientStartModel;
	JSpinner _transientStartSpinner;
	SpinnerNumberModel _dataStartModel;
	JSpinner _dataStartSpinner;
	SpinnerNumberModel _transientStopModel;
	JSpinner _transientStopSpinner;
	SpinnerNumberModel _promptDelayModel;
	public static JSpinner _promptDelaySpinner;
	SpinnerNumberModel _promptWidthModel;
	public static JSpinner _promptWidthSpinner;

	// fit settings
	JSpinner _xSpinner;
	JSpinner _ySpinner;
	public static JSpinner _thresholdSpinner;
	JSpinner _chiSqTargetSpinner;

	JSpinner _scatterSpinner; // scatter experiment

	// parameter panel
	JPanel _paramPanel;
	int _paramPanelIndex;
	boolean _noFit;

	// single exponential fit
	public static JTextField _aParam1;
	public static JCheckBox _aFix1;
	public static JTextField _tParam1;
	public static JCheckBox _tFix1;
	public static JTextField _zParam1;
	public static JCheckBox _zFix1;
	JTextField _chiSqParam1;
	JTextField _AICParam1;
	JLabel _errorLabel1;
	JCheckBox _startParam1;

	// double exponential fit///////////
	public static JTextField _a1Param2;
	public static JCheckBox _a1Fix2;
	public static JTextField _a2Param2;
	public static JCheckBox _a2Fix2;
	public static JTextField _t1Param2;
	public static JCheckBox _t1Fix2;
	public static JTextField _t2Param2;
	public static JCheckBox _t2Fix2;
	public static JTextField _zParam2;
	public static JCheckBox _zFix2;
	JTextField _chiSqParam2;
	JTextField _AICParam2;
	JLabel _errorLabel2;
	JCheckBox _startParam2;

	// triple exponential fit//////////////
	public static JTextField _a1Param3;
	public static JCheckBox _a1Fix3;
	public static JTextField _a2Param3;
	public static JCheckBox _a2Fix3;
	public static JTextField _a3Param3;
	public static JCheckBox _a3Fix3;
	public static JTextField _t1Param3;
	public static JCheckBox _t1Fix3;
	public static JTextField _t2Param3;
	public static JCheckBox _t2Fix3;
	public static JTextField _t3Param3;
	public static JCheckBox _t3Fix3;
	public static JTextField _zParam3;
	public static JCheckBox _zFix3;
	JTextField _chiSqParam3;
	JTextField _AICParam3;
	JLabel _errorLabel3;
	JCheckBox _startParam3;

	// stretched exponential fit
	public static JTextField _aParam4;
	public static JCheckBox _aFix4;
	public static JTextField _tParam4;
	public static JCheckBox _tFix4;
	public static JTextField _hParam4;
	public static JCheckBox _hFix4;
	public static JTextField _zParam4;
	public static JCheckBox _zFix4;
	JTextField _chiSqParam4;
	JTextField _AICParam4;
	JLabel _errorLabel4;
	JCheckBox _startParam4;

	public static JButton _openButton;
	JButton _quitButton;
	public static JButton _fitButton;
	String _fitButtonText = FIT_IMAGE;

	public static JCheckBox useDetailStat;
	String defaultExcitationPath;
	boolean setAsDefault = false;

	public UserInterfacePanel(final boolean tabbed, final boolean showTau,
		final int maxBin, final double xInc, final String[] analysisChoices,
		final String[] binningChoices,
		final FittingCursorHelper fittingCursorHelper,
		final IFitterEstimator fitterEstimator)
	{
		final String lifetimeLabel = "" + (showTau ? TAU : LAMBDA);

		_fittingCursorHelper = fittingCursorHelper;
		_fitterEstimator = fitterEstimator;
		_maxBin = maxBin;
		_xInc = xInc;

		_frame = new JFrame(TITLE);

		// create outer panel
		final JPanel outerPanel = new JPanel();
		outerPanel.setLayout(new BoxLayout(outerPanel, BoxLayout.Y_AXIS));

		// test
		final String analysisChoiceTemp[] =
			{ analysisChoices[0], analysisChoices[2] };

		if (tabbed) {
			final JTabbedPane tabbedPane = new JTabbedPane();

			final JPanel fitPanel = createFitPanel(analysisChoices);
			tabbedPane.addTab("Fit", fitPanel);

			final JPanel cursorPanel = createCursorPanel();
			tabbedPane.addTab("Cursors", cursorPanel);

			final JPanel controlPanel = createControlPanel(binningChoices);
			tabbedPane.addTab("Control", controlPanel);

			// Create cards and the panel that contains the cards
			_cardPanel = new JPanel(new CardLayout());
			_cardPanel.add(createSingleExponentialPanel(lifetimeLabel),
				SINGLE_EXPONENTIAL);
			_cardPanel.add(createDoubleExponentialPanel(lifetimeLabel),
				DOUBLE_EXPONENTIAL);
			_cardPanel.add(createTripleExponentialPanel(lifetimeLabel),
				TRIPLE_EXPONENTIAL);
			_cardPanel.add(createStretchedExponentialPanel(lifetimeLabel),
				STRETCHED_EXPONENTIAL);
			tabbedPane.addTab("Params", _cardPanel);

			outerPanel.add(tabbedPane);
		}
		else {
			// create inner panel
			final JPanel innerPanel = new JPanel();
			innerPanel.setLayout(new BoxLayout(innerPanel, BoxLayout.X_AXIS));

			final JPanel fitPanel = createFitPanel(analysisChoiceTemp);// done to
																																	// avoid
																																	// Imagej2
																																	// configs
			fitPanel.setBorder(border("Fit"));
			innerPanel.add(fitPanel);

			final JPanel cursorPanel = createCursorPanel();
			cursorPanel.setBorder(border("Cursors"));
			innerPanel.add(cursorPanel);

			final JPanel controlPanel = createControlPanel(binningChoices);
			controlPanel.setBorder(border("Control"));
			innerPanel.add(controlPanel);

			// Create cards and the panel that contains the cards
			_cardPanel = new JPanel(new CardLayout());
			_cardPanel.add(createSingleExponentialPanel(lifetimeLabel),
				SINGLE_EXPONENTIAL);
			_cardPanel.add(createDoubleExponentialPanel(lifetimeLabel),
				DOUBLE_EXPONENTIAL);
			_cardPanel.add(createTripleExponentialPanel(lifetimeLabel),
				TRIPLE_EXPONENTIAL);
			_cardPanel.add(createStretchedExponentialPanel(lifetimeLabel),
				STRETCHED_EXPONENTIAL);
			_cardPanel.setBorder(border("Params"));
			innerPanel.add(_cardPanel);

			outerPanel.add(innerPanel);
		}

		// Lay out the buttons from left to right.
		final JPanel buttonPanel = new JPanel();
		buttonPanel.setLayout(new BoxLayout(buttonPanel, BoxLayout.LINE_AXIS));
		buttonPanel.setBorder(BorderFactory.createEmptyBorder(0, 10, 10, 10));
		buttonPanel.add(Box.createHorizontalGlue());
		_openButton = new JButton("New File/Batch");
		_openButton.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(final ActionEvent e) {
				if (null != _listener) {

					_listener.openFile();
					SLIMProcessor.record(SLIMProcessor.SET_START_BATCH, "");
				}
			}
		});
		buttonPanel.add(_openButton);
		buttonPanel.add(Box.createRigidArea(new Dimension(10, 0)));
		_quitButton = new JButton("Quit");
		_quitButton.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(final ActionEvent e) {
				if (null != _listener) {
					_listener.quit();
				}
			}
		});
		buttonPanel.add(_quitButton);
		buttonPanel.add(Box.createRigidArea(new Dimension(10, 0)));
		_fitButton = new JButton(_fitButtonText);
		_fitButton.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(final ActionEvent e) {

				final String text = e.getActionCommand();
				if (text.equals(_fitButtonText)) {
					enableAll(false);
					setFitButtonState(false);
					if (null != _listener) {
						// /macro recorder for start fitting
						SLIMProcessor.waitRecord(1000);
						SLIMProcessor.record(SLIMProcessor.SET_START_FITTING, "");

						_listener.doFit();

					}
				}
				else {
					setFitButtonState(true);
					if (null != _listener) {
						_listener.cancelFit();
					}
				}
			}
		});
		buttonPanel.add(_fitButton);

		outerPanel.add(buttonPanel);
		_frame.getContentPane().add(outerPanel);

		_frame.pack();
		final Dimension preferred = _frame.getPreferredSize();
		_frame.setMinimumSize(preferred);
		_frame.addComponentListener(new ComponentAdapter() {

			@Override
			public void componentResized(final ComponentEvent e) {
				// allow horizontal but not vertical resize
				int width = _frame.getWidth();
				if (width < (int) preferred.getWidth()) {
					width = (int) preferred.getWidth();
				}
				_frame.setSize(width, (int) preferred.getHeight());
			}

		});

		// no prompt initially
		enablePromptCursors(false);

		// set up and show initial cursors
		_fittingCursorHelper.setFittingCursorUI(this);
	}

	@Override
	public JFrame getFrame() {
		return _frame;
	}

	@Override
	public void setListener(final IUserInterfacePanelListener listener) {
		_listener = listener;
	}

	@Override
	public void setThresholdListener(final IThresholdUpdate thresholdListener) {
		_thresholdListener = thresholdListener;
	}

	@Override
	public void disable() {
		enableAll(false);
	}

	@Override
	public void reset() {
		enableAll(true);
		// sagar
		setFitButtonState(true);
		// /the following macro recording parameter are set to false so that SLIM
		// plugin resumes normal operation when restarted
		SLIMProcessor.macroParams.disableMacro();

	}

	@Override
	public void disableButtons() {
		enableButtons(false);
	}

	@Override
	public void resetButtons() {
		enableButtons(true);
	}

	private JPanel createFitPanel(final String[] analysisChoices) {
		final JPanel fitPanel = new JPanel();
		fitPanel.setBorder(new EmptyBorder(0, 0, 8, 8));
		fitPanel.setLayout(new SpringLayout());

		final JLabel regionLabel = new JLabel("Region");
		regionLabel.setHorizontalAlignment(SwingConstants.RIGHT);
		fitPanel.add(regionLabel);
		// TODO all these comboboxes are problematical:
		// if you click on the containing window & move it the combo list
		// doesn't close and remains active & drawn in original spot.
		// See:
		// http://stackoverflow.com/questions/10982273/swt-awt-new-frame-jcombobox-never-lose-focus-when-window-is-moved
		// (but in that case problem was mixing SWT/AWT/Swing; this is just a
		// JComboBox in a JPanel in a JTabbedPane.)
		_regionComboBox = new JComboBox(REGION_ITEMS);
		_regionComboBox.setSelectedItem(ALL_REGION);
		_regionComboBox.addItemListener(new ItemListener() {

			@Override
			public void itemStateChanged(final ItemEvent e) {
				if (e.getStateChange() == ItemEvent.SELECTED) {
					final String item = (String) e.getItem();
					if (SUM_REGION.equals(item)) {
						_fitButtonText = FIT_SUMMED_PIXELS;
					}
					else if (ROIS_REGION.equals(item)) {
						_fitButtonText = FIT_SUMMED_ROIS;
					}
					else if (PIXEL_REGION.equals(item)) {
						_fitButtonText = FIT_PIXEL;
					}
					else if (ALL_REGION.equals(item)) {
						_fitButtonText = FIT_IMAGE;
					}
					_fitButton.setText(_fitButtonText);

					SLIMProcessor.record(SLIMProcessor.SET_REGION_TYPE, item);

				}
			}
		});
		fitPanel.add(_regionComboBox);

		final JLabel algorithmLabel = new JLabel("Algorithm");
		algorithmLabel.setHorizontalAlignment(SwingConstants.RIGHT);
		fitPanel.add(algorithmLabel);
		_algorithmComboBox = new JComboBox(ALGORITHM_ITEMS);
		_algorithmComboBox.setSelectedItem(SLIM_CURVE_RLD_LMA_ALGORITHM);
		refitUponStateChangeAlgorithm(_algorithmComboBox);// sagar
		fitPanel.add(_algorithmComboBox);

		final JLabel functionLabel = new JLabel("Function");
		functionLabel.setHorizontalAlignment(SwingConstants.RIGHT);
		fitPanel.add(functionLabel);
		_functionComboBox = new JComboBox(FUNCTION_ITEMS);
		_functionComboBox.addItemListener(new ItemListener() {

			@Override
			public void itemStateChanged(final ItemEvent e) {
				if (e.getStateChange() == ItemEvent.SELECTED) {
					flagFunctionComboBox = true;
					final String item = (String) e.getItem();
					// SLIMProcessor.macroParams.algotype=1;
					final CardLayout cl = (CardLayout) (_cardPanel.getLayout());
					cl.show(_cardPanel, item);
					reconcileStartParam();
					updateFittedImagesComboBox(FUNCTION_ITEMS, item);
					updateMeanValueComboBoxComboBox(FUNCTION_ITEMS, item);
					
//							SLIMProcessor.macroParams.fucntion_type=1;
//							IJ.log("item llistener fro function has been reached");
					// ADDMACRO
					// add macro to record the setFunctionType function
					SLIMProcessor.macroParams.setFunction(item);
					SLIMProcessor.record(SLIMProcessor.SET_FUNCTION_TYPE, item);
					flagFunctionComboBox = false;

				}
			}
		});

		refitUponStateChange(_functionComboBox);
		fitPanel.add(_functionComboBox);

		final JLabel noiseModelLabel = new JLabel("Noise Model");
		noiseModelLabel.setHorizontalAlignment(SwingConstants.RIGHT);
		fitPanel.add(noiseModelLabel);
		_noiseModelComboBox = new JComboBox(NOISE_MODEL_ITEMS);
		_noiseModelComboBox.setSelectedItem(MAXIMUM_LIKELIHOOD);
		_noiseModelComboBox.addItemListener(

		new ItemListener() {

			@Override
			public void itemStateChanged(final ItemEvent e) {
				if (e.getStateChange() == ItemEvent.SELECTED) {
					final String item = (String) e.getItem();
					SLIMProcessor.record(SLIMProcessor.SET_NOISE_MODEL, item);

				}
			}
		}

		);

		refitUponStateChange(_noiseModelComboBox);
		fitPanel.add(_noiseModelComboBox);

		final JLabel fittedImagesLabel = new JLabel("Fitted Images");
		fittedImagesLabel.setHorizontalAlignment(SwingConstants.RIGHT);
		fitPanel.add(fittedImagesLabel);

		_fittedImagesComboBox = new JComboBox(SINGLE_FITTED_IMAGE_ITEMS);

		_fittedImagesComboBox.addItemListener(

		new ItemListener() {

			@Override
			public void itemStateChanged(final ItemEvent e) {
				if (e.getStateChange() == ItemEvent.SELECTED && !flagFunctionComboBox) {

					final String item = (String) e.getItem();
					// IJ.log(item);
					final String selectedItemFittedComboBox =
						Integer.toString(_fittedImagesComboBox.getSelectedIndex());
					// IJ.log(selectedItemFittedComboBox);
					SLIMProcessor.record(SLIMProcessor.SET_FITTED_IMAGES,
						selectedItemFittedComboBox);

				}
			}
		}

		);

		fitPanel.add(_fittedImagesComboBox);
		// refitUponStateChangeFittedImage(_fittedImagesComboBox);//sagar

		final JLabel dummyLabel = new JLabel("");
		dummyLabel.setHorizontalAlignment(SwingConstants.RIGHT);
		fitPanel.add(dummyLabel);
		_colorizeGrayScale = new JCheckBox("Colorize grayscale");
		// _colorizeGrayScale.addItemListener

//		_colorizeGrayScale.addItemListener(new ItemListener() {
//		      public void itemStateChanged(ItemEvent e) {
//		        IJ.log("Checked? " + _colorizeGrayScale.isSelected());
//		      }
//		    });

		fitPanel.add(_colorizeGrayScale);

		// IJ.log("lenght"+Integer.toString(analysisChoices.length));
		final int choices = analysisChoices.length;
		if (choices > 0) {
			final List<JCheckBox> checkBoxList = new ArrayList<JCheckBox>();
			boolean firstChoice = true;
			for (final String analysisChoice : analysisChoices) {
				final String labelString = firstChoice ? "Analysis" : "";
				firstChoice = false;
				final JLabel choiceLabel = new JLabel(labelString);
				choiceLabel.setHorizontalAlignment(SwingConstants.RIGHT);
				fitPanel.add(choiceLabel);
				final JCheckBox checkBox = new JCheckBox(analysisChoice);
				fitPanel.add(checkBox);
				checkBoxList.add(checkBox);
			}
			_analysisCheckBoxList = checkBoxList.toArray(new JCheckBox[0]);
		}
		else {
			_analysisCheckBoxList = new JCheckBox[0];
		}

		SpringUtilities.makeCompactGrid(fitPanel, 6 + choices, 2, 4, 4, 4, 4);

		final JPanel panel = new JPanel(new BorderLayout());
		panel.add("North", fitPanel);

		_fitAllChannels = new JCheckBox("Fit all channels");
		_fitAllChannels.setSelected(true);

		panel.add("South", _fitAllChannels);
		return panel;
	}

	/**
	 * Used to build an appropriate list of fitted images, according to the fit
	 * function selected.
	 *
	 */
	private void updateFittedImagesComboBox(final String[] items,
		final String selectedItem)
	{
		if (SINGLE_EXPONENTIAL.equals(selectedItem)) {
			updateComboBox(_fittedImagesComboBox, SINGLE_FITTED_IMAGE_ITEMS);
		}
		else if (DOUBLE_EXPONENTIAL.equals(selectedItem)) {
			updateComboBox(_fittedImagesComboBox, DOUBLE_FITTED_IMAGE_ITEMS);
		}
		else if (TRIPLE_EXPONENTIAL.equals(selectedItem)) {
			updateComboBox(_fittedImagesComboBox, TRIPLE_FITTED_IMAGE_ITEMS);
		}
		else if (STRETCHED_EXPONENTIAL.equals(selectedItem)) {
			updateComboBox(_fittedImagesComboBox, STRETCHED_FITTED_IMAGE_ITEMS);
		}
	}
	
	private void updateMeanValueComboBoxComboBox(final String[] items,
			final String selectedItem)
	{
		IJ.log("in box of mean val change"+selectedItem+meanDoubleStrings.length);

		if (SINGLE_EXPONENTIAL.equals(selectedItem)) {
			updateComboBox(_meanValueComboBox, meanSingleStrings);
		}
		else if (DOUBLE_EXPONENTIAL.equals(selectedItem)) {
			IJ.log("in double");
			updateComboBox(_meanValueComboBox, meanDoubleStrings);
		}
		else if (TRIPLE_EXPONENTIAL.equals(selectedItem)) {
			updateComboBox(_meanValueComboBox, meanTripleStrings);
		}
		else if (STRETCHED_EXPONENTIAL.equals(selectedItem)) {
			updateComboBox(_meanValueComboBox, strechedExpo);
		}
	}

	/*
	 * Updates a combo box with a new list of items.
	 */
	private void updateComboBox(final JComboBox comboBox, final String[] items) {
		// Had problems with "comboBox.removeAll()":
		for (int i = comboBox.getItemCount() - 1; i >= 0; --i) {
			comboBox.removeItemAt(i);
		}
		for (final String item : items) {
			comboBox.addItem(item);
		}
		comboBox.setSelectedIndex(0);
	}

	private JPanel createCursorPanel() {
		final JPanel cursorPanel = new JPanel();
		cursorPanel.setBorder(new EmptyBorder(0, 0, 8, 8));
		cursorPanel.setLayout(new SpringLayout());

		// emulating TRI2 cursor listing order here
		final JLabel excitationBaselineLabel = new JLabel("Excitation Baseline");
		excitationBaselineLabel.setHorizontalAlignment(SwingConstants.RIGHT);
		cursorPanel.add(excitationBaselineLabel);
		_promptBaselineModel = new SpinnerNumberModel(0.0, 0.0, 1000.0, 0.001);
		_promptBaselineSpinner = new JSpinner(_promptBaselineModel);
		_promptBaselineSpinner.addChangeListener(new ChangeListener() {

			@Override
			public void stateChanged(final ChangeEvent e) {
				_fittingCursorHelper.setPromptBaseline(getPromptBaseline());

				if (!SLIMProcessor.macroParams.isExcitationChanging) {
					SLIMProcessor.macroParams.isMacroUsedForExcitation = false;
					// SLIMProcessor.macroParams.setPromptBaseLine(Double.parseDouble(getPromptBaseline()));
					// TODO add check for excitaiton file being loaded
					SLIMProcessor.record(SLIMProcessor.SET_PROMPT_BASELINE,
						_promptBaselineSpinner.getValue().toString());
				}

			}

		});
		cursorPanel.add(_promptBaselineSpinner);

		final JLabel transStartLabel = new JLabel("Transient Start");
		transStartLabel.setHorizontalAlignment(SwingConstants.RIGHT);
		cursorPanel.add(transStartLabel);
		_transientStartModel =
			new SpinnerNumberModel(0.0, 0.0, _maxBin * _xInc, _xInc);
		_transientStartSpinner = new JSpinner(_transientStartModel);

		// _fittingCursorHelper.setTransientStart(getTransientStart());
		_transientStartSpinner.addChangeListener(new ChangeListener() {

			@Override
			public void stateChanged(final ChangeEvent e) {
				// IJ.log(getTransientStart());
				// ADD recorder here

				if (!SLIMProcessor.macroParams.isExcitationChanging) {
					SLIMProcessor.macroParams.transientStartMacroUsed = false;
					_fittingCursorHelper.setTransientStart(getTransientStart());

					if (!SLIMProcessor.macroParams.firstTimeRecordTransientStart) {
						SLIMProcessor.record(SLIMProcessor.SET_TRANSIENT_START,
							getTransientStart());

					}
					SLIMProcessor.macroParams.firstTimeRecordTransientStart = false;
				}

			}
		});
		cursorPanel.add(_transientStartSpinner);

		final JLabel dataStartLabel = new JLabel("Data Start");
		dataStartLabel.setHorizontalAlignment(SwingConstants.RIGHT);
		cursorPanel.add(dataStartLabel);
		_dataStartModel = new SpinnerNumberModel(0.0, 0.0, _maxBin * _xInc, _xInc);
		_dataStartSpinner = new JSpinner(_dataStartModel);
		_dataStartSpinner.addChangeListener(new ChangeListener() {

			@Override
			public void stateChanged(final ChangeEvent e) {

				_fittingCursorHelper.setDataStart(getDataStart());

				if (!SLIMProcessor.macroParams.isExcitationChanging) {// /added for
																															// avoiding auto
																															// recording
																															// during
																															// excitation
																															// change
					SLIMProcessor.macroParams.DataStartMacroUsed = false;

					if (!SLIMProcessor.macroParams.firstTimeRecordDataStart) {
						SLIMProcessor.record(SLIMProcessor.SET_DATA_START, getDataStart());

					}
					SLIMProcessor.macroParams.firstTimeRecordDataStart = false;
				}

			}
		});
		cursorPanel.add(_dataStartSpinner);

		final JLabel transStopLabel = new JLabel("Transient End");
		transStopLabel.setHorizontalAlignment(SwingConstants.RIGHT);
		cursorPanel.add(transStopLabel);
		_transientStopModel =
			new SpinnerNumberModel(0.0, 0.0, _maxBin * _xInc, _xInc);
		_transientStopSpinner = new JSpinner(_transientStopModel);
		_transientStopSpinner.addChangeListener(new ChangeListener() {

			@Override
			public void stateChanged(final ChangeEvent e) {

				_fittingCursorHelper.setTransientStop(getTransientStop());
				SLIMProcessor.macroParams.transientStopMacroUsed = false;

				if (!SLIMProcessor.macroParams.firstTimeRecordTransientStop) {
					SLIMProcessor.record(SLIMProcessor.SET_TRANSIENT_STOP,
						getTransientStop());
				}
				SLIMProcessor.macroParams.firstTimeRecordTransientStop = false;
			}
		});
		cursorPanel.add(_transientStopSpinner);

		final JLabel excitationStartLabel = new JLabel("Excitation Delay");
		excitationStartLabel.setHorizontalAlignment(SwingConstants.RIGHT);
		cursorPanel.add(excitationStartLabel);
		_promptDelayModel =
			new SpinnerNumberModel(0.0, 0.0, _maxBin * _xInc, _xInc);
		_promptDelaySpinner = new JSpinner(_promptDelayModel);
		_promptDelaySpinner.addChangeListener(new ChangeListener() {

			@Override
			public void stateChanged(final ChangeEvent e) {
				_fittingCursorHelper.setPromptDelay(getPromptDelay());
				// sagar//record the delay time
				SLIMProcessor.record(SLIMProcessor.SET_DELAY_PROMPT, getPromptDelay());
				SLIMProcessor.macroParams.isDelayExcitationMacroused = false;

			}
		});
		cursorPanel.add(_promptDelaySpinner);

		final JLabel excitationStopLabel = new JLabel("Excitation Width");
		excitationStopLabel.setHorizontalAlignment(SwingConstants.RIGHT);
		cursorPanel.add(excitationStopLabel);
		_promptWidthModel =
			new SpinnerNumberModel(0.0, 0.0, _maxBin * _xInc, _xInc);
		_promptWidthSpinner = new JSpinner(_promptWidthModel);
		_promptWidthSpinner.addChangeListener(new ChangeListener() {

			@Override
			public void stateChanged(final ChangeEvent e) {
				_fittingCursorHelper.setPromptWidth(getPromptWidth());
				SLIMProcessor.record(SLIMProcessor.SET_WIDTH_PROMPT, getPromptWidth());

			}
		});
		cursorPanel.add(_promptWidthSpinner);

		/*JLabel excitationBaselineLabel = new JLabel("Excitation Baseline");
		excitationBaselineLabel.setHorizontalAlignment(SwingConstants.RIGHT);
		cursorPanel.add(excitationBaselineLabel);
		_promptBaselineField = new JTextField(9);
		_promptBaselineField.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				_fittingCursorHelper.setPromptBaseline(_promptBaselineField.getText());
			}
		});
		_promptBaselineField.addFocusListener(new FocusAdapter() {
			public void focusLost(FocusEvent e) {
				_fittingCursorHelper.setPromptBaseline(_promptBaselineField.getText());
			}
		});

		cursorPanel.add(_promptBaselineField);
		JLabel transStartLabel = new JLabel("Transient Start");
		transStartLabel.setHorizontalAlignment(SwingConstants.RIGHT);
		cursorPanel.add(transStartLabel);
		_transientStartField = new JTextField(9);
		_transientStartField.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				_fittingCursorHelper.setTransientStart(_transientStartField.getText());
			}
		});
		_transientStartField.addFocusListener(new FocusAdapter() {
			public void focusLost(FocusEvent e) {
				_fittingCursorHelper.setTransientStart(_transientStartField.getText());
			}
		});
		cursorPanel.add(_transientStartField);

		JLabel dataStartLabel = new JLabel("Data Start");
		dataStartLabel.setHorizontalAlignment(SwingConstants.RIGHT);
		cursorPanel.add(dataStartLabel);
		_dataStartField = new JTextField(9);
		_dataStartField.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				_fittingCursorHelper.setDataStart(_dataStartField.getText());
			}
		});
		_dataStartField.addFocusListener(new FocusAdapter() {
			public void focusLost(FocusEvent e) {
				_fittingCursorHelper.setDataStart(_dataStartField.getText());
			}
		});
		cursorPanel.add(_dataStartField);

		JLabel transStopLabel = new JLabel("Transient End");
		transStopLabel.setHorizontalAlignment(SwingConstants.RIGHT);
		cursorPanel.add(transStopLabel);
		_transientStopField = new JTextField(9);
		_transientStopField.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				_fittingCursorHelper.setTransientStop(_transientStopField.getText());
			}
		});
		_transientStopField.addFocusListener(new FocusAdapter() {
			public void focusLost(FocusEvent e) {
				_fittingCursorHelper.setTransientStop(_transientStopField.getText());
			}
		});
		cursorPanel.add(_transientStopField);

		JLabel excitationStartLabel = new JLabel("Excitation Delay");
		excitationStartLabel.setHorizontalAlignment(SwingConstants.RIGHT);
		cursorPanel.add(excitationStartLabel);
		_promptDelayField = new JTextField(9);
		_promptDelayField.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				_fittingCursorHelper.setPromptDelay(_promptDelayField.getText());
			}
		});
		_promptDelayField.addFocusListener(new FocusAdapter() {
			public void focusLost(FocusEvent e) {
				_fittingCursorHelper.setPromptDelay(_promptDelayField.getText());
			}
		});
		cursorPanel.add(_promptDelayField);

		JLabel excitationStopLabel = new JLabel("Excitation Width");
		excitationStopLabel.setHorizontalAlignment(SwingConstants.RIGHT);
		cursorPanel.add(excitationStopLabel);
		_promptWidthField = new JTextField(9);
		_promptWidthField.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				_fittingCursorHelper.setPromptWidth(_promptWidthField.getText());
			}
		});
		_promptWidthField.addFocusListener(new FocusAdapter() {
			public void focusLost(FocusEvent e) {
				_fittingCursorHelper.setPromptWidth(_promptWidthField.getText());
			}
		});
		cursorPanel.add(_promptWidthField);*/

		final JLabel dummyLabel = new JLabel("");
		dummyLabel.setHorizontalAlignment(SwingConstants.RIGHT);
		cursorPanel.add(dummyLabel);
		_showBins = new JCheckBox("Display as indices");
		_showBins.addItemListener(new ItemListener() {

			@Override
			public void itemStateChanged(final ItemEvent e) {
				final boolean showBins = e.getStateChange() == ItemEvent.SELECTED;
				// TODO 4/2/13
				/**
				 * I tried having two models, one for bins and one for time values and
				 * swapping them here. Crashes when one model has integer values and the
				 * other doubles. So, switch to doubles for all, but still doesn't work.
				 * Same thing having a single model that gets different min/max/inc. In
				 * 'bins' mode I could never get the spinner to work, or even type in
				 * new values. Therefore, for now, just disable these spinners
				 * altogether when in 'bins' mode.
				 */
				_transientStartSpinner.setEnabled(!showBins);
				_dataStartSpinner.setEnabled(!showBins);
				_transientStopSpinner.setEnabled(!showBins);
				_promptDelaySpinner.setEnabled(!showBins);
				_promptWidthSpinner.setEnabled(!showBins);
				_fittingCursorHelper.setShowBins(showBins);
			}
		});
		// TODO 4/2/13
		// Swapping models doesn't work correctly.
		// Failing that, I could either remove the JSpinner from cursor fields or
		// else
		// just disable the model swap.
		// Let's go with the latter; we lose the ability to show the underlying bins
		// unfortunately.
		cursorPanel.add(_showBins);

		final JLabel excitationLabel = new JLabel("Excitation");
		excitationLabel.setHorizontalAlignment(SwingConstants.RIGHT);
		cursorPanel.add(excitationLabel);

		_promptComboBox = new JComboBox(EXCITATION_ITEMS);
		_promptComboBox.setSelectedIndex(0);

		_promptComboBox.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(final ActionEvent e) {

				SLIMProcessor.macroParams.isExcitationChanging = true;
				final String selectedItem = (String) _promptComboBox.getSelectedItem();
				boolean isExcitationLoaded = false;
				if (EXCITATION_FILE.equals(selectedItem)) {
					// sagar
					if (!SLIMProcessor.macroParams.isMacroUsedForExcitation) {
						final OpenDialog dialog =
							new OpenDialog("Load Excitation File", "");
						final String directory = dialog.getDirectory();
						final String fileName = dialog.getFileName();

						if (null != fileName && !fileName.equals("") && null != _listener) {
							isExcitationLoaded =
								_listener.loadExcitation(directory + fileName);
						}

						// //add recorder for using that specific IRF file
						SLIMProcessor
							.record(SLIMProcessor.SET_EXCITATION, dialog.getPath());
						SLIMProcessor.waitRecord(500);

					}

					else {
						isExcitationLoaded =
							_listener
								.loadExcitation(SLIMProcessor.macroParams.excitationFileName);
					}

				}
				else if (EXCITATION_CREATE.equals(selectedItem)) {
					final SaveDialog dialog =
						new SaveDialog("Save Excitation File", "", "");
					final String directory = dialog.getDirectory();
					final String fileName = dialog.getFileName();

					defaultExcitationPath = directory + fileName;

					if (null != fileName && !fileName.equals("") && null != _listener) {
						isExcitationLoaded =
							_listener.createExcitation(directory + fileName);

						// /here add the code to write the working deafultIRF's path in the
						// workingDirectory path

					}
				}
				else if (EXCITATION_ESTIMATE.equals(selectedItem)) {
					final SaveDialog dialog =
						new SaveDialog("Save Excitation File", "", "");
					final String directory = dialog.getDirectory();
					final String fileName = dialog.getFileName();
					if (null != fileName && !fileName.equals("") && null != _listener) {
						isExcitationLoaded =
							_listener.estimateExcitation(directory + fileName);
					}
				}
				else if (EXCITATION_GAUSSIAN.equals(selectedItem)) {
					final SaveDialog dialog =
						new SaveDialog("Save Excitation File", "", "");
					final String directory = dialog.getDirectory();
					final String fileName = dialog.getFileName();
					if (null != fileName && !fileName.equals("") && null != _listener) {
						isExcitationLoaded =
							_listener.gaussianExcitation(directory + fileName);
					}
				}

				else if (LOAD_DEFAULT.equals(selectedItem)) {
					// should load everything with dealy, width, baseline
					if (!SLIMProcessor.macroParams.isMacroUsedForDefaultExcitation) {

						SLIMProcessor.record(SLIMProcessor.DEFAULT_EXCITATION_LOAD, "true");
						SLIMProcessor.waitRecord(1000);
					}
					String defaultIRFPath = null;
					// //read file name for default location

					defaultIRFPath = Prefs.get(KEY_EXCITATION, null);

					if (defaultIRFPath != null) {
						isExcitationLoaded = _listener.loadExcitation(defaultIRFPath);
					}
					else {
						IJ.log("nothing found as default excitation. Please try to save default excitation again");
					}

					final String str1 = Prefs.get(KEY_BASE_PROMPT, null);
					final String str2 = Prefs.get(KEY_WIDTH, null);
					final String str3 = Prefs.get(KEY_DELAY_TIME, null);

					// /check for exception when field does not have 3 values should go
					// here//

					if (str1 != null) {
						// /set the _promptBaselineSpinner
						_promptBaselineSpinner.setValue(Double.parseDouble(str1));
					}
					if (str2 != null) {
						// set the _promptWidthSpinner
						_promptWidthSpinner.setValue(Double.parseDouble(str2));
					}
					if (str3 != null) {
						// set the _promptDelaySpinner
						_promptDelaySpinner.setValue(Double.parseDouble(str3));
					}
					// }

				}

				else if (SET_AS_DEFAULT.equals(selectedItem)) {
					// set the default configuration with transient start, end and width
					String workingDirectory = System.getProperty("user.dir");
					workingDirectory += "\\plugins\\Analyze\\";

					// code for writing the fiel name and excitaiton
					// isExcitationLoaded = _listener.createExcitation(workingDirectory +
					// "DefaultExcitation.irf");

					if (_promptDelaySpinner.isEnabled()) {
						final String baseDefaultExcitation =
							_promptBaselineSpinner.getValue().toString();
						final String widthDefaultExcitation =
							_promptWidthSpinner.getValue().toString();
						final String delayDefaultExcitation =
							_promptDelaySpinner.getValue().toString();

						// /write the transient times
						// /these should be static values

						Prefs.set(KEY_EXCITATION, defaultExcitationPath);
						Prefs.set(KEY_BASE_PROMPT, baseDefaultExcitation);
						Prefs.set(KEY_WIDTH, widthDefaultExcitation);
						Prefs.set(KEY_DELAY_TIME, delayDefaultExcitation);

						// Prefs.set(key, text);
//								if(defaultExcitationPath!=null){
//									try {
//
//
//										BufferedWriter writer=new BufferedWriter(new FileWriter(workingDirectory+"configDefaultExcitation.txt"));
//										BufferedWriter writerTime=new BufferedWriter(new FileWriter(workingDirectory+"configExcitationTime.txt"));
//										writer.write(defaultExcitationPath);
//
//										///writing the timing values for the default excitation
//
//										writerTime.write(baseDefaultExcitation);
//										writerTime.newLine();
//										writerTime.write(widthDefaultExcitation);
//										writerTime.newLine();
//										writerTime.write(delayDefaultExcitation);
//
//										writer.close();
//										writerTime.close();
//
//									} catch (IOException e1) {
//										// TODO Auto-generated catch block
//										e1.printStackTrace();
//									}
//								}
					}
					else {
						IJ.log("nothing to save as default");
					}

				}

				if (isExcitationLoaded) {
					_promptComboBox.setSelectedItem(EXCITATION_FILE);
					enablePromptCursors(true);
				}
				else {
					_promptComboBox.setSelectedItem(EXCITATION_NONE);
					_promptDelaySpinner.setValue(0);
					_promptWidthSpinner.setValue(0);
					_promptBaselineSpinner.setValue(0);

					/* String text = _fittingCursorHelper.getShowBins() ? "0" : "0.0";
					_promptDelayField.setText(text);
					_promptWidthField.setText(text);
					_promptBaselineField.setText("0.0"); */
					enablePromptCursors(false);
					if (null != _listener) {
						_listener.cancelExcitation();
					}
				}
				_listener.reFit();

				SLIMProcessor.macroParams.isExcitationChanging = false;
			}
		});
		// ///deafult excitation load
		// C:\Users\Sagar\Desktop\wqe.irf
//		_listener.createExcitation("C:\\Users\\Sagar\\Desktop\\wqe.irf");
//		_promptComboBox.setSelectedItem(EXCITATION_FILE);
		// enablePromptCursors(true);
		// _promptComboBox.setSelectedItem(LOAD_DEFAULT);
		// /default excitation load ends

		cursorPanel.add(_promptComboBox);

		final JLabel dummyLabel2 = new JLabel("");
		dummyLabel2.setHorizontalAlignment(SwingConstants.RIGHT);
		cursorPanel.add(dummyLabel2);
		_estimateCursorsButton = new JButton("Estimate Cursors");
		_estimateCursorsButton.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(final ActionEvent e) {
				if (null != _listener) {
					_listener.estimateCursors();
				}
			}
		});
		cursorPanel.add(_estimateCursorsButton);

		// rows, cols, initX, initY, xPad, yPad
		SpringUtilities.makeCompactGrid(cursorPanel, 9, 2, 4, 4, 4, 4);

		final JPanel panel = new JPanel(new BorderLayout());
		panel.add("North", cursorPanel);

		return panel;
	}

	/*
	 * Creates a panel that has some settings that control the fit.
	 */
	private JPanel createControlPanel(final String[] binningChoices) {
		final JPanel controlPanel = new JPanel();
		controlPanel.setBorder(new EmptyBorder(0, 0, 8, 8));
		controlPanel.setLayout(new SpringLayout());

		final JLabel xLabel = new JLabel("X");
		xLabel.setHorizontalAlignment(SwingConstants.RIGHT);
		controlPanel.add(xLabel);
		_xSpinner =
			new JSpinner(new SpinnerNumberModel(X_VALUE, X_MIN, X_MAX, X_INC));
		refitUponStateChange(_xSpinner);
		controlPanel.add(_xSpinner);

		final JLabel yLabel = new JLabel("Y");
		yLabel.setHorizontalAlignment(SwingConstants.RIGHT);
		controlPanel.add(yLabel);
		_ySpinner =
			new JSpinner(new SpinnerNumberModel(Y_VALUE, Y_MIN, Y_MAX, Y_INC));
		refitUponStateChange(_ySpinner);
		controlPanel.add(_ySpinner);

		final JLabel thresholdLabel = new JLabel("Threshold");
		thresholdLabel.setHorizontalAlignment(SwingConstants.RIGHT);
		controlPanel.add(thresholdLabel);
		_thresholdSpinner =
			new JSpinner(new SpinnerNumberModel(THRESH_VALUE, THRESH_MIN, THRESH_MAX,
				THRESH_INC));
		updateThresholdChange(_thresholdSpinner);
		controlPanel.add(_thresholdSpinner);

		final JLabel chiSqTargetLabel = new JLabel(CHI_SQ_TARGET);
		chiSqTargetLabel.setHorizontalAlignment(SwingConstants.RIGHT);
		controlPanel.add(chiSqTargetLabel);
		_chiSqTargetSpinner =
			new JSpinner(new SpinnerNumberModel(CHISQ_VALUE, CHISQ_MIN, CHISQ_MAX,
				CHISQ_INC));
		// refitUponStateChange(_chiSqTargetSpinner);sagar

		_chiSqTargetSpinner.addChangeListener(new ChangeListener() {

			@Override
			public void stateChanged(final ChangeEvent e) {
				// sagar added
				paramSetMacro.chi2MacroUsed = false;
				final String chi2Value = _chiSqTargetSpinner.getValue().toString();
				SLIMProcessor.record(SLIMProcessor.SET_CHI2_TARGET, chi2Value);
				if (null != _listener) {
					_listener.reFit();

				}
			}
		});

		controlPanel.add(_chiSqTargetSpinner);

		final JLabel binningLabel = new JLabel("Bin");
		binningLabel.setHorizontalAlignment(SwingConstants.RIGHT);
		controlPanel.add(binningLabel);
		_binningComboBox = new JComboBox(binningChoices);
		refitUponStateChangeBinning(_binningComboBox);// sagar
		controlPanel.add(_binningComboBox);
		final JLabel meanCheckBoxLabel = new JLabel("");
		//meanCheckBoxLabel.setHorizontalAlignment(SwingConstants.RIGHT);
		controlPanel.add(meanCheckBoxLabel);
		useDetailStat=new JCheckBox("Display mean");
		controlPanel.add(useDetailStat);
		
		final JLabel meanValueLabel = new JLabel("Mean Value");//mean vallue options
		meanValueLabel.setHorizontalAlignment(SwingConstants.RIGHT);
		controlPanel.add(meanValueLabel);
		_meanValueComboBox = new JComboBox(meanSingleStrings);
		//refitUponStateChangeMeanValue(_meanValueComboBox);// sagar
		_meanValueComboBox.addItemListener(new ItemListener() {

			@Override
			public void itemStateChanged(final ItemEvent e) {
				// /item added for macro recording
				// sagar

				if (e.getStateChange() == ItemEvent.SELECTED && null != _listener) {
					final String item = (String) e.getItem();
					int num=_meanValueComboBox.getSelectedIndex();
					//get the mean values
					///WARNINNG-> add something so that is not accessed before fit is done
					//get some flag that enables after fitting is done
					if(SLIMProcessor.macroParams.fittingDone){
						meanValueTextField.setText(SLIMProcessor.macroParams.meanStatValues[num]);
					}
					//_listener.reFit();
				}

			}
		});
		controlPanel.add(_meanValueComboBox);
		_meanValueComboBox.setEnabled(false);
		useDetailStat.addItemListener(new ItemListener() {

			@Override
			public void itemStateChanged(final ItemEvent e) {
				SLIMProcessor.macroParams.useDetailStat=!useDetailStat.isSelected();
				_meanValueComboBox.setEnabled(useDetailStat.isSelected());
				if(useDetailStat.isSelected()){
					_fittedImagesComboBox.setSelectedIndex(0);
				}
			}
		});
		
		final JLabel MeanValLabel = new JLabel("");
		MeanValLabel.setHorizontalAlignment(SwingConstants.RIGHT);
		controlPanel.add(MeanValLabel);
		meanValueTextField = new JTextField(9);
		meanValueTextField.setEditable(false);
		controlPanel.add(meanValueTextField);
		
		int rows = 8;

		final boolean showScatter = userPreferences.getBoolean(SCATTER, false);
		userPreferences.putBoolean(SCATTER, showScatter);
		if (showScatter) {
			++rows;

			final JLabel scatterLabel = new JLabel("Scatter"); // SCATTER
			scatterLabel.setHorizontalAlignment(SwingConstants.RIGHT);
			controlPanel.add(scatterLabel);
			_scatterSpinner = new JSpinner();
			// see
			// http://implementsblog.com/2012/11/26/java-gotcha-jspinner-preferred-size/
			final Dimension size = _scatterSpinner.getPreferredSize();
			final SpinnerNumberModel model =
				new SpinnerNumberModel(SCATTER_VALUE, SCATTER_MIN, SCATTER_MAX,
					SCATTER_INC);
			_scatterSpinner.setModel(model);
			_scatterSpinner.setPreferredSize(size);
			refitUponStateChange(_scatterSpinner);
			controlPanel.add(_scatterSpinner);
		}

		// rows, cols, initX, initY, xPad, yPad
		SpringUtilities.makeCompactGrid(controlPanel, rows, 2, 4, 4, 4, 4);

		final JPanel panel = new JPanel(new BorderLayout());
		panel.add("North", controlPanel);

		return panel;
	}

	// TODO the fitted parameter UI could have a JSpinner that only shows up when
	// you fix a parameter.
	// See: https://forums.oracle.com/forums/thread.jspa?threadID=1357061
	// It's quite hacky though.

	/*
	 * Creates panel for the single exponential version of the fit parameters.
	 */
	private JPanel createSingleExponentialPanel(final String lifetimeLabel) {
		final JPanel expPanel = new JPanel();
		expPanel.setBorder(new EmptyBorder(0, 0, 8, 8));
		expPanel.setLayout(new SpringLayout());

		final JLabel aLabel1 = new JLabel("A");
		aLabel1.setHorizontalAlignment(SwingConstants.RIGHT);
		expPanel.add(aLabel1);
		_aParam1 = new JTextField(9);
		// _a1Param1.setEditable(false);
		expPanel.add(_aParam1);
		_aFix1 = new JCheckBox("Fix");
		// _a1Fix1.addItemListener(this);
		expPanel.add(_aFix1);
		refitUponStateChangeA1(_aParam1, _aFix1);

		// sagar

		final JLabel t1Label1 = new JLabel(lifetimeLabel);
		t1Label1.setHorizontalAlignment(SwingConstants.RIGHT);
		expPanel.add(t1Label1);
		_tParam1 = new JTextField(9);
		// _t1Param1.setEditable(false);
		expPanel.add(_tParam1);
		_tFix1 = new JCheckBox("Fix");
		// _t1Fix1.addItemListener(this);
		expPanel.add(_tFix1);
		refitUponStateChangeT1(_tParam1, _tFix1);
		// refitUponStateChange(_aParam1, _aFix1);

		final JLabel zLabel1 = new JLabel("Z");
		zLabel1.setHorizontalAlignment(SwingConstants.RIGHT);
		expPanel.add(zLabel1);
		_zParam1 = new JTextField(9);
		// _zParam1.setEditable(false);
		expPanel.add(_zParam1);
		_zFix1 = new JCheckBox("Fix");
		// _zFix1.addItemListener(this);
		expPanel.add(_zFix1);
		refitUponStateChangeZ1(_zParam1, _zFix1);

		final JLabel chiSqLabel1 = new JLabel("" + CHI + SQUARE + SUB_R);
		chiSqLabel1.setHorizontalAlignment(SwingConstants.RIGHT);
		expPanel.add(chiSqLabel1);
		_chiSqParam1 = new JTextField(9);
		_chiSqParam1.setEditable(false);
		expPanel.add(_chiSqParam1);
		final JLabel nullLabel1 = new JLabel("");
		expPanel.add(nullLabel1);

		final JLabel AICLabel1 = new JLabel("AIC");
		AICLabel1.setHorizontalAlignment(SwingConstants.RIGHT);
		expPanel.add(AICLabel1);
		_AICParam1 = new JTextField(9);
		_AICParam1.setEditable(false);
		expPanel.add(_AICParam1);
		final JLabel nullLabel2 = new JLabel("");
		expPanel.add(nullLabel2);

		final JLabel nullLabel3 = new JLabel("");
		expPanel.add(nullLabel3);
		_errorLabel1 = new JLabel(FITTING_ERROR);
		_errorLabel1.setVisible(false);
		expPanel.add(_errorLabel1);
		final JLabel nullLabel4 = new JLabel("");
		expPanel.add(nullLabel4);
		
		

		// TODO:
		// SLIMPlotter look & feel:
		// Color fixColor = _a1Param1.getBackground();
		// Color floatColor = a1Label1.getBackground();
		// _a1Param1.setBackground(floatColor);
		// _t1Param1.setBackground(floatColor);
		// _zParam1.setBackground(floatColor);
		// _chiSqParam1.setBackground(floatColor);

		// rows, cols, initX, initY, xPad, yPad
		SpringUtilities.makeCompactGrid(expPanel, 6, 3, 4, 4, 4, 4);

		final JPanel panel = new JPanel(new BorderLayout());
		panel.add("North", expPanel);

		_startParam1 = new JCheckBox("Use as starting parameters for fit");
		_startParam1.setSelected(true);
		_startParam1.setEnabled(false);

		// TODO ARG 9/21/12 disabled non-functioning UI
		// panel.add("South", _startParam1);
		return panel;
	}

	/*
	 * Creates panel for the double exponential version of the fit parameters.
	 */
	private JPanel createDoubleExponentialPanel(final String lifetimeLabel) {
		final JPanel expPanel = new JPanel();
		expPanel.setBorder(new EmptyBorder(0, 0, 8, 8));
		expPanel.setLayout(new SpringLayout());

		final JLabel a1Label2 = new JLabel("A" + SUB_1);
		a1Label2.setHorizontalAlignment(SwingConstants.RIGHT);
		expPanel.add(a1Label2);
		_a1Param2 = new JTextField(9);
		// _a1Param2.setEditable(false);
		expPanel.add(_a1Param2);
		_a1Fix2 = new JCheckBox("Fix");
		// _a1Fix2.addItemListener(this);
		expPanel.add(_a1Fix2);
		// refitUponStateChange(_a1Param2, _a1Fix2)
		refitUponStateChangeA1(_a1Param2, _a1Fix2);// sagar

		final JLabel t1Label2 = new JLabel(lifetimeLabel + SUB_1);
		t1Label2.setHorizontalAlignment(SwingConstants.RIGHT);
		expPanel.add(t1Label2);
		_t1Param2 = new JTextField(9);
		// _t1Param2.setEditable(false);
		expPanel.add(_t1Param2);
		_t1Fix2 = new JCheckBox("Fix");
		// _t1Fix2.addItemListener(this);
		expPanel.add(_t1Fix2);
		// refitUponStateChange(_t1Param2, _t1Fix2);//sagar
		refitUponStateChangeT1(_t1Param2, _t1Fix2);

		final JLabel a2Label2 = new JLabel("A" + SUB_2);
		a2Label2.setHorizontalAlignment(SwingConstants.RIGHT);
		expPanel.add(a2Label2);
		_a2Param2 = new JTextField(9);
		// _a2Param2.setEditable(false);
		expPanel.add(_a2Param2);
		_a2Fix2 = new JCheckBox("Fix");
		// _a2Fix2.addItemListener(this);
		expPanel.add(_a2Fix2);
		// refitUponStateChange(_a2Param2, _a2Fix2);//sagar
		refitUponStateChangeA2(_a2Param2, _a2Fix2);// sagar

		final JLabel t2Label2 = new JLabel(lifetimeLabel + SUB_2);
		t2Label2.setHorizontalAlignment(SwingConstants.RIGHT);
		expPanel.add(t2Label2);
		_t2Param2 = new JTextField(9);
		// _t2Param2.setEditable(false);
		expPanel.add(_t2Param2);
		_t2Fix2 = new JCheckBox("Fix");
		// _t2Fix2.addItemListener(this);
		expPanel.add(_t2Fix2);
		// refitUponStateChange(_t2Param2, _t2Fix2);//sagar
		refitUponStateChangeT2(_t2Param2, _t2Fix2);// sagar

		final JLabel zLabel2 = new JLabel("Z");
		zLabel2.setHorizontalAlignment(SwingConstants.RIGHT);
		expPanel.add(zLabel2);
		_zParam2 = new JTextField(9);
		// _zParam2.setEditable(false);
		expPanel.add(_zParam2);
		_zFix2 = new JCheckBox("Fix");
		// _zFix2.addItemListener(this);
		expPanel.add(_zFix2);
//		refitUponStateChange(_zParam2, _zFix2);
		refitUponStateChangeZ1(_zParam2, _zFix2);

		final JLabel chiSqLabel2 = new JLabel("" + CHI + SQUARE + SUB_R);
		chiSqLabel2.setHorizontalAlignment(SwingConstants.RIGHT);
		expPanel.add(chiSqLabel2);
		_chiSqParam2 = new JTextField(9);
		_chiSqParam2.setEditable(false);
		expPanel.add(_chiSqParam2);
		final JLabel nullLabel2 = new JLabel("");
		expPanel.add(nullLabel2);

		final JLabel AICLabel2 = new JLabel("AIC");
		AICLabel2.setHorizontalAlignment(SwingConstants.RIGHT);
		expPanel.add(AICLabel2);
		_AICParam2 = new JTextField(9);
		_AICParam2.setEditable(false);
		expPanel.add(_AICParam2);
		final JLabel nullLabel3 = new JLabel("");
		expPanel.add(nullLabel3);

		final JLabel nullLabel4 = new JLabel("");
		expPanel.add(nullLabel4);
		_errorLabel2 = new JLabel(FITTING_ERROR);
		_errorLabel2.setVisible(false);
		expPanel.add(_errorLabel2);
		final JLabel nullLabel5 = new JLabel("");
		expPanel.add(nullLabel5);

		// TODO:
		// From SLIMPlotter
		// Color fixColor = _a1Param2.getBackground();
		// Color floatColor = a1Label2.getBackground();
		// _a1Param2.setBackground(floatColor);
		// _t1Param2.setBackground(floatColor);
		// _a2Param2.setBackground(floatColor);
		// _t2Param2.setBackground(floatColor);
		// _zParam2.setBackground(floatColor);
		// _chiSqParam2.setBackground(floatColor);

		// rows, cols, initX, initY, xPad, yPad
		SpringUtilities.makeCompactGrid(expPanel, 8, 3, 4, 4, 4, 4);

		final JPanel panel = new JPanel(new BorderLayout());
		panel.add("North", expPanel);

		_startParam2 = new JCheckBox("Use as starting parameters for fit");
		_startParam2.setSelected(true);
		_startParam2.setEnabled(false);
		// TODO ARG 9/21/12 disabled non-functioning UI
		// panel.add("South", _startParam2);
		return panel;
	}

	/*
	 * Creates panel for the triple exponential version of the fit parameters.
	 */
	private JPanel createTripleExponentialPanel(final String lifetimeLabel) {
		final JPanel expPanel = new JPanel();
		expPanel.setBorder(new EmptyBorder(0, 0, 8, 8));
		expPanel.setLayout(new SpringLayout());

		final JLabel a1Label3 = new JLabel("A" + SUB_1);
		a1Label3.setHorizontalAlignment(SwingConstants.RIGHT);
		expPanel.add(a1Label3);
		_a1Param3 = new JTextField(9);
		// _a1Param3.setEditable(false);
		expPanel.add(_a1Param3);
		_a1Fix3 = new JCheckBox("Fix");
		// _a1Fix3.addItemListener(this);
		expPanel.add(_a1Fix3);
		// refitUponStateChange(_a1Param3, _a1Fix3);//sagar
		refitUponStateChangeA1(_a1Param3, _a1Fix3);

		final JLabel t1Label3 = new JLabel(lifetimeLabel + SUB_1);
		t1Label3.setHorizontalAlignment(SwingConstants.RIGHT);
		expPanel.add(t1Label3);
		_t1Param3 = new JTextField(9);
		// _t1Param3.setEditable(false);
		expPanel.add(_t1Param3);
		_t1Fix3 = new JCheckBox("Fix");
		// _t1Fix3.addItemListener(this);
		expPanel.add(_t1Fix3);
		// refitUponStateChange(_t1Param3, _t1Fix3);//sagar
		refitUponStateChangeT1(_t1Param3, _t1Fix3);

		final JLabel a2Label3 = new JLabel("A" + SUB_2);
		a2Label3.setHorizontalAlignment(SwingConstants.RIGHT);
		expPanel.add(a2Label3);
		_a2Param3 = new JTextField(9);
		// _a2Param3.setEditable(false);
		expPanel.add(_a2Param3);
		_a2Fix3 = new JCheckBox("Fix");
		// _a2Fix3.addItemListener(this);
		expPanel.add(_a2Fix3);
		// refitUponStateChange(_a2Param3, _a2Fix3);

		refitUponStateChangeA2(_a2Param3, _a2Fix3);

		final JLabel t2Label3 = new JLabel(lifetimeLabel + SUB_2);
		t2Label3.setHorizontalAlignment(SwingConstants.RIGHT);
		expPanel.add(t2Label3);
		_t2Param3 = new JTextField(9);
		// _t2Param3.setEditable(false);
		expPanel.add(_t2Param3);
		_t2Fix3 = new JCheckBox("Fix");
		// _t2Fix3.addItemListener(this);
		expPanel.add(_t2Fix3);
		// refitUponStateChange(_t2Param3, _t2Fix3);//sagar
		refitUponStateChangeT2(_t2Param3, _t2Fix3);

		final JLabel a3Label3 = new JLabel("A" + SUB_3);
		a3Label3.setHorizontalAlignment(SwingConstants.RIGHT);
		expPanel.add(a3Label3);
		_a3Param3 = new JTextField(9);
		// _a3Param3.setEditable(false);
		expPanel.add(_a3Param3);
		_a3Fix3 = new JCheckBox("Fix");
		// _a3Fix3.addItemListener(this);
		expPanel.add(_a3Fix3);
		// refitUponStateChange(_a3Param3, _a3Fix3);
		refitUponStateChangeA3(_a3Param3, _a3Fix3);

		final JLabel t3Label3 = new JLabel(lifetimeLabel + SUB_3);
		t3Label3.setHorizontalAlignment(SwingConstants.RIGHT);
		expPanel.add(t3Label3);
		_t3Param3 = new JTextField(9);
		// _t3Param3.setEditable(false);
		expPanel.add(_t3Param3);
		_t3Fix3 = new JCheckBox("Fix");
		// _t3Fix3.addItemListener(this);
		expPanel.add(_t3Fix3);
		// refitUponStateChange(_t3Param3, _t3Fix3);
		refitUponStateChangeT3(_t3Param3, _t3Fix3);

		final JLabel zLabel3 = new JLabel("Z");
		zLabel3.setHorizontalAlignment(SwingConstants.RIGHT);
		expPanel.add(zLabel3);
		_zParam3 = new JTextField(9);
		// _zParam3.setEditable(false);
		expPanel.add(_zParam3);
		_zFix3 = new JCheckBox("Fix");
		// _zFix3.addItemListener(this);
		expPanel.add(_zFix3);
//		refitUponStateChange(_zParam3, _zFix3);//sagar
		refitUponStateChangeZ1(_zParam3, _zFix3);

		final JLabel chiSqLabel3 = new JLabel("" + CHI + SQUARE + SUB_R);
		chiSqLabel3.setHorizontalAlignment(SwingConstants.RIGHT);
		expPanel.add(chiSqLabel3);
		_chiSqParam3 = new JTextField(9);
		_chiSqParam3.setEditable(false);
		expPanel.add(_chiSqParam3);
		final JLabel nullLabel3 = new JLabel("");
		expPanel.add(nullLabel3);

		final JLabel AICLabel3 = new JLabel("AIC");
		AICLabel3.setHorizontalAlignment(SwingConstants.RIGHT);
		expPanel.add(AICLabel3);
		_AICParam3 = new JTextField(9);
		_AICParam3.setEditable(false);
		expPanel.add(_AICParam3);
		final JLabel nullLabel4 = new JLabel("");
		expPanel.add(nullLabel4);

		final JLabel nullLabel5 = new JLabel("");
		expPanel.add(nullLabel5);
		_errorLabel3 = new JLabel(FITTING_ERROR);
		_errorLabel3.setVisible(false);
		expPanel.add(_errorLabel3);
		final JLabel nullLabel6 = new JLabel("");
		expPanel.add(nullLabel6);

		// TODO:
		// SLIMPlotter look & feel:
		// Color fixColor = _a1Param3.getBackground();
		// Color floatColor = a1Label3.getBackground();
		// _a1Param3.setBackground(floatColor);
		// _t1Param3.setBackground(floatColor);
		// _a2Param3.setBackground(floatColor);
		// _t2Param3.setBackground(floatColor);
		// _a3Param3.setBackground(floatColor);
		// _t3Param3.setBackground(floatColor);
		// _zParam3.setBackground(floatColor);
		// _chiSqParam3.setBackground(floatColor);

		// rows, cols, initX, initY, xPad, yPad
		SpringUtilities.makeCompactGrid(expPanel, 10, 3, 4, 4, 4, 4);

		final JPanel panel = new JPanel(new BorderLayout());
		panel.add("North", expPanel);

		_startParam3 = new JCheckBox("Use as starting parameters for fit");
		_startParam3.setSelected(true);
		_startParam3.setEnabled(false);
		// TODO ARG 9/21/12 disabled non-functioning UI
		// panel.add("South", _startParam3);
		return panel;
	}

	/*
	 * Creates panel for the stretched exponential version of the fit parameters.
	 */
	private JPanel createStretchedExponentialPanel(final String lifetimeLabel) {
		final JPanel expPanel = new JPanel();
		expPanel.setBorder(new EmptyBorder(0, 0, 8, 8));
		expPanel.setLayout(new SpringLayout());

		final JLabel a1Label4 = new JLabel("A");
		a1Label4.setHorizontalAlignment(SwingConstants.RIGHT);
		expPanel.add(a1Label4);
		_aParam4 = new JTextField(9);
		// _a1Param1.setEditable(false);
		expPanel.add(_aParam4);
		_aFix4 = new JCheckBox("Fix");
		// _a1Fix1.addItemListener(this);
		expPanel.add(_aFix4);
		// refitUponStateChange(_aParam4, _aFix4);//SAGAR
		refitUponStateChangeA1(_aParam4, _aFix4);

		final JLabel tLabel4 = new JLabel(lifetimeLabel);
		tLabel4.setHorizontalAlignment(SwingConstants.RIGHT);
		expPanel.add(tLabel4);
		_tParam4 = new JTextField(9);
		// _t1Param1.setEditable(false);
		expPanel.add(_tParam4);
		_tFix4 = new JCheckBox("Fix");
		// _t1Fix1.addItemListener(this);
		expPanel.add(_tFix4);
		// refitUponStateChange(_tParam4, _tFix4);//sagar
		refitUponStateChangeT1(_tParam4, _tFix4);

		final JLabel hLabel4 = new JLabel("H");
		hLabel4.setHorizontalAlignment(SwingConstants.RIGHT);
		expPanel.add(hLabel4);
		_hParam4 = new JTextField(9);
		// _hParam4.setEditable(false);
		expPanel.add(_hParam4);
		_hFix4 = new JCheckBox("Fix");
		// _hFix4.addItemListener(this);
		expPanel.add(_hFix4);
		// refitUponStateChange(_hParam4, _hFix4);
		refitUponStateChangeH1(_hParam4, _hFix4);

		final JLabel zLabel1 = new JLabel("Z");
		zLabel1.setHorizontalAlignment(SwingConstants.RIGHT);
		expPanel.add(zLabel1);
		_zParam4 = new JTextField(9);
		// _zParam1.setEditable(false);
		expPanel.add(_zParam4);
		_zFix4 = new JCheckBox("Fix");
		// _zFix1.addItemListener(this);
		expPanel.add(_zFix4);
		// refitUponStateChange(_zParam4, _zFix4);//sagar
		refitUponStateChangeZ1(_zParam4, _zFix4);

		final JLabel chiSqLabel4 = new JLabel("" + CHI + SQUARE + SUB_R);
		chiSqLabel4.setHorizontalAlignment(SwingConstants.RIGHT);
		expPanel.add(chiSqLabel4);
		_chiSqParam4 = new JTextField(9);
		_chiSqParam4.setEditable(false);
		expPanel.add(_chiSqParam4);
		final JLabel nullLabel4 = new JLabel("");
		expPanel.add(nullLabel4);

		final JLabel AICLabel4 = new JLabel("AIC");
		AICLabel4.setHorizontalAlignment(SwingConstants.RIGHT);
		expPanel.add(AICLabel4);
		_AICParam4 = new JTextField(9);
		_AICParam4.setEditable(false);
		expPanel.add(_AICParam4);
		final JLabel nullLabel5 = new JLabel("");
		expPanel.add(nullLabel5);

		final JLabel nullLabel6 = new JLabel("");
		expPanel.add(nullLabel6);
		_errorLabel4 = new JLabel(FITTING_ERROR);
		_errorLabel4.setVisible(false);
		expPanel.add(_errorLabel4);
		final JLabel nullLabel7 = new JLabel("");
		expPanel.add(nullLabel7);

		// TODO:
		// SLIMPlotter look & feel:
		// Color fixColor = _a1Param1.getBackground();
		// Color floatColor = a1Label1.getBackground();
		// _a1Param1.setBackground(floatColor);
		// _t1Param1.setBackground(floatColor);
		// _zParam1.setBackground(floatColor);
		// _chiSqParam1.setBackground(floatColor);

		// rows, cols, initX, initY, xPad, yPad
		SpringUtilities.makeCompactGrid(expPanel, 7, 3, 4, 4, 4, 4);

		final JPanel panel = new JPanel(new BorderLayout());
		panel.add("North", expPanel);

		_startParam4 = new JCheckBox("Use as starting parameters for fit");
		_startParam4.setSelected(true);
		_startParam4.setEnabled(false);
		// TODO ARG 9/21/12 disabled non-functioning UI
		// panel.add("South", _startParam4);
		return panel;
	}

	private Border border(final String title) {
		return BorderFactory.createCompoundBorder(BorderFactory.createTitledBorder(
			ETCHED_BORDER, title), EMPTY_BORDER);
	}

	private void setFitButtonState(final boolean on) {
		_fitButton.setText(on ? _fitButtonText : CANCEL_FIT);
	}

	private boolean getFitButtonState() {
		return _fitButton.getText().equals(_fitButtonText);
	}

	/**
	 * Triggers refit if dropdown list selection changes.
	 *
	 */
	private void refitUponStateChange(final ItemSelectable itemSelectable) {
		itemSelectable.addItemListener(new ItemListener() {

			@Override
			public void itemStateChanged(final ItemEvent e) {
				// /item added for macro recording
				// sagar

				if (e.getStateChange() == ItemEvent.SELECTED && null != _listener) {
					_listener.reFit();
				}

			}
		});
	}

	/**
	 * macro recorder specific macro recorder This one is for Binning
	 *
	 */

	private void refitUponStateChangeBinning(final ItemSelectable itemSelectable)
	{
		itemSelectable.addItemListener(new ItemListener() {

			@Override
			public void itemStateChanged(final ItemEvent e) {
				// /item added for macro recording
				// sagar

				if (e.getStateChange() == ItemEvent.SELECTED && null != _listener) {
					final String item = (String) e.getItem();
					SLIMProcessor.record(SLIMProcessor.SET_BINNING, item);
					_listener.reFit();
				}

			}
		});
	}
	
	private void refitUponStateChangeMeanValue(final ItemSelectable itemSelectable)
	{
		itemSelectable.addItemListener(new ItemListener() {

			@Override
			public void itemStateChanged(final ItemEvent e) {
				// /item added for macro recording
				// sagar

				if (e.getStateChange() == ItemEvent.SELECTED && null != _listener) {
					final String item = (String) e.getItem();
					int num=((JComboBox) itemSelectable).getSelectedIndex();
					//get the mean values
					IJ.log(Integer.toString(num));
					//_listener.reFit();
				}

			}
		});
	}

	/**
	 * macro recorder specific macro recorder This one is for Algorithm
	 *
	 */

	private void
		refitUponStateChangeAlgorithm(final ItemSelectable itemSelectable)
	{
		itemSelectable.addItemListener(new ItemListener() {

			@Override
			public void itemStateChanged(final ItemEvent e) {
				// /item added for macro recording
				// sagar

				if (e.getStateChange() == ItemEvent.SELECTED && null != _listener) {
					final String item = (String) e.getItem();
					SLIMProcessor.record(SLIMProcessor.SET_ALGORITHM_TYPE, item);
					_listener.reFit();
				}

			}
		});
	}

	/**
	 * macro recorder specific macro recorder This one is for fitted Images
	 *
	 */

	private void refitUponStateChangeFittedImage(
		final ItemSelectable itemSelectable)
	{
		itemSelectable.addItemListener(new ItemListener() {

			@Override
			public void itemStateChanged(final ItemEvent e) {
				// /item added for macro recording
				// sagar

				if (e.getStateChange() == ItemEvent.SELECTED && null != _listener) {
					final String item = (String) e.getItem();

				}

			}
		});
	}

	/**
	 * Triggers refit if text field edited.
	 *
	 */
	private void refitUponStateChange(final JTextField textField) {
		textField.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(final ActionEvent e) {
				// trigger if just edited text
				_listener.reFit();
			}
		});
		textField.addFocusListener(new FocusListener() {

			private String _text;

			@Override
			public void focusGained(final FocusEvent e) {
				_text = textField.getText();
			}

			@Override
			public void focusLost(final FocusEvent e) {

				if (!_text.equals(textField.getText())) {
					// sagar added
					// SLIMProcessor.record(SLIMProcessor.SET_A_VALUE,_text);

					// trigger if just edited text
					_listener.reFit();
				}
			}
		});
	}

	/**
	 * Triggers refit if fitted parameter value or checkbox change.
	 *
	 */
	private void refitUponStateChange(final JTextField textField,
		final JCheckBox checkBox)
	{
		refitUponStateChange(textField);

		checkBox.addItemListener(new ItemListener() {

			@Override
			public void itemStateChanged(final ItemEvent e) {
				// definitely trigger if DESELECTED
				// also if SELECTED, in case text field already edited
				// IJ.log("reached");
				_listener.reFit();
			}
		});
	}

	void refitUponStateChangeZ1(final JTextField textField,
		final JCheckBox checkBox)
	{

		textField.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(final ActionEvent e) {
				// trigger if just edited text
				_listener.reFit();
			}
		});
		textField.addFocusListener(new FocusListener() {

			private String _text;

			@Override
			public void focusGained(final FocusEvent e) {
				_text = textField.getText();

			}

			@Override
			public void focusLost(final FocusEvent e) {

				if (!_text.equals(textField.getText())) {
					// sagar
					// SLIMProcessor.record(SLIMProcessor.SET_A_VALUE,_text);

					// trigger if just edited text
					_listener.reFit();
					SLIMProcessor.record(SLIMProcessor.SET_Z1_VALUE, textField.getText());
					SLIMProcessor.macroParams.z1macroused = false;
				}
			}
		});

		checkBox.addItemListener(new ItemListener() {

			@Override
			public void itemStateChanged(final ItemEvent e) {
				// definitely trigger if DESELECTED
				// also if SELECTED, in case text field already edited
				// IJ.log("reached");
				_listener.reFit();

				// //macro recorder
				SLIMProcessor.record(SLIMProcessor.SET_Z1_FIX, Boolean
					.toString(checkBox.isSelected()));

			}
		});
	}

	void refitUponStateChangeH1(final JTextField textField,
		final JCheckBox checkBox)
	{

		textField.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(final ActionEvent e) {
				// trigger if just edited text
				_listener.reFit();
			}
		});
		textField.addFocusListener(new FocusListener() {

			private String _text;

			@Override
			public void focusGained(final FocusEvent e) {
				_text = textField.getText();

			}

			@Override
			public void focusLost(final FocusEvent e) {

				if (!_text.equals(textField.getText())) {
					// sagar
					// SLIMProcessor.record(SLIMProcessor.SET_A_VALUE,_text);

					// trigger if just edited text
					_listener.reFit();
					SLIMProcessor.record(SLIMProcessor.SET_H1_VALUE, textField.getText());
					SLIMProcessor.macroParams.h1macroused = false;
				}
			}
		});

		checkBox.addItemListener(new ItemListener() {

			@Override
			public void itemStateChanged(final ItemEvent e) {
				// definitely trigger if DESELECTED
				// also if SELECTED, in case text field already edited
				// IJ.log("reached");
				_listener.reFit();
				SLIMProcessor.record(SLIMProcessor.SET_H_FIX, Boolean.toString(checkBox
					.isSelected()));
			}
		});
	}

	void refitUponStateChangeT1(final JTextField textField,
		final JCheckBox checkBox)
	{

		textField.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(final ActionEvent e) {
				// trigger if just edited text
				_listener.reFit();
			}
		});
		textField.addFocusListener(new FocusListener() {

			private String _text;

			@Override
			public void focusGained(final FocusEvent e) {
				_text = textField.getText();

			}

			@Override
			public void focusLost(final FocusEvent e) {

				if (!_text.equals(textField.getText())) {
					// sagar
					// SLIMProcessor.record(SLIMProcessor.SET_A_VALUE,_text);

					// trigger if just edited text
					_listener.reFit();
					if (!SLIMProcessor.macroParams.isa1MacroRecording) {
						SLIMProcessor.macroParams.ist1MacroRecording = true;

						SLIMProcessor.record(SLIMProcessor.SET_T1_VALUE, textField
							.getText());
						SLIMProcessor.macroParams.t1macroused = false;
					}

					SLIMProcessor.macroParams.isa1MacroRecording = false;
				}
			}
		});

		checkBox.addItemListener(new ItemListener() {

			@Override
			public void itemStateChanged(final ItemEvent e) {
				// definitely trigger if DESELECTED
				// also if SELECTED, in case text field already edited
				// IJ.log("reached");
				_listener.reFit();
				SLIMProcessor.record(SLIMProcessor.SET_T1_FIX, Boolean
					.toString(checkBox.isSelected()));
			}
		});
	}

	void refitUponStateChangeA1(final JTextField textField,
		final JCheckBox checkBox)
	{

		textField.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(final ActionEvent e) {
				// trigger if just edited text
				_listener.reFit();
			}
		});
		textField.addFocusListener(new FocusListener() {

			private String _text;

			@Override
			public void focusGained(final FocusEvent e) {
				_text = textField.getText();

			}

			@Override
			public void focusLost(final FocusEvent e) {

				if (!_text.equals(textField.getText())) {
					// sagar
					// SLIMProcessor.record(SLIMProcessor.SET_A_VALUE,_text);

					// trigger if just edited text
					_listener.reFit();

					if (!SLIMProcessor.macroParams.ist1MacroRecording) {

						SLIMProcessor.macroParams.isa1MacroRecording = true;

						SLIMProcessor.record(SLIMProcessor.SET_A1_VALUE, textField
							.getText());
						SLIMProcessor.macroParams.a1macroused = false;
					}
					SLIMProcessor.macroParams.ist1MacroRecording = false;
				}
			}
		});

		checkBox.addItemListener(new ItemListener() {

			@Override
			public void itemStateChanged(final ItemEvent e) {
				// definitely trigger if DESELECTED
				// also if SELECTED, in case text field already edited
				// IJ.log("reached");
				_listener.reFit();
				SLIMProcessor.record(SLIMProcessor.SET_A1_FIX, Boolean
					.toString(checkBox.isSelected()));
			}
		});
	}

	void refitUponStateChangeT2(final JTextField textField,
		final JCheckBox checkBox)
	{

		textField.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(final ActionEvent e) {
				// trigger if just edited text
				_listener.reFit();
			}
		});
		textField.addFocusListener(new FocusListener() {

			private String _text;

			@Override
			public void focusGained(final FocusEvent e) {
				_text = textField.getText();

			}

			@Override
			public void focusLost(final FocusEvent e) {

				if (!_text.equals(textField.getText())) {
					// sagar

					// trigger if just edited text
					_listener.reFit();

					if (!SLIMProcessor.macroParams.isa2MacroRecording) {
						SLIMProcessor.macroParams.ist2MacroRecording = true;

						SLIMProcessor.record(SLIMProcessor.SET_T2_VALUE, textField
							.getText());
						SLIMProcessor.macroParams.t2macroused = false;
					}
					SLIMProcessor.macroParams.isa2MacroRecording = false;

				}
			}
		});

		checkBox.addItemListener(new ItemListener() {

			@Override
			public void itemStateChanged(final ItemEvent e) {
				// definitely trigger if DESELECTED
				// also if SELECTED, in case text field already edited
				_listener.reFit();
				SLIMProcessor.record(SLIMProcessor.SET_T2_FIX, Boolean
					.toString(checkBox.isSelected()));
			}
		});
	}

	void refitUponStateChangeA2(final JTextField textField,
		final JCheckBox checkBox)
	{

		textField.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(final ActionEvent e) {
				// trigger if just edited text
				_listener.reFit();
			}
		});
		textField.addFocusListener(new FocusListener() {

			private String _text;

			@Override
			public void focusGained(final FocusEvent e) {
				_text = textField.getText();

			}

			@Override
			public void focusLost(final FocusEvent e) {

				if (!_text.equals(textField.getText())) {
					// sagar
					// SLIMProcessor.record(SLIMProcessor.SET_A_VALUE,_text);

					// trigger if just edited text
					_listener.reFit();

					if (!SLIMProcessor.macroParams.ist2MacroRecording) {
						SLIMProcessor.macroParams.isa2MacroRecording = true;
						SLIMProcessor.record(SLIMProcessor.SET_A2_VALUE, textField
							.getText());
						SLIMProcessor.macroParams.a2macroused = false;

					}

					SLIMProcessor.macroParams.ist2MacroRecording = false;
				}
			}
		});

		checkBox.addItemListener(new ItemListener() {

			@Override
			public void itemStateChanged(final ItemEvent e) {
				// definitely trigger if DESELECTED
				// also if SELECTED, in case text field already edited
				// IJ.log("reached");
				_listener.reFit();
				SLIMProcessor.record(SLIMProcessor.SET_A2_FIX, Boolean
					.toString(checkBox.isSelected()));
			}
		});
	}

	void refitUponStateChangeT3(final JTextField textField,
		final JCheckBox checkBox)
	{

		textField.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(final ActionEvent e) {
				// trigger if just edited text
				_listener.reFit();
			}
		});
		textField.addFocusListener(new FocusListener() {

			private String _text;

			@Override
			public void focusGained(final FocusEvent e) {
				_text = textField.getText();

			}

			@Override
			public void focusLost(final FocusEvent e) {

				if (!_text.equals(textField.getText())) {
					// sagar
					// SLIMProcessor.record(SLIMProcessor.SET_A_VALUE,_text);

					// trigger if just edited text
					_listener.reFit();

					if (!SLIMProcessor.macroParams.isa3MacroRecording) {
						SLIMProcessor.macroParams.ist3MacroRecording = true;
						SLIMProcessor.record(SLIMProcessor.SET_T3_VALUE, textField
							.getText());
						SLIMProcessor.macroParams.t3macroused = false;
					}
					SLIMProcessor.macroParams.isa3MacroRecording = false;
				}
			}
		});

		checkBox.addItemListener(new ItemListener() {

			@Override
			public void itemStateChanged(final ItemEvent e) {
				// definitely trigger if DESELECTED
				// also if SELECTED, in case text field already edited
				// IJ.log("reached");
				_listener.reFit();
				SLIMProcessor.record(SLIMProcessor.SET_T3_FIX, Boolean
					.toString(checkBox.isSelected()));
			}
		});
	}

	void refitUponStateChangeA3(final JTextField textField,
		final JCheckBox checkBox)
	{

		textField.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(final ActionEvent e) {
				// trigger if just edited text
				_listener.reFit();
			}
		});
		textField.addFocusListener(new FocusListener() {

			private String _text;

			@Override
			public void focusGained(final FocusEvent e) {
				_text = textField.getText();

			}

			@Override
			public void focusLost(final FocusEvent e) {

				if (!_text.equals(textField.getText())) {
					// sagar
					// SLIMProcessor.record(SLIMProcessor.SET_A_VALUE,_text);

					// trigger if just edited text
					_listener.reFit();
					if (!SLIMProcessor.macroParams.ist3MacroRecording) {
						SLIMProcessor.macroParams.isa3MacroRecording = true;

						SLIMProcessor.record(SLIMProcessor.SET_A3_VALUE, textField
							.getText());
						SLIMProcessor.macroParams.a3macroused = false;

					}

					SLIMProcessor.macroParams.ist3MacroRecording = false;
				}
			}
		});

		checkBox.addItemListener(new ItemListener() {

			@Override
			public void itemStateChanged(final ItemEvent e) {
				// definitely trigger if DESELECTED
				// also if SELECTED, in case text field already edited
				// IJ.log("reached");
				_listener.reFit();
				SLIMProcessor.record(SLIMProcessor.SET_A3_FIX, Boolean
					.toString(checkBox.isSelected()));
			}
		});
	}

	/**
	 * Triggers refit if spinner value changes.
	 *
	 */
	private void refitUponStateChange(final JSpinner spinner) {
		spinner.addChangeListener(new ChangeListener() {

			@Override
			public void stateChanged(final ChangeEvent e) {
				if (null != _listener) {
					_listener.reFit();
				}
			}
		});
	}

	/**
	 * Propagates a threshold spinner value change.
	 *
	 */
	private void updateThresholdChange(final JSpinner thresholdSpinner) {
		thresholdSpinner.addChangeListener(new ChangeListener() {

			@Override
			public void stateChanged(final ChangeEvent e) {

				final SpinnerModel spinnerModel = thresholdSpinner.getModel();
				if (spinnerModel instanceof SpinnerNumberModel) {
					final int threshold =
						(Integer) ((SpinnerNumberModel) spinnerModel).getValue();
					if (null != _thresholdListener) {
						_thresholdListener.updateThreshold(threshold);
						paramSetMacro.thresholdMacroUsed = false;
						final String threshValue = _thresholdSpinner.getValue().toString();
						SLIMProcessor.record(SLIMProcessor.SET_THRESHOLD, threshValue);
					}
					if (null != _listener) {
						if (FitRegion.SUMMED == getRegion()) {
							// threshold affects a summed fit
							_listener.reFit();
						}
					}
				}
			}
		});
	}

	/*
	 * Disables and enables UI during and after a fit.
	 *
	 */
	private void enableAll(final boolean enable) {
		// fit algorithm settings
		_regionComboBox.setEnabled(enable);
		_algorithmComboBox.setEnabled(enable);
		_functionComboBox.setEnabled(enable);
		_noiseModelComboBox.setEnabled(enable);
		_fittedImagesComboBox.setEnabled(enable);
		_colorizeGrayScale.setEnabled(enable);
		for (final JCheckBox checkBox : _analysisCheckBoxList) {
			checkBox.setEnabled(enable);
		}
		_fitAllChannels.setEnabled(enable);

		// cursors settings
		_transientStartSpinner.setEnabled(enable);
		_dataStartSpinner.setEnabled(enable);
		_transientStopSpinner.setEnabled(enable);
		boolean promptEnable = enable;
		if (enable) {
			// do we have a prompt?
			promptEnable = _fittingCursorHelper.getPrompt();
		}
		enablePromptCursors(promptEnable);
		_promptComboBox.setEnabled(enable);

		// fit control settings
		_xSpinner.setEnabled(enable);
		_ySpinner.setEnabled(enable);
		_thresholdSpinner.setEnabled(enable);
		if (null != _scatterSpinner) {
			_scatterSpinner.setEnabled(enable);
		}
		_chiSqTargetSpinner.setEnabled(enable);
		_binningComboBox.setEnabled(enable);

		// single exponent fit
		_aParam1.setEditable(enable);
		_aFix1.setEnabled(enable);
		_tParam1.setEditable(enable);
		_tFix1.setEnabled(enable);
		_zParam1.setEditable(enable);
		_zFix1.setEnabled(enable);

		// double exponent fit
		_a1Param2.setEditable(enable);
		_a1Fix2.setEnabled(enable);
		_a2Param2.setEditable(enable);
		_a2Fix2.setEnabled(enable);
		_t1Param2.setEditable(enable);
		_t1Fix2.setEnabled(enable);
		_t2Param2.setEditable(enable);
		_t2Fix2.setEnabled(enable);
		_zParam2.setEditable(enable);
		_zFix2.setEnabled(enable);

		// triple exponent fit
		_a1Param3.setEditable(enable);
		_a1Fix3.setEnabled(enable);
		_a2Param3.setEditable(enable);
		_a2Fix3.setEnabled(enable);
		_a3Param3.setEditable(enable);
		_a3Fix3.setEnabled(enable);
		_t1Param3.setEditable(enable);
		_t1Fix3.setEnabled(enable);
		_t2Param3.setEditable(enable);
		_t2Fix3.setEnabled(enable);
		_t3Param3.setEditable(enable);
		_t3Fix3.setEnabled(enable);
		_zParam3.setEditable(enable);
		_zFix3.setEnabled(enable);

		// stretched exonent fit
		_aParam4.setEditable(enable);
		_aFix4.setEnabled(enable);
		_tParam4.setEditable(enable);
		_tFix4.setEnabled(enable);
		_hParam4.setEditable(enable);
		_hFix4.setEnabled(enable);
		_zParam4.setEditable(enable);
		_zFix4.setEnabled(enable);

		if (enable) {
			reconcileStartParam();
		}
	}

	/**
	 * Disables and enables buttons.
	 *
	 */
	private void enableButtons(final boolean enable) {
		_openButton.setEnabled(enable);
		_quitButton.setEnabled(enable);
		_fitButton.setEnabled(enable);
	}

	@Override
	public FitRegion getRegion() {
		FitRegion region = null;
		final String selected = (String) _regionComboBox.getSelectedItem();
		if (selected.equals(SUM_REGION)) {
			region = FitRegion.SUMMED;
		}
		else if (selected.equals(ROIS_REGION)) {
			region = FitRegion.ROI;
		}
		else if (selected.equals(PIXEL_REGION)) {
			region = FitRegion.POINT;
		}
		else if (selected.equals(ALL_REGION)) {
			region = FitRegion.EACH;
		}
		return region;
	}

	@Override
	public void setRegion(final FitRegion region) {
		switch (region) {
			case EACH:
				_regionComboBox.setSelectedItem(ALL_REGION);
				break;
			case POINT:
				_regionComboBox.setSelectedItem(PIXEL_REGION);
				break;
			case ROI:
				_regionComboBox.setSelectedItem(ROIS_REGION);
				break;
			case SUMMED:
				_regionComboBox.setSelectedItem(SUM_REGION);
				break;
			default:
				throw new IllegalStateException("Unknown region: " + region);

		}
	}

	@Override
	public FitAlgorithm getAlgorithm() {

		if (SLIMProcessor.macroParams.getAlgorithm() != null) {
			return SLIMProcessor.macroParams.getAlgorithm();
		}
		else {
			FitAlgorithm algorithm = null;
			final String selected = (String) _algorithmComboBox.getSelectedItem();
			if (selected.equals(JAOLHO_LMA_ALGORITHM)) {
				algorithm = FitAlgorithm.JAOLHO;
			}
			else if (selected.equals(SLIM_CURVE_RLD_ALGORITHM)) {
				algorithm = FitAlgorithm.SLIMCURVE_RLD;
			}
			else if (selected.equals(SLIM_CURVE_LMA_ALGORITHM)) {
				algorithm = FitAlgorithm.SLIMCURVE_LMA;
			}
			else if (selected.equals(SLIM_CURVE_RLD_LMA_ALGORITHM)) {
				algorithm = FitAlgorithm.SLIMCURVE_RLD_LMA;
			}
			return algorithm;
		}
	}

	@Override
	public FitFunction getFunction() {
		FitFunction function = null;
		final String selected = (String) _functionComboBox.getSelectedItem();

		if (selected.equals(SINGLE_EXPONENTIAL)) {
			function = FitFunction.SINGLE_EXPONENTIAL;
		}
		else if (selected.equals(DOUBLE_EXPONENTIAL)) {
			function = FitFunction.DOUBLE_EXPONENTIAL;
		}
		else if (selected.equals(TRIPLE_EXPONENTIAL)) {
			function = FitFunction.TRIPLE_EXPONENTIAL;
		}
		else if (selected.equals(STRETCHED_EXPONENTIAL)) {
			function = FitFunction.STRETCHED_EXPONENTIAL;
		}
		return function;
	}

	@Override
	public String[] getAnalysisList() {

		final List<String> analysisList = new ArrayList<String>();
		final int i = 0;
		for (final JCheckBox checkBox : _analysisCheckBoxList) {
			if (checkBox.isSelected()) {
				analysisList.add(checkBox.getText());
				// Prefs.set(SLIMProcessor.KEY_ANALYSIS_LIST+Integer.toString(i++),checkBox.getText());
			}
		}

		return analysisList.toArray(new String[0]);
	}

	/**
	 * Get noise model for fit.
	 *
	 */
	@Override
	public NoiseModel getNoiseModel() {
		NoiseModel noiseModel = null;
		final String selected = (String) _noiseModelComboBox.getSelectedItem();
		if (selected.equals(GAUSSIAN_FIT)) {
			noiseModel = NoiseModel.GAUSSIAN_FIT;
		}
		else if (selected.equals(POISSON_FIT)) {
			noiseModel = NoiseModel.POISSON_FIT;
		}
		else if (selected.equals(POISSON_DATA)) {
			noiseModel = NoiseModel.POISSON_DATA;
		}
		else if (selected.equals(MAXIMUM_LIKELIHOOD)) {
			noiseModel = NoiseModel.MAXIMUM_LIKELIHOOD;
		}
		return noiseModel;
	}

	/**
	 * Returns list of fitted images to display.
	 *
	 */
	@Override
	public String getFittedImages() {
		final StringBuffer returnValue = new StringBuffer();
		final String selected = (String) _fittedImagesComboBox.getSelectedItem();
		// IJ.log("selected is " + selected);
		final String[] fittedImages = selected.split(" ");
		for (final String fittedImage : fittedImages) {
			final boolean fit = true;
			if ("A".equals(fittedImage)) {

			}
			else if ("T".equals(fittedImage)) {

			}
			else if ("Z".equals(fittedImage)) {

			}
			if (fit) {
				returnValue.append(fittedImage);
				returnValue.append(' ');
			}
		}
		// IJ.log("changes to " + returnValue);
		return returnValue.toString();
	}

	/**
	 * Returns whether to create colorized grayscale fitted images.
	 *
	 */
	@Override
	public boolean getColorizeGrayScale() {
		return _colorizeGrayScale.isSelected();
	}

	@Override
	public boolean getFitAllChannels() {
		return _fitAllChannels.isSelected();
	}

	@Override
	public int getX() {
		return (Integer) _xSpinner.getValue();
	}

	@Override
	public void setX(final int x) {
		_xSpinner.setValue(x);
	}

	@Override
	public int getY() {
		return (Integer) _ySpinner.getValue();
	}

	@Override
	public void setY(final int y) {
		_ySpinner.setValue(y);
	}

	@Override
	public int getThreshold() {

		if (paramSetMacro.thresholdMacroUsed) {
			return SLIMProcessor.macroParams.getThresholdValue();
		}
		else {
			return (Integer) _thresholdSpinner.getValue();
		}

	}

	@Override
	public void setThreshold(final int threshold) {
		_thresholdSpinner.setValue(threshold);
	}

	@Override
	public String getBinning() {
		final String selected = (String) _binningComboBox.getSelectedItem();
		return selected;
	}

	@Override
	public double getChiSquareTarget() {

		if (paramSetMacro.chi2MacroUsed) {
			return SLIMProcessor.macroParams.getChiSquareTarget();
		}
		else {
			return (Double) _chiSqTargetSpinner.getValue();
		}
	}

	@Override
	public void setChiSquareTarget(final double chiSqTarget) {
		_chiSqTargetSpinner.setValue(chiSqTarget);
	}

	@Override
	public double getScatter() {
		// scatter option might be turned off in preferences
		double returnValue = 0.0;
		if (null != _scatterSpinner) {
			returnValue = (Double) _scatterSpinner.getValue();
		}
		return returnValue;
	}

	@Override
	public int getParameterCount() {
		int count = 0;
		final String function = (String) _functionComboBox.getSelectedItem();
		if (function.equals(SINGLE_EXPONENTIAL)) {
			count = 4;
		}
		else if (function.equals(DOUBLE_EXPONENTIAL)) {
			count = 6;
		}
		else if (function.equals(TRIPLE_EXPONENTIAL)) {
			count = 8;
		}
		else if (function.equals(STRETCHED_EXPONENTIAL)) {
			count = 5;
		}
		return count;
	}

	@Override
	public void setFittedParameterCount(final int count) {
		_fittedParameterCount = count;
	}

	@Override
	public double[] getParameters() {
		double parameters[] = null;
		if (_noFit) {
			final String function = (String) _functionComboBox.getSelectedItem();
			if (function.equals(SINGLE_EXPONENTIAL)) {
				parameters = new double[4];
			}
			else if (function.equals(DOUBLE_EXPONENTIAL)) {
				parameters = new double[6];
			}
			else if (function.equals(TRIPLE_EXPONENTIAL)) {
				parameters = new double[8];
			}
			else if (function.equals(STRETCHED_EXPONENTIAL)) {
				parameters = new double[5];
			}
			for (int i = 0; i < parameters.length; ++i) {
				parameters[i] = Double.NaN;
			}
		}
		else {
			try {
				final String function = (String) _functionComboBox.getSelectedItem();
				if (function.equals(SINGLE_EXPONENTIAL)) {
					parameters = new double[4];

					parameters[2] = Double.valueOf(_aParam1.getText());
					parameters[3] = Double.valueOf(_tParam1.getText());
					parameters[1] = Double.valueOf(_zParam1.getText());
//					

					// sagar

					// will be needed in gui free operation
//					parameters[2] = SLIMProcessor.macroParams.a1macroused?SLIMProcessor.macroParams.geta1(): Double.valueOf(_aParam1.getText());
//					parameters[3] = SLIMProcessor.macroParams.t1macroused?SLIMProcessor.macroParams.gett1(): Double.valueOf(_tParam1.getText());
//					parameters[1] = SLIMProcessor.macroParams.z1macroused?SLIMProcessor.macroParams.getz1(): Double.valueOf(_zParam1.getText());

				}
				else if (function.equals(DOUBLE_EXPONENTIAL)) {
					parameters = new double[6];
					parameters[2] = Double.valueOf(_a1Param2.getText());
					parameters[3] = Double.valueOf(_t1Param2.getText());
					parameters[4] = Double.valueOf(_a2Param2.getText());
					parameters[5] = Double.valueOf(_t2Param2.getText());
					parameters[1] = Double.valueOf(_zParam2.getText());

					// will be needed in gui free operation
//					parameters[2] = SLIMProcessor.macroParams.a1macroused?SLIMProcessor.macroParams.geta1(): Double.valueOf(_a1Param2.getText());
//					parameters[3] = SLIMProcessor.macroParams.t1macroused?SLIMProcessor.macroParams.gett1(): Double.valueOf(_t1Param2.getText());
//					parameters[4] = SLIMProcessor.macroParams.a2macroused?SLIMProcessor.macroParams.geta2(): Double.valueOf(_a2Param2.getText());
//					parameters[5] = SLIMProcessor.macroParams.t2macroused?SLIMProcessor.macroParams.gett2(): Double.valueOf(_t2Param2.getText());
//					parameters[1] = SLIMProcessor.macroParams.z1macroused?SLIMProcessor.macroParams.getz1(): Double.valueOf(_zParam2.getText());

				}
				else if (function.equals(TRIPLE_EXPONENTIAL)) {
					parameters = new double[8];
					parameters[2] = Double.valueOf(_a1Param3.getText());
					parameters[3] = Double.valueOf(_t1Param3.getText());
					parameters[4] = Double.valueOf(_a2Param3.getText());
					parameters[5] = Double.valueOf(_t2Param3.getText());
					parameters[6] = Double.valueOf(_a3Param3.getText());
					parameters[7] = Double.valueOf(_t3Param3.getText());
					parameters[1] = Double.valueOf(_zParam3.getText());

					// will be needed in gui free operation
//					parameters[2] = SLIMProcessor.macroParams.a1macroused?SLIMProcessor.macroParams.geta1(): Double.valueOf(_a1Param3.getText());
//					parameters[3] = SLIMProcessor.macroParams.t1macroused?SLIMProcessor.macroParams.gett1(): Double.valueOf(_t1Param3.getText());
//					parameters[4] = SLIMProcessor.macroParams.a2macroused?SLIMProcessor.macroParams.geta2(): Double.valueOf(_a2Param3.getText());
//					parameters[5] = SLIMProcessor.macroParams.t2macroused?SLIMProcessor.macroParams.gett2(): Double.valueOf(_t2Param3.getText());
//					parameters[6] = SLIMProcessor.macroParams.a3macroused?SLIMProcessor.macroParams.geta3(): Double.valueOf(_a3Param3.getText());
//					parameters[7] = SLIMProcessor.macroParams.t3macroused?SLIMProcessor.macroParams.gett3(): Double.valueOf(_t3Param3.getText());
//
//					parameters[1] = SLIMProcessor.macroParams.z1macroused?SLIMProcessor.macroParams.getz1(): Double.valueOf(_zParam3.getText());

					// sagar

				}
				else if (function.equals(STRETCHED_EXPONENTIAL)) {
					parameters = new double[5];
					parameters[2] = Double.valueOf(_aParam4.getText());
					parameters[3] = Double.valueOf(_tParam4.getText());
					parameters[4] = Double.valueOf(_hParam4.getText());
					parameters[1] = Double.valueOf(_zParam4.getText());
					// sagar
					// will be needed in gui free operation
//					parameters[2] = SLIMProcessor.macroParams.a1macroused?SLIMProcessor.macroParams.geta1(): Double.valueOf(_aParam4.getText());
//					parameters[3] = SLIMProcessor.macroParams.t1macroused?SLIMProcessor.macroParams.gett1(): Double.valueOf(_tParam4.getText());
//					parameters[4] = SLIMProcessor.macroParams.h1macroused?SLIMProcessor.macroParams.geth1(): Double.valueOf(_hParam4.getText());
//
//					parameters[1] = SLIMProcessor.macroParams.z1macroused?SLIMProcessor.macroParams.getz1(): Double.valueOf(_zParam4.getText());

				}
			}
			catch (final NumberFormatException e) {
				// TODO recover
			}

			parameters[0] = 0.0; // chiSquare
		}
		return parameters;
	}

	@Override
	public void setParameters(final double params[], final double AIC) {
		// parameters NaN signals error
		_noFit = Double.isNaN(params[0]);

		final String function = (String) _functionComboBox.getSelectedItem();
		if (function.equals(SINGLE_EXPONENTIAL)) {
			String a, t, z, chiSq, aic;
			if (_noFit) {
				// fitted parameters could not be determined
				a = t = z = chiSq = aic = NO_FIT;
			}
			else {
				a = paramToString(params[2], 3);
				t = paramToString(params[3], 3);
				z = paramToString(params[1], 3);
				chiSq = paramToString(params[0], 6);
				aic = paramToString(AIC, 6);
			}
			_aParam1.setText(a);
			_tParam1.setText(t);
			_zParam1.setText(z);
			_chiSqParam1.setText(chiSq);
			_AICParam1.setText(aic);

			// show error message as appropriate
			_errorLabel1.setVisible(_noFit);
		}
		else if (function.equals(DOUBLE_EXPONENTIAL)) {
			String a1, t1, a2, t2, z, chiSq, aic;
			if (_noFit) {
				// fitted parameters could not be determined
				a1 = t1 = a2 = t2 = z = chiSq = aic = NO_FIT;
			}
			else {
				a1 = paramToString(params[2], 3);
				t1 = paramToString(params[3], 3);
				a2 = paramToString(params[4], 3);
				t2 = paramToString(params[5], 3);
				z = paramToString(params[1], 3);
				chiSq = paramToString(params[0], 6);
				aic = paramToString(AIC, 6);
			}

			_a1Param2.setText(a1);
			_t1Param2.setText(t1);
			_a2Param2.setText(a2);
			_t2Param2.setText(t2);
			_zParam2.setText(z);
			_chiSqParam2.setText(chiSq);
			_AICParam2.setText(aic);

			// show error message as appropriate
			_errorLabel2.setVisible(_noFit);
		}
		else if (function.equals(TRIPLE_EXPONENTIAL)) {
			String a1, t1, a2, t2, a3, t3, z, chiSq, aic;
			if (_noFit) {
				// fitted parameters could not be determined
				a1 = t1 = a2 = t2 = a3 = t3 = z = chiSq = aic = NO_FIT;
			}
			else {
				a1 = paramToString(params[2], 3);
				t1 = paramToString(params[3], 3);
				a2 = paramToString(params[4], 3);
				t2 = paramToString(params[5], 3);
				a3 = paramToString(params[6], 3);
				t3 = paramToString(params[7], 3);
				z = paramToString(params[1], 3);
				chiSq = paramToString(params[0], 6);
				aic = paramToString(AIC, 6);
			}
			_a1Param3.setText(a1);
			_t1Param3.setText(t1);
			_a2Param3.setText(a2);
			_t2Param3.setText(t2);
			_a3Param3.setText(a3);
			_t3Param3.setText(t3);
			_zParam3.setText(z);
			_chiSqParam3.setText(chiSq);
			_AICParam3.setText(aic);

			// show error message as appropriate
			_errorLabel3.setVisible(_noFit);
		}
		else if (function.equals(STRETCHED_EXPONENTIAL)) {
			String a, t, h, z, chiSq, aic;
			if (_noFit) {
				// fitted parameters could not be determined
				a = t = h = z = chiSq = aic = NO_FIT;
			}
			else {
				a = paramToString(params[2], 3);
				t = paramToString(params[3], 3);
				h = paramToString(params[4], 3);
				z = paramToString(params[1], 3);
				chiSq = paramToString(params[0], 6);
				aic = paramToString(AIC, 6);
			}
			_aParam4.setText(a);
			_tParam4.setText(t);
			_hParam4.setText(h);
			_zParam4.setText(z);
			_chiSqParam4.setText(chiSq);
			_AICParam4.setText(aic);

			// show error message as appropriate
			_errorLabel4.setVisible(_noFit);
		}
	}

	private String paramToString(final double param, final int places) {
		return "" + _fitterEstimator.roundToDecimalPlaces(param, places);
	}

	/**
	 * This version is used to initialize the parameters.
	 *
	 */
	@Override
	public void setFunctionParameters(final int function, final double params[]) {
		switch (function) {
			case 0:
				_aParam1.setText("" + (float) params[2]);
				_tParam1.setText("" + (float) params[3]);
				_zParam1.setText("" + (float) params[1]);
				_chiSqParam1.setText("" + (float) params[0]);
				_errorLabel1.setVisible(false);
				break;
			case 1:
				_a1Param2.setText("" + (float) params[2]);
				_t1Param2.setText("" + (float) params[3]);
				_a2Param2.setText("" + (float) params[4]);
				_t2Param2.setText("" + (float) params[5]);
				_zParam2.setText("" + (float) params[1]);
				_chiSqParam2.setText("" + (float) params[0]);
				_errorLabel2.setVisible(false);
				break;
			case 2:
				_a1Param3.setText("" + (float) params[2]);
				_t1Param3.setText("" + (float) params[3]);
				_a2Param3.setText("" + (float) params[4]);
				_t2Param3.setText("" + (float) params[5]);
				_a3Param3.setText("" + (float) params[6]);
				_t3Param3.setText("" + (float) params[7]);
				_zParam3.setText("" + (float) params[1]);
				_chiSqParam3.setText("" + (float) params[0]);
				_errorLabel3.setVisible(false);
				break;
			case 3:
				_aParam4.setText("" + (float) params[0]);
				_tParam4.setText("" + (float) params[1]);
				_hParam4.setText("" + (float) params[2]);
				_zParam4.setText("" + (float) params[1]);
				_chiSqParam4.setText("" + (float) params[0]);
				_errorLabel4.setVisible(false);
				break;
		}
	}

	@Override
	public boolean[] getFree() {
		boolean free[] = null;
		final String function = (String) _functionComboBox.getSelectedItem();// sagar
		// String function = SLIMProcessor.macroParams.getFunction().toString();
		if (function.equals(SINGLE_EXPONENTIAL)) {
			free = new boolean[3];
			free[0] = !_aFix1.isSelected();
			free[1] = !_tFix1.isSelected();
			free[2] = !_zFix1.isSelected();
		}
		else if (function.equals(DOUBLE_EXPONENTIAL)) {
			free = new boolean[5];
			free[0] = !_a1Fix2.isSelected();
			free[1] = !_t1Fix2.isSelected();
			free[2] = !_a2Fix2.isSelected();
			free[3] = !_t2Fix2.isSelected();
			free[4] = !_zFix2.isSelected();
		}
		else if (function.equals(TRIPLE_EXPONENTIAL)) {
			free = new boolean[7];
			free[0] = !_a1Fix3.isSelected();
			free[1] = !_t1Fix3.isSelected();
			free[2] = !_a2Fix3.isSelected();
			free[3] = !_t2Fix3.isSelected();
			free[4] = !_a3Fix3.isSelected();
			free[5] = !_t3Fix3.isSelected();
			free[6] = !_zFix3.isSelected();

		}
		else if (function.equals(STRETCHED_EXPONENTIAL)) {
			free = new boolean[4];
			free[0] = !_aFix4.isSelected();
			free[1] = !_tFix4.isSelected();
			free[2] = !_hFix4.isSelected();
			free[3] = !_zFix4.isSelected();
		}
		return free;
	}

	@Override
	public boolean getRefineFit() {
		JCheckBox checkBox = null;
		final String function = (String) _functionComboBox.getSelectedItem();
		if (function.equals(SINGLE_EXPONENTIAL)) {
			checkBox = _startParam1;
		}
		else if (function.equals(DOUBLE_EXPONENTIAL)) {
			checkBox = _startParam2;
		}
		else if (function.equals(TRIPLE_EXPONENTIAL)) {
			checkBox = _startParam3;
		}
		else if (function.equals(STRETCHED_EXPONENTIAL)) {
			checkBox = _startParam4; // TODO use an array of checkboxes, etc.
		}
		return !checkBox.isSelected();
	}

	/**
	 * Gets the transient start cursor.
	 *
	 */
	@Override
	public String getTransientStart() {
		return _transientStartSpinner.getValue().toString();
	}

	/**
	 * Sets the transient start cursor.
	 *
	 */
	@Override
	public void setTransientStart(final String transientStart) {
		_transientStartSpinner.setValue(Double.parseDouble(transientStart));
	}

	/**
	 * Gets the data start cursor.
	 * 
	 */
	@Override
	public String getDataStart() {

		return _dataStartSpinner.getValue().toString();
	}

	/**
	 * Sets the data start cursor.
	 * 
	 */
	@Override
	public void setDataStart(final String dataStart) {
		_dataStartSpinner.setValue(Double.parseDouble(dataStart));
	}

	/**
	 * Gets the transient end cursor.
	 *
	 */
	@Override
	public String getTransientStop() {
		return _transientStopSpinner.getValue().toString();
	}

	/**
	 * Sets the transient end cursor.
	 *
	 */
	@Override
	public void setTransientStop(final String transientStop) {
		_transientStopSpinner.setValue(Double.parseDouble(transientStop));
	}

	/**
	 * Gets the excitation delay cursor.
	 *
	 */
	@Override
	public String getPromptDelay() {
		return _promptDelaySpinner.getValue().toString();
	}

	/**
	 * Sets the excitation delay cursor.
	 *
	 */
	@Override
	public void setPromptDelay(final String promptDelay) {
		_promptDelaySpinner.setValue(Double.parseDouble(promptDelay));
	}

	/**
	 * Gets the excitation width cursor.
	 * 
	 */
	@Override
	public String getPromptWidth() {
		return _promptWidthSpinner.getValue().toString();
	}

	/**
	 * Sets the excitation width cursor.
	 *
	 */
	@Override
	public void setPromptWidth(final String promptWidth) {
		_promptWidthSpinner.setValue(Double.parseDouble(promptWidth));
	}

	/**
	 * Gets the excitation baseline.
	 *
	 */
	@Override
	public String getPromptBaseline() {
		return _promptBaselineSpinner.getValue().toString();
	}

	/**
	 * Sets the excitation baseline.
	 *
	 */
	@Override
	public void setPromptBaseline(final String promptBaseline) {
		_promptBaselineSpinner.setValue(Double.parseDouble(promptBaseline));
	}

	private int parseInt(final JTextField field) {
		int value = 0;
		try {
			value = Integer.parseInt(field.getText());
		}
		catch (final NumberFormatException e) {
			IJ.log("Error parsing " + field.getName());
		}
		return value;
	}

	/**
	 * Gray out the prompt cursors if no prompt is loaded.
	 *
	 */
	private void enablePromptCursors(final boolean enable) {
		_promptDelaySpinner.setEnabled(enable);
		_promptWidthSpinner.setEnabled(enable);
		_promptBaselineSpinner.setEnabled(enable);
	}

	/**
	 * This decides whether the existing parameters could be used as the initial
	 * values for another fit.
	 */
	private void reconcileStartParam() {
		// parameter counts happen to be unique for each fit function
		final boolean enable = (_fittedParameterCount == getParameterCount());
		_startParam1.setEnabled(enable);
		_startParam2.setEnabled(enable);
		_startParam3.setEnabled(enable);
		_startParam4.setEnabled(enable);
	}

	/**
	 * Gets the path and name for a prompt file.
	 *
	 */
	private String getFileName(final String title) {
		final OpenDialog dialog = new OpenDialog(title, "");
		return dialog.getDirectory() + dialog.getFileName();
	}
}
