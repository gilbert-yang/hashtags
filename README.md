# Tweets hashtags calculator

This application is calculating how related for a set of tweets implementing by java.

## Example:
The graph below could be constructed from the following tweets (showing hashtags
only):
- #Gretel #data
- #data #startup #privacy
- #data
- #gretel
- #rocketship #Gretel
- #cats #cats #cats
Average Degree: (3 + 2 + 2 +2 + 1) edges / 6 nodes = ~1.67


<!-- ![](image.png) -->


## How to run?
### Install maven:
#### Windows:
Follow the instruction on [Maven installation guide](https://maven.apache.org/install.html).

#### Mac:
```
brew install maven
```

#### Test maven installation
```
mvn -v
```

### Run the project

```
mvn clean package
java -cp target/graph-1.0-SNAPSHOT.jar com.example.gretel.tweets.GraphCalculator

```

## How to use?

There are 7 commands you can use when launch the calculator.

### init
This is the command to initialize the tweet graph. By default it will use the file under `src/main/java/com/example/gretel/static/tweets.txt`.


### add
This is the command to add new tweet. For simplicity, the input here is `#tag1,#tag2,#tag3`. 

### remove
This is the command to remove existing tweet. If there is no such tweet, it will return false. For simplicity, the input here is `#tag1,#tag2,#tag3`. 

### list-all
This is the command to list all existing tweets. By default it will show the first 100 tweets.

### avg
This is the command to calculate the avarage degree for the current graph.

### help
This is the commmand to show help information about how to use this calculator.

### exit
This is the command to exit the calculator.