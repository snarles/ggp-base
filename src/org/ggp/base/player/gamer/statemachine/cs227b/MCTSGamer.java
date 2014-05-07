package org.ggp.base.player.gamer.statemachine.cs227b;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

import org.ggp.base.util.statemachine.MachineState;
import org.ggp.base.util.statemachine.Move;
import org.ggp.base.util.statemachine.StateMachine;
import org.ggp.base.util.statemachine.exceptions.GoalDefinitionException;
import org.ggp.base.util.statemachine.exceptions.MoveDefinitionException;
import org.ggp.base.util.statemachine.exceptions.TransitionDefinitionException;

public class MCTSGamer extends GrimgauntPredatorGamer {
	int diaglevel = 7;
	protected static long Start = 0;
	protected static long MaxTime = 0;
	String name = "MCTS Gamer";

	@Override
	public void stateMachineMetaGame(long timeout) throws TransitionDefinitionException, MoveDefinitionException, GoalDefinitionException
	{
		//do nothing
	}

	public void setName(String s) {
		name = s;
	}

	private boolean timeout() {
		return (System.currentTimeMillis() - Start > (MaxTime - 3000));
	}

	private boolean panic() {
		return (System.currentTimeMillis() - Start > (MaxTime - 300));
	}

	// ==============
	// MOVE SELECTION
	// ==============
	@Override
	public Move stateMachineSelectMove(long timeout)
			throws TransitionDefinitionException, MoveDefinitionException,
			GoalDefinitionException {
		Start = System.currentTimeMillis();
		MaxTime = (timeout - Start);
		System.out.println("\n========================\nNext Move:");
		System.out.println("Round Time Limit: " + MaxTime + "ms");
		TreeNode ThisMove = new TreeNode(getCurrentState(), null, null);
		System.out.println("Current state".concat(getCurrentState().toString()));
		return ThisMove.getMove();
	}


	public class TreeNode {
	    private MachineState state;
	    private Move move;
	    private boolean isMaxState;
	    public long id;

	    ArrayList<TreeNode> children;
	    double visits, utility, nodeValue, worstChild;
		private TreeNode parent;

	    public TreeNode(MachineState state,Move move, TreeNode parent) {
	    	this.children = null;
	    	this.state = state;
	    	this.move = move;
	    	this.parent = parent;
	    	this.visits = 0;
	    	this.utility = 0;
	    	this.id = System.currentTimeMillis();
	    }

	    public Move getMove() throws MoveDefinitionException, TransitionDefinitionException, GoalDefinitionException {
	    	System.out.println("Beginning MCTS.");
	        while (!timeout()) {
	        	System.out.printf("Sel-");
		    	TreeNode cur = this;
	        	int count = 0;
		        while (!cur.isLeaf()) {
		        	count++;
		        	if (count > 5) {
		        		printd("while!curisleaf ",cur.toString(),3);
		        		for (TreeNode child : cur.children) {
		        			printd("child",child.toString(),3);
		        		}
		        		pausd(8);
		        	}
		            cur = cur.select();
		        }
		        System.out.printf("Exp-");
		        Move oneMove = cur.expand();
		        if (oneMove != null) {
		        	System.out.println("\n Only one move.");
		        	return oneMove;
		        }
		        System.out.printf("Sim-");
		        cur = cur.select();
		        double score = cur.simulate();
		        System.out.printf("Prop ");
		        propagate(cur, score);
	        }
	        System.out.println("\nMCTS Finished, beginning move decision.");

	        Move bestMove = null;
	        double bestScore = -1;
	        for (TreeNode child : this.children) {
	        	if (panic()) break;
	        	double newScore = child.utility;
	        	printd("Child move:",child.move.toString(),3);
	        	printd("Child score",String.valueOf(newScore),3);
	        	if (newScore > bestScore) {
	        		bestScore = newScore;
	        		bestMove = child.move;
	        	}
	        }
	        pausd(8);
	        System.out.println("Move Decided. Returning.");
	        System.out.println(bestMove.toString());
	        return bestMove;
	    }

