package viewer;

//package edu.purdue.touch.util;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.text.DecimalFormat;
import java.util.Collection;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JToolTip;
import javax.swing.ToolTipManager;
import javax.swing.event.MouseInputAdapter;

import main.AdjMatrix;
import main.Gradient;
import main.Molecule;
import main.SMSDpair;

import org.openscience.cdk.interfaces.IAtomContainer;

import cern.colt.matrix.DoubleMatrix2D;


/**
 * 
 * <p>
 * <strong>Title:</strong> HeatMap
 * </p>
 * 
 * <p>
 * Description: HeatMap is a JPanel that displays a 2-dimensional array of data
 * using a selected color gradient scheme.
 * </p>
 * <p>
 * For specifying data, the first index into the double[][] array is the x-
 * coordinate, and the second index is the y-coordinate. In the constructor and
 * updateData method, the 'useGraphicsYAxis' parameter is used to control
 * whether the row y=0 is displayed at the top or bottom. Since the usual
 * graphics coordinate system has y=0 at the top, setting this parameter to true
 * will draw the y=0 row at the top, and setting the parameter to false will
 * draw the y=0 row at the bottom, like in a regular, mathematical coordinate
 * system. This parameter was added as a solution to the problem of
 * "Which coordinate system should we use? Graphics, or mathematical?", and
 * allows the user to choose either coordinate system. Because the HeatMap will
 * be plotting the data in a graphical manner, using the Java Swing framework
 * that uses the standard computer graphics coordinate system, the user's data
 * is stored internally with the y=0 row at the top.
 * </p>
 * <p>
 * There are a number of defined gradient types (look at the static fields), but

				f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
 * you can create any gradient you like by using either of the following
 * functions in the Gradient class:
 * <ul>
 * <li>public static Color[] createMultiGradient(Color[] colors, int numSteps)</li>
 * <li>public static Color[] createGradient(Color one, Color two, int numSteps)</li>
 * </ul>
 * You can then assign an arbitrary Color[] object to the HeatMap as follows:
 * 
 * <pre>
 * myHeatMap.updateGradient(Gradient.createMultiGradient(new Color[] { Color.red,
 *     Color.white, Color.blue }, 256));
 * </pre>
 * 
 * </p>
 * 
 * <p>
 * By default, the graph title, axis titles, and axis tick marks are not
 * displayed. Be sure to set the appropriate title before enabling them.
 * </p>
 * 
 * <hr />
 * <p>
 * <strong>Copyright:</strong> Copyright (c) 2007, 2008
 * </p>
 * 
 * <p>
 * HeatMap is free software; you can redistribute it and/or modify it under the
 * terms of the GNU General Public License as published by the Free Software
 * Foundation; either version 2 of the License, or (at your option) any later
 * version.
 * </p>
 * 
 * <p>
 * HeatMap is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR
 * A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 * </p>
 * 
 * <p>
 * You should have received a copy of the GNU General Public License along with
 * HeatMap; if not, write to the Free Software Foundation, Inc., 51 Franklin St,
 * Fifth Floor, Boston, MA 02110-1301 USA
 * </p>
 * 
 * @author Matthew Beckler (matthew@mbeckler.org)
 * @author Josh Hayes-Sheen (grey@grevian.org), Converted to use BufferedImage.
 * @author J. Keller (jpaulkeller@gmail.com), Added transparency (alpha)
 *         support, data ordering bug fix.
 * @version 1.6
 */

public class HeatMap extends JPanel {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private static final DecimalFormat DIG2_FORMAT = new DecimalFormat("##.##");
	private static final int MIN_RECT_SZ = 8;
	private double[][] data;
	private int[][] dataColorIndices;
	
	// molecule array from AdjMatrix
	private Molecule[] molArray;
	ImageToolTip chemTip;

	// these four variables are used to print the axis labels
	private double xMin;
	private double xMax;
	private double yMin;
	private double yMax;

	private String title;
	private String xAxis;
	private String yAxis;

