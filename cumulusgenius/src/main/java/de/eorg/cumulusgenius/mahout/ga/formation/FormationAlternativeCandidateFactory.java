/**
 * 
 */
package de.eorg.cumulusgenius.mahout.ga.formation;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.SortedSet;
import java.util.logging.Logger;

import org.uncommons.watchmaker.framework.CandidateFactory;

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
public class FormationAlternativeCandidateFactory implements
		CandidateFactory<FormationAlternative> {

	private static Logger log = Logger
			.getLogger(FormationAlternativeCandidateFactory.class.getName());

	private Map<Component, SortedSet<ComponentSolution>> solutionSpace;

	/**
	 * @param solutionSpace
	 */
	public FormationAlternativeCandidateFactory(
			Map<Component, SortedSet<ComponentSolution>> solutionSpace) {
		super();
		this.solutionSpace = solutionSpace;
	}

	@Override
	public List<FormationAlternative> generateInitialPopulation(int size,
			Random rand) {
		return generateInitialPopulation(size, null, rand);
	}

	@Override
	public List<FormationAlternative> generateInitialPopulation(int size,
			Collection<FormationAlternative> reuse, Random rand) {
		List<FormationAlternative> initPop = new ArrayList<FormationAlternative>();
		if (reuse != null)
			initPop.addAll(reuse);
		while (initPop.size() < size) {
			FormationSolution fs = new FormationSolution();
			for (Component c : solutionSpace.keySet())
				fs.getComponentSolutions()
						.add(new ArrayList<ComponentSolution>(solutionSpace
								.get(c)).get(rand.nextInt(solutionSpace.get(c)
								.size())));
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
			if (!initPop.contains(fa))
				initPop.add(fa);

		}

		log.fine("generated population from given " + reuse + " result "
				+ initPop);
		return initPop;
	}

	@Override
	public FormationAlternative generateRandomCandidate(Random rand) {

		FormationSolution fs = new FormationSolution();
		for (Component c : solutionSpace.keySet())
			fs.getComponentSolutions().add(
					new ArrayList<ComponentSolution>(solutionSpace.get(c))
							.get(rand.nextInt(solutionSpace.get(c).size())));
		FormationAlternative fa = new FormationAlternative("Formation_"
				+ new Date().getTime(), fs);

		log.fine("created random candidate " + fa);
		return fa;
	}
}
