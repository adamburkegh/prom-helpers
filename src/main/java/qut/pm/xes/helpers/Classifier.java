package qut.pm.xes.helpers;

import org.deckfour.xes.classification.XEventClassifier;
import org.deckfour.xes.info.impl.XLogInfoImpl;

public enum Classifier {

	STANDARD(XLogInfoImpl.STANDARD_CLASSIFIER),
	NAME(XLogInfoImpl.NAME_CLASSIFIER),
	RESOURCE(XLogInfoImpl.RESOURCE_CLASSIFIER),
	LIFECYCLE_TRANSITION(XLogInfoImpl.LIFECYCLE_TRANSITION_CLASSIFIER);
	
	private XEventClassifier eventClassifier;
	
	private Classifier(XEventClassifier ec) {
		this.eventClassifier = ec;
	}

	public XEventClassifier getEventClassifier() {
		return eventClassifier;
	}
	
}
