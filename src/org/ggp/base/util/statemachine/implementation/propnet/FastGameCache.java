package org.ggp.base.util.statemachine.implementation.propnet;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Set;

import org.ggp.base.util.gdl.grammar.Gdl;
import org.ggp.base.util.gdl.grammar.GdlConstant;
import org.ggp.base.util.gdl.grammar.GdlRelation;
import org.ggp.base.util.gdl.grammar.GdlSentence;
import org.ggp.base.util.propnet.architecture.Component;
import org.ggp.base.util.propnet.architecture.PropNet;
import org.ggp.base.util.propnet.architecture.components.And;
import org.ggp.base.util.propnet.architecture.components.Not;
import org.ggp.base.util.propnet.architecture.components.Or;
import org.ggp.base.util.propnet.architecture.components.Proposition;
import org.ggp.base.util.propnet.architecture.components.Transition;
import org.ggp.base.util.propnet.factory.OptimizingPropNetFactory;
import org.ggp.base.util.statemachine.MachineState;
import org.ggp.base.util.statemachine.Move;
import org.ggp.base.util.statemachine.Role;
import org.ggp.base.util.statemachine.StateMachine;
import org.ggp.base.util.statemachine.exceptions.GoalDefinitionException;
import org.ggp.base.util.statemachine.exceptions.MoveDefinitionException;
import org.ggp.base.util.statemachine.exceptions.TransitionDefinitionException;
import org.ggp.base.util.statemachine.implementation.prover.query.ProverQueryBuilder;


public class FastGameCache {

    private PropNet propNet;
    private ArrayList<Integer> allCounts = null;
    private HashSet<Integer> netState = null;
    private HashSet<Integer> diffs = null;
    private HashSet<Integer> legals = null;
    private HashSet<Integer> trueProps = null;
    private HashSet<Integer> trueBase = null;
    private HashSet<Integer> trueInputs = null;
    private HashSet<Integer> trueTransitions = null;
    private HashSet<Integer> trueGoals = null;
    private int diffCount = 0;

    public FastGameCache(PropNet pn) {
    	propNet = pn;
    	allCounts = pn.getAllCounts();
    	netState = new HashSet<Integer>();
    	diffs = new HashSet<Integer>();
    	legals = new HashSet<Integer>();
    	trueProps = new HashSet<Integer>();
    	trueBase = new HashSet<Integer>();
    	trueInputs = new HashSet<Integer>();
    	trueTransitions = new HashSet<Integer>();
    	trueGoals = new HashSet<Integer>();
    }

    public FastGameCache(PropNet pn, HashSet<Integer> ns,HashSet<Integer> df, HashSet<Integer> lg, HashSet<Integer> tp, HashSet<Integer> tb, HashSet<Integer> ti, HashSet<Integer> tt, HashSet<Integer> tg)
    {
    	propNet =pn;
    	allCounts = pn.getAllCounts();
    	netState = new HashSet<Integer>(ns);
    	diffs = new HashSet<Integer>(df);
    	legals = new HashSet<Integer>(lg);
    	trueProps = new HashSet<Integer>(tp);
    	trueBase = new HashSet<Integer>(tb);
    	trueInputs = new HashSet<Integer>(ti);
    	trueTransitions = new HashSet<Integer>(tt);
    	trueGoals = new HashSet<Integer>(tg);
    }
    public FastGameCache duplicate() {
    	FastGameCache newb = new FastGameCache(propNet,netState,diffs,legals,trueProps,trueBase,trueInputs,trueTransitions,trueGoals);
    	return newb;
    }
    public void clearDiffs() {
    	diffs = new HashSet<Integer>();
    }
    public void notifyUpdate(Integer i, boolean b) {
    	boolean currentV = netState.contains(i);
    	boolean currentD = diffs.contains(i);
    	boolean different = (currentV != b);
    	if (different && ! currentD) {
    		notifyDiff(i);
    	}
    	else if (!different && currentD) {
    		diffs.remove(i);
    		diffCount = diffCount - allCounts.get(i.intValue());
    	}
    }
    public void notifyDiff(Integer i) {
    	diffs.add(i);
    	diffCount = diffCount + allCounts.get(i.intValue());
    }

    public void notifyMoves(Set<Integer> is) {
    	for (Integer i : trueInputs) {
			notifyUpdate(i,false);
    	}
    	for (Integer i : is) {
			notifyUpdate(i,true);
    	}
    }

    public void justUpdate(Integer i, boolean b) {
		Component c = propNet.findComponent(i);
		if (b) {
	    	netState.add(i);
			if (c.getSp()=="LEGAL") {
				legals.add(i);
			}
			else if (c.getSp()=="BASE") {
				trueBase.add(i);
			}
			else if (c.getSp()=="INPUT") {
				trueInputs.add(i);
			}
			else if (c.getSp()=="TRANS") {
				trueTransitions.add(i);
			}
			else if (c.getSp()=="GOAL") {
				trueGoals.add(i);
			}
		}
		else {
			netState.remove(i);
			if (c.getSp()=="LEGAL") {
				legals.remove(i);
			}
			else if (c.getSp()=="BASE") {
				trueBase.remove(i);
			}
			else if (c.getSp()=="INPUT") {
				trueInputs.remove(i);
			}
			else if (c.getSp()=="TRANS") {
				trueTransitions.remove(i);
			}
			else if (c.getSp()=="GOAL") {
				trueGoals.remove(i);
			}
		}
    }
    public Set<Integer> getState() {
    	return new HashSet<Integer>(netState);
    }
    public PriorityQueue<Integer> getDiffs() {
    	return new PriorityQueue<Integer>(diffs);
    }
    public int getDiffCount() {
    	return diffCount;
    }
    public String stringBase() {
    	String s="";
    	for (Integer i : trueBase) {
    		s = s.concat("[");
    		s=s.concat(propNet.findComponent(i.intValue()).toString());
    		s=s.concat("]");
    	}
    	return s;
    }
    @Override
	public String toString() {
    	String s="";
    	s = s.concat("Diffs:").concat(String.valueOf(arg0))
    	s = s.concat(stringBase());

    }

}
