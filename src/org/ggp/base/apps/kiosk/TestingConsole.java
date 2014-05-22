package org.ggp.base.apps.kiosk;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.Scanner;
import java.util.Set;

import org.ggp.base.player.gamer.exception.MetaGamingException;
import org.ggp.base.player.gamer.exception.MoveSelectionException;
import org.ggp.base.player.gamer.statemachine.sample.FuzzyPropNetGamer;
import org.ggp.base.player.gamer.statemachine.sample.SampleLegalGamer;
import org.ggp.base.player.gamer.statemachine.sample.SampleMonteCarloGamer;
import org.ggp.base.util.game.Game;
import org.ggp.base.util.gdl.grammar.Gdl;
import org.ggp.base.util.gdl.grammar.GdlConstant;
import org.ggp.base.util.gdl.grammar.GdlSentence;
import org.ggp.base.util.gdl.grammar.GdlTerm;
import org.ggp.base.util.match.Match;
import org.ggp.base.util.propnet.architecture.Component;
import org.ggp.base.util.propnet.architecture.PropNet;
import org.ggp.base.util.propnet.architecture.components.Proposition;
import org.ggp.base.util.statemachine.MachineState;
import org.ggp.base.util.statemachine.Move;
import org.ggp.base.util.statemachine.StateMachine;
import org.ggp.base.util.statemachine.exceptions.MoveDefinitionException;
import org.ggp.base.util.statemachine.exceptions.TransitionDefinitionException;
import org.ggp.base.util.statemachine.implementation.propnet.FuzzyPropNetMachine;
import org.ggp.base.util.statemachine.implementation.prover.ProverStateMachine;


public class TestingConsole {
	int printlimit = 700;
	int printcount = 0;
	int diaglevel = 10;
	//Change this:
	//String dir = "C:/github/ggp-base/games/gamemaster/";
	String dir = "/Users/snarles/github/ggp-base/games/gamemaster/";
	//String gamef = "alquerque.kif";
	//String gamef = "connectfour.kif";
	//String gamef = "pentago.kif";
	//String gamef = "skirmish.kif";
	String gamef = "tictactoe.kif";

	String gameFile = dir.concat(gamef);
	StateMachine psm = new ProverStateMachine();
	StateMachine lsm = new FuzzyPropNetMachine();
	MachineState currentState;
	int currentTurn = -1;
	boolean messageEachTurn = true;
	PropNet pn = null;
	List<Gdl> theRules = null;
	Game theGame = null;

	public static void main(String[] args)  {
		TestingConsole tc = new TestingConsole();
		//tc.run0();
		tc.run();
	}

	public void run() {
		String content = "";
		try {
			Scanner scanman = new Scanner(new File(gameFile));
			content = scanman.useDelimiter("\\Z").next();
			scanman.close();
		}
		catch (IOException e) {
		}
		//System.out.println(content);
		String content2 = Game.preprocessRulesheet(content);
		//System.out.println(content2);
		theGame = Game.createEphemeralGame(content2);
		theRules = theGame.getRules();
		psm.initialize(theRules);
		psm.setSeed(10);

		lsm.initialize(theRules);

		pn = ((FuzzyPropNetMachine) lsm).getPropNet();
		//printComponents();paused(1);
		//		PropNetAnnotatedFlattener af = new PropNetAnnotatedFlattener(theRules);
//		long start = System.currentTimeMillis();
//		List<GdlRule> flatDescription = af.flatten();
//		long elapsed = System.currentTimeMillis()-start;
//		printd("Time to flatten",String.valueOf(elapsed),3);

		/**
		paused(1);
		for (GdlRule g : flatDescription) {
			System.out.println(g.toString());
		}
		**/
		//paused(1);
//		OptimizingPropNetFactory op = new OptimizingPropNetFactory();
//		try {
//			pn = op.create(theRules,false);
//		} catch (InterruptedException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}
		//paused(1);

		randomAdvance();

		//((FuzzyPropNetMachine) lsm).setState(currentState);

		paused(1);
		randomAdvance();
		randomAdvance();
		randomAdvance();
		randomAdvance();
		randomAdvance();
		paused(1);
		run2();

//		List<List<Move>> moves = null;
//		try {
//			randomAdvance();
//			randomAdvance();
//			moves = getMoves();
//		}
//		catch (Exception e) {
//			printd("ERROR","",0);
//		}
//
		//Component c = pn.getInitProposition();
		//printd("ip:",c.toString3(),3);

	}

