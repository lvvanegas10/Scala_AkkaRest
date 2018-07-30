package com.knoldus.inc

import akka.actor.ActorSystem
import akka.http.caching.LfuCache
import akka.http.caching.scaladsl.Cache
import akka.http.scaladsl.Http
import com.knoldus.inc.entities._
import akka.stream.ActorMaterializer
import com.knoldus.inc.routes.UserRoutes
import com.knoldus.inc.shopping.ComputeTeam
import com.typesafe.config.ConfigFactory
import scala.concurrent.ExecutionContextExecutor
import scala.io.StdIn
import scala.collection.mutable.HashMap
import scala.io.Source



/**
  * -- AUTOR --
  * Laura Valeria Vanegas lv.vanegas10
  * Punto 2
  * Propiedad Intelectual:
  *   Se realiza siguiento el template dado en https://github.com/solitudelad/akka-http-cache
  *
  */

object Client {

  def main(args: Array[String]): Unit = {

    implicit val system: ActorSystem = ActorSystem()
    implicit val materializer: ActorMaterializer = ActorMaterializer()
    implicit val executionContext: ExecutionContextExecutor = system.dispatcher

    /**
      * 1. Instantiation of Akka-Http Cache, LfuCache
      * 2. By Default Caching Strategy Provided by Akka-Http is Least Frequently Used
      * 3. For customizing additional setting like time to live etc
      * we can use CachingSettings class and configure it for our requirement
      */
    val cache: Cache[String, Float] = LfuCache[String, Float]

    var hashInit = HashMap.empty[String, List[Player]]
    val resourcesPath = getClass.getResource("/players.csv")

    val bufferedSource = Source.fromFile(resourcesPath.getPath)
    for (line <- bufferedSource.getLines) {
      val info = line.split(",")
      var list = List[Player]()
      val player = new Player(name = info(1), team = info(2), score=info(3).toInt)
      if (hashInit.getOrElse(info(2), null) == null){
        list = player :: Nil
      }
      else{
          list = player:: hashInit(player.team)
      }
      hashInit += (player.team -> list)
    }
    bufferedSource.close

    val computeTeam = new ComputeTeam(cache,hashInit)
    val userRoutes: UserRoutes = new UserRoutes(cache, computeTeam)

    /**
      * Setting up Akka-Http Server and binding the routes
      */
    val config = ConfigFactory.load("settings.properties")
    val hostname = config.getString("http.host")
    val port = config.getInt("http.port")
    val server = Http().bindAndHandle(userRoutes.routes, hostname, port)
    println(s"Listening on $hostname:$port")
    println("Http server started!")
    StdIn.readLine()

    server.flatMap(_.unbind)
    system.terminate()
    println("Http server terminated!")
  }
}
