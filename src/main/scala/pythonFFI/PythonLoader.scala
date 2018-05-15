package pythonFFI

import jep.Jep
import org.apache.logging.log4j.scala.Logger
import scala.collection.JavaConverters.asScalaBuffer

class PythonLoader {
  val logger = Logger(PythonLoader.getClass)

  val rootPath = "KNN/src/"
  val jep = new Jep()

  def runTestPy() = {
    jep.runScript(rootPath + "add.py")
    val a: Int = 2
    val b: Int = 3

    // There are multiple ways to evaluate.
    // First:
    jep.eval(s"c = add($a, $b)")
    val ans1 = jep.getValue("c").asInstanceOf[Int]
    logger.debug(s"${ans1}")
    // Second:
    val ans2 = jep
      .invoke("add", a.asInstanceOf[Object], b.asInstanceOf[Object])
      .asInstanceOf[Int]
    logger.debug(s"${ans2}")
  }

  def runSentimentModel(evalSentence: String): Tuple2[String, String] = {
    logger.debug(s"Eval sentence with Sentiment Model: ${evalSentence}")

    jep.runScript(rootPath + "useModelByScalaRequest.py")
    val javaList: java.util.List[String] = jep
      .invoke("execModel", evalSentence.asInstanceOf[Object])
      .asInstanceOf[java.util.List[String]]

    val scalaList =  asScalaBuffer(javaList).toList
    logger.debug(s"Evaled sentence with Sentiment Model: ${scalaList}")
    return (scalaList(0),
      BigDecimal(scalaList(1).asInstanceOf[Double])
        .setScale(2, BigDecimal.RoundingMode.HALF_UP).toString())
  }

}

object PythonLoader {
  def apply: PythonLoader = new PythonLoader()
}