	private boolean drawTitle = false;
	private boolean drawXTitle = false;
	private boolean drawYTitle = false;
	private boolean drawLegend = true;
	private boolean drawXTicks = false;
	private boolean drawYTicks = false;

	private Color[] colors;
	private Color bg = Color.white;
	private Color fg = Color.black;

	private BufferedImage bufferedImage;
	private Graphics2D bufferedGraphics;
	private JToolTip molToolTip;
	private StructureDisplay tdp1;
	private Object[][] MCSarray;
	private SideDisplay disp;
	private AdjMatrix adjRef;
	private boolean useGraphicsYAxis;
	private DoubleMatrix2D matrix;

	/**
	 * @param data
	 *            The data to display, must be a complete array (non-ragged)
	 * @param useGraphicsYAxis
	 *            If true, the data will be displayed with the y=0 row at the
	 *            top of the screen. If false, the data will be displayed with
	 *            they=0 row at the bottom of the screen.
	 * @param colors
	 *            A variable of the type Color[]. See also
	 *            {@link #createMultiGradient} and {@link #createGradient}.
	 * @throws Exception 
	 */
	private void init(double[][] data) {
		int lim = 90;			// maximum size of rectangle
		int k = Math.min(lim, (int) Math.ceil(940.0 / (data.length + 1)));		// assuming preferred W=H=1000
		if (k >= MIN_RECT_SZ) {
		this.setPreferredSize(new Dimension(60 + k * (data.length + 1),
				60 + k * (data[0].length + 1)));
		this.setMaximumSize(new Dimension(60 + k * (data.length + 1),
				60 + k * (data[0].length + 1)));
		this.setSize(new Dimension(60 + k * (data.length + 1),
				60 + k * (data[0].length + 1)));
		} else {
			this.setPreferredSize(new Dimension(1000, 1000));
			this.setSize(new Dimension(1000, 1000));
		}
		this.validate();
		updateData(data, this.useGraphicsYAxis);
	}
	
	
	public HeatMap(double[][] data, boolean useGraphicsYAxis, Color[] colors) throws Exception {
		super();		
		this.useGraphicsYAxis = useGraphicsYAxis;
		this.setDoubleBuffered(true);
		this.bg = Color.white;
		this.fg = Color.black;
		
		tdp1 = new StructureDisplay();
		//tdp2 = new StructureDisplay();
		molToolTip = new ImageToolTip(tdp1);
		
		updateGradient(colors);
		init(data);


		// this is the expensive function that draws the data plot into a
		// BufferedImage. The data plot is then cheaply drawn to the screen when
		// needed, saving us a lot of time in the end.
		drawData();
		repaint();
	}
	
	/**
	 * @wbp.parser.constructor
	 */
	public HeatMap(AdjMatrix adm, boolean useGraphicsYAxis, SideDisplay disp, Color[] colors) throws Exception {
		this(adm.getConnMatrix().toArray(), useGraphicsYAxis, colors);
		this.adjRef = adm;
		this.molArray = adm.getMolArray();
		this.MCSarray = adm.getMCSMatrix().toArray();
		this.matrix = adm.getConnMatrix();
		this.disp = disp;
		//addMouseMotionListener(this);
		MouseListener listen = new MouseListener(this);
		this.addMouseListener(listen);
		this.addMouseMotionListener(listen);		
	}

	public void updateMap(int i) {
		if (i == 0) {
			this.molArray = adjRef.getMolArray();
			this.MCSarray = adjRef.getMCSMatrix().toArray();
			this.matrix = adjRef.getConnMatrix();
			init(adjRef.getConnMatrix().toArray());
		} else {
			this.molArray = adjRef.molVector()[i-1];
			this.MCSarray = adjRef.getCCSMSDMatr()[i-1].toArray();
			matrix = adjRef.getCCDoubleMatr()[i-1];
			init(adjRef.getCCDoubleMatr()[i-1].toArray());
		}
		
		drawData();
		repaint();
	}

