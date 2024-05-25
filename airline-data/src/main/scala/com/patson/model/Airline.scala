package com.patson.model

import com.patson.data._
import com.patson.model.AirlineBaseSpecialization.{DelegateSpecialization, Specialization}

import java.util.{Calendar, Date}
import scala.collection.mutable.ListBuffer

case class Airline(name: String, isGenerated : Boolean = false, var id : Int = 0) extends IdObject {
  val airlineInfo = AirlineInfo(0, 0, 0, 0, 0)
  var allianceId : Option[Int] = None
  var bases : List[AirlineBase] = List.empty

  def setBalance(balance : Long) = {
    airlineInfo.balance = balance
  }

  def setCurrentServiceQuality(serviceQuality : Double) {
    airlineInfo.currentServiceQuality = serviceQuality
  }

  def setTargetServiceQuality(targetServiceQuality : Int) {
    airlineInfo.targetServiceQuality = targetServiceQuality
  }

  def setReputation(reputation : Double) {
    airlineInfo.reputation = reputation
  }

  def setMaintenanceQuality(maintenanceQuality : Double) {
    airlineInfo.maintenanceQuality = maintenanceQuality
  }

  def removeCountryCode() = {
    airlineInfo.countryCode = None
  }

  def setCountryCode(countryCode : String) = {
    airlineInfo.countryCode = Some(countryCode)
  }

  def getCountryCode() = {
    airlineInfo.countryCode
  }

  def setAirlineCode(airlineCode : String) = {
    airlineInfo.airlineCode = airlineCode
  }

  def getAirlineCode() = {
    airlineInfo.airlineCode
  }

  def setSkipTutorial(value : Boolean) = {
    airlineInfo.skipTutorial = value
  }

  def isSkipTutorial = {
    airlineInfo.skipTutorial
  }

  def setInitialized(value : Boolean) = {
    airlineInfo.initialized = value
  }

  def isInitialized = {
    airlineInfo.initialized
  }


  def setAllianceId(allianceId : Int) = {
    this.allianceId = Some(allianceId)
  }

  def getAllianceId() : Option[Int] = {
    allianceId
  }


  def setBases(bases : List[AirlineBase]) {
    this.bases = bases
  }

  //  import FlightCategory._
  //  val getLinkLimit = (flightCategory :FlightCategory.Value) => flightCategory match {
  //      case DOMESTIC => None
  //      case REGIONAL => None
  //      case INTERCONTINENTAL =>
  //        if (airlineGrade.value <= 4) {
  //         Some(0)
  //        } else {
  //          Some((airlineGrade.value - 4) * 3)
  //        }
  //  }


  def airlineGrade : AirlineGrade = {
    val reputation = airlineInfo.reputation
    AirlineGrade.findGrade(reputation)
  }


  def getBases() = bases

  def getHeadQuarter() = bases.find(_.headquarter)

  def getBalance() = airlineInfo.balance

  def getCurrentServiceQuality() = airlineInfo.currentServiceQuality

  def getTargetServiceQuality() : Int = airlineInfo.targetServiceQuality

  def getReputation() = airlineInfo.reputation

  def getMaintenanceQuality() = airlineInfo.maintenanceQuality

  def getDefaultAirlineCode() : String = {
    var code = name.split("\\s+").foldLeft("")((foldString, nameToken) => {
      val firstCharacter = nameToken.charAt(0)
      if (Character.isLetter(firstCharacter)) {
        foldString + firstCharacter.toUpper
      } else {
        foldString
      }
    })

    if (code.length() > 2) {
      code = code.substring(0, 2)
    } else if (code.length() < 2) {
      if (name.length == 1) {
        code = (name.charAt(0).toString + name.charAt(0)).toUpperCase()
      } else {
        code = name.substring(0, 2).toUpperCase()
      }
    }
    code
  }

  lazy val slogan = AirlineSource.loadSlogan(id)
  lazy val previousNames = AirlineSource.loadPreviousNameHistory(id).sortBy(_.updateTimestamp.getTime)(Ordering.Long.reverse).map(_.name)

  def getDelegateInfo() : DelegateInfo = {
    val busyDelegates = DelegateSource.loadBusyDelegatesByAirline(id)
    val availableCount = delegateCount - busyDelegates.size

    DelegateInfo(availableCount, delegateBoosts, busyDelegates)
  }

  val BASE_DELEGATE_COUNT = 5
  val DELEGATE_PER_LEVEL = 3
  lazy val delegateCount = BASE_DELEGATE_COUNT +
    airlineGrade.value * DELEGATE_PER_LEVEL +
    AirlineSource.loadAirlineBasesByAirline(id).flatMap(_.specializations).filter(_.isInstanceOf[DelegateSpecialization]).map(_.asInstanceOf[DelegateSpecialization].delegateBoost).sum +
    delegateBoosts.map(_.amount).sum
  lazy val delegateBoosts = AirlineSource.loadAirlineModifierByAirlineId(id).filter(_.modifierType == AirlineModifierType.DELEGATE_BOOST).map(_.asInstanceOf[DelegateBoostAirlineModifier])
}


