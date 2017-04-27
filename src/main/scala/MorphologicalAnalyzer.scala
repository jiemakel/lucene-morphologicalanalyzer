package fi.seco.lucene

import org.apache.lucene.analysis.TokenStream
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute
import org.apache.lucene.analysis.tokenattributes.PositionIncrementAttribute
import org.apache.lucene.analysis.tokenattributes.OffsetAttribute
import org.apache.lucene.analysis.tokenattributes.PayloadAttribute
import org.apache.lucene.util.BytesRef
import org.apache.lucene.util.NumericUtils
import org.apache.lucene.analysis.Analyzer
import org.apache.lucene.analysis.Analyzer.TokenStreamComponents
import fi.seco.lexical.hfst.HFSTLexicalAnalysisService.WordToResults
import org.apache.lucene.analysis.Tokenizer
import org.apache.lucene.util.IOUtils
import org.apache.lucene.analysis.CharacterUtils
import fi.seco.lexical.combined.CombinedLexicalAnalysisService
import java.util.Locale
import java.util.Collections
import java.util.Collection
import scala.collection.JavaConverters._
import scala.collection.mutable.ArrayBuffer
import org.apache.lucene.store.ByteArrayDataOutput
import org.apache.lucene.util.SmallFloat

class MorphologicalAnalysisTokenStream(var tokens: Iterable[(Int, String, Iterable[(Float,Iterable[Iterable[String]])])] = null, payloads: Boolean = false) extends TokenStream {
  
  private val termAttr: CharTermAttribute = addAttribute(classOf[CharTermAttribute])
  private val posAttr: PositionIncrementAttribute = addAttribute(classOf[PositionIncrementAttribute])
  private val offAttr: OffsetAttribute = addAttribute(classOf[OffsetAttribute])
  private val plAttr: PayloadAttribute = if (payloads) addAttribute(classOf[PayloadAttribute]) else null
  
  private var wordsIterator: Iterator[(Int, String, Iterable[(Float,Iterable[Iterable[String]])])] = null // offset+word->analyses | weight->analysisParts | part

  override def reset(): Unit = {
    wordsIterator = tokens.iterator
  }
  
  private var analysesIterator: Iterator[(Float,Iterable[Iterable[String]])] = Iterator.empty
  
  private var analysisIterator: Iterator[Iterable[String]] = Iterator.empty
  
  private var analysisPartIterator: Iterator[String] = Iterator.empty

  private var startOffset = 0
  private var endOffset = 0
  private var analysisIndex = 0
  private var weight = 0.0f
  private var analysisPartIndex = 0

  final override def incrementToken(): Boolean = {
    clearAttributes()
    val analysisToken = if (!analysisPartIterator.hasNext) { // end of wordparts
      if (!analysisIterator.hasNext) { // end of analysis
        if (!analysesIterator.hasNext) { // end of analyses
          if (!wordsIterator.hasNext) return false // end of words
          val n = wordsIterator.next // next word
          analysesIterator = n._3.iterator
          posAttr.setPositionIncrement(1)
          startOffset = n._1
          weight = 1.0f
          analysisIndex = 0
          analysisPartIndex = 0
          val word = n._2
          endOffset = startOffset + word.length - 2 // W=[word]
          word
        } else {
          val n2 = analysesIterator.next // next analysis
          analysisIterator = n2._2.iterator
          posAttr.setPositionIncrement(0)
          weight = n2._1
          analysisIndex = 0
          analysisPartIndex = 0
          analysisPartIterator = analysisIterator.next.iterator
          analysisPartIterator.next
        }
      } else {
        posAttr.setPositionIncrement(0)
        analysisPartIterator = analysisIterator.next.iterator // next analysis part
        analysisIndex += 1
        analysisPartIndex = 0
        analysisPartIterator.next
      }
    } else { // next analysis part
      posAttr.setPositionIncrement(0)
      analysisPartIndex += 1
      analysisPartIterator.next
    }
    offAttr.setOffset(startOffset, endOffset)
    if (payloads) {
      val payload = new Array[Byte](1 + 5 + 5)
      val payloadOutput = new ByteArrayDataOutput(payload)
      payloadOutput.writeByte(SmallFloat.floatToByte52(weight))
      payloadOutput.writeVInt(analysisIndex)
      payloadOutput.writeVInt(analysisPartIndex)
      plAttr.setPayload(new BytesRef(payload,0, payloadOutput.getPosition))
    }
    termAttr.append(analysisToken)
    return true
  }
}

class MorphologicalTokenizer(locale: Locale, inflections: java.util.List[String] = Collections.EMPTY_LIST.asInstanceOf[java.util.List[String]], segmentBaseform: Boolean = false, guessUnknown: Boolean = true, segmentUnknown: Boolean = false, maxEditDistance: Int = 0, depth: Int = 1) extends Tokenizer {
  
  import MorphologicalAnalyzer._
  
  val analyzer = new CombinedLexicalAnalysisService()
  
  val tokenStream = new MorphologicalAnalysisTokenStream() {
    override def reset() = {
      MorphologicalTokenizer.this.reset()
      super.reset()
    }
    override def end() = {
      MorphologicalTokenizer.this.end()
      super.end()
    }
    override def close() = {
      MorphologicalTokenizer.this.close()
      super.close()
    }
  }
  
  val arr = new Array[Char](8 * 1024)
  override def reset(): Unit = {
    super.reset()
    val buffer = new StringBuilder();
    var numCharsRead: Int = input.read(arr, 0, arr.length)
    while (numCharsRead != -1) {
      buffer.appendAll(arr, 0, numCharsRead)
      numCharsRead = input.read(arr, 0, arr.length)
    }
    input.close()
    val string = buffer.toString
    tokenStream.tokens = analysisToTokenStream(analyzer.analyze(buffer.toString, locale, inflections, segmentBaseform, guessUnknown, segmentUnknown, maxEditDistance, depth))
  }
  
