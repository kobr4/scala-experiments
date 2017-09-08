package com.kobr4.snippets

import scalatags.Text.all._
import scalatags.Text.tags2.{nav, title}


trait WithHtml {
  def toHtml(readOnly: Boolean): Frag
}

case class Sample(name: String, mail: String) extends WithHtml {
  override def toHtml(readOnly: Boolean) = {
    form(
      Page.createTable(List(),
        List(
          List(raw("name"), input(`type` := "text", value := name)),
          List(raw("mail"), input(`type` := "text", value := mail))
        )
      )
    )
  }
}


object Page {
  def helloWorld(arg: WithHtml): String = html(
    head(
      title("Hello World"),
      link(rel := "stylesheet", href := "http://maxcdn.bootstrapcdn.com/bootstrap/3.3.7/css/bootstrap.min.css")
    ),
    body(
      div(cls := "container")(
        createNavBar(Some("My Title"), List("h1", "h2", "h3")),
        div(cls := "jumbotron")(
          p()("Hello world")
          ,
          arg.toHtml(false)
          //createTable(List("t1", "t2"), List(List("t0_1", "t0_2"), List("t1_1", "t1_2"))),
        )
      )
    )
  ).toString()


  def createTable(header: List[String], data: List[List[Frag]]) =
    table(tr(header.map(h => th(h))), data.map(line => tr(line.map(element => td(element)))))

  def createNavBar(title: Option[String], header: List[String]) =
    nav(cls := "navbar navbar-default")(
      //div(cls:="container-fluid")(
      div(cls := "navbar-header")(
        title.map(t => a(cls := "navbar-brand")(t))
      ),
      div(cls := "navbar-collapse collapse")(
        ul(cls := "nav navbar-nav")(
          header.map(h =>
            li()(a(h))
          )
        )
      )
      //)
    )

  def createForm(formField: Map[String, String]) = {
    createTable(List(), formField.keys.map(k => List(raw(k), raw(formField.getOrElse(k, "")))).toList)
  }
}
