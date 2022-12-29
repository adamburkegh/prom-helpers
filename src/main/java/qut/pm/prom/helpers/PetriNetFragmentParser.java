package qut.pm.prom.helpers;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.processmining.acceptingpetrinet.models.AcceptingPetriNet;
import org.processmining.models.graphbased.NodeID;
import org.processmining.models.graphbased.directed.petrinet.PetrinetNode;
import org.processmining.models.graphbased.directed.petrinet.StochasticNet;
import org.processmining.models.graphbased.directed.petrinet.elements.Arc;
import org.processmining.models.graphbased.directed.petrinet.elements.Place;
import org.processmining.models.graphbased.directed.petrinet.elements.Transition;
import org.processmining.models.graphbased.directed.petrinet.impl.StochasticNetImpl;
import org.processmining.models.semantics.petrinet.Marking;

import qut.pm.spm.AcceptingStochasticNet;
import qut.pm.spm.AcceptingStochasticNetImpl;

/**
 * Allows the creation of Petri nets with short one line ASCII sketches, for example
 * <code>initialPlace -> [transition1] -> mp -> [transition2] -> finalPlace</code> 
 * 
 * Larger nets can be created with multiple invocations. Existing nodes will be looked up by 
 * label. 
 * 
 * Weighted transitions without weights, as in <code>{b}</code>, are defaulted to weight 1.0.
 * 
 * Nodes with duplicate labels can be specified using a [tranLabel__id] syntax. 
 * 
 * Current limitations: SPN support is only weighted transitions. No time distributions or weighted 
 * arcs.
 * 
 * Methods for creating an {@code AcceptingPetriNet} and {@code AcceptingStochasticNet} are also 
 * provided. These use naming conventions to identify initial and final markings per 
 * {@link #createAcceptingNet(String, String)}.
 * 
 * Grammar
 * 
 * <pre>
 * PETRI_ONELINE_NET 	:: PLACE EDGE TRANSITION EDGE PLACE_LED_SUBNET
 * PLACE_LED_SUBNET  	:: PLACE EDGE TRANSITION EDGE PLACE_LED_SUBNET
 * PLACE_LED_SUBNET  	:: PLACE 
 * TRANSITION_SUBNET 	:: TRANSITION EDGE PLACE EDGE TRANSITION_SUBNET
 * TRANSITION_SUBNET 	:: TRANSITION 
 * TRANSITION        	:: SIMPLE_TRANSITION || WEIGHTED_TRANSITION
 * SIMPLE_TRANSITION 	:: TRAN_START TRAN_LABEL TRAN_END
 * WEIGHTED_TRANSITION  :: WEIGHTED_TRAN_VALUE | WEIGHTED_TRAN_DEFAULT
 * WEIGHTED_TRAN_VALUE  :: '{' TRAN_LABEL WEIGHT '}'
 * WEIGHTED_TRAN_DEFAULT:: '{' TRAN_LABEL '}'
 * TRAN_LABEL			:: TLABEL || ID_LABEL
 * ID_LABEL				:: TLABEL ID_PREFIX ID
 * TLABEL				:: LABEL || SILENT_LABEL
 * PLACE             	:: LABEL
 * EDGE              	:: '->'
 * SIMPLE_TRAN_START	:: '['
 * SIMPLE_TRAN_END		:: ']' 
 * ID_PREFIX			:: '__'
 * WEIGHT			 	:: NUM_STR
 * ID             		:: NUM_STR
 * NUM_STR				:: numeric string
 * SILENT_LABEL         :: 'tau'
 * LABEL             	:: alphanumeric string
 * </pre>
 *
 * Doesn't work for extended codepoints (eg UTF-16).
 * 
 * @param netText
 * @return
 */
public class PetriNetFragmentParser{

	private static String ID_LEXEME = "__";
	
	private static enum TokenInfo{
		SIMPLE_TRAN_START("\\["),
		SIMPLE_TRAN_END("\\]"),
		WEIGHTED_TRAN_START("\\{"),
		WEIGHTED_TRAN_END("\\}"),
		ID_PREFIX(ID_LEXEME),
		EDGE("->"),
		SILENT_LABEL("tau"),
		LABEL("[a-zA-Z][a-zA-Z0-9]*"),
		WEIGHT("[0-9]+\\.[0-9]+"),
		ID("[0-9]+"),
		TERMINAL("");