case class DelegateInfo(availableCount : Int, boosts : List[DelegateBoostAirlineModifier], busyDelegates: List[BusyDelegate]) {
  //take away all the boosted ones that are unoccupied, those are not eligible for permanent tasks (country relation/campaign etc)
  val permanentAvailableCount = {
    val cooldownDelegateCount = busyDelegates.filter(_.availableCycle.isDefined).length
    val unoccupiedBonusDelegateCount = boosts.map(_.amount).sum - cooldownDelegateCount
    if (unoccupiedBonusDelegateCount > 0) {
      availableCount - unoccupiedBonusDelegateCount
    } else {
      availableCount
    }
  }

}

case class AirlineInfo(var balance : Long, var currentServiceQuality : Double, var maintenanceQuality : Double, var targetServiceQuality : Int, var reputation : Double, var countryCode : Option[String] = None, var airlineCode : String = "", var skipTutorial : Boolean = false, var initialized : Boolean = false)

object TransactionType extends Enumeration {
  type TransactionType = Value
  val CAPITAL_GAIN, CREATE_LINK = Value
}

object OtherIncomeItemType extends Enumeration {
  type OtherBalanceItemType = Value
  val LOAN_INTEREST, BASE_UPKEEP, OVERTIME_COMPENSATION, SERVICE_INVESTMENT, LOUNGE_UPKEEP, LOUNGE_COST, LOUNGE_INCOME, ASSET_EXPENSE, ASSET_REVENUE, ADVERTISEMENT, DEPRECIATION, FUEL_PROFIT = Value
}

object CashFlowType extends Enumeration {
  type CashFlowType = Value
  val BASE_CONSTRUCTION, BUY_AIRPLANE, SELL_AIRPLANE, CREATE_LINK, FACILITY_CONSTRUCTION, OIL_CONTRACT, ASSET_TRANSACTION = Value
}

object Period extends Enumeration {
  type Period = Value
  val WEEKLY, MONTHLY, YEARLY = Value
}


