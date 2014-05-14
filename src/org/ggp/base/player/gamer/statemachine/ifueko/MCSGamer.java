package org.ggp.base.player.gamer.statemachine.ifueko;

import java.util.List;

import org.ggp.base.player.gamer.event.GamerSelectedMoveEvent;
import org.ggp.base.util.statemachine.MachineState;
import org.ggp.base.util.statemachine.Move;
import org.ggp.base.util.statemachine.StateMachine;
import org.ggp.base.util.statemachine.exceptions.GoalDefinitionException;
import org.ggp.base.util.statemachine.exceptions.MoveDefinitionException;
import org.ggp.base.util.statemachine.exceptions.TransitionDefinitionException;


public class MCSGamer extends GrimgauntPredatorGamer {

	int[] moveTotalPoints;
	int[] moveTotalAttempts;

	class DepthChargeWorker extends Thread {
		int mvIdx;
		DepthChargeWorker(int moveIdx) {
			super();
			mvIdx = moveIdx;
		}

		void runDepthCharge(Move move) {
			int theScore = performDepthChargeFromMove(getCurrentState(), move);
		    moveTotalPoints[mvIdx] += theScore;
		    moveTotalAttempts[mvIdx] += 1;
		}

	}

	@Override
	public Move stateMachineSelectMove(long timeout) throws TransitionDefinitionException, MoveDefinitionException, GoalDefinitionException
	{
	    StateMachine theMachine = getStateMachine();
		long start = System.currentTimeMillis();
		long finishBy = timeout - 1000;

		List<Move> moves = theMachine.getLegalMoves(getCurrentState(), getRole());
		Move selection = moves.get(0);
		if (moves.size() > 1) {
    		int[] moveTotalPoints = new int[moves.size()];
    		int[] moveTotalAttempts = new int[moves.size()];
    		//init countdown latch
    		for (int i = 0; true; i = (i+1) % moves.size()) {
    		    if (System.currentTimeMillis() > finishBy)
    		        break;
    		    //initialize workers

    		    int theScore = performDepthChargeFromMove(getCurrentState(), moves.get(i));
    		    moveTotalPoints[i] += theScore;
    		    moveTotalAttempts[i] += 1;

    		}
    		//wait for latch while time is okay. then move on
    		double[] moveExpectedPoints = new double[moves.size()];
    		for (int i = 0; i < moves.size(); i++) {
    		    moveExpectedPoints[i] = (double)moveTotalPoints[i] / moveTotalAttempts[i];
    		}
    		int bestMove = 0;
    		double bestMoveScore = moveExpectedPoints[0];
    		for (int i = 1; i < moves.size(); i++) {
    		    if (moveExpectedPoints[i] > bestMoveScore) {
    		        bestMoveScore = moveExpectedPoints[i];
    		        bestMove = i;
    		    }
    		}
    		selection = moves.get(bestMove);
		}

		long stop = System.currentTimeMillis();

		notifyObservers(new GamerSelectedMoveEvent(moves, selection, stop - start));
		return selection;
	}

	private int[] depth = new int[1];
	int performDepthChargeFromMove(MachineState theState, Move myMove) {
	    StateMachine theMachine = getStateMachine();
	    try {
            MachineState finalState = theMachine.performDepthCharge(theMachine.getRandomNextState(theState, getRole(), myMove), depth);
            return theMachine.getGoal(finalState, getRole());
        } catch (Exception e) {
            e.printStackTrace();
            return 0;
        }
	}
}