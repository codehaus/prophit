package orbit.model;

/**
 * Factory object used to construct a new {@link ModelBuilder}.
 */
public class ModelBuilderFactory
{
	public static ModelBuilder newModelBuilder()
	{
		return new ModelBuilderImpl();
	}
}
