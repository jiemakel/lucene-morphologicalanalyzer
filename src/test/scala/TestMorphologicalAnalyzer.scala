import java.util.Locale

import fi.seco.lucene.MorphologicalAnalyzer
import org.apache.lucene.analysis.tokenattributes.{CharTermAttribute, OffsetAttribute, PositionIncrementAttribute}
import org.junit.Assert._
import org.junit.Test


class TestMorphologicalAnalyzer {
  
  @Test
  def testMorphologicalAnalyzer {
    val a = new MorphologicalAnalyzer(new Locale("fi"))
    val t = a.tokenStream("test", "juoksin läpi kaupungin")
    val term = t.getAttribute(classOf[CharTermAttribute])
    val pos = t.getAttribute(classOf[PositionIncrementAttribute])
    val off = t.getAttribute(classOf[OffsetAttribute])
    t.reset()
    assertTrue(t.incrementToken())
    assertEquals(0,off.startOffset)
    assertEquals(7,off.endOffset)
    assertEquals(1,pos.getPositionIncrement)
    assertEquals("W=juoksin",term.toString)
    assertTrue(t.incrementToken())
    assertEquals(0,off.startOffset)
    assertEquals(7,off.endOffset)
    assertEquals(0,pos.getPositionIncrement)
    assertEquals("BL=juosta",term.toString)
  }
  
}