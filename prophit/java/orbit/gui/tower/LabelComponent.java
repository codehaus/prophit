package orbit.gui.tower;

import orbit.gui.*;
import orbit.model.Call;
import orbit.util.Log;

import org.apache.log4j.Category;

import gl4java.GLEnum;
import gl4java.GLFunc;
import gl4java.GLUFunc;
import gl4java.utils.glut.GLUTEnum;
import gl4java.utils.glut.GLUTFunc;
import gl4java.utils.glut.fonts.GLUTFuncLightImplWithFonts;

import java.awt.Component;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.*;
import java.awt.geom.Rectangle2D;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.*;

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

	private List callHolderList = new ArrayList();

	public void initialize(Component canvas, GLFunc gl, GLUFunc glu)
	{
		super.initialize(canvas, gl, glu);

		canvas.addComponentListener(new ComponentAdapter()
			{
				public void componentResized(ComponentEvent e)
				{
					invalidate();
				}				
			});
		
		canvas.addMouseListener(new MouseAdapter()
			{
				public void mouseClicked(MouseEvent e)
				{
					// System.out.println(e);
					if ( e.getModifiers() == MouseEvent.BUTTON1_MASK )
					{
						CallHolder ch = getCallFromPoint(e.getPoint());
						if ( ch != null )
						{
							Call selectedCall = ch.call.getCall();
							Log.debug(LOG, "Mouse selected call ", selectedCall);
							if ( e.getClickCount() == 2 )
							{
								model.getRootRenderState().setRenderCall(selectedCall);
							}
							else
							{
								model.setSelectedCall(selectedCall);
							}
						}
					}
				}
			});
		
		canvas.addMouseMotionListener(new MouseMotionAdapter()
			{
				public void mouseMoved(MouseEvent e)
				{
					CallHolder ch = getCallFromPoint(e.getPoint());
					if ( ch != null )
					{
						model.setMouseOverCall(ch.call.getCall());
					}
				}
			});
	}

	protected void addListeners()
	{
		this.model.addListener(new PropertyChangeListener()
			{
				public void propertyChange(PropertyChangeEvent evt)
				{
					if ( BlockDiagramModel.RENDER_CALL_PROPERTY.equals(evt.getPropertyName()) ||
						 BlockDiagramModel.MOUSEOVER_CALL_PROPERTY.equals(evt.getPropertyName()) ||
						 BlockDiagramModel.SELECTED_CALL_PROPERTY.equals(evt.getPropertyName()) ||
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
	
	public synchronized void paintComponent()
	{
		Rectangle2D.Double rootRectangle = new Rectangle2D.Double(0, 0, Constants.DIAGRAM_EXTENT, Constants.DIAGRAM_EXTENT);
		CallLayoutAlgorithm layout = new CallLayoutAlgorithm(new CallAdapter(model.getRootRenderState().getRenderCall()),
															 TimeMeasure.TotalTime,
															 model.getLevels(),
															 rootRectangle);

		LabelSearch search = new LabelSearch();
		layout.setCallback(search);
		layout.execute();

		List unsortedCalls = search.getList();

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

		callHolderList = new LineInterceptSearch().execute(unsortedCalls);
	
		// TODO: standardize the construction of this
		GLUTFunc glut = new GLUTFuncLightImplWithFonts(gl, glu);
		int index = 0;
		for ( Iterator i = callHolderList.iterator(); i.hasNext(); ++index )
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
			int textX = (int)( labelX + index * labelDX );
			int textY = (int)( labelY + index * labelDY );
			
			String labelText = UIUtil.getShortName(holder.call.getName()) + " [ " + UIUtil.formatPercent( holder.call.getInclusiveTime(TimeMeasure.TotalTime) / model.getRootRenderState().getRenderCall().getTime() ) + " ] ";
			
			holder.setLabelRectangle(new Rectangle(textX,
												   (int)( viewport[3] - textY - FONT_HEIGHT + TEXT_BORDER ),
												   glut.glutBitmapLength(GLUTEnum.GLUT_BITMAP_HELVETICA_12, labelText),
												   FONT_HEIGHT));

			gl.glPushAttrib(GLEnum.GL_LINE_BIT);
			gl.glPushAttrib(GLEnum.GL_COLOR_BUFFER_BIT);
			try
			{
				if ( model.getMouseOverCall() != null &&
						  model.getMouseOverCall().equals(holder.call.getCall()) )
				{
					gl.glLineWidth(3);
				}
				else if ( model.getSelectedCall() != null &&
					 model.getSelectedCall().equals(holder.call.getCall()) )
				{
					GLUtils.glColor(gl, colorModel.getSelectedCallColor());
					gl.glLineWidth(2);
				}
				else
				{
					gl.glLineWidth(1);
				}
			
				gl.glBegin(GLEnum.GL_LINES);
				gl.glVertex3d(worldCoordinates[0], worldCoordinates[1], worldCoordinates[2]);
				gl.glVertex3d(rect.x + rect.width / 2, rect.y + rect.height / 2, topZ);
				gl.glEnd();

				textBegin();
				gl.glTranslated(textX,
								textY,
								0);
				GLUtils.drawText(gl,
								 glut,
								 0,
								 0,
								 labelText,
								 colorModel.getTextColor());

				textEnd();
			}
			finally
			{
				gl.glPopAttrib();
				gl.glPopAttrib();
			}			
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

	private CallHolder getCallFromPoint(Point point)
	{
		for ( Iterator i = callHolderList.iterator(); i.hasNext(); )
		{
			CallHolder ch = (CallHolder)i.next();
			if ( ch.getLabelRectangle() != null &&
				 ch.getLabelRectangle().contains(point) )
			{
				return ch;		
			}
		}
		return null;
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
		private Rectangle labelRectangle = null;
		
		public CallHolder(CallAdapter call, Rectangle2D.Double rectangle, int depth)
		{
			this.call = call;
			this.rectangle = rectangle;
			this.depth = depth;
		}

		public void setLabelRectangle(Rectangle rect)
		{
			this.labelRectangle = rect;
		}
		
		public Rectangle getLabelRectangle()
		{
			return labelRectangle;
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


