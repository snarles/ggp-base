package org.ggp.base.player.gamer.statemachine.cs227b;

import java.util.List;

import org.ggp.base.player.gamer.event.GamerSelectedMoveEvent;
import org.ggp.base.util.statemachine.MachineState;
import org.ggp.base.util.statemachine.Move;
import org.ggp.base.util.statemachine.exceptions.GoalDefinitionException;
import org.ggp.base.util.statemachine.exceptions.MoveDefinitionException;
import org.ggp.base.util.statemachine.exceptions.TransitionDefinitionException;

public class MinimaxPlayer extends GrimgauntPredatorGamer {

	protected static final int MAX_DEPTH = 10;

	@Override
	public Move stateMachineSelectMove(long timeout)
			throws TransitionDefinitionException, MoveDefinitionException,
			GoalDefinitionException {
		long start = System.currentTimeMillis();

		List<Move> moves = getStateMachine().getLegalMoves(getCurrentState(),
				getRole());
		int bestScoreSoFar = -1;
		Move bestMoveSoFar = null;

		for (Move move : moves) {
			int bestScoreAfterMove = minimaxScoreForMove(getCurrentState(),
					move);
			if (bestScoreAfterMove > bestScoreSoFar) {
				bestScoreSoFar = bestScoreAfterMove;
				bestMoveSoFar = move;
				if (bestScoreSoFar == 100) {
					break;
				}
			}
		}

		long stop = System.currentTimeMillis();

		notifyObservers(new GamerSelectedMoveEvent(moves, bestMoveSoFar, stop
				- start));
		return bestMoveSoFar;
	}

	private int minimaxScoreForMove(MachineState state, Move myMove)
			throws TransitionDefinitionException, MoveDefinitionException,
			GoalDefinitionException {

		//Minimize
		int worstScoreSoFar = 100;
		for (List<Move> jointMove : getStateMachine().getLegalJointMoves(state,
				getRole(), myMove)) {
			int bestScoreSoFar = -1;
			MachineState stateAfterMove = getStateMachine().getNextState(state,
					jointMove);
			if (getStateMachine().isTerminal(stateAfterMove)) {
				bestScoreSoFar = getStateMachine().getGoal(stateAfterMove,
						getRole());
			} else {
				// Choose the move for us in the next state which maximizes our
				// score
				List<Move> moves = getStateMachine().getLegalMoves(
						stateAfterMove, getRole());
				for (Move myNextMove : moves) {
					int bestScoreAfterMove = minimaxScoreForMove(
							stateAfterMove, myNextMove);
					bestScoreSoFar = Math.max(bestScoreSoFar,
							bestScoreAfterMove);
					if (bestScoreSoFar == 100)
						break;
				}
			}
			// Choose the joint move for the opponents that minimizes our score
			worstScoreSoFar = Math.min(worstScoreSoFar, bestScoreSoFar);
			if (worstScoreSoFar == 0)
				break;
		}
		return worstScoreSoFar;
	}
}