/**
 * 
 */
package de.eorg.cumulusgenius.mahout.ga.formation;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.logging.Logger;

import org.apache.mahout.ga.watchmaker.STFitnessEvaluator;

import de.eorg.cumulusgenius.shared.cloudmapping.logic.ahp.AnalyticHierarchyProcess;
import de.eorg.cumulusgenius.shared.cloudmapping.model.ahp.values.Evaluation;
import de.eorg.cumulusgenius.shared.cloudmapping.model.ahp.values.EvaluationResult;
import de.eorg.cumulusgenius.shared.cloudmapping.model.jama.Matrix;
import de.eorg.cumulusgenius.shared.cloudmapping.model.mapping.EFormationValueAttribute;
import de.eorg.cumulusgenius.shared.cloudmapping.model.mapping.FormationAlternative;
import de.eorg.cumulusgenius.shared.cloudmapping.model.mapping.FormationDecision;

/**
 * @author mugglmenzel
 * 
 */
public class FormationAlternativeFitnessEvaluatorParallel extends
		STFitnessEvaluator<FormationAlternative> {

	private static Logger log = Logger
			.getLogger(FormationAlternativeFitnessEvaluatorParallel.class
					.getName());

	private FormationDecision formationDecision;
	
	/**
	 * @param formationDecision
	 */
	public FormationAlternativeFitnessEvaluatorParallel(
			FormationDecision formationDecision) {
		super();
		this.formationDecision = formationDecision;
	}

	@Override
	public boolean isNatural() {
		return true;
	}

	@Override
	protected void evaluate(List<? extends FormationAlternative> population,
			List<Double> evaluations) {
		log.finer("Evaluator evaluates population: " + population);

		List<FormationAlternative> formationAlts = new ArrayList<FormationAlternative>();
		for (FormationAlternative fa : population)
			formationAlts.add(fa);

		List<Evaluation> formationEvaluations = new ArrayList<Evaluation>();
		try {

			Evaluation formationValueEvaluation = new Evaluation();
			formationValueEvaluation.getEvaluations().add(
					createFormationValueMatrixParallel(formationAlts));
			Evaluation formationTrafficEvaluation = new Evaluation();
			formationTrafficEvaluation.getEvaluations().add(
					createFormationTrafficMatrixParallel(formationAlts));

			formationEvaluations.add(formationValueEvaluation);
			formationEvaluations.add(formationTrafficEvaluation);
		} catch (Exception e) {
			e.printStackTrace();
		}
		formationDecision.getAlternatives().clear();
		formationDecision.getAlternatives().addAll(formationAlts);

		System.out.println("preparing AHP...");
		AnalyticHierarchyProcess ahpFormation = new AnalyticHierarchyProcess(
				formationDecision);
		ahpFormation.calculateWeights();
		try {
			ahpFormation.calculateAlternativeValues(formationEvaluations);
		} catch (Exception e) {
			e.printStackTrace();
		}

		System.out.println("creating threads...");
		ExecutorService exec = Executors.newFixedThreadPool(16);
		List<Future<Void>> futures = new ArrayList<Future<Void>>();
		for (int i = 0; i < formationDecision.getAlternatives().size(); i++) {
			Callable<Void> c = new EvaluationThread(ahpFormation, i);
			futures.add(exec.submit(c));
		}
		exec.shutdown();
		System.out.println("returning Voids...");
		for (Future<Void> f : futures)
			try {
				f.get();
			} catch (InterruptedException e) {
				e.printStackTrace();
			} catch (ExecutionException e) {
				e.printStackTrace();
			}

		System.out.println("calculating indices...");
		EvaluationResult er = ahpFormation.calculateIndices();

		if (er != null) {
			// evaluations = new ArrayList<Double>();
			for (FormationAlternative fa : population) {
				evaluations.add(er.getResultMultiplicativeIndexMap().get(fa));
			}
			log.fine("evaluation results " + evaluations);
		}
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
