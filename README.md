# Apache Karaf Patch example

This is a very simple example of patch bundle, embedding some resources used to update the runtime, and using
some services (bundles, features, configuration, ...).

## Build

```
mvn clean install
```

## Installation

Just install the patch bundle using:

```
karaf@root()> bundle:install -s mvn:net.nanthrax.example/path/1.1-SNAPSHOT
```

or 

```
karaf@root()> bundle:install -s http://..../patch-1.1-SNAPSHOT.jar
```
