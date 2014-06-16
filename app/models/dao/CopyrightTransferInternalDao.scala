/**
 * Created by Kuba on 2014-05-08.
 */
package models.dao

import play.api._
import play.api.mvc._
import java.sql.{DriverManager, ResultSet}
import models.copyright._
import scala.slick.driver.MySQLDriver.simple._
import scala.slick.driver.MySQLDriver
import play.api.db.DB
import utils.{SqlUtils, TokenGenerator}
import play.api.Play.current
import com.google.common.base.Optional
import java.sql.Date
import scala.slick.lifted
import slick.ojs.Tables
import slick.ojs
import org.joda.time.DateTime
import models.copyright.Copyright
import models.copyright.CopyrightTransferRequest
import models.copyright.CorrespondingAuthor



object CopyrightTransferInternalDao {

  def saveTransfer(filledForm: CopyrightTransferRequest) {
    Database.forDataSource(DB.getDataSource("internal")).withSession {
      implicit session =>
        slick.internal.Tables.Copyrighttransfer.insert(slick.internal.Tables.CopyrighttransferRow(
          0, filledForm.copyrightData.ojsId, filledForm.copyrightData.title,
          filledForm.copyrightData.correspondingAuthor.firstName
            + " " + filledForm.copyrightData.correspondingAuthor.middleName.getOrElse("")
            + " " + filledForm.copyrightData.correspondingAuthor.lastName,
          filledForm.copyrightData.correspondingAuthor.affiliation,
          filledForm.copyrightData.correspondingAuthor.email,
          new Date(filledForm.dateFilled.toDate().getTime()),
          filledForm.ipAddress,
          TokenGenerator.generate(),
          false,
          Option[Date](new Date(0)),
          filledForm.copyrightData.financialDisclosure
        ))
    }
  }

  def markTransferAsConfirmed(tokenSHA: String) : Int = {
    Database.forDataSource(DB.getDataSource("internal")).withSession {
      implicit session =>
        val map = slick.internal.Tables.Copyrighttransfer
          .filter(_.linktokenshasum === tokenSHA)
          .map(row => (row.datelinkconfirmed, row.linkconfirmed))
        return map.update((Option[Date](SqlUtils.getCurrnetSqlDate()), true))
    }
  }

  def removeTransfer(id: Int) = {
    Database.forDataSource(DB.getDataSource("internal")).withSession {
      implicit session =>
        slick.internal.Tables.Copyrighttransfer
          .filter(_.id === id)
          .mutate(_.delete())
    }
  }

  def transferExists(ojsArticleId: Int): Boolean = {
    Database.forDataSource(DB.getDataSource("internal")).withSession {
      implicit session =>
        slick.internal.Tables.Copyrighttransfer
          .filter(_.ojsarticleid === ojsArticleId).exists.run
    }
  }

  val yearFn = SimpleFunction[Int]("year")

  def listTransfer(ojsJournalId:Long, year:Int, volumeId: Int):Seq[slick.internal.Tables.CopyrighttransferRow] = {
    var articleIds:Seq[Long] = Seq(0, 1)

    if(volumeId != -1) {
      Database.forDataSource(DB.getDataSource("ojs")).withSession {
        implicit session =>
          articleIds = (for {
            article <- ojs.Tables.Articles if article.journalId === ojsJournalId && yearFn(Seq(article.lastModified)) === year
            issue <- ojs.Tables.PublishedArticles if issue.issueId === volumeId.toLong && article.journalId === ojsJournalId
          } yield issue.articleId).run
      }
    } else {
      Database.forDataSource(DB.getDataSource("ojs")).withSession {
        implicit session =>
          articleIds = (for {
            article <- ojs.Tables.Articles if article.journalId === ojsJournalId && yearFn(Seq(article.lastModified)) === year
          } yield article.articleId).run
      }
    }

    Database.forDataSource(DB.getDataSource("internal")).withSession {
      implicit session =>
        return (for {
          transfer <- slick.internal.Tables.Copyrighttransfer if transfer.ojsarticleid inSetBind articleIds.map(_.toInt)
        } yield transfer).list
    }

  }

  def listTransfer(ids: Seq[Int]) : List[CopyrightTransferRequest] = {
    Database.forDataSource(DB.getDataSource("internal")).withSession {
      implicit session =>
        (for {
          transfer <- slick.internal.Tables.Copyrighttransfer if transfer.id inSetBind ids
        } yield transfer).list.map(transfer => {
          CopyrightTransferRequest(
            Option(transfer.id), Copyright(
              transfer.ojsarticleid, transfer.title, CorrespondingAuthor(
                // TODO Get first and middle name in DB
                "", None, transfer.correspondingname, transfer.correspondingaffiliation, transfer.correspondingemail
              ),
              slick.internal.Tables.Authorscontribution.filter(_.copyrighttransferid === transfer.id).list.map(contribution => Contribution(
                // TODO Get first and middle name in DB
                "", None, contribution.authorname, contribution.affiliation, contribution.contribution, contribution.percent
              )),
              transfer.financialdisclosure
            ),
            new DateTime(transfer.dateformfilled.getTime), transfer.filleripaddress, if (transfer.linkconfirmed) CopyrightTransferStatus.CONFIRMED else CopyrightTransferStatus.UNCONFIRMED
          )
        })
    }
  }

  def getDefaultJournalId() : Int = {
    Database.forDataSource(DB.getDataSource("internal")).withSession {
      implicit session =>
        (for {
          transfer <- slick.internal.Tables.Copyrighttransfer
        } yield transfer.ojsarticleid ).first
    }
  }

  def getDefaultYear() : Int = {
    GeneralOjsDao.getYearsJournalActive(getDefaultJournalId()).head
  }

  def getDefaultVolumeId() : Int = {
    GeneralOjsDao.getIssuesForJournal(getDefaultJournalId()).head._1.toInt

  }
}