case class AirlineTransaction(airlineId : Int, transactionType : TransactionType.Value, amount : Long, var cycle : Int = 0)
case class AirlineIncome(airlineId : Int, profit : Long = 0, revenue: Long = 0, expense: Long = 0, links : LinksIncome, transactions : TransactionsIncome, others : OthersIncome, period : Period.Value = Period.WEEKLY, var cycle : Int = 0) {
  /**
   * Current income is expected to be MONTHLY/YEARLY. Adds parameter (WEEKLY income) to this current income object and return a new Airline income with period same as this object but cycle as the parameter
   */
  def update(income2 : AirlineIncome) : AirlineIncome = {
    AirlineIncome(airlineId, 
        profit = profit + income2.profit,
        revenue = revenue + income2.revenue,
        expense = expense + income2.expense,
        links = links.update(income2.links),
        transactions = transactions.update(income2.transactions),
        others = others.update(income2.others),
        period = period,
        cycle = income2.cycle)
  }
}
case class LinksIncome(airlineId : Int, profit : Long = 0, revenue : Long = 0, expense : Long = 0, ticketRevenue: Long = 0, airportFee : Long = 0, fuelCost : Long = 0, crewCost : Long = 0, inflightCost : Long = 0, delayCompensation : Long = 0, maintenanceCost: Long = 0, loungeCost : Long = 0, depreciation : Long = 0, period : Period.Value = Period.WEEKLY, var cycle : Int = 0) {
  def update(income2 : LinksIncome) : LinksIncome = {
    LinksIncome(airlineId, 
        profit = profit + income2.profit,
        revenue = revenue + income2.revenue,
        expense = expense + income2.expense,
        ticketRevenue = ticketRevenue + income2.ticketRevenue,
        airportFee = airportFee + income2.airportFee,
        fuelCost = fuelCost + income2.fuelCost,
        crewCost = crewCost + income2.crewCost,
        inflightCost = inflightCost + income2.inflightCost,
        delayCompensation = delayCompensation + income2.delayCompensation,
        maintenanceCost = maintenanceCost + income2.maintenanceCost,
        loungeCost = loungeCost + income2.loungeCost,
        depreciation = depreciation + income2.depreciation,
        period = period,
        cycle = income2.cycle)
  }
}
case class TransactionsIncome(airlineId : Int, profit : Long = 0, revenue: Long = 0, expense: Long = 0, capitalGain : Long = 0, createLink : Long = 0,  period : Period.Value = Period.WEEKLY, var cycle : Int = 0) {
  def update(income2 : TransactionsIncome) : TransactionsIncome = {
    TransactionsIncome(airlineId, 
        profit = profit + income2.profit,
        revenue = revenue + income2.revenue,
        expense = expense + income2.expense,
        capitalGain = capitalGain + income2.capitalGain,
        createLink = createLink + income2.createLink,
        period = period,
        cycle = income2.cycle)
  }  
}
case class OthersIncome(airlineId : Int, profit : Long = 0, revenue: Long = 0, expense: Long = 0, loanInterest : Long = 0, baseUpkeep : Long = 0, overtimeCompensation : Long = 0, serviceInvestment : Long = 0, advertisement : Long = 0, loungeUpkeep : Long = 0, loungeCost : Long = 0, loungeIncome : Long = 0, assetExpense : Long = 0, assetRevenue : Long = 0, fuelProfit : Long = 0, depreciation : Long = 0, period : Period.Value = Period.WEEKLY, var cycle : Int = 0) {
  def update(income2 : OthersIncome) : OthersIncome = {
    OthersIncome(airlineId, 
        profit = profit + income2.profit,
        revenue = revenue + income2.revenue,
        expense = expense + income2.expense,
        loanInterest = loanInterest + income2.loanInterest,
        baseUpkeep = baseUpkeep + income2.baseUpkeep,
        overtimeCompensation = overtimeCompensation + income2.overtimeCompensation,
        serviceInvestment = serviceInvestment + income2.serviceInvestment,
        advertisement = advertisement + income2.advertisement,
        loungeUpkeep = loungeUpkeep + income2.loungeUpkeep,
        loungeCost = loungeCost + income2.loungeCost,
        loungeIncome = loungeIncome + income2.loungeIncome,
        assetExpense = assetExpense + income2.assetExpense,
        assetRevenue = assetRevenue + income2.assetRevenue,
        fuelProfit = fuelProfit + income2.fuelProfit,
        depreciation = depreciation + income2.depreciation,
        period = period,
        cycle = income2.cycle)
  }    
}


case class AirlineCashFlowItem(airlineId : Int, cashFlowType : CashFlowType.Value, amount : Long, var cycle : Int = 0)
case class AirlineCashFlow(airlineId : Int, cashFlow : Long = 0, operation : Long = 0, loanInterest : Long = 0, loanPrincipal : Long = 0, baseConstruction : Long = 0, buyAirplane : Long = 0, sellAirplane : Long = 0,  createLink : Long = 0, facilityConstruction : Long = 0, oilContract : Long = 0, assetTransactions : Long = 0, period : Period.Value = Period.WEEKLY, var cycle : Int = 0) {
/**
   * Current income is expected to be MONTHLY/YEARLY. Adds parameter (WEEKLY income) to this current income object and return a new Airline income with period same as this object but cycle as the parameter
   */
  def update(cashFlow2 : AirlineCashFlow) : AirlineCashFlow = {
    AirlineCashFlow(airlineId, 
        cashFlow = cashFlow + cashFlow2.cashFlow,
        operation = operation + cashFlow2.operation,
        loanInterest = loanInterest + cashFlow2.loanInterest,
        loanPrincipal = loanPrincipal + cashFlow2.loanPrincipal,
        baseConstruction = baseConstruction + cashFlow2.baseConstruction,
        buyAirplane = buyAirplane + cashFlow2.buyAirplane,
        sellAirplane = sellAirplane + cashFlow2.sellAirplane,
        createLink = createLink + cashFlow2.createLink,
        facilityConstruction = facilityConstruction + cashFlow2.facilityConstruction,
        oilContract = oilContract + cashFlow2.oilContract,
        assetTransactions = assetTransactions + cashFlow2.assetTransactions,
        period = period,
        cycle = cashFlow2.cycle)
  }
}

