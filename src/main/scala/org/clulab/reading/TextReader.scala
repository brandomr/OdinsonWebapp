package org.clulab.reading

import java.io.File

import ai.lum.common.ConfigUtils._
import ai.lum.common.FileUtils._
import ai.lum.common.TryWithResources._
import ai.lum.common.ConfigFactory
import ai.lum.odinson.extra.ProcessorsUtils
import ai.lum.odinson.{ExtractorEngine, Document => OdinsonDocument}
import org.clulab.processors.Processor
import TextReader.fileContents

import scala.io.Source

object TextReader{
  def fileContents(fn: String): String = using(Source.fromFile(fn)) { f =>
    f.getLines().mkString("\n")
  }
  def fromFile(proc: Processor, ruleFile: String): TextReader = {
    val yml = fileContents(ruleFile)
    new TextReader(proc, yml)
  }
}
class TextReader(val proc: Processor, val rules: String) {

  val config = ConfigFactory.load()
  val numEvidenceDisplay = config.get[Int]("ui.numEvidenceDisplay").getOrElse(3)
  val consolidateByLemma = config.get[Boolean]("ui.lemmaConsolidation").getOrElse(true)

  def extractMatchesFromFile(filename: String): Seq[Match] = {
    extractMatches(fileContents(filename))
  }

  def extractMatches(text: String): Seq[Match] = {
    val procDoc = proc.annotate(text)
    val odinsonDocument = ProcessorsUtils.convertDocument(procDoc)
    val ee = mkExtractorEngine(odinsonDocument)
    val reader = new CorpusReader(ee, numEvidenceDisplay, consolidateByLemma)
    reader.extractMatches(rules)
  }

  /**
   * Constructs an [[ai.lum.odinson.ExtractorEngine]] from a single-doc in-memory index ([[org.apache.lucene.store.RAMDirectory]])
   */
  def mkExtractorEngine(doc: OdinsonDocument): ExtractorEngine = {
    ExtractorEngine.inMemory(doc)
  }

}
