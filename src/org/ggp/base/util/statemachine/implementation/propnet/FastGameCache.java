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
    private HashSet<Integer> diffs = null;
    private HashSet<Integer> legals = null;
    private HashSet<Integer> netState = null;
    private HashSet<Integer> trueProps = null;
    private HashSet<Integer> trueBase = null;
    private HashSet<Integer> trueInputs = null;
    private HashSet<Integer> trueTransitions = null;

    private int nComponents = 0;

    public FastGameCache(PropNet pn) {
    	propNet = pn;
    	nComponents = pn.getSize();
    	netState = new HashSet<Integer>();
    	diffs = new HashSet<Integer>();
    	legals = new HashSet<Integer>();
    	trueProps = new HashSet<Integer>();
    	trueInputs = new HashSet<Integer>();
    	trueTransitions = new HashSet<Integer>();
    }

    public FastGameCache duplicate() {
    	FastGameCache newb = new FastGameCache(propNet);
    	return newb;
    }



}
