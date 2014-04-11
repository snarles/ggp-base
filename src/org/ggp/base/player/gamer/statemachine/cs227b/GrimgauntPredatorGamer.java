package org.ggp.base.player.gamer.statemachine.cs227b;

import org.ggp.base.apps.player.detail.DetailPanel;
import org.ggp.base.apps.player.detail.SimpleDetailPanel;
import org.ggp.base.player.gamer.exception.GamePreviewException;
import org.ggp.base.player.gamer.statemachine.StateMachineGamer;
import org.ggp.base.util.game.Game;
import org.ggp.base.util.statemachine.StateMachine;
import org.ggp.base.util.statemachine.cache.CachedStateMachine;
import org.ggp.base.util.statemachine.exceptions.GoalDefinitionException;
import org.ggp.base.util.statemachine.exceptions.MoveDefinitionException;
import org.ggp.base.util.statemachine.exceptions.TransitionDefinitionException;
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

	/**
	 * Implement this to build useful game state before game starts.
	 */
	@Override
	public void stateMachineMetaGame(long timeout)
			throws TransitionDefinitionException, MoveDefinitionException, GoalDefinitionException {
		// Does nothing here.  Optional.
	}

	@Override
	public String getName() {
		return this.getClass().getSimpleName();
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
