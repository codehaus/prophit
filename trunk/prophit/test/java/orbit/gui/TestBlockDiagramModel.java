package orbit.gui;

import orbit.gui.tower.NameListAlgorithm;
import orbit.model.CallGraph;
import orbit.parsers.Loader;
import orbit.parsers.LoaderFactory;
import orbit.parsers.ParseException;

import junit.framework.TestCase;

import java.io.File;
import java.util.List;

/**
 * Test the BlockDiagramModel by constructing a BlockDiagramModel and manually
 * exercising its methods.
 */
public class TestBlockDiagramModel
	extends TestCase
{
	public TestBlockDiagramModel(String name)
	{
		super(name);
	}

	/**
	 * Test searching for the <root> node of the hsqldb profile.
	 * This is a test for a bug that actually turned out to be in WildcardMatchExpression
	 */
	public void testSearchForRoot() throws ParseException
	{
		Loader loader = LoaderFactory.instance().createLoader(new File(System.getProperty("basedir") + "/data/hsqldb.hprof.txt"));
		loader.parse();
		CallGraph cg = loader.solve();
		
		BlockDiagramModel model = new BlockDiagramModel(cg);
		NameListAlgorithm nla = new NameListAlgorithm();
		nla.setModel(model);
		nla.execute();
		
		assertEquals(model.getRootCall().getName(), "<root>");

		// Test searching for <root> and *<root>*
		{
			model.setNameSearchString("<root>");
			List names = model.getNameSearchNames();
			assertEquals(1, names.size());
			assertEquals(model.getRootCall().getName(), names.get(0));
		}
		{
			model.setNameSearchString("*<root>*");
			List names = model.getNameSearchNames();
			assertEquals(1, names.size());
			assertEquals(model.getRootCall().getName(), names.get(0));
		}
	}
}
