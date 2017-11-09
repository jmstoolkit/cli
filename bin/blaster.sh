#!/bin/bash
# Scott Douglass <scott@swdouglass.com>
# License: GPLv3
# Copyright: 2017
#
BIN_DIR=$(dirname $0)
. $BIN_DIR/clirc.sh
COMMAND="com.jmstoolkit.cli.Blaster"
JAVA_OPTS="-Djava.util.logging.config.file=logging.properties"
# Change the name of the properties file:
#JAVA_OPTS="-Dapp.properties=myfile.props -Djndi.properties=some.props"
java $JAVA_OPTS $COMMAND $*

