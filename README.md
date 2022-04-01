
### Properties
Opprett en run configuration for Bootstrap.kt og angi properties nedenfor som environment-variable

```properties
# suppress inspection "UnusedProperty" for whole file
USE_AUTHENTICATION=false;
KAFKA_BROKERS=0.0.0.0:9092;
KAFKA_SCHEMA_REGISTRY=http://0.0.0.0:8081;
KAFKA_CONSUMER_TOPIC=aapen-person-pdl-dokument-v1;
KAFKA_CONSUMER_GROUP_ID=pdl-person-mq-adapter-group-id;
UR_MQ_PRODUCER_QUEUE=DEV.QUEUE.1;
UR_MQ_HOST=0.0.0.0;
UR_MQ_PORT=1414;
UR_MQ_QUEUE_MANAGER_NAME=mq_mngr_lokal;
UR_MQ_CHANNEL=DEV.APP.SVRCONN;
OS_MQ_PRODUCER_QUEUE=DEV.QUEUE.2;
OS_MQ_HOST=0.0.0.0;
OS_MQ_PORT=1414;
OS_MQ_QUEUE_MANAGER_NAME=mq_mngr_lokal;
OS_MQ_CHANNEL=DEV.APP.SVRCONN;
MQ_USERNAME=app;
MQ_PASSWORD=passw0rd;
HTTP_PORT=8042;
```
### docker-compose
Start kafka avhengigheter og postgres databse ved å kjøre
```
docker compose up -d
``` 
i mappen som har docker-compose.yml fila.

Det finnes en kafka produser i test pakken devtools som produserer identhendelse og en PDL mock server, denne har hardokdede responser

For å kjøre applikasjonen lokalt trengs en IBM mq server den kan snakke med

GUI er da tilgjengelig på https://0.0.0.0:9443/ibmmq/console/ med brukernav admin og passord passw0rd