object Airline {
  def fromId(id : Int) = {
    val airlineWithJustId = Airline("<unknown>")
    airlineWithJustId.id = id
    airlineWithJustId
  }
  val MAX_SERVICE_QUALITY : Double = 100
  val MAX_MAINTENANCE_QUALITY : Double = 100
  val MAX_REPUTATION_BY_PASSENGERS : Double = 100
  val MAX_REPUTATION : Double = 100


  def resetAirline(airlineId : Int, newBalance : Long, resetExtendedInfo : Boolean = false) : Option[Airline] = {
    AirlineSource.loadAirlineById(airlineId, true) match {
      case Some(airline) =>
        LinkSource.deleteLinksByAirlineId(airlineId)//remove all links

        //remove all airplanes
        AirplaneSource.deleteAirplanesByCriteria(List(("owner", airlineId)));

        //remove all assets
        AirportAssetSource.loadAirportAssetsByAirline(airlineId).foreach { asset =>
          AirportAssetSource.deleteAirportAsset(asset.id)
        }

        //remove all bases
        airline.getBases().foreach(_.delete)

        //remove all loans
        BankSource.loadLoansByAirline(airlineId).foreach { loan =>
          BankSource.deleteLoan(loan.id)
        }

        //remove all oil contract
        OilSource.deleteOilContractByCriteria(List(("airline", airlineId)))

        airline.getAllianceId().foreach { allianceId =>
          AllianceSource.loadAllianceById(allianceId).foreach { alliance =>
            alliance.members.find(_.airline.id == airline.id).foreach { member =>
              alliance.removeMember(member, true)
            }
          }
        }

        AllianceSource.loadAllianceMemberByAirline(airline).foreach { allianceMember =>
          AllianceSource.deleteAllianceMember(airlineId)
          if (allianceMember.role == AllianceRole.LEADER) { //remove the alliance
            AllianceSource.deleteAlliance(allianceMember.allianceId)
          }
        }


        AirlineSource.deleteReputationBreakdowns(airline.id)

        NegotiationSource.deleteLinkDiscountsByAirline(airline.id)

        airline.setBalance(newBalance)

        //unset country code
        airline.removeCountryCode()
        //unset service investment
        airline.setTargetServiceQuality(0)
        airline.setCurrentServiceQuality(0)

        if (resetExtendedInfo) {
          airline.setReputation(0)
          airline.setInitialized(false)
          AirportSource.deleteAirlineAppealsFromAllAirports(airlineId)
          LoyalistSource.deleteLoyalistsByAirline(airlineId)
        }

        //reset all busy delegates
        DelegateSource.deleteBusyDelegateByCriteria(List(("airline", "=", airlineId)))

        //reset all campaigns, has to be after delegate
        CampaignSource.deleteCampaignsByAirline(airline.id)

        //reset all notice
        NoticeSource.deleteNoticesByAirline(airline.id)

        AirlineSource.saveAirlineInfo(airline)
        println(s"Reset airline - $airline")
        Some(airline)
      case None =>
        None
    }
  }
}

case class AirlineGrade(value : Int, reputationCeiling : Int, description: String) {
  AirlineGrade.addGrade(this)
  val getBaseLimit = {
    if (value <= 2) {
      1
    } else {
      value - 1
    }

  }

  val getModelFamilyLimit =  {
    Math.max(2, Math.min(10, value))
  }
}

object AirlineGrade {
  val allGrades = ListBuffer[AirlineGrade]()
  val NEW = AirlineGrade(1, 20, "New Airline")
  val LOCAL = AirlineGrade(2, 40, "Local Airline")
  val MUNICIPAL = AirlineGrade(3, 60, "Municipal Airline")
  val REGIONAL = AirlineGrade(4, 80, "Regional Airline")
  val CONTINENTAL = AirlineGrade(5, 100, "Continental Airline")
  val LESSER_INTERNATIONAL = AirlineGrade(6, 125, "Lesser International Airline")
  val THIRD_INTERNATIONAL = AirlineGrade(7, 150, "Third-class International Airline")
  val SECOND_INTERNATIONAL = AirlineGrade(8, 175, "Second-class International Airline")
  val MAJOR_INTERNATIONAL = AirlineGrade(9, 200, "Major International Airline")
  val TOP_INTERNATIONAL = AirlineGrade(10, 250, "Top International Airline")
  val TOP_INTERNATIONAL_2 = AirlineGrade(11, 300, "Top International Airline II")
  val TOP_INTERNATIONAL_3 = AirlineGrade(12, 350, "Top International Airline III")
  val TOP_INTERNATIONAL_4 = AirlineGrade(13, 400, "Top International Airline IV")
  val TOP_INTERNATIONAL_5 = AirlineGrade(14, 450, "Top International Airline V")
  val EPIC = AirlineGrade(15, 500, "Epic Airline")
  val ULTIMATE = AirlineGrade(16, 550, "Ultimate Airline")
  val LEGENDARY = AirlineGrade(17, 600, "Legendary Airline")
  val CELESTIAL = AirlineGrade(18, 700, "Celestial Airline")
  val MYTHIC = AirlineGrade(19, 800, "Mythic Airline")

