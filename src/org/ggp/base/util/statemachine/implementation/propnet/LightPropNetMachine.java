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
public class LightPropNetMachine extends StateMachine {
	private boolean diagnosticMode = true;

	private PropNet propNet;
    private List<Role> roles;
    private List<GdlConstant> roleNames;
    private boolean[] netState = null;
    private MachineState currentState = null;
    private int pnSz = 0; //size of propnet
    Map<GdlSentence, Proposition> baseMap;
    private Set<Integer> legals;
    private Set<Integer> goals;
    private Set<Integer> deltaTrans = null;
    private Set<Integer> currentInputs;
    private Set<Integer> checkFalse = null;
    private Set<Integer> checkTrue = null;
    private Set<Integer> trueBase;
    private PriorityQueue<Integer> deltaState = null;



    public LightPropNetMachine() {

    }
    public LightPropNetMachine(PropNet propNet0, List<Role> roles0,List<GdlConstant> roleNames0,
			boolean[] netState0, MachineState currentState0,
			int pnSz0, Map<GdlSentence, Proposition> baseMap0,
			Set<Integer> legals0, Set<Integer> goals0, Set<Integer> deltaTrans0, Set<Integer> currentInputs0,
			Set<Integer> checkFalse0, Set<Integer> checkTrue0, Set<Integer> trueBase0,
			PriorityQueue<Integer> deltaState0)
	{
		propNet = propNet0;
		roles = new ArrayList<Role>(roles0);
		roleNames = new ArrayList<GdlConstant>(roleNames0);
		netState = Arrays.copyOf(netState0,pnSz0);
		currentState = new MachineState(new HashSet<GdlSentence>(currentState0.getContents()));
		pnSz = pnSz0;
		baseMap = baseMap0;
		legals = new HashSet<Integer>(legals0);
		goals = new HashSet<Integer>(goals0);
		deltaTrans = new HashSet<Integer>(deltaTrans0);
		currentInputs = new HashSet<Integer>(currentInputs0);
		checkFalse = new HashSet<Integer>(checkFalse0);
		checkTrue = new HashSet<Integer>(checkTrue0);
		trueBase = new HashSet<Integer>(trueBase0);
		deltaState = new PriorityQueue<Integer>(deltaState0);
    }

    public LightPropNetMachine duplicate() {
    	return new LightPropNetMachine(propNet,
			roles,
			roleNames,
			netState,
			currentState,
			pnSz,
			baseMap,
			legals,
			goals,
			deltaTrans,
			currentInputs,
			checkFalse,
			checkTrue,
			trueBase,
			deltaState);
    }
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
        roleNames = propNet.getRoleNames();
        baseMap = propNet.getBasePropositions();
        netState = new boolean[pnSz];
        currentInputs = new HashSet<Integer>();
        deltaTrans = new HashSet<Integer>();
        trueBase = new HashSet<Integer>();
        deltaState = new PriorityQueue<Integer>();
        legals = new HashSet<Integer>();
        goals = new HashSet<Integer>();
        checkFalse = new HashSet<Integer>();
        checkTrue = new HashSet<Integer>();

