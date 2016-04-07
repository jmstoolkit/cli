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

package com.jmstoolkit;

import java.util.logging.Level;
import java.util.logging.Logger;
import javax.jms.ConnectionFactory;
import javax.jms.Destination;
import javax.jms.Message;
import javax.jms.MessageListener;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.jms.listener.DefaultMessageListenerContainer;
import org.springframework.jndi.JndiTemplate;

/**
 *
 * @author Scott Douglass
 */
public abstract class AbstractSpringMessageListener implements MessageListener {
  /** The logger for this class. */
  private static final Logger LOGGER =
    Logger.getLogger(AbstractSpringMessageListener.class.getName());
  /** Property for the application name. */
  public static final String P_APP_NAME = "app.name";
  /** Default value for the application name. */
  public static final String D_APP_NAME = "JMSToolKit";
  /** Default value of the name of the JNDI properties file. */
  public static final String D_JNDI_PROPERTIES = "jndi.properties";

  /** Integer value of the number of messages received. */
  private Integer messagesReceived = 0;
  /** Spring DefaultMessageListenerContainer that wraps up the JMS objects. */
  private DefaultMessageListenerContainer listenerContainer =
    new DefaultMessageListenerContainer();
  /** Spring JndiTemplate, which wraps up the JNDI objects. */
  private JndiTemplate jndiTemplate;
  /** Spring wrapper for the JMS destination objects. */
  private JmsTemplate jmsTemplate;

  @Override
  public abstract void onMessage(Message msg);

  /** Start the Spring ListenerContainer. */
  public void start() {
    if (!listenerContainer.isRunning()) {
      listenerContainer.setMessageListener(this);
      LOGGER.info("Starting listener...");
      listenerContainer.initialize();
      final ConnectionFactory connectionFactory =
        getListenerContainer().getConnectionFactory();
      if (connectionFactory == null) {
        LOGGER.log(Level.SEVERE,
          "Message Listener Container has null connection factory.");
      } else {
        getListenerContainer().start();
      if (!listenerContainer.isRunning()) {
        LOGGER.log(Level.SEVERE, "Message Listener Container did  not start.");
      }
      }
    }
  }

  /** Shutdown the Spring ListenerContainer.*/
  public void stop() {
    if (getListenerContainer().isRunning()) {
      LOGGER.info("Stopping listener...");
      getListenerContainer().stop(new Stop());
      //listenerContainer.shutdown();
    }
  }

  /**
   * @return the connectionFactory
   */
  public final ConnectionFactory getConnectionFactory() {
    return this.getListenerContainer().getConnectionFactory();
  }

  /**
   * @return the jndiTemplate
   */
  public final JndiTemplate getJndiTemplate() {
    return jndiTemplate;
  }

  /**
   * @param inJndiTemplate the jndiTemplate to set
   */
  public final void setJndiTemplate(final JndiTemplate inJndiTemplate) {
    this.jndiTemplate = inJndiTemplate;
  }

  /**
   * @param inMessagesReceived the number of messages received
   */
  public final void setMessagesReceived(final Integer inMessagesReceived) {
    this.messagesReceived = inMessagesReceived;
  }

  /** Class which is passed to the Spring listener to shut it down. */
  private class Stop implements Runnable {

    @Override
    public void run() {
      LOGGER.info("MessageListener shut down.");
    }
  }

  /**
   * @return the messagesReceived
   */
  public final Integer getMessagesReceived() {
    return messagesReceived;
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
  public final void setJmsTemplate(final JmsTemplate inJmsTemplate) {
    this.jmsTemplate = inJmsTemplate;
  }

  /**
   * @return The ListenerContainer
   */
  public final DefaultMessageListenerContainer getListenerContainer() {
    return listenerContainer;
  }

  /**
   * @param inListenerContainer the listenerContainer to set
   */
  public final void setListenerContainer(
    final DefaultMessageListenerContainer inListenerContainer) {
    this.listenerContainer = inListenerContainer;
  }

  /** @return true if the Spring listener is running. */
  public final Boolean isRunning() {
    return this.getListenerContainer().isRunning();
  }

  /**
   * @param inDestination The JMS destination for the Spring listener.
   */
  public final void setDestination(final Destination inDestination) {
    getListenerContainer().setDestination(inDestination);
  }

  /**
   * @param inConnectionFactory The JMS ConnectionFactory for the listener.
   */
  public final void setConnectionFactory(
    final ConnectionFactory inConnectionFactory) {
    getListenerContainer().setConnectionFactory(inConnectionFactory);
  }

}
