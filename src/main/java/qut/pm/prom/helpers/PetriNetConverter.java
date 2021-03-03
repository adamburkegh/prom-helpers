package qut.pm.prom.helpers;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.processmining.framework.plugin.PluginContext;
import org.processmining.models.graphbased.directed.petrinet.StochasticNet;
import org.processmining.plugins.pnml.importing.StochasticNetDeserializer;
import org.processmining.plugins.pnml.simple.PNMLRoot;
import org.simpleframework.xml.Serializer;
import org.simpleframework.xml.core.Persister;

public class PetriNetConverter {
	
	private static final String DOT = "DOT";
	private static final String FRAG = "FRAG";
	private static final String PNML = "PNML";
	private static boolean verbose = false;
	private static final Set<String> INPUT_FORMATS;
	private static final Set<String> OUTPUT_FORMATS;
	
	static {
		INPUT_FORMATS = new HashSet<String>();
		INPUT_FORMATS.add(FRAG);
		INPUT_FORMATS.add(PNML);
		OUTPUT_FORMATS = new HashSet<String>();
		OUTPUT_FORMATS.add(DOT);
		OUTPUT_FORMATS.add(PNML);
	}
	
	
	private static void log(String m) {
		if (verbose)
			System.out.println(m);
	}

	public static void main(String[] args) throws Exception{
		CommandLineParser parser = new DefaultParser();
		final Options options = new Options();
		options.addOption(new Option("v", "verbose", false, "Verbose output."));
		Option inFormat   = Option.builder( "i" )
                .hasArg()
                .longOpt("input-format")
                .desc("Input format (PNML,FRAG)")
                .build();
		options.addOption(inFormat);
		Option outFormat   = Option.builder( "o" )
                .hasArg()
                .longOpt("output-format")
                .desc("Output format (PNML,DOT)")
                .build();
		options.addOption(outFormat);
		CommandLine cmd = parser.parse(options,args);
		String[] inFiles = cmd.getArgs();
		verbose = cmd.hasOption("v");
		String inFormatValue = cmd.getOptionValue("i",PNML);
		String outFormatValue = cmd.getOptionValue("o",DOT);
		if (!INPUT_FORMATS.contains(inFormatValue)){
			exitWithHelp(options, "Invalid input format:" + inFormatValue);
		}
		if (!OUTPUT_FORMATS.contains(outFormatValue)){
			exitWithHelp(options, "Invalid output format:" + outFormatValue);
		}
		for (String fileIn: inFiles) {
			processFile(fileIn, inFormatValue, outFormatValue);	
		}		
	}

	private static void exitWithHelp(final Options options, String msg) {
		HelpFormatter helpFormatter = new HelpFormatter();
		log(msg);
		helpFormatter.printHelp("pnc",options,true);
		System.exit(1);
	}
	
	private static void processFile(String inFile, String inFormatValue, String outFormatValue)
		throws Exception
	{
		File pnf = new File(inFile);
		if (!pnf.exists()) {
			System.err.println("File " + inFile + " not found");
			System.exit(1);
		}
		if (pnf.isDirectory()) {
			convertDirectory(pnf,inFormatValue,outFormatValue);
		}else {
			convertFile(pnf,inFormatValue,outFormatValue);
		}
	}

	private static void convertDirectory(File pnfDir, String inFormatValue, String outFormatValue) throws Exception {
		log("Converting directory " + pnfDir.getName());
		for (File pnf : pnfDir.listFiles(new FilenameFilter() {
			@Override
			public boolean accept(File dir, String name) {
				return name.contains(".xml") || name.contains(".pnml");
			}

		})) {
			try {
				convertFile(pnf, inFormatValue, outFormatValue);
			} catch (Exception e) {
				System.err.println("Invalid file " + pnf.getName() + " " + e.getMessage());
			}
		}
	}

	private static StochasticNet readPNML(File petriNetFileIn) throws Exception {
		StochasticNetDeserializer snd = new StochasticNetDeserializer();
		Serializer serializer = new Persister();
		PNMLRoot pnml = serializer.read(PNMLRoot.class, petriNetFileIn);
		PluginContext uipc = 
				new HeadlessDefinitelyNotUIPluginContext(new ConsoleUIPluginContext(), "spn_dot_converter");
		String pnfName = petriNetFileIn.getName();
		Object[] obj = snd.convertToNet(uipc, pnml, pnfName, true);
		StochasticNet net = (StochasticNet)obj[0];
		return net;
	}
	
	private static void convertFile(File petriNetFileIn,String inFormatValue, String outFormatValue) throws Exception {
		StochasticNet net = null;
		if (inFormatValue.equals(PNML)) {
			net = readPNML(petriNetFileIn);
		}else {
			// == FRAG
			net = PetrinetExportUtils.readPNetFragmentToStochasticNet(petriNetFileIn);
		}
		String pnfName = petriNetFileIn.getName();
		String prefix = pnfName.substring(0, pnfName.lastIndexOf(".") );
		log("Converting file " + pnfName);
		if (outFormatValue.equals(DOT)) {
			writeDOT(petriNetFileIn, net, prefix);
		}else {
			// PNML
			writePNML(petriNetFileIn,net,prefix);
		}
	}

	private static void writeDOT(File petriNetFileIn, StochasticNet net, String prefix) throws IOException {
		String dot = PetrinetExportUtils.convertPetrinetToDOT(net);
		File outputFile = 
				new File(petriNetFileIn.getParentFile().getAbsolutePath() 
						+ File.separator + prefix + ".dot");
        BufferedWriter writer = new BufferedWriter(new FileWriter(outputFile));
        writer.write(dot);
        writer.flush();
        writer.close();
        log("Output written to " + outputFile.getAbsolutePath());
	}
	
	private static void writePNML(File petriNetFileIn, StochasticNet net, String prefix) throws Exception {
		File outputFile = 
				new File(petriNetFileIn.getParentFile().getAbsolutePath() 
						+ File.separator + prefix + ".pnml");
		PetrinetExportUtils.storePNMLModel(outputFile,net);
        log("Output written to " + outputFile.getAbsolutePath());
	}
	
	
}
