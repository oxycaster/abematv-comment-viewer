import java.nio.file.{Files, Path}
import java.sql.Timestamp
import java.time.format.DateTimeFormatter
import javafx.scene.input.KeyCode

import collection.JavaConverters._
import scala.util.Try
import scalafx.Includes._
import scalafx.animation.{KeyFrame, Timeline}
import scalafx.application.JFXApp
import scalafx.beans.property.{ObjectProperty, StringProperty}
import scalafx.beans.value.ObservableValue
import scalafx.collections.ObservableBuffer
import scalafx.scene.control._
import scalafx.event.ActionEvent
import scalafx.scene.Scene
import scalafx.scene.image.{Image, ImageView}
import scalafx.scene.input.{KeyEvent, MouseEvent}
import scalafx.scene.layout.AnchorPane
import scalafx.stage.{DirectoryChooser, FileChooser, Modality, Stage}
import scalafx.stage.FileChooser.ExtensionFilter
import scalafx.util.{Duration, StringConverter}
import scalafxml.core.{FXMLView, NoDependencyResolver}
import scalafxml.core.macros.sfxml


trait UserMessageViewTableViewUpdate {
  def updateTableView(userRows: Seq[Comment])
}

@sfxml
class UserMessageViewController(private[this] val tableView1: TableView[Comment],

                                private[this] val columnUserId: TableColumn[Comment, String],
                                private[this] val columnCreatedAt: TableColumn[Comment, String],
                                private[this] val columnMessage: TableColumn[Comment, String],

                                private[this] val closeButton: Button) extends UserMessageViewTableViewUpdate {

  // カラム表示とオブジェクトプロパティのマッピング
  columnUserId.cellValueFactory = { x => new StringProperty(x.value.userId) }
  columnCreatedAt.cellValueFactory = { x =>
    val ts = new Timestamp(x.value.createdAtMs)
    new StringProperty(ts.toLocalDateTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")))
  }
  columnMessage.cellValueFactory = { x => new StringProperty(x.value.message) }


  closeButton.onMouseClicked = handle {
    closeButton.getScene.getWindow.asInstanceOf[javafx.stage.Stage].close()
  }

  def updateTableView(userRows: Seq[Comment]) = {
    tableView1.items = ObservableBuffer[Comment](userRows)
  }
}
