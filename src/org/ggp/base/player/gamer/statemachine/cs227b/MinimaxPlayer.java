package org.ggp.base.player.gamer.statemachine.cs227b;

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
 * Base class for Grimgaunt Predator.  Extend this abstract class and implement the following methods:
 *		stateMachineSelectMove
 *		stateMachineMetaGame (optional)
 * For example, a stateless exhaustive search would only implement stateMachineSelectMove, with appropriate class name:
 *		public class GrimgauntPredatorStatelessBritishMuseumGamer extends GrimgauntPredatorGamer {
 *			@override public Move stateMachineSelectMove(final long timeout) { Exhaustive search code... }
 *		}
 */
public class MinimaxPlayer extends GrimgauntPredatorGamer {

	private static final int MAX_SEARCH_DEPTH = 20;
	private static Role Player;
	private final Random theRandom = new Random();
	StateMachine theMachine = null;
	int numberOfRoles = -1;

	@Override
	public void stateMachineMetaGame(final long timeout)
			throws TransitionDefinitionException, MoveDefinitionException, GoalDefinitionException {
		System.out.println("stateMachineMetaGame(): entering");
		if (theMachine == null) {
			theMachine = getStateMachine();
		}
		numberOfRoles = theMachine.getRoles().size();
		System.out.println("stateMachineMetaGame(): exiting, players=" + numberOfRoles);
	}

	@Override
	public Move stateMachineSelectMove(final long timeout)
			throws TransitionDefinitionException, MoveDefinitionException, GoalDefinitionException {
		final long startTimeMs = System.currentTimeMillis();
		Role currentRole = getRole();
		Player = currentRole;
		final MachineState currentState = getCurrentState();
		System.out.println("stateMachineSelectMove(): entering, role is " + currentRole + ", timeout is " + timeout);
		List<Move> allLegalMoves = null;
		try {
			allLegalMoves = theMachine.getLegalMoves(currentState, currentRole);
		} catch (MoveDefinitionException mde) {
			System.err.println("stateMachineSelectMove(): MoveDefinitionException: " + mde.getMessage());
		}
		Move selection = null;
		if (allLegalMoves != null && !allLegalMoves.isEmpty()) {
			if (allLegalMoves.size() == 1) {
				// No choice possible.  Don't bother calling findBestMove.  Most likely the move is noop.
				selection = allLegalMoves.get(0);
			} else {
				selection = findBestMove(currentState, currentRole, timeout);
			}
		}
		if (selection == null) {
			System.err.println("stateMachineSelectMove(): ERROR: no legal moves found");
		}
		notifyObservers(new GamerSelectedMoveEvent(allLegalMoves, selection, System.currentTimeMillis() - startTimeMs));
		System.out.println("stateMachineSelectMove(): exiting, move=" + selection);
		return selection;
	}

	/**
	 * Get best move given current game state, role, maximum search depth and timeout.
	 *
	 * @param currentState MachineState
	 * @param role Role
	 * @param searchDepth int
	 * @param timeout long
	 * @return Move
	 * @throws MoveDefinitionException
	 * @throws TransitionDefinitionException
	 * @throws GoalDefinitionException
	 */
	protected Move findBestMove(final MachineState currentState, Role role, final long timeout)
			throws MoveDefinitionException, TransitionDefinitionException, GoalDefinitionException {
		Move result = null;
		if (currentState == null || role == null || theMachine == null) {
			System.err.println("findBestMove(): ERROR: null state, move or role");
		} else {
			int bestScoreFound = Integer.MIN_VALUE;
			final List<Move> legalMovesForRoleInState = theMachine.getLegalMoves(currentState, role);
			if (legalMovesForRoleInState != null && !legalMovesForRoleInState.isEmpty()) {
				for (final Move moveUnderConsideration : legalMovesForRoleInState) {
					if (isAlmostTimedOut(timeout)) {
						System.err.println("findBestMove(): WARNING: timed out. Searching has ended.");
						break;
					}
					if (moveUnderConsideration == null) {
						System.err.println("findBestMove(): ERROR: null move gotten");
					} else {
						final MachineState nextState = theMachine.getNextState(currentState,
								theMachine.getRandomJointMove(currentState, role, moveUnderConsideration));
						final int score = minimize(nextState, moveUnderConsideration, MAX_SEARCH_DEPTH, timeout);
						if (score > bestScoreFound) {
							bestScoreFound = score;
							result = moveUnderConsideration;
						}
					}
				}
			} else {
				System.err.println("findBestMove(): ERROR: no legal moves found");
			}
		}
		System.out.println("findBestMove(): exiting, move=" + result);
		return result;
	}


	protected int minimize(final MachineState GameState, final Move searchedMove, int searchDepth, final long timeout)
			throws GoalDefinitionException, TransitionDefinitionException, MoveDefinitionException {
		int score = 100;
		Role role = getRole();
		List<Move> legalMoves = theMachine.getLegalMoves(GameState, role);
		for (final Move move : legalMoves) {
			final MachineState nextState = theMachine.getNextState(GameState,
					theMachine.getRandomJointMove(GameState, role, move));
			int newScore = maximize(nextState, move, (searchDepth-1), timeout);
			if (newScore < score) score = newScore;
		}
		return score;
	}

	protected int maximize(final MachineState GameState, final Move searchedMove, int searchDepth, final long timeout)
			throws GoalDefinitionException, TransitionDefinitionException, MoveDefinitionException {
		int score = 0;
		Role role = getRole();
		List<Move> legalMoves = theMachine.getLegalMoves(GameState, role);
		for (final Move move : legalMoves) {
			int newScore = 0;
			final MachineState nextState = theMachine.getNextState(GameState,
					theMachine.getRandomJointMove(GameState, role, move));
			if (searchDepth == 0 || theMachine.isTerminal(GameState)) {
				newScore = theMachine.getGoal(GameState, role);
			} else {
				newScore = minimize(nextState, move, searchDepth, timeout);
			}
			if (newScore > score) score = newScore;

		}
		return score;
	}

}