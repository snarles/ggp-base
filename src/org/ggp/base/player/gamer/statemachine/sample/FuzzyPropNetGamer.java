package org.ggp.base.player.gamer.statemachine.sample;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

import org.ggp.base.apps.player.detail.DetailPanel;
import org.ggp.base.apps.player.detail.SimpleDetailPanel;
import org.ggp.base.player.gamer.Gamer;
import org.ggp.base.player.gamer.event.GamerSelectedMoveEvent;
import org.ggp.base.player.gamer.exception.AbortingException;
import org.ggp.base.player.gamer.exception.GamePreviewException;
import org.ggp.base.player.gamer.exception.MetaGamingException;
import org.ggp.base.player.gamer.exception.MoveSelectionException;
import org.ggp.base.player.gamer.exception.StoppingException;
import org.ggp.base.util.game.Game;
import org.ggp.base.util.gdl.grammar.GdlTerm;
import org.ggp.base.util.logging.GamerLogger;
import org.ggp.base.util.statemachine.MachineState;
import org.ggp.base.util.statemachine.Move;
import org.ggp.base.util.statemachine.Role;
import org.ggp.base.util.statemachine.StateMachine;
import org.ggp.base.util.statemachine.cache.CachedStateMachine;
import org.ggp.base.util.statemachine.exceptions.GoalDefinitionException;
import org.ggp.base.util.statemachine.exceptions.MoveDefinitionException;
import org.ggp.base.util.statemachine.exceptions.TransitionDefinitionException;
import org.ggp.base.util.statemachine.implementation.propnet.FuzzyPropNetMachine;
import org.ggp.base.util.statemachine.implementation.prover.ProverStateMachine;

/**
 * Charles Zheng
 */
public final class FuzzyPropNetGamer extends Gamer
{
	boolean diagnosticMode = true;
    // Internal state about the current state of the state machine.
    private Role role;
    private MachineState currentState;
    private FuzzyPropNetMachine stateMachine;