	/**
	 * Specify the coordinate bounds for the map. Only used for the axis labels,
	 * which must be enabled seperately. Calls repaint() when finished.
	 * 
	 * @param xMin
	 *            The lower bound of x-values, used for axis labels
	 * @param xMax
	 *            The upper bound of x-values, used for axis labels
	 */
	public void setCoordinateBounds(double xMin, double xMax, double yMin,
			double yMax) {
		this.xMin = xMin;
		this.xMax = xMax;
		this.yMin = yMin;
		this.yMax = yMax;

		repaint();
	}

	/**
	 * Specify the coordinate bounds for the X-range. Only used for the axis
	 * labels, which must be enabled seperately. Calls repaint() when finished.
	 * 
	 * @param xMin
	 *            The lower bound of x-values, used for axis labels
	 * @param xMax
	 *            The upper bound of x-values, used for axis labels
	 */
	public void setXCoordinateBounds(double xMin, double xMax) {
		this.xMin = xMin;
		this.xMax = xMax;

		repaint();
	}

	/**
	 * Specify the coordinate bounds for the X Min. Only used for the axis
	 * labels, which must be enabled seperately. Calls repaint() when finished.
	 * 
	 * @param xMin
	 *            The lower bound of x-values, used for axis labels
	 */
	public void setXMinCoordinateBounds(double xMin) {
		this.xMin = xMin;

		repaint();
	}

	/**
	 * Specify the coordinate bounds for the X Max. Only used for the axis
	 * labels, which must be enabled seperately. Calls repaint() when finished.
	 * 
	 * @param xMax
	 *            The upper bound of x-values, used for axis labels
	 */
	public void setXMaxCoordinateBounds(double xMax) {
		this.xMax = xMax;

		repaint();
	}

	/**
	 * Specify the coordinate bounds for the Y-range. Only used for the axis
	 * labels, which must be enabled seperately. Calls repaint() when finished.
	 * 
	 * @param yMin
	 *            The lower bound of y-values, used for axis labels
	 * @param yMax
	 *            The upper bound of y-values, used for axis labels
	 */
	public void setYCoordinateBounds(double yMin, double yMax) {
		this.yMin = yMin;
		this.yMax = yMax;

		repaint();
	}

	/**
	 * Specify the coordinate bounds for the Y Min. Only used for the axis
	 * labels, which must be enabled seperately. Calls repaint() when finished.
	 * 
	 * @param yMin
	 *            The lower bound of Y-values, used for axis labels
	 */
	public void setYMinCoordinateBounds(double yMin) {
		this.yMin = yMin;

		repaint();
	}

	/**
	 * Specify the coordinate bounds for the Y Max. Only used for the axis
	 * labels, which must be enabled seperately. Calls repaint() when finished.
	 * 
	 * @param yMax
	 *            The upper bound of y-values, used for axis labels
	 */
	public void setYMaxCoordinateBounds(double yMax) {
		this.yMax = yMax;

		repaint();
	}

	/**
	 * Updates the title. Calls repaint() when finished.
	 * 
	 * @param title
	 *            The new title
	 */
	public void setTitle(String title) {
		this.title = title;

		repaint();
	}

	/**
	 * Updates the state of the title. Calls repaint() when finished.
	 * 
	 * @param drawTitle
	 *            Specifies if the title should be drawn
	 */
	public void setDrawTitle(boolean drawTitle) {
		this.drawTitle = drawTitle;

		repaint();
	}

	/**
	 * Updates the X-Axis title. Calls repaint() when finished.
	 * 
	 * @param xAxisTitle
	 *            The new X-Axis title
	 */
	public void setXAxisTitle(String xAxisTitle) {
		this.xAxis = xAxisTitle;

		repaint();
	}

	/**
	 * Updates the state of the X-Axis Title. Calls repaint() when finished.
	 * 
	 * @param drawXAxisTitle
	 *            Specifies if the X-Axis title should be drawn
	 */
	public void setDrawXAxisTitle(boolean drawXAxisTitle) {
		this.drawXTitle = drawXAxisTitle;

		repaint();
	}

