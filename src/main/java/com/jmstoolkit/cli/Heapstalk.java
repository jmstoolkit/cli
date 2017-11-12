/*
 * Copyright 2011, Scott Douglass <scott@swdouglass.com>
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License
 * on the World Wide Web for more details:
 * http://www.fsf.org/licensing/licenses/gpl.txt
 */
package com.jmstoolkit.cli;

import com.jmstoolkit.JTKException;
import com.jmstoolkit.Settings;
import gnu.getopt.Getopt;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.jms.ConnectionFactory;
import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.TextMessage;
import javax.naming.NamingException;
import org.springframework.jndi.JndiTemplate;

/**
 * This class is intended for testing JVM heap growth. The received messages
 * will not be written out but will be added to a Collection continuously.
 *
 * @author Scott Douglass
 */
public class Heapstalk extends Receiver {

  /**
   * Logger for this class.
   */
  private static final Logger LOGGER
    = Logger.getLogger(Heapstalk.class.getName());
  /**
   * List of messages.
   */
  private final List<String> messageList = new ArrayList<String>();
  /**
   * A message with this text body will end the program.
   */
  private static final String M_EXIT = "exit";

  @Override
  public void onMessage(final Message msg) {
    setMessagesReceived((Integer) (getMessagesReceived() + 1));
    try {
      if (msg instanceof TextMessage) {
        String messageText = ((TextMessage) msg).getText();
        messageList.add(messageText + "\n");
        LOGGER.log(Level.INFO, "Message received: {0}", getMessagesReceived());
        if (messageText.equalsIgnoreCase(M_EXIT)) {
          this.stop();
        }
      }
    } catch (JMSException e) {
      LOGGER.log(Level.SEVERE, "Could not get message text", e);
    }
  }

  /**
   * Main method to run this dang thing.
   * @param args Command line arguments
   */
  public static void main(final String[] args) {
    try {
      Settings.loadSystemSettings(Settings.APP_PROPERTIES);
    } catch (JTKException e) {
      // no app.propertis, so we'll need -i and -c for the jndi object names
    }
    String jndiPropertiesFileName = D_JNDI_PROPERTIES;
    final String textEncoding = System.getProperty(P_ENCODING, D_ENCODING);

    final Getopt getopt = new Getopt(D_APP_NAME, args, "c:i:j:f:e:h");
    int optionLetter;
    while ((optionLetter = getopt.getopt()) != -1) {
      switch (optionLetter) {
        case 'c':
          System.setProperty(P_CONNECTION_FACTORY_NAME, getopt.getOptarg());
          break;
        case 'i':
          System.setProperty(P_DESTINATION_NAME, getopt.getOptarg());
          break;
        case 'j':
          jndiPropertiesFileName = getopt.getOptarg();
          break;
        case 'h':
          System.out.println("Arguments:\n  [ -i JMS Destination JNDI name ]\n"
            + "[ -c JMS ConnectionFactory JNDI name ]\n"
            + "[ -j JNDI properties ]\n  ");
          System.exit(X_ERROR);
      }
    }
    // load the jndi.properties, if we fail exit
    try {
      Settings.loadSystemSettings(
        System.getProperty(D_JNDI_PROPERTIES, jndiPropertiesFileName));
    } catch (JTKException e) {
      System.out.println(e.toStringWithStackTrace());
      System.exit(X_ERROR);
    }

    final Heapstalk receiver = new Heapstalk();
    receiver.setEncoding(textEncoding);

    receiver.setJndiTemplate(new JndiTemplate(System.getProperties()));

    try {
      receiver.setConnectionFactory(getSpringConnectionFactory(
        (ConnectionFactory) receiver.getJndiTemplate().lookup(
          System.getProperty(P_CONNECTION_FACTORY_NAME))));
    } catch (NamingException e) {
      System.out.println("JNDI object could not be found: "
        + System.getProperty(P_CONNECTION_FACTORY_NAME));
      System.out.println(JTKException.formatException(e));
      System.exit(X_ERROR);
    }

    try {
      receiver.setDestination((Destination) receiver.getJndiTemplate().lookup(
        System.getProperty(P_DESTINATION_NAME)));
    } catch (NamingException e) {
      System.out.println("JNDI object could not be found: "
        + System.getProperty(P_DESTINATION_NAME));
      System.out.println(JTKException.formatException(e));
      System.exit(X_ERROR);
    }
    receiver.start();
  }
}
