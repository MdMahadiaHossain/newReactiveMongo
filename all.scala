build.sbt

name := """playWithReactiveMongo"""
organization := "com.brainbinary"

version := "1.0-SNAPSHOT"

lazy val root = (project in file(".")).enablePlugins(PlayScala)

scalaVersion := "2.13.3"

libraryDependencies += guice
libraryDependencies += ws
libraryDependencies += "org.scalatestplus.play" %% "scalatestplus-play" % "5.0.0" % Test

libraryDependencies ++= Seq(
  "org.reactivemongo" %% "play2-reactivemongo" % "0.20.11-play27"
)

libraryDependencies ++= Seq(
  "org.reactivemongo" %% "reactivemongo-play-json-compat" % "0.20.11-play27"
  )

// Adds additional packages into Twirl
//TwirlKeys.templateImports += "com.brainbinary.controllers._"

// Adds additional packages into conf/routes
// play.sbt.routes.RoutesKeys.routesImport += "com.brainbinary.binders._"


apoplication.conf
# https://www.playframework.com/documentation/latest/Configuration

play.modules.enabled += "play.modules.reactivemongo.ReactiveMongoModule"

mongo-async-driver {
  akka {
    loglevel = DEBUG
  }
}

mongodb.uri = "mongodb://play:playpass@127.0.0.1:27017/playApp?authenticationDatabase=admin&rm.nbChannelsPerNode=10&readPreference=secondaryPreferred"


Models and DAO

package models

import com.google.inject.Inject
import play.modules.reactivemongo.ReactiveMongoApi
import reactivemongo.api.bson.BSONObjectID
import java.{util => ju}
//import reactivemongo.api.collections.bson.BSONCollection
import scala.concurrent.ExecutionContext
import reactivemongo.api.bson.collection.BSONCollection  // this is necessary
import reactivemongo.api.bson.BSONValue
import scala.util.Try
import scala.util.Success


case class Man(
    _id:BSONObjectID,
    age:ju.Date
    )

object ManBSONFormat{
    import reactivemongo.api.bson._  // this is necessary
    implicit object ISODate extends BSONReader[ju.Date] {

      override def readTry(bson: BSONValue): Try[ju.Date] = Success.apply(new ju.Date())

   
  }
  implicit object ISODateWrite extends BSONWriter[ju.Date] {

    override def writeTry(t: ju.Date): Try[BSONValue] = Success.apply(BSONDateTime(t.getTime()))

    
  }


    implicit val m = Macros.handler[Man] 
    
}

class TestDAO @Inject()(reactiveMongoApi:ReactiveMongoApi)(implicit executionContext:ExecutionContext) {

    private def collection = reactiveMongoApi.database.map(_.collection[BSONCollection]("test"))

    import ManBSONFormat._
    def insertMan(man:Man) = collection.map(_.insert(true).one[Man](man)).flatten
  
  
     def getTransactionsOfAccount(dateFrom:ju.Date,dateTo:ju.Date): Future[List[BSONDocument]] = collection.map(_.aggregateWith[BSONDocument]() {
    aggregatorFramework =>    // No super type cast
      val obj = BSONDocument
      import aggregatorFramework.PipelineOperator
      PipelineOperator(obj(
            "$match" -> obj(
              "date" -> obj("$gte" -> dateFrom, "$lte" -> dateTo) // for date in bson document will need explicit reader and writter
            )
          )) -> List(
        PipelineOperator(
          obj(
            "$limit" -> 6
          )
        )
      )


  }.collect[List](1, Cursor.FailOnError())(List, executionContext)).flatten
    
  
}


Controller

package controllers.home

import play.api.mvc.BaseController
import play.api.mvc.ControllerComponents
import javax.inject.Singleton
import com.google.inject.Inject
import scala.concurrent.ExecutionContext
import scala.concurrent.Future
import play.api.mvc.RequestHeader
import play.api.mvc.AnyContent
import play.api.mvc.Request
import play.api.mvc.MessagesAbstractController
import play.api.mvc.MessagesControllerComponents
import play.api.mvc.MessagesRequest
import play.api.mvc.Action
import play.api.libs.json.JsValue
import play.api.libs.json.JsObject
import play.modules.reactivemongo.ReactiveMongoApi

import models.TestDAO
import reactivemongo.api.bson.BSONObjectID // this is necessary
import java.{util => ju}
import models.Man
import reactivemongo.api.bson.BSONDocument
import play.api.libs.json.Json



@Singleton
class Home2Controller @Inject()( controllerComponents: MessagesControllerComponents, testDAO:TestDAO)(implicit executionContext:ExecutionContext) extends MessagesAbstractController(controllerComponents){
  
    def hello2():Action[AnyContent]  = Action.async{
        request : MessagesRequest[AnyContent] => 
           
           testDAO.insertMan(Man(BSONObjectID.generate(),new ju.Date)).map{
               w =>

               import models.ManBSONFormat._
               BSONDocument().asOpt[Man]
               import _root_.play.api.libs.json.__
               import reactivemongo.play.json.compat._
               import bson2json._

                Ok(Json.toJson(BSONDocument("name"->"Jack")))
           }

      
       
    }


    


}
