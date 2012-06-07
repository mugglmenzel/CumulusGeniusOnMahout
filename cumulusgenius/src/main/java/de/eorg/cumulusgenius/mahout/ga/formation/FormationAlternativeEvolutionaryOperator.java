/**
 * 
 */
package de.eorg.cumulusgenius.mahout.ga.formation;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.SortedSet;
import java.util.logging.Logger;

import org.uncommons.watchmaker.framework.EvolutionaryOperator;

import de.eorg.cumulusgenius.shared.cloudmapping.model.mapping.Attribute;
import de.eorg.cumulusgenius.shared.cloudmapping.model.mapping.Component;
import de.eorg.cumulusgenius.shared.cloudmapping.model.mapping.ComponentSolution;
import de.eorg.cumulusgenius.shared.cloudmapping.model.mapping.EFormationValueAttribute;
import de.eorg.cumulusgenius.shared.cloudmapping.model.mapping.EProviderAttribute;
import de.eorg.cumulusgenius.shared.cloudmapping.model.mapping.FormationAlternative;
import de.eorg.cumulusgenius.shared.cloudmapping.model.mapping.FormationSolution;

/**
 * @author mugglmenzel
 * 
 */
public class FormationAlternativeEvolutionaryOperator implements
		EvolutionaryOperator<FormationAlternative> {

	private static Logger log = Logger
			.getLogger(FormationAlternativeEvolutionaryOperator.class.getName());

	// private Set<FormationAlternative> blockList = new
	// HashSet<FormationAlternative>();
	private Map<Component, SortedSet<ComponentSolution>> solutionSpace;

	/**
	 * @param blockList
	 * @param solutionSpace
	 */
	public FormationAlternativeEvolutionaryOperator(
	// Set<FormationAlternative> blockList,
			Map<Component, SortedSet<ComponentSolution>> solutionSpace) {
		super();
		// this.blockList = blockList;
		this.solutionSpace = solutionSpace;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.uncommons.watchmaker.framework.EvolutionaryOperator#apply(java.util
	 * .List, java.util.Random)
	 * 
	 * Replace loosers in population
	 */
	@Override
	public List<FormationAlternative> apply(
			List<FormationAlternative> population, Random rand) {

		log.finer("have to evolve " + population.size());
		List<FormationAlternative> newPop = new ArrayList<FormationAlternative>();
		// blockList.addAll(population);
		for (int i = 0; i < population.size(); i++) {
			int decider = rand.nextInt(2);
			FormationSolution fsOrig = population.get(i).getFormation();
			FormationSolution fs = new FormationSolution();
			if (decider == 0) {
				for (Component c : solutionSpace.keySet())
					fs.getComponentSolutions().add(
							new ArrayList<ComponentSolution>(solutionSpace
									.get(c)).get(rand.nextInt(solutionSpace
									.get(c).size())));
				log.finer("substituted all components");
			} else {

				int csRand = rand
						.nextInt(fsOrig.getComponentSolutions().size());

				for (ComponentSolution cs : fsOrig.getComponentSolutions())
					fs.getComponentSolutions().add(cs);

				Component c = fs.getComponentSolutions().get(csRand)
						.getComponent();

				fs.getComponentSolutions()
						.set(csRand,
								new ArrayList<ComponentSolution>(solutionSpace
										.get(c)).get(rand.nextInt(solutionSpace
										.get(c).size())));
				log.finer("substituted component " + c);
			}
			log.fine("evolved " + fsOrig + " to new formation solution " + fs);

			// create formation alternative with network costs

			double networkSendValue = 0D;
			double networkRecieveValue = 0D;

			List<ComponentSolution> comps = fs.getComponentSolutions();
			for (int h = 0; h < comps.size() - 1; h++)
				for (int j = h + 1; j < comps.size(); j++)
					if (comps
							.get(h)
							.getCombinationTotalValue()
							.getService()
							.getProvider()
							.equals(comps.get(j).getCombinationTotalValue()
									.getService().getProvider())) {
						networkRecieveValue += (Double) comps
								.get(h)
								.getCombinationTotalValue()
								.getService()
								.getProvider()
								.getAttribute(
										EProviderAttribute.NETWORK_COST_RECIEVE)
								.getValue();
						networkSendValue += (Double) comps
								.get(h)
								.getCombinationTotalValue()
								.getService()
								.getProvider()
								.getAttribute(
										EProviderAttribute.NETWORK_COST_SEND)
								.getValue();
					} else {
						networkRecieveValue += (Double) comps
								.get(h)
								.getCombinationTotalValue()
								.getService()
								.getProvider()
								.getAttribute(
										EProviderAttribute.INTERNET_COST_RECIEVE)
								.getValue();
						networkSendValue += (Double) comps
								.get(h)
								.getCombinationTotalValue()
								.getService()
								.getProvider()
								.getAttribute(
										EProviderAttribute.INTERNET_COST_SEND)
								.getValue();
					}

			double value = 0D;
			for (ComponentSolution cs : comps) {
				value += cs.getCombinationTotalValue().getTotalValue();
				// System.out.println("value: " + value);
			}
			fs.getAttributes().add(
					new Attribute<Double>(
							EFormationValueAttribute.NETWORK_COST_RECIEVE,
							networkSendValue));
			fs.getAttributes().add(
					new Attribute<Double>(
							EFormationValueAttribute.NETWORK_COST_SEND,
							networkRecieveValue));
			fs.getAttributes()
					.add(new Attribute<Double>(EFormationValueAttribute.VALUE,
							value));

			String secName = "";
			for (ComponentSolution cs : fs.getComponentSolutions())
				secName += "|"
						+ cs.getComponent().getName()
						+ "=["
						+ cs.getCombinationTotalValue().getAppliance()
								.getName() + "->"
						+ cs.getCombinationTotalValue().getService().getName()
						+ "]|";
			FormationAlternative fa = new FormationAlternative("Formation_"
					+ secName, fs);

			newPop.add(fa);
		}

		log.finer("evolved population from given " + population + " to "
				+ newPop);
		List<FormationAlternative> populationDiff = new ArrayList<FormationAlternative>(
				newPop);
		populationDiff.removeAll(population);
		System.out.println("evolved population of " + newPop.size()
				+ " differs in " + populationDiff.size() + " ("
				+ (100 * populationDiff.size() / population.size())
				+ "%) of soldiers.");
		return newPop;
	}
}
