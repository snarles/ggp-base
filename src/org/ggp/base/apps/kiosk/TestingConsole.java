package org.ggp.base.apps.kiosk;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Scanner;

import org.ggp.base.util.game.Game;
import org.ggp.base.util.gdl.grammar.Gdl;


public class TestingConsole {

	public static void main(String[] args) throws IOException {
		/**BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
		System.out.println("Enter game file");
		String gameFile = in.readLine();**/
		String gameFile = "/Users/snarles/github/ggp-base/games/games/ticTacToe/ticTacToe.kif";
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
		for (Gdl g : theRules) {
			System.out.println(g.toString());
		}
	}



}
