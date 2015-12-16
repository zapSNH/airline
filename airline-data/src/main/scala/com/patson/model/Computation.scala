package com.patson.model

import com.patson.model.airplane._
import com.patson.data.CycleSource

object Computation {
  //distance vs max speed
  val speedLimits = List((300, 350), (400, 500), (400, 700))  
  def calculateDuration(airplaneModel: Model, distance : Int) = {
    var remainDistance = distance
    var duration = 0;
    for ((distanceBucket, maxSpeed) <- speedLimits if(remainDistance > 0)) {
      val speed = Math.min(maxSpeed, airplaneModel.speed)
      if (distanceBucket >= remainDistance) {
        duration += remainDistance * 60 / speed
      } else {
        duration += distanceBucket * 60 / speed
      }
      remainDistance -= distanceBucket
    }
    
    if (remainDistance > 0) {
      duration += remainDistance * 60 / airplaneModel.speed
    }
    duration
  }

  def calculateMaxFrequency(airplaneModel: Model, distance : Int) : Int = {
    if (airplaneModel.range < distance) {
      0
    } else {
      val duration = calculateDuration(airplaneModel, distance)
      val roundTripTime = (duration + airplaneModel.turnoverTime) * 2
      val availableFlightTimePerWeek = (3.5 * 24 * 60).toInt //assume per week only 3 days are "flyable"
      //println(airplaneModel + " distance " + distance + " freq: " + availableFlightTimePerWeek / roundTripTime + " times")
      availableFlightTimePerWeek / roundTripTime
    }
  }
  
  def calculateAge(fromCycle : Int) = {
    val currentCycle = CycleSource.loadCycle()
    currentCycle - fromCycle 
  }
  
  def calculateAirplaneValue(airplane : Airplane) : Int = {
    val maxAge = 30 * 52 //after age 30 no value...
    //80% off * condition * (age out of 30 years ratio)
    val value = airplane.model.price * 0.8 * airplane.condition / 100 * ((maxAge - calculateAge(airplane.constructedCycle)).toDouble / maxAge)
    if (value < 0) 0 else value.toInt
  }
  
  def calculateAirportRadius(airport : Airport) : Int = {
    airport.size match {
      case 1 => 50
      case 2 => 100
      case n if (n >= 3) => 200
      case _ => 0
    }
  }
  
  def getFlightType(fromAirport : Airport, toAirport : Airport) = { //need quick calculation
    var longitudeDelta = Math.abs(fromAirport.longitude - toAirport.longitude)
    if (longitudeDelta >= 180) { longitudeDelta = 360 - longitudeDelta } //wraps around
    var latitudeDelta = Math.abs(fromAirport.latitude - toAirport.latitude)
    
    import FlightType._
    if (fromAirport.countryCode == toAirport.countryCode) { //domestic
      if (longitudeDelta <= 20 && latitudeDelta <= 15) {
        SHORT_HAUL_DOMESTIC
      } else {
        LONG_HAUL_DOMESTIC
      }
    } else { //international
      if (longitudeDelta <= 20 && latitudeDelta <= 20) {
        SHORT_HAUL_INTERNATIONAL
      } else if (longitudeDelta <= 50 && latitudeDelta <= 30) {
        LONG_HAUL_INTERNATIONAL
      } else {
        ULTRA_LONG_HAUL_INTERNATIONAL
      }
    }
  }
}