	/**
	 * Updates the Y-Axis title. Calls repaint() when finished.
	 * 
	 * @param yAxisTitle
	 *            The new Y-Axis title
	 */
	public void setYAxisTitle(String yAxisTitle) {
		this.yAxis = yAxisTitle;

		repaint();
	}

	/**
	 * Updates the state of the Y-Axis Title. Calls repaint() when finished.
	 * 
	 * @param drawYAxisTitle
	 *            Specifies if the Y-Axis title should be drawn
	 */
	public void setDrawYAxisTitle(boolean drawYAxisTitle) {
		this.drawYTitle = drawYAxisTitle;

		repaint();
	}

	/**
	 * Updates the state of the legend. Calls repaint() when finished.
	 * 
	 * @param drawLegend
	 *            Specifies if the legend should be drawn
	 */
	public void setDrawLegend(boolean drawLegend) {
		this.drawLegend = drawLegend;

		repaint();
	}

	/**
	 * Updates the state of the X-Axis ticks. Calls repaint() when finished.
	 * 
	 * @param drawXTicks
	 *            Specifies if the X-Axis ticks should be drawn
	 */
	public void setDrawXTicks(boolean drawXTicks) {
		this.drawXTicks = drawXTicks;

		repaint();
	}

	/**
	 * Updates the state of the Y-Axis ticks. Calls repaint() when finished.
	 * 
	 * @param drawYTicks
	 *            Specifies if the Y-Axis ticks should be drawn
	 */
	public void setDrawYTicks(boolean drawYTicks) {
		this.drawYTicks = drawYTicks;

		repaint();
	}

	/**
	 * Updates the foreground color. Calls repaint() when finished.
	 * 
	 * @param fg
	 *            Specifies the desired foreground color
	 */
	public void setColorForeground(Color fg) {
		this.fg = fg;

		repaint();
	}

	/**
	 * Updates the background color. Calls repaint() when finished.
	 * 
	 * @param bg
	 *            Specifies the desired background color
	 */
	public void setColorBackground(Color bg) {
		this.bg = bg;

		repaint();
	}

	/**
	 * Updates the gradient used to display the data. Calls drawData() and
	 * repaint() when finished.
	 * 
	 * @param colors
	 *            A variable of type Color[]
	 */
	public void updateGradient(Color[] colors) {
		this.colors = (Color[]) colors.clone();

		if (data != null) {
			updateDataColors();

			drawData();

			repaint();
		}
	}

	/**
	 * This uses the current array of colors that make up the gradient, and
	 * assigns a color index to each data point, stored in the dataColorIndices
	 * array, which is used by the drawData() method to plot the points.
	 */
	private void updateDataColors() {
		// We need to find the range of the data values,
		// in order to assign proper colors.
		double largest = Double.MIN_VALUE;
		double smallest = Double.MAX_VALUE;
		for (int x = 0; x < data.length; x++) {
			for (int y = 0; y < data[0].length; y++) {
				if (Double.isNaN(data[x][y])) continue;
				largest = Math.max(data[x][y], largest);
				smallest = Math.min(data[x][y], smallest);
			}
		}
		double range = largest - smallest;

		// dataColorIndices is the same size as the data array
		// It stores an int index into the color array
		dataColorIndices = new int[data.length][data[0].length];

		// assign a Color to each data point
		for (int x = 0; x < data.length; x++) {
			for (int y = 0; y < data[0].length; y++) {
				if (Double.isNaN(data[x][y])) continue;
				double norm = (data[x][y] - smallest) / range; // 0 < norm < 1
				int colorIndex = (int) Math.floor(norm * (colors.length - 1));
				dataColorIndices[x][y] = colorIndex;
			}
		}
	}

