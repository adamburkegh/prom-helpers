package qut.pm.spm;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Set;

import org.processmining.acceptingpetrinet.models.AcceptingPetriNet;
import org.processmining.framework.plugin.PluginContext;
import org.processmining.models.graphbased.directed.petrinet.Petrinet;
import org.processmining.models.graphbased.directed.petrinet.StochasticNet;
import org.processmining.models.semantics.petrinet.Marking;

public interface AcceptingStochasticNet extends AcceptingPetriNet{

	String getId();

	StochasticNet getNet();

	Marking getInitialMarking();

	Set<Marking> getFinalMarkings();

	@Deprecated
	default void init(Petrinet net) {
		throw new UnsupportedOperationException("Deprecated method in superinterface");
	}

	@Deprecated
	default void init(PluginContext context, Petrinet net) {
		throw new UnsupportedOperationException("Deprecated method in superinterface");
	}

	void setInitialMarking(Marking initialMarking);

	void setFinalMarkings(Set<Marking> finalMarkings);

	@Deprecated
	default void importFromStream(PluginContext context, InputStream input) throws Exception{
		throw new UnsupportedOperationException("Deprecated method in superinterface");
	}

	void exportToFile(PluginContext context, File file) throws IOException;

}