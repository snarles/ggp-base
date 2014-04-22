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
 * Compulsive deliberation player example, which is brute-force search, but with search depth
 * limit and timeout limit.
 * Also, now looks one move ahead to make sure we're not facing a forced loss in the move after the one
 * having the highest score found so far, as a small prelude to the whole Minimax thing that comes next.
 */
public class CompulsiveDeliberationGamer extends GrimgauntPredatorGamer {
	private static final int MAX_SEARCH_DEPTH = 20;
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
		final Role currentRole = getRole();
		final MachineState currentState = getCurrentState();
		System.out.println("stateMachineSelectMove(): entering, role is " + currentRole + ", timeout is " + timeout);
		List<Move> allLegalMoves = null;
		try {
			allLegalMoves = theMachine.getLegalMoves(currentState, currentRole);
			System.out.println("stateMachineSelectMove(): Got " + allLegalMoves.size() + " moves.");
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
		} else {
			System.err.println("stateMachineSelectMove(): ERROR: no legal moves found");
		}
		if (selection == null) {
			System.err.println("stateMachineSelectMove(): ERROR: Could not select a move!  Move selection is null.");
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
	protected Move findBestMove(final MachineState currentState, final Role role, final long timeout)
			throws MoveDefinitionException, TransitionDefinitionException, GoalDefinitionException {
		Move result = null;
		if (currentState == null || role == null || theMachine == null) {
			System.err.println("findBestMove(): ERROR: null state, move or role");
		} else {
			int bestScoreFound = MINIMUM_GAME_GOAL;
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
						final List<List<Move>> allLegalJointMoves = theMachine.getLegalJointMoves(currentState, role, moveUnderConsideration);
						for (final List<Move> nextJointMove : allLegalJointMoves) {
							//final List<Move> nextJointMove = theMachine.getRandomJointMove(currentState, role, moveUnderConsideration);
							final MachineState nextState = theMachine.getNextState(currentState, nextJointMove);
							int score = getMoveScore(nextState, role, moveUnderConsideration, MAX_SEARCH_DEPTH, timeout);
							// ***************** Following code ripped off from SampleSearchLightGamer ********************
						    boolean forcedLoss = false;
						    if (score > MINIMUM_GAME_GOAL) {
						    	try {
									for (final List<Move> nextNextJointMove : theMachine.getLegalJointMoves(nextState)) {
										if (isAlmostTimedOut(timeout)) {
											System.err.println("findBestMove(): WARNING: timed out. Searching has ended.");
											break;
										}
										try {
											final MachineState nextNextState = theMachine.getNextState(nextState, nextNextJointMove);
									    	if (theMachine.isTerminal(nextNextState) && theMachine.getGoal(nextNextState, role) <= MINIMUM_GAME_GOAL) {	// we lose
												forcedLoss = true;
												System.err.println("findBestMove(): Considered bad move " + nextNextJointMove + " with score " + score);
												break;
									    	}
										} catch (GoalDefinitionException gde) {
											System.err.println("findBestMove(): GoalDefinitionException: " + gde.getMessage());
										}
								    }
						    	} catch (MoveDefinitionException mse) {
									System.err.println("findBestMove(): MoveDefinitionException: " + mse.getMessage());
						    	}
						    }
							// *********************************************************************************************
							if (forcedLoss) {
								score /= 2;			// Still need to consider score, but let's try dividing by two.
							}
							if (score > bestScoreFound) {
								bestScoreFound = score;
								result = moveUnderConsideration;
							}
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

	/**
	 * Recursive method to get score for a move under consideration.
	 *
	 * @param searchedGameState MachineState
	 * @param role Role
	 * @param searchedMove Move
	 * @param searchDepth int
	 * @param timeout long
	 * @return int
	 * @throws GoalDefinitionException
	 * @throws MoveDefinitionException
	 * @throws TransitionDefinitionException
	 */
	private int getMoveScore(final MachineState searchedGameState, final Role role, final Move searchedMove, int searchDepth, final long timeout)
			throws GoalDefinitionException, TransitionDefinitionException, MoveDefinitionException {
		int result = MINIMUM_GAME_GOAL;
		if (searchedGameState == null || searchedMove == null || role == null || theMachine == null) {
			System.err.println("getMoveScore(): ERROR: bad game state, move or role");
			result = Integer.MIN_VALUE;		// Out of range score indicates error
		} else if (theMachine.isTerminal(searchedGameState)) {
			// Best possible situation: return goal value at terminal game state
			result = theMachine.getGoal(searchedGameState, role);
		} else if (searchDepth <= 0) {
			// No measure of score possible at non-terminal state.  Approximates Random player if search depth is very shallow.
			System.out.println("getMoveScore(): WARNING: At depth limit, returning random score");
			result = theRandom.nextInt(MAXIMUM_GAME_GOAL);		// [0..MAXIMUM_GAME_GOAL-1]
		} else {
			final List<Move> legalMovesForRoleInState = theMachine.getLegalMoves(searchedGameState, role);
			if (legalMovesForRoleInState != null && !legalMovesForRoleInState.isEmpty()) {
				for (final Move nextMove : legalMovesForRoleInState) {
					if (isAlmostTimedOut(timeout)) {
						System.err.println("getMoveScore(): WARNING: timed out. Searching has ended.");
						break;
					}
					final MachineState nextState = theMachine.getNextState(searchedGameState,
							theMachine.getRandomJointMove(searchedGameState, role, nextMove));
					final int tmpScore = getMoveScore(nextState, role, nextMove, searchDepth-1, timeout);
					if (tmpScore > result) {
						result = tmpScore;
					}
				}
			} else {
				System.err.println("getMoveScore(): ERROR: no legal moves found");
			}
		}
		if (result > MINIMUM_GAME_GOAL) {
			System.out.println("getMoveScore(): exiting, score is " + result + ", at depth " + searchDepth);
		}
		return result;
	}
}
