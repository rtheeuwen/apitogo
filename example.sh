#!/bin/bash
nc -z localhost 8080 >/dev/null 2>/dev/null
if [ $? -eq 0 ];
then
    >&2 echo 'Another process is listening on port 8080.'
    exit
fi
clear
mvn clean package assembly:assembly
java -classpath ./target/test-classes:./target/apitogo-0.1-alpha-jar-with-dependencies.jar api.to.go.Example &
JAVA_PID=$!
sleep 3

curl -d "{\"id\":0,\"name\":\"Person A\",\"contacts\":[{\"id\":0,\"name\":\"Contact A\",\"phones\":[{\"id\":0,\"number\":123},{\"id\":0,\"number\":345}]},{\"id\":0,\"name\":\"ContactB\",\"phones\":[{\"id\":0,\"number\":567},{\"id\":0,\"number\":789}]}]}" localhost:8080/persons>/dev/null 2> /dev/null
curl -d "{\"id\":0,\"name\":\"Person B\",\"contacts\":[{\"id\":0,\"name\":\"Contact C\",\"phones\":[{\"id\":0,\"number\":123},{\"id\":0,\"number\":345}]},{\"id\":0,\"name\":\"Contact D\",\"phones\":[{\"id\":0,\"number\":567},{\"id\":0,\"number\":789}]}]}" localhost:8080/persons>/dev/null 2> /dev/null

printf '\e[1;36m\n%s\n\n\t%s\n\t%s\n\t%s\n\n%s\e[m\n' "Example application is running at http://localhost:8080" "See: http://localhost:8080/persons" "See: http://localhost:8080/numberOfContacts?person=1" "See: http://localhost:8080/contactsInCommon?1=1&2=2" "Press [return] to exit."
read input
kill $JAVA_PID
