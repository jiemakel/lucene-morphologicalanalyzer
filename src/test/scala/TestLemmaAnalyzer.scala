import java.util.Locale

import fi.seco.lucene.LemmaAnalyzer
import org.apache.lucene.analysis.tokenattributes.{CharTermAttribute, OffsetAttribute, PositionIncrementAttribute}
import org.junit.Assert._
import org.junit.Test


class TestLemmaAnalyzer {
  
  @Test
  def testLemmaAnalyzer {
    var a = new LemmaAnalyzer(new Locale("fi"))
    var t = a.tokenStream("test", "Juoksin läpi kaupungin")
    var term = t.getAttribute(classOf[CharTermAttribute])
    var pos = t.getAttribute(classOf[PositionIncrementAttribute])
    var off = t.getAttribute(classOf[OffsetAttribute])
    t.reset()
    assertTrue(t.incrementToken())
    assertEquals(0,off.startOffset)
    assertEquals(7,off.endOffset)
    assertEquals(1,pos.getPositionIncrement)
    assertEquals("juoksin",term.toString)
    assertTrue(t.incrementToken())
    assertEquals(0,off.startOffset)
    assertEquals(7,off.endOffset)
    assertEquals(0,pos.getPositionIncrement)
    assertEquals("juosta",term.toString)
    a = new LemmaAnalyzer(new Locale("fi"), false)
    t = a.tokenStream("test", "juoksin läpi kaupungin")
    term = t.getAttribute(classOf[CharTermAttribute])
    pos = t.getAttribute(classOf[PositionIncrementAttribute])
    off = t.getAttribute(classOf[OffsetAttribute])
    t.reset()
    assertTrue(t.incrementToken())
    assertEquals(0,off.startOffset)
    assertEquals(7,off.endOffset)
    assertEquals(1,pos.getPositionIncrement)
    assertEquals("juosta",term.toString)    
  }
  
}