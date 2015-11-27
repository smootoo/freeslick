package freeslick.profile.utils

import slick.SlickException
import slick.ast._
import slick.compiler.Phase
import slick.driver.JdbcDriver
import slick.util.ConstArray
import slick.util.MacroSupport.macroSupportInterpolation

// For profiles that don't provide built-in pagination
// Freeslick's Oracle and MSSQL profiles need this
// This is code adapted from code that used to live in Slick's main library
trait DriverRowNumberPagination { jdbcDriver: JdbcDriver =>
  trait RowNumberPagination { queryBuilder: QueryBuilder =>
    case class StarAnd(child: Node) extends UnaryNode with SimplyTypedNode {
      type Self = StarAnd
      override protected[this] def rebuild(child: Node) = StarAnd(child)
      override protected def buildType = UnassignedType
    }

    override protected def buildComprehension(c: Comprehension) {
      if (c.fetch.isDefined || c.offset.isDefined) {
        val r = new AnonSymbol
        val rn = symbolName(r)
        val tn = symbolName(new AnonSymbol)
        val c2 = DriverRowNumberPagination.makeSelectPageable(c, r)
        val c3 = Phase.fixRowNumberOrdering.fix(c2, None).asInstanceOf[Comprehension]
        b"select "
        buildSelectModifiers(c)
        c3.select match {
          case Pure(StructNode(ch), _) =>
            b.sep(ch.filter { case (_, RowNumber(_)) => false; case _ => true }, ", ") {
              case (sym, StarAnd(RowNumber(_))) => b"*"
              case (sym, _) => b += symbolName(sym)
            }
          case o => throw new SlickException("Unexpected node " + o + " in SELECT slot of " + c)
        }
        b" from ("
        queryBuilder.buildComprehension(c3)
        b") $tn where $rn"
        (c.fetch, c.offset) match {
          case (Some(t), Some(d)) => b" between ${QueryParameter.constOp[Long]("+")(_ + _)(d, LiteralNode(1L).infer())} and ${QueryParameter.constOp[Long]("+")(_ + _)(t, d)}"
          case (Some(t), None) => b" between 1 and $t"
          case (None, Some(d)) => b" > $d"
          case _ => throw new SlickException("Unexpected empty fetch/offset")
        }
        b" order by $rn"
      } else queryBuilder.buildComprehension(c)
    }
  }
}

object DriverRowNumberPagination {
  /**
   * Create aliases for all selected rows (unless it is a "select *" query),
   * add a RowNumber column, and remove FETCH and OFFSET clauses. The SELECT
   * clause of the resulting Comprehension always has the shape
   * Pure(StructNode(_)).
   */
  def makeSelectPageable(c: Comprehension, rn: AnonSymbol): Comprehension = c.select match {
    case Pure(StructNode(ch), _) =>
      c.copy(select = Pure(StructNode(ch :+ (rn -> RowNumber()))), fetch = None, offset = None)
    case Pure(ProductNode(ch), _) =>
      c.copy(select = Pure(StructNode(ch.map(n => new AnonSymbol -> n) :+ (rn -> RowNumber()))), fetch = None, offset = None)
    case Pure(n, _) =>
      c.copy(select = Pure(StructNode(ConstArray(new AnonSymbol -> n, rn -> RowNumber()))), fetch = None, offset = None)
    case o => throw new SlickException("Unexpected node type " + o + " in SELECT slot of " + c)
  }
}

