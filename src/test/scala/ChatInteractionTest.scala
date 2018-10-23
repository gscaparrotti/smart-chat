import Client.{AcceptRegistrationFromRegister, ResponseForChatCreation, StringMessageFromServer, UserAndGroupActive}
import OneToOneChatServer.Message
import RegisterServer.{JoinGroupChatRequest, JoinRequest}
import akka.actor.{Actor, ActorSystem, Props}
import akka.testkit.{ImplicitSender, TestKit, TestProbe}
import org.scalatest.{BeforeAndAfterAll, Matchers, WordSpecLike}

class ChatInteractionTest extends TestKit(ActorSystem("MySpec")) with ImplicitSender
  with WordSpecLike with Matchers with BeforeAndAfterAll{

  override def afterAll: Unit = {
    TestKit.shutdownActorSystem(system)
  }

  "Server when interacts with clients" must {

    "accept clients request to register themselves and give back users list and chat group list" in {
      val clientOne = TestProbe("clientOne")
      val clientTwo = TestProbe("clientTwo")
      val server = system.actorOf(Props[RegisterServer], name = "welcomeServer1")
      //ClientOne registration with username
      clientOne.send(server, RegisterServer.JoinRequest("clientOne"))
      clientOne.expectMsg(AcceptRegistrationFromRegister(true))
      clientOne.send(server, RegisterServer.AllUsersAndGroupsRequest)
      clientOne.expectMsg(UserAndGroupActive(List("clientOne"), List()))

      //ClientTwo registration with username
      clientTwo.send(server, RegisterServer.JoinRequest("clientTwo"))
      clientTwo.expectMsg(AcceptRegistrationFromRegister(true))
      clientTwo.send(server, RegisterServer.AllUsersAndGroupsRequest)
      clientTwo.expectMsg(UserAndGroupActive(List("clientOne", "clientTwo"), List()))
    }

    "accept a client request to create an oneToOneChat, accept the request of a second Client to join to it and then exchange messages between them" in {
      val clientOne = TestProbe("clientOne")
      val clientTwo = TestProbe("clientTwo")
      val server = system.actorOf(Props[RegisterServer], name = "welcomeServer2")

      clientOne.send(server, RegisterServer.JoinRequest("clientOne"))
      clientOne.expectMsg(AcceptRegistrationFromRegister(true))
      clientTwo.send(server, RegisterServer.JoinRequest("clientTwo"))
      clientTwo.expectMsg(AcceptRegistrationFromRegister(true))

      //Request from ClientOne to create a oneToOne chat with ClientTwo
      clientOne.send(server, RegisterServer.NewOneToOneChatRequest("clientTwo"))
      val testchatServer = clientOne.expectMsgPF()({
        case ResponseForChatCreation(true, oneToOneServer) if oneToOneServer.isDefined => oneToOneServer.get
      })

      //Start chat bewtween two users
      val firstMessageText = "msgFromClientOne"
      clientOne.send(testchatServer, Message(firstMessageText))
      val firstMessageIndex = clientTwo.expectMsgPF()({
        case StringMessageFromServer(`firstMessageText`, messageNumber) => messageNumber
      })
      clientOne.expectMsgPF()({
        case StringMessageFromServer(`firstMessageText`, `firstMessageIndex`) => Unit
      })

      val secondMessageText = "msgFromClientTwo"
      val secondMessageIndex = firstMessageIndex + 1
      clientTwo.send(testchatServer, Message(secondMessageText))
      clientOne.expectMsgPF()({
        case StringMessageFromServer(`secondMessageText`, `secondMessageIndex`) => Unit
      })
      clientTwo.expectMsgPF()({
        case StringMessageFromServer(`secondMessageText`, `secondMessageIndex`) => Unit
      })
    }

    "accept a client request to create a chat group and a second client request to join to it" in {
      val clientOne = TestProbe("clientOne")
      val clientTwo = TestProbe("clientTwo")
      val server = system.actorOf(Props[RegisterServer], name = "welcomeServer3")

      clientOne.send(server, RegisterServer.JoinRequest("clientOne"))
      clientOne.expectMsg(AcceptRegistrationFromRegister(true))
      clientTwo.send(server, RegisterServer.JoinRequest("clientTwo"))
      clientTwo.expectMsg(AcceptRegistrationFromRegister(true))

      //Request from ClientOne to create a chatGroup
      clientOne.send(server, RegisterServer.NewGroupChatRequest("chatGroupName"))
      clientOne.expectMsgPF()({
        case ResponseForChatCreation(true, actor) if actor.isDefined => Unit
      })

      //Request from ClientOne to join to chatGroup named "chatGroupName"
      clientOne.send(server, RegisterServer.JoinGroupChatRequest("chatGroupName"))
      clientOne.expectMsgPF()({
        case ResponseForChatCreation(true, chatGroupServer) if chatGroupServer.isDefined => Unit
      })

      //Request from ClientTwo to join to chatGroup named "chatGroupName"
      clientTwo.send(server, RegisterServer.JoinGroupChatRequest("chatGroupName"))
      clientTwo.expectMsgPF()({
        case ResponseForChatCreation(true, chatGroupServer) if chatGroupServer.isDefined => Unit
      })
    }

    "refuse a client request, without user name, to register to server" in {
      val clientWithoutUserName = TestProbe("clientWithoutUserName")
      val server = system.actorOf(Props[RegisterServer], name = "welcomeServer4")
      clientWithoutUserName.send(server, RegisterServer.JoinRequest(null))
      clientWithoutUserName.expectMsg(AcceptRegistrationFromRegister(false))
      clientWithoutUserName.send(server, RegisterServer.JoinRequest(""))
      clientWithoutUserName.expectMsg(AcceptRegistrationFromRegister(false))
    }

    "refuse a client request to create an oneToOne chat with an unregistered user" in {
      val clientOne = TestProbe("clientOne")
      val server = system.actorOf(Props[RegisterServer], name = "welcomeServer6")
      clientOne.send(server, RegisterServer.JoinRequest("clientOne"))
      clientOne.expectMsg(AcceptRegistrationFromRegister(true))
      //ClientOne create a oneToOnechatGroup with an inexistent user
      clientOne.send(server, RegisterServer.NewOneToOneChatRequest("unregisteredUser"))
      clientOne.expectMsgPF()({
        case ResponseForChatCreation(false, oneToOneServer) => Unit
      })
    }

    "refuse a client request to create a group chat with a name already used" in {
      val clientOne = TestProbe("clientOne")
      val server = system.actorOf(Props[RegisterServer], name = "welcomeServer7")
      clientOne.send(server, RegisterServer.JoinRequest("clientOne"))
      clientOne.expectMsg(AcceptRegistrationFromRegister(true))
      //ClientOne create a chatGroup with same group name
      clientOne.send(server, RegisterServer.NewGroupChatRequest("chatGroupName"))
      clientOne.expectMsgPF()({
        case ResponseForChatCreation(true, chatserver) if chatserver.isDefined => Unit
      })
      clientOne.send(server, RegisterServer.NewGroupChatRequest("chatGroupName"))
      clientOne.expectMsgPF()({
        case ResponseForChatCreation(false, chatserver) if chatserver.isEmpty => Unit
      })
    }

    "refuse a client request to join to a chat group with an inexisting name" in {
      val clientOne = TestProbe("clientOne")
      val server = system.actorOf(Props[RegisterServer], name = "welcomeServer8")
      clientOne.send(server, RegisterServer.JoinRequest("clientOne"))
      clientOne.expectMsg(AcceptRegistrationFromRegister(true))
      //ClientOne request to join to an inexistent chat group
      clientOne.send(server, RegisterServer.JoinGroupChatRequest("inexistentChatGroup"))
      clientOne.expectMsgPF()({
        case ResponseForChatCreation(false, chatGroupServer) => Unit
      })

    }
  }
}
