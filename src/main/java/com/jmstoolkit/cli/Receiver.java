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

import com.jmstoolkit.AbstractSpringMessageListener;
import com.jmstoolkit.JTKException;
import com.jmstoolkit.Settings;
import gnu.getopt.Getopt;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.jms.BytesMessage;
import javax.jms.ConnectionFactory;
import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.TextMessage;
import javax.naming.NamingException;
import org.springframework.jms.connection.UserCredentialsConnectionFactoryAdapter;
import org.springframework.jndi.JndiTemplate;

/**
 *
 * @author Scott Douglass
 */
public class Receiver extends AbstractSpringMessageListener {

  /**
   * Logger for this class.
   */
  private static final Logger LOGGER
    = Logger.getLogger(Receiver.class.getName());
  /**
   * Property name for the character set encoding.
   */
  protected static final String P_ENCODING = "jmstoolkit.encoding";
  /**
   * Default character set encoding value: UTF-8.
   */
  protected static final String D_ENCODING = "UTF-8";
  /**
   * Property name for the connection factory.
   */
  protected static final String P_CONNECTION_FACTORY_NAME = "jmstoolkit.cf";
  /**
   * Property name for the JMS destination.
   */
  protected static final String P_DESTINATION_NAME = "jmstoolkit.destination";
  /**
   * Property name for the ConnectionFactory user.
   */
  protected static final String P_USERNAME = "jmstoolkit.username";
  /**
   * Property name for the ConnectionFatory password.
   */
  protected static final String P_PASSWORD = "jmstoolkit.password";
  /**
   * Exit code when maximum number of messages has been received.
   */
  protected static final int X_MAX_MESSAGES = 2;
  /**
   * Exit code when an error occurs.
   */
  protected static final int X_ERROR = 1;
  /**
   * The maximum number of messages to receive.
   */
  private Integer maximumMessagesToReceive = 0;
  /**
   * The OutputStream to send received messages.
   */
  private OutputStream outputStream;
  /**
   * The encoding for the text messages.
   */
  private String encoding = D_ENCODING;
  /**
   * The Writer for the OutputSream.
   */
  private Writer outputWriter = null;

  @Override
  public void onMessage(Message msg) {
    setMessagesReceived((Integer) (getMessagesReceived() + 1));
    try {
      if (getOutputWriter() == null) {
        setOutputWriter(new BufferedWriter(new OutputStreamWriter(
          getOutputStream() == null ? System.out : getOutputStream(),
          getEncoding())));
      }
      if (msg instanceof TextMessage) {
        getOutputWriter().write(((TextMessage) msg).getText() + "\n");
      } else if (msg instanceof BytesMessage) {
        getOutputWriter().write("BytesMessage not supported at this time.");
      } else {
        getOutputWriter().write("Unknown message type: "
          + msg.getClass().getName());
      }
      getOutputWriter().flush();

    } catch (UnsupportedEncodingException e) {
      LOGGER.log(Level.SEVERE, "Bad encoding: " + getEncoding(), e);
    } catch (JMSException e) {
      LOGGER.log(Level.SEVERE, "Could not get message text", e);
    } catch (IOException e) {
      LOGGER.log(Level.SEVERE, "Error writing to output stream", e);
    }

    if (getMessagesReceived().intValue()
      == getMaximumMessagesToReceive().intValue()) {
      this.stop();
    }
  }

  @Override
  public final void stop() {
    super.stop();
    if (getOutputWriter() != null) {
      try {
        getOutputWriter().close();
      } catch (IOException ex) {
        //
      }
    }
    System.exit(X_MAX_MESSAGES);
  }

  /**
   * arguments: 
   * <code>
   * -i destination JNDI name (input) 
   * -o output filename (otherwise stdout) 
   * -j jndi properties file (defaults to jndi.properties) 
   * -f connection factory JNDI name 
   * -n maximum number of messages to receive
   *</code>
   * @param args Command line arguments
   */
  public static void main(final String[] args) {
    try {
      Settings.loadSystemSettings(Settings.APP_PROPERTIES);
    } catch (JTKException ex) {
      // no app.propertis, so we'll need -i and -optionLetter for the jndi object names
    }
    String jndiPropertiesFileName = D_JNDI_PROPERTIES;
    String outputFileName = "";
    Integer maximumNumberOfMessages = 0;
    String textEncoding = System.getProperty(P_ENCODING, D_ENCODING);

    Getopt getopt = new Getopt(D_APP_NAME, args, "c:i:o:j:f:n:e:h");
    int optionLetter;
    while ((optionLetter = getopt.getopt()) != -1) {
      switch (optionLetter) {
        case 'c':
          System.setProperty(P_CONNECTION_FACTORY_NAME, getopt.getOptarg());
          break;
        case 'i':
          System.setProperty(P_DESTINATION_NAME, getopt.getOptarg());
          break;
        case 'o':
          outputFileName = getopt.getOptarg();
          break;
        case 'j':
          jndiPropertiesFileName = getopt.getOptarg();
          break;
        case 'n':
          maximumNumberOfMessages = Integer.valueOf(getopt.getOptarg());
          break;
        case 'e':
          textEncoding = getopt.getOptarg();
          break;
        case 'h':
          System.out.println("Arguments:\n  [ -i JMS Destination JNDI name ]\n"
            + "[ -c JMS ConnectionFactory JNDI name ]\n  "
            + "[ -j JNDI properties ]\n  "
            + "[ -e character encoding (default: UTF-8) ]\n  "
            + "[ -n number of message to receive ]\n  "
            + "[ -o output file (if not set output to stdout) ]");
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

    FileOutputStream outputStream = null;
    if (!outputFileName.isEmpty()) {
      try {
        outputStream = new FileOutputStream(new File(outputFileName));
      } catch (FileNotFoundException e) {
        System.out.println("Output file: " + outputFileName
          + " could not be created.");
        System.out.println(JTKException.formatException(e));
        System.exit(X_ERROR);
      }
    }

    final Receiver receiver = new Receiver();
    receiver.setMaximumMessagesToReceive(maximumNumberOfMessages);
    receiver.setEncoding(textEncoding);
    receiver.setOutputStream(outputStream);

    receiver.setJndiTemplate(new JndiTemplate(System.getProperties()));

    try {
      receiver.setConnectionFactory(getSpringConnectionFactory(
        (ConnectionFactory) receiver.getJndiTemplate().lookup(
          System.getProperty(P_CONNECTION_FACTORY_NAME))));
      if (receiver.getListenerContainer().getConnectionFactory() == null) {
        throw new NamingException("Something is wrong.");
      }
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

  /**
   * Lordy, no XAConnectionFactory support in the
   * UserCredentialsConnectionFactoryAdapter! See:
   * https://jira.springsource.org/browse/SPR-7952
   *
   * Also, Spring 3 changed the CachingConnectionFactory so that it extends
   * SingleConnectionFactory which explicitly does not allow the username and
   * password to be passed to the createTopic,createQueue methods. Arrgh.
   *
   * @param basecf is any implementation of ConnectionFactory
   * @return UserCredentialsConnectionFactoryAdapter
   */
  public static ConnectionFactory getSpringConnectionFactory(
    final ConnectionFactory basecf) {
    final UserCredentialsConnectionFactoryAdapter uccfa
      = new UserCredentialsConnectionFactoryAdapter();
    //CachingConnectionFactory ccf = new CachingConnectionFactory(basecf);
    //ccf.setCacheProducers(true);
    //uccfa.setTargetConnectionFactory(ccf);
    uccfa.setTargetConnectionFactory(basecf);
    uccfa.setUsername(System.getProperty(P_USERNAME));
    uccfa.setPassword(System.getProperty(P_PASSWORD));
    return uccfa;
  }

  /**
   * @return the outputStream
   */
  public final OutputStream getOutputStream() {
    return outputStream;
  }

  /**
   * @param inOutputStream the outputStream to set
   */
  public final void setOutputStream(final OutputStream inOutputStream) {
    this.outputStream = inOutputStream;
  }

  /**
   * @return the encoding
   */
  public final String getEncoding() {
    return encoding;
  }

  /**
   * @param inEncoding the encoding to set
   */
  public final void setEncoding(final String inEncoding) {
    this.encoding = inEncoding;
  }

  /**
   * @return the outputWriter
   */
  public final Writer getOutputWriter() {
    return outputWriter;
  }

  /**
   * @param inOutputWriter the outputWriter to set
   */
  public final void setOutputWriter(final Writer inOutputWriter) {
    this.outputWriter = inOutputWriter;
  }

  /**
   * @return the maximumMessagesToReceive
   */
  public final Integer getMaximumMessagesToReceive() {
    return maximumMessagesToReceive;
  }

  /**
   * @param inMaximumMessagesToReceive the maximumMessagesToReceive to set
   */
  public final void setMaximumMessagesToReceive(
    final Integer inMaximumMessagesToReceive) {
    this.maximumMessagesToReceive = inMaximumMessagesToReceive;
  }
}
