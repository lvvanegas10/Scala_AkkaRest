package com.knoldus.inc.routes

import akka.http.caching.scaladsl.Cache
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.server.Directives._
import com.knoldus.inc.entities._
import com.knoldus.inc.teamsRequest.ComputeTeam
import de.heikoseeberger.akkahttpplayjson.PlayJsonSupport

/**
  *
  * @param cache
  * @param computeCart
  */

class UserRoutes(cache: Cache[String, Float], computeTeam: ComputeTeam) extends PlayJsonSupport {

  val routes = addPlayer ~ putPlayer  ~ delPlayer ~ getPlayersTeam ~ computeScores


  def getPlayersTeam: Route =
    get {
        path("team") {
          parameters('key.as[String]) { (key) =>
            val resp = computeTeam.getPlayers(key)
            if (resp !=  null)
              complete(resp)
            else
              complete(400, "Error: team does not exist")
        }
      }
    }

  def addPlayer: Route =
    (post & entity(as[Player])) { player =>
      path("player") {
        val resp = computeTeam.addPlayer(player)
        if (resp !=  null)
          complete(resp)
        else
          complete(400, "Error: player already exists in that team")
      }
    }

  def putPlayer: Route =
    (put & entity(as[Player])) { player =>
      path("player") {
        val resp = computeTeam.putPlayer(player)
        if (resp !=  null)
          complete(resp)
        else
          complete(400, "Error: player do not exists in that team. You can only change stats")
      }
    }

  def delPlayer: Route =
    (delete & entity(as[Player])) { player =>
      path("player") {
        val resp = computeTeam.delPlayer(player)
        if (resp !=  null)
          complete(resp)
        else
          complete(400, "Error: player do not exists in that team")
      }
    }

  def computeScores: Route =
    get {
      path("player") {
          parameters('team1.as[String], 'team2.as[String]) { (team1, team2) =>
            val resp = computeTeam.getScores(team1,team2)
            if (!resp.startsWith("Error:"))
              complete(resp)
            else
              complete(400, resp)
        }
      }
    }

}
