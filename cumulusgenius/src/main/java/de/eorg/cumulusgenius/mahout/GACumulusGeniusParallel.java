package de.eorg.cumulusgenius.mahout;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.logging.LogManager;

import org.apache.mahout.ga.watchmaker.MahoutFitnessEvaluator;
import org.apache.mahout.ga.watchmaker.STEvolutionEngine;
import org.uncommons.watchmaker.framework.EvaluatedCandidate;

import de.eorg.cumulusgenius.mahout.ga.formation.FormationAlternativeCandidateFactory;
import de.eorg.cumulusgenius.mahout.ga.formation.FormationAlternativeEvolutionaryOperator;
import de.eorg.cumulusgenius.mahout.ga.formation.FormationAlternativeFitnessEvaluatorParallel;
import de.eorg.cumulusgenius.mahout.ga.formation.FormationAlternativeSelectionStrategy;
import de.eorg.cumulusgenius.mahout.ga.formation.FormationAlternativeTerminator;
import de.eorg.cumulusgenius.shared.cloudmapping.logic.ahp.AnalyticHierarchyProcess;
import de.eorg.cumulusgenius.shared.cloudmapping.model.AMI;
import de.eorg.cumulusgenius.shared.cloudmapping.model.EC2Resource;
import de.eorg.cumulusgenius.shared.cloudmapping.model.ahp.configuration.Alternative;
import de.eorg.cumulusgenius.shared.cloudmapping.model.ahp.configuration.Criterion;
import de.eorg.cumulusgenius.shared.cloudmapping.model.ahp.configuration.CriterionType;
import de.eorg.cumulusgenius.shared.cloudmapping.model.ahp.configuration.Goal;
import de.eorg.cumulusgenius.shared.cloudmapping.model.ahp.configuration.GoalType;
import de.eorg.cumulusgenius.shared.cloudmapping.model.ahp.values.Evaluation;
import de.eorg.cumulusgenius.shared.cloudmapping.model.ahp.values.EvaluationResult;
import de.eorg.cumulusgenius.shared.cloudmapping.model.jama.Matrix;
import de.eorg.cumulusgenius.shared.cloudmapping.model.mapping.ApplianceAlternative;
import de.eorg.cumulusgenius.shared.cloudmapping.model.mapping.ApplianceDecision;
import de.eorg.cumulusgenius.shared.cloudmapping.model.mapping.Attribute;
import de.eorg.cumulusgenius.shared.cloudmapping.model.mapping.CombinationTotalValue;
import de.eorg.cumulusgenius.shared.cloudmapping.model.mapping.CombinationValue;
import de.eorg.cumulusgenius.shared.cloudmapping.model.mapping.Component;
import de.eorg.cumulusgenius.shared.cloudmapping.model.mapping.ComponentSolution;
import de.eorg.cumulusgenius.shared.cloudmapping.model.mapping.ComputeDecision;
import de.eorg.cumulusgenius.shared.cloudmapping.model.mapping.ComputeServiceAlternative;
import de.eorg.cumulusgenius.shared.cloudmapping.model.mapping.EApplianceAttribute;
import de.eorg.cumulusgenius.shared.cloudmapping.model.mapping.EComputeServiceAttribute;
import de.eorg.cumulusgenius.shared.cloudmapping.model.mapping.EFormationValueAttribute;
import de.eorg.cumulusgenius.shared.cloudmapping.model.mapping.EProviderAttribute;
import de.eorg.cumulusgenius.shared.cloudmapping.model.mapping.FormationAlternative;
import de.eorg.cumulusgenius.shared.cloudmapping.model.mapping.FormationDecision;
import de.eorg.cumulusgenius.shared.cloudmapping.model.mapping.FormationSolution;
import de.eorg.cumulusgenius.shared.cloudmapping.model.mapping.FormationValue;
import de.eorg.cumulusgenius.shared.cloudmapping.model.mapping.Provider;

public class GACumulusGeniusParallel {

	private static boolean COMPARISON = true;

	private static Integer NUM_APPLIANCES = 5;

	private static Integer NUM_SERVICES = 5;

	private static Integer NUM_PROVIDERS = 3;

	private static Integer NUM_COMPONENTS = 3;

	private static Integer STEP_SIZE = 1;

	private static long TIME_INTERMEDIATE_TOTAL = 0L;

	private static long TIME_COMPONENTS_TOTAL = 0L;

	private static long TIME_TOTAL = 0L;

	private static Integer GA_POPULATION_SIZE = 100;

	private static Double GA_ELITE_FACTOR = 0.2D;

	private static List<Provider> providers = new ArrayList<Provider>();

