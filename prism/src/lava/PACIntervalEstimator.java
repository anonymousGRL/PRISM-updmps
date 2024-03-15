package lava;

import common.Interval;
import explicit.Distribution;
import explicit.IMDP;
import explicit.IMDPSimple;
import explicit.MDP;
import param.Function;
import prism.Evaluator;
import prism.Prism;

import java.util.*;

public class PACIntervalEstimator extends MAPEstimator {

	protected double error_tolerance;

    public PACIntervalEstimator(Prism prism, Experiment ex) {
		super(prism, ex);
		error_tolerance = ex.error_tolerance;
		this.name = "PAC";
    }

	@Override
	protected Interval<Double> getTransitionInterval(TransitionTriple t) {
		double point = mode(t);
		//System.out.println("Point = " + point);
		double confidence_interval = confidenceInterval(t);
		//System.out.println("confidence_interval = " + confidence_interval);
		double precision = 1e-8;
		double lower_bound = Math.max(point - confidence_interval, precision);
		double upper_bound = Math.min(point + confidence_interval, 1-precision);
		//System.out.println("confidence interval: " + confidence_interval);
		//System.out.println("[l, u]: [" + lower_bound +", "+  upper_bound+ "]")
		return new Interval<>(lower_bound, upper_bound);
	}

	/**
	 * Get minimum (width) interval for each transition.
	 * @return Map from transition to minimal interval
	 */
	public Map<TransitionTriple, Interval<Double>> computeMinIntervals() {
		Map<Function, List<TransitionTriple>> functionMap = this.getFunctionMap();
		Map<TransitionTriple, Interval<Double>> minIntervalMap = new HashMap<>();

		for (Function func : functionMap.keySet()){
			List<TransitionTriple> transitions = functionMap.get(func);
			List<Interval<Double>> intervals = new ArrayList<>();

			for (TransitionTriple transition : transitions){
				intervals.add(getTransitionInterval(transition));
			}

			Interval<Double> minInterval = Collections.min(intervals, Comparator.comparingDouble(interval -> interval.getUpper() - interval.getLower()));

			for (TransitionTriple transition : transitions){
				minIntervalMap.put(transition, minInterval);
			}
		}

		return minIntervalMap;
	}

	@Override
	public IMDP<Double> buildPointIMDP(MDP<Double> mdp) {
		//System.out.println("Building IMDP");
		int numStates = mdp.getNumStates();
		IMDPSimple<Double> imdp = new IMDPSimple<>(numStates);
		imdp.addInitialState(mdp.getFirstInitialState());
		imdp.setStatesList(mdp.getStatesList());
		imdp.setConstantValues(mdp.getConstantValues());
		imdp.setIntervalEvaluator(Evaluator.forDoubleInterval());

		Map<TransitionTriple, Interval<Double>> minIntervals = computeMinIntervals();

		for (int s = 0; s < numStates; s++) {
			int numChoices = mdp.getNumChoices(s);
			final int state = s;
			for (int i = 0 ; i < numChoices; i++) {
				final String action = getActionString(mdp, s, i);

				Distribution<Interval<Double>> distrNew = new Distribution<>(Evaluator.forDoubleInterval());
				mdp.forEachDoubleTransition(s, i, (int sFrom, int sTo, double p)->{
					TransitionTriple t = new TransitionTriple(state, action, sTo);
					Interval<Double> interval;
					if (true) {
						if (0 < p && p < 1.0) {
							interval = this.ex.optimizations ? minIntervals.get(t) : getTransitionInterval(t);
							//System.out.println("Transition:" + t + " Naive Interval: " + interval + " New Interval: " + minIntervals.get(t));
							//System.out.println("Triple: " + t + " Interval: " + interval);
							distrNew.add(sTo, interval);
							this.intervalsMap.put(t, interval);
						} else if (p == 1.0) {
							interval = new Interval<Double>(p, p);
							distrNew.add(sTo, interval);
							this.intervalsMap.put(t, interval);
						}
					} else {
						if (!this.constantMap.containsKey(t)) {
							interval = this.ex.optimizations ? minIntervals.get(t) : getTransitionInterval(t);
							//System.out.println("Transition:" + t + " Naive Interval: " + interval + " New Interval: " + minIntervals.get(t));
							//System.out.println("Triple: " + t + " Interval: " + interval);
							distrNew.add(sTo, interval);
							this.intervalsMap.put(t, interval);
						} else {
							p = this.constantMap.get(t);
							interval = new Interval<Double>(p, p);
							distrNew.add(sTo, interval);
							this.intervalsMap.put(t, interval);
						}
					}
				});
				imdp.addActionLabelledChoice(s, distrNew, getActionString(mdp, s, i));
			}
		}
		Map<String, BitSet> labels = mdp.getLabelToStatesMap();
		Iterator<Map.Entry<String, BitSet>> it = labels.entrySet().iterator();
		while (it.hasNext()) {
			Map.Entry<String, BitSet> entry = it.next();
			imdp.addLabel(entry.getKey(), entry.getValue());
		}
		this.estimate = imdp;

		return imdp;
	}

	@Override
	public double averageDistanceToSUL() {
		double totalDist = 0.0;

		for (TransitionTriple t : super.trueProbabilitiesMap.keySet()) {
			Interval<Double> interval = this.intervalsMap.get(t);
			double p = super.trueProbabilitiesMap.get(t);
			double dist = maxIntervalPointDistance(interval, p);
			totalDist += dist;
		}

		double averageDist = totalDist / super.trueProbabilitiesMap.keySet().size();
		return averageDist;

	}

	protected Double confidenceInterval(TransitionTriple t) {
		return computePACBound(t);
	}

	private Double computePACBound(TransitionTriple t) {
		double alpha = error_tolerance; // probability of error (i.e. 1-alpha is probability of correctly specifying the interval)
		int m = this.getNumLearnableTransitions();
		//System.out.println("m = " + m);
		int n = getStateActionCount(t.getStateAction());
		alpha = (error_tolerance*(1.0/(double) m));///((double) this.mdp.getNumChoices(t.getStateAction().getState())); // distribute error over all transitions

		double delta = Math.sqrt((Math.log(2 / alpha))/(2*n));
		return delta;
	}
}
