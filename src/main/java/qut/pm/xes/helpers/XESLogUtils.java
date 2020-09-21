package qut.pm.xes.helpers;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.deckfour.xes.classification.XEventClassifier;
import org.deckfour.xes.classification.XEventNameClassifier;
import org.deckfour.xes.model.XAttribute;
import org.deckfour.xes.model.XLog;

public class XESLogUtils {

	public static final String XES_CONCEPT_NAME = "concept:name";
	
	private static final Logger LOGGER = LogManager.getLogger();

	public static String getLogName(XLog log) {
		XAttribute nameAttr = log.getAttributes().get(XES_CONCEPT_NAME);
		return nameAttr == null ? "" : nameAttr.toString();
	}
	
	public static XEventClassifier detectNameBasedClassifier(XLog log) {
		LOGGER.debug( "Detected classifiers: {} ", log.getClassifiers());
		XEventClassifier nameClassifier = new XEventNameClassifier();
		Set<String> nameKeys = new HashSet<String>();
		Collections.addAll(nameKeys, nameClassifier.getDefiningAttributeKeys());
		
		for (XEventClassifier classifier: log.getClassifiers()) {
			Set<String> classifierKeys = new HashSet<String>();
			Collections.addAll(classifierKeys, classifier.getDefiningAttributeKeys());
			if (nameKeys.equals(classifierKeys)){
				return classifier;
			}
		}
		for (XEventClassifier classifier: log.getClassifiers()) {
			for (String attrKey: classifier.getDefiningAttributeKeys()) {
				if (nameKeys.contains(attrKey)) {
					return classifier;
				}
			}
		}
		return nameClassifier;
	}
	
	
}
