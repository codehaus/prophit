package orbit.gui;

import orbit.model.*;
import orbit.parsers.*;
import orbit.gui.CallDetails;

import junit.framework.TestCase;

import java.io.File;

/*
 Uses the test data file:

 test/data/profiles/SelfCallerHierarchy.hprof.txt

 Aside from the URLClassLoader, the CallGraph is:

<root> 221.0 [-1->0]
  bugs.SelfCallerHierarchy.firstLoop(SelfCallerHierarchy.java) 124.0 [0->9]
    bugs.SumContainer.add(SelfCallerHierarchy.java) 112.0 [9->37]
      bugs.SumContainer.addChildren(SelfCallerHierarchy.java) 112.0 [37->4]
        java.util.ArrayList.get(ArrayList.java) 22.0 [4->5]
          java.util.ArrayList.RangeCheck(ArrayList.java) 9.0 [5->7]
        bugs.SumContainer.add(SelfCallerHierarchy.java) 61.0 [4->12]
          bugs.SumContainer.addChildren(SelfCallerHierarchy.java) 56.0 [12->2]
            java.util.ArrayList.get(ArrayList.java) 21.0 [2->10]
              java.util.ArrayList.RangeCheck(ArrayList.java) 14.0 [10->46]
            bugs.SimpleSum.add(SelfCallerHierarchy.java) 2.0 [2->17]
            java.util.ArrayList.size(ArrayList.java) 1.0 [2->19]
        bugs.SimpleSum.add(SelfCallerHierarchy.java) 1.0 [4->24]
  bugs.SelfCallerHierarchy.secondLoop(SelfCallerHierarchy.java) 93.0 [0->16]
    bugs.SumContainer.add(SelfCallerHierarchy.java) 90.0 [16->38]
      bugs.SumContainer.addChildren(SelfCallerHierarchy.java) 90.0 [38->8]
        bugs.SimpleSum.add(SelfCallerHierarchy.java) 1.0 [8->23]
        bugs.SumContainer.add(SelfCallerHierarchy.java) 73.0 [8->28]
          bugs.SumContainer.addChildren(SelfCallerHierarchy.java) 72.0 [28->6]
            java.util.ArrayList.get(ArrayList.java) 15.0 [6->11]
              java.util.ArrayList.RangeCheck(ArrayList.java) 10.0 [11->47]
            bugs.SimpleSum.add(SelfCallerHierarchy.java) 1.0 [6->25]
            bugs.SumContainer.add(SelfCallerHierarchy.java) 41.0 [6->27]
              bugs.SumContainer.addChildren(SelfCallerHierarchy.java) 40.0 [27->3]
                java.util.ArrayList.get(ArrayList.java) 9.0 [3->14]
                  java.util.ArrayList.RangeCheck(ArrayList.java) 6.0 [14->48]
                bugs.SimpleSum.add(SelfCallerHierarchy.java) 2.0 [3->15]
        java.util.ArrayList.get(ArrayList.java) 5.0 [8->29]
          java.util.ArrayList.RangeCheck(ArrayList.java) 4.0 [29->13]
*/

/**
 * See the description of the bug in test/java/bugs/SelfCallerHierarchy.java
 */
public class TestRecursiveCallDetails
	extends TestCase
{
	CallGraph cg;	

	public TestRecursiveCallDetails(String name)
	{
		super(name);
	}
	
	public void setUp() throws Exception
	{
		Loader loader = LoaderFactory.instance().createLoader(new File("test/data/profiles/SelfCallerHierarchy.hprof.txt"));
		loader.parse();
		cg = loader.solve();
	}

	public void testInclusiveTime() throws Exception
	{
		Call root = (Call)cg.getRoot().getChildren().get(0);
		Call SumContainer$add = (Call)root.getChildren().get(0);

		String callName = "bugs.SumContainer.add(SelfCallerHierarchy.java)";
		assertEquals(callName, SumContainer$add.getName());

		CallDetails cd = new CallDetails(cg.getRoot(), SumContainer$add);

		assertEquals(221.0, cd.getRootTime(), 0.001);
		assertEquals(202.0, cd.getInclusiveTime(), 0.001);
		assertEquals(7.0, cd.getExclusiveTime(), 0.001);

		assertEquals("totalTime : 336.0, timeByCallName : {bugs.SelfCallerHierarchy.secondLoop(SelfCallerHierarchy.java)=90.0, bugs.SumContainer.addChildren(SelfCallerHierarchy.java)=134.0, bugs.SelfCallerHierarchy.firstLoop(SelfCallerHierarchy.java)=112.0}", cd.getCallersRollupList().toString());
		assertEquals("totalTime : 202.0, timeByCallName : {bugs.SumContainer.addChildren(SelfCallerHierarchy.java)=202.0}", cd.getCalleesRollupList().toString());
	}	
}
