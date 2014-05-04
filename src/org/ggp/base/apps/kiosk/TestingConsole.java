package org.ggp.base.apps.kiosk;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;
import java.util.Scanner;
import java.util.Set;

import org.ggp.base.util.game.Game;
import org.ggp.base.util.gdl.grammar.Gdl;
import org.ggp.base.util.gdl.grammar.GdlDistinct;
import org.ggp.base.util.gdl.grammar.GdlLiteral;
import org.ggp.base.util.gdl.grammar.GdlNot;
import org.ggp.base.util.gdl.grammar.GdlOr;
import org.ggp.base.util.gdl.grammar.GdlRule;
import org.ggp.base.util.gdl.grammar.GdlSentence;
import org.ggp.base.util.prover.Prover;
import org.ggp.base.util.prover.aima.AimaProver;
import org.ggp.base.util.prover.aima.cache.ProverCache;
import org.ggp.base.util.prover.aima.knowledge.KnowledgeBase;
import org.ggp.base.util.prover.aima.renamer.VariableRenamer;
import org.ggp.base.util.prover.aima.substituter.Substituter;
import org.ggp.base.util.prover.aima.substitution.Substitution;
import org.ggp.base.util.prover.aima.unifier.Unifier;
import org.ggp.base.util.statemachine.MachineState;
import org.ggp.base.util.statemachine.Move;
import org.ggp.base.util.statemachine.Role;
import org.ggp.base.util.statemachine.StateMachine;
import org.ggp.base.util.statemachine.exceptions.MoveDefinitionException;
import org.ggp.base.util.statemachine.exceptions.TransitionDefinitionException;
import org.ggp.base.util.statemachine.implementation.prover.ProverStateMachine;
import org.ggp.base.util.statemachine.implementation.prover.query.ProverQueryBuilder;
import org.ggp.base.util.statemachine.implementation.prover.result.ProverResultParser;
// Aima prover


public class TestingConsole {
	//Change this:
	//String dir = "C:/github/ggp-base/games/gamemaster/";
	String dir ="/Users/snarles/github/ggp-base/games/gamemaster/";

	//String gamef = "alquerque.kif";
	//String gamef = "connectfour.kif";
	String gamef = "chinesecheckers.kif";
	//String gamef = "hex.kif";
	//String gamef = "pentago.kif";
	//String gamef = "skirmish.kif";


	String gameFile = dir.concat(gamef);
	StateMachine psm = new ProverStateMachine();
	Prover prover = ((ProverStateMachine) psm).getProver();
	KnowledgeBase knowledgeBase;
	MachineState currentState;
	int currentTurn = -1;
	boolean messageEachTurn = true;
	boolean messageEachStep = true;
	int maxprint = 1000;
	int printcounter = 0;
	int recursionDepth = 0;
	String[] recursionPadding = {"R0:"," R1:","  R2:","   R3:","    R4:","     R5:","      R6:","       R7:","        R8:","         R9:","          R10:"};
	int printdepth = 8;

	int resultType = 0;

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



		/**BufferedReader gameRead = new BufferedReader(new FileReader(gameFile));
		String text = gameRead.read();
		System.out.println(text);**/
		Scanner scanman = new Scanner(new File(gameFile));
		String content = scanman.useDelimiter("\\Z").next();
		scanman.close();
		//System.out.println(content);
		String content2 = Game.preprocessRulesheet(content);
		//System.out.println(content2);
		Game theGame = Game.createEphemeralGame(content2);
		List<Gdl> theRules = theGame.getRules();
		//for (Gdl g : theRules) {
		//	System.out.println(g.toString());
		//}

		start = System.currentTimeMillis();
		//StateMachine psm = new ProverStateMachine();
		//psm = new CachedStateMachine(psm);
		psm.initialize(theRules);
		prover = ((ProverStateMachine) psm).getProver();
		knowledgeBase = ((AimaProver) prover).getKnowledgeBase();

		elapsed = System.currentTimeMillis()-start;
		message = "Time to initialize: ";
		message = message.concat(String.valueOf(elapsed));
		System.out.println(message);


		// Running time test
		/**
		int nits = 1;
		//messageEachTurn= false; nits=10; //Uncomment this to do multiple runs
		for (int j =1; j< 1+nits; j++) {
			currentTurn = -1;
			start = System.currentTimeMillis();
			for (int i = 1; i < 101; i++) {
				randomAdvance();
			}
			elapsed = System.currentTimeMillis()-start;

			message = "Total time: ";
			message = message.concat(String.valueOf(elapsed));
			System.out.println(message);
		}
		**/