	public void run2()  {
		Match theMatch = new Match("test",-1,10,10,theGame);
		Match theMatch2 = new Match("test",-1,10,10,theGame);
		SampleLegalGamer sean = new SampleLegalGamer();
		//sean.setName("sean");
		FuzzyPropNetGamer charles = new FuzzyPropNetGamer();
		//charles.setName("charles");

		GdlConstant r = psm.getRoles().get(0).getName();
		GdlConstant r2 = psm.getRoles().get(1).getName();
		sean.setMatch(theMatch);
		sean.setRoleName(r);

		charles.setMatch(theMatch2);
		charles.setRoleName(r2);
		psm.initialize(theRules);
		currentState = psm.getInitialState();
		printd("Official initial state: ",currentState.toString(),2);

		printd("Sean is",r.toString(),3);
		printd("Charles is",r2.toString(),3);


		long receptionTime = System.currentTimeMillis();
		try {
			sean.metaGame(receptionTime + 10000);
			sean.setSeed(0);
			charles.metaGame(System.currentTimeMillis() + 10000);
			charles.setSeed(0);
		}
		catch (MetaGamingException e) {
			System.out.println("METAGMING ERROR");
		}
		long start;
		long timelimit = 10000;
		boolean gameOver = false;
		boolean pauser = true;
		printd("Start game","",3);
		paused(1);
		while(!gameOver) {
			start = System.currentTimeMillis();
			GdlTerm move1 = null;
			try {
				move1 = sean.selectMove(start + timelimit);
			} catch (MoveSelectionException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			printd("sean sel:",String.valueOf(System.currentTimeMillis() - start),3);
			start = System.currentTimeMillis();
			GdlTerm move2 = null;
			try {
				move2 = charles.selectMove(start+ timelimit);
			} catch (MoveSelectionException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			printd("ch sel m:",String.valueOf(System.currentTimeMillis() - start),3);

			//Move move1 = psm.getMoveFromTerm(sean.selectMove(System.currentTimeMillis() + 10000));
			//Move move2 = psm.getMoveFromTerm(charles.selectMove(System.currentTimeMillis() + 10000));

			List<GdlTerm> moves = new ArrayList<GdlTerm>();
			moves.add(move1);
			moves.add(move2);
			theMatch.appendMoves(moves);
			theMatch2.appendMoves(moves);

			List<Move> moves2 = new ArrayList<Move>();
			for (GdlTerm sentence : moves)
			{
				moves2.add(psm.getMoveFromTerm(sentence));
			}
			try {
				String s = "";
				s = s.concat("Moves: ");
				s = s.concat(moves2.get(0).toString());
				s=s.concat(" , ");
				s = s.concat(moves2.get(1).toString());
				printd(s,"",2);
				currentState = psm.getNextState(currentState, moves2);
			} catch (TransitionDefinitionException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			printd("Official state: ",currentState.toString(),2);
			gameOver = psm.isTerminal(currentState);
			if (gameOver) {printd("GAME OVER","",1);}
			if (pauser) paused(1);
		}
		//System.out.println(move.toString());
	}




	public void printComponents() {
		ArrayList<Component> components = pn.getComponentsS();
		for (Component c : components) {
			printd("Component:",c.toString3().concat("++").concat(String.valueOf(c.getOutcount(0))),2);
		}
	}

	public void printPropositions() {
		Set<Proposition> propositions = pn.getPropositions();
		int count = 0;
		for (Proposition p : propositions) {
			count++;
			printd("Proposition:",p.toString2(),2);
			if (count > 1000) {
				count = 0;
				paused(2);
			}
		}
	}



	// chooses a legal joint move at random and advances the game
	public void randomAdvance()  {
		try {
			MachineState state1;
			MachineState state2;
			long start; long elapsed; String message;
			List<Move> selectedMove;
			List<List<Move>> legals;
			if (currentTurn == -1) {
				currentTurn++;
				start = System.currentTimeMillis();
				state1= psm.getInitialState();
				elapsed = System.currentTimeMillis()-start;
				message = "PSM Time to get initial state: ";
				message = message.concat(String.valueOf(elapsed));
				if (messageEachTurn) {
					System.out.println(message);
				}


				start = System.currentTimeMillis();
				state2 = lsm.getInitialState();
				elapsed = System.currentTimeMillis()-start;
				message = "LSM Time to get initial state: ";
				message = message.concat(String.valueOf(elapsed));
				if (messageEachTurn) {
					System.out.println(message);
				}

				printd("PSM State:",state1.toString(),3);
				printd("LSM State:",state2.toString(),3);
				currentState = state1;
			}
			else {
				currentTurn++;
				if (messageEachTurn) {
					System.out.println("Turn: ".concat(String.valueOf(currentTurn)));
				}
				legals = getMoves();
				selectedMove = legals.get(new Random().nextInt(legals.size()));
				for (Move m : selectedMove) {
					printd("Move:",m.toString(),3);
				}
				System.out.println(selectedMove.toString());
				for (Move m : selectedMove) {
					printd("Move:",m.toString(),3);
				}
				//((FuzzyPropNetMachine) lsm).printCaches();
				state1 = getNextState(selectedMove,psm,"PSM");
				state2 = getNextState(selectedMove,lsm,"LSM");
				printd("State:",currentState.toString(),3);
				Set<GdlSentence> contents1 = state1.getContents();
				Set<GdlSentence> contents2 = state2.getContents();
				if (contents1.equals(contents2)) {
					printd("!!MATCH!!","",3);
				}
				else {
					printd("MISMATCH!!!","",0);
				}
				currentState = state1;
//				int[] jj = ((FuzzyPropNetMachine) lsm).currentGoals();
//				printd("Goals",String.valueOf(jj[0]).concat(" ").concat(String.valueOf(jj[1])),3);
			}
		}
		catch (Exception e) {
			e.printStackTrace();
		}
	}

	public List<List<Move>> getMoves() throws MoveDefinitionException {
		long start = System.currentTimeMillis();
		List<List<Move>> moves = psm.getLegalJointMoves(currentState);
		long elapsed = System.currentTimeMillis()-start;
		String message = "PSM Time to get moves: ";
		message = message.concat(String.valueOf(elapsed));

		start = System.currentTimeMillis();
		List<List<Move>> moves2 = lsm.getLegalJointMoves(currentState);
		elapsed = System.currentTimeMillis()-start;
		String message2 = "LSM Time to get moves: ";
		message2 = message2.concat(String.valueOf(elapsed));


		if (messageEachTurn) {
			System.out.println(message);
			for (List<Move> jointmoves : moves) {
				printd("PSM Mv:",jointmoves.get(0).toString().concat(jointmoves.get(1).toString()),3);
			}
			System.out.println(message2);
			for (List<Move> jointmoves : moves2) {
				printd("LSM Mv:",jointmoves.get(0).toString().concat(jointmoves.get(1).toString()),3);
			}
		}
		return moves;
	}
	public void printd(String s, String u, int i) {
		if (i < diaglevel) {
			printcount++;
			if (printcount > printlimit) {
				paused(1);
				printcount = 0;
			}
			System.out.println(s.concat(u));
		}
	}
	public MachineState getNextState(List<Move> move, StateMachine sm, String sn) throws TransitionDefinitionException {
		long start = System.currentTimeMillis();
		MachineState newState = sm.getNextState(currentState,move);
		long elapsed = System.currentTimeMillis()-start;
		String message = sn.concat(":Time for sm to get next state: ");
		message = message.concat(String.valueOf(elapsed));
		if (messageEachTurn) {
			System.out.println(message);
			printd(sn.concat(" STATE:"), newState.toString(),3);
		}
		return newState;
	}

	public void paused(int i) {
		if (i < diaglevel) {
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

}
