package org.ggp.base.player.gamer.statemachine.sample;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

import org.ggp.base.apps.player.detail.DetailPanel;
import org.ggp.base.apps.player.detail.SimpleDetailPanel;
import org.ggp.base.player.gamer.event.GamerSelectedMoveEvent;
import org.ggp.base.player.gamer.exception.GamePreviewException;
import org.ggp.base.player.gamer.statemachine.StateMachineGamer;
import org.ggp.base.util.game.Game;
import org.ggp.base.util.gdl.grammar.GdlSentence;
import org.ggp.base.util.statemachine.MachineState;
import org.ggp.base.util.statemachine.Move;
import org.ggp.base.util.statemachine.StateMachine;
import org.ggp.base.util.statemachine.cache.CachedStateMachine;
import org.ggp.base.util.statemachine.exceptions.GoalDefinitionException;
import org.ggp.base.util.statemachine.exceptions.MoveDefinitionException;
import org.ggp.base.util.statemachine.exceptions.TransitionDefinitionException;
import org.ggp.base.util.statemachine.implementation.prover.ProverStateMachine;

/**
 * SampleSearchLightGamer is a simple state-machine-based Gamer. It will,
 * to the best of its ability, never pick a move which will give its opponent
 * a possible one-move win. It will also spend up to two seconds looking for
 * one-move wins it can take. This makes it slightly more challenging than the
 * RandomGamer, while still playing reasonably fast.
 *
 * Essentially, it has a one-move search-light that it shines out, allowing it
 * to avoid moves that are immediately terrible, and also choose moves that are
 * immediately excellent.
 *
 * This approach implicitly assumes that it is playing an alternating-play game,
 * which is not always true. It will play simultaneous-action games less well.
 * It also assumes that it is playing a zero-sum game, where its opponent will
 * always force it to lose if given that option.
 *
 * This player is fairly good at games like Tic-Tac-Toe, Knight Fight, and Connect Four.
 * This player is pretty terrible at most games.
 *
 * @author Sam Schreiber
 */
public final class EFGamer extends StateMachineGamer
{
	private Set<GdlSentence> knownRelations = new HashSet<GdlSentence>();
	private HashMap<GdlSentence,Integer> evaluationFunction = new HashMap<GdlSentence,Integer>();
	/**
	 * Does nothing
	 */
	@Override
	public void stateMachineMetaGame(long timeout) throws TransitionDefinitionException, MoveDefinitionException, GoalDefinitionException
	{
		CachedStateMachine theMachine = (CachedStateMachine) getStateMachine();
		ProverStateMachine theMachine0 = (ProverStateMachine) theMachine.getStateMachine();
		System.out.println("Hello");
		//theMachine0.qqquery();
		long finishBy = timeout - 3000;
		int counter = 0;
		while (System.currentTimeMillis() < finishBy) {
			Set<GdlSentence> harvestedRelations = theMachine.harvestDepthCharge(getCurrentState(),timeout-4000);
			knownRelations.addAll(harvestedRelations);
			counter++;
		}
		System.out.println();
		System.out.print("Loop count:");
		System.out.println(counter);
		System.out.print("Harvested:");
		System.out.println(knownRelations.size());

		for (GdlSentence g : knownRelations) {
			int xx = theRandom.nextInt(20)-10;
			//System.out.println();
			//System.out.print(g.toString());
			//System.out.print(" = ");
			//System.out.print(xx);
			evaluationFunction.put(g,new Integer(xx));
		}

		// Do nothing.
		// TODO: we may want to look into this too!

	}

	private Random theRandom = new Random();

	/**
	 * Employs a simple sample "Search Light" algorithm.  First selects a default legal move.
	 * It then iterates through all of the legal moves in random order, updating the current move selection
	 * using the following criteria.
	 * <ol>
	 * 	<li> If a move produces a 1 step victory (given a random joint action) select it </li>
	 * 	<li> If a move produces a 1 step loss avoid it </li>
	 * 	<li> If a move allows a 2 step forced loss avoid it </li>
	 * 	<li> Otherwise select the move </li>
	 * </ol>
	 */
	@Override
	public Move stateMachineSelectMove(long timeout) throws TransitionDefinitionException, MoveDefinitionException, GoalDefinitionException
	{
		// TODO: Improve this code!!!  This is where most of the code will go.

	    StateMachine theMachine = getStateMachine();
		long start = System.currentTimeMillis();
		long finishBy = timeout - 1000;

		List<Move> moves = theMachine.getLegalMoves(getCurrentState(), getRole());
		Move selection = (moves.get(new Random().nextInt(moves.size())));

		// Shuffle the moves into a random order, so that when we find the first
		// move that doesn't give our opponent a forced win, we aren't always choosing
		// the first legal move over and over (which is visibly repetitive).
		List<Move> movesInRandomOrder = new ArrayList<Move>();
		while(!moves.isEmpty()) {
		    Move aMove = moves.get(theRandom.nextInt(moves.size()));
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

		MachineState s = getCurrentState();
		System.out.println();
		System.out.print("The current state");
		System.out.print(s.toString());
		System.out.print("is worth");
		System.out.print(evaluateState(s));

		int best = -1000;
		for(Move moveUnderConsideration : movesInRandomOrder) {
		    // Check to see if there's time to continue.
		    if(System.currentTimeMillis() > finishBy) break;

		    // Get the next state of the game, if we take the move we're considering.
		    // Since it's our turn, in an alternating-play game the opponent will only
		    // have one legal move, and so calling "getRandomJointMove" with our move
		    // fixed will always return the joint move consisting of our move and the
		    // opponent's no-op. In a simultaneous-action game, however, the opponent
		    // may have many moves, and so we will randomly pick one of our opponent's
		    // possible actions and assume they do that.
		    MachineState nextState = theMachine.getNextState(getCurrentState(), theMachine.getRandomJointMove(getCurrentState(), getRole(), moveUnderConsideration));
		    int value = evaluateState(nextState);

		    System.out.println();
			System.out.print("The state");
			System.out.print(nextState.toString());
			System.out.print("is worth");
			System.out.print(value);

		    if (value > best) {
		    	best = value;
		    	selection=moveUnderConsideration;
		    }
		}


		long stop = System.currentTimeMillis();

		notifyObservers(new GamerSelectedMoveEvent(moves, selection, stop - start));
		return selection;
	}

	private int evaluateState(MachineState s){
		int sum = 0;
		for (GdlSentence g : knownRelations) {
			if (s.containsRelation(g)) {
				sum = sum+evaluationFunction.get(g).intValue();
			}
		}
		return sum;
	}

	@Override
	public void stateMachineStop() {
		// Do nothing.
	}
	/**
	 * Uses a CachedProverStateMachine
	 */
	@Override
	public StateMachine getInitialStateMachine() {
		return new CachedStateMachine(new ProverStateMachine());
	}

	@Override
	public String getName() {
		return "EFGamer";
	}

	@Override
	public DetailPanel getDetailPanel() {
		return new SimpleDetailPanel();
	}

	@Override
	public void preview(Game g, long timeout) throws GamePreviewException {
		// Do nothing.
	}

	@Override
	public void stateMachineAbort() {
		// Do nothing.
	}


}