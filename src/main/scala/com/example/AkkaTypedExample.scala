package com.example

import akka.NotUsed
import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ ActorRef, ActorSystem, Behavior, DispatcherSelector, Terminated }

object AkkaTypedExample extends App{
  val system: ActorSystem[HelloWorldMain.Start] =
    ActorSystem(HelloWorldMain.main, "hello")

  system ! HelloWorldMain.Start("World")
  system ! HelloWorldMain.Start("Akka")
}

object HelloWorld {
  final case class Greet(whom: String, replyTo: ActorRef[Greeted])
  trait Event
  final case class Greeted(whom: String, from: ActorRef[Greet]) extends Event

  val greeter: Behavior[Greet] = Behaviors.receive { (context, message) ⇒
    context.log.info("Hello {}!", message.whom)
    message.replyTo ! Greeted(message.whom, context.self)
    Behaviors.same
  }
}

object HelloWorldBot {

  def bot(greetingCounter: Int, max: Int): Behavior[HelloWorld.Greeted] =
    Behaviors.receive { (context, message) ⇒
      val n = greetingCounter + 1
      context.log.info("Greeting {} for {}", n, message.whom)
      if (n == max) {
        Behaviors.stopped
      } else {
        message.from ! HelloWorld.Greet(message.whom, context.self)
        bot(n, max)
      }
    }
}

object HelloWorldMain {

  final case class Start(name: String)

  val main: Behavior[Start] =
    Behaviors.setup { context ⇒
      val greeter = context.spawn(HelloWorld.greeter, "greeter")

      Behaviors.receiveMessage { message ⇒
        val replyTo = context.spawn(HelloWorldBot.bot(greetingCounter = 0, max = 3), message.name)
        greeter ! HelloWorld.Greet(message.name, replyTo)
        Behaviors.same
      }
    }
}