	/**
	 * @param args
	 * @throws Exception
	 */
	public static void main(String[] args) throws Exception {
		System.out.println("args: " + args.length);
		if (args.length > 0) {
			NUM_APPLIANCES = new Integer(args[0]);
			if (args.length > 1 && args[1] != null)
				NUM_SERVICES = new Integer(args[1]);
			if (args.length > 2 && args[2] != null)
				NUM_PROVIDERS = new Integer(args[2]);
			if (args.length > 3 && args[3] != null)
				NUM_COMPONENTS = new Integer(args[3]);
			if (args.length > 4 && args[4] != null)
				STEP_SIZE = new Integer(args[4]);
			if (args.length > 5 && args[5] != null)
				COMPARISON = new Boolean(args[5]);
		}

		for (int i = 0; i < NUM_PROVIDERS; i++) {
			Provider p = new Provider("provider-" + i);
			p.getAttributes().add(
					new Attribute<Double>(
							EProviderAttribute.NETWORK_COST_RECIEVE, 100 + Math
									.random() * 100));
			p.getAttributes().add(
					new Attribute<Double>(EProviderAttribute.NETWORK_COST_SEND,
							100 + Math.random() * 100));
			p.getAttributes().add(
					new Attribute<Double>(
							EProviderAttribute.INTERNET_COST_RECIEVE,
							500 + Math.random() * 100));
			p.getAttributes().add(
					new Attribute<Double>(
							EProviderAttribute.INTERNET_COST_SEND, 500 + Math
									.random() * 100));
			providers.add(p);
		}

		// control Logging output with file
		LogManager.getLogManager().readConfiguration(
				new FileInputStream(new File("logging.properties")));

		FileWriter fw_c = new FileWriter("out_components.txt");
		BufferedWriter out_c = new BufferedWriter(fw_c);
		out_c.write("Component,Components,Number of AMIs,Number of Services,AMIs Model Creation (ms),AMIs Evaluation (ms),Services Model Creation (ms),Services Evaluation (ms),Combination Model Creation (ms),Combination Evaluation (ms),Total (ms)\n");
		out_c.close();
		FileWriter fw_f = new FileWriter("out_formation.txt");
		BufferedWriter out_f = new BufferedWriter(fw_f);
		out_f.write("Components,Number of AMIs,Number of Services,Formation Solutions Model Creation (ms),Formation Solutions Evaluation (ms),Total (ms), GA vs. Full\n");
		out_f.close();

		System.out.println("Parameters:" + ", appliances=" + NUM_APPLIANCES
				+ ", services=" + NUM_SERVICES + ", providers=" + NUM_PROVIDERS
				+ ", components=" + NUM_COMPONENTS);

		for (int i = 1; i <= NUM_COMPONENTS; i++)
			computeSolution(i);
	}

	private static void computeSolution(int numComps) throws Exception {

		List<Component> components = new ArrayList<Component>();
		for (int i = 1; i <= numComps; i++)
			components.add(new Component("compo_" + i));
		for (Component c : components)
			c.setConnectedComponents(new HashSet<Component>(components));

		// Map<Component, CombinationTotalValue> componentSolutions = new
		// HashMap<Component, CombinationTotalValue>();
		// current work:
		Map<Component, SortedSet<ComponentSolution>> componentSolutions = new HashMap<Component, SortedSet<ComponentSolution>>();
		for (int i = STEP_SIZE; i <= NUM_APPLIANCES; i = i + STEP_SIZE)
			for (int j = STEP_SIZE; j <= NUM_SERVICES; j = j + STEP_SIZE) {
				System.out.println("\n\n\n---------------------------");
				System.out.println("Running Components=" + components.size()
						+ ", AMIs=" + i + ", Services=" + j);

				TIME_COMPONENTS_TOTAL = 0L;
				for (Component c : components) {
					// computeSolutions(c, i, j);
					componentSolutions.put(c, computeComponent(c, i, j));

				}
				if (components.size() > 1) {
					System.out.println("---------------------------");
					System.out.print("naive best formation solution: ");
					for (Component c : components)
						System.out
								.print(componentSolutions.get(c).last() + " ");
					System.out.println();
					System.out.println("---------------------------");

					// current work:
					System.out
							.println("---------------------------\nglobal best formation solution: "
									+ computeFormations(componentSolutions,
											numComps, i, j).last());
					System.out.println("---------------------------");
				}

				// got all results, now clear it:
				componentSolutions = new HashMap<Component, SortedSet<ComponentSolution>>();

			}
	}

	private static SortedSet<FormationValue> computeGABestFormation(
			final Map<Component, SortedSet<ComponentSolution>> solutionSpace,
			FormationDecision formationDecision) {
		FormationAlternativeCandidateFactory candidateFactory = new FormationAlternativeCandidateFactory(
				solutionSpace);
		STEvolutionEngine<FormationAlternative> stee = new STEvolutionEngine<FormationAlternative>(
				candidateFactory, new FormationAlternativeEvolutionaryOperator(
						solutionSpace),
				new MahoutFitnessEvaluator<FormationAlternative>(
						new FormationAlternativeFitnessEvaluatorParallel(
								formationDecision)),
				new FormationAlternativeSelectionStrategy(), new Random());

		int compSolutionsSize = new ArrayList<ComponentSolution>(solutionSpace
				.values().iterator().next()).size();
		int populationSize = GA_POPULATION_SIZE > Math.pow(compSolutionsSize,
				solutionSpace.size()) ? new Double(Math.pow(compSolutionsSize,
				solutionSpace.size())).intValue() : GA_POPULATION_SIZE;
		int eliteCount = new Double(Math.ceil(populationSize * GA_ELITE_FACTOR))
				.intValue();
		double solutionSpaceSize = Math.pow(compSolutionsSize,
				solutionSpace.size());

		System.out.println("Calulating GA with population of " + populationSize
				+ " identifying " + eliteCount + " elite soldiers from "
				+ solutionSpaceSize + " solutions in "
				+ Math.ceil(solutionSpaceSize / populationSize * 2)
				+ " generations.");
		List<EvaluatedCandidate<FormationAlternative>> elite = stee
				.evolvePopulation(populationSize, eliteCount,
						new FormationAlternativeTerminator(solutionSpace));

		SortedSet<FormationValue> result = new TreeSet<FormationValue>();
		for (EvaluatedCandidate<FormationAlternative> ec : elite)
			result.add(new FormationValue(ec.getCandidate().getFormation()
					.getComponentSolutions(), ec.getFitness()));
		return result;
	}

