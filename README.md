# Automatic bassoon

## Build

```
./gradlew clean build
```

## Run

```
java -Dconfig=<path-to-config> -jar ./build/libs/automatic-bassoon-1.0-SNAPSHOT.jar
```

## Environment

`ZK_CONNECTION_STRING` - Zookeeper connection string containing a comma separated list of `host:port` pairs,
each corresponding to a ZooKeeper server. Default is `localhost:2181`

`HTTP_PORT` - Port webserver is running on. Default is `8080`

