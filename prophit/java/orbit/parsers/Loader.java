package orbit.parsers;

import orbit.model.CallGraph;
import orbit.model.ModelBuilder;
import orbit.model.ModelBuilderFactory;
import orbit.util.Util;

import java.io.*;
import java.util.List;

/**
 * Coordinates a Parser and a Solver to load a profile from a file.
 * The Parser and Solver should already be constructed appropriately.
 */
public interface Loader
{
    /**
     * If the {@link #execute} method returns null, this method may return an error message which
     * describes why the profile could not be loaded.
     */
    public String getError();

    
    /**
     * If any recoverable errors occur while the profile is being loaded, this method will return them.
     */
    public String getWarning();

    public void parse() throws ParseException;

    public CallGraph solve();

}