	private static SortedSet<FormationValue> computeFormations(
			Map<Component, SortedSet<ComponentSolution>> componentSolutions,
			int numComps, int numAMIs, int numServices) throws Exception {

		// System.out.print("formation (size="
		// + fs.getComponentSolutionMap().size() + "): " + fs);
		long startTimeFormation = new Date().getTime();
		FormationDecision fd = new FormationDecision();
		fd.setName("Combined Decision");

		Goal valuableFormation = new Goal("Highest Sum Value of Components");
		valuableFormation.setGoalType(GoalType.POSITIVE);
		Criterion formationValue = new Criterion("Value");
		formationValue.setType(CriterionType.QUANTITATIVE);
		valuableFormation.addChild(formationValue);
		fd.addGoal(valuableFormation);

		Goal cheapestFormation = new Goal(
				"Cheapest Inter-Connection Costs Formation");
		cheapestFormation.setGoalType(GoalType.NEGATIVE);
		Criterion formationCosts = new Criterion(
				"Inter-Connection Traffic Costs");
		formationCosts.setType(CriterionType.QUANTITATIVE);
		cheapestFormation.addChild(formationCosts);
		fd.addGoal(cheapestFormation);

		List<FormationSolution> fs = new ArrayList<FormationSolution>(
				cartesianProduct(new ArrayList<Set<ComponentSolution>>(
						componentSolutions.values())));

		long endTimeFormation = new Date().getTime();

		long startTimeFormationEval = new Date().getTime();
		SortedSet<FormationValue> result = new TreeSet<FormationValue>();
		long endTimeFormationEval = new Date().getTime();

		FormationValue bestGA = null;
		FormationValue bestFull = null;

		if ((COMPARISON && fs.size() > 1)
				|| 10 * GA_POPULATION_SIZE < fs.size()
				|| Math.pow(16, 3) < fs.size()) {
			long endTimeFormationGA = new Date().getTime();
			System.out.println("Formation Decision Model took "
					+ (endTimeFormationGA - startTimeFormation) + " ms");

			startTimeFormationEval = new Date().getTime();
			result = computeGABestFormation(componentSolutions, fd);
			endTimeFormationEval = new Date().getTime();
			System.out.println("Formation Solutions Eval took "
					+ (endTimeFormationEval - startTimeFormationEval) + " ms");
			System.out.println("Worst Formation from GA Elite: "
					+ result.first());
			System.out
					.println("Best Formation from GA Elite: " + result.last());
			bestGA = result.last();
		}
		if (Math.pow(16, 3) >= fs.size()) {
			long startTimeFormationFull = new Date().getTime();
			fd = createFormationDecisionAlternatives(fd, fs);

			List<FormationAlternative> formAlts = fd.getAlternatives();
			List<Evaluation> formationEvaluations = createFormationEvaluations(formAlts);

			long endTimeFormationFull = new Date().getTime();
			System.out.println("Formation Solutions Model took "
					+ (endTimeFormation - startTimeFormation
							+ endTimeFormationFull - startTimeFormationFull)
					+ " ms (for " + fs.size() + " formation solutions)");
			endTimeFormation = endTimeFormation
					+ (endTimeFormationFull - startTimeFormationFull);

			startTimeFormationEval = new Date().getTime();

			System.out.println("preparing AHP...");
			AnalyticHierarchyProcess ahpFormation = new AnalyticHierarchyProcess(
					fd);
			ahpFormation.calculateWeights();
			ahpFormation.calculateAlternativeValues(formationEvaluations);

			System.out.println("creating threads...");
			ExecutorService exec = Executors.newFixedThreadPool(16);
			List<Future<Void>> futures = new ArrayList<Future<Void>>();
			for (int i = 0; i < fd.getAlternatives().size(); i++) {
				Callable<Void> c = new EvaluationThread(ahpFormation, i);
				futures.add(exec.submit(c));
			}
			exec.shutdown();
			System.out.println("returning Voids...");
			for (Future<Void> f : futures)
				f.get();

			System.out.println("calculating indices...");
			result = createSortedFormationResults(ahpFormation
					.calculateIndices());

			endTimeFormationEval = new Date().getTime();
			System.out.println("Formation Solutions Eval took "
					+ (endTimeFormationEval - startTimeFormationEval)
					+ " ms (for " + fs.size() + " formation solutions)");

			System.out.println("Worst Formation Solution: " + result.first());
			System.out.println("Best Formation Solution: " + result.last());
			bestFull = result.last();

		}

		TIME_TOTAL = TIME_COMPONENTS_TOTAL
				+ (endTimeFormation - startTimeFormation)
				+ (endTimeFormationEval - startTimeFormationEval);

		FileWriter fw = new FileWriter("out_formation.txt", true);
		BufferedWriter out = new BufferedWriter(fw);
		out.write("" + numComps + "," + numAMIs + "," + numServices + ","
				+ (endTimeFormation - startTimeFormation) + ","
				+ (endTimeFormationEval - startTimeFormationEval) + ","
				+ TIME_TOTAL + "," + compareGAFull(fd, bestGA, bestFull) + "\n");
		out.close();

		return result;
	}

