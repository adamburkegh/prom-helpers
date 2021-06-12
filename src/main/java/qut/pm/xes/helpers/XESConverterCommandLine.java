package qut.pm.xes.helpers;

import java.io.FileOutputStream;

import org.deckfour.xes.model.XLog;
import org.deckfour.xes.out.XesXmlSerializer;

public class XESConverterCommandLine {

	public static void main(String[] args) throws Exception{
		// very basic for now - DCDT only
		DelimitedTraceToXESConverter converter = new DelimitedTraceToXESConverter();
		XLog log = converter.convertFile(args[0],",,", "\n");
		XesXmlSerializer xser = new XesXmlSerializer();
		FileOutputStream fos = new FileOutputStream(args[1]);
		xser.serialize(log, fos);
		fos.close();
	}
	
}