  def addGrade(grade : AirlineGrade) = {
    allGrades.append(grade)
  }

  def findGrade(reputation : Double) = {
    allGrades.find(_.reputationCeiling > reputation).getOrElse(allGrades.last)
  }
}

object AirlineModifier {
  def fromValues(modifierType : AirlineModifierType.Value, creationCycle : Int, expiryCycle : Option[Int], properties : Map[AirlineModifierPropertyType.Value, Long]) : AirlineModifier = {
    import AirlineModifierType._
    val modifier = modifierType match {
      case NERFED => NerfedAirlineModifier(creationCycle)
      case DELEGATE_BOOST => DelegateBoostAirlineModifier(
        properties(AirlineModifierPropertyType.STRENGTH).toInt,
        properties(AirlineModifierPropertyType.DURATION).toInt,
        creationCycle)
      case BANNER_LOYALTY_BOOST => BannerLoyaltyAirlineModifier(
        properties(AirlineModifierPropertyType.STRENGTH).toInt,
        creationCycle)
    }

    modifier
  }
}



abstract class AirlineModifier(val modifierType : AirlineModifierType.Value, val creationCycle : Int, val expiryCycle : Option[Int], var id : Int = 0) extends IdObject {
  def properties : Map[AirlineModifierPropertyType.Value, Long]
  def isHidden : Boolean //should it be visible to admin only
}

case class NerfedAirlineModifier(override val creationCycle : Int) extends AirlineModifier(AirlineModifierType.NERFED, creationCycle, None) {
  val FULL_EFFECT_DURATION = 300 //completely kicks in after 100 cycles
  val FULL_COST_MULTIPLIER = 1.25
  val costMultiplier = (currentCycle : Int) => {
    val age = currentCycle - creationCycle
    if (age >= FULL_EFFECT_DURATION) {
      FULL_COST_MULTIPLIER
    } else if (age >= 0) {
      1 + age.toDouble / FULL_EFFECT_DURATION * (FULL_COST_MULTIPLIER - 1)
    } else {
      1
    }
  }

  override def properties : Map[AirlineModifierPropertyType.Value, Long] = Map.empty
  override def isHidden = true
}

case class DelegateBoostAirlineModifier(amount : Int, duration : Int, override val creationCycle : Int) extends AirlineModifier(AirlineModifierType.DELEGATE_BOOST, creationCycle, Some(creationCycle + duration)) {
  lazy val internalProperties = Map[AirlineModifierPropertyType.Value, Long](AirlineModifierPropertyType.STRENGTH -> amount , AirlineModifierPropertyType.DURATION -> duration)
  override def properties : Map[AirlineModifierPropertyType.Value, Long] = internalProperties
  override def isHidden = true
}

case class BannerLoyaltyAirlineModifier(amount : Int, override val creationCycle : Int) extends AirlineModifier(AirlineModifierType.BANNER_LOYALTY_BOOST, creationCycle, Some(creationCycle +  10 * 52)) {
  lazy val internalProperties = Map[AirlineModifierPropertyType.Value, Long](AirlineModifierPropertyType.STRENGTH -> amount)
  override def properties : Map[AirlineModifierPropertyType.Value, Long] = internalProperties
  override def isHidden = false
}


object AirlineModifierType extends Enumeration {
  type AirlineModifierType = Value
  val NERFED, DELEGATE_BOOST, BANNER_LOYALTY_BOOST = Value
}

object AirlineModifierPropertyType extends Enumeration {
  type AirlineModifierPropertyType = Value
  val STRENGTH, DURATION = Value
}

case class NameHistory(name : String, updateTimestamp : Date)