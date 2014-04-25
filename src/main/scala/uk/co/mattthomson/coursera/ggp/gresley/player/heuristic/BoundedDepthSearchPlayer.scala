package uk.co.mattthomson.coursera.ggp.gresley.player.heuristic

import uk.co.mattthomson.coursera.ggp.gresley.player.Player
import uk.co.mattthomson.coursera.ggp.gresley.gdl.{Action, GameState, GameDescription}
import akka.actor.ActorRef

class BoundedDepthSearchPlayer(depthLimit: Int) extends Player[Seq[String]] {
  override def initialize(game: GameDescription, role: String): Seq[String] = game.roles.filter(_ != role)

  override def play(state: GameState, role: String, source: ActorRef, otherRoles: Seq[String]): Seq[String] = {
    val chosenAction = bestMove(state, role, otherRoles)
    log.info(s"Chosen action: $chosenAction")

    source ! chosenAction
    otherRoles
  }

  private def bestMove(state: GameState, role: String, otherRoles: Seq[String]) = {
    val legalActions = state.legalActions(role)
    if (legalActions.size == 1) legalActions.head else {
      val initialAction: Option[Action] = None
      val (_, bestAction) = legalActions.foldLeft((-1, initialAction))(tryNextMinScore(state, role, otherRoles, 101, 1))
      bestAction.get
    }
  }

  private def minScore(state: GameState, role: String, otherRoles: Seq[String], alpha: Int, beta: Int, level: Int)(action: Action): Int = {
    val otherActions: Seq[Map[String, Action]] = otherRoles.foldLeft(Seq(Map[String, Action]()))(addAction(state))

    otherActions.foldLeft(beta)(tryNextMaxScore(state, action, role, otherRoles, alpha, level + 1))
  }

  private def maxScore(role: String, otherRoles: Seq[String], level: Int)(state: GameState, alpha: Int, beta: Int): Int = {
    if (state.isTerminal) state.value(role)
    else if (level > depthLimit) state.value(role)
    else {
      val initialAction: Option[Action] = None
      val (bestScore, _) = state.legalActions(role).foldLeft((alpha, initialAction))(tryNextMinScore(state, role, otherRoles, beta, level))
      bestScore
    }
  }

  private def addAction(state: GameState)(soFar: Seq[Map[String, Action]], role: String): Seq[Map[String, Action]] = {
    soFar.flatMap(actions => state.legalActions(role).map { action => actions + (role -> action) })
  }

  private def tryNextMinScore(state: GameState, role: String, otherRoles: Seq[String], beta: Int, level: Int)(bestSoFar: (Int, Option[Action]), action: Action) = {
    val (alpha, _) = bestSoFar
    if (alpha == 100) bestSoFar
    else if (alpha >= beta) (beta, None)
    else {
      val score = minScore(state, role, otherRoles, alpha, beta, level)(action)
      if (score > alpha) (score, Some(action)) else bestSoFar
    }
  }

  private def tryNextMaxScore(state: GameState, action: Action, role: String, otherRoles: Seq[String], alpha: Int, level: Int)(beta: Int, otherActions: Map[String, Action]): Int = {
    if (beta == 0) beta
    else if (beta <= alpha) alpha
    else {
      val actions = Map(role -> action) ++ otherActions
      val newState = state.update(actions)
      val score = maxScore(role, otherRoles, level)(newState, alpha, beta)
      if (score < beta) score else beta
    }
  }
}
