/**
 * 
 */
package de.eorg.cumulusgenius.mahout.ga.formation;

import java.util.ArrayList;
import java.util.Map;
import java.util.SortedSet;
import java.util.logging.Logger;

import org.uncommons.watchmaker.framework.PopulationData;
import org.uncommons.watchmaker.framework.TerminationCondition;

import de.eorg.cumulusgenius.shared.cloudmapping.model.mapping.Component;
import de.eorg.cumulusgenius.shared.cloudmapping.model.mapping.ComponentSolution;

/**
 * @author mugglmenzel
 * 
 */
public class FormationAlternativeTerminator implements TerminationCondition {

	private static Logger log = Logger
			.getLogger(FormationAlternativeTerminator.class.getName());

	private Map<Component, SortedSet<ComponentSolution>> solutionSpace;

	/**
	 * @param blockList
	 * @param solutionSpace
	 */
	public FormationAlternativeTerminator(
			Map<Component, SortedSet<ComponentSolution>> solutionSpace) {
		super();
		this.solutionSpace = solutionSpace;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.uncommons.watchmaker.framework.TerminationCondition#shouldTerminate
	 * (org.uncommons.watchmaker.framework.PopulationData)
	 */
	@Override
	public boolean shouldTerminate(PopulationData<?> populationData) {
		double solutionSpaceSize = Math.pow(new ArrayList<ComponentSolution>(
				solutionSpace.values().iterator().next()).size(), solutionSpace
				.size());
		log.fine("sizes: solutionSpace " + solutionSpaceSize + ", population "
				+ populationData.getPopulationSize() + ", generation "
				+ populationData.getGenerationNumber() + ", max generations "
				+ (solutionSpaceSize / populationData.getPopulationSize()));
		// || Math.pow(new
		// ArrayList<ComponentSolution>(solutionSpace.values().iterator().next()).size(),
		// solutionSpace.size()) - blockList.size() <
		// populationData.getPopulationSize()
		return populationData.getElapsedTime() > 5 * 60 * 1000
				// || populationData.getGenerationNumber() > 500
				|| solutionSpaceSize == populationData.getPopulationSize()
				|| populationData.getGenerationNumber() > solutionSpaceSize
						/ populationData.getPopulationSize() * 2;
	}

}
