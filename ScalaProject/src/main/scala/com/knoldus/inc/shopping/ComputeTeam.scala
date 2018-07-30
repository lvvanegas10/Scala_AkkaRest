package com.knoldus.inc.shopping

import akka.http.caching.scaladsl.Cache
import akka.stream.Materializer
import com.knoldus.inc.entities._
import scala.concurrent.{ExecutionContext}
import scala.collection.mutable.HashMap
import scala.concurrent.duration._
import scala.concurrent.{Await, Future}



/**
  *
  * @param cache
  * @param executionContext
  * @param materializer
  */
class ComputeTeam(cache: Cache[String, Float], hashMapParam: HashMap[String,List[Player]])(implicit val executionContext: ExecutionContext, implicit val materializer: Materializer) {

  var mapBuffer: HashMap[String,List[Player]] = hashMapParam

  def calculateTeamScore(players:List[Player]): Future[Float] = {
    var total: Float = 0
    var num: Int = 0
    for (player <- players) {
      total += player.score
      num += 1
    }
    Future.successful(total/num)
  }

  def playerExist(name:String, players: List[Player]): Boolean ={
    val list = players.filter(_.name == name)
    if(list ==Nil)
      false
    else
      true
  }

  def teamExists(name:String): Boolean ={
    val exists = mapBuffer.getOrElse(name, null)
    if(exists ==null)
      false
    else
      true
  }

  def addPlayer(player: Player): Player ={
    var list = List[Player]()
    var resp = player
    if (mapBuffer.getOrElse(player.team, null) == null){
        list = player :: Nil
    }
    else{
      if(playerExist(player.name, mapBuffer(player.team)))
        resp = null
      else
        list = player:: mapBuffer(player.team)
    }

    if(resp!= null){
      mapBuffer += (player.team -> list)
      cache.remove(player.team)
      cache.getOrLoad(player.team, _ => calculateTeamScore(list))
    }
    resp
  }

  def putPlayer(player:Player):Player ={
    var list: List[Player] = mapBuffer.getOrElse(player.team, null)
    var resp = player
    if(list== null)
      resp = null
    else if(playerExist(player.name, list)){
      list = list.filter(_.name != player.name)
      list = player :: list
      cache.remove(player.team)
      cache.getOrLoad(player.team, _ => calculateTeamScore(list))
    }
    else
      resp = null
    resp
  }

  def delPlayer(player:Player):Player ={
    var list: List[Player] = mapBuffer.getOrElse(player.team, null)
    var resp = player
    if(list== null)
      resp = null
    else if(playerExist(player.name, list)){
      list = list.filter(_.name != player.name)
      cache.remove(player.team)
      cache.getOrLoad(player.team, _ => calculateTeamScore(list))
    }
    else
      resp = null
    resp
  }

  def getPlayers(name:String):List[Player] ={
     mapBuffer.getOrElse(name.replace("_", " "), null)
  }

  def getScores(teamA:String, teamB:String): String={
    val team1 = teamA.replace("_", " ")
    val team2 = teamB.replace("_", " ")

    if (!teamExists(team1))
      "Error: the team " + team1 + " does not exist, or have no players"
    else if (!teamExists(team2))
      "Error: the team " + team2 + " does not exist, or have no players"
    else{
      val score1 = cache.getOrLoad(team1, _ => calculateTeamScore(mapBuffer.getOrElse(team1, null)))
      val score2  = cache.getOrLoad(team2, _ => calculateTeamScore(mapBuffer.getOrElse(team2, null)))

      var value1 = 0.0
      var value2= 0.0

      score1.map(i=> value1=i)
      score2.map(i=> value2=i)

      Await.result(score1, 5.seconds)
      Await.result(score2, 5.seconds)

      if (value1 > value2)
        "The winner team is " + team1 + " with a score of " + value1 + ", " + team2 + " lose with " + value2
      else if (value1 < value2)
        "The winner team is " + team2 + " with a score of " + value2 + ", " + team1 + " lose with " + value1
      else
        "Both teams scores are equal to " + value1 + ". The game is tied"
    }
  }
}
