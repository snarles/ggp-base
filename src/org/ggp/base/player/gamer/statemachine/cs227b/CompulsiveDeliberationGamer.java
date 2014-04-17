package org.ggp.base.player.gamer.statemachine.cs227b;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.ggp.base.player.gamer.event.GamerSelectedMoveEvent;
import org.ggp.base.util.statemachine.MachineState;
import org.ggp.base.util.statemachine.Move;
import org.ggp.base.util.statemachine.Role;
import org.ggp.base.util.statemachine.StateMachine;
import org.ggp.base.util.statemachine.exceptions.GoalDefinitionException;
import org.ggp.base.util.statemachine.exceptions.MoveDefinitionException;
import org.ggp.base.util.statemachine.exceptions.TransitionDefinitionException;

/**
 * Based upon SampleSearchLightGamer, but extended to handle non-terminal states.
 */
public class CompulsiveDeliberationGamer extends GrimgauntPredatorGamer {
	private static final int MAX_SEARCH_DEPTH = 10;
	private final Random theRandom = new Random();
	StateMachine theMachine = null;
	int players = -1;

	@Override
	public void stateMachineMetaGame(final long timeout)
			throws TransitionDefinitionException, MoveDefinitionException, GoalDefinitionException {
		System.out.println("stateMachineMetaGame(): entering");
		if (theMachine == null) {
			theMachine = getStateMachine();
		}
		players = theMachine.getRoles().size();
		System.out.println("stateMachineMetaGame(): exiting, players=" + players);
	}

	@Override
	public Move stateMachineSelectMove(final long timeout)
			throws TransitionDefinitionException, MoveDefinitionException, GoalDefinitionException {

		System.out.println("stateMachineSelectMove(): entering");
		final long start = System.currentTimeMillis();			// Start time
		final List<Move> moves = theMachine.getLegalMoves(getCurrentState(), getRole());
		Move selection = moves.get(new Random().nextInt(moves.size()));

		// Shuffle the moves into a random order, so that when we find the first
		// move that doesn't give our opponent a forced win, we aren't always choosing
		// the first legal move over and over (which is visibly repetitive).
		final List<Move> movesInRandomOrder = new ArrayList<Move>();
		while (!moves.isEmpty()) {
			final Move aMove = moves.get(theRandom.nextInt(moves.size()));
			movesInRandomOrder.add(aMove);
			moves.remove(aMove);
		}

		// Go through all of the legal moves in a random over, and consider each one.
		// For each move, we want to determine whether taking that move will give our
		// opponent a one-move win. We're also interested in whether taking that move
		// will immediately cause us to win or lose.
		//
		// Our main goal is to find a move which won't give our opponent a one-move win.
		// We will also continue considering moves for two seconds, in case we can stumble
		// upon a move which would cause us to win: if we find such a move, we will just
		// immediately take it.
		int highestGoalFound = 0;
		for (final Move moveUnderConsideration : movesInRandomOrder) {

			// Check to see if there's time to continue.
			if (isAlmostTimedOut(timeout)) break;

			// Get the next state of the game, if we take the move we're considering.
			// Since it's our turn, in an alternating-play game the opponent will only
			// have one legal move, and so calling "getRandomJointMove" with our move
			// fixed will always return the joint move consisting of our move and the
			// opponent's no-op. In a simultaneous-action game, however, the opponent
			// may have many moves, and so we will randomly pick one of our opponent's
			// possible actions and assume they do that.
			MachineState nextState = theMachine.getNextState(getCurrentState(), theMachine.getRandomJointMove(getCurrentState(), getRole(), moveUnderConsideration));

			// Does the move under consideration end the game? If it does, do we win
			// or lose? If we lose, don't bother considering it. If we win, then we
			// definitely want to take this move. If its goal is better than our current
			// best goal, go ahead and tentatively select it
			if (theMachine.isTerminal(nextState)) {
				if (theMachine.getGoal(nextState, getRole()) == MINIMUM_GAME_GOAL) {
					continue;
				} else if (theMachine.getGoal(nextState, getRole()) == MAXIMUM_GAME_GOAL) {
					selection = moveUnderConsideration;
					break;
				} else {
					if (theMachine.getGoal(nextState, getRole()) > highestGoalFound) {
						selection = moveUnderConsideration;
						highestGoalFound = theMachine.getGoal(nextState, getRole());
					}
					continue;
				}
			} else {
				selection = getBestMove(nextState, getRole(), moveUnderConsideration, timeout);
			}

			// Check whether any of the legal joint moves from this state lead to
			// a loss for us. Again, this only makes sense in the context of an alternating
			// play zero-sum game, in which this is the opponent's move and they are trying
			// to make us lose, and so if they are offered any move that will make us lose
			// they will take it.
			boolean forcedLoss = false;
			for (final List<Move> jointMove : theMachine.getLegalJointMoves(nextState)) {
				final MachineState nextNextState = theMachine.getNextState(nextState, jointMove);
				if (theMachine.isTerminal(nextNextState)) {
					if (theMachine.getGoal(nextNextState, getRole()) == MINIMUM_GAME_GOAL) {
						forcedLoss = true;
						break;
					}
				}
				// Check to see if there's time to continue.
				if (isAlmostTimedOut(timeout)) {
					forcedLoss = true;
					break;
				}
			}

			// If we've verified that this move isn't going to lead us to a state where
			// our opponent can defeat us in one move, we should keep track of it.
			if (!forcedLoss) {
				selection = moveUnderConsideration;
			}
		}

		final long stop = System.currentTimeMillis();		// End time
		notifyObservers(new GamerSelectedMoveEvent(moves, selection, stop - start));
		System.out.println("stateMachineSelectMove(): exiting, move=" + selection);
		return selection;
	}

	// **********************************************************************************************

	/**
	 * Converted from JavaScript examples in notes.
	 *
	 * @param state State
	 * @param role Role
	 * @return Move
	 * @throws MoveDefinitionException
	 * @throws TransitionDefinitionException
	 * @throws GoalDefinitionException
	 */
	private Move getBestMove(final MachineState state, final Role role, final Move move, final long timeout)
			throws MoveDefinitionException, TransitionDefinitionException, GoalDefinitionException {

		System.out.println("getBestMove(): entering");
		final List<List<Move>> actions = theMachine.getLegalJointMoves(state, role, move);
		Move action = actions.get(0).get(0);
		int score = 0;
		for (int i = 0; i < actions.size(); ++i) {
			// Check to see if there's time to continue.
			if (isAlmostTimedOut(timeout)) break;

			final int result = getMaxScore(role, simulate(actions.get(i), state), move, MAX_SEARCH_DEPTH, timeout);
			if (result == MAXIMUM_GAME_GOAL) {
				return actions.get(i).get(0);
			}
			if (result > score) {
				score = result;
				action = actions.get(i).get(0);
			}
		}
		System.out.println("getBestMove(): exiting, move=" + move);
		return action;
	}

	/**
	 * Converted from JavaScript examples in notes.
	 *
	 * @param role Role
	 * @param state MachineState
	 * @return int
	 * @throws GoalDefinitionException
	 * @throws MoveDefinitionException
	 * @throws TransitionDefinitionException
	 */
	protected int getMaxScore(final Role role, final MachineState state, final Move move, final int depth, final long timeout)
			throws GoalDefinitionException, MoveDefinitionException, TransitionDefinitionException {

		System.out.println("getMaxScore(): entering");
		if (theMachine.isTerminal(state)) {
			return theMachine.getGoal(state, role);
		}
		if (depth <= 0) {
			return MINIMUM_GAME_GOAL;		// TODO: Is this correct?
		}
		final List<List<Move>> actions = theMachine.getLegalJointMoves(state, role, move);
		int score = 0;
		for (int i = 0; i < actions.size(); ++i) {
			// Check to see if there's time to continue.
			if (isAlmostTimedOut(timeout)) break;

			final int result = getMaxScore(role, simulate(actions.get(i), state), move, depth-1, timeout);
			if (result > score) {
				score = result;
			}
		}
		System.out.println("getMaxScore(): exiting, score=" + score);
		return score;
	}

	/**
	 * Converted from JavaScript examples in notes.
	 *
	 * @param move Move
	 * @param state MachineState
	 * @return MachineState
	 * @throws TransitionDefinitionException
	 */
	protected MachineState simulate(final List<Move> moves, final MachineState state)
			throws TransitionDefinitionException {

		System.out.println("simulate(): entering");
		if (moves == null || moves.size() < players) {
			return state;
		};
		final MachineState nextState = theMachine.getNextState(state, moves);
		System.out.println("simulate(): exiting, state=" + nextState);
		return nextState;
	}
}
