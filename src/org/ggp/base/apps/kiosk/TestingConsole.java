package org.ggp.base.apps.kiosk;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Scanner;
import java.util.Set;

import org.ggp.base.player.gamer.exception.MetaGamingException;
import org.ggp.base.player.gamer.exception.MoveSelectionException;
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
import org.ggp.base.util.propnet.factory.OptimizingPropNetFactory;
import org.ggp.base.util.statemachine.MachineState;
import org.ggp.base.util.statemachine.Move;
import org.ggp.base.util.statemachine.Role;
import org.ggp.base.util.statemachine.StateMachine;
import org.ggp.base.util.statemachine.exceptions.MoveDefinitionException;
import org.ggp.base.util.statemachine.exceptions.TransitionDefinitionException;
import org.ggp.base.util.statemachine.implementation.prover.ProverStateMachine;


public class TestingConsole {
	int printlimit = 700;
	int printcount = 0;
	int diaglevel = 10;
	//Change this:
	String dir = "C:/github/ggp-base/games/gamemaster/";
	//String dir = "/Users/snarles/github/ggp-base/games/gamemaster/";
	//String gamef = "alquerque.kif";
	//String gamef = "connectfour.kif";
	//String gamef = "pentago.kif";
	//String gamef = "skirmish.kif";
	String gamef = "tictactoe.kif";

	String gameFile = dir.concat(gamef);
	StateMachine psm = new ProverStateMachine();
	MachineState currentState;
	int currentTurn = -1;
	boolean messageEachTurn = true;
	PropNet pn = null;

	public static void main(String[] args) throws IOException, MoveDefinitionException, FileNotFoundException, TransitionDefinitionException, MetaGamingException, MoveSelectionException {
		TestingConsole tc = new TestingConsole();
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
		Game theGame = Game.createEphemeralGame(content2);
		List<Gdl> theRules = theGame.getRules();
		psm.initialize(theRules);


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
		OptimizingPropNetFactory op = new OptimizingPropNetFactory();
		try {
			pn = op.create(theRules,false);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		long start = System.currentTimeMillis();
		pn.topoSort();
		long elapsed = System.currentTimeMillis()-start;
		printd("Time to sort",String.valueOf(elapsed),3);
		pn.labelComponents();
		//paused(1);
		//printComponents();
		//paused(1);
		//printPropositions();
		List<List<Move>> moves = null;
		try {
			randomAdvance();
			randomAdvance();
			moves = getMoves();
		}
		catch (Exception e) {
			printd("ERROR","",0);
		}
		Set<GdlSentence> contents = currentState.getContents();
		for (GdlSentence g : contents) {
			printd("g:",g.toString(),3);
			printd("p:",pn.getProposition(g).toString3(),3);
		}
		List<Move> l1 = moves.get(0);
		for (Move m : l1) {
			printd("m:",m.toString(),3);
			GdlTerm g = m.getContents();
		}
		//paused(1);
		//printComponents();
		Map<GdlSentence,Proposition> ips = pn.getInputPropositions();
		Set<GdlSentence> ipsk = ips.keySet();
		Set<GdlTerm> rolesT = new HashSet();
		for (GdlSentence g : ipsk) {
			printd("key:",g.toString(),3);
			List<GdlTerm> b = g.getBody();
			rolesT.add(b.get(0));
			for (GdlTerm g2 : b) {
				printd("val:",g2.toString(),4);
				for (Move m : l1) {
					GdlTerm g3 = m.getContents();
					if (g3.equals(g2)) {
						printd(" match:",m.toString(),3);
					}
				}
			}
		}
		List<Role> roles = psm.getRoles();
		for (GdlTerm g : rolesT) {
			printd("role:",g.toString(),3);
			for (Role r : roles) {
				GdlConstant t = r.getName();
				if (t.equals(g)) {
					printd("match:",r.toString(),3);
				}
			}
		}

	}

	public void run3() throws IOException, MoveDefinitionException, FileNotFoundException, TransitionDefinitionException, MetaGamingException, MoveSelectionException {
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
		SampleMonteCarloGamer sean = new SampleMonteCarloGamer();
		//sean.setName("sean");
		SampleMonteCarloGamer charles = new SampleMonteCarloGamer();
		//charles.setName("charles");

		GdlConstant r = psm.getRoles().get(0).getName();
		GdlConstant r2 = psm.getRoles().get(1).getName();
		sean.setMatch(theMatch);
		sean.setRoleName(r);

		charles.setMatch(theMatch);
		charles.setRoleName(r2);

		long receptionTime = System.currentTimeMillis();
		sean.metaGame(receptionTime + 10000);
		sean.setSeed(0);
		charles.metaGame(System.currentTimeMillis() + 10000);
		charles.setSeed(0);

		long start;
		long timelimit = 10000;
		boolean gameOver = false;
		boolean pauser = false;
		while(!gameOver) {
			start = System.currentTimeMillis();
			GdlTerm move1 = sean.selectMove(start + timelimit);
			System.out.println(String.valueOf(System.currentTimeMillis() - start));
			if (pauser) {
				paused(1);
			}
			start = System.currentTimeMillis();
			GdlTerm move2 = charles.selectMove(start+ timelimit);
			System.out.println(String.valueOf(System.currentTimeMillis() - start));
			MachineState state = sean.getCurrentState();
			System.out.println(state.toString());
			if (pauser) {
				paused(1);
			}
			gameOver = psm.isTerminal(state);
			//Move move1 = psm.getMoveFromTerm(sean.selectMove(System.currentTimeMillis() + 10000));
			//Move move2 = psm.getMoveFromTerm(charles.selectMove(System.currentTimeMillis() + 10000));

			List<GdlTerm> moves = new ArrayList<GdlTerm>();
			moves.add(move1);
			moves.add(move2);
			theMatch.appendMoves(moves);
		}
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


	public void printComponents() {
		ArrayList<Component> components = pn.getComponentsS();
		for (Component c : components) {
			printd("Component:",c.toString3(),2);
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
