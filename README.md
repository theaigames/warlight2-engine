warlight2-engine
============

This is the game engine for Warlight AI Challenge 2 at theaigames.com

This version of our Warlight AI Challenge 2 engine has been set up for local use, for your own convenience. Note that this does *not* include the map generator and the visualizer.

To compile (Windows, untested):

    cd [project folder]
    dir /b /s *.java>sources.txt
    md classes
    javac -d classes @sources.txt -cp lib/java-json.jar
    del sources.txt

To compile (Linux):

    cd [project folder]
    mkdir bin/
    javac -sourcepath src/ -d bin/ -cp lib/java-json.jar `find src/ -name '*.java' -regex '^[./A-Za-z0-9]*$'`
    
To run:

    cd [project folder]
    java -cp lib/java-json.jar:bin com.theaigames.game.warlight2.Warlight2 [map file] [your bot1] [your bot2] 2>err.txt 1>out.txt

[map file] is a file that contains a string representation of the map that the game will use. An example is included in this repository called "example-map.txt". For other maps, go to any Warlight AI Challenge 2 game on theaigames.com and add "/map" to the end of the URL and copy that text to a file.

[your bot1] and [your bot2] could be any command for running a bot process. For instance "java -cp /home/dev/starterbot/bin/ main.BotStarter" or "node /home/user/bot/Bot.js"

Errors will be logged to err.txt, output dump will be logged to out.txt.
