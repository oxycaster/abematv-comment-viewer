import java.sql.Timestamp
import java.time.format.DateTimeFormatter
import java.time.{ZoneId, ZonedDateTime}
import java.util.Base64
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

import AbemaTVApi.{ApplicationKey, DeviceID, SlotID}
import dispatch.Defaults._
import dispatch._
import org.json4s.DefaultFormats
import org.json4s.JsonAST.JNull
import org.json4s.native.JsonMethods

import scala.util.Random


case class Profile(userId: String, createdAt: Long)
case class UserProfile(profile: Profile, token: String)
case class MediaStatus(dashISOFF: Option[Boolean])
case class Channel(id: String,  name: String, order: Int, mediaStatus: MediaStatus)
case class SlotGroup(id: Option[String],
                     name: Option[String],
                     lastSlotId: Option[String])

case class SharedLink(twitter: String,
                      facebook: String,
                      google: String,
                      line: String,
                      copy: String,
                      screen: String)

case class Episode(sequence: Long)
case class Credit(casts: Seq[String], crews: Seq[String], copyrights: Seq[String])
case class Series(id: String,
                  //themeColor: String,
                  updatedAt: Long)
case class ProvidedInfo(thumbImg: String, updatedAt: Long)
case class Program(id: String, episode: Episode, credit: Credit, series: Series, providedInfo: ProvidedInfo)

case class Slot(id: String,
                title: String,
                startAt: Long,
                endAt: Long,
                programs: Seq[Program],
                tableStartAt: Long,
                tableEndAt: Long,
                highlight: String,
                tableHighlight: Option[String],
                detailHighlight: Option[String],
                content: String,
                displayProgramId: String,
                // mark: {},
                // flags: {},
                channelId: String,
                slotGroup: SlotGroup,
                links: Option[String],
                sharedLink: SharedLink,
                externalContent: Option[String]
               )

case class ChannelSchedule(channelId: String, date: String, slots: Seq[Slot])

case class Media(channels: Seq[Channel],
                 channelSchedules: Seq[ChannelSchedule],
                 availableDates: Seq[String],
                 version: String)

case class Comment(id: String,
                   message: String,
                   createdAtMs: Long,
                   userId: String)

case class Comments(comments: Seq[Comment], count: Long)


case class SendCommentResponse(id: String, createdAtMs: Long)

case class Playback(hls: Option[String], dash: Option[String])
case class ChannelResponse(id: String, name: String, playback: Playback)
case class ChannelsResponse(channels: Seq[ChannelResponse])

class AbemaTVApi(val applicationKey: ApplicationKey,
                 val deviceID: DeviceID) {

  private[this] val secretKey: String = AbemaTVApi.createApplicationKeySecret(applicationKey, deviceID)
  val userProfile: UserProfile = AbemaTVApi.getUserProfile(deviceID, secretKey)

  def getChannelSlot(channelId: String): Slot = AbemaTVApi.getChannelSlot(userProfile.token, channelId)

  def getChannelSlotId(channelId: String): SlotID = AbemaTVApi.getChannelSlotId(userProfile.token, channelId)

  def getChannelComments(slotId: SlotID): Comments = AbemaTVApi.getChannelComments(userProfile.token, slotId)

  def sendComment(slotId: SlotID, message: String): SendCommentResponse = AbemaTVApi.sendComment(userProfile.token, slotId, message)

}

object AbemaTVApi {
  type ApplicationKey = String
  type DeviceID = String
  type SlotID = String

  def apply(applicationKey: ApplicationKey): AbemaTVApi = new AbemaTVApi(applicationKey, generateDeviceId())

  def apply(applicationKey: ApplicationKey, deviceID: DeviceID): AbemaTVApi = new AbemaTVApi(applicationKey, deviceID)

  private def createApplicationKeySecret(appKey: String, d: DeviceID) = s(appKey, d, o())

  private[this] def generateDeviceId(): DeviceID = {
    var n = ""
    (0 until 32).foreach(r => {
      val e = (16 * Random.nextFloat()).toInt | 0
      r > 4 && r < 21 && (r % 4 == 0) && ((n += "-") == true)

      val a = if (12 == r) {
        4
      } else {
        if (16 == r) {
          3 & e | 8
        } else {
          e
        }
      }

      n += a.toHexString
    })
    n
  }

  private[this] def i(e: Mac): String = {
    val t = Base64.getEncoder.encodeToString(e.doFinal())
    t.split('=').mkString("").split('+').mkString("-").split('/').mkString("_")
  }

  private[this] def a(e: ZonedDateTime): Double = scala.math.floor(Timestamp.from(e.toInstant).getTime / 1e3)

  private[this] def s(e: ApplicationKey, t: DeviceID, n: ZonedDateTime): String = {
    val algo = "HmacSHA256"
    val r = Mac.getInstance(algo)
    r.init(new SecretKeySpec(e.getBytes, algo))
    val o = n.getMonthValue
    //val s = 0

    r.update(e.getBytes)
    (0 until o).foreach { x =>
      val l = r.doFinal()
      r.reset()
      r.update(l)
    }

    var s = i(r)
    r.reset()
    r.update((s + t).getBytes())

    val d = n.getDayOfMonth % 5
    (0 until d).foreach { x =>
      val h = r.doFinal()
      r.reset()
      r.update(h)
    }
    s = i(r)
    r.reset()
    r.update((s + a(n).toLong.toString).getBytes())

    val m = n.getHour % 5
    (0 until m).foreach { x =>
      val v = r.doFinal()
      r.reset()
      r.update(v)
    }
    i(r)
  }

  private[this] def o(): ZonedDateTime = {
    val e = ZonedDateTime.now()
      .withZoneSameInstant(ZoneId.of("UTC"))

    e.plusHours(1)
      .withMinute(0)
      .withSecond(0)
  }

  private def getUserProfile(deviceID: DeviceID, secretKey: String): UserProfile = {
    import org.json4s.JsonDSL._
    implicit val formats = DefaultFormats

    val sendJson = JsonMethods.render(("deviceId" -> deviceID) ~ ("applicationKeySecret" -> secretKey))
    val sendJsonStr = JsonMethods.compact(sendJson)

    val request = (host("api.abema.io").secure / "v1" / "users")
      .POST
      .setBody(sendJsonStr)
      .setHeader("Content-Type", "application/json")
    val client = new Http(dispatch.Http.defaultClientBuilder)
    val res = client(request OK as.json4s.Json).apply().camelizeKeys.extract[UserProfile]
    client.shutdown()
    res
  }

  private def getChannelSlot(token: String, channelId: String) = {
    import org.json4s.JsonDSL._
    implicit val formats = DefaultFormats

    val from = ZonedDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"))
    val to = ZonedDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"))

    val request = (host("api.abema.io").secure / "v1" / "media")
      .GET
      .addQueryParameter("dateFrom", from)
      .addQueryParameter("dateTo", to)
      .addHeader("Accept-Encoding", "gzip")
      .addHeader("Content-Type", "application/json")
      .addHeader("authorization", "bearer " + token)

    val client = new Http(dispatch.Http.defaultClientBuilder)
    val res = client(request OK as.json4s.Json).apply().camelizeKeys.extract[Media]
    client.shutdown()

    val now = ZonedDateTime.now().withZoneSameInstant(ZoneId.of("UTC"))
    val t = scala.math.floor(Timestamp.from(now.toInstant).getTime / 1000).toLong

    res.channelSchedules
      .filter(x => x.channelId == channelId)
      .flatMap{ x =>
        x.slots.find(x => (x.startAt <= t) && (t <= x.endAt))
      }.head
  }

  private def getChannelSlotId(token: String, channelId: String) = getChannelSlot(token, channelId).id

  private def getChannelComments(token: String, slotId: SlotID) = {
    import org.json4s.JsonDSL._
    implicit val formats = DefaultFormats

    val request = (host("api.abema.io").secure / "v1" / "slots" / slotId / "comments")
      .GET
      .addQueryParameter("limit", 100.toString)
      .addHeader("Accept-Encoding", "gzip")
      .addHeader("Content-Type", "application/json")
      .addHeader("authorization", "bearer " + token)
    val client = new Http(dispatch.Http.defaultClientBuilder)
    val res = client(request OK as.json4s.Json).apply().camelizeKeys.extract[Comments]
    client.shutdown()
    res
  }

  private def sendComment(token: String, slotId: SlotID, message: String) = {
    import org.json4s.JsonDSL._
    implicit val formats = DefaultFormats

    val sendJson = JsonMethods.render(("message" -> message) ~ ("share" -> JNull))
    val sendJsonStr = JsonMethods.compact(sendJson)

    val request = (host("api.abema.io").secure / "v1" / "slots" / slotId / "comments")
      .POST
      .setBody(sendJsonStr)
      .addHeader("Accept-Encoding", "gzip")
      .addHeader("Content-Type", "application/json")
      .addHeader("authorization", "bearer " + token)
    val client = new Http(dispatch.Http.defaultClientBuilder)
    val res = client(request OK as.json4s.Json).apply().camelizeKeys.extract[SendCommentResponse]
    client.shutdown()
    res
  }

  def getChannels() = {
    import org.json4s.JsonDSL._
    implicit val formats = DefaultFormats

    val request = (host("api.abema.io").secure / "v1" / "channels")
      .GET
      .addHeader("Accept-Encoding", "gzip")
      .addHeader("Content-Type", "application/json")

    val client = new Http(dispatch.Http.defaultClientBuilder)
    val res = client(request OK as.json4s.Json).apply().camelizeKeys.extract[ChannelsResponse]
    client.shutdown()
    res
  }
}
