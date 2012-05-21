package org.w3.rdfvalidator

import org.scalatra.test.scalatest._
import org.scalatest.matchers._

class RDFValidatorTest extends ScalatraFunSuite with ShouldMatchers {

  addServlet(classOf[RDFValidator], "/*")

  def validate(uri:String, expectedUnicornStatus:String, contains:String) =
    test(uri) {
      get("/rdfvalidator?uri="+uri) {
        status should equal (200)
        val unicornStatus = (scala.xml.XML.loadString(body) \ "status" \ "@value").head.asInstanceOf[scala.xml.Text].toString
        unicornStatus should equal (expectedUnicornStatus)
        body should include (contains)
      }
    }

  def failed(uri:String, contains:String) =
    validate(uri, "failed", contains)

  def passed(uri:String, contains:String) =
    validate(uri, "passed", contains)

  failed("http://www.w3.org/2000/10/rdf-tests/rdfcore/rdf-containers-syntax-vs-schema/error001.rdf", "E206")
  failed("http://www.w3.org/2000/10/rdf-tests/rdfcore/rdf-containers-syntax-vs-schema/error002.rdf", "E204")
  passed("http://www.w3.org/People/Berners-Lee/card.rdf", "")

}
