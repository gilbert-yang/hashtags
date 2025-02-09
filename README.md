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
```brew install maven```

#### Test maven installation
```mvn -v```


### Run the project
```
mvn clean compile install test

```