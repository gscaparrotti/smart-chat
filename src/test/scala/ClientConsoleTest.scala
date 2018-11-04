
import java.io.File

import Client._
import akka.actor.{ActorSystem, ExtendedActorSystem, PoisonPill, Props}
import akka.testkit.{ImplicitSender, TestActor, TestActorRef, TestKit, TestProbe}
import com.typesafe.config.ConfigFactory
import org.scalatest.{BeforeAndAfterAll, Matchers, WordSpecLike}

class ClientConsoleTest extends TestKit(ActorSystem("MySystem")) with ImplicitSender
  with WordSpecLike with Matchers with BeforeAndAfterAll{

  override def afterAll: Unit = {
    TestKit.shutdownActorSystem(system)
  }

  "A client when interacts with a console/view" must {

    implicit var system = ActorSystem.create("MySystem", ConfigFactory.parseFile(new File("src/main/scala/res/client.conf")))
    val client = system.actorOf(Props(new Client(system.asInstanceOf[ExtendedActorSystem])))
    system = ActorSystem.create("MySystem", ConfigFactory.parseFile(new File("src/main/scala/res/server.conf")))
    val server = system.actorOf(Props(new RegisterServer()), name = "server")

    "if receives userName from login view, sends a registration request to server" in {

      val logInconsoleA = TestProbe("logInConsoleA")
      val testActorA = TestProbe("testActorA")

      //not empty username: the username inserted by console is registered
      logInconsoleA.send(client,Client.LogInFromConsole("userA"))//JoinRequest("userA") sent to server
      logInconsoleA.expectNoMessage()
      testActorA.send(server,RegisterServer.JoinRequest("userB"))//to get user and group list, applicant must be registered
      testActorA.expectMsg(Client.AcceptRegistrationFromRegister(true))
      testActorA.send(server,RegisterServer.AllUsersAndGroupsRequest)
      var userList = testActorA.expectMsgPF()({
        case (UserAndGroupActive(userList,groupList))=> userList
      })
      assert(userList.contains(("userA")))

      //empty username: the username inserted by console is registered
      logInconsoleA.send(client,Client.LogInFromConsole(""))//JoinRequest("") sent to server
      logInconsoleA.expectNoMessage()
      testActorA.send(server,RegisterServer.AllUsersAndGroupsRequest)
      userList = testActorA.expectMsgPF()({
        case (UserAndGroupActive(userList,groupList))=> userList
      })
      assert(userList.length==2)
    }

    "if receives request from console to create a one to one chat" in {
      val logInconsoleB = TestProbe("logInConsoleB")
      val testActorB = TestProbe("testActorB")

      logInconsoleB.send(client,Client.LogInFromConsole("userC"))//JoinRequest("userC") sent to server
      logInconsoleB.expectNoMessage()
      testActorB.send(server,RegisterServer.JoinRequest("userD"))
      testActorB.expectMsg(Client.AcceptRegistrationFromRegister(true))
      logInconsoleB.send(client,Client.RequestForChatCreationFromConsole("userD"))
      logInconsoleB.expectNoMessage()
      //test if chat is created correctly
      logInconsoleB.send(server,RegisterServer.GetServerRef("userC"))
      val testchatServer = logInconsoleB.expectMsgPF()({
        case ResponseForServerRefRequest(serverOpt) if serverOpt.isDefined => serverOpt.get
      })
//      testActorB.send(testchatServer,OneToOneChatServer.Message("messageFromUserD"))
//      testActorB.expectMsg(Client.StringMessageFromServer("messageFromUserD",1,"userD"))

//      val otherlogInconsole = TestProbe("otherlogInConsole")
//      otherlogInconsole.send(client,Client.LogInFromConsole("userE"))//JoinRequest("userC") sent to server
//      otherlogInconsole.expectNoMessage()
//      logInconsoleB.send(client,Client.RequestForChatCreationFromConsole("userE")) //userA has't userC ref in local usersList
//      logInconsoleB.expectNoMessage()

//      logInconsole.send(server,RegisterServer.GetServerRef("userC"))
//      val otherTestchatServer = logInconsole.expectMsgPF()({
//        case ResponseForServerRefRequest(serverOpt) if serverOpt.isDefined => serverOpt.get
//      })
//      logInconsole.send(otherTestchatServer,OneToOneChatServer.Message("messageFromOtherTestActor"))
//      testActor.expectMsg(Client.StringMessageFromServer("messageFromOtherTestActor",1,"userA"))

    }

    "if receives message from client console, it checks if recipient ref is stored" in {

      val serverTest = TestProbe("serverRegister")

      //log in from console
//      val clientTestA = TestActorRef[Client](Props(new Client(system.asInstanceOf[ExtendedActorSystem])))
//      val clientConsoleA = TestProbe("logInConsoleA")
//      clientConsoleA.send(clientTestA,Client.LogInFromConsole("userA"))//server receives JoinRequest("userA")
//      clientConsoleA.expectNoMessage()
//      serverTest.send(clientTestA,Client.AcceptRegistrationFromRegister(true))
//      serverTest.expectMsg(RegisterServer.AllUsersAndGroupsRequest)
//
//      val clientConsoleB = TestProbe("logInConsoleB")
//      val clientTestB = TestActorRef[Client](Props(new Client(system.asInstanceOf[ExtendedActorSystem])))
//      clientConsoleB.send(clientTestB,Client.LogInFromConsole("userB"))//server receives JoinRequest("userB")
//      clientConsoleB.expectNoMessage()
//      serverTest.send(clientTestB,Client.AcceptRegistrationFromRegister(true))
//      serverTest.expectMsg(RegisterServer.AllUsersAndGroupsRequest)
//
//      //first message from clientA: recipient isn't stored in client map(friendName,chatServer)
//      clientConsoleA.send(clientTestA,RequestForChatCreationFromConsole("userA")) //server receives AllUsersAndGroupsRequest and NewOneToOneChatRequest
//      clientConsoleA.expectNoMessage()
    }

//    "if receives an attachment from client console" in {
//      val clientTest = TestActorRef[Client](Props(new Client(system.asInstanceOf[ExtendedActorSystem])))
//      clientTest.tell(AttachmentMessageFromConsole,self)
//      //An attachment is sent to ChatServer
//      expectNoMessage()
//    }

//    "if receives a request from console of creating a new chat group and forward it to register" in {
//      val clientTest = TestActorRef[Client](Props(new Client(system.asInstanceOf[ExtendedActorSystem])))
//      clientTest.tell(CreateGroupRequestFromConsole("groupName"),self)
//      expectNoMessage()
//      //expectMsgClass(classOf[NewGroupChatRequest])
//    }

//    "if receives a request from console of joining to an existing chat group and forward it to register" in {
//      val groupName = "TestNameGroup"
//      val clientTest = TestActorRef[Client](Props(new Client(system.asInstanceOf[ExtendedActorSystem])))
//      clientTest.tell(JoinGroupRequestFromConsole(groupName),self)
//      expectNoMessage()
//      //expectMsgClass(JoinGroupChatRequest(groupName).getClass)
//    }
  }
}