	private static Double compareGAFull(FormationDecision fd,
			FormationValue bestGA, FormationValue bestFull) {

		if (bestGA == null || bestFull == null)
			return null;

		FormationSolution fsGA = new FormationSolution();
		fsGA.setComponentSolutions(bestGA.getComponentSolutions());
		FormationSolution fsFull = new FormationSolution();
		fsFull.setComponentSolutions(bestFull.getComponentSolutions());
		List<FormationSolution> fs = new ArrayList<FormationSolution>();
		fs.add(fsGA);
		fs.add(fsFull);
		System.out.println("Comparing following solutions: GA:" + fsGA
				+ ", fsFull:" + fsFull);

		AnalyticHierarchyProcess ahp = new AnalyticHierarchyProcess(
				createFormationDecisionAlternatives(fd, fs));
		EvaluationResult formationEvalResult = null;
		try {
			formationEvalResult = ahp.evaluateFull(
					createFormationEvaluations(fd.getAlternatives()), 15, true);
		} catch (Exception e) {
			e.printStackTrace();
		}

		SortedSet<FormationValue> results = createSortedFormationResults(formationEvalResult);
		FormationValue previous = null;
		double bestGAValue = 1D;
		double bestFullValue = 1D;
		for (FormationValue fv : results) {
			if (previous != null)
				System.out.println("Comparison prev: diff: "
						+ (previous.getFormationValue() - fv
								.getFormationValue())
						+ ", ratio: "
						+ (previous.getFormationValue() / fv
								.getFormationValue()));
			previous = fv;
			if (fv.equals(bestGA))
				bestGAValue = fv.getFormationValue();
			if (fv.equals(bestFull))
				bestFullValue = fv.getFormationValue();
		}
		System.out.println("Comparison best: diff: "
				+ (bestGAValue - bestFullValue) + ", ratio: "
				+ (bestGAValue / bestFullValue));

		return bestGAValue / bestFullValue;

	}

	private static FormationDecision createFormationDecisionAlternatives(
			FormationDecision fd, List<FormationSolution> fs) {
		fd.getAlternatives().clear();

		for (int i = 0; i < fs.size(); i++) {

			double networkSendValue = 0D;
			double networkRecieveValue = 0D;

			List<ComponentSolution> comps = new ArrayList<ComponentSolution>(fs
					.get(i).getComponentSolutions());
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
			for (ComponentSolution cs : fs.get(i).getComponentSolutions()) {
				value += cs.getCombinationTotalValue().getTotalValue(); //
				// System.out.println("value: " + value);
			}
			fs.get(i)
					.getAttributes()
					.add(new Attribute<Double>(
							EFormationValueAttribute.NETWORK_COST_RECIEVE,
							networkSendValue));
			fs.get(i)
					.getAttributes()
					.add(new Attribute<Double>(
							EFormationValueAttribute.NETWORK_COST_SEND,
							networkRecieveValue));
			fs.get(i)
					.getAttributes()
					.add(new Attribute<Double>(EFormationValueAttribute.VALUE,
							value));
			FormationAlternative fa = new FormationAlternative(
					"formation_" + i, fs.get(i));
			fd.addAlternative(fa);
		}

		return fd;
	}