	    public TreeNode select() {
	    	printd("Select","",2);
	    	if (this.visits == 0) return this;
	    	printd("Forchild","",11);
	    	for (TreeNode child: this.children) {
	    		printd("Anychild","",11);
	    		if (child.visits == 0) return child;
	    	}
	    	double score = -1;
	    	TreeNode result = this;
	    	printd("Forchild ",String.valueOf(this.children.size()),2);
	    	for (TreeNode child: this.children) {
	    		printd("Anychild","",2);
	    		double newScore = UCT(child);
	    		if (newScore > score) {
	    			score = newScore;
	    			result = child;
	    		}
	    	}
	    	return result;
	    }

	    private double UCT(TreeNode node) {
	    	return node.utility+Math.sqrt(2*Math.log(this.visits)/node.visits);
	    }

	    public Move expand() throws MoveDefinitionException, TransitionDefinitionException {
	    	//boolean nextMaxType = !(this.isMaxState);
	    	this.children = new ArrayList<TreeNode>();
	    	List<Move> moves = getStateMachine().getLegalMoves(getCurrentState(), getRole());
	    	if (moves.size() == 1){
	    		return moves.get(0);
	    	}
	    	for (Move m : moves) {
	    		printd("Considering move ",m.toString(),4);
	    		for (List<Move> jointMove : getStateMachine().getLegalJointMoves(state,	getRole(), m)) {
					if (timeout()) {
						System.out.println("Timed out expanding tree.");
						break;
					}
					MachineState nextState = getStateMachine().getNextState(state, jointMove);
					if (nextState == null) System.out.println("NULL STATE");
					if (m == null) System.out.println("NULL MOVE");
					if (this == null) System.out.println("NULL NODE");
					TreeNode newChild = new TreeNode(nextState, m, this);
					if (newChild == null) System.out.println("NULL CHILD");
					children.add(newChild);
	    		}

	    	}
	    	return null;
	    }

	    public double simulate() throws TransitionDefinitionException, MoveDefinitionException, GoalDefinitionException {
	    	StateMachine sm = getStateMachine();
	    	printd("Simulating state: ",state.toString(),3);
	    	int[] theDepth = {1,2};
	    	//pausd(5);
	    	MachineState finalState = sm.performDepthCharge(state, theDepth);
	    	if (getStateMachine().isTerminal(finalState))  System.out.println("TERMINAL FOUND");
	    	printd("Done simulating state: ",finalState.toString(),3);
	    	//pausd(5);
	    	double ans = 0;
	    	try {
	    		ans = sm.getGoal(finalState, getRole());
	    	}
	    	catch (GoalDefinitionException g) {
	    		printd("Goadl definition exception",finalState.toString(),1);
	    	}
	    	return ans;
	    }

	    public void propagate(TreeNode n, double score) {
	    	n.visits++;
	    	n.utility += score;
	    }

	    public boolean isLeaf() {
	        return children == null;
	    }


	    public void updateStats(double value) {
	        visits++;
	        utility += value;
	    }

	    public int arity() {
	        return children == null ? 0 : children.size();
	    }

	    @Override
		public String toString() {
	    	return String.valueOf(id);
	    }

	    // diagnostic print, prints depending on the level of diagnostics needed
	    public void printd(String s, String t, int lv) {
	    	if (lv < diaglevel) {
	    		System.out.println(name.concat(" ").concat(s.concat(t)));
	    	}
	    }

	    // Pauses
	    public void pausd(int lv) {
	    	if (lv < diaglevel) {
	    		try {
	    			BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
	    			System.out.println("Pausing:");
	    			String etc = in.readLine();
	    		}
	    		catch (IOException e) {

	    		}
	    	}
	    }
	}

}