		public static final TokenInfo[] LEX_VALUES = 
				{SIMPLE_TRAN_START,SIMPLE_TRAN_END,
				 WEIGHTED_TRAN_START,WEIGHTED_TRAN_END,
						ID_PREFIX,EDGE,SILENT_LABEL,LABEL,WEIGHT,ID}; 
		
		private Pattern pattern;
		
		private TokenInfo(String regex){
			this.pattern = Pattern.compile("^\\s*("+regex+")");
		}
		
		
	}
	
	private static class Token{
		public final PetriNetFragmentParser.TokenInfo tokenInfo;
		public final String sequence;
		
		public Token(PetriNetFragmentParser.TokenInfo token, String sequence) {
			this.tokenInfo = token;
			this.sequence = sequence;
		}
		
		public String toString() {
			return sequence + ":" + tokenInfo;
		}
	}
	
	public static final Set<String> INITIAL_PLACE_LABELS;
	public static final Set<String> FINAL_PLACE_LABELS;
	
	static {
		INITIAL_PLACE_LABELS = new TreeSet<>();
		INITIAL_PLACE_LABELS.add("Start");
		INITIAL_PLACE_LABELS.add("Initial");
		INITIAL_PLACE_LABELS.add("I");
		FINAL_PLACE_LABELS = new TreeSet<>();
		FINAL_PLACE_LABELS.add("End");
		FINAL_PLACE_LABELS.add("Final");
		FINAL_PLACE_LABELS.add("F");
	}

	
	private LinkedList<Token> tokens = new LinkedList<Token>();
	private Token lookahead = null;
	private StochasticNet net;
	private Map<String,PetrinetNode> nodeLookup = new HashMap<>();
	private NodeMapper nodeMapper = new NodeMapper();
	
	public NodeMapper addToNet(StochasticNet net, String netText) {
		tokenize(netText);
		this.net = net;
		parse();
		return nodeMapper ;
	}
	
	public StochasticNet createNet(String label, String netText) {
		StochasticNet net = new StochasticNetImpl(label);
		nodeLookup = new HashMap<>();
		nodeMapper = new NodeMapper();
		addToNet(net,netText);
		return net;
	}

	/**
	 * 
	 * Returns an AcceptingPetriNet with one initial and one final place marked. Initial and final
	 * markings are determined by labeling convention but will only be applied where places have the
	 * correct edge properties, ie, only outgoing for initial places, only incoming for final.
	 * 
	 *  Naming conventions for initial places, in order of checking: Start, Initial, I.
	 *  
	 *  Naming conventions for final places, in order of checking: End, Final, F.
	 * 
	 * @param label
	 * @param netText
	 * @return
	 */
	public AcceptingStochasticNet createAcceptingNet(String label, String netText) {
		StochasticNet net = new StochasticNetImpl(label);
		nodeLookup = new HashMap<>();
		addToNet(net,netText);
		return markInitialFinalPlaces(net);
	}


	public AcceptingStochasticNet markInitialFinalPlaces(StochasticNet net) {
		Set<Place> initialCandidates = new TreeSet<>();
		Set<Place> finalCandidates = new TreeSet<>();
		for (Place place: net.getPlaces()) {
			 if ( INITIAL_PLACE_LABELS.contains(place.getLabel()) 
					 && net.getInEdges(place).isEmpty() ) 
			 {
				 initialCandidates.add(place);
			 }else {
				 if (FINAL_PLACE_LABELS.contains(place.getLabel())
						 && net.getOutEdges(place).isEmpty()) 
				 {
					 finalCandidates.add(place);
				 }
			 }
		}
		Marking initialMarking = markPlaceFromCandidates(initialCandidates, INITIAL_PLACE_LABELS);
		Marking finalMarking = markPlaceFromCandidates(finalCandidates, FINAL_PLACE_LABELS);
		Set<Marking> finalMarkings = new HashSet<>();
		finalMarkings.add(finalMarking);
		return new AcceptingStochasticNetImpl(net,initialMarking,finalMarkings);
	}

	private Marking markPlaceFromCandidates(Set<Place> initialCandidates, Set<String> identifyingLabels) {
		Marking resultMarking = new Marking();
		for (String initLabel: identifyingLabels) {
			for (Place initPlace: initialCandidates) {
				if (initLabel.equals(initPlace.getLabel())){
					resultMarking.add(initPlace);
					break;
				}
			}
		}
		return resultMarking;
	}
	
	public AcceptingPetriNet createAcceptingNetArgs(String label, String ... specs) {
		if (specs.length == 0) {
			throw new RuntimeException("Cannot create empty Accepting Petri Net");
		}
		AcceptingPetriNet anet = createAcceptingNet(label,specs[0]);
		for (int i=1; i<specs.length; i++) {
			addToAcceptingNet(anet, specs[i]);
		}		
		return anet;
	}
	
	/**
	 * Precondition: the underlying <code>Petrinet</code> in <code>anet</code> is a 
	 * <code>StochasticNet</code>.
	 * 
	 * @param anet
	 * @param netText
	 */
	public NodeMapper addToAcceptingNet(AcceptingPetriNet anet, String netText) {
		net = (StochasticNet)anet.getNet();
		NodeMapper nm = addToNet(net,netText);
		anet = markInitialFinalPlaces(net);
		return nm;
	}
	
	public StochasticNet createNetArgs(String label, String ... specs) {
		if (specs.length == 0) {
			return new StochasticNetImpl(label);
		}
		StochasticNet net = createNet(label, specs[0]);
		for (int i=1; i<specs.length; i++) {
			addToNet(net, specs[i]);
		}
		return net;
	}


	private void tokenize(String str) {
		tokens.clear();
		String s = str.trim();
		while (!s.equals("")) {
			boolean match = false;
		    for (PetriNetFragmentParser.TokenInfo info : TokenInfo.LEX_VALUES) {
		        Matcher m = info.pattern.matcher(s);
		        if (m.find()) {
		        	match = true;
		        	String tok = m.group().trim();
		        	tokens.add(new Token(info, tok));
		        	s = m.replaceFirst("");
		        	break;
		        }
		    }
		    if (!match) 
		    	throw new RuntimeException("Unexpected character in input:"+s);
		}
    }

	private void parse() {
		lookahead = tokens.getFirst();
		petriOnelineNet();
		if (lookahead.tokenInfo != TokenInfo.TERMINAL)
			throw new RuntimeException("Unexpected symbol " + lookahead + " found");
	}


	private void petriOnelineNet() {
		Place p1 = place();
		edge();
		Transition transition = transition();
		edge();
		Place p2 = placeLedSubnet();
		readdArc(p1, transition);
		readdArc(transition, p2);			
	}

	private void readdArc(Transition transition, Place p2) {
		Arc arc = net.getArc(transition,p2);
		if (arc == null)
			net.addArc(transition, p2);
	}

	private void readdArc(Place p1, Transition transition) {
		Arc arc = net.getArc(p1,transition);
		if (arc == null)
			net.addArc(p1, transition);
	}
	
	private Place placeLedSubnet() {
		Place head = place();
		if (lookahead.tokenInfo == TokenInfo.EDGE) {
			edge();
			Transition transition = transition();
			edge();
			Place tail = placeLedSubnet();
			readdArc(head, transition);
			readdArc(transition, tail);
		}
		return head;
	}

	private Transition transition() {
		Transition transition = null;
		if (lookahead.tokenInfo.equals(TokenInfo.SIMPLE_TRAN_START)) {
			transition = simpleTransition();
		}
		if (lookahead.tokenInfo.equals(TokenInfo.WEIGHTED_TRAN_START)) {
			transition = weightedValueTransition();
		}
		nextToken();
		return transition;
	}

	private Transition weightedValueTransition() {
		Transition transition = null;
		nextToken();
		String label = "";
		String id = "";
		boolean silentTransition = false;
		double weight = 1.0;
		if (lookahead.tokenInfo.equals(TokenInfo.LABEL)) {
			label = tranLabel();
			nextToken();
		}else if(lookahead.tokenInfo.equals(TokenInfo.SILENT_LABEL)) {
			label = tranLabel();
			silentTransition = true;
			nextToken();
		}else {
			throw new RuntimeException("Expected label, but found " + lookahead );
		}
		if (lookahead.tokenInfo.equals(TokenInfo.ID_PREFIX)){
			nextToken();
			id = id();
			nextToken();
		}
		if (lookahead.tokenInfo.equals(TokenInfo.WEIGHT)) {
			weight = weight();
			nextToken();			
		} 
		if (!lookahead.tokenInfo.equals(TokenInfo.WEIGHTED_TRAN_END)) {
			tokenError(TokenInfo.WEIGHTED_TRAN_END,TokenInfo.ID_PREFIX);
		}
		String genId = genId(label, id);
		if (id.isEmpty()) {
			transition = checkExistingTransition(label);
		}else {
			transition = checkExistingTransitionById(genId);
		}
		if (transition == null) {
			transition = net.addImmediateTransition(label, weight);
			transition.setInvisible(silentTransition);
			nodeLookup.put(label,transition);
			nodeMapper.put(transition.getId(), genId);
		}
		return transition;
	}

