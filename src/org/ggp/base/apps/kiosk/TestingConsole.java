package org.ggp.base.apps.kiosk;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
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
	//String gamef = "pentago.kif";
	String gamef = "skirmish.kif";


	String gameFile = dir.concat(gamef);
	StateMachine psm = new ProverStateMachine();
	Prover prover = ((ProverStateMachine) psm).getProver();
	KnowledgeBase knowledgeBase;
	MachineState currentState;
	int currentTurn = -1;
	boolean messageEachTurn = true;
	boolean messageEachStep = true;

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

	public List<List<Move>> getMovesDetailed() throws MoveDefinitionException {
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

	public List<Move> getLegalMovesDetailed(MachineState state, Role role) throws MoveDefinitionException
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

	private Set<GdlSentence> ask(GdlSentence query, Set<GdlSentence> context, boolean askOne)
	{
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

		for (GdlLiteral goal : goals) {
			System.out.println("GOAL:".concat(goal.toString()));
		}

		ask(goals, ct, new Substitution(), new ProverCache(), new VariableRenamer(), askOne, answers, alreadyAsking);

		Set<GdlSentence> results = new HashSet<GdlSentence>();
		for (Substitution theta : answers)
		{
			results.add(Substituter.substitute(query, theta));
		}

		return results;
	}

	private void ask(LinkedList<GdlLiteral> goals, KnowledgeBase context, Substitution theta, ProverCache cache, VariableRenamer renamer, boolean askOne, Set<Substitution> results, Set<GdlSentence> alreadyAsking)
	{
		String message = "";
		if (goals.size() > 0) {
			message = message.concat(" Goals size:");
			message = message.concat(String.valueOf(goals.size()));
		}
		if (goals.size() == 0)
		{
			results.add(theta);
		}
		else
		{
			GdlLiteral literal = goals.removeFirst();
			GdlLiteral qPrime = Substituter.substitute(literal, theta);
			message = message.concat(" qPrime: ");
			message = message.concat(qPrime.toString());
			if (messageEachStep) {
				if (goals.size() > 1) {
					System.out.println(message);
					for (GdlLiteral goal : goals) {
						System.out.println("GOAL:".concat(goal.toString()));
					}
				}
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
	}

	public Set<GdlSentence> askAll(GdlSentence query, Set<GdlSentence> context)
	{
		return ask(query, context, false);
	}

	private void askDistinct(GdlDistinct distinct, LinkedList<GdlLiteral> goals, KnowledgeBase context, Substitution theta, ProverCache cache, VariableRenamer renamer, boolean askOne, Set<Substitution> results, Set<GdlSentence> alreadyAsking)
	{
		if (!distinct.getArg1().equals(distinct.getArg2()))
		{
			ask(goals, context, theta, cache, renamer, askOne, results, alreadyAsking);
		}
	}

	private void askNot(GdlNot not, LinkedList<GdlLiteral> goals, KnowledgeBase context, Substitution theta, ProverCache cache, VariableRenamer renamer, boolean askOne, Set<Substitution> results, Set<GdlSentence> alreadyAsking)
	{
		LinkedList<GdlLiteral> notGoals = new LinkedList<GdlLiteral>();
		notGoals.add(not.getBody());

		Set<Substitution> notResults = new HashSet<Substitution>();
		ask(notGoals, context, theta, cache, renamer, true, notResults, alreadyAsking);

		if (notResults.size() == 0)
		{
			ask(goals, context, theta, cache, renamer, askOne, results, alreadyAsking);
		}
	}

	public GdlSentence askOne(GdlSentence query, Set<GdlSentence> context)
	{
		Set<GdlSentence> results = ask(query, context, true);
		return (results.size() > 0) ? results.iterator().next() : null;
	}

	private void askOr(GdlOr or, LinkedList<GdlLiteral> goals, KnowledgeBase context, Substitution theta, ProverCache cache, VariableRenamer renamer, boolean askOne, Set<Substitution> results, Set<GdlSentence> alreadyAsking)
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

	private void askSentence(GdlSentence sentence, LinkedList<GdlLiteral> goals, KnowledgeBase context, Substitution theta, ProverCache cache, VariableRenamer renamer, boolean askOne, Set<Substitution> results, Set<GdlSentence> alreadyAsking)
	{
		if (!cache.contains(sentence))
		{
			//Prevent infinite loops on certain recursive queries.
			if(alreadyAsking.contains(sentence)) {
				return;
			}
			alreadyAsking.add(sentence);
			List<GdlRule> candidates = new ArrayList<GdlRule>();
			candidates.addAll(knowledgeBase.fetch(sentence));
			candidates.addAll(context.fetch(sentence));

			Set<Substitution> sentenceResults = new HashSet<Substitution>();
			for (GdlRule rule : candidates)
			{
				GdlRule r = renamer.rename(rule);
				Substitution thetaPrime = Unifier.unify(r.getHead(), sentence);

				if (thetaPrime != null)
				{
					LinkedList<GdlLiteral> sentenceGoals = new LinkedList<GdlLiteral>();
					for (int i = 0; i < r.arity(); i++)
					{
						sentenceGoals.add(r.get(i));
					}

					ask(sentenceGoals, context, theta.compose(thetaPrime), cache, renamer, false, sentenceResults, alreadyAsking);
				}
			}

			cache.put(sentence, sentenceResults);
			alreadyAsking.remove(sentence);
		}

		for (Substitution thetaPrime : cache.get(sentence))
		{
			ask(goals, context, theta.compose(thetaPrime), cache, renamer, askOne, results, alreadyAsking);
			if (askOne && (results.size() > 0))
			{
				break;
			}
		}
	}

	public boolean prove(GdlSentence query, Set<GdlSentence> context)
	{
		return askOne(query, context) != null;
	}
}
