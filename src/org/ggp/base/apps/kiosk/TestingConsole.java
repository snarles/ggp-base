package org.ggp.base.apps.kiosk;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.List;
import java.util.Random;
import java.util.Scanner;

import org.ggp.base.player.gamer.exception.MetaGamingException;
import org.ggp.base.player.gamer.exception.MoveSelectionException;
import org.ggp.base.player.gamer.statemachine.cs227b.MCTSGamer;
import org.ggp.base.util.game.Game;
import org.ggp.base.util.gdl.grammar.Gdl;
import org.ggp.base.util.gdl.grammar.GdlConstant;
import org.ggp.base.util.gdl.grammar.GdlTerm;
import org.ggp.base.util.match.Match;
import org.ggp.base.util.statemachine.MachineState;
import org.ggp.base.util.statemachine.Move;
import org.ggp.base.util.statemachine.StateMachine;
import org.ggp.base.util.statemachine.exceptions.MoveDefinitionException;
import org.ggp.base.util.statemachine.exceptions.TransitionDefinitionException;
import org.ggp.base.util.statemachine.implementation.prover.ProverStateMachine;


public class TestingConsole {
	//Change this:
	String dir = "C:/github/ggp-base/games/gamemaster/";
	//String gamef = "alquerque.kif";
	//String gamef = "connectfour.kif";
	String gamef = "pentago.kif";
	//String gamef = "skirmish.kif";


	String gameFile = dir.concat(gamef);
	StateMachine psm = new ProverStateMachine();
	MachineState currentState;
	int currentTurn = -1;
	boolean messageEachTurn = true;

	public static void main(String[] args) throws IOException, MoveDefinitionException, FileNotFoundException, TransitionDefinitionException, MetaGamingException, MoveSelectionException {
		TestingConsole tc = new TestingConsole();
		tc.run();
	}

	public void run() throws IOException, MoveDefinitionException, FileNotFoundException, TransitionDefinitionException, MetaGamingException, MoveSelectionException {
		Scanner scanman = new Scanner(new File(gameFile));
		String content = scanman.useDelimiter("\\Z").next();
		scanman.close();
		//System.out.println(content);
		String content2 = Game.preprocessRulesheet(content);
		System.out.println(content2);
		Game theGame = Game.createEphemeralGame(content2);
		List<Gdl> theRules = theGame.getRules();
		psm.initialize(theRules);

		Match theMatch = new Match("test",-1,10,10,theGame);
		MCTSGamer sean = new MCTSGamer();
		sean.setName("sean");
		MCTSGamer charles = new MCTSGamer();
		charles.setName("charles");
		GdlConstant r = psm.getRoles().get(0).getName();
		GdlConstant r2 = psm.getRoles().get(1).getName();
		sean.setMatch(theMatch);
		sean.setRoleName(r);
		charles.setMatch(theMatch);
		charles.setRoleName(r2);

		long receptionTime = System.currentTimeMillis();
		sean.metaGame(receptionTime + 10000);
		charles.metaGame(System.currentTimeMillis() + 10000);
		GdlTerm move1 = sean.selectMove(System.currentTimeMillis() + 30000);
		pause();
		GdlTerm move2 = charles.selectMove(System.currentTimeMillis() + 30000);
		//System.out.println(move.toString());
	}

	public void run2() throws IOException, MoveDefinitionException, FileNotFoundException, TransitionDefinitionException {
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
		//psm = new CachedStateMachine(psm);
		psm.initialize(theRules);
		elapsed = System.currentTimeMillis()-start;
		message = "Time to initialize: ";
		message = message.concat(String.valueOf(elapsed));
		System.out.println(message);


		//System.out.println(currentState.toString());
		int nits = 1;
		messageEachTurn= false; nits=5;
		for (int j =1; j< 1+nits; j++) {
			currentTurn = -1;
			start = System.currentTimeMillis();
			for (int i = 1; i < 15; i++) {
				randomAdvance();
			}
			elapsed = System.currentTimeMillis()-start;

			message = "Total time: ";
			message = message.concat(String.valueOf(elapsed));
			System.out.println(message);
		}
	}

	// chooses a legal joint move at random and advances the game
	public void randomAdvance() throws TransitionDefinitionException, MoveDefinitionException {
		long start; long elapsed; String message;
		List<Move> selectedMove;
		List<List<Move>> legals;
		if (currentTurn == -1) {
			currentTurn++;
			start = System.currentTimeMillis();
			currentState = psm.getInitialState();
			elapsed = System.currentTimeMillis()-start;
			message = "Time to get initial state: ";
			message = message.concat(String.valueOf(elapsed));
			if (messageEachTurn) {
				System.out.println(message);
			}
		}
		else {
			currentTurn++;
			if (messageEachTurn) {
				System.out.println("Turn: ".concat(String.valueOf(currentTurn)));
			}
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
		if (messageEachTurn) {
			System.out.println(message);
		}
		return moves;
	}

	public MachineState getNextState(List<Move> move) throws TransitionDefinitionException {
		long start = System.currentTimeMillis();
		MachineState newState = psm.getNextState(currentState,move);
		long elapsed = System.currentTimeMillis()-start;
		String message = "Time to get next state: ";
		message = message.concat(String.valueOf(elapsed));
		if (messageEachTurn) {
			System.out.println(message);
		}
		return newState;
	}

	public void pause() {
		BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
		System.out.println("Enter game file");
		try {
			String gameFile = in.readLine();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

}