		// Play 5 turns so that the GdlPool is loaded
		messageEachTurn = false;
		for (int i = 1; i < 5; i++) {
			randomAdvance();
		}
		messageEachTurn = true;

		legals = getMovesDetailed();


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

	public List<List<Move>> getMovesDetailed() throws MoveDefinitionException, IOException {
		MachineState state = currentState;
        List<List<Move>> legals = new ArrayList<List<Move>>();
        for (Role role : psm.getRoles()) {
            legals.add(getLegalMovesDetailed(state, role));
        }

        List<List<Move>> crossProduct = new ArrayList<List<Move>>();
        crossProductLegalMoves(legals, crossProduct, new LinkedList<Move>());

		return crossProduct;
	}

    public void crossProductLegalMoves(List<List<Move>> legals, List<List<Move>> crossProduct, LinkedList<Move> partial)
    {
        if (partial.size() == legals.size()) {
            crossProduct.add(new ArrayList<Move>(partial));
        } else {
            for (Move move : legals.get(partial.size())) {
                partial.addLast(move);
                crossProductLegalMoves(legals, crossProduct, partial);
                partial.removeLast();
            }
        }
    }

	public List<Move> getLegalMovesDetailed(MachineState state, Role role) throws MoveDefinitionException, IOException
	{
		long start = System.currentTimeMillis();
		Set<GdlSentence> results = askAll(ProverQueryBuilder.getLegalQuery(role), ProverQueryBuilder.getContext(state));
		long elapsed = System.currentTimeMillis()-start;
		String message = "Time to ask all: ";
		message = message.concat(String.valueOf(elapsed));
		if (messageEachTurn) {
			System.out.println(message);
		}

		if (results.size() == 0)
		{
			throw new MoveDefinitionException(state, role);
		}

		return new ProverResultParser().toMoves(results);
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


	//Methods pasted from AimaProver

	private Set<GdlSentence> ask(GdlSentence query, Set<GdlSentence> context, boolean askOne) throws IOException
	{
		//System.out.println("****FIRST ASK");
		LinkedList<GdlLiteral> goals = new LinkedList<GdlLiteral>();
		goals.add(query);

		Set<Substitution> answers = new HashSet<Substitution>();
		Set<GdlSentence> alreadyAsking = new HashSet<GdlSentence>();

		long start = System.currentTimeMillis();
		KnowledgeBase ct = new KnowledgeBase(context);
		long elapsed = System.currentTimeMillis()-start;
		String message = "Time to get context: ";
		message = message.concat(String.valueOf(elapsed));
		if (messageEachTurn) {
			System.out.println(message);
		}
		System.out.println(currentState.toString());
		/**
		Map<GdlConstant, List<GdlRule>> ctcontents = ct.getContents();

		for (GdlConstant gc : ctcontents.keySet()) {
			System.out.println("KEY: ".concat(gc.toString()));
			List<GdlRule> vals = ctcontents.get(gc);
			for (GdlRule v : vals) {
				System.out.println("  VALUE: ".concat(v.toString()));
			}
		}
		**/

		for (GdlLiteral goal : goals) {
			System.out.println("GOAL:".concat(goal.toString()));
		}

		ask(goals, ct, new Substitution(), new ProverCache(), new VariableRenamer(), askOne, answers, alreadyAsking);

		start = System.currentTimeMillis();
		Set<GdlSentence> results = new HashSet<GdlSentence>();
		for (Substitution theta : answers)
		{
			results.add(Substituter.substitute(query, theta));
		}
		elapsed = System.currentTimeMillis()-start;
		message = "Time to get results: ";
		message = message.concat(String.valueOf(elapsed));
		if (messageEachTurn) {
			System.out.println(message);
		}
		return results;
	}

	private void ask(LinkedList<GdlLiteral> goals, KnowledgeBase context, Substitution theta, ProverCache cache, VariableRenamer renamer, boolean askOne, Set<Substitution> results, Set<GdlSentence> alreadyAsking) throws IOException
	{
		recursionDepth++;
		String message = "";
		if (goals.size() > 0) {
			message = message.concat("Goals size:");
			message = message.concat(String.valueOf(goals.size()));
		}
		if (goals.size() == 0)
		{
			//printd("No goals");
			results.add(theta);
			String rmessage = "Result: ";
			if (resultType ==1) {
				rmessage = "NotResult:";
			}
			if (resultType ==2) {
				rmessage = "SntResult:";
			}
			printd(rmessage.concat(theta.toString()).concat(" no. ").concat(String.valueOf(results.size())));
		}
		else
		{
			GdlLiteral literal = goals.removeFirst();
			GdlLiteral qPrime = Substituter.substitute(literal, theta);

			if (qPrime instanceof GdlDistinct)
			{
				message = message.concat("[Distinct] ");
			}
			else if (qPrime instanceof GdlNot)
			{
				message = message.concat("[Not] ");
			}
			else if (qPrime instanceof GdlOr)
			{
				message = message.concat("[Or] ");
			}
			else {
				message = message.concat("[Sentence] ");
			}
			message = message.concat(" literal: ");
			message = message.concat(literal.toString());

			message = message.concat(" qPrime: ");
			message = message.concat(qPrime.toString());
			printd(message);
			for (GdlLiteral goal : goals) {
				printd("GOAL:".concat(goal.toString()));
			}
			if (qPrime instanceof GdlDistinct)
			{
				GdlDistinct distinct = (GdlDistinct) qPrime;
				askDistinct(distinct, goals, context, theta, cache, renamer, askOne, results, alreadyAsking);
			}
			else if (qPrime instanceof GdlNot)
			{
				GdlNot not = (GdlNot) qPrime;
				askNot(not, goals, context, theta, cache, renamer, askOne, results, alreadyAsking);
			}
			else if (qPrime instanceof GdlOr)
			{
				GdlOr or = (GdlOr) qPrime;
				askOr(or, goals, context, theta, cache, renamer, askOne, results, alreadyAsking);
			}
			else
			{
				GdlSentence sentence = (GdlSentence) qPrime;
				askSentence(sentence, goals, context, theta, cache, renamer, askOne, results, alreadyAsking);
			}

			goals.addFirst(literal);
		}

		recursionDepth--;
	}

	public Set<GdlSentence> askAll(GdlSentence query, Set<GdlSentence> context) throws IOException
	{
		return ask(query, context, false);
	}

	private void askDistinct(GdlDistinct distinct, LinkedList<GdlLiteral> goals, KnowledgeBase context, Substitution theta, ProverCache cache, VariableRenamer renamer, boolean askOne, Set<Substitution> results, Set<GdlSentence> alreadyAsking) throws IOException
	{
		if (!distinct.getArg1().equals(distinct.getArg2()))
		{
			ask(goals, context, theta, cache, renamer, askOne, results, alreadyAsking);
		}
	}

	private void askNot(GdlNot not, LinkedList<GdlLiteral> goals, KnowledgeBase context, Substitution theta, ProverCache cache, VariableRenamer renamer, boolean askOne, Set<Substitution> results, Set<GdlSentence> alreadyAsking) throws IOException
	{
		LinkedList<GdlLiteral> notGoals = new LinkedList<GdlLiteral>();
		notGoals.add(not.getBody());

		Set<Substitution> notResults = new HashSet<Substitution>();
		int oldresultType = resultType;
		resultType=1;
		ask(notGoals, context, theta, cache, renamer, true, notResults, alreadyAsking);
		resultType=oldresultType;

		if (notResults.size() == 0)
		{
			ask(goals, context, theta, cache, renamer, askOne, results, alreadyAsking);
		}
	}

	public GdlSentence askOne(GdlSentence query, Set<GdlSentence> context) throws IOException
	{
		System.out.println("********askone");
		Set<GdlSentence> results = ask(query, context, true);
		return (results.size() > 0) ? results.iterator().next() : null;
	}

	private void askOr(GdlOr or, LinkedList<GdlLiteral> goals, KnowledgeBase context, Substitution theta, ProverCache cache, VariableRenamer renamer, boolean askOne, Set<Substitution> results, Set<GdlSentence> alreadyAsking) throws IOException
	{
		for (int i = 0; i < or.arity(); i++)
		{
			goals.addFirst(or.get(i));
			ask(goals, context, theta, cache, renamer, askOne, results, alreadyAsking);
			goals.removeFirst();

			if (askOne && (results.size() > 0))
			{
				break;
			}
		}
	}

	public void printd(String s) throws IOException {
		if (printcounter == maxprint) {
			System.out.println("PRINT LIMIT REACHED");
			BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
			System.out.println("Press enter");
			String temp = in.readLine();
			printcounter = 0;
		}
		if (printcounter < maxprint && messageEachStep  && recursionDepth < printdepth) {
			printcounter++;
			System.out.println(recursionPadding[recursionDepth].concat(s));
		}
	}

	private void askSentence(GdlSentence sentence, LinkedList<GdlLiteral> goals, KnowledgeBase context, Substitution theta, ProverCache cache, VariableRenamer renamer, boolean askOne, Set<Substitution> results, Set<GdlSentence> alreadyAsking) throws IOException
	{
		if (cache.contains(sentence)) {
			printd("Cache Contains Sentence");
		}
		if (!cache.contains(sentence))
		{
			//Prevent infinite loops on certain recursive queries.
			if(alreadyAsking.contains(sentence)) {
				return;
			}
			alreadyAsking.add(sentence);
			List<GdlRule> candidates = new ArrayList<GdlRule>();
			List<GdlRule> candidates2 = new ArrayList<GdlRule>();
			candidates.addAll(knowledgeBase.fetch(sentence));
			candidates2.addAll(context.fetch(sentence));

			Set<Substitution> sentenceResults = new HashSet<Substitution>();
			long start = System.currentTimeMillis();
			printd("Theta:".concat(theta.toString()));
			printd("Candidates:");

			int grulecount = 0;
			for (GdlRule rule : candidates)
			{
				GdlRule r = renamer.rename(rule);
				Substitution thetaPrime = Unifier.unify(r.getHead(), sentence);


				if (thetaPrime != null)
				{
					printd("G.rule: ".concat(rule.toString()));
					LinkedList<GdlLiteral> sentenceGoals = new LinkedList<GdlLiteral>();
					for (int i = 0; i < r.arity(); i++)
					{
						sentenceGoals.add(r.get(i));
					}
					int oldresultType = resultType;
					resultType = 2;
					ask(sentenceGoals, context, theta.compose(thetaPrime), cache, renamer, false, sentenceResults, alreadyAsking);
					resultType = oldresultType;
				}
				else {
					grulecount++;
				}
			}
			if (grulecount > 0) {
				printd("Null game rules: ".concat(String.valueOf(grulecount)));
			}
			int srulecount = 0;
			for (GdlRule rule : candidates2)
			{
				GdlRule r = renamer.rename(rule);
				Substitution thetaPrime = Unifier.unify(r.getHead(), sentence);

				if (thetaPrime != null)
				{
					printd("State rule: ".concat(rule.toString()));
					LinkedList<GdlLiteral> sentenceGoals = new LinkedList<GdlLiteral>();
					for (int i = 0; i < r.arity(); i++)
					{
						sentenceGoals.add(r.get(i));
					}
					int oldresultType = resultType;
					resultType = 2;
					ask(sentenceGoals, context, theta.compose(thetaPrime), cache, renamer, false, sentenceResults, alreadyAsking);
					resultType = oldresultType;
				}
				else {
					srulecount++;
				}
			}
			if (srulecount > 0) {
				printd("Null state rules: ".concat(String.valueOf(srulecount)));
			}


			long elapsed = System.currentTimeMillis()-start;
			printd("Candidates call: ".concat(String.valueOf(elapsed)));

			cache.put(sentence, sentenceResults);
			alreadyAsking.remove(sentence);
		}
		List<Substitution> cgs = cache.get(sentence);
		if (cgs.size() > 0) {
			printd("Sentence theta--".concat(sentence.toString()).concat(" size ").concat(String.valueOf(cgs.size())));
		}
		long start = System.currentTimeMillis();
		for (Substitution thetaPrime : cgs)
		{
			ask(goals, context, theta.compose(thetaPrime), cache, renamer, askOne, results, alreadyAsking);
			if (askOne && (results.size() > 0))
			{
				break;
			}
		}
		long elapsed = System.currentTimeMillis()-start;
		if (cgs.size() > 0) {
			printd("Theta call: ".concat(String.valueOf(elapsed)));
		}
	}

	public boolean prove(GdlSentence query, Set<GdlSentence> context) throws IOException
	{
		return askOne(query, context) != null;
	}
}
