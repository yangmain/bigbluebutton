package org.bigbluebutton.client.meeting

import akka.actor.{Actor, ActorLogging, Props}
import org.bigbluebutton.client.bus._
import org.bigbluebutton.common2.messages.BbbCoreWithEvelopeMsg

object MeetingActor {
  def props(meetingId: String, msgToAkkaAppsEventBus: MsgToAkkaAppsEventBus,
            msgToClientEventBus: MsgToClientEventBus): Props =
  Props(classOf[MeetingActor], meetingId, msgToAkkaAppsEventBus, msgToClientEventBus)
}

class MeetingActor(val meetingId: String, msgToAkkaAppsEventBus: MsgToAkkaAppsEventBus,
                   msgToClientEventBus: MsgToClientEventBus) extends Actor with ActorLogging {

  private val userMgr = new UsersManager

  def receive = {
    case msg: ConnectMsg => handleConnectMsg(msg)
    case msg: DisconnectMsg => handleDisconnectMsg(msg)
    case msg: MsgFromClientMsg => handleMsgFromClientMsg(msg)
    case msg: BbbCoreWithEvelopeMsg => handleBbbServerMsg(msg)
      // TODO: Should keep track of user lifecycle so we can remove when user leaves the meeting.
  }

  private def createUser(id: String): User = {
    User(id, msgToAkkaAppsEventBus, meetingId, msgToClientEventBus)
  }

  def handleConnectMsg(msg: ConnectMsg): Unit = {
    UsersManager.findWithId(userMgr, msg.connInfo.userId) match {
      case Some(m) => m.actorRef forward(msg)
      case None =>
        val m = createUser(msg.connInfo.userId)
        UsersManager.add(userMgr, m)
        m.actorRef forward(msg)
    }
  }

  def handleDisconnectMsg(msg: DisconnectMsg): Unit = {
    for {
      m <- UsersManager.findWithId(userMgr, msg.connInfo.meetingId)
    } yield {
      m.actorRef forward(msg)
    }
  }

  def handleMsgFromClientMsg(msg: MsgFromClientMsg):Unit = {
    for {
      m <- UsersManager.findWithId(userMgr, msg.connInfo.meetingId)
    } yield {
      m.actorRef forward(msg)
    }
  }

  def handleBbbServerMsg(msg: BbbCoreWithEvelopeMsg): Unit = {
    for {
      msgType <- msg.envelope.routing.get("msgType")
    } yield {
      handleServerMsg(msgType, msg)
    }
  }

  def handleServerMsg(msgType: String, msg: BbbCoreWithEvelopeMsg): Unit = {
    msgType match {
      case "direct" => handleDirectMessage(msg)
      case "broadcast" => handleBroadcastMessage(msg)
      case "system" => handleSystemMessage(msg)
    }
  }

  private def forwardToUser(msg: BbbCoreWithEvelopeMsg): Unit = {
    for {
      userId <- msg.envelope.routing.get("userId")
      m <- UsersManager.findWithId(userMgr, userId)
    } yield {
      m.actorRef forward(msg)
    }
  }

  def handleDirectMessage(msg: BbbCoreWithEvelopeMsg): Unit = {
    // In case we want to handle specific messages. We can do it here.
    forwardToUser(msg)
  }

  def handleBroadcastMessage(msg: BbbCoreWithEvelopeMsg): Unit = {
    // In case we want to handle specific messages. We can do it here.
    forwardToUser(msg)
  }

  def handleSystemMessage(msg: BbbCoreWithEvelopeMsg): Unit = {
    // In case we want to handle specific messages. We can do it here.
    forwardToUser(msg)
  }
}
