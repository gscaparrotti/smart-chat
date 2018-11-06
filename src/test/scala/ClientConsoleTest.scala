
import java.io.File

import Client._
import RegisterServer._
import akka.actor.{ActorRef, ActorSystem, ExtendedActorSystem, PoisonPill, Props}
import akka.testkit.{ImplicitSender, TestActor, TestActorRef, TestKit, TestProbe}
import com.typesafe.config.ConfigFactory
import org.scalatest.{BeforeAndAfterAll, Matchers, WordSpecLike}

class ClientConsoleTest extends TestKit(ActorSystem.create("MySystem", ConfigFactory.parseFile(new File("src/main/scala/res/server.conf")))) with ImplicitSender
  with WordSpecLike with Matchers with BeforeAndAfterAll{

  override def afterAll: Unit = {
    TestKit.shutdownActorSystem(system)
  }

  "A client when interacts with a console/view" must {

    "if receives request from console to create a one to one chat" in {

      val server = system.actorOf(Props(new RegisterServer()), name = "server")
      val userA = system.actorOf(Props(new Client(system.asInstanceOf[ExtendedActorSystem])), name = "userA")
      val userB = system.actorOf(Props(new Client(system.asInstanceOf[ExtendedActorSystem])), name = "userB")

      server.tell(JoinRequest("actor"), this.testActor)
      expectMsg(AcceptRegistrationFromRegister(true))
      userA.tell(Client.LogInFromConsole("userA"), this.testActor)
      userB.tell(Client.LogInFromConsole("userB"), this.testActor)
      var present = false
      while(!present) {
        server.tell(AllUsersAndGroupsRequest, this.testActor)
        val users = expectMsgPF()({
          case UserAndGroupActive(users, _) => users
        })
        if(users.size == 3) {
          present = true
        }
      }
      userA.tell(Client.RequestForChatCreationFromConsole("userB"), this.testActor)
      server.tell(GetServerRef("userB"), sender = userA)
    }
  }
}
