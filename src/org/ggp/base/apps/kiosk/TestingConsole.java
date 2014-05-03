package org.ggp.base.apps.kiosk;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.List;
import java.util.Random;
import java.util.Scanner;

import org.ggp.base.util.game.Game;
import org.ggp.base.util.gdl.grammar.Gdl;
import org.ggp.base.util.statemachine.MachineState;
import org.ggp.base.util.statemachine.Move;
import org.ggp.base.util.statemachine.Role;
import org.ggp.base.util.statemachine.StateMachine;
import org.ggp.base.util.statemachine.exceptions.MoveDefinitionException;
import org.ggp.base.util.statemachine.exceptions.TransitionDefinitionException;
import org.ggp.base.util.statemachine.implementation.prover.ProverStateMachine;


public class TestingConsole {
	StateMachine psm = new ProverStateMachine();
	MachineState currentState;

	public static void main(String[] args) throws IOException, MoveDefinitionException, FileNotFoundException, TransitionDefinitionException {
		TestingConsole tc = new TestingConsole();
		tc.run();
	}

	public void run() throws IOException, MoveDefinitionException, FileNotFoundException, TransitionDefinitionException {
		/**BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
		System.out.println("Enter game file");
		String gameFile = in.readLine();**/
		//String gameFile = "/Users/snarles/github/ggp-base/games/gamemaster/connectfour.kif";

		long start = 0;
		long elapsed = 0;
		String message = "";
		List<Move> selectedMove;
		MachineState newState;
		List<List<Move>> legals;

		String gameFile = "C:/github/ggp-base/games/gamemaster/skirmish.kif";
		/**BufferedReader gameRead = new BufferedReader(new FileReader(gameFile));
		String text = gameRead.read();
		System.out.println(text);**/
		Scanner scanman = new Scanner(new File(gameFile));
		String content = scanman.useDelimiter("\\Z").next();
		scanman.close();
		//System.out.println(content);
		String content2 = Game.preprocessRulesheet(content);
		System.out.println(content2);
		Game theGame = Game.createEphemeralGame(content2);
		List<Gdl> theRules = theGame.getRules();
		//for (Gdl g : theRules) {
		//	System.out.println(g.toString());
		//}

		start = System.currentTimeMillis();
		//StateMachine psm = new ProverStateMachine();
		psm.initialize(theRules);
		elapsed = System.currentTimeMillis()-start;
		message = "Time to initialize: ";
		message = message.concat(String.valueOf(elapsed));
		System.out.println(message);

		start = System.currentTimeMillis();
		currentState = psm.getInitialState();
		elapsed = System.currentTimeMillis()-start;
		message = "Time to get initial state: ";
		message = message.concat(String.valueOf(elapsed));
		System.out.println(message);

		//System.out.println(currentState.toString());

		List<Role> roles = psm.getRoles();
		for (int i = 1; i < 11; i++) {
			System.out.println("Iteration: ".concat(String.valueOf(i)));
			legals = getMoves();
			selectedMove = legals.get(new Random().nextInt(legals.size()));
			currentState = getNextState(selectedMove);
		}

	}

	public List<List<Move>> getMoves() throws MoveDefinitionException {
		long start = System.currentTimeMillis();
		List<List<Move>> moves = psm.getLegalJointMoves(currentState);
		long elapsed = System.currentTimeMillis()-start;
		String message = "Time to get moves: ";
		message = message.concat(String.valueOf(elapsed));
		System.out.println(message);
		return moves;
	}

	public MachineState getNextState(List<Move> move) throws TransitionDefinitionException {
		long start = System.currentTimeMillis();
		MachineState newState = psm.getNextState(currentState,move);
		long elapsed = System.currentTimeMillis()-start;
		String message = "Time to get next state: ";
		message = message.concat(String.valueOf(elapsed));
		System.out.println(message);
		return newState;
	}

}
