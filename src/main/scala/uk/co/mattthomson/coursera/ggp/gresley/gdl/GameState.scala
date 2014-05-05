package uk.co.mattthomson.coursera.ggp.gresley.gdl

case class GameState(game: GameDescription, trueFacts: Set[Fact]) {
  private lazy val stateFacts = propagateConditionals(game.constantFacts, Map(), game.stateRules)

  lazy val legalActions = {
    game.actions.map { case (role, as) => (role, as.filter(isLegal(role))) }
  }

  def isLegal(role: String)(action: Action): Boolean = {
    val fact = Legal(Role(role), action)
    game.constantFacts.getOrElse(classOf[Legal], Set()).contains(fact) ||
      game.legalMoveRules.exists { rule => rule.proves(fact, stateFacts, Map(), Some(this)) }
  }

  def update(actions: Map[String, Action]) = {
    def isTrue(stateFacts: Map[FactTag, Set[Fact]], actions: Map[Role, Action])(fact: Fact): Boolean = {
      val nextFact = Next(fact)
      game.constantFacts.getOrElse(classOf[Next], Set()).contains(nextFact) ||
        game.nextStateRules.exists { rule => rule.proves(nextFact, stateFacts, actions, Some(this)) }
    }

    val actionsWithRoles = actions.map { case (role, action) => (Role(role), action) }.toMap
    val updatedStateFacts = propagateConditionals(stateFacts, actionsWithRoles, game.stateRules)

    val facts = game.baseFacts.filter(isTrue(updatedStateFacts, actionsWithRoles))

    new GameState(game, facts)
  }

  lazy val falseFacts = game.baseFacts -- trueFacts

  lazy val isTerminal = game.terminalRules.exists { rule => rule.proves(Terminal, stateFacts, Map(), Some(this)) }

  def value(role: String) = propagateConditionals(stateFacts, Map(), game.goalRules)
    .getOrElse(classOf[Goal], Set())
    .collect { case g @ Goal(Role(LiteralTerm(`role`)), _) => g.value }
    .headOption
    .getOrElse(0)

  private def propagateConditionals(facts: Map[FactTag, Set[Fact]], moves: Map[Role, Action], conditionals: Set[Conditional]): Map[FactTag, Set[Fact]] = {
    val updatedFacts = conditionals.foldLeft(facts) { case (f, conditional) => conditional.propagate(f, moves, Some(this)) }

    def totalSize(m: Map[_, Set[_]]) = m.map { case (_, v) => v.size}.sum
    if (totalSize(facts) == totalSize(updatedFacts)) facts else propagateConditionals(updatedFacts, moves, conditionals)
  }
}
