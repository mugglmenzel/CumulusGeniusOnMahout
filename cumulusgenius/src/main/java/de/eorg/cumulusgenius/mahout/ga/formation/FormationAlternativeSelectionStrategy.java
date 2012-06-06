/**
 * 
 */
package de.eorg.cumulusgenius.mahout.ga.formation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.logging.Logger;

import org.uncommons.watchmaker.framework.EvaluatedCandidate;
import org.uncommons.watchmaker.framework.SelectionStrategy;

import de.eorg.cumulusgenius.shared.cloudmapping.model.mapping.FormationAlternative;

/**
 * @author mugglmenzel
 * 
 */
public class FormationAlternativeSelectionStrategy implements
		SelectionStrategy<FormationAlternative> {

	private static Logger log = Logger
			.getLogger(FormationAlternativeSelectionStrategy.class.getName());
	
	@Override
	public <S extends FormationAlternative> List<S> select(
			List<EvaluatedCandidate<S>> population, boolean naturalOrder,
			int size, Random rand) {
		log.fine("Selecting " + size + " from population.");
		
		List<S> result = new ArrayList<S>();
		Collections.sort(population);
		if (!naturalOrder)
			Collections.reverse(population);
		for (EvaluatedCandidate<S> c : population.subList(0, size))
			result.add(c.getCandidate());
		return result;
	}
}
