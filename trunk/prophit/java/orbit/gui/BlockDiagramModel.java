package orbit.gui;

import orbit.model.Call;
import orbit.model.CallGraph;
import orbit.model.InclusiveTimeFilter;
import orbit.util.Log;

import org.apache.log4j.Category;

import java.beans.PropertyChangeSupport;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class BlockDiagramModel
{
	public static final Category LOG = Category.getInstance(BlockDiagramModel.class);

	/** The call which is rendered at the base of the tower diagram */
	public static final String RENDER_CALL_PROPERTY = "renderCall";
	/** Maximum stack depth which will be rendered */
	public static final String NUM_LEVELS_PROPERTY = "numLevels";
	/** Whether the user has clicked the Wireframe checkbox */
	public static final String WIREFRAME_PROPERTY = "wireFrame";
	
	/** The call which has been selected by the user */
	public static final String SELECTED_CALL_PROPERTY = "selectedCall";
	/** Search string entered in the search bar */
	public static final String NAME_SEARCH_STRING_PROPERTY = "nameSearchString";
	/** Call which the mouse is hovering over */
	public static final String MOUSEOVER_CALL_PROPERTY = "mouseOverCall";

	/** X-offset of the tower diagram */
	public static final String SHIFT_HORIZONTAL_PROPERTY = "shiftHorizontal";
	/** Y-offset of the tower diagram */
	public static final String SHIFT_VERTICAL_PROPERTY = "shiftVertical";
	/** Location of the 'eye' (viewpoint) in world coordinates */
	public static final String EYE_POSITION_PROPERTY = "eyePosition";

	/** Default stack depth to show when a profile is loaded */
	public static int DEFAULT_LEVELS = 5;

	/** Starting location of EYE_POSITION_PROPERTY */
	private static EyeLocation DEFAULT_EYE_LOCATION = new EyeLocation(2.5, -Math.PI / 4, Math.PI / 4);
	/** Increment to move the SHIFT_HORIZONTAL_PROPERTY and SHIFT_VERTICAL_PROPERTY */
	private static double SHIFT_STEP = 0.05;

	private final CallGraph cg;

	private RootRenderState rootState;

	private PropertyChangeSupport changeSupport;

	private Map glNameToCallMap = null;
	private Map nameToCallListMap = null;

	private int levels = DEFAULT_LEVELS;
	private LevelOfDetail lod = LevelOfDetail.Standard;
	private boolean wireframe = false;
	private double shiftVertical = 0;
	private double shiftHorizontal = 0;
	private EyeLocation eyeLocation;
	private Call rootCall = null;
	private Call mouseOverCall = null;
	private Call selectedCall = null;
	private String searchString = null;
	private ArrayList nameSearchMatches = new ArrayList();
	
	public BlockDiagramModel(CallGraph cg)
	{
		this.eyeLocation = (EyeLocation)DEFAULT_EYE_LOCATION.clone();

		this.changeSupport = new PropertyChangeSupport(this);
		this.cg = cg;
		this.rootState = new RootRenderState(new RootRenderState.Listener()
			{
				public void renderCallChanged(Call oldCall, Call newCall)
				{
					invalidateDiagram();
					changeSupport.firePropertyChange(RENDER_CALL_PROPERTY, oldCall, newCall);
				}
			}, 
			getRootCall());
	}

	/**
	 * Clean up all references
	 */
	public void dispose()
	{
		rootState = null;
		eyeLocation = null;
		mouseOverCall = null;
		selectedCall = null;
	}
	
	public synchronized void addListener(PropertyChangeListener listener)
	{
		changeSupport.addPropertyChangeListener(listener);
	}
	
	public synchronized void removeListener(PropertyChangeListener listener)
	{
		changeSupport.removePropertyChangeListener(listener);
	}

	public Call getRootCall()
	{
		return cg.getRoot().filter(createFilter());
	}

	public RootRenderState getRootRenderState()
	{
		return rootState;
	}
	
	public void setSelectedCall(Call call)
	{
		Call oldSelectedCall = selectedCall;
		if ( call != oldSelectedCall )
		{
			selectedCall = call;
			changeSupport.firePropertyChange(SELECTED_CALL_PROPERTY, oldSelectedCall, selectedCall);
		}
	}

	/**
	 * Get the selected call. Secondary selected calls may be obtained from {@link #getAllSelectedCalls}.
	 */
	public Call getSelectedCall()
	{
		return selectedCall;
	}

	/**
	 * Get all selected calls. The user-selected call is first in the list. The secondary selected calls
	 * make up the remainder of the list.
	 */
	public List getAllSelectedCalls()
	{
		ArrayList list = new ArrayList();
		if ( selectedCall != null )
		{
			list.add(selectedCall);
			for ( Iterator i = getCallsByName(selectedCall.getName()).iterator(); i.hasNext(); )
			{
				Call call = (Call)i.next();
				list.add(call);
			}
		}
		return list;
	}

	/**
	 * Set the search string that the user typed in to the name-search. This search
	 * string is used to construct a list of matching names.
	 *
	 * @see #getNameSearchNames
	 * @see #getCallsByName
	 */
	public void setNameSearchString(String newSearchString)
	{
		if ( newSearchString == null )
			newSearchString = "";
		
		if ( !newSearchString.equals(searchString) )
		{
			String oldSearchString = searchString;
			searchString = newSearchString;
			WildcardMatchExpression expr = new WildcardMatchExpression(searchString);
			nameSearchMatches.clear();
			Set keySet = nameToCallListMap.keySet();
			Log.debug(LOG, "Searching for ", searchString, " in ", keySet);
			for ( Iterator i = keySet.iterator(); i.hasNext(); )
			{
				String name = (String)i.next();
				if ( expr.match(name) )
				{
					Log.debug(LOG, "Search matches ", name);
					nameSearchMatches.add(name);
				}
			}
			changeSupport.firePropertyChange(NAME_SEARCH_STRING_PROPERTY, oldSearchString, searchString);
		}
	}

	/**
	 * Get a list of all calls which matched the {@link #setNameSearchString search string}.
	 */
	public List getAllSearchResultCalls()
	{
		List list = new ArrayList();

		for ( Iterator i = getNameSearchNames().iterator(); i.hasNext(); )
		{
			String name = (String)i.next();
			for ( Iterator j = getCallsByName(name).iterator(); j.hasNext(); )
			{
				Call call = (Call)j.next();
				list.add(call);
			}
		}
		return list;
	}
	
	public void setMouseOverCall(Call call)
	{
		Call oldCall = mouseOverCall;
		if ( call != oldCall )
		{
			mouseOverCall = call;
			changeSupport.firePropertyChange(MOUSEOVER_CALL_PROPERTY, oldCall, mouseOverCall);
		}
	}

	public Call getMouseOverCall()
	{
		return mouseOverCall;
	}

	/**
	 * @param map a map from OpenGL 'names' (integers) to Calls. This map
	 * can be used to identify a Call when it is selected in the diagram.
	 */
	public void setGLNameToCallMap(Map map)
	{
		Log.debug(LOG, "Set glNameToCallMap = ", map);
		glNameToCallMap = map;
	}

	public Call getCallByGLName(int name)
	{
		Call mouseOverCall = null;
		if ( glNameToCallMap != null )
			mouseOverCall = (Call)glNameToCallMap.get(new Integer(name));
		else
			Log.debug(LOG, "No glNameToCallMap in BlockDiagramModel.getCallByGLName");
		return mouseOverCall;
	}

	/**
	 * @param map a map from call names to Lists of Calls. 
	 */
	public void setNameToCallListMap(Map map)
	{
		nameToCallListMap = map;
	}

	public List getCallsByName(String name)
	{
		Log.debug(LOG, "Looking for ", name, " in ", nameToCallListMap);

		List list = null;
		if ( nameToCallListMap != null )
		{
			list = (List)nameToCallListMap.get(name);
			Log.debug(LOG, "Found ", list);
		}
		if ( list == null )
			list = Collections.EMPTY_LIST;

		return list;
	}

	public void moveEye(double yaw, double pitch)
	{
		EyeLocation old = eyeLocation;
		eyeLocation = eyeLocation.move(0, yaw, pitch);
		changeSupport.firePropertyChange(EYE_POSITION_PROPERTY, old, eyeLocation);
	}

	public EyeLocation getEye()
	{
		return eyeLocation;
	}

	public void shiftVertical(boolean up)
	{
		double old = shiftVertical;
		if ( up )
			shiftVertical += SHIFT_STEP;
		else
			shiftVertical -= SHIFT_STEP;
		changeSupport.firePropertyChange(SHIFT_VERTICAL_PROPERTY, new Double(old), new Double(shiftVertical));
	}

	public double getShiftVertical()
	{
		return shiftVertical;
	}

	public void shiftHorizontal(boolean right)
	{
		double old = shiftHorizontal;
		if ( right )
			shiftHorizontal += SHIFT_STEP;
		else
			shiftHorizontal -= SHIFT_STEP;
		changeSupport.firePropertyChange(SHIFT_HORIZONTAL_PROPERTY, new Double(old), new Double(shiftHorizontal));
	}

	public double getShiftHorizontal()
	{
		return shiftHorizontal;
	}

	public void setWireframe(boolean b)
	{
		boolean old = this.wireframe;
		if ( b != old )
		{
			this.wireframe = b;
			changeSupport.firePropertyChange(WIREFRAME_PROPERTY, old, b);
		}
	}

	public boolean isWireframe()
	{
		return wireframe;
	}
	
	public void setLevels(int levels)
	{
		int old = this.levels;
		if ( levels != old )
		{
			this.levels = levels;
			invalidateDiagram();
			changeSupport.firePropertyChange(NUM_LEVELS_PROPERTY, old, levels);
		}
	}

	public int getLevels()
	{
		return levels;
	}
	
	public void addLevel()
	{
		setLevels(levels + 1);
	}

	public void removeLevel()
	{
		if ( levels > 0 )
			setLevels(levels - 1);
	}

	public LevelOfDetail getLevelOfDetail()
	{
		return lod;
	}

	public void setLevelOfDetail(LevelOfDetail newLOD)
	{
		LevelOfDetail old = lod;
		if ( lod != newLOD )
		{
			lod = newLOD;
			invalidateDiagram();
			rootState.setRenderCall(rootState.getRenderCall().filter(createFilter()));
		}
	}

	/**
	 * @return a list of names which match the user's search string. Initially an empty list.
	 */
	private List getNameSearchNames()
	{
		return Collections.unmodifiableList(nameSearchMatches);
	}

	private Call.Filter createFilter()
	{
		return new InclusiveTimeFilter(lod.getThreshold());
	}
	
	/**
	 * Call this method when the set of blocks in the diagram has been changed.
	 */
	private void invalidateDiagram()
	{
		glNameToCallMap = null;
		nameToCallListMap = null;
	}
}
