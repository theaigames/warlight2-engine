poker-engine
============

Poker engine for Heads-up Omaha at TheAIGames.com

This version of our Poker engine has been set up for local use, for your own convenience. Notice that this engine is not *only* for Heads-up Omaha and can play other versions of poker as well. We will probably host other poker competitions in the future.

To compile (Linux):

    cd [project folder]
    javac -sourcepath src/ -d bin/ `find src/ -name '*.java' -regex '^[./A-Za-z0-9]*$'`
    
To run:

    java nl.starapple.backend.RunPoker 2000 [your bot1] [your bot2] 2>err.txt 1>out.txt
    
[your bot1] could be any command for running a bot process. For instance "java main.BotStarter" or "node /home/user/bot/Bot.js"

2000 is the starting stack for both bots.

Errors will be logged to err.txt, output dump will be logged to out.txt.
