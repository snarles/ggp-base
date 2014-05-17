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


@SuppressWarnings("unused")
public class FastPropNetMachine extends StateMachine {
    /** The underlying proposition network  */
	private boolean diagnosticMode = true;
    private PropNet propNet;
    /** The topological ordering of the propositions */
    //private List<Proposition> ordering;
    /** The player roles */
    private List<Role> roles;
    private Set<Integer> netState = null;
    private PriorityQueue<Integer> deltaState = null;
    private Set<Integer> deltaTrans = null;
    private boolean resolved = false;
    private MachineState currentState = null;
    private List<FastGameCache> gameCaches = null;

    @Override
    public void initialize(List<Gdl> description) {
        try {
			propNet = OptimizingPropNetFactory.create(description, false);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
        propNet.topoSort();
        propNet.labelComponents();
        roles = propNet.getRoles();
        netState = new HashSet<Integer>();
        gameCaches = new ArrayList<FastGameCache>();
        for (Component c : propNet.getComponentsS()) {
        	deltaState.add(c.getIdInt());
        }
    }


	@Override
	public boolean isTerminal(MachineState state) {
		// TODO: Compute whether the MachineState is terminal.
		return false;
	}

	@Override
	public int getGoal(MachineState state, Role role)
	throws GoalDefinitionException {
		// TODO: Compute the goal for role in state.
		return -1;
	}


//	@Override
//	public MachineState getInitialState() {
//		// TODO: Compute the initial state.
//		goToInitial();
//		currentState = getStateFromBase();
//		return null;
//	}
//
//
//	public void goToInitial() {
//		netState = new boolean[propNet.getSize()];
//
//		deltaState = new PriorityQueue<Integer>();
//		Integer i0 = new Integer(propNet.getInitProposition().getId());
//		deltaState.add(i0);
//		netState[i0.intValue()] = true;
//		resolved = false;
//		resolve();
//		goToNext();
//		netState[i0.intValue()] = false;
//		deltaState.add(i0);
//		//printNetState();
//		if (diagnosticMode) {
//			printCurrentState();
//		}
//	}



//	public void setState(MachineState state) {
//		currentState = state;
//		Set<GdlSentence> props = state.getContents();
//		netState = new boolean[propNet.getSize()];
//		deltaState = new PriorityQueue<Integer>();
//		for (GdlSentence g : props) {
//			deltaState.add(new Integer(propNet.getProposition(g).getId()));
//		}
//		resolved = false;
//		resolve();
//	}

	// Given updates loaded in deltaState, propagates changes forward
	public void resolve()
	{
		Map<Integer,Set<Integer>> outputMatrix = propNet.getOutputMatrix();
		while (deltaState.size() > 0) {
			Integer i = deltaState.remove();
			boolean currentS= netState.contains(i);
			boolean oldS = currentS;
			Component c = propNet.findComponent(i.intValue());
			Set<Integer> inputs = c.getInputIds();
			if (c instanceof And) {
				currentS = true;
				for (Integer i1 : inputs) {
					currentS = currentS && netState.contains(i1);
				}
			}
			else if (c instanceof Or) {
				currentS = false;
				for (Integer i1 : inputs) {
					currentS = currentS || netState[i1.intValue()];
				}
			}
			else if (c instanceof Not) {
				currentS = true;
				for (Integer i1 : inputs) {
					currentS = ! netState[i1.intValue()];
				}
			}
			else {
				currentS = true;
				for (Integer i1 : inputs) {
					currentS = netState[i1.intValue()];
				}
			}
			if ((currentS != oldS) || c.getLevel()==0) {
				String s = c.toString2();
				s = s.concat(" from ");
				s= s.concat(String.valueOf(oldS));
				s = s.concat(" to ");
				s= s.concat(String.valueOf(currentS));
				printd("Delta:",s);
				netState[i] = currentS;
				Set<Integer> outputs = c.getOutputIds();
				if (! (c instanceof Transition)) {
					for (Integer i1 : outputs) {
						if (! deltaState.contains(i1)) {
							deltaState.add(i1);
						}
					}
				}
				//printNetState();
				if (diagnosticMode) {
					printd("Delta:",s);
					if (! (c instanceof Transition)) {
						for (Integer i1 : outputs) {
							printd("  ->delta:",i1.toString());
						}
					}
				}
				//paused();
			}
		}
		resolved = true;
	}

	public void goToNext() {
		Map<Integer,Set<Integer>> transitionMap = propNet.getTransitionMatrix();
		deltaState = new PriorityQueue<Integer>();
		for (Integer i1 : transitionMap.keySet()) {
			for (Integer i2 : transitionMap.get(i1)) {
				boolean oldV = netState[i2.intValue()];
				boolean newV = netState[i1.intValue()];
				if (oldV != newV) {
					deltaState.add(i2);
					netState[i2.intValue()] = newV;
				}
			}
		}
		resolved = false;
	}



	/**
	 * Computes the legal moves for role in state.
	 */
	//Note: note completely done: inefficient
	@Override
	public List<Move> getLegalMoves(MachineState state, Role role)
	throws MoveDefinitionException {
		if (state.equals(currentState)) {
			Map<Role,Set<Proposition>> legals = propNet.getLegalPropositions();
			Map<Proposition,Proposition> legmap = propNet.getLegalInputMap();
		}
		// TODO: Compute legal moves.
		return null;
	}

	/**
	 * Computes the next state given state and the list of moves.
	 */
	@Override
	public MachineState getNextState(MachineState state, List<Move> moves)
	throws TransitionDefinitionException {
		// TODO: Compute the next state.
		return null;
	}

	//updates current state with moves
	public void updateCurrent(List<Move> moves) {
		List<GdlSentence> dodos = toDoes(moves);
		Map<GdlSentence,Proposition> inputMap = propNet.getInputPropositions();
		Set<Integer> newInputs = new HashSet();
		for (GdlSentence g : dodos) {
			Proposition p = inputMap.get(g);
			newInputs.add(new Integer(p.getId()));
		}
		for (Integer i : oldInputs) {
			netState[i.intValue()] = false;
		}
		for (Integer i : newInputs) {
			netState[i.intValue()] = true;
		}
		Set<Integer> ups = symmDiff(newInputs,oldInputs);
		updateNetState(ups);
		if (diagnosticMode) {
			printCurrentState();
		}
		oldInputs = newInputs;
	}

	public void printCurrentState() {
		currentState = getStateFromBase();
		printd("PropNetState:",currentState.toString());
	}

	public MachineState getCurrentState() {
		currentState = getStateFromBase();
		return currentState;
	}

	public Set<Integer> symmDiff(Set<Integer> s1, Set<Integer> s2) {
		Set<Integer> ans = new HashSet();
		for (Integer i : s1) {
			if (! s2.contains(i)) {
				ans.add(i);
			}
		}
		for (Integer i : s2) {
			if (! s1.contains(i)) {
				ans.add(i);
			}
		}
		return ans;
	}

	// updates the current net state after adding new ints to deltaState
	public void updateNetState(Set<Integer> ups) {
		for (Integer i : ups) {
			deltaState.add(i);
		}
		resolve();
		goToNext();
		//printNetState();

	}

	/* Already implemented for you */
	@Override
	public List<Role> getRoles() {
		return roles;
	}

	/* Helper methods */

	/**
	 * The Input propositions are indexed by (does ?player ?action).
	 *
	 * This translates a list of Moves (backed by a sentence that is simply ?action)
	 * into GdlSentences that can be used to get Propositions from inputPropositions.
	 * and accordingly set their values etc.  This is a naive implementation when coupled with
	 * setting input values, feel free to change this for a more efficient implementation.
	 *
	 * @param moves
	 * @return
	 */
	private List<GdlSentence> toDoes(List<Move> moves)
	{
		List<GdlSentence> doeses = new ArrayList<GdlSentence>(moves.size());
		Map<Role, Integer> roleIndices = getRoleIndices();

		for (int i = 0; i < roles.size(); i++)
		{
			int index = roleIndices.get(roles.get(i));
			doeses.add(ProverQueryBuilder.toDoes(roles.get(i), moves.get(index)));
		}
		return doeses;
	}

	/**
	 * Takes in a Legal Proposition and returns the appropriate corresponding Move
	 * @param p
	 * @return a PropNetMove
	 */
	public static Move getMoveFromProposition(Proposition p)
	{
		return new Move(p.getName().get(1));
	}

	/**
	 * Helper method for parsing the value of a goal proposition
	 * @param goalProposition
	 * @return the integer value of the goal proposition
	 */
    private int getGoalValue(Proposition goalProposition)
	{
		GdlRelation relation = (GdlRelation) goalProposition.getName();
		GdlConstant constant = (GdlConstant) relation.get(1);
		return Integer.parseInt(constant.toString());
	}

	/**
	 * A Naive implementation that computes a PropNetMachineState
	 * from the true BasePropositions.  This is correct but slower than more advanced implementations
	 * You need not use this method!
	 * @return PropNetMachineState
	 */
	public MachineState getStateFromBase()
	{
		Set<GdlSentence> contents = new HashSet<GdlSentence>();
		for (Proposition p : propNet.getBasePropositions().values())
		{
			int i = p.getId();
			if (netState[i])
			{
				contents.add(p.getName());
			}

		}
		return new MachineState(contents);
	}

	public PropNet getPropNet()
	{
		return propNet;
	}

	public boolean[] getNetState() {
		return netState;
	}

	public void printNetState() {
		if (diagnosticMode) {
			for (int i = 0; i < propNet.getSize(); i++) {
				String s = "";
				if (netState[i]) {
					s=s.concat("[ X ]");
				}
				else {
					s=s.concat("| . |");
				}
				if (deltaState.contains(new Integer(i))) {
					s=s.concat("[ X ]");
				}
				else {
					s=s.concat("| . |");
				}
				s=s.concat(propNet.getComponentsS().get(i).toString2());
				System.out.println(s);
			}
		}
	}

	public void paused() {
		if (diagnosticMode) {
			BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
			System.out.println("[PAUSED]");
			try {
				String gameFile = in.readLine();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}

	public void printd(String s, String t) {
		if (diagnosticMode) {
			System.out.println(s.concat(t));
		}
	}
}
