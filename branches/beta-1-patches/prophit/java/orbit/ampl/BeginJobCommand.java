package orbit.ampl;

import java.io.BufferedReader;
import java.io.Writer;

public class BeginJobCommand
	extends Command
{
	private final Datum modelDatum;
	private final Datum dataDatum;
	private final Datum commandsDatum;
	private String jobNumber = null;
	
	public BeginJobCommand(String userName, Datum modelDatum, Datum dataDatum, Datum commandsDatum)
	{
		super(userName);

		this.modelDatum = modelDatum;
		this.dataDatum = dataDatum;
		this.commandsDatum = commandsDatum;
	}

	public String getJobNumber()
	{
		return jobNumber;
	}
	
	public void execute(Writer writer, BufferedReader reader)
	{
		MultiDatum md = new MultiDatum();
		md.add(new StringDatum("TYPE NCO\n"));
		md.add(new StringDatum("SOLVER DONLP2\n\n"));

		if ( commandsDatum != null )
		{
			MultiDatum commands = new MultiDatum();
			commands.add(new StringDatum("BEGIN.COM[" + commandsDatum.getSize() + "]\n"));
			commands.add(commandsDatum);
			md.add(commands);
		}

		MultiDatum model = new MultiDatum();
		model.add(new StringDatum("BEGIN.MOD[" + modelDatum.getSize() + "]\n"));
		model.add(modelDatum);
		md.add(model);

		MultiDatum data = new MultiDatum();
		data.add(new StringDatum("BEGIN.DAT[" + dataDatum.getSize() + "]\n"));
		data.add(dataDatum);
		md.add(data);

		writeHeader(writer, "begin job " + md.getSize());
		print(writer, md.getText());

		flush(writer);

		// There is a -1 in the reply for some reason
		readLine(reader);
		jobNumber = readLine(reader);
	}
}
