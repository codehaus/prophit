package orbit.parsers;

import orbit.model.ModelBuilder;

import java.util.List;

public interface Parser
{
	public boolean isFileFormatRecognized();

	public void execute(ModelBuilder builder) throws ParseException;
}