	/**
	 * This function generates data that is not vertically-symmetric, which
	 * makes it very useful for testing which type of vertical axis is being
	 * used to plot the data. If the graphics Y-axis is used, then the lowest
	 * values should be displayed at the top of the frame. If the non-graphics
	 * (mathematical coordinate-system) Y-axis is used, then the lowest values
	 * should be displayed at the bottom of the frame.
	 * 
	 * @return double[][] data values of a simple vertical ramp
	 */
	public static double[][] generateRampTestData() {
		double[][] data = new double[50][50];
		for (int x = 0; x < 50; x++) {
			for (int y = 0; y < 50; y++) {
				data[x][y] = y+1;
			}
		}

		return data;
	}
	
	public static double[][] generateGradTestData(int n) {
		double[][] data = new double[n][n];
		for (int x = 0; x < n; x++) {
			for (int y = 0; y < n; y++) {
				data[x][y] = x-y;
			}
		}

		return data;
	}

	/**
	 * This function generates an appropriate data array for display. It uses
	 * the function: z = sin(x)*cos(y). The parameter specifies the number of
	 * data points in each direction, producing a square matrix.
	 * 
	 * @param dimension
	 *            Size of each side of the returned array
	 * @return double[][] calculated values of z = sin(x)*cos(y)
	 */
	public static double[][] generateSinCosData(int dimension) {
		if (dimension % 2 == 0) {
			dimension++; // make it better
		}

		double[][] data = new double[dimension][dimension];
		double sX, sY; // s for 'Scaled'

		for (int x = 0; x < dimension; x++) {
			for (int y = 0; y < dimension; y++) {
				sX = 2 * Math.PI * (x / (double) dimension); // 0 < sX < 2 * Pi
				sY = 2 * Math.PI * (y / (double) dimension); // 0 < sY < 2 * Pi
				data[x][y] = Math.sin(sX) * Math.cos(sY);
			}
		}

		return data;
	}

	/**
	 * This function generates an appropriate data array for display. It uses
	 * the function: z = Math.cos(Math.abs(sX) + Math.abs(sY)). The parameter
	 * specifies the number of data points in each direction, producing a square
	 * matrix.
	 * 
	 * @param dimension
	 *            Size of each side of the returned array
	 * @return double[][] calculated values of z = Math.cos(Math.abs(sX) +
	 *         Math.abs(sY));
	 */
	public static double[][] generatePyramidData(int dimension) {
		if (dimension % 2 == 0) {
			dimension++; // make it better
		}

		double[][] data = new double[dimension][dimension];
		double sX, sY; // s for 'Scaled'

		for (int x = 0; x < dimension; x++) {
			for (int y = 0; y < dimension; y++) {
				sX = 6 * (x / (double) dimension); // 0 < sX < 6
				sY = 6 * (y / (double) dimension); // 0 < sY < 6
				sX = sX - 3; // -3 < sX < 3
				sY = sY - 3; // -3 < sY < 3
				data[x][y] = Math.cos(Math.abs(sX) + Math.abs(sY));
			}
		}

		return data;
	}

	/**
	 * Updates the data display, calls drawData() to do the expensive re-drawing
	 * of the data plot, and then calls repaint().
	 * 
	 * @param data
	 *            The data to display, must be a complete array (non-ragged)
	 * @param useGraphicsYAxis
	 *            If true, the data will be displayed with the y=0 row at the
	 *            top of the screen. If false, the data will be displayed with
	 *            the y=0 row at the bottom of the screen.
	 */
	public void updateData(double[][] data, boolean useGraphicsYAxis) {
		this.data = new double[data.length][data[0].length];
		for (int ix = 0; ix < data.length; ix++) {
			for (int iy = 0; iy < data[0].length; iy++) {
				// we use the graphics Y-axis internally
				if (useGraphicsYAxis) {
					this.data[ix][iy] = data[ix][iy];
				} else {
					this.data[ix][iy] = data[ix][data[0].length - iy - 1];
				}
			}
		}

		updateDataColors();

		drawData();

		repaint();
	}

