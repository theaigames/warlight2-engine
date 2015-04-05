warlight2-engine
================

This is the game engine for Warlight AI Challenge 2 at theaigames.com

This version of our Warlight AI Challenge 2 engine has been set up for local use, for your own convenience. Note that this does *not* include the map generator and the visualizer.

Usage
-----

### Download

You can download the latest version of the Warlight2 Engine here: [warlight2-engine-1.0.0.jar](https://github.com/flungo/warlight2-engine/releases/download/v1.0.0/warlight2-engine-1.0.0.jar). Find the all downloads (including older versions) on the [release page](https://github.com/flungo/warlight2-engine/releases).

#### Running the jar

The downloaded `.jar` file is easily run on any OS with the Java Virtual Machine (JVM) installed with the following:

```
java -jar warlight2-engine-1.0.0.jar [map file] [your bot1] [your bot2] 2>err.txt 1>out.txt
```

-	`[map file]` is a file that contains a string representation of the map that the game will use. An example is included in this repository called `example-map.txt`. For other maps, go to any Warlight AI Challenge 2 game on theaigames.com and add `/map` to the end of the URL and copy that text to a file.
-	`[your bot1]` and `[your bot2]` could be any command for running a bot process. For instance `java -cp /home/dev/starterbot/bin/ main.BotStarter` or `node /home/user/bot/Bot.js`

Errors will be logged to `err.txt`, output dump will be logged to `out.txt`.

### Build from source

#### Getting the source

If you have `git` installed the easiest way to get the source is to clone the repository:

```
git clone https://github.com/flungo/warlight2-engine
```

Alternatively, you can download the repository as a [zip](https://github.com/flungo/warlight2-engine/archive/master.zip) or [tar.gz](https://github.com/flungo/warlight2-engine/archive/master.tar.gz) and extract using the appropriate tools for your OS.

#### Compiling the source

Compilation is made simple regardless of platform through [maven](https://maven.apache.org). As long as you have [maven installed](https://maven.apache.org/download.cgi), you should be able to build it with the following:

```
cd warlight2-engine
mvn build
```

#### Running compiled jar

After building from source, the output file will be in the `target` folder. To run you need to do the following command:

```
java -jar target/warlight2-engine-1.0.0-jar-with-dependencies.jar [map file] [your bot1] [your bot2] 2>err.txt 1>out.txt
```

For information on the run arguments, see [Run Section](#running-the-jar)
