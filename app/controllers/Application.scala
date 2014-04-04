package controllers

import play.api._
import play.api.mvc._
import java.sql.{Connection, DriverManager, ResultSet}

object Application extends Controller {

  private def mysql_test = {
    val passwd = System.getenv("OJS_DB_PASSWD")
    val conn_str = "jdbc:mysql://sql.udl.pl:3306/slonka_ojs238?user=slonka_ojs&password=" + passwd
    var contents = ""

    classOf[com.mysql.jdbc.Driver]

    val conn = DriverManager.getConnection(conn_str)
    try {
      val statement = conn.createStatement(ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY)

      val rs = statement.executeQuery("SELECT * FROM journals")

      while (rs.next) {
        contents += rs.getString("path") + " "
      }

    } finally {
      conn.close()
    }
    contents
  }

  def pgsql_test = {

    val passwd = System.getenv("INTERNAL_DB_PASSWD")
    val user = "zlamwfnyrreuew"
    val conn_str = "jdbc:postgresql://ec2-54-246-101-204.eu-west-1.compute.amazonaws.com:5432/d9ml8v51vhnu5u?ssl=true&sslfactory=org.postgresql.ssl.NonValidatingFactory"
    var contents = ""

    println(classOf[org.postgresql.Driver])

    val conn = DriverManager.getConnection(conn_str, user, passwd)
    try {

      val statement = conn.createStatement(ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY)

      val rs = statement.executeQuery("SELECT * FROM authors")

      while (rs.next) {
        contents += rs.getString("email") + " "
      }
    } finally {
      conn.close()
    }
    contents
  }

  def index = Action {

    var contents = "Your new application is ready. So let's start coding, John Doe! "

    contents += "MySQL: "
    contents += mysql_test

    contents += "PgSQL: "
    contents += pgsql_test

    Ok(views.html.index(contents))
  }



}

