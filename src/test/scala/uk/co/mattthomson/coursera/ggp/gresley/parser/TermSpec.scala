package uk.co.mattthomson.coursera.ggp.gresley.parser

import org.scalatest.FlatSpec
import org.scalatest.matchers.ShouldMatchers
import uk.co.mattthomson.coursera.ggp.gresley.parser.Term._

class TermSpec extends FlatSpec with ShouldMatchers {
  "A literal term" should "be constructable from a string" in {
    val term: Term = "y"

    term should be (LiteralTerm("y"))
  }

  it should "not be substitutable" in {
    val term = LiteralTerm("y")
    val values = Map("x" -> "1", "y" -> "2", "z" -> "3")

    term.substitute(values) should be (term)
  }

  it should "match against itself" in {
    val term: Term = "y"

    term.matches(term) should be (Some(Map()))
  }

  it should "not match against another literal" in {
    val term: Term = "y"

    term.matches("z") should be (None)
  }

  it should "match against a variable" in {
    val term: Term = "y"

    term.matches(VariableTerm("x")) should be (Some(Map("x" -> "y")))
  }

  "A variable term" should "substitute for a matching value" in {
    val term = VariableTerm("y")
    val values = Map("x" -> "1", "y" -> "2", "z" -> "3")

    term.substitute(values) should be (LiteralTerm("2"))
  }

  it should "not substitute if no matching value" in {
    val term = VariableTerm("a")
    val values = Map("x" -> "1", "y" -> "2", "z" -> "3")

    term.substitute(values) should be(term)
  }

  it should "match against itself" in {
    val term = VariableTerm("x")

    term.matches(term) should be (Some(Map()))
  }

  it should "not match against another variable" in {
    val term = VariableTerm("x")

    term.matches(VariableTerm("y")) should be (None)
  }

  it should "match against a literal" in {
    val term = VariableTerm("x")

    term.matches("y") should be (Some(Map("x" -> "y")))
  }
}