	private Transition checkExistingTransitionById(String id) {
		NodeID nodeId = nodeMapper.getNode(id);
		if (nodeId == null)
			return null;
		Transition transition = null;
		net.getNodes();
		for (PetrinetNode node: net.getNodes()) {
			if (nodeId.equals(node.getId())){
				transition = (Transition)node;
				break;
			}
		}
		return transition;
	}

	private String genId(String label, String id) {
		if (!id.isEmpty()) {
			return label + ID_LEXEME + id;
		}
		return label;
	}

	private double weight() {
		return Double.valueOf(lookahead.sequence);
	}

	private Transition simpleTransition() {
		Transition transition = null;
		nextToken();
		String label = "";
		String id = "";
		boolean silentTransition = false;
		if (lookahead.tokenInfo.equals(TokenInfo.LABEL)) {
			label = tranLabel();
			nextToken();
		}else if(lookahead.tokenInfo.equals(TokenInfo.SILENT_LABEL)) {
			label = tranLabel();
			silentTransition = true;
			nextToken();
		}else {
			throw new RuntimeException("Expected label, but found " + lookahead );
		}
		if (lookahead.tokenInfo.equals(TokenInfo.SIMPLE_TRAN_END)) {
			transition = checkExistingTransition(label);
		}else if (lookahead.tokenInfo.equals(TokenInfo.ID_PREFIX)){
			transition = null;
			nextToken();
			id = id();
			nextToken();
			if (!lookahead.tokenInfo.equals(TokenInfo.SIMPLE_TRAN_END)) {
				tokenError(TokenInfo.SIMPLE_TRAN_END,TokenInfo.ID_PREFIX);				
			}
		}else {
			tokenError(TokenInfo.SIMPLE_TRAN_END,TokenInfo.ID_PREFIX);
		}
		if (transition == null) {
			transition = net.addTransition(label);
			transition.setInvisible(silentTransition);
			nodeLookup.put(label,transition);
			String genId = genId(label, id);
			nodeMapper.put(transition.getId(), genId);
		}
		return transition;
	}

	private String id() {
		return lookahead.sequence;
	}

	private void tokenError(TokenInfo ... tokens) {
		throw new RuntimeException("Expected one of " + Arrays.toString(tokens) 
								+ ", but found " + lookahead );
	}
	
	private String tranLabel() {
		return lookahead.sequence;
	}

	private Transition checkExistingTransition(String label) {
		return (Transition)checkExistingNode(label,Transition.class);
	}
	
	private Place checkExistingPlace(String label) {
		return (Place)checkExistingNode(label,Place.class);
	}

	
	private PetrinetNode checkExistingNode(String label, Class<?> expectedClass) {
		PetrinetNode existing = nodeLookup.get(label);
		if (existing != null)
			if (!(expectedClass.isInstance(existing))) {
				throw new RuntimeException("New node " + label + " duplicates existing node of wrong type");
			}
		return existing;
	}


	private void edge() {
		if (lookahead.tokenInfo != TokenInfo.EDGE)
			throw new RuntimeException("Expected ->, but found " + lookahead );
		nextToken();		
	}


	private Place place() {
		String label = lookahead.sequence;
		Place place = checkExistingPlace(label);
		if (place == null) {
			place = net.addPlace(label);
			nodeLookup.put(label,place);
		}
		nextToken();
		return place;
	}

	private void nextToken() {
		tokens.pop();
		// at the end of input we return an epsilon token
		if (tokens.isEmpty())
			lookahead = new Token(TokenInfo.TERMINAL, "");
		else
			lookahead = tokens.getFirst();
	}		
	
}