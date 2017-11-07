#!/bin/bash
JAR="jmstoolkit-cli-jar-with-dependencies.jar"
CLASSPATH="`pwd`/${JAR}:${CLASSPATH}"
#COMMAND="-jar $JAR"
# Set to the directory where your JMS provider jar files are
#JMS_PROVIDER_DIR=`pwd`/activemq
if [ "X${JMS_PROVIDER_DIR}" != "X" ]; then
  for J in `ls ${JMS_PROVIDER_DIR}/*.jar`; do
    CLASSPATH=${J}:${CLASSPATH}
  done
fi
for J in `ls lib/*.jar 2>/dev/null`; do
  CLASSPATH=${J}:${CLASSPATH}
done
export CLASSPATH
COMMAND="com.jmstoolkit.cli.Sender"

JAVA_OPTS="-Djava.util.logging.config.file=logging.properties $JAVA_OPTS"
# Change the name of the properties file:
#JAVA_OPTS="-Dapp.properties=myfile.props -Djndi.properties=some.props"


FIFO="$1"
if [ ! -p "$FIFO" ]; then
  mkfifo "$FIFO"
fi
shift

java -classpath $CLASSPATH $JAVA_OPTS $COMMAND -p $FIFO $*

