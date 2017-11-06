/*
 * Copyright 2011, Scott Douglass <scott@swdouglass.com>
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed inputBuffer the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License
 * on the World Wide Web for more details:
 * http://www.fsf.org/licensing/licenses/gpl.txt
 */
package com.jmstoolkit.cli;

import com.jmstoolkit.Settings;
import com.jmstoolkit.JTKException;
import gnu.getopt.Getopt;
import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.UnknownHostException;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.jms.BytesMessage;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.TextMessage;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.jms.core.MessagePostProcessor;

/**
 *
 * @author Scott Douglass
 */
public class Sender {
  /** Logger for the class. */
  private static final Logger LOGGER = Logger.getLogger(Sender.class.getName());
  /** Property for the application name. */
  public static final String P_APP_NAME = "app.name";
  /** Default value for the application name property. */
  public static final String D_APP_NAME = "JMSToolKit";
  /** Default JNDI properties file. */
  public static final String D_JNDI_PROPERTIES = "jndi.properties";
  /** Property name or the correlation ID. */
  public static final String P_CORRELATION_ID = "correlation_id";
  /** Default value of the correlation ID property. */
  public static final String D_CORRELATION_ID = UUID.randomUUID().toString();
  /** Actual correlation ID value.*/
  public static final String CORRELATION_ID =
    System.getProperty(P_CORRELATION_ID, D_CORRELATION_ID);
  /** Propety name for the hostname. */
  public static final String P_HOSTNAME = "hostname";
  /** Default value for the hostname property. */
  public static final String D_HOSTNAME = "unknown";
  /** Property name for the text encoding. */
  private static final String P_ENCODING = "jmstoolkit.encoding";
  /** Default text encoding: UTF-8. */
  private static final String D_ENCODING = "UTF-8";
  /** Property name for the connection factory. */
  private static final String P_CONNECTION_FACTORY_NAME = "jmstoolkit.cf";
  /** Property name for the JMS destination. */
  private static final String P_DESTINATION_NAME = "jmstoolkit.destination";
  /** Message source/type "file". */
  public static final String TYPE_FILE = "file";
  /** Message source/type "stdin". */
  public static final String TYPE_STDIN = "stdin";
  /** Message source/type "fifo". */
  public static final String TYPE_PIPE = "fifo";
  /** Exit code for success. */
  private static final int X_OK = 0;
  /** Exit code for error. */
  private static final int X_ERROR = 1;
  /** Application name. */
  private static final String APP_NAME = "Sender";
  /** Message text encoding. */
  private String encoding = D_ENCODING;
  /** Message source/type. */
  private String messageType;
  /** Message text. */
  private String message = "";
  /** Spring JmsTemplate. */
  private JmsTemplate jmsTemplate;

  /** Constructor. */
  public Sender() {
    System.setProperty(P_APP_NAME, APP_NAME);
    try {
      System.setProperty(P_HOSTNAME,
        java.net.InetAddress.getLocalHost().getHostName());
    } catch (UnknownHostException e) {
      LOGGER.log(Level.WARNING, "Couldn't determine hostname", e);
      System.setProperty(P_HOSTNAME, D_HOSTNAME);
    }
  }

  /**
   *
   * @param args command line arguments
   */
  public static void main(final String[] args) {
    try {
      Settings.loadSystemSettings(Settings.APP_PROPERTIES);
    } catch (JTKException ex) {
      // no app.propertis, so we'll need -i and -c for the jndi object names
    }
    String jndiPropertiesFileName = D_JNDI_PROPERTIES;
    String inputFileName = "";
    final String textEncoding = System.getProperty(P_ENCODING, D_ENCODING);
    String inputPipeName = "";

    final Getopt getopt = new Getopt(APP_NAME, args, "c:o:j:f:i:hp:");
    int optionLetter;
    while ((optionLetter = getopt.getopt()) != -1) {
      switch (optionLetter) {
        case 'c':
          System.setProperty(P_CONNECTION_FACTORY_NAME, getopt.getOptarg());
          break;
        case 'o':
          System.setProperty(P_DESTINATION_NAME, getopt.getOptarg());
          break;
        case 'f':
          inputFileName = getopt.getOptarg();
          break;
        case 'j':
          jndiPropertiesFileName = getopt.getOptarg();
          break;
        case 'i':
          System.setProperty(P_CORRELATION_ID, getopt.getOptarg());
          break;
        case 'p': // Persistent sender
          inputPipeName = getopt.getOptarg();
          break;
        case 'h':
          System.out.println("Arguments:\n"
            + "[ -o destination ] JMS Destination JNDI name\n"
            + "[ -c connection factory ] JMS ConnectionFactory JNDI name\n"
            + "   Default values for -o and -c are set in app.properties\n"
            + "   Use java -Dapp.properties to change name of properties file\n"
            + "[ -j properties ] JNDI properties file (default: jndi.properties)\n"
            + "[ -e encoding ] character encoding (default: UTF-8)\n"
            + "[ -f file ] file to send\n"
            + "[ -p fifo ] read from named pipe/fifo\n"
            + "  If neither -p nor -f, read from stdin\n"
            + "[ -i id ] JMS Correlation ID");
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

    // Initialize the beans
    final ClassPathXmlApplicationContext applicationContext =
      new ClassPathXmlApplicationContext(new String[]{"/app-context.xml"});
    applicationContext.start();
    final Sender sender = (Sender) applicationContext.getBean(APP_NAME);

    sender.setEncoding(textEncoding);
    if (!inputPipeName.isEmpty()) {
      sender.messageType = TYPE_PIPE;
      sender.readAndSend(inputPipeName);
    } else if (inputFileName.isEmpty()) {
      sender.messageType = TYPE_STDIN;
      sender.sendTextFromStandardInput();
    } else {
      sender.messageType = TYPE_FILE;
      sender.sendTextFile(inputFileName);
    }
    System.exit(X_OK);
  }

  /**
   * Send text messages to a Queue using JmsTemplate. Only use JmsTemplate with
   * a J2EE container which pools JMS resources, or use the
   * CachingConnectionFactory.
   *
   * @param inMessage
   *          The text message to send.
   */
  public final void sendMessage(final Object inMessage) {
    this.getJmsTemplate().convertAndSend(inMessage);
  }
  /**
   *
   * @param inMessage the messaage to send
   * @param inProcessor the preprocessor for the message
   */
  public final void sendMessage(final Object inMessage,
    final MessagePostProcessor inProcessor) {
    this.getJmsTemplate().convertAndSend(inMessage, inProcessor);
  }

  /**
   *
   * @param inFileName name of file to send
   */
  public final void sendTextFile(final String inFileName) {
    message = loadTextFile(inFileName);
    sendMessage(message, new BasicMessageProcessor());
  }

  /**
   * Sends text from standard input.
   */
  public final void sendTextFromStandardInput() {
    message = readLinesFromStdin();
    sendMessage(message, new BasicMessageProcessor());
  }

  /**
   *
   * @param inStream message stream
   * @param inMessageLength messages length
   */
  public final void sendInputStream(final InputStream inStream,
    final Integer inMessageLength) {
    final byte[] buffer = new byte[inMessageLength];
    final BufferedInputStream messageInputStream =
      new BufferedInputStream(new ByteArrayInputStream(buffer));
    try {
      while (messageInputStream.read(buffer) != -1) {
        sendMessage(buffer, new BasicMessageProcessor());
      }
    } catch (IOException e) {
      LOGGER.log(Level.SEVERE, "Error reading input stream.", e);
    }
  }

  /**
   *
   * @param inFileName input file
   * @return the text of the file
   */
  public final String loadTextFile(final String inFileName) {
    BufferedInputStream messageFileStream = null;
    final File messageFile = new File(inFileName);
    final StringBuilder messageString = new StringBuilder("");
    final byte[] buffer = new byte[4096];
    try {
      messageFileStream =
        new BufferedInputStream(new FileInputStream(messageFile));
      while (messageFileStream.read(buffer) != -1) {
        messageString.append(new String(buffer));
      }
    } catch (FileNotFoundException e) {
      LOGGER.log(Level.SEVERE, "You want a what?", e);
    } catch (IOException e) {
      LOGGER.log(Level.SEVERE, "Sorry, I don't do that.", e);
    } finally {
      try {
        messageFileStream.close();
      } catch (IOException e) {
        //
      }
    }
    return messageString.toString().trim();
  }

  /**
   * Read lines from stdin.
   * @return the text from stdin
   */
  public final String readLinesFromStdin() {
    final StringBuilder input = new StringBuilder();
    try {
      final BufferedReader inputBuffer =
        new BufferedReader(new InputStreamReader(System.in));
      String line = "";
      while ((line = inputBuffer.readLine()) != null) {
        input.append(line);
        input.append("\n");
      }
    } catch (IOException e) {
    }
    return input.toString();
  }

  /**
   *
   * @param inputPipeName name of input pipe
   */
  public final void readAndSend(final String inputPipeName) {
    final StringBuilder input = new StringBuilder();
    try {
      final BufferedReader inputBuffer =
        new BufferedReader(new FileReader(new File(inputPipeName)));
      String line = "";
      while (true) {
        line = inputBuffer.readLine();
        if (line == null) {
          if (!input.toString().equals("null")
            && input.length() != 0) {
            sendMessage(input.toString(), new BasicMessageProcessor());
            input.delete(0, input.length());
          }
          try {
            Thread.sleep(100); // Give up the CPU
          } catch (InterruptedException ex) {
          }
        } else {
          input.append(line);
          input.append("\n");
        }
      }
    } catch (IOException e) {
      System.out.println(e);
    }
  }

  /**
   * Useful message pre-processor class.
   */
  public class BasicMessageProcessor implements MessagePostProcessor {

    @Override
    public final Message postProcessMessage(final Message msg)
      throws JMSException {
      msg.setStringProperty("app", System.getProperty(P_APP_NAME, D_APP_NAME));
      msg.setStringProperty("user", System.getProperty("user.name"));
      msg.setStringProperty("host", System.getProperty("hostname"));
      msg.setStringProperty("size", getMessageLength(msg));
      msg.setJMSCorrelationID(
        System.getProperty(P_CORRELATION_ID, CORRELATION_ID));
      msg.setJMSType(messageType);
      return msg;
    }

    /**
     *
     * @param msg the message
     * @return the length of the message
     */
    public final String getMessageLength(final Message msg) {
      String length = "unknown";
      try {
        if (msg instanceof TextMessage) {
          length = ((TextMessage) msg).getText().getBytes().length + "B";
        } else if (msg instanceof BytesMessage) {
          length = ((BytesMessage) msg).getBodyLength() + "B";
        }
      } catch (JMSException ex) {
      }
      return length;
    }
  }

  /**
   * @return the messageLength
   */
  public final String getMessageLength() {
    return message.getBytes().length + "B";
  }

  /**
   * @return the messageType
   */
  public final String getMessageType() {
    return messageType;
  }

  /**
   * @param messageType the messageType to set
   */
  public final void setMessageType(String messageType) {
    this.messageType = messageType;
  }

  /**
   * @return the message
   */
  public final String getMessage() {
    return message;
  }

  /**
   * @param inMessage the message to set
   */
  public final void setMessage(String inMessage) {
    this.message = inMessage;
  }

  /**
   * @return the jmsTemplate
   */
  public final JmsTemplate getJmsTemplate() {
    return jmsTemplate;
  }

  /**
   * @param inJmsTemplate the jmsTemplate to set
   */
  public final void setJmsTemplate(JmsTemplate inJmsTemplate) {
    this.jmsTemplate = inJmsTemplate;
  }

  /**
   * @return the encoding
   */
  public final String getEncoding() {
    return encoding;
  }

  /**
   * @param encoding the encoding to set
   */
  public final void setEncoding(String encoding) {
    this.encoding = encoding;
  }
}
