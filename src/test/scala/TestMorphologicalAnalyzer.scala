import org.junit.Test
import org.junit.Assert._
import org.hamcrest.CoreMatchers._
import fi.seco.lucene.MorphologicalAnalyzer
import java.util.Locale
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute
import org.apache.lucene.analysis.tokenattributes.PositionIncrementAttribute
import org.apache.lucene.analysis.tokenattributes.OffsetAttribute


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
    assertEquals("BFIRST_IN_SENTENCE=TRUE",term.toString)
  }
  
}