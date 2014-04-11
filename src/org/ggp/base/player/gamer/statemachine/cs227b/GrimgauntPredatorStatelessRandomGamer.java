package org.ggp.base.player.gamer.statemachine.cs227b;

import java.util.List;
import java.util.Random;

import org.ggp.base.util.statemachine.Move;
import org.ggp.base.util.statemachine.exceptions.GoalDefinitionException;
import org.ggp.base.util.statemachine.exceptions.MoveDefinitionException;
import org.ggp.base.util.statemachine.exceptions.TransitionDefinitionException;

/**
 * Simplest possible GrimgauntPredatorGamer.  Selects a random legal move.
 */
public class GrimgauntPredatorStatelessRandomGamer extends GrimgauntPredatorGamer {

	@Override
	public void stateMachineMetaGame(long timeout)
			throws TransitionDefinitionException, MoveDefinitionException, GoalDefinitionException {
		// Do nothing
	}

	@Override
	public Move stateMachineSelectMove(long timeout)
			throws TransitionDefinitionException, MoveDefinitionException, GoalDefinitionException {
		final List<Move> moves = getStateMachine().getLegalMoves(getCurrentState(), getRole());
		return moves.get(new Random().nextInt(moves.size()));
	}
}
