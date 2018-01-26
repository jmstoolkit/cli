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

import com.jmstoolkit.Settings;
import com.jmstoolkit.JTKException;
import gnu.getopt.Getopt;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Random;
import org.springframework.context.support.ClassPathXmlApplicationContext;
/**
 * Send any number of messages and show the throughput.
 * @author scott
 */
public class Blaster extends Sender {
  /** Default number of messages to send. */
  private static final int D_MESSAGE_COUNT = 1000;
  /** Default size of messages. */
  private static final int D_MESSAGE_SIZE = 32;
  /** Default number of sending threads. */
  private static final int D_THREADS = 1;
  /** Message content for generated messages. */
  private static final String MESSAGE_CHARACTERS =
    "你好上海abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
  /** Timestamp format. */
  private static final SimpleDateFormat DATE_TIME = new SimpleDateFormat(
    "yyyy-MM-dd @ HH:mm:ss", Locale.getDefault());
  /** Time only format. */
  private static final SimpleDateFormat TIME =
    new SimpleDateFormat("mm:ss.SSS", Locale.getDefault());
  /** Message type of random. */
  private static final String TYPE_RANDOM = "random";
  /** Random text GENERATOR. */
  private static final Random GENERATOR = new Random();
  /** Default name of the application. */
  private static final String APP_NAME = "QueueBlaster";

  /** Constructor for a Blaster. */
  public Blaster() {
    super();
  }

  /**
   * @param args
   *          -optionLetter number of messages to send
   *          -f file to send
   *          -s size of generated random message
   */
  public static void main(final String[] args) {
    System.out.println("JMSToolKit - http://jmstoolkit.com/\n");
    // load the jndi.properties, if we fail exit
    try {
      Settings.loadSystemSettings(
        System.getProperty(D_JNDI_PROPERTIES, D_JNDI_PROPERTIES));
    } catch (JTKException e) {
      System.out.println(e.toStringWithStackTrace());
      System.exit(1);
    }

    // Initialize the beans
    final ClassPathXmlApplicationContext applicationContext =
      new ClassPathXmlApplicationContext(new String[]{"/app-context.xml"});
    applicationContext.start();
    final Blaster blaster = (Blaster) applicationContext.getBean(APP_NAME);

    // deal with command line arguments
    Integer messageCount = D_MESSAGE_COUNT;
    Integer messageSize = D_MESSAGE_SIZE;
    Integer numberOfThreads = D_THREADS;
    String inputFileName = "";
    final Getopt getopts = new Getopt(APP_NAME, args, "c:s:f:t:h");
    int optionLetter;
    while ((optionLetter = getopts.getopt()) != -1) {
      switch (optionLetter) {
        case 'c':
          messageCount = Integer.valueOf(getopts.getOptarg());
          break;
        case 's':
          messageSize = Integer.valueOf(getopts.getOptarg());
          break;
        case 'f':
          inputFileName = getopts.getOptarg();
          break;
        case 't':
          numberOfThreads = Integer.valueOf(getopts.getOptarg());
          break;
        case 'h':
          System.out.println("Arguments: -c count [ -s size | -f file ]");
          System.exit(1);
      }
    }

    if (messageSize != D_MESSAGE_SIZE && !inputFileName.isEmpty()) {
      System.out.println("Ignoring message size argument. Using input file.");
    }
    if (numberOfThreads > D_THREADS) {
      System.out.println("Ignoring thread count argument."
        + " Only one thread supported currently.");
    }
    if (!inputFileName.isEmpty()) {
      blaster.setMessage(blaster.loadTextFile(inputFileName));
      blaster.setMessageType(TYPE_FILE);
      System.out.println("Input file size: " + blaster.getMessageLength());
      // messageSize 0 will give us the old message format: "Blaster nnn"
    } else if (messageSize != 0) {
      blaster.setMessage(blaster.createMessage(messageSize));
      blaster.setMessageType(TYPE_RANDOM);
      System.out.println("Message size: " + blaster.getMessageLength());
    } else {
      blaster.setMessage(blaster.readLinesFromStdin());
      blaster.setMessageType(TYPE_STDIN);
    }
    System.out.println("Sending " + messageCount + " messages...");
    blaster.sendMessages(messageCount);
    System.exit(0);
  }

  /**
   * Create random text.
   * @param inSize number of letters
   * @return the text
   */
  public final String createMessage(final Integer inSize) {
    final StringBuilder messageBuilder = new StringBuilder();
    for (int i = 1; i <= inSize; i++) {
      messageBuilder.append(MESSAGE_CHARACTERS.charAt(
        GENERATOR.nextInt(MESSAGE_CHARACTERS.length())));
      if (i % 70 == 0) {
        messageBuilder.append("\n");
      }
    }
    return messageBuilder.toString();
  }

  /**
   * Sends messages as fast as the JmsTemplate can muster, and prints some
   * timing info.
   * @param messages the number of messages to send
   */
  public final void sendMessages(final Integer messages) {
    final Date start = new Date();
    System.out.println("JMS Correlation ID: " + CORRELATION_ID);
    System.out.println("Starting time: " + DATE_TIME.format(start));
    final long startTime = System.currentTimeMillis();
    for (int m = 1; m <= messages; m++) {
      sendMessage(getMessage().isEmpty() ? APP_NAME + m : getMessage(),
        new BasicMessageProcessor());
      if (m >= 100 && m % 100 == 0) {
        final long partialElapsed = System.currentTimeMillis() - startTime;
        System.out.println("  * " + m + " messages in (ms): " + partialElapsed
          + " - m/s: " +
          new Double(new Double(m) / new Double(partialElapsed) * 1000).intValue());
      }
    }
    // print the aggregate result
    final long elapsedTime = System.currentTimeMillis() - startTime;
    final Date end = new Date();
    final Date elapsed = new Date(end.getTime() - start.getTime());
    System.out.println("Ending time: " + DATE_TIME.format(end));
    System.out.println("Elapsed time: " + TIME.format(elapsed));
    System.out.println("Elapsed time (ms): " + elapsedTime);
  }
}
