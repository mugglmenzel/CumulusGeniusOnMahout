/**
 * 
 */
package de.eorg.cumulusgenius.mahout.ga.formation;

import java.util.ArrayList;
import java.util.List;
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
public class FormationAlternativeFitnessEvaluator extends
		STFitnessEvaluator<FormationAlternative> {

	private static Logger log = Logger
			.getLogger(FormationAlternativeFitnessEvaluator.class.getName());

	private FormationDecision formationDecision;

	/**
	 * @param formationDecision
	 */
	public FormationAlternativeFitnessEvaluator(
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

		Evaluation formationValueEvaluation = new Evaluation();
		formationValueEvaluation.getEvaluations().add(
				createFormationValueMatrix(formationAlts));
		Evaluation formationTrafficEvaluation = new Evaluation();
		formationTrafficEvaluation.getEvaluations().add(
				createFormationTrafficMatrix(formationAlts));
		List<Evaluation> formationEvaluations = new ArrayList<Evaluation>();
		formationEvaluations.add(formationValueEvaluation);
		formationEvaluations.add(formationTrafficEvaluation);

		formationDecision.getAlternatives().clear();
		formationDecision.getAlternatives().addAll(formationAlts);

		AnalyticHierarchyProcess ahp = new AnalyticHierarchyProcess(
				formationDecision);

		EvaluationResult er = null;
		try {
			er = ahp.evaluateFull(formationEvaluations, 15, true);
		} catch (Exception e) {
			e.printStackTrace();
		}

		if (er != null) {
			// evaluations = new ArrayList<Double>();
			for (FormationAlternative fa : population) {
				evaluations.add(er.getResultMultiplicativeIndexMap().get(fa));
			}
			log.fine("evaluation results " + evaluations);
		}
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
}