	private static List<Evaluation> createFormationEvaluations(
			List<FormationAlternative> formAlternatives) {
		List<Evaluation> formationEvaluations = new ArrayList<Evaluation>();

		try {
			Evaluation formationValueEvaluation = new Evaluation();
			formationValueEvaluation.getEvaluations().add(
					createFormationValueMatrixParallel(formAlternatives));
			Evaluation formationTrafficEvaluation = new Evaluation();
			formationTrafficEvaluation.getEvaluations().add(
					createFormationTrafficMatrixParallel(formAlternatives));
			formationEvaluations.add(formationValueEvaluation);
			formationEvaluations.add(formationTrafficEvaluation);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return formationEvaluations;
	}

	private static SortedSet<FormationValue> createSortedFormationResults(
			EvaluationResult formationEvalResult) {
		SortedSet<FormationValue> result = new TreeSet<FormationValue>();

		for (Alternative a : formationEvalResult
				.getResultMultiplicativeIndexMap().keySet()) {
			Double value = formationEvalResult
					.getResultMultiplicativeIndexMap().get(a);
			result.add(new FormationValue(((FormationAlternative) a)
					.getFormation().getComponentSolutions(), value));
		}

		return result;
	}

	private static SortedSet<ComponentSolution> computeComponent(Component c,
			int numAMIs, int numServices) throws Exception {

		long startTimeAMIModel = new Date().getTime();
		List<AMI> amis = new ArrayList<AMI>();
		for (int i = 0; i < numAMIs; i++) {
			AMI ami = new AMI("ami-" + i);
			ami.getAttributes().add(
					new Attribute<Double>(EApplianceAttribute.COSTPERHOUR,
							new Double((Math.random() * 0.4 + 0.1))));
			ami.getAttributes().add(
					new Attribute<Double>(EApplianceAttribute.POPULARITY,
							new Double(Math.random())));
			amis.add(ami);

		}

		// System.out.println("Generated AMIs: " + amis);

		ApplianceDecision ad = new ApplianceDecision();
		ad.setName("AMI Decision");

		Goal bestAppliance = new Goal("Best Appliance");
		bestAppliance.setGoalType(GoalType.POSITIVE);
		Criterion appliancePopularity = new Criterion("Appliance Popularity");
		appliancePopularity.setType(CriterionType.QUANTITATIVE);
		bestAppliance.addChild(appliancePopularity);

		Goal cheapestAppliance = new Goal("Cheapest Appliance");
		cheapestAppliance.setGoalType(GoalType.NEGATIVE);
		Criterion applianceCosts = new Criterion("Appliance Costs");
		applianceCosts.setType(CriterionType.QUANTITATIVE);
		cheapestAppliance.addChild(applianceCosts);

		ad.addGoal(bestAppliance);
		ad.addGoal(cheapestAppliance);

		List<ApplianceAlternative> applianceAlternatives = new ArrayList<ApplianceAlternative>();
		for (AMI a : amis) {
			ApplianceAlternative aa = new ApplianceAlternative(a,
					"alternative-" + a.getName());
			applianceAlternatives.add(aa);
			ad.addAlternative(aa);
		}

		// System.out.println("AMI Decision: " + ad);

		List<Evaluation> amiEvaluations = new ArrayList<Evaluation>();
		Evaluation evBest = new Evaluation();
		evBest.getEvaluations().add(
				createAMIMatrix(applianceAlternatives,
						EApplianceAttribute.POPULARITY));
		Evaluation evCheapest = new Evaluation();
		evCheapest.getEvaluations().add(
				createAMIMatrix(applianceAlternatives,
						EApplianceAttribute.COSTPERHOUR));
		amiEvaluations.add(evBest);
		amiEvaluations.add(evCheapest);

		long endTimeAMIModel = new Date().getTime();
		System.out.println("AMI Model Creation took "
				+ (endTimeAMIModel - startTimeAMIModel) + " ms");

		long startTimeAMIEval = new Date().getTime();
		AnalyticHierarchyProcess ahpAMI = new AnalyticHierarchyProcess(ad);
		EvaluationResult amiEvalResult = ahpAMI.evaluateFull(amiEvaluations);
		long endTimeAMIEval = new Date().getTime();
		System.out.println("AMI Evaluation took "
				+ (endTimeAMIEval - startTimeAMIEval) + " ms");

		long startTimeServiceModel = new Date().getTime();
		List<EC2Resource> services = new ArrayList<EC2Resource>();
		for (int i = 0; i < numServices; i++) {
			EC2Resource ec2 = new EC2Resource("ec2-" + i);
			ec2.getAttributes().add(
					new Attribute<Double>(EComputeServiceAttribute.COSTPERHOUR,
							new Double((Math.random() * 0.39 + 0.01))));
			ec2.getAttributes().add(
					new Attribute<Double>(
							EComputeServiceAttribute.CPUBENCHMARK, new Double(
									Math.random() * 1000)));
			ec2.getAttributes().add(
					new Attribute<Double>(
							EComputeServiceAttribute.RAMBENCHMARK, new Double(
									Math.random() * 1000)));
			ec2.getAttributes().add(
					new Attribute<Double>(
							EComputeServiceAttribute.DISKBENCHMARK, new Double(
									Math.random() * 1000)));
			ec2.getAttributes().add(
					new Attribute<Double>(EComputeServiceAttribute.MAXLATENCY,
							new Double(Math.random() * 450 + 50)));
			ec2.getAttributes().add(
					new Attribute<Double>(EComputeServiceAttribute.AVGLATENCY,
							new Double(Math.random() * 490 + 10)));
			ec2.getAttributes().add(
					new Attribute<Double>(
							EComputeServiceAttribute.SERVICEPOPULARITY,
							new Double(Math.random())));
			ec2.getAttributes().add(
					new Attribute<Double>(EComputeServiceAttribute.UPTIME,
							new Double(Math.random() * 0.1 + 0.9)));

			ec2.setProvider(providers.get(new Random().nextInt(NUM_PROVIDERS)));

			services.add(ec2);
		}

		ComputeDecision sd = new ComputeDecision();
		sd.setName("Service Decision");

		Goal bestService = new Goal("Best Service");
		bestService.setGoalType(GoalType.POSITIVE);
		Criterion servicePopularity = new Criterion("Service Popularity");
		servicePopularity.setType(CriterionType.QUANTITATIVE);
		Criterion serviceCPU = new Criterion("Service CPU");
		serviceCPU.setType(CriterionType.BENCHMARK);
		Criterion serviceRAM = new Criterion("Service RAM");
		serviceRAM.setType(CriterionType.BENCHMARK);
		Criterion serviceDisk = new Criterion("Service Disk");
		serviceDisk.setType(CriterionType.BENCHMARK);
		Criterion serviceUptime = new Criterion("Service Uptime");
		serviceUptime.setType(CriterionType.QUANTITATIVE);

		bestService.addChild(servicePopularity);
		bestService.addChild(serviceCPU);
		bestService.addChild(serviceRAM);
		bestService.addChild(serviceDisk);
		bestService.addChild(serviceUptime);

		Goal cheapestService = new Goal("Cheapest Service");
		cheapestService.setGoalType(GoalType.NEGATIVE);
		Criterion serviceCosts = new Criterion("Service Costs");
		serviceCosts.setType(CriterionType.QUANTITATIVE);
		cheapestService.addChild(serviceCosts);

		Goal latencyService = new Goal("Low Latency Service");
		latencyService.setGoalType(GoalType.NEGATIVE);
		Criterion serviceMaxLatency = new Criterion("Service Max Latency");
		serviceMaxLatency.setType(CriterionType.BENCHMARK);
		Criterion serviceAvgLatency = new Criterion("Service Avg Latency");
		serviceAvgLatency.setType(CriterionType.BENCHMARK);
		latencyService.addChild(serviceMaxLatency);
		latencyService.addChild(serviceAvgLatency);

		sd.addGoal(bestService);
		sd.addGoal(cheapestService);
		sd.addGoal(latencyService);

		List<ComputeServiceAlternative> ec2Alternatives = new ArrayList<ComputeServiceAlternative>();
		for (EC2Resource e : services) {
			ComputeServiceAlternative ea = new ComputeServiceAlternative(e,
					"alternative-" + e.getName());
			ec2Alternatives.add(ea);
			sd.addAlternative(ea);
		}

		List<Evaluation> serviceEvaluations = new ArrayList<Evaluation>();

		Evaluation evServicePopularity = new Evaluation();
		evServicePopularity.getEvaluations().add(
				createServiceMatrix(ec2Alternatives,
						EComputeServiceAttribute.SERVICEPOPULARITY));
		evServicePopularity.getEvaluations().add(
				createServiceMatrix(ec2Alternatives,
						EComputeServiceAttribute.CPUBENCHMARK));
		evServicePopularity.getEvaluations().add(
				createServiceMatrix(ec2Alternatives,
						EComputeServiceAttribute.RAMBENCHMARK));
		evServicePopularity.getEvaluations().add(
				createServiceMatrix(ec2Alternatives,
						EComputeServiceAttribute.DISKBENCHMARK));
		Evaluation evServiceCheapest = new Evaluation();
		evServiceCheapest.getEvaluations().add(
				createServiceMatrix(ec2Alternatives,
						EComputeServiceAttribute.COSTPERHOUR));
		Evaluation evServiceLatency = new Evaluation();
		evServiceLatency.getEvaluations().add(
				createServiceMatrix(ec2Alternatives,
						EComputeServiceAttribute.MAXLATENCY));
		evServiceLatency.getEvaluations().add(
				createServiceMatrix(ec2Alternatives,
						EComputeServiceAttribute.AVGLATENCY));
		serviceEvaluations.add(evServicePopularity);
		serviceEvaluations.add(evServiceCheapest);
		serviceEvaluations.add(evServiceLatency);

		long endTimeServiceModel = new Date().getTime();
		System.out.println("Service Model Creation took "
				+ (endTimeServiceModel - startTimeServiceModel) + " ms");

		long startTimeServiceEval = new Date().getTime();
		AnalyticHierarchyProcess ahpService = new AnalyticHierarchyProcess(sd);
		EvaluationResult serviceEvalResult = ahpService
				.evaluateFull(serviceEvaluations);
		long endTimeServiceEval = new Date().getTime();
		System.out.println("Service Evaluation took "
				+ (endTimeServiceEval - startTimeServiceEval) + " ms");

		long startTimeCombined = new Date().getTime();
		List<CombinationValue> combinations = new ArrayList<CombinationValue>();
		for (ApplianceAlternative aa : applianceAlternatives)
			for (ComputeServiceAlternative csa : ec2Alternatives)
				combinations.add(new CombinationValue(aa.getAppl(), csa
						.getComputeService(), amiEvalResult
						.getResultMultiplicativeIndexMap().get(aa),
						serviceEvalResult.getResultMultiplicativeIndexMap()
								.get(csa)));

		long endTimeCombined = new Date().getTime();
		System.out.println("Combination Model took "
				+ (endTimeCombined - startTimeCombined) + " ms (for "
				+ combinations.size() + " combinations)");
		long startTimeCombinedEval = new Date().getTime();

		SortedSet<ComponentSolution> combinationResults = new TreeSet<ComponentSolution>();
		for (CombinationValue cv : combinations) {
			Double value = cv.getApplianceValue() + cv.getServiceValue();
			combinationResults.add(new ComponentSolution(c,
					new CombinationTotalValue(cv, value)));
		}
		long endTimeCombinedEval = new Date().getTime();
		System.out.println("Combination Eval took "
				+ (endTimeCombinedEval - startTimeCombinedEval) + " ms");

		System.out.println("Worst Combination: " + combinationResults.first());
		System.out.println("Best Combination: " + combinationResults.last());

		TIME_INTERMEDIATE_TOTAL = ((endTimeAMIModel - startTimeAMIModel)
				+ (endTimeAMIEval - startTimeAMIEval)
				+ (endTimeServiceModel - startTimeServiceModel)
				+ (endTimeServiceEval - startTimeServiceEval)
				+ (endTimeCombined - startTimeCombined) + (endTimeCombinedEval - startTimeCombinedEval));
		TIME_COMPONENTS_TOTAL += TIME_INTERMEDIATE_TOTAL;

		FileWriter fw = new FileWriter("out_components.txt", true);
		BufferedWriter out = new BufferedWriter(fw);
		out.write("" + c.getName() + "," + NUM_COMPONENTS + "," + numAMIs + ","
				+ numServices + "," + (endTimeAMIModel - startTimeAMIModel)
				+ "," + (endTimeAMIEval - startTimeAMIEval) + ","
				+ (endTimeServiceModel - startTimeServiceModel) + ","
				+ (endTimeServiceEval - startTimeServiceEval) + ","
				+ (endTimeCombined - startTimeCombined) + ","
				+ (endTimeCombinedEval - startTimeCombinedEval) + ","
				+ TIME_INTERMEDIATE_TOTAL + "\n");
		out.close();

		return combinationResults;
	}

	private static Matrix createAMIMatrix(List<ApplianceAlternative> alt,
			EApplianceAttribute attr) {
		double[][] critEv = new double[alt.size()][alt.size()];
		double c;

		for (int a = 0; a < alt.size(); a++) {
			c = (Double) alt.get(a).getAppl().getAttribute(attr).getValue();
			for (int b = 0; b < alt.size(); b++) {

				critEv[a][b] = c
						/ (Double) alt.get(b).getAppl().getAttribute(attr)
								.getValue();
				// System.out.println("[" + critEv[a][b] + "]");
			}
			// System.out.println("\n");
		}
		Matrix mat = new Matrix(critEv);

		return mat;
	}

	private static Matrix createServiceMatrix(
			List<ComputeServiceAlternative> alt, EComputeServiceAttribute attr) {
		double[][] critEv = new double[alt.size()][alt.size()];
		double c;

		for (int a = 0; a < alt.size(); a++) {
			c = (Double) alt.get(a).getComputeService().getAttribute(attr)
					.getValue();
			for (int b = 0; b < alt.size(); b++) {

				critEv[a][b] = c
						/ (Double) alt.get(b).getComputeService()
								.getAttribute(attr).getValue();
				// System.out.println("[" + critEv[a][b] + "]");
			}
			// System.out.println("\n");
		}
		Matrix mat = new Matrix(critEv);

		return mat;
	}

	private static Matrix createFormationValueMatrix(
			List<FormationAlternative> alt) {
		double[][] critEv = new double[alt.size()][alt.size()];
		double c = 1D;

		for (int a = 0; a < alt.size(); a++) {
			c = (Double) alt.get(a).getFormation()
					.getAttribute(EFormationValueAttribute.VALUE).getValue();

			for (int b = 0; b < alt.size(); b++) {

				double d = (Double) alt.get(b).getFormation()
						.getAttribute(EFormationValueAttribute.VALUE)
						.getValue();
				critEv[a][b] = c / d;
				// System.out.println("[" + critEv[a][b] + "]");
			}
			// System.out.println("\n");
		}
		Matrix mat = new Matrix(critEv);

		return mat;
	}

	private static Matrix createFormationValueMatrixParallel(
			List<FormationAlternative> alt) throws Exception {
		// final double[][] critEv = new double[alt.size()][alt.size()];
		Matrix mat = new Matrix(alt.size(), alt.size());

		System.out.println("  creating formation value matrix threads...");
		ExecutorService exec = Executors.newFixedThreadPool(16);
		List<Future<Void>> futures = new ArrayList<Future<Void>>();
		for (int a = 0; a < alt.size(); a++) {
			Callable<Void> c = new EvaluationFormationValueMatrixThread(a, alt,
					mat);
			futures.add(exec.submit(c));
		}
		exec.shutdown();

		System.out.println("  returning formation value matrix Voids...");
		for (Future<Void> f : futures)
			f.get();
		exec = null;
		System.gc();

		return mat;
	}

	private static Matrix createFormationTrafficMatrix(
			List<FormationAlternative> alt) {
		double[][] critEv = new double[alt.size()][alt.size()];
		double c = 1D;

		for (int a = 0; a < alt.size(); a++) {
			c = (Double) alt
					.get(a)
					.getFormation()
					.getAttribute(EFormationValueAttribute.NETWORK_COST_RECIEVE)
					.getValue()
					+ (Double) alt
							.get(a)
							.getFormation()
							.getAttribute(
									EFormationValueAttribute.NETWORK_COST_SEND)
							.getValue();

			for (int b = 0; b < alt.size(); b++) {

				double d = (Double) alt
						.get(b)
						.getFormation()
						.getAttribute(
								EFormationValueAttribute.NETWORK_COST_RECIEVE)
						.getValue()
						+ (Double) alt
								.get(a)
								.getFormation()
								.getAttribute(
										EFormationValueAttribute.NETWORK_COST_SEND)
								.getValue();
				critEv[a][b] = c / d;
				// System.out.println("[" + critEv[a][b] + "]");
			}
			// System.out.println("\n");
		}
		Matrix mat = new Matrix(critEv);

		return mat;
	}

	private static Matrix createFormationTrafficMatrixParallel(
			List<FormationAlternative> alt) throws Exception {
		Matrix mat = new Matrix(alt.size(), alt.size());

		System.out.println("  creating formation traffic matrix threads...");
		ExecutorService exec = Executors.newFixedThreadPool(16);
		List<Future<Void>> futures = new ArrayList<Future<Void>>();
		for (int a = 0; a < alt.size(); a++) {
			Callable<Void> c = new EvaluationFormationTrafficMatrixThread(a,
					alt, mat);
			futures.add(exec.submit(c));
		}

		exec.shutdown();

		System.out.println("  returning formation traffic matrix Voids...");
		for (Future<Void> f : futures)
			f.get();
		exec = null;
		System.gc();

		return mat;
	}

	public static Set<FormationSolution> cartesianProduct(
			List<Set<ComponentSolution>> sets) {
		if (sets.size() < 2)
			throw new IllegalArgumentException(
					"Can't have a product of fewer than two sets (got "
							+ sets.size() + ")");

		return _cartesianProduct(0, sets);
	}

	private static Set<FormationSolution> _cartesianProduct(int index,
			List<Set<ComponentSolution>> sets) {
		Set<FormationSolution> ret = new HashSet<FormationSolution>();
		if (index == sets.size()) {
			ret.add(new FormationSolution());
		} else {
			for (ComponentSolution cs : sets.get(index)) {
				for (FormationSolution fs : _cartesianProduct(index + 1, sets)) {
					fs.getComponentSolutions().add(cs);
					ret.add(fs);
				}
			}
		}
		return ret;
	}

	private static class EvaluationThread implements Callable<Void> {

		private AnalyticHierarchyProcess ahpFormation;
		private int i;

		public EvaluationThread(AnalyticHierarchyProcess ahpFormation, int i) {
			this.ahpFormation = ahpFormation;
			this.i = i;
		}

		@Override
		public Void call() throws Exception {

			// only one Alternative at a time!
			// ahpFormation.getDecision().getAlternatives().clear();
			// ahpFormation.getDecision().addAlternative(formAlternative);

			try {
				ahpFormation.evaluateSingle(this.i);
			} catch (Exception e) {
				e.printStackTrace();
			}

			// System.out.println("thread " + Thread.currentThread() +
			// " has finished with " + result + ".");
			return null;

		}
	}

	private static class EvaluationFormationValueMatrixThread implements
			Callable<Void> {

		private int a;
		private List<FormationAlternative> alt;

		private Matrix eval;

		/**
		 */
		public EvaluationFormationValueMatrixThread(int a,
				List<FormationAlternative> alt, Matrix eval) {
			super();
			this.a = a;
			this.alt = alt;
			this.eval = eval;
		}

		@Override
		public Void call() throws Exception {
			for (int b = 0; b < alt.size(); b++) {
				// synchronized (critEv) {
				eval.set(
						a,
						b,
						(Double) alt.get(a).getFormation()
								.getAttribute(EFormationValueAttribute.VALUE)
								.getValue()
								/ (Double) alt
										.get(b)
										.getFormation()
										.getAttribute(
												EFormationValueAttribute.VALUE)
										.getValue());
				// }
			}
			return null;
		}

	}

	private static class EvaluationFormationTrafficMatrixThread implements
			Callable<Void> {

		private int a;
		private List<FormationAlternative> alt;

		private Matrix eval;

		/**
		 */
		public EvaluationFormationTrafficMatrixThread(int a,
				List<FormationAlternative> alt, Matrix eval) {
			super();
			this.a = a;
			this.alt = alt;
			this.eval = eval;
		}

		@Override
		public Void call() throws Exception {
			for (int b = 0; b < alt.size(); b++) {
				// synchronized (critEv) {
				eval.set(
						a,
						b,
						((Double) alt
								.get(a)
								.getFormation()
								.getAttribute(
										EFormationValueAttribute.NETWORK_COST_SEND)
								.getValue() + (Double) alt
								.get(a)
								.getFormation()
								.getAttribute(
										EFormationValueAttribute.NETWORK_COST_RECIEVE)
								.getValue())
								/ ((Double) alt
										.get(b)
										.getFormation()
										.getAttribute(
												EFormationValueAttribute.NETWORK_COST_RECIEVE)
										.getValue() + (Double) alt
										.get(b)
										.getFormation()
										.getAttribute(
												EFormationValueAttribute.NETWORK_COST_SEND)
										.getValue()));
				// }
			}
			return null;
		}
	}

}
