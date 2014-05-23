package org.ggp.base.util.statemachine.implementation.propnet;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.math.BigInteger;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
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
    private boolean[] resolvedNetState = null;
    private boolean[] netState = null;
    private double[] fuzzyState = null;
    private MachineState currentState = null;
    private MachineState currentStateCache = null;
    private int pnSz = 0; //size of propnet
    //private double fuzzy0 = -1.0; // now as exponent
    //private double fuzzy1 = Math.log(1.0-Math.exp(fuzzy0));
    private double fuzzy0=0.1;
    private double fuzzy1=1.0-fuzzy0;
    Map<GdlSentence, Proposition> baseMap;
    private Set<Integer> legals;
    private Set<Integer> goals;
    private Set<Integer> transitions;
    private Set<Integer> currentInputs;
    private Set<Integer> allInputs;
    private Set<Integer> legalsCache;
    private Set<Integer> goalsCache;
    private Set<Integer> transitionsCache;
    private boolean[] netStateCache = null;
    private boolean[] resolvedNetStateCache = null;
    private double[] fuzzyStateCache = null;


    //done
    //sets the fuzzy values, also if you want to recalculate
    public void setFuzzy(double d, double e, boolean res) {
    	fuzzy0 = d;
    	fuzzy1 = 1.0-d;
    	if (res) {
    		resolve();
    	}
    }
    public FuzzyPropNetMachine() {

    }

    public FuzzyPropNetMachine(PropNet propNet0, List<Role> roles0,List<GdlConstant> roleNames0,
    		boolean[] resolvedNetState0, boolean[] netState0, double[] fuzzyState0, MachineState currentState0,
    		MachineState currentStateCache0, int pnSz0, double fuzzy00, double fuzzy10, Map<GdlSentence, Proposition> baseMap0,
    		Set<Integer> legals0, Set<Integer> goals0, Set<Integer> transitions0, Set<Integer> allInputs0, Set<Integer> currentInputs0,
    		Set<Integer> legalsCache0, Set<Integer> goalsCache0, Set<Integer> transitionsCache0,
    		boolean[] netStateCache0, boolean[] resolvedNetStateCache0, double[] fuzzyStateCache0)
    {
    	propNet = propNet0;
    	roles = new ArrayList<Role>(roles0);
    	roleNames = new ArrayList<GdlConstant>(roleNames0);
    	resolvedNetState = Arrays.copyOf(resolvedNetState0,pnSz0);
    	netState = Arrays.copyOf(netState0,pnSz0);
    	fuzzyState = Arrays.copyOf(fuzzyState0,pnSz0);
    	currentState = new MachineState(new HashSet<GdlSentence>(currentState0.getContents()));
    	currentStateCache = new MachineState(new HashSet<GdlSentence>(currentStateCache0.getContents()));
    	pnSz = pnSz0;
    	fuzzy0 = fuzzy00;
    	fuzzy1 = fuzzy10;
    	baseMap = baseMap0;
    	legals = new HashSet<Integer>(legals0);
    	goals = new HashSet<Integer>(goals0);
    	transitions = new HashSet<Integer>(transitions0);
    	currentInputs = new HashSet<Integer>(currentInputs0);
    	allInputs = allInputs0;
    	legalsCache = new HashSet<Integer>(legalsCache0);
    	goalsCache = new HashSet<Integer>(goalsCache0);
    	transitionsCache = new HashSet<Integer>(transitionsCache0);
    	resolvedNetStateCache = Arrays.copyOf(resolvedNetStateCache0,pnSz0);
    	netStateCache = Arrays.copyOf(netStateCache0,pnSz0);
    	fuzzyStateCache = Arrays.copyOf(fuzzyStateCache0,pnSz0);
    }

    public FuzzyPropNetMachine duplicate() {
    	return new FuzzyPropNetMachine(propNet, roles,roleNames,
        		resolvedNetState, netState, fuzzyState, currentState,
        		currentStateCache, pnSz, fuzzy0, fuzzy1, baseMap,
        		legals, goals, transitions, allInputs, currentInputs,
        		legalsCache, goalsCache, transitionsCache,
        		netStateCache, resolvedNetStateCache, fuzzyStateCache);
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
        resolvedNetState = netState;
        fuzzyState = new double[pnSz];
        legals = new HashSet<Integer>();
        goals = new HashSet<Integer>();
        allInputs = propNet.getAllInputs();


        resolve();
        goToNext();
        cacheStuff();
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
	public double getFuzzyTerminal(MachineState state) {
		setState(state);
		Proposition p = propNet.getTerminalProposition();
		return fuzzyState[p.getId()];
	}
	// used when exploring moves, etc, and to implement common random numbers
	public void cacheStuff() {
		//printCurrentState("Caching state: ");
		netStateCache = Arrays.copyOf(netState, pnSz);
		fuzzyStateCache = Arrays.copyOf(fuzzyState,pnSz);
		resolvedNetStateCache = Arrays.copyOf(resolvedNetState, pnSz);
		currentStateCache = new MachineState(new HashSet<GdlSentence>(currentState.getContents()));
		legalsCache = new HashSet<Integer>(legals);
		goalsCache = new HashSet<Integer>(goals);
		transitionsCache = new HashSet<Integer>(transitions);

	}
	public void loadCache() {
		netState = Arrays.copyOf(netStateCache,pnSz);
		resolvedNetState = Arrays.copyOf(resolvedNetStateCache, pnSz);
		fuzzyState = Arrays.copyOf(fuzzyStateCache,pnSz);
		currentState = new MachineState(new HashSet<GdlSentence>(currentStateCache.getContents()));
		legals = new HashSet<Integer>(legalsCache);
		goals = new HashSet<Integer>(goalsCache);
		transitions = new HashSet<Integer>(transitionsCache);

		//printCurrentState("Loading cache: ");
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
		resolvedNetState = Arrays.copyOf(netState,pnSz);
		goToNext();
		resolve();
		if (diagnosticMode) {
			//printd("initial: ",i0.toString());
			//printNetState();
			printCurrentState("FPNM Initialized state to ");
		}
	}

	//done
	//called by all getGoal, getTerminal, etc.
	public void setState(MachineState state) {
		Set<GdlSentence> props1 = currentState.getContents();
		Set<GdlSentence> props2 = state.getContents();
		if (props1.equals(props2)) {
			//printd("State match: not changing","");
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
		String s = "";
		for (GdlSentence g : dodos) {
			Proposition p = inputMap.get(g);
			newInputs.add(new Integer(p.getId()));
			s=s.concat(String.valueOf(p.getId()));
			s=s.concat(", ");
		}
		//printd("Newinputs: ",s);
		boolean flag = false;
		if (props1.equals(props2)) {
			//printd("State match: not changing","");
		}
		else {
			printd("reset state ","");
			setState0(state);
			flag = true;
		}
		if ((! currentInputs.equals(newInputs)) || flag) {
			for (Integer i : currentInputs) {
				netState[i.intValue()] = false;
			}
			for (Integer i : newInputs) {
				netState[i.intValue()] = true;
				//printd("Setting ",i.toString());
			}
			resolve();
		}
	}

	// done
	// utility function used by SetState and SetStateMove
	public void setState0(MachineState state) {
		Set<GdlSentence> props1 = state.getContents();
		netState = new boolean[pnSz];
		fuzzyState = new double[pnSz];
		Arrays.fill(fuzzyState, fuzzy0);
		for (GdlSentence g : props1) {
			Proposition p = baseMap.get(g);
			netState[p.getId()] = true;
		}
		currentState = state;
	}

	public double fuzzyAnd(double[] x) {
		double sum = 1.0;
		int sz = x.length;
		for (int i = 0; i < sz; i++) {
			sum = sum * x[i];
		}
		return sum;
	}

	public double fuzzyOr(double[] x) {
		double sum = 0.0;
		int sz = x.length;
		for (int i = 0; i < sz; i++) {
			sum = 1.0-((1.0-sum) * (1.0-x[i]));
		}
		return sum;
	}

	// done
	public void resolve()
	{
		fuzzyState = new double[pnSz];
		Arrays.fill(fuzzyState, fuzzy0);
        legals = new HashSet<Integer>();
        goals = new HashSet<Integer>();
        transitions = new HashSet<Integer>();
		for (int i=0; i < pnSz; i++) {
			if (netState[i]) {
				fuzzyState[i]=fuzzy1;
			}
			//printd("Iteration:",String.valueOf(i));
			Component c = propNet.findComponent(i);
			//printd("  comp:",c.toString3());

			String message = c.toString2();
			message = message.concat(String.valueOf(netState[i]));

			Set<Integer> inputs = c.getInputIds();
			if (c instanceof And) {
				netState[i] = true;
				double[] x=new double[inputs.size()];
				int count = 0;
				for (Integer i1 : inputs) {
					netState[i] = netState[i] && netState[i1.intValue()];
					x[count] =  fuzzyState[i1.intValue()];
					count++;
				}
				//double ans = sum;
				fuzzyState[i] = fuzzyAnd(x);
				//fuzzyState[i] = ans;
			}
			else if (c instanceof Or) {
				netState[i] = false;
				double[] x=new double[inputs.size()];
				int count = 0;
				for (Integer i1 : inputs) {
					netState[i] = netState[i] || netState[i1.intValue()];
					x[count] = fuzzyState[i1.intValue()];
					count++;
				}
				fuzzyState[i] = fuzzyOr(x);
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
				String cc = c.getSp();
				Integer ii = c.getIdInt();
				if (cc =="LEGAL") {
					legals.add(ii);
				}
				else if (cc=="GOAL") {
					goals.add(ii);
				}
				else if (cc=="zTRANS") {
					//printd("trans added",ii.toString());
					transitions.add(ii);
				}
			}
			message = message.concat(" to ");
			message = message.concat(String.valueOf(netState[i]));
			//printd(":",message);
		}
		//printd("Resolved:",String.valueOf(transitions.size()));
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
		//printd("NextState:",String.valueOf(legals.size()));
	}

	//done
	//utility function used by getLegalMoves
	public List<Integer> getLegalInputs(GdlConstant r) {
		//printd("RoleName:",r.toString());
		List<Integer> ans = new ArrayList<Integer>();
		for (Integer i : legals) {
			if (propNet.findComponent(i.intValue()).getOwner().equals(r)) {
				ans.add(i);
				//printd(" move:",propNet.findComponent(i.intValue()).toString2());
			}
			else {
				//printd(" NOT move:",propNet.findComponent(i.intValue()).toString2());
			}
		}
		return ans;
	}

	//done
	@Override
	public List<Move> getLegalMoves(MachineState state, Role role)
	throws MoveDefinitionException {
		setState(state);
		//printd("Role:",role.toString());
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
		resolvedNetState = Arrays.copyOf(netState,pnSz);
		goToNext();
		resolve();
		return currentState;
	}


	public void printLegals() {
		for (Integer i : legals) {
			printd("Legal:",propNet.findComponent(i).toString2());
		}
	}

	public void printCurrentState(String s) {
		printd("PropNetState:",s.concat(currentState.toString()));
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

	public void synchState() {
		currentState = getStateFromBase();
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
			int count = 0;
			for (int i = 0; i < propNet.getSize(); i++) {
				String s = "";
				if (resolvedNetState[i]) {
					s=s.concat("[ X ]");
				}
				else {
					s=s.concat("| . |");
				}

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
				count++;
				if (count % 500==0) {
					paused();
				}
			}
		}
	}

	@Override
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

	@Override
	public void printd(String s, String t) {
		if (diagnosticMode) {
			System.out.println(s.concat(t));
		}
	}
	public MachineState getCurrentState() {
		return new MachineState(new HashSet<GdlSentence>(currentState.getContents()));
	}
	public int randGeometric(double p) {
		double d = rand.nextDouble();
		double l = Math.log(d)/Math.log(p);
		return (int) Math.floor(l);
	}
	//gets heuristic joint moves
	public List<Move> getHeuristicJointMoves(MachineState state) {
		setState(state);
		cacheStuff();
		try {
			// setting up all the moves for everyone
	        List<List<Move>> legals = new ArrayList<List<Move>>();
	        for (Role role : roles) {
	            legals.add(getLegalMoves(state, role));
	        }
	        List<List<Move>> crossProduct = new ArrayList<List<Move>>();
	        Map<Role,Map<Move,List<List<Move>>>> moveMap = new HashMap<Role, Map<Move,List<List<Move>>>>();
	        crossProductLegalMoves(legals, crossProduct, new LinkedList<Move>());
	        int count = 0;
	        for (Role r : roles) {
	        	moveMap.put(r, new HashMap<Move,List<List<Move>>>());
	        	List<Move> legalMoves = legals.get(count);
	        	for (Move m : legalMoves) {
	        		moveMap.get(r).put(m, new ArrayList<List<Move>>());
	        	}
	        	count++;
	        }
	        for (List<Move> jointMove : crossProduct) {
	        	count = 0;
	        	for (Role r : roles) {
	        		Move move = jointMove.get(count);
	        		moveMap.get(r).get(move).add(jointMove);
	        		count++;
	        	}
	        }
	        Map <Role,Integer> roleToInt = new HashMap<Role, Integer>();
	        for (Role r : roles) {

	        }

			Map<List<Move>,List<Double>> jointFuzzyGoals= new ArrayList<List<List<Double>>>();
			Map<List<Move>,MachineState> finalStates = new HashMap<List<Move>,MachineState>();
			for (List<Move> moves : jointMoves) {
				loadCache();
				MachineState finalState = getNextState(state,moves);
				finalStates.put(moves, finalState);
				jointFuzzyGoals
				for (Role r : roles) {

				}

			}
			return null;
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return null;
		}

	}

}