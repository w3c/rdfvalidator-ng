package org.w3.rdfvalidator

import org.eclipse.jetty.server.nio.SelectChannelConnector
import org.eclipse.jetty.server.{ Server }
import org.eclipse.jetty.server.handler.ContextHandlerCollection
import org.eclipse.jetty.webapp.WebAppContext
import org.eclipse.jetty.servlet.{ DefaultServlet, ServletContextHandler, ServletHolder }

object JettyMain {
  
  def main(args: Array[String]) = {
    val server: Server = new Server

    server setGracefulShutdown 500
    server setSendServerVersion false
    server setSendDateHeader true
    server setStopAtShutdown true

    val connector = new SelectChannelConnector
    connector setPort 8080
    connector setMaxIdleTime 90000
    server addConnector connector

    val webapp = "src/main/webapp"
    val webApp = new WebAppContext
    webApp setContextPath "/"
    webApp setResourceBase webapp
    webApp setDescriptor (webapp+"/WEB-INF/web.xml");

    server setHandler webApp

    server.start()
  }
}
