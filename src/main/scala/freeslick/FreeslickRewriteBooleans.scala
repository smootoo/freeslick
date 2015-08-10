package freeslick

import slick.ast.{ Library, Node }
import slick.compiler.RewriteBooleans
import slick.ast.TypeUtil.:@

class FreeslickRewriteBooleans extends RewriteBooleans {
  override def rewrite(n: Node): Node = n match {
    case Library.SilentCast(sc) :@ tpe if isBooleanLike(tpe) =>
      sc
    case Library.Cast(ca) :@ tpe if isBooleanLike(tpe) =>
      n
    case n =>
      super.rewrite(n)
  }
}
