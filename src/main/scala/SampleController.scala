import java.net.{HttpURLConnection, URL}
import java.nio.file.{Files, Path, Paths}
import java.sql.Timestamp
import java.time.format.DateTimeFormatter
import javafx.scene.Parent
import javafx.scene.input.KeyCode

import net.sf.javavp8decoder.imageio.WebPImageReader
import net.sf.javavp8decoder.imageio.WebPImageReaderSpi
import java.io._
import javax.imageio.stream.FileImageInputStream

import collection.JavaConverters._
import scala.util.Try
import scalafx.Includes._
import scalafx.animation.{KeyFrame, Timeline}
import scalafx.application.JFXApp
import scalafx.beans.property.{ObjectProperty, StringProperty}
import scalafx.beans.value.ObservableValue
import scalafx.collections.ObservableBuffer
import scalafx.embed.swing.SwingFXUtils
import scalafx.scene.control._
import scalafx.event.ActionEvent
import scalafx.scene.Scene
import scalafx.scene.image.{Image, ImageView}
import scalafx.scene.input.{KeyEvent, MouseEvent}
import scalafx.scene.layout.AnchorPane
import scalafx.scene.text.Text
import scalafx.stage.{DirectoryChooser, FileChooser, Modality, Stage}
import scalafx.stage.FileChooser.ExtensionFilter
import scalafx.util.{Duration, StringConverter}
import scalafxml.core.{FXMLLoader, FXMLView, NoDependencyResolver}
import scalafxml.core.macros.sfxml

@sfxml
class SampleController(private[this] val label1: Label,
                       private[this] val text1: Text,
                       private[this] val imageView1: ImageView,

                       private[this] val tableView1: TableView[Comment],

                       private[this] val columnId: TableColumn[Comment, String],
                       private[this] val columnUserId: TableColumn[Comment, String],
                       private[this] val columnCreatedAt: TableColumn[Comment, String],
                       private[this] val columnMessage: TableColumn[Comment, String],

                       private[this] val choiceBox1: ChoiceBox[ChannelResponse],
                       private[this] val textField1: TextField) {

  val applicationKey = "v+Gjs=25Aw5erR!J8ZuvRrCx*rGswhB&qdHd_SYerEWdU&a?3DzN9BRbp5KwY4hEmcj5#fykMjJ=AuWz5GSMY-d@H7DMEh3M@9n2G552Us$$k9cD=3TxwWe86!x#Zyhe"

  // チャンネル一覧を取得してくる
  val channels: Seq[ChannelResponse] = AbemaTVApi.getChannels().channels

  choiceBox1.items = ObservableBuffer[ChannelResponse](channels)

  choiceBox1.converter_=(new StringConverter[ChannelResponse] {
    override def toString(t: ChannelResponse): String = t.name
    override def fromString(string: String): ChannelResponse = channels.filter(_.name == string).head
  })

  // チャンネルセレクタが変更される度に行う処理
  choiceBox1.value.onChange {
    val selectedItem = choiceBox1.value.value

    val api = AbemaTVApi(applicationKey)
    val slot = api.getChannelSlot(choiceBox1.value.value.id)
    label1.text.value = slot.title
    text1.text.value = slot.content
    imageView1.image.value = getWebPImageFromURL(s"https://hayabusa.io/abema/programs/${slot.displayProgramId}/thumb001.q85.w136.h77.v1509448694.webp?width=136&height=77")

    println(selectedItem)
    tableView1.items_=(ObservableBuffer[Comment](Seq.empty[Comment]))
  }

  // 一個のチャンネルを選択する
  choiceBox1.selectionModel().selectFirst()

  // カラム表示とオブジェクトプロパティのマッピング
  columnId.cellValueFactory = { x => new StringProperty(x.value.id) }
  columnUserId.cellValueFactory = { x => new StringProperty(x.value.userId) }
  columnMessage.cellValueFactory = { x => new StringProperty(x.value.message) }
  columnCreatedAt.cellValueFactory = { x =>
    val ts = new Timestamp(x.value.createdAtMs)
    new StringProperty(ts.toLocalDateTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")))
  }

  // 行が選択された時に行う処理
  tableView1.selectionModel().selectedItemProperty().addListener {
    (observable, oldValue, newValue) =>
      val userRows = tableView1.getItems.filter(x => x.userId == newValue.userId)

      val resource = getClass.getResource("/usermessage.fxml")

      // Instead of FXMLView, we create a new ScalaFXML loader
      val loader = new FXMLLoader(resource, NoDependencyResolver)
      loader.load()

      val root = loader.getRoot[Parent] // javafx.scene.Parent

      val controller = loader.getController[UserMessageViewTableViewUpdate]
      controller.updateTableView(userRows.toSeq)

      val newStage = new Stage(){
        scene = new Scene(root)
      }
      //newStage.initModality(Modality.ApplicationModal)
      newStage.show()
      userRows.foreach(println)
  }

  // コメント投稿処理
  textField1.onKeyPressed = (e: KeyEvent) => {
    if (e.getCode == KeyCode.ENTER){
      if (!textField1.text.value.isEmpty) {
        Try {
          textField1.disable_=(true)
          val api = AbemaTVApi(applicationKey)
          val slotId = api.getChannelSlotId(choiceBox1.value.value.id)
          api.sendComment(slotId, textField1.text.value)
          textField1.text_=("")
          textField1.disable_=(false)
        }.recover {
          case e: Throwable => {
            textField1.text_=("")
            textField1.disable_=(false)
          }
        }
      }
    }
  }

  // 一定周期で選択中のチャンネルのコメントを取得してきて更新
  val keyFrame = KeyFrame(10000 ms, onFinished = {
    event: ActionEvent =>
      val api = AbemaTVApi(applicationKey)
      val slot = api.getChannelSlot(choiceBox1.value.value.id)
      val comments = api.getChannelComments(slot.id).comments.sortBy(_.createdAtMs)

      label1.text.value = slot.title
      text1.text.value = slot.content
      imageView1.image.value = getWebPImageFromURL(s"https://hayabusa.io/abema/programs/${slot.displayProgramId}/thumb001.q85.w136.h77.v1509448694.webp?width=136&height=77")

      val newComments = comments.filterNot(nx => tableView1.getItems.exists(x => x.id == nx.id) )
      tableView1.getItems.appendAll(newComments)
  })

  val timeline = new Timeline {
    keyFrames = Seq(keyFrame)
    cycleCount = Timeline.Indefinite
  }
  timeline.play()

  def download(url: String, path: Path) = {
    val stream = new URL(url).openStream
    val bytes = Stream.continually(stream.read).takeWhile( -1 != ).map(_.byteValue).toArray
    val bw = new BufferedOutputStream(new FileOutputStream(path.toFile))
    bw.write(bytes)
    bw.close()
  }

  def getWebPImageFromURL(url: String) = {
    download(url, Paths.get("thumbnail.webp"))
    val fiis = new FileImageInputStream(Paths.get("thumbnail.webp").toFile)
    val reader = new WebPImageReader(new WebPImageReaderSpi())
    reader.setInput(fiis)
    val image = reader.read(0, null)
    fiis.close()

    SwingFXUtils.toFXImage(image, null)
  }
}
