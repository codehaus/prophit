package orbit.parsers;

import orbit.model.ModelBuilder;

public interface Parser
{
	public boolean isFileFormatRecognized();

	public void execute(ModelBuilder builder) throws ParseException;
}
