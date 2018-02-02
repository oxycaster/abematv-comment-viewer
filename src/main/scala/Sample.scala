import scalafx.Includes._
import scalafx.application.JFXApp
import scalafx.scene.Scene
import scalafxml.core.{FXMLView, NoDependencyResolver}

object Sample extends JFXApp {

  val resource = getClass.getResource("/sample.fxml")
  val root = FXMLView(resource, NoDependencyResolver)

  stage = new JFXApp.PrimaryStage() {
    title = "AbemaTVコメントビューア"
    scene = new Scene(root)
  }
}
