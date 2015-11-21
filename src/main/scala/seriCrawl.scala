/**
 * Created by Xiaosu on 11/05/15.
 */


import java.util.{Date, Calendar}

import scala.collection.JavaConversions._

import org.jsoup.{Connection, Jsoup}

import org.jsoup.nodes.{Element, Document}
import org.jsoup.select.Elements

import org.openqa.selenium.remote.DesiredCapabilities
import org.openqa.selenium.phantomjs.PhantomJSDriver
import org.openqa.selenium.remote.RemoteWebElement

import java.util.concurrent.TimeUnit
import scala.collection.JavaConversions._
import org.apache.commons.lang3.StringUtils
import org.openqa.selenium.{By, OutputType, Dimension}
import java.io.File
import org.apache.commons.io.FileUtils


var section: String = "sports"
var start_Date: String = "20000101"
var end_Date: String = "20151231"

var url: String = "http://query.nytimes.com/search/sitesearch/#/*/from" + start_Date + "to" + end_Date + "/allresults/1/allauthors/relevance/" + section.capitalize


val capabilities = new DesiredCapabilities()
// Set PhantomJS Path
capabilities.setCapability("phantomjs.binary.path", "/home/tongx/Github/others/phantomjs/bin/phantomjs")
val driver = new PhantomJSDriver(capabilities)

var nextpage: Boolean = true

var allLinks: List[String] = List()

while(nextpage) {

  driver.get(url)

  //driver.getCurrentUrl()

  var newlinks: List[String] = driver.findElementsByCssSelector("#searchResults a").map(x => x.getAttribute("href")).toList.distinct



  //contents.map {_.length}

  nextpage = driver.findElementsByCssSelector(".next").nonEmpty

  if(nextpage) {
    driver.findElementByCssSelector(".next").click()
  }else {
    nextpage = false
  }


}


/*  Another way to grab the links in the home webpage */
var html: String = driver.getPageSource
var docM: Document = Jsoup.parse(html)
var content: Elements = docM.getElementsByClass("searchResults")
var linksM: List[String] = content.select("a[href]").toList.map{x => x.attr("href")}




driver.close()
driver.quit()


/*   ///////////////////////////////////////////////////////////////   */

/*  Load a Document from a URL */
var doc: Document = Jsoup.connect(url).get()
doc.title()

/*  Use selector to find elements in a document  */
var name: List[String] = List("StoryHeader","story")
/* so far cannot find a good way to append two Elements to one Elements. */
/* As a result, I forth the Element to be a String and then concatenate two strings */
var stories: Document = Jsoup.parse(name.map {x => doc.getElementsByClass(x).select("a[href]").toString}.reduce{_+_})
var stories_a: Elements = stories.select("a[href]")

var links: List[String] = stories_a.toList.map {x => x.attr("href")}