  final override def incrementToken(): Boolean = throw new UnsupportedOperationException("Can't increment")
  val fiLocale = new Locale("fi")
}

class MorphologicalAnalyzerlocale(locale: Locale, inflections: java.util.List[String] = Collections.EMPTY_LIST.asInstanceOf[java.util.List[String]], segmentBaseform: Boolean = false, guessUnknown: Boolean = true, segmentUnknown: Boolean = false, maxEditDistance: Int = 0, depth: Int = 1) extends Analyzer {
  
  override def createComponents(fieldName: String): TokenStreamComponents = {
    val tokenizer = new MorphologicalTokenizer(locale, inflections, segmentBaseform, guessUnknown, segmentUnknown, maxEditDistance, depth)
    return new TokenStreamComponents(tokenizer, tokenizer.tokenStream)
  }
  
}

case class WordToAnalysis(
  word: String,
  analysis: List[Analysis]
)

case class Analysis(
  weight: Float,
  wordParts: List[WordPart],
  globalTags: List[Map[String,List[String]]]
)
 
case class WordPart(
  lemma: String, 
  tags: List[Map[String,List[String]]]
)

object MorphologicalAnalyzer {
  def filterGlobalTag(tag: String): Boolean = tag match {
    case "WHITESPACE" => true
    case "POS_MATCH" => true
    case "BEST_MATCH" => true
    case "HEAD" => true
    case "DEPREL" => true
    case "BASEFORM_FREQUENCY" => true
    case _ => false
  }

  def filterTag(tag: String): Boolean = tag match {
    case "BASEFORM_FREQUENCY" => true
    case "SEGMENT" => true
    case _ => false
  }
  
  def scalaAnalysisToTokenStream(analysis: List[WordToAnalysis], filterGlobalTag: (String) => Boolean = filterGlobalTag, filterTag: (String) => Boolean = filterTag): Iterable[(Int, String, Iterable[(Float,Iterable[Iterable[String]])])] = {
    val wordsToAnalysis = new ArrayBuffer[(Int, String, Iterable[(Float,Iterable[Iterable[String]])])] 
    var offset = 0
    for (word <- analysis)
      if (word.analysis(0).globalTags.exists(_.contains("WHITESPACE")))
        offset += word.word.length
      else {
        val analyses = new ArrayBuffer[(Float,Iterable[Iterable[String]])]
        val analysisParts = new ArrayBuffer[Iterable[String]]
        for (analysis <- word.analysis) {
          val prefix = if (analysis.globalTags.exists(_.contains("BEST_MATCH"))) "B" else "O"
          val analysis2 = new ArrayBuffer[Iterable[String]]
          val ganalysis = new ArrayBuffer[String]
          analysis2 += ganalysis
          for (globalTags <- analysis.globalTags;(tag, tagValues) <- globalTags.toSeq; if !filterGlobalTag(tag); tagValue <- tagValues) ganalysis += prefix + tag+"="+tagValue
          var lemma = ""
          for (wordPart <- analysis.wordParts) {
            val partAnalysis = new ArrayBuffer[String]
            for (tags <- wordPart.tags;(tag, tagValues) <- tags; if !filterTag(tag); tagValue <- tagValues) partAnalysis += prefix + tag+"="+tagValue
            if (!partAnalysis.isEmpty) analysis2 += partAnalysis
            lemma += wordPart.lemma
          }
          ganalysis += prefix + "L="+lemma
          analyses += ((analysis.weight, analysis2))
        }
        wordsToAnalysis += ((offset, "W="+word.word, analyses)) 
        offset += word.word.length
      }
    wordsToAnalysis
  }
  
  def analysisToTokenStream(analysis: java.util.List[WordToResults], filterGlobalTag: (String) => Boolean = filterGlobalTag, filterTag: (String) => Boolean = filterTag): Iterable[(Int, String, Iterable[(Float,Iterable[Iterable[String]])])] = {
    val wordsToAnalysis = new ArrayBuffer[(Int, String, Iterable[(Float,Iterable[Iterable[String]])])] 
    var offset = 0
    for (word <- analysis.asScala)
      if (word.getAnalysis.get(0).getGlobalTags().containsKey("WHITESPACE"))
        offset += word.getWord.length
      else {
        val analyses = new ArrayBuffer[(Float,Iterable[Iterable[String]])]
        val analysisParts = new ArrayBuffer[Iterable[String]]
        for (analysis <- word.getAnalysis.asScala) {
          val prefix = if (analysis.getGlobalTags.containsKey("BEST_MATCH")) "B" else "O"
          val analysis2 = new ArrayBuffer[Iterable[String]]
          val ganalysis = new ArrayBuffer[String]
          analysis2 += ganalysis
          for ((tag, tagValues) <-  analysis.getGlobalTags.asScala.toSeq; if !filterGlobalTag(tag); tagValue <- tagValues.asScala) ganalysis += prefix + tag+"="+tagValue
          var lemma = ""
          for (wordPart <- analysis.getParts.asScala) {
            val partAnalysis = new ArrayBuffer[String]
            for ((tag, tagValues) <- wordPart.getTags.asScala; if !filterTag(tag); tagValue <- tagValues.asScala) partAnalysis += prefix + tag+"="+tagValue
            if (!partAnalysis.isEmpty) analysis2 += partAnalysis
            lemma += wordPart.getLemma
          }
          ganalysis += prefix + "L="+lemma
          analyses += ((analysis.getWeight, analysis2))
        }
        wordsToAnalysis += ((offset, "W="+word.getWord, analyses)) 
        offset += word.getWord.length
      }
    wordsToAnalysis
  }
}