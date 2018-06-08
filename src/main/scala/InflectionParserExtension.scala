import java.util.Locale

import fi.seco.lucene.MorphologicalAnalyzer
import org.apache.lucene.queryparser.ext.{ExtensionQuery, ParserExtension}
import org.apache.lucene.search.TermInSetQuery
import org.apache.lucene.util.BytesRef

import scala.collection.JavaConverters._

class InflectionParserExtension(locale: Locale) extends ParserExtension {
  override def parse(query: ExtensionQuery) = new TermInSetQuery(query.getField,MorphologicalAnalyzer.analyzer.allInflections(query.getRawQueryString,locale).asScala.map(s => new BytesRef(s)).toSeq:_*)
}