	/**
	 * Creates a BufferedImage of the actual data plot.
	 * 
	 * After doing some profiling, it was discovered that 90% of the drawing
	 * time was spend drawing the actual data (not on the axes or tick marks).
	 * Since the Graphics2D has a drawImage method that can do scaling, we are
	 * using that instead of scaling it ourselves. We only need to draw the data
	 * into the bufferedImage on startup, or if the data or gradient changes.
	 * This saves us an enormous amount of time. Thanks to Josh Hayes-Sheen
	 * (grey@grevian.org) for the suggestion and initial code to use the
	 * BufferedImage technique.
	 * 
	 * Since the scaling of the data plot will be handled by the drawImage in
	 * paintComponent, we take the easy way out and draw our bufferedImage with
	 * 1 pixel per data point. Too bad there isn't a setPixel method in the
	 * Graphics2D class, it seems a bit silly to fill a rectangle just to set a
	 * single pixel...
	 * 
	 * This function should be called whenever the data or the gradient changes.
	 */
	private void drawData() {
		int w = this.getWidth(); //== 0 ? this.getPreferredSize().width : this.getWidth());
		//int k = Math.min(lim, (int) Math.floor(940.0 / (data.length + 1)));	
		int k = Math.min(9000, (int) Math.floor(1.0 * (w-60) / (1.0 * (data.length + 1))));
		//System.out.println(k+ "  " + w + "   " + this.getHeight());
		if (k < 1) k = 1;
		boolean re3d = (k >= MIN_RECT_SZ);
		bufferedImage = new BufferedImage(k*(data.length + 1), k*(data[0].length + 1), 
				BufferedImage.TYPE_INT_ARGB);
		bufferedGraphics = bufferedImage.createGraphics();		
		
		for (int x = 0; x < data.length; x++) {
			for (int y = 0; y < data[0].length; y++) {
				if (data[x][y] == 0.0)	{	// 	exclude 0 points
					bufferedGraphics.setColor(Color.BLACK);
				} else {
				bufferedGraphics.setColor(colors[dataColorIndices[x][y]]);
				}
				if (re3d) {
					bufferedGraphics.fill3DRect(k*(x + 1), k*(y + 1), k, k, true);
				} else {
					bufferedGraphics.fillRect(k*(x + 1), k*(y + 1), k, k);
				}
			}
		}
		
		for (int y = 0; y < data[0].length; y++) {
			bufferedGraphics.setColor(Color.cyan);
			bufferedGraphics.fill3DRect(0, k*(y + 1), k, k, re3d);
		}
		
		for (int x = 0; x < data.length; x++) {
			bufferedGraphics.setColor(Color.magenta);
			bufferedGraphics.fill3DRect(k*(x + 1), 0, k, k, re3d);
		}
		
	}

