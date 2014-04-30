package org.ggp.base.player.gamer.statemachine.ifueko;

import java.util.List;

import org.ggp.base.player.gamer.event.GamerSelectedMoveEvent;
import org.ggp.base.util.statemachine.MachineState;
import org.ggp.base.util.statemachine.Move;
import org.ggp.base.util.statemachine.exceptions.GoalDefinitionException;
import org.ggp.base.util.statemachine.exceptions.MoveDefinitionException;
import org.ggp.base.util.statemachine.exceptions.TransitionDefinitionException;

public class DepthLimitedSearchPlayer extends GrimgauntPredatorGamer {

	protected static final int MAX_DEPTH = 5;
	protected static long Start = 0;

	@Override
	public Move stateMachineSelectMove(long timeout)
			throws TransitionDefinitionException, MoveDefinitionException,
			GoalDefinitionException {
		Start = System.currentTimeMillis();

		List<Move> moves = getStateMachine().getLegalMoves(getCurrentState(),
				getRole());
		int bestScoreSoFar = -1;
		Move bestMoveSoFar = null;

		for (Move move : moves) {
			int bestScoreAfterMove = Minimize(getCurrentState(), move, 0);
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
				- Start));
		return bestMoveSoFar;
	}

	private boolean timeout() {
		return (System.currentTimeMillis() - Start > TIMEOUT_SAFETY_MARGIN);
	}


	private int Minimize(MachineState state, Move myMove, int depth) throws GoalDefinitionException, MoveDefinitionException, TransitionDefinitionException {
		//Minimize
				int worstScoreSoFar = 100;
				for (List<Move> jointMove : getStateMachine().getLegalJointMoves(state,	getRole(), myMove)) {
					if (timeout()) break;
					MachineState stateAfterMove = getStateMachine().getNextState(state, jointMove);
					int nextScore = Maximize(stateAfterMove, depth);
					worstScoreSoFar = Math.min(worstScoreSoFar, nextScore);
					if (worstScoreSoFar == 0)
						break;
				}
				return worstScoreSoFar;

	}

	private int Maximize(MachineState state, int depth) throws GoalDefinitionException, MoveDefinitionException, TransitionDefinitionException {
		int bestScoreSoFar = -1;
		if (getStateMachine().isTerminal(state) || depth > MAX_DEPTH) {
			bestScoreSoFar = getStateMachine().getGoal(state, getRole());
		} else {
			// Maximize
			List<Move> moves = getStateMachine().getLegalMoves(state, getRole());
			for (Move myNextMove : moves) {
				if (timeout())
					break;
				int bestScoreAfterMove = Minimize(state, myNextMove, (depth + 1));
				bestScoreSoFar = Math.max(bestScoreSoFar, bestScoreAfterMove);
				if (bestScoreSoFar == 100)
					break;
			}
		}
		return bestScoreSoFar;
	}
}

