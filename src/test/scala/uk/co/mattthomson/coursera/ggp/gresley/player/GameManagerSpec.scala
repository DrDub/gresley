package uk.co.mattthomson.coursera.ggp.gresley.player

import org.scalatest.{BeforeAndAfter, FlatSpec}
import akka.testkit.{ImplicitSender, TestKit}
import akka.actor.{Actor, Props, ActorSystem}
import akka.pattern.ask
import scala.concurrent.duration._
import uk.co.mattthomson.coursera.ggp.gresley.gdl._
import uk.co.mattthomson.coursera.ggp.gresley.player.GameManager.{SelectMove, NewGame, GamesInProgress}
import akka.util.Timeout
import uk.co.mattthomson.coursera.ggp.gresley.moveselector.MoveSelectorPropsFactory
import uk.co.mattthomson.coursera.ggp.gresley.player.Player.Ready

class GameManagerSpec extends TestKit(ActorSystem("TestActorSystem")) with FlatSpec with ImplicitSender with BeforeAndAfter {
  val manager = system.actorOf(Props(new GameManager(_ => new DummyPlayer, new DummyMoveSelectorPropsFactory)))

  after {
    import system.dispatcher
    implicit val timeout = Timeout(1)

    val games = manager ? GamesInProgress
    games.foreach(g => g.asInstanceOf[List[String]].foreach(abortGame))
  }

  "The manager" should "respond to an info message" in {
    manager ! Info
    expectMsg("((name gresley) (status available))")
  }

  it should "respond to a start message" in {
    val id = startGame

    manager ! GamesInProgress
    expectMsg(List(id))
  }

  it should "respond to a play message" in {
    val id = startGame

    manager ! Play(id, None)
    expectMsg(Action("left", Nil))
  }

  it should "respond to a stop message" in {
    val id = startGame

    manager ! Stop(id, List(Action("left", Nil), Action("right", Nil)))
    expectMsg("done")

    manager ! GamesInProgress
    expectMsg(Nil)
  }

  it should "respond to an abort message" in {
    val id = startGame

    abortGame(id)

    manager ! GamesInProgress
    expectMsg(Nil)
  }

  private def startGame: String = {
    val game = GameDescription("(role black)")
    val id = s"id-${System.nanoTime()}"
    manager ! Start(id, "black", game, 1.second, 2.seconds)
    expectMsg(Ready)

    id
  }

  private def abortGame(id: String) {
    manager ! Abort(id)
    expectMsg("done")
  }
}

class DummyPlayer extends Actor {
  override def receive: Receive = {
    case NewGame(_, _, source) => source ! Ready
    case SelectMove(source) => source ! Action("left", Nil)
  }
}

class DummyMoveSelector extends Actor {
  def receive = {
    case _ => sender ! Action("left", Nil)
  }
}

class DummyMoveSelectorPropsFactory extends MoveSelectorPropsFactory {
  override def forGame(game: GameDescription) = Props[DummyMoveSelector]
}