	/**
	 * The overridden painting method, now optimized to simply draw the data
	 * plot to the screen, letting the drawImage method do the resizing. This
	 * saves an extreme amount of time.
	 */
	public void paintComponent(Graphics g) {
		super.paintComponent(g);
		Graphics2D g2d = (Graphics2D) g;

		int width = this.getWidth();
		int height = this.getHeight();

		this.setOpaque(true);

		// clear the panel
		g2d.setColor(bg);
		g2d.fillRect(0, 0, width, height);

		// draw the heat map
		//if (bufferedImage == null) {
			// Ideally, we only to call drawData in the constructor, or if we
			// change the data or gradients. We include this just to be safe.
			drawData();
		//}

		// The data plot itself is drawn with 1 pixel per data point, and the
		// drawImage method scales that up to fit our current window size. This
		// is very fast, and is much faster than the previous version, which
		// redrew the data plot each time we had to repaint the screen.
		g2d.drawImage(bufferedImage, 31, 31, width - 30, height - 30, 0, 0,
				bufferedImage.getWidth(), bufferedImage.getHeight(), null);
		//g2d.drawImage(bufferedImage, 31, 31, null);

		// border
		g2d.setColor(fg);
		g2d.draw3DRect(30, 30, width - 60, height - 60, true);

		// title
		if (drawTitle && title != null) {
			g2d.drawString(title, (width / 2) - 4 * title.length(), 20);
		}

		// axis ticks - ticks start even with the bottom left coner, end very
		// close to end of line (might not be right on)
		int numXTicks = (width - 60) / 50;
		int numYTicks = (height - 60) / 50;

		String label = "";
		DecimalFormat df = new DecimalFormat("##.##");

		// Y-Axis ticks
		if (drawYTicks) {
			int yDist = (int) ((height - 60) / (double) numYTicks); // distance
			// between
			// ticks
			for (int y = 0; y <= numYTicks; y++) {
				g2d.drawLine(26, height - 30 - y * yDist, 30, height - 30 - y
						* yDist);
				label = df.format(((y / (double) numYTicks) * (yMax - yMin))
						+ yMin);
				int labelY = height - 30 - y * yDist - 4 * label.length();
				// to get the text to fit nicely, we need to rotate the graphics
				g2d.rotate(Math.PI / 2);
				g2d.drawString(label, labelY, -14);
				g2d.rotate(-Math.PI / 2);
			}
		}

		// Y-Axis title
		if (drawYTitle && yAxis != null) {
			// to get the text to fit nicely, we need to rotate the graphics
			g2d.rotate(Math.PI / 2);
			g2d.drawString(yAxis, (height / 2) - 4 * yAxis.length(), -3);
			g2d.rotate(-Math.PI / 2);
		}

		// X-Axis ticks
		if (drawXTicks) {
			int xDist = (int) ((width - 60) / (double) numXTicks); // distance
			// between
			// ticks
			for (int x = 0; x <= numXTicks; x++) {
				g2d.drawLine(30 + x * xDist, height - 30, 30 + x * xDist,
						height - 26);
				label = df.format(((x / (double) numXTicks) * (xMax - xMin))
						+ xMin);
				int labelX = (31 + x * xDist) - 4 * label.length();
				g2d.drawString(label, labelX, height - 14);
			}
		}

		// X-Axis title
		if (drawXTitle && xAxis != null) {
			g2d.drawString(xAxis, (width / 2) - 4 * xAxis.length(), height - 3);
		}

		// Legend
		if (!drawLegend) {
			g2d.drawRect(width - 20, 30, 10, height - 60);
			for (int y = 0; y < height - 61; y++) {
				int yStart = height
						- 31
						- (int) Math.ceil(y
								* ((height - 60) / (colors.length * 1.0)));
				yStart = height - 31 - y;
				g2d.setColor(colors[(int) ((y / (double) (height - 60)) * (colors.length * 1.0))]);
				g2d.fillRect(width - 19, yStart, 9, 1);
			}
		}
		
//		g2d.drawRect(20, 30, 60, height - 60);
//		for (int y = 0; y < height - 61; y++) {
//			int yStart = height
//					- 31
//					- (int) Math.ceil(y
//							* ((height - 60) / (colors.length * 1.0)));
//			yStart = height - 31 - y;
//			g2d.setColor(colors[(int) ((y / (double) (height - 60)) * (colors.length * 1.0))]);
//			g2d.fillRect(width - 19, yStart, 9, 1);
//		}
	}
	
	public JToolTip createToolTip() {
		ToolTipManager.sharedInstance().setReshowDelay(0);
		ToolTipManager.sharedInstance().setInitialDelay(0);
	      return molToolTip;
	    }
	
