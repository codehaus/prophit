package orbit.gui.tower;

import orbit.gui.BlockDiagramModel;
import orbit.gui.Constants;
import orbit.gui.CallAdapter;
import orbit.gui.CallLayoutAlgorithm;
import orbit.gui.Constants;
import orbit.gui.GLUtils;
import orbit.gui.TimeMeasure;
import orbit.gui.UIUtil;
import orbit.model.Call;
import orbit.util.Log;

import gl4java.GLEnum;
import gl4java.utils.glut.GLUTFunc;
import gl4java.utils.glut.fonts.GLUTFuncLightImplWithFonts;

import org.apache.log4j.Category;

import java.awt.geom.Rectangle2D;
import java.awt.event.ComponentEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.LinkedList;

/**
 * Labels a set of blocks by drawing an arrow from the labeling text to the center of the block.
 */
public class LabelComponent
	extends AbstractDisplayListComponent
{
	private static final int NUM_LABELS = 5;

	// Each label is spaced in the X-direction from the other labels by Window.width / LABEL_X_SPACING
	private static final double LABEL_X_SPACING = 20;
	
	public static Category LOG = Category.getInstance(LabelComponent.class);
	
	/**
	 * Need to invalidate the display list because the mapping from model to window
	 * coordinates implemented by gluProject and gluUnProject will be changed.
	 */
	public void componentResized(ComponentEvent e)
	{
		invalidate();
	}
	
	protected void addListeners()
	{
		this.model.addListener(new PropertyChangeListener()
			{
				public void propertyChange(PropertyChangeEvent evt)
				{
					if ( BlockDiagramModel.RENDER_CALL_PROPERTY.equals(evt.getPropertyName()) ||
						 BlockDiagramModel.NUM_LEVELS_PROPERTY.equals(evt.getPropertyName()) ||
						 BlockDiagramModel.SHIFT_HORIZONTAL_PROPERTY.equals(evt.getPropertyName()) ||
						 BlockDiagramModel.SHIFT_VERTICAL_PROPERTY.equals(evt.getPropertyName()) ||
						 BlockDiagramModel.EYE_POSITION_PROPERTY.equals(evt.getPropertyName()) )
					{
						Log.debug(LOG, "Got PropertyChangeEvent ", evt, ". Invalidating LabelComponent");
						invalidate();
					}
				}
			});
	}
	
	public void paintComponent()
	{
		Rectangle2D.Double rootRectangle = new Rectangle2D.Double(0, 0, Constants.DIAGRAM_EXTENT, Constants.DIAGRAM_EXTENT);
		CallLayoutAlgorithm layout = new CallLayoutAlgorithm(new CallAdapter(model.getRootRenderState().getRenderCall()),
															 TimeMeasure.TotalTime,
															 model.getLevels(),
															 rootRectangle);

		LabelSearch search = new LabelSearch();
		layout.setCallback(search);
		layout.execute();

		List calls = search.getList();

		final int[] viewport = new int[4];
		final double[] mvMatrix = new double[16], projMatrix = new double[16],
			windowCoordinates = new double[3], modelCoordinates = new double[3],
			worldCoordinates = new double[3];
		
		gl.glGetIntegerv(GLEnum.GL_VIEWPORT, viewport);
		gl.glGetDoublev(GLEnum.GL_MODELVIEW_MATRIX, mvMatrix);
		gl.glGetDoublev(GLEnum.GL_PROJECTION_MATRIX, projMatrix);

		final double labelDX = viewport[2] / LABEL_X_SPACING;
		final double labelDY = - ( TEXT_BORDER + FONT_HEIGHT );
		final double labelX = 140;
		final double labelY = viewport[3] + labelDY;

		class LineInterceptCalculator
		{
			public boolean labelLinesIntersect(CallHolder first, CallHolder second, int firstIndex)
			{
				/*
				 * Point-slope form of a line equation is y - y1 = m1(x - x1)
				 * In this case, y1 and x1 are the block coordinates of 'first',
				 *   y2 and x2 are the block coordinates of 'second',
				 * x and y are the coordinates of the line extending from x1,y1 to label1, and
				 *   x2,y2 to label2
				 * Solving for y = y on both lines determines the point of y-intersection of the two lines
				 * If the y-coordinate of the point of intersection is greater than the y-coordinate of the
				 *   labels, then 'first' may be drawn to the left of 'second'. Otherwise it should not because
				 *   the lines will cross in the diagram
				 *
				 * The formula for the y-intersection is:
				 *   y = m1 * ( - y2 / m2 + x2 + y1 / m1 - x1 ) / ( 1 - m1 / m2 )
				 */
				double labelX1 = labelX + firstIndex * labelDX;
				double labelY1 = labelY + firstIndex * labelDY;
				
				double labelX2 = labelX + ( firstIndex + 1 ) * labelDX;
				double labelY2 = labelY + ( firstIndex + 1 ) * labelDY;

				double[] firstBlockWindowCoordinates = computeBlockWindowCoordinates(first);
				double[] secondBlockWindowCoordinates = computeBlockWindowCoordinates(second);
				
				double blockX1 = firstBlockWindowCoordinates[0];
				double blockY1 = firstBlockWindowCoordinates[1];
				
				double blockX2 = secondBlockWindowCoordinates[0];
				double blockY2 = secondBlockWindowCoordinates[1];

				double m1 = Double.MAX_VALUE;
				if ( labelX1 != blockX1 )
					m1 = ( labelY1 - blockY1 ) / ( labelX1 - blockX1 );

				double m2 = Double.MAX_VALUE;
				if ( labelX2 != blockX2 )
					m2 = ( labelY2 - blockY2 ) / ( labelX2 - blockX2 );

				if ( m1 == m2 )
				{
					return false;
				}
				else
				{
					double yIntersection = m1 * ( -blockY2 / m2 + blockX2 + blockY1 / m1 - blockX1 ) / ( 1 - m1 / m2 );
					Log.debug(LOG, first, " intersects ", second, " at ", yIntersection);
					return yIntersection < labelY2 && yIntersection > blockY2;
				}												   
			}

			/*
			 * Computes the angle of the line in radians from <code>ch</code> to the point ( windowX, windowY ).
			 */
			private double[] computeBlockWindowCoordinates(CallHolder ch)
			{
				double[] blockWindowCoordinates = new double[3];

				glu.gluProject(ch.getWorldCoordinates(), mvMatrix, projMatrix, viewport, blockWindowCoordinates);

				return blockWindowCoordinates;
			}
		}

		/*
		 * Determines an ordering of the calls such that no two label lines intersect
		 */
		class LineInterceptSearch
		{
			private final LineInterceptCalculator calculator = new LineInterceptCalculator();
			private List allCalls;
			private List calls;
			private List bestList = null;
			private int maxDepth = 0;
			
			public List execute(List allCalls)
			{
				this.allCalls = allCalls;
				this.calls = new ArrayList(allCalls.size());
				for ( int i = 0; i < allCalls.size(); ++i )
					calls.add(null);
				
				if ( execute(0) )
				{
					return calls;
				}
				else
				{
					LOG.error("Could not find a label ordering on " + allCalls);
					return allCalls;
				}
			}

			private boolean execute(int index)
			{
				for ( int i = 0; i < allCalls.size(); ++i )
				{
					CallHolder ch = (CallHolder)allCalls.get(i);
					if ( !calls.contains(ch) )
					{
						if ( LOG.isDebugEnabled() )
						{
							StringBuffer tabs = new StringBuffer();
							for ( int j = 0; j < index; ++j )
								tabs.append("  ");
							LOG.debug(tabs + "Searching " + index +  " = " + ch);
						}
						calls.set(index, ch);
						if ( index == 0 || !calculator.labelLinesIntersect((CallHolder)calls.get(index - 1),
																		   (CallHolder)calls.get(index),
																		   index - 1) )
						{
							if ( index == allCalls.size() - 1 )
							{
								return true;
							}
							else if ( execute(index + 1) )
							{
								return true;
							}
						}
						calls.set(index, null);
					}
				}
				return false;
			}
		}

		calls = new LineInterceptSearch().execute(calls);
	
		// TODO: standardize the construction of this
		GLUTFunc glut = new GLUTFuncLightImplWithFonts(gl, glu);
		int index = 0;
		for ( Iterator i = calls.iterator(); i.hasNext(); ++index )
		{
			CallHolder holder = (CallHolder)i.next();

			windowCoordinates[0] = labelX + index * labelDX;
			windowCoordinates[1] = labelY + index * labelDY;
								// 1.0 is the far clipping plane
			windowCoordinates[2] = 0.5;

			// System.out.println("Mouse at [ " + windowCoordinates[0] + ", " + windowCoordinates[1] +
			// ", " + windowCoordinates[2] + " ]");
								
			glu.gluUnProject(windowCoordinates[0], windowCoordinates[1], windowCoordinates[2],
							 mvMatrix, projMatrix, viewport, worldCoordinates);
								
			// System.out.println("World : [ " + worldCoordinates[0] + ", " + worldCoordinates[1] +
			// ", " + worldCoordinates[2] + " ]");

			double[] blockCoordinates = holder.getWorldCoordinates();
			Rectangle2D.Double rect = holder.rectangle;
			double topZ = ( holder.depth + 1 ) * Constants.BLOCK_HEIGHT;
						
			gl.glBegin(GLEnum.GL_LINES);
			gl.glVertex3d(worldCoordinates[0], worldCoordinates[1], worldCoordinates[2]);
			gl.glVertex3d(rect.x + rect.width / 2, rect.y + rect.height / 2, topZ);
			gl.glEnd();

			textBegin();
			gl.glTranslated(labelX + index * labelDX,
							labelY + index * labelDY,
							0);
			GLUtils.drawText(gl,
							 glut,
							 0,
							 0,
							 holder.call.getName() + " [ " + UIUtil.formatPercent( holder.call.getInclusiveTime(TimeMeasure.TotalTime) / model.getRootRenderState().getRenderCall().getTime() ) + " ] ",
							 colorModel.getTextColor());
			textEnd();
		}
	}

	// TODO: uncopy these from BlockDiagramView
	private void textBegin()
	{
		int width = canvas.getSize().width;
		int height = canvas.getSize().height;

		gl.glMatrixMode(GLEnum.GL_PROJECTION);
		gl.glPushMatrix();
		gl.glLoadIdentity();
		glu.gluOrtho2D(0, width, 0, height);

		gl.glMatrixMode(GLEnum.GL_MODELVIEW);
		gl.glPushMatrix();
		gl.glLoadIdentity();
		
		gl.glDisable(GLEnum.GL_LIGHTING);
		gl.glEnable(GLEnum.GL_DITHER);
	}

	private void textEnd()
	{
		gl.glPopMatrix();

		gl.glMatrixMode(GLEnum.GL_PROJECTION);
		gl.glPopMatrix();
	}

	private class LabelSearch
		implements CallLayoutAlgorithm.Callback
	{
		private final List list = new LinkedList();
		private CallAdapter call = null;
		private Rectangle2D.Double rectangle = null;
		private int depth = -1;

		/**
		 * @return a List of NUM_LABELS CallHolder objects which have the maximum inclusive time
		 * in the visible call graph. Only calls which have no children are considered.
		 * Returns a copy of the original list.
		 */
		public List getList()
		{
			return new ArrayList(list);
		}
		
		public boolean beginCall(CallAdapter call, Rectangle2D.Double rectangle, int depth)
		{
			this.call = call;
			this.rectangle = rectangle;
			this.depth = depth;

			return true;
		}

		public void endCall(CallAdapter call)
		{
			/*
			 * Only if the call passed to #beginCall had no children will it be non-null,
			 * because the pointer is set to null in each #endCall.
			 */
			if ( this.call != null )
			{
				addCall();
			}
			this.call = null;
			this.rectangle = null;
			this.depth = -1;
		}

		/**
		 * Adds the current call to the list if there are not yet NUM_LABELS calls in the list, or
		 * if its inclusive time is greater than the minimum inclusive time of any call in the list.
		 */
		private void addCall()
		{
			if ( list.size() < NUM_LABELS )
			{
				CallHolder holder = new CallHolder(call, rectangle, depth);
				list.add(holder);
				if ( list.size() == NUM_LABELS )
				{
					Collections.sort(list);
				}
			}
			else
			{
				double minimumTime = ((CallHolder)list.get(0)).call.getInclusiveTime(TimeMeasure.TotalTime);
				double time = call.getInclusiveTime(TimeMeasure.TotalTime);
				if ( time > minimumTime )
				{
					list.remove(0);
					list.add(new CallHolder(call, rectangle, depth));
					Collections.sort(list);
				}
			}
		}
	}

	private static class CallHolder
		implements Comparable
	{
		private final CallAdapter call;
		private final Rectangle2D.Double rectangle;
		private final int depth;
		
		public CallHolder(CallAdapter call, Rectangle2D.Double rectangle, int depth)
		{
			this.call = call;
			this.rectangle = rectangle;
			this.depth = depth;
		}

		public double[] getWorldCoordinates()
		{
			double topZ = ( depth + 1 ) * Constants.BLOCK_HEIGHT;
			return new double[]{ rectangle.x + rectangle.width / 2, rectangle.y + rectangle.height / 2, topZ };
		}
		
		public int compareTo(Object o)
		{
			CallHolder other = (CallHolder)o;
			return (int)( call.getInclusiveTime(TimeMeasure.TotalTime) - other.call.getInclusiveTime(TimeMeasure.TotalTime) );
		}

		public String toString()
		{
			return call.getName() + " [ depth = " + depth + " ]";
		}
	}
}


