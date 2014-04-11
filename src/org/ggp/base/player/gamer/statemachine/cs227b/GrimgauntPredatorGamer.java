package org.ggp.base.player.gamer.statemachine.cs227b;

import org.ggp.base.apps.player.detail.DetailPanel;
import org.ggp.base.apps.player.detail.SimpleDetailPanel;
import org.ggp.base.player.gamer.exception.GamePreviewException;
import org.ggp.base.player.gamer.statemachine.StateMachineGamer;
import org.ggp.base.util.game.Game;
import org.ggp.base.util.statemachine.StateMachine;
import org.ggp.base.util.statemachine.cache.CachedStateMachine;
import org.ggp.base.util.statemachine.implementation.prover.ProverStateMachine;

/**
 * Base class for Grimgaunt Predator.  Extend this abstract class and implement the following methods:
 *		stateMachineSelectMove
 *		stateMachineMetaGame (optional)
 * For example, a stateless exhaustive search would only implement stateMachineSelectMove, with appropriate class name:
 *		public class GrimgauntPredatorStatelessBritishMuseumGamer extends GrimgauntPredatorGamer {
 *			@override public Move stateMachineSelectMove(final long timeout) { Exhaustive search code... }
 *		}
 */
public abstract class GrimgauntPredatorGamer extends StateMachineGamer {

	@Override
	public String getName() {
		return "Grimgaunt Predator";
	}

	@Override
	public DetailPanel getDetailPanel() {
		return new SimpleDetailPanel();
	}

	@Override
	public StateMachine getInitialStateMachine() {
		return new CachedStateMachine(new ProverStateMachine());
	}

	@Override
	public void stateMachineStop() {
		// Do nothing
	}

	@Override
	public void stateMachineAbort() {
		// Do nothing
	}

	@Override
	public void preview(Game g, long timeout) throws GamePreviewException {
		// Do nothing
	}
}