        for (Component c : propNet.getComponentsS()) {
        	deltaState.add(c.getIdInt());
        }
        resolve();
        goToNext();
        resolve();
        long elapsed = System.currentTimeMillis()-start;
        printd("Time for LPNM to init:",String.valueOf(elapsed));
    }

	/**
	 * Computes if the state is terminal. Should return the value
	 * of the terminal proposition for the state.
	 */
	@Override
	public boolean isTerminal(MachineState state) {
		setState(state);
		int i0 = propNet.getTerminalProposition().getId();
		//printCurrentState("Terminal? ".concat(String.valueOf(netState[i0])));
		return netState[i0];
	}

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


	@Override
	public MachineState getInitialState() {
		// TODO: Compute the initial state.
		Integer i0 = new Integer(propNet.getInitProposition().getId());
		netState[i0.intValue()] = true;
		deltaState.add(i0);
		checkFalse.add(i0);
		resolve();
		goToNext();
		resolve();
		if (diagnosticMode) {
			printCurrentState("LPNM Initialized state to ");
		}
		//printNetState();
		getStateFromBase();
		return new MachineState(new HashSet<GdlSentence>(currentState.getContents()));
	}

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
			//printd("reset state ","");
			setState0(state);
			flag = true;
		}
		if ((! currentInputs.equals(newInputs)) || flag) {
			for (Integer i : currentInputs) {
				if (! newInputs.contains(i)) {
					netState[i.intValue()] = false;
					deltaState.add(i);
				}
			}
			for (Integer i : newInputs) {
				if (! currentInputs.contains(i)) {
					netState[i.intValue()] = true;
					deltaState.add(i);
				}
			}
			currentInputs = newInputs;
			resolve();
		}
	}
	// utility function used by SetState and SetStateMove
	public void setState0(MachineState state) {
		//printCurrentState("changing state from :");
		Set<GdlSentence> props2 = state.getContents();
		Set<GdlSentence> props1 = currentState.getContents();
		for (GdlSentence g : props2) {
			if ( ! props1.contains(g)) {
				Proposition p = baseMap.get(g);
				trueBase.add(p.getIdInt());
				deltaState.add(p.getIdInt());
				netState[p.getId()] = true;
				checkFalse.add(p.getIdInt());
			}
		}
		for (GdlSentence g : props1) {
			if ( ! props2.contains(g)) {
				Proposition p = baseMap.get(g);
				trueBase.remove(p.getIdInt());
				deltaState.add(p.getIdInt());
				netState[p.getId()] = false;
				checkTrue.add(p.getIdInt());
			}
		}
		getStateFromBase();
		//printCurrentState("changing state to :");
	}

	// Given updates loaded in deltaState, propagates changes forward
	public void resolve()
	{
		if (fullDiagnosticMode) printNetState();
        while (deltaState.size() > 0) {
        	Integer ii = deltaState.remove();
			int i = ii.intValue();
			boolean oldB = netState[i];
			Component c = propNet.findComponent(i);
			String message = c.toString2();
			message = message.concat(String.valueOf(netState[i]));

			Set<Integer> inputs = c.getInputIds();

			if (c instanceof And) {
				netState[i] = true;
				for (Integer i1 : inputs) {
					netState[i] = netState[i] && netState[i1.intValue()];
				}
			}
			else if (c instanceof Or) {
				netState[i] = false;
				for (Integer i1 : inputs) {
					netState[i] = netState[i] || netState[i1.intValue()];
				}
			}
			else if (c instanceof Not) {
				for (Integer i1 : inputs) {
					netState[i] = ! netState[i1.intValue()];
				}
			}
			else {
				for (Integer i1 : inputs) {
					netState[i] = netState[i1.intValue()];
				}
			}
			String cc = c.getSp();
			if (netState[i] && !oldB) {
				if (cc =="LEGAL") {
					legals.add(ii);
				}
				else if (cc=="GOAL") {
					goals.add(ii);
				}
				else if (cc=="BASE") {
					trueBase.add(ii);
				}

				else if (cc=="zTRANS") {
					deltaTrans.add(ii);
				}
			}
			if (!netState[i] && oldB) {
				if (cc =="LEGAL") {
					legals.remove(ii);
				}
				else if (cc=="GOAL") {
					goals.remove(ii);
				}
				else if (cc=="BASE") {
					trueBase.remove(ii);
				}
				else if (cc=="zTRANS") {
					deltaTrans.add(ii);
				}
			}

			message = message.concat(" to ");
			message = message.concat(String.valueOf(netState[i]));
			if (netState[i] != oldB || c.getLevel()==0) {
				Set<Integer> outputs = c.getOutputIds();
				for (Integer i2 : outputs) {
					deltaState.add(i2);
				}
				if (fullDiagnosticMode) {
				  printd(" comp: ",c.toString3());
				  printd(":",message);
				}
			}
		}
        if (fullDiagnosticMode) { paused(); }
		//printd("Resolved:",String.valueOf(deltaTrans.size()));
	}



	public void goToNext() {
		deltaState = new PriorityQueue<Integer>();
		boolean[] oldNetState = Arrays.copyOf(netState, pnSz);
		for (Integer i1 : checkFalse) {
			netState[i1.intValue()]=false;
		}
		for (Integer i1 : checkTrue) {
			netState[i1.intValue()]=true;
		}

		for (Integer i1 : deltaTrans) {
			Component c = propNet.findComponent(i1.intValue());
			for (Integer i2 : c.getTransOutputIds()) {
				boolean oldV = netState[i2.intValue()];
				boolean newV = netState[i1.intValue()];
				if (oldV != newV) {
					deltaState.add(i2);
					netState[i2.intValue()] = newV;
					if (newV) {
						trueBase.add(i2);
					}
					else {
						trueBase.remove(i2);
					}
				}
			}
		}
		for (Integer i1 : checkFalse) {
			if (! netState[i1.intValue()]) {
				deltaState.add(i1);
				trueBase.remove(i1);
			}
		}
		for (Integer i1 : checkTrue) {
			if (netState[i1.intValue()]) {
				deltaState.add(i1);
				trueBase.add(i1);
			}
		}
		checkTrue = new HashSet<Integer>();
		checkFalse = new HashSet<Integer>();
		deltaTrans = new HashSet<Integer>();
		getStateFromBase();
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
		String record = currentState.toString().concat(String.valueOf(isTerminal(currentState)));
		setState(state);
		List<Integer> is = getLegalInputs(role.getName());
		List<Move> ans = new ArrayList<Move>();
		for (Integer i : is) {
			Proposition p = (Proposition) propNet.findComponent(i);
			ans.add(getMoveFromProposition(p));
		}
		if (ans.size()==0) {
			printd(record,"");
			printCurrentState("Get Legal moves for");
			printLegals();
			printd("Is terminal? ", String.valueOf(isTerminal(state)));
			paused();
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
		return new MachineState(new HashSet<GdlSentence>(currentState.getContents()));
	}


	public void printLegals() {
		for (Integer i : legals) {
			printd("Legal:",propNet.findComponent(i).toString2());
		}
	}

	public void printCurrentState() {
		currentState = getStateFromBase();
		printd("PropNetState:",currentState.toString());
	}

	public MachineState getCurrentState() {
		getStateFromBase();
		return new MachineState(new HashSet<GdlSentence>(currentState.getContents()));
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

	public MachineState getStateFromBase()
	{
		Set<GdlSentence> contents = new HashSet<GdlSentence>();
		for (Integer ii : trueBase)
		{
			Proposition p = (Proposition) propNet.findComponent(ii.intValue());
			contents.add(p.getName());
		}
		currentState =new MachineState(contents);
		return currentState;
	}
	public MachineState getStateFromBase0()
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
				s=s.concat(propNet.getComponentsS().get(i).toString3());
				System.out.println(s);
			}
			printd("CheckTrue: ",checkTrue.toString());
			printd("CheckFalse: ",checkTrue.toString());
			printd("deltraTrans: ",deltaTrans.toString());
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

	public void printCurrentState(String s) {
		printd("PropNetState:",s.concat(currentState.toString()));
	}
}