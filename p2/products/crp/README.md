# Central Reverse Proxy

This product is a reverse http and websocket proxy for a bluemind installation that can cloned.
It consists of two verticles:
- a store containing the users emails and their corresponding bluemind instance as well as the ip of these instances (`net.bluemind.central.reverse.proxy.model`).
- the proxy itself which forward requests to a bluemind instance based on a user email (`net.bluemind.central.reverse.proxy`).

The store 
- reads all its information from the kafka topics of a given installation,
- associate each user email addresses to the instance ip where the user lives,
- store those associtations in memory

The proxy query the store each time a new request comes in based on the user email. 
This email is retrieve either:
- from a cookie left by the proxy itself from a previous request
- from the basic auth header (mapi)
- from the login page form.
If none of the above match, a random ip is chosen among the bluemind instance (user not logged in yet)


## Testing

### Requirement

The product can be run locally, but requires:

- one or more bluemind installation (with users having the mailapp configured)
- a kafka instance (configured in the product via `bm.kafka.bootstrap.servers`  
like in `-Dbm.kafka.bootstrap.servers=127.0.0.1:9093`) 


##### docker-compose.yml example for kafka 

```
version: "2"

services:
  zookeeper:
    image: docker.io/bitnami/zookeeper:3.7
    ports:
      - "2181:2181"
    volumes:
      - "zookeeper_data:/bitnami"
    environment:
      - ALLOW_ANONYMOUS_LOGIN=yes
  kafka:
    image: docker.io/bitnami/kafka:2
```


### Setup
Next the required kafka topics must be created and populated in kafka.
In the following example, we assume kafka has be launched via `docker-compose` with the above
configuration, and the kafka container has `e84ab7c413e3` for id.

#### Installation information
Information about a bluemind installation are stored in a topic suffixed by `__orphans__`
(ie `installationId-__orphans__`).

Create the topic:
```
docker exec -it e84ab7c413e3 kafka-topics.sh --bootstrap-server kafka:9092 \
--create --topic installationId-__orphans__ --replication-factor 1 --partitions 4
```

Add an installation:
```
docker exec -it e84ab7c413e3 kafka-console-producer.sh --bootstrap-server kafka:9092 \
--topic installationId-__orphans__ --property "parse.key=true" \
--property "key.separator=|" --property "value.serializer=org.apache.kafka.common.serialization.ByteArrayDeserializer"

> {"type":"installation","owner":"","uid":"abc","id":1,"valueClass":"any"}|{"uid":"bm-master","value":{"ip":"192.168.xxx.xxx","tags":["bm/nginx"]}
```

Add a domain:
```
> {"type":"dir","owner":"","uid":"abc","id":3,"valueClass":"any"}|{"value":{"entry":{"dataLocation":""}}}
```
#### User information
User information are stored in a different topic, suffixed by the internal domain name 
(like in `installationId-a085866e_internal`)

Create the topic:
```
docker exec -it e84ab7c413e3 kafka-topics.sh --bootstrap-server kafka:9092 \
--create --topic installationId-a085866e_internal --replication-factor 1 --partitions 4
```

Add a user:
```
docker exec -it e84ab7c413e3 kafka-console-producer.sh --bootstrap-server kafka:9092 \
--topic installationId-a085866e_internal --property "parse.key=true" \
--property "key.separator=|" --property "value.serializer=org.apache.kafka.common.serialization.ByteArrayDeserializer"

> {"type":"dir","owner":"","uid":"abc","id":1,"valueClass":"any"}|{"value":{"value":{"emails":[{"address":"jdoe@devenv.blue","allAliases":false}]},"entry":{"dataLocation":"bm-matser"}}}
```

#### Other usefull kafka commands:

- Listing all topics:
```
docker exec -it e84ab7c413e3 kafka-topics.sh --bootstrap-server kafka:9092 \
--list
```

- Deleting a topic
```
docker exec -it e84ab7c413e3 kafka-topics.sh --bootstrap-server kafka:9092 \
--delete --topic installtionId-__orphans__
```

- Subscibing to a topic:
```
docker exec -it e84ab7c413e3 kafka-console-consumer.sh --bootstrap-server kafka:9092 \
--topic installationId-__orphans__ --from-beginning --property "print.key=true"
```

### SSL configuration
The proxy don't handle SSL. Testing the mailapp, require either:
- to use nginx in front of this proxy to handle SSL request
- or to add the `/root/dev-unsecure-cookie` file on each bluemind VM.

### Test
The proxy is available on port 8080.

With the previous configuration, requests from `jdoe@devenv.blue`, once connected, 
should be forwarded to 192.168.xxx.xxx
since the installation behind this ip hold the `bm-master` data location associated with this user.
