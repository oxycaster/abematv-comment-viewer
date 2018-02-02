name := "abematv-comment-viewer"
version := "0.1"
scalaVersion := "2.12.4"
maintainer := "oxycaster"
packageSummary := "package summary."
packageDescription := "package description."

// enablePlugins(JavaAppPackaging)
enablePlugins(JDKPackagerPlugin)

// ScalaFX + FXML
libraryDependencies += "org.scalafx" % "scalafx_2.12" % "8.0.144-R12"
libraryDependencies += "org.scalafx" %% "scalafxml-core-sfx8" % "0.4"
addCompilerPlugin("org.scalamacros" % "paradise" % "2.1.0" cross CrossVersion.full)

libraryDependencies += "ch.qos.logback" % "logback-classic" % "1.2.3"
libraryDependencies += "org.dispatchhttp" %% "dispatch-core" % "0.14.0-RC1"
libraryDependencies += "org.dispatchhttp" %% "dispatch-json4s-native" % "0.14.0-RC1"


// javafxのjarを引き込む
val jfxrtJar = file(System.getenv("JAVA_HOME") + "/jre/lib/ext/jfxrt.jar")
unmanagedJars in Compile += Attributed.blank(jfxrtJar)

// // アイコンファイルをOSごとに変更する
// lazy val iconGlob = sys.props("os.name").toLowerCase match {
//   case os if os.contains("mac") ⇒ "*.icns"
//   case os if os.contains("win") ⇒ "*.ico"
//   case _ ⇒ "*.png"
// }
// jdkAppIcon := (sourceDirectory.value ** iconGlob).getPaths.headOption.map(file)

jdkPackagerType := "all"
jdkPackagerJVMArgs := Seq("-Xmx1g", "-Dconfig.file=./production.conf", "-Dconfig.trace=loads", "-Xdock:name=AbemaCommentViewer")
jdkPackagerProperties := Map("app.name" -> name.value, "app.version" -> version.value)
jdkPackagerAppArgs := Seq(maintainer.value, packageSummary.value, packageDescription.value)

mappings in Universal += {
  file("production.conf") -> "production.conf"
}

fork := true
fork in run := true
connectInput in run := true
exportJars := true
