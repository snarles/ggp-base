package org.ggp.base.player.gamer.statemachine.cs227b;

import java.util.ArrayList;
import java.util.List;

import org.ggp.base.util.statemachine.MachineState;
import org.ggp.base.util.statemachine.Move;
import org.ggp.base.util.statemachine.StateMachine;
import org.ggp.base.util.statemachine.exceptions.GoalDefinitionException;
import org.ggp.base.util.statemachine.exceptions.MoveDefinitionException;
import org.ggp.base.util.statemachine.exceptions.TransitionDefinitionException;

public class MCTSmk3 extends GrimgauntPredatorGamer {

	protected static long Start = 0;
	protected static long MaxTime = 0;
	private MaxNode root;

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
		root = new MaxNode(getCurrentState(), null);
		MaxNode curr;
		while (!timeout()) {
			System.out.println("New iteration @ " + playclock() + "ms.");
			curr = root;
			System.out.println("Selecting node.");
			curr = curr.select();
			System.out.println("Expanding node.");
			curr.expand();
			System.out.println("Simulating game.");
			double score = curr.simulate();
			System.out.println("Propagating.");
			curr.propagate(score);
		}
		return root.getMove();
	}


	public class MaxNode {

		private MachineState state;
		public double visits;
		public double score;

		private ArrayList<MinNode> children;
		private MinNode parent;

		public MaxNode(MachineState state, MinNode parent){
			this.state = state;
			this.visits = 0;
			this.score = 0;
			this.children = null;
			this.parent = parent;
		}

		public Move getMove() {
			double bestScore = -1;
			Move bestMove = null;
			for (MinNode child : this.children) {
				if (child.visits > 0) {
					if (child.score > bestScore) {
						bestMove = child.move;
						bestScore = child.score;
					}
				}
			}
			return bestMove;
		}

		public MaxNode select() {
			if(this.visits == 0 || getStateMachine().isTerminal(this.state)) return this;
			for (MinNode child : this.children) {
				if(child.visits == 0) return child.select();
			}
			double bestScore = 0;
			MinNode bestChild = null;
			for (MinNode child : this.children) {
				double childScore = (child.score/this.visits)+Math.sqrt(2*Math.log(this.visits)/child.visits);
				if (childScore > bestScore) {
					bestScore = childScore;
					bestChild = child;
				}
			}
			return bestChild.select();
		}

		public void expand() throws MoveDefinitionException, TransitionDefinitionException {
			if(!getStateMachine().isTerminal(this.state)) {
				this.children = new ArrayList<MinNode>();
				List<Move> moves = getStateMachine().getLegalMoves(this.state, getRole());
				for (Move move : moves) {
					MinNode child = new MinNode(move, this);
					this.children.add(child);
					child.expand(this.state);
				}
			}

		}

		private int[] depth = new int[1];
		public double simulate() throws GoalDefinitionException, TransitionDefinitionException, MoveDefinitionException {
			StateMachine sm = getStateMachine();
			System.out.println("Starting depth charge.");
		    MachineState finalState = sm.performDepthCharge(this.state, depth);
		    System.out.println("Ending depth charge. Explored down " + depth[0]);
		    return (sm.getGoal(finalState, getRole()))/100.0;

		}

		public void propagate(double value) {
			this.score += value;
			this.visits++;
			if (this.parent != null) this.parent.propagate(this.score);
		}

	}

	public class MinNode {

		public Move move;
		public double visits;
		public double score;

		private ArrayList<MaxNode> children;
		private MaxNode parent;

		public MinNode(Move move, MaxNode parent){
			this.move = move;
			this.visits = 0;
			this.score = 0;
			this.children = null;
			this.parent = parent;
		}


		public MaxNode select() {

			for (MaxNode child : this.children) {
				if(child.visits == 0) return child;
			}
			double worstScore = Double.POSITIVE_INFINITY;
			MaxNode worstChild = null;
			for (MaxNode child : this.children) {
				double childScore = (child.score/this.visits)+Math.sqrt(2*Math.log(this.visits)/child.visits);
				if (childScore < worstScore) {
					worstScore = childScore;
					worstChild = child;
				}
			}
			return worstChild.select();
		}

		public void expand(MachineState state) throws MoveDefinitionException, TransitionDefinitionException {
			this.children = new ArrayList<MaxNode>();
			for (List<Move> jointMove : getStateMachine().getLegalJointMoves(state,	getRole(), this.move)) {
				MachineState newstate = getStateMachine().getNextState(state, jointMove);
				MaxNode child = new MaxNode(newstate, this);
				children.add(child);
			}
		}

		public void propagate(double value) {
			this.score += value;
			this.visits++;
			if (this.parent != null) this.parent.propagate(this.score);
		}

	}

}