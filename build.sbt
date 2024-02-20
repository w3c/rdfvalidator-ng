val jettyVersion = "8.1.16.v20140903"
val servletVersion = "3.0.0.v201112011016"

lazy val rdfValidator = project.in(file("."))
  .settings(
    organization := "org.w3",
    version := "1.1",
    scalaVersion := "2.12.5",
    scalacOptions ++= Seq("-deprecation"),
    javacOptions ++= Seq("-Xlint:unchecked"),
    assembly / mainClass := Some("org.w3.rdfvalidator.JettyMain"),
    assembly / assemblyJarName := "rdf-validator.jar",
    assembly / test := {},
//    assemblyMergeStrategy in assembly <<= (assemblyMergeStrategy in assembly) {
//      val fs = System.getProperty("file.separator")
//      (old) => {
//        case r if r.endsWith("about.html") => MergeStrategy.discard
//        case r if r.startsWith(List("javax", "xml", "stream").mkString(fs)) => MergeStrategy.concat
//        case x => old(x)
//      }
//    },
    resolvers += "Typesafe Repository" at "https://repo.typesafe.com/typesafe/releases/",
    resolvers += "Sonatype snapshots" at "https://oss.sonatype.org/content/repositories/snapshots",
    resolvers += "apache-repo-releases" at "https://repository.apache.org/content/repositories/releases/",
    libraryDependencies += "javax.mail" % "mail" % "1.4.7",
    // the newest versions of Jena break the code :-/
    // libraryDependencies += "org.apache.jena" % "jena-arq" % "2.12.1",
    libraryDependencies += "org.apache.jena" % "jena-arq" % "2.11.2",
    libraryDependencies += "org.eclipse.jetty" % "jetty-webapp" % jettyVersion % "compile" intransitive(),
    libraryDependencies += "org.eclipse.jetty" % "jetty-util" % jettyVersion % "compile" intransitive(),
    libraryDependencies += "org.eclipse.jetty" % "jetty-server" % jettyVersion % "compile" intransitive(),
    libraryDependencies += "org.eclipse.jetty" % "jetty-io" % jettyVersion % "compile" intransitive(),
    libraryDependencies += "org.eclipse.jetty" % "jetty-http" % jettyVersion % "compile" intransitive(),
    libraryDependencies += "org.eclipse.jetty" % "jetty-security" % jettyVersion % "compile" intransitive(),
    libraryDependencies += "org.eclipse.jetty" % "jetty-continuation" % jettyVersion % "compile" intransitive(),
    libraryDependencies += "org.eclipse.jetty" % "jetty-xml" % jettyVersion % "compile" intransitive(),
    libraryDependencies += "org.eclipse.jetty" % "jetty-servlet" % jettyVersion % "compile" intransitive(),
    libraryDependencies += "org.eclipse.jetty" % "jetty-servlets" % jettyVersion % "compile" intransitive(),
    libraryDependencies += "org.eclipse.jetty.orbit" % "javax.servlet" % servletVersion % "compile" artifacts (Artifact("javax.servlet", "jar", "jar")) intransitive(),
    ivyXML :=
      <dependency org="org.eclipse.jetty.orbit" name="javax.servlet" rev={ servletVersion }>
        <artifact name="javax.servlet" type="orbit" ext="jar"/>
      </dependency>
  )


