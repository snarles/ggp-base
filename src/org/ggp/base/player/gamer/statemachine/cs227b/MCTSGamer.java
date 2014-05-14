package org.ggp.base.player.gamer.statemachine.cs227b;

import java.util.ArrayList;
import java.util.List;

import org.ggp.base.util.statemachine.MachineState;
import org.ggp.base.util.statemachine.Move;
import org.ggp.base.util.statemachine.StateMachine;
import org.ggp.base.util.statemachine.exceptions.GoalDefinitionException;
import org.ggp.base.util.statemachine.exceptions.MoveDefinitionException;
import org.ggp.base.util.statemachine.exceptions.TransitionDefinitionException;

public class MCTSGamer extends GrimgauntPredatorGamer {

	protected static long Start = 0;
	protected static long MaxTime = 0;

	@Override
	public void stateMachineMetaGame(long timeout) throws TransitionDefinitionException, MoveDefinitionException, GoalDefinitionException
	{
		//do nothing
	}

	private boolean timeout() {
		return (System.currentTimeMillis() - Start > (MaxTime - 2000));
	}

	private boolean panic() {
		return (System.currentTimeMillis() - Start > (MaxTime - 300));
	}

	private long playclock() {
		return (System.currentTimeMillis()-Start);
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
		return ThisMove.getMove();
	}


	public class TreeNode {

	    private MachineState state;
	    private Move move;
	    private boolean isMaxState;

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
	    }

	    public Move getMove() throws MoveDefinitionException, TransitionDefinitionException, GoalDefinitionException {
	    	System.out.println("Beginning MCTS.");
	        while (!timeout()) {
	        	System.out.printf("Sel-");
		    	TreeNode cur = this;
		        while (!cur.isLeaf()) {
		        	System.out.printf("-");
		            cur = cur.select();
		        }
		        System.out.printf("Exp-");
		        cur.expand();
		        System.out.printf("Sim-");
		        System.out.println(cur);
		        cur = cur.select();
		        double score = cur.simulate();
		        System.out.printf("Prop ");
		        propagate(cur, score);
	        }
	        System.out.println("\nMCTS Finished, beginning move decision.");
	        Move bestMove = null;
	        double bestScore = 0;
	        for (TreeNode child : this.children) {
	        	if (panic()) break;
	        	double newScore = child.utility;
	        	if (newScore > bestScore) {
	        		bestScore = newScore;
	        		bestMove = child.move;
	        	}
	        }
	        System.out.println("Move Decided. Returning.");
	        return bestMove;
	    }

	    public TreeNode select() {
	    	if (this.visits == 0) return this;
	    	for (TreeNode child: this.children) {
	    		if (child.visits == 0) return child;
	    	}
	    	double score = 0;
	    	TreeNode result = this;
	    	for (TreeNode child: this.children) {
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

	    public void expand() throws MoveDefinitionException, TransitionDefinitionException {
	    	//boolean nextMaxType = !(this.isMaxState);
	    	this.children = new ArrayList<TreeNode>();
	    	List<Move> moves = getStateMachine().getLegalMoves(getCurrentState(), getRole());
	    	for (Move m : moves) {
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
	    }

	    public double simulate() throws TransitionDefinitionException, MoveDefinitionException, GoalDefinitionException {
	    	StateMachine sm = getStateMachine();
	    	MachineState finalState = sm.performDepthCharge(state, null);
	    	if (getStateMachine().isTerminal(finalState))  System.out.println("TERMINAL FOUND");
	    	return sm.getGoal(finalState, getRole());
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
	}

	public class SubNode {

	}

}