	@Override
	public String getToolTipText(MouseEvent e) {
		Point p = e.getPoint();
		Dimension d = this.getSize();
		int w = d.width - 61;	// remove borders
		int h = d.height - 61;
		double scaleX = w * 1.0 / (data.length + 1.0);
		double scaleY = h * 1.0 / (data.length + 1.0);
		int mouseX = (int) Math.floor((p.getX() - 31.0) / scaleX);
		int mouseY = (int) Math.floor((p.getY() - 31.0) / scaleY);
		try {
			tdp1.clear();
		} catch (Exception e2) {
			// TODO Auto-generated catch block
			e2.printStackTrace();
		}
		ToolTipManager.sharedInstance().setEnabled(false);

		if (mouseX < 0 || mouseX > data.length || mouseY <= 0 || mouseY > data[0].length ) {
			return "";
		}
		
		if (mouseX == 0) {
			IAtomContainer mol1 = molArray[mouseY - 1].getMol();
			ToolTipManager.sharedInstance().setEnabled(true);
			try {
				tdp1.setMol(mol1);
			} catch (Exception e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
			return "";
		} else if (mouseX != 0 && data[mouseX - 1][mouseY - 1] != 0.0) {
			ToolTipManager.sharedInstance().setEnabled(true);
			return DIG2_FORMAT.format((data[mouseX - 1][mouseY - 1]));
		} 
		return "";
	}

	
	public class MouseListener extends MouseInputAdapter {
		private HeatMap heat;
		public MouseListener(HeatMap heat) {
			this.heat = heat;
		}
		public void mouseClicked(MouseEvent e) {
		}
		
		public void mouseMoved(MouseEvent e) {
			try {
				heat.disp.clear();
			} catch (Exception e2) {
				// TODO Auto-generated catch block
				e2.printStackTrace();
			}
			Point p = e.getPoint();
			Dimension d = heat.getSize();
			int w = d.width - 61;	// remove borders
			int h = d.height - 61;
			double scaleX = w * 1.0 / (heat.data.length + 1.0);
			double scaleY = h * 1.0 / (heat.data.length + 1.0);
			int mouseX = (int) Math.floor((p.getX() - 31.0) / scaleX);
			int mouseY = (int) Math.floor((p.getY() - 31.0) / scaleY);
			if (mouseX <= 0 || mouseX > heat.data.length || mouseY <= 0 || 
					mouseY > heat.data[0].length ) {
				try {
					heat.disp.clear();
				} catch (Exception e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}
				return;
			}
			Molecule query = heat.molArray[mouseY - 1];
			Molecule target = heat.molArray[mouseX - 1];
			SMSDpair pair = (SMSDpair) heat.MCSarray[mouseY - 1][mouseX - 1]; // if i>j invert
			
			if (pair == null) {
				try {
					heat.disp.clear();
				} catch (Exception e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}
				return;
			}
			
	
			
			Collection<Integer> queryHi;
			Collection<Integer> targetHi;
			if (mouseX > mouseY) {
				queryHi = pair.queryHi();
				targetHi = pair.targetHi();
			} else {
				queryHi = pair.targetHi();
				targetHi = pair.queryHi();
			}
			
			//System.out.println(pair.queryHi());
			//System.out.println(query.getMol().hashCode());
			//System.out.println(pair.targetHi());
			//System.out.println(target.getMol().hashCode());
			
			try {
				heat.disp.set(query, target, queryHi, targetHi);
			} catch (Exception e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
		}	
	}
	
	public Molecule[] getMolArray() {
		return molArray;
	}


	public DoubleMatrix2D getMatrix() {
		return matrix;
	}


	public SideDisplay getDisp() {
		return disp;
	}
	
	public double[][] getData() {
		return data;
	}
	
	public Object[][] getMCSArr() {
		return MCSarray;
	}


	public static void main(String[] args) throws Exception {
		HeatMap heat = new HeatMap(HeatMap.generateGradTestData(2),//adm.getConnMatrix().toArray(),
				true, Gradient.GRADIENT_RED_TO_GREEN);
		final JFrame f = new JFrame("Heatmap");
		f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		f.setSize(1200, 1000);
		//f.setLayout(new GridLayout(0, 4));
		//heat.setPreferredSize(new Dimension(1000, 1000));
		//heat.createToolTip();
		heat.setToolTipText(TOOL_TIP_TEXT_KEY);
		f.getContentPane().add(heat);

		javax.swing.SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				f.pack();
				f.setVisible(true);
			}
		});
	}

}
