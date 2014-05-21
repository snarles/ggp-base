package org.ggp.base.util.statemachine.implementation.propnet;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.text.DecimalFormat;
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
public class FuzzyPropNetMachine extends StateMachine {
    /** The underlying proposition network  */
	private boolean diagnosticMode = true;
    private PropNet propNet;
    /** The topological ordering of the propositions */
    //private List<Proposition> ordering;
    /** The player roles */
    private List<Role> roles;
    private List<GdlConstant> roleNames;
    private boolean[] netState = null;
    private double[] fuzzyState = null;
    private MachineState currentState = null;
    private int pnSz = 0; //size of propnet
    private double fuzzy0 = 0.1;
    private double fuzzy1 = 0.9;
    private double expP = 5.0; // exponent for log-sum-exp operation
    Map<GdlSentence, Proposition> baseMap;
    private Set<Integer> legals;
    private Set<Integer> goals;
    private Set<Integer> transitions;
    private Set<Integer> currentInputs;

    //done
    //sets the fuzzy values, also if you want to recalculate
    public void setFuzzy(double d, double e, boolean res) {
    	fuzzy0 = d;
    	fuzzy1 = 1.0-d;
    	expP = e;
    	if (res) {
    		resolve();
    	}
    }


    // builds propnet and initializes
    @Override
    public void initialize(List<Gdl> description) {
    	long start = System.currentTimeMillis();
        try {
			propNet = OptimizingPropNetFactory.create(description, false);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
        propNet.topoSort();
        propNet.labelComponents();
        pnSz = propNet.getSize();
        roles = propNet.getRoles();
        baseMap = propNet.getBasePropositions();
        roleNames = propNet.getRoleNames();
        netState = new boolean[pnSz];
        fuzzyState = new double[pnSz];
        legals = new HashSet<Integer>();
        goals = new HashSet<Integer>();
        currentInputs = new HashSet<Integer>();

        resolve();
        goToNext();
        long elapsed = System.currentTimeMillis()-start;
        printd("Time for LPNM to init:",String.valueOf(elapsed));
    }

    //done
	@Override
	public boolean isTerminal(MachineState state) {
		setState(state);
		int i0 = propNet.getTerminalProposition().getId();
		// TODO: Compute whether the MachineState is terminal.
		return netState[i0];
	}

	// done
	@Override
	public int getGoal(MachineState state, Role role)
	throws GoalDefinitionException {
		// TODO: Compute the goal for role in state.
		int ans = -1;
		boolean flag = false;
		setState(state);
		GdlConstant r = role.getName();
		for (Integer i : goals) {
			Component c = propNet.findComponent(i);
			if (c.getOwner().equals(r)) {
				int newAns = c.getGoal();
				if (flag && (newAns != ans)) {
					System.out.println("!!!GOAL DEF ERROR!!!");
				}
				ans = newAns;
				flag = true;
			}
		}
		return ans;
	}

	public double getFuzzyGoal(MachineState state, Role role) {
		setState(state);
		Set<Proposition> gs = propNet.getGoalPropositions(role);
		double sum = 0;
		for (Proposition p : gs) {
			sum = sum + p.getGoal() * fuzzyState[p.getId()];
		}
		return sum;
	}

	//done
	@Override
	public MachineState getInitialState() {
		// TODO: Compute the initial state.
		goToInitial();
		//printNetState();
		currentState = getStateFromBase();
		return currentState;
	}

	//done
	//called by get Initial..
	public void goToInitial() {
		Integer i0 = new Integer(propNet.getInitProposition().getId());
		netState[i0.intValue()] = true;
		resolve();
		goToNext();
		resolve();
		if (diagnosticMode) {
			printCurrentState();
		}
	}

	//done
	//called by all getGoal, getTerminal, etc.
	public void setState(MachineState state) {
		Set<GdlSentence> props1 = currentState.getContents();
		Set<GdlSentence> props2 = state.getContents();
		if (props1.equals(props2)) {
			return;
		}
		else {
			setState0(state);
			resolve();
		}
	}

	// called by getNextState
	public void setStateMove(MachineState state, List<Move> moves) {
		Set<GdlSentence> props1 = currentState.getContents();
		Set<GdlSentence> props2 = state.getContents();
		List<GdlSentence> dodos = toDoes(moves);
		Map<GdlSentence,Proposition> inputMap = propNet.getInputPropositions();
		Set<Integer> newInputs = new HashSet<Integer>();
		for (GdlSentence g : dodos) {
			Proposition p = inputMap.get(g);
			newInputs.add(new Integer(p.getId()));
		}
		boolean flag = false;
		if (!props1.equals(props2)) {
			setState0(state);
			flag = true;
		}
		if ((! currentInputs.equals(newInputs)) || flag) {
			for (Integer i : currentInputs) {
				netState[i.intValue()] = false;
			}
			for (Integer i : newInputs) {
				netState[i.intValue()] = true;
			}
			resolve();
		}
	}

	// done
	// utility function used by SetState and SetStateMove
	public void setState0(MachineState state) {
		Set<GdlSentence> props1 = currentState.getContents();
		netState = new boolean[pnSz];
		fuzzyState = new double[pnSz];
		Arrays.fill(fuzzyState, fuzzy0);
		for (GdlSentence g : props1) {
			Proposition p = baseMap.get(g);
			netState[p.getId()] = true;
		}
		currentState = state;
	}

	// done
	// Given updates loaded in deltaState, propagates changes forward
	public void resolve()
	{
		fuzzyState = new double[pnSz];
		Arrays.fill(fuzzyState, fuzzy0);
        legals = new HashSet<Integer>();
        goals = new HashSet<Integer>();
        transitions = new HashSet<Integer>();
		for (int i=0; i < pnSz; i++) {
			//printd("Iteration:",String.valueOf(i));
			Component c = propNet.findComponent(i);
			//printd("  comp:",c.toString3());

			String message = c.toString2();
			message = message.concat(String.valueOf(netState[i]));

			Set<Integer> inputs = c.getInputIds();
			if (c instanceof And) {
				netState[i] = true;
				double sum = 0.0;
				for (Integer i1 : inputs) {
					netState[i] = netState[i] && netState[i1.intValue()];
					sum = sum + Math.exp(expP * (1.0-fuzzyState[i1.intValue()]));
				}
				double ans = Math.log(sum)/expP;
				if (ans > fuzzy1) {ans = fuzzy1;}
				if (ans < fuzzy0) {ans = fuzzy0;}
				fuzzyState[i] = 1.0-ans;
			}
			else if (c instanceof Or) {
				netState[i] = false;
				double sum = 0.0;
				for (Integer i1 : inputs) {
					netState[i] = netState[i] || netState[i1.intValue()];
					sum = sum + Math.exp(expP * fuzzyState[i1.intValue()]);
				}
				double ans = Math.log(sum)/expP;
				if (ans > fuzzy1) {ans = fuzzy1;}
				if (ans < fuzzy0) {ans = fuzzy0;}
				fuzzyState[i] = ans;
			}
			else if (c instanceof Not) {
				for (Integer i1 : inputs) {
					netState[i] = ! netState[i1.intValue()];
					fuzzyState[i] = 1.0- fuzzyState[i1.intValue()];
				}
			}
			else {
				for (Integer i1 : inputs) {
					netState[i] = netState[i1.intValue()];
					fuzzyState[i] = fuzzyState[i1.intValue()];
				}
			}
			if (netState[i]) {
				fuzzyState[i] = fuzzy1;
				String cc = c.getSp();
				Integer ii = c.getIdInt();
				if (cc =="LEGAL") {
					legals.add(ii);
				}
				else if (cc=="GOAL") {
					goals.add(ii);
				}
				else if (cc=="TRANS") {
					transitions.add(ii);
				}
			}
			message = message.concat(" to ");
			message = message.concat(String.valueOf(netState[i]));
			//printd(":",message);
		}
		printd("Resolved:",String.valueOf(transitions.size()));
	}

	//done
	// After resolving propnet, gets next state by using transitions
	public void goToNext() {
		boolean[] netStateNew = new boolean[pnSz];
		for (Integer i1 : transitions) {
			Component c = propNet.findComponent(i1.intValue());
			for (Integer i2 : c.getTransOutputIds()) {
				//printd("On:",i2.toString());
				netStateNew[i2.intValue()] = true;
			}
		}
		netState = netStateNew;
		currentState = getStateFromBase();
		printd("NextState:",String.valueOf(legals.size()));
	}

	//done
	//utility function used by getLegalMoves
	public List<Integer> getLegalInputs(GdlConstant r) {
		printd("RoleName:",r.toString());
		List<Integer> ans = new ArrayList<Integer>();
		for (Integer i : legals) {
			printd("Owner:",propNet.findComponent(i.intValue()).getOwner().toString());
			if (propNet.findComponent(i.intValue()).getOwner().equals(r)) {
				ans.add(i);
			}
		}
		return ans;
	}

	//done
	@Override
	public List<Move> getLegalMoves(MachineState state, Role role)
	throws MoveDefinitionException {
		setState(state);
		printd("Role:",role.toString());
		List<Integer> is = getLegalInputs(role.getName());
		List<Move> ans = new ArrayList<Move>();
		for (Integer i : is) {
			Proposition p = (Proposition) propNet.findComponent(i);
			ans.add(getMoveFromProposition(p));
		}
		return ans;
	}

	//done
	@Override
	public MachineState getNextState(MachineState state, List<Move> moves)
	throws TransitionDefinitionException {
		setStateMove(state,moves);
		goToNext();
		resolve();
		return currentState;
	}


	public void printLegals() {
		for (Integer i : legals) {
			printd("Legal:",propNet.findComponent(i).toString2());
		}
	}

	public void printCurrentState() {
		printd("PropNetState:",currentState.toString());
	}


	// predone
	@Override
	public List<Role> getRoles() {
		return roles;
	}

	// predone
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

	// predone
	public static Move getMoveFromProposition(Proposition p)
	{
		return new Move(p.getName().get(1));
	}

	// predone; not needed
    private int getGoalValue(Proposition goalProposition)
	{
		GdlRelation relation = (GdlRelation) goalProposition.getName();
		GdlConstant constant = (GdlConstant) relation.get(1);
		return Integer.parseInt(constant.toString());
	}

	// done; modified from original
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
			DecimalFormat myFormatter = new DecimalFormat("0.00");
			for (int i = 0; i < propNet.getSize(); i++) {
				String s = "";
				if (netState[i]) {
					s=s.concat("[ X ]");
				}
				else {
					s=s.concat("| . |");
				}
				s=s.concat("=");
				s=s.concat(myFormatter.format(fuzzyState[i]));
				s=s.concat("=");
				s=s.concat(propNet.getComponentsS().get(i).toString3());
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