import Client.StringMessageFromServer
import OneToOneChatServer.Message
import akka.actor.{Actor, ActorRef}

class OneToOneChatServer(one: ActorRef, two: ActorRef) extends Actor {

  val memberOne: ActorRef = one
  val memberTwo: ActorRef = two
  private var messageNumber: Long = 0

  override def receive: Receive = {
    case Message(text) => sender() match {
      case `memberOne` => messageNumber += 1; memberTwo ! StringMessageFromServer(text, messageNumber)
      case `memberTwo` => messageNumber += 1; memberOne ! StringMessageFromServer(text, messageNumber)
    }
    case _ => println("unknown message")
  }

}

object OneToOneChatServer {

  case class Message(text:String)
  case class Attachment(payload:AttachmentContent)
}

class AttachmentContent {

}
