package com.knoldus.inc.entities

import play.api.libs.json.{Json, OFormat}

case class Player(name:String, team:String, score: Int)

object Player {
  implicit val Player: OFormat[Player] = Json.format[Player]
}