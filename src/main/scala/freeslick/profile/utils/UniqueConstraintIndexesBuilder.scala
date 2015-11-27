package freeslick.profile.utils

import slick.driver.JdbcDriver
import slick.lifted.Index

trait UniqueConstraintIndexesBuilder {
  jdbcDriver: JdbcDriver =>

  trait UniqueConstraintIndexes {
    tableDDLBuilder: TableDDLBuilder =>
    protected def createIndexStmts(idx: Index): Seq[String] = {
      val indexStmt = tableDDLBuilder.createIndex(idx)
      /* Adding unique index does not imply a unique constraint and this is needed for any foreign keys,
       * so add constraint explicitly. Although in practical terms, the constraint will add an index to enforce
       * the constraint, it is considered good practice to also create the index explicitly
       * https://asktom.oracle.com/pls/asktom/f?p=100:11:2017491281965674::::P11_QUESTION_ID:36858373078604 */
      if (idx.unique) {
        val sb = new StringBuilder
        sb append "ALTER TABLE " append quoteIdentifier(table.tableName) append " ADD "
        sb append "CONSTRAINT " append quoteIdentifier(idx.name + "_cons") append " UNIQUE("
        addIndexColumnList(idx.on, sb, idx.table.tableName)
        sb append ")"
        Seq(indexStmt, sb.toString())
      } else Seq(indexStmt)
    }
  }
}
