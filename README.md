# cli
Command line messaging tools

java -jar jmstoolkit-cli-jar-with-dncies.jar -h
Arguments:
[ -o destination ] JMS Destination JNDI name
[ -c connection factory ] JMS ConnectionFactory JNDI name
   Default values for -o and -c are set in app.properties
   Use java -Dapp.properties to change name of properties file
[ -j properties ] JNDI properties file (default: jndi.properties)
[ -e encoding ] character encoding (default: UTF-8)
[ -f file ] file to send
[ -p fifo ] read from named pipe/fifo
  If neither -p nor -f, read from stdin
[ -i id ] JMS Correlation ID
