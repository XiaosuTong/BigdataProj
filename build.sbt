//The expressions in build.sbt are independent of on another, and they are expressions,
//rather than complete Scala statements. An implication of this is that you cannot define
//a top-level val, object, class, or method in build.sbt

// sbt needs black lines as delimiter to tell where one expression stops

//there are two options to add third-party libraries, first called unmanaged dependencies,
//which is to drop jars to lib and they will be placed on the project classpath. You can
//change the unmanaged-base key if want to use a different directory rather than lib
//unmanagedBase <<= baseDirectory {base => base / "custom_lib"}


name := "BigDataProj"

version := "1.0"

scalaVersion := "2.11.7"

//Not all packages live on the same server, sbt uses the standard Maven2 repository by
//default. If your dependency is not on one of the default repositories, you have to add
// a resolver to help Ivy find it.
resolvers += "Local couchdb-scala repo" at (baseDirectory.value / "lib/couchdb-scala_2.11").toURI.toString

libraryDependencies ++= Seq(
  "com.ibm" % "couchdb-scala_2.11" % "0.6.0",
  "org.jsoup" % "jsoup" % "1.7.2",
  "commons-codec" % "commons-codec" % "1.9",
  "com.github.detro.ghostdriver" % "phantomjsdriver" % "1.1.0",
  "org.slf4j" % "slf4j-nop" % "1.7.10",
  "net.liftweb" % "lift-json_2.11" % "2.6",
//  "net.liftweb" % "lift-json_2.10" % "2.6",
  "com.google.code.gson" % "gson" % "2.3.1" ,
  "org.apache.spark" %% "spark-yarn" % "1.5.2" % "provided"
)

//sbt will add your project's Scala version to the artifact name. This is just a shortcut. You could write this without the %%:
//libraryDependencies += "org.scala-tools" % "scala-stm_2.9.1" % "0.3"
//Assuming the scalaVersion for your build is 2.9.1, the following is identical:
// libraryDependencies += "org.scala-tools" %% "scala-stm" % "0.3"

