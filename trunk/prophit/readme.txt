Installation:

Unzip the distribution
Set your JAVA_HOME to point to the Java into which you installed GL4Java (e.g. c:\java\jdk1.3)
	For GL4Java to work, the application must actually be run with JAVA_HOME\jre\bin\java, not JAVA_HOME\bin\java.
	The run.bat script takes care of this.
If you are using the 4NT shell, set the environment variable 'FOURNT_SHELL' to 'true' (or anything non-empty)
Run run.bat
Open one of the profiles in the data\ directory

To profile something else, run java with the '-prof' option. A fun thing to do would be to profile the profiler itself... Then you can load up the java.prof file that 'java -prof' dumps out when the process ends (or when you hit 'Control-Break').

Each block represents a function call. The blue-er a block is, the less time was spent in that function (not counting children). The redder it is, the more of a hotspot that function is.

Known bugs:
Sometimes the blocks are rendered 'outside the lines'
If you maximize the window, the bottom of the screen doesn't get used

Please report any other issues.
