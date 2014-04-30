package org.ggp.base.player.gamer.statemachine.cs227b;

import java.util.List;

import org.ggp.base.player.gamer.event.GamerSelectedMoveEvent;
import org.ggp.base.util.statemachine.MachineState;
import org.ggp.base.util.statemachine.Move;
import org.ggp.base.util.statemachine.exceptions.GoalDefinitionException;
import org.ggp.base.util.statemachine.exceptions.MoveDefinitionException;
import org.ggp.base.util.statemachine.exceptions.TransitionDefinitionException;

public class AlphaBetaPlayer extends GrimgauntPredatorGamer {

	protected static final int MAX_DEPTH = 2;
	protected static long Start = 0;
	protected static long MaxTime = 0;


	// =================
	// METAGAME FUNTIONS
	// =================
	@Override
	public void stateMachineMetaGame(long timeout) throws TransitionDefinitionException, MoveDefinitionException, GoalDefinitionException
	{
		//do nothing
	}

	private boolean timeout() {
		return (System.currentTimeMillis() - Start > (MaxTime - 1000));
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
		List<Move> moves = getStateMachine().getLegalMoves(getCurrentState(),
				getRole());
		int bestScoreSoFar = -1;
		Move bestMoveSoFar = null;

		for (Move move : moves) {
			int bestScoreAfterMove = Minimize(getCurrentState(), move, 0, -1, 101);
			System.out.println("Move Score: "+ bestScoreAfterMove);
			if (bestScoreAfterMove > bestScoreSoFar) {
				bestScoreSoFar = bestScoreAfterMove;
				bestMoveSoFar = move;
				if (bestScoreSoFar == 100) {
					break;
				}
			}
		}

		long stop = System.currentTimeMillis();
		System.out.println("Playtime: "+ (stop-Start) + "ms");
		notifyObservers(new GamerSelectedMoveEvent(moves, bestMoveSoFar, stop
				- Start));
		return bestMoveSoFar;
	}

	private int Minimize(MachineState state, Move myMove, int depth, int alpha, int beta) throws GoalDefinitionException, MoveDefinitionException, TransitionDefinitionException {
		//Minimize
				for (List<Move> jointMove : getStateMachine().getLegalJointMoves(state,	getRole(), myMove)) {
					if (timeout()) {
						System.out.println("Timed out at minimize depth: "+ depth);
						break;
					}
					MachineState stateAfterMove = getStateMachine().getNextState(state, jointMove);
					int nextScore = Maximize(stateAfterMove, depth, alpha, beta);
					beta = Math.min(beta, nextScore);
					if (beta <= alpha)
						break;
				}
				return beta;

	}

	private int Maximize(MachineState state, int depth, int alpha, int beta) throws GoalDefinitionException, MoveDefinitionException, TransitionDefinitionException {
		if (getStateMachine().isTerminal(state) || depth >= MAX_DEPTH) {
			return GoalEval(state);
		} else {
			// Maximize
			List<Move> moves = getStateMachine().getLegalMoves(state, getRole());
			for (Move myNextMove : moves) {
				if (timeout()){
					System.out.println("Timed out at maximize depth: "+ depth);
					break;
				}
				int best = Minimize(state, myNextMove, (depth + 1), alpha, beta);
				alpha = Math.max(alpha, best);
				if (beta <= alpha)
					break;
			}
		}
		return alpha;
	}

	// ==========
	// HEURISTICS
	// ==========

	private int FixedDepthEval(MachineState state) throws GoalDefinitionException {
		if(getStateMachine().isTerminal(state) ) {
			//System.out.println("Terminal State" + getStateMachine().getGoal(state, getRole()));
			return getStateMachine().getGoal(state, getRole());
		} else {
			//System.out.println("Nonterminal State" + 0);
			return 0;
		}
	}

	private int MobilityEval(MachineState state) throws GoalDefinitionException, MoveDefinitionException {
		if(getStateMachine().isTerminal(state) ) {
			return getStateMachine().getGoal(state, getRole());
		} else {
			int legalMoves = getStateMachine().getLegalMoves(getCurrentState(),
					getRole()).size();
			return legalMoves;
		}
	}

	private int FocusEval(MachineState state) throws GoalDefinitionException, MoveDefinitionException {
		if(getStateMachine().isTerminal(state) ) {
			return getStateMachine().getGoal(state, getRole());
		} else {
			int legalMoves = getStateMachine().getLegalMoves(getCurrentState(),
					getRole()).size();
			return 100 - legalMoves;
		}
	}

	private int GoalEval(MachineState state) throws GoalDefinitionException {
		//System.out.println(getRole() + " DEPTH SCORE: " + getStateMachine().getGoal(state, getRole()));
		return getStateMachine().getGoal(state, getRole());
	}
}