	public Move stateMachineSelectMove(long timeout) throws TransitionDefinitionException, MoveDefinitionException, GoalDefinitionException
	{
	    FuzzyPropNetMachine theMachine = getStateMachine();
	    long start = System.currentTimeMillis();
		long finishBy = timeout - 1000;
		//stateMachine.printNetState();
		//paused();

		List<Move> moves = theMachine.getLegalMoves(getCurrentState(), getRole());
		theMachine.cacheStuff();
		currentState = theMachine.getCurrentState();
		Move selection = moves.get(0);

		if (moves.size() > 1) {
    		double[] moveTotalPoints = new double[moves.size()];
    		int[] moveTotalAttempts = new int[moves.size()];

    		// Perform depth charges for each candidate move, and keep track
    		// of the total score and total attempts accumulated for each move.
    		for (int i = 0; i < moves.size(); i++) {
    		    //if (System.currentTimeMillis() > finishBy) {break;}
    			//printd("FPNG state: ",getCurrentState().toString());
    			theMachine.loadCache();
		    	//theMachine.printLegals();

    			Move myMove = moves.get(i);
    			double[] ans = {0.0,0.0};
    		    try {
    		    	//printd("FPNG depth charge","");
    		    	//printd("Input state:",theState.toString());
    		    	//theMachine.printCurrentState("from FPNG depth charge: ");
    		    	//printd("Input state:",currentState.toString());
    	            List<Move> rmoves = theMachine.getRandomJointMove(currentState, getRole(), myMove);
    	            MachineState finalState = theMachine.getNextState(currentState, rmoves);
    	            theMachine.printCurrentState("final:");
    	            //paused();theMachine.printNetState();paused();
    	            ans[0]= theMachine.getFuzzyGoal(finalState, getRole());
    	            ans[1]= theMachine.getFuzzyTerminal(finalState);
    	        } catch (Exception e) {
    	            e.printStackTrace();
    	        }



    		    double[] theScore = ans;
    		    //paused();theMachine.printNetState();paused();
    		    printd("Move:".concat(moves.get(i).toString())," : ".concat(String.valueOf(theScore[0])));
    		    printd(" term:",String.valueOf(theScore[1]));
    		    moveTotalPoints[i] += theScore[0];
    		    moveTotalAttempts[i] += 1;
    		}

    		// Compute the expected score for each move.
    		double[] moveExpectedPoints = new double[moves.size()];
    		for (int i = 0; i < moves.size(); i++) {
    		    moveExpectedPoints[i] = (double)moveTotalPoints[i] / moveTotalAttempts[i];
    		}

    		// Find the move with the best expected score.
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
		printd("FPG select move time:",String.valueOf(stop-start));

		notifyObservers(new GamerSelectedMoveEvent(moves, selection, stop - start));
		return selection;
	}

	private int[] depth = new int[1];
	double performDepthChargeFromMove(MachineState theState, Move myMove) {
	    FuzzyPropNetMachine theMachine = getStateMachine();
	    try {
	    	//printd("FPNG depth charge","");
	    	theMachine.setState(theState);
	    	theMachine.synchState();
	    	//printd("Input state:",theState.toString());
	    	//theMachine.printCurrentState("from FPNG depth charge: ");
            MachineState finalState = theMachine.getRandomNextState(theState, getRole(), myMove);
            return theMachine.getFuzzyGoal(finalState, getRole());
        } catch (Exception e) {
            e.printStackTrace();
            return 0;
        }
	}
	//paste from parent class

    // =====================================================================
    // Next, methods which can be used by subclasses to get information about
    // the current state of the game, and tweak the state machine on the fly.

	/**
	 * Returns the current state of the game.
	 */
	public final MachineState getCurrentState()
	{
		return currentState;
	}

	/**
	 * Returns the role that this gamer is playing as in the game.
	 */
	public final Role getRole()
	{
		return role;
	}

	/**
	 * Returns the state machine.  This is used for calculating the next state and other operations, such as computing
	 * the legal moves for all players, whether states are terminal, and the goal values of terminal states.
	 */
	public final FuzzyPropNetMachine getStateMachine()
	{
		return stateMachine;
	}

    /**
     * Cleans up the role, currentState and stateMachine. This should only be
     * used when a match is over, and even then only when you really need to
     * free up resources that the state machine has tied up. Currently, it is
     * only used in the Proxy, for players designed to run 24/7.
     */
    protected final void cleanupAfterMatch() {
        role = null;
        currentState = null;
        stateMachine = null;
        setMatch(null);
        setRoleName(null);
    }

    /**
     * Switches stateMachine to newStateMachine, playing through the match
     * history to the current state so that currentState is expressed using
     * a MachineState generated by the new state machine.
     *
     * This is not done in a thread-safe fashion with respect to the rest of
     * the gamer, so be careful when using this method.
     *
     * @param newStateMachine the new state machine
     */
    protected final void switchStateMachine(FuzzyPropNetMachine newStateMachine) {
        try {
            MachineState newCurrentState = newStateMachine.getInitialState();
            Role newRole = newStateMachine.getRoleFromConstant(getRoleName());

            // Attempt to run through the game history in the new machine
            List<List<GdlTerm>> theMoveHistory = getMatch().getMoveHistory();
            for(List<GdlTerm> nextMove : theMoveHistory) {
                List<Move> theJointMove = new ArrayList<Move>();
                for(GdlTerm theSentence : nextMove)
                    theJointMove.add(newStateMachine.getMoveFromTerm(theSentence));
                newCurrentState = newStateMachine.getNextStateDestructively(newCurrentState, theJointMove);
            }

            // Finally, switch over if everything went well.
            role = newRole;
            currentState = newCurrentState;
            stateMachine = newStateMachine;
        } catch (Exception e) {
            GamerLogger.log("GamePlayer", "Caught an exception while switching state machine!");
            GamerLogger.logStackTrace("GamePlayer", e);
        }
    }

    // =====================================================================
    // Finally, methods which are overridden with proper state-machine-based
	// semantics. These basically wrap a state-machine-based view of the world
	// around the ordinary metaGame() and selectMove() functions, calling the
	// new stateMachineMetaGame() and stateMachineSelectMove() functions after
	// doing the state-machine-related book-keeping.

	/**
	 * A wrapper function for stateMachineMetaGame. When the match begins, this
	 * initializes the state machine and role using the match description, and
	 * then calls stateMachineMetaGame.
	 */

	@Override
	public final void metaGame(long timeout) throws MetaGamingException
	{
		try
		{
			stateMachine = getInitialStateMachine();
			stateMachine.initialize(getMatch().getGame().getRules());
			currentState = stateMachine.getInitialState();
			role = stateMachine.getRoleFromConstant(getRoleName());
			getMatch().appendState(currentState.getContents());

			stateMachineMetaGame(timeout);
		}
		catch (Exception e)
		{
		    GamerLogger.logStackTrace("GamePlayer", e);
			throw new MetaGamingException(e);
		}
	}

	/**
	 * A wrapper function for stateMachineSelectMove. When we are asked to
	 * select a move, this advances the state machine up to the current state
	 * and then calls stateMachineSelectMove to select a move based on that
	 * current state.
	 */

	@Override
	public final GdlTerm selectMove(long timeout) throws MoveSelectionException
	{
		try
		{
			stateMachine.doPerMoveWork();
			//printd("*1*","");
			List<GdlTerm> lastMoves = getMatch().getMostRecentMoves();
			if (lastMoves != null)
			{
				printd("FPNM **Updating match**","");
				List<Move> moves = new ArrayList<Move>();
				for (GdlTerm sentence : lastMoves)
				{
					moves.add(stateMachine.getMoveFromTerm(sentence));
				}
				//printd("*3*","");
				currentState = stateMachine.getNextState(currentState, moves);
				getMatch().appendState(currentState.getContents());
			}

			return stateMachineSelectMove(timeout).getContents();
		}
		catch (Exception e)
		{
		    GamerLogger.logStackTrace("GamePlayer", e);
			throw new MoveSelectionException(e);
		}
	}


	@Override
	public void stop() throws StoppingException {
		try {
			stateMachine.doPerMoveWork();

			List<GdlTerm> lastMoves = getMatch().getMostRecentMoves();
			if (lastMoves != null)
			{
				List<Move> moves = new ArrayList<Move>();
				for (GdlTerm sentence : lastMoves)
				{
					moves.add(stateMachine.getMoveFromTerm(sentence));
				}

				currentState = stateMachine.getNextState(currentState, moves);
				getMatch().appendState(currentState.getContents());
				getMatch().markCompleted(stateMachine.getGoals(currentState));
			}

			stateMachineStop();
		}
		catch (Exception e)
		{
			GamerLogger.logStackTrace("GamePlayer", e);
			throw new StoppingException(e);
		}
	}


	@Override
	public void abort() throws AbortingException {
		try {
			stateMachineAbort();
		}
		catch (Exception e)
		{
			GamerLogger.logStackTrace("GamePlayer", e);
			throw new AbortingException(e);
		}
	}
	public void setSeed(long seed) {
		getStateMachine().setSeed(seed);
	}


    //paste from SampleGamer
	public void stateMachineMetaGame(long timeout) throws TransitionDefinitionException, MoveDefinitionException, GoalDefinitionException
	{
		// Sample gamers do no metagaming at the beginning of the match.
	}



	/** This will currently return "SampleGamer"
	 * If you are working on : public abstract class MyGamer extends SampleGamer
	 * Then this function would return "MyGamer"
	 */

	@Override
	public String getName() {
		return getClass().getSimpleName();
	}

	// This is the default State Machine

	public FuzzyPropNetMachine getInitialStateMachine() {
		return new FuzzyPropNetMachine();
	}

	// This is the defaul Sample Panel

	@Override
	public DetailPanel getDetailPanel() {
		return new SimpleDetailPanel();
	}




	public void stateMachineStop() {
		// Sample gamers do no special cleanup when the match ends normally.
	}


	public void stateMachineAbort() {
		// Sample gamers do no special cleanup when the match ends abruptly.
	}


	@Override
	public void preview(Game g, long timeout) throws GamePreviewException {
		// Sample gamers do no game previewing.
	}

	// diagnostic

	public void paused() {
		if (diagnosticMode) {
			BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
			System.out.println("[PAUSED]");
			try {
				String gameFile = in.readLine();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}

	public void printd(String s, String t) {
		if (diagnosticMode) {
			System.out.println(s.concat(t));
		}
	}
}