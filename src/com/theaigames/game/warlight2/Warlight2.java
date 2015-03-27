// Copyright 2015 theaigames.com (developers@theaigames.com)

//    Licensed under the Apache License, Version 2.0 (the "License");
//    you may not use this file except in compliance with the License.
//    You may obtain a copy of the License at

//        http://www.apache.org/licenses/LICENSE-2.0

//    Unless required by applicable law or agreed to in writing, software
//    distributed under the License is distributed on an "AS IS" BASIS,
//    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
//    See the License for the specific language governing permissions and
//    limitations under the License.
//	
//    For the full copyright and license information, please view the LICENSE
//    file that was distributed with this source code.

package com.theaigames.game.warlight2;

import java.io.*;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Scanner;

import java.lang.Thread;
import java.util.zip.*;
import java.util.Properties;
import java.net.URL;

import com.theaigames.engine.Engine;
import com.theaigames.engine.Logic;
import com.theaigames.engine.io.IOPlayer;

import com.theaigames.game.warlight2.move.AttackTransferMove;
import com.theaigames.game.warlight2.move.MoveResult;
import com.theaigames.game.warlight2.move.PlaceArmiesMove;
import com.theaigames.game.warlight2.map.Map;

/**
 * Warlight2 class
 * 
 * Main class for Warlight2
 * 
 * @author Jim van Eeden <jim@starapple.nl>
 */

public class Warlight2 implements Logic
{
	private String playerName1, playerName2;
	private final String mapFile;

	private Processor processor;
	private Player player1, player2;
	private int maxRounds;

	private String secretKey, accessKey;
	
	private final int STARTING_ARMIES = 5;
	private final long TIMEBANK_MAX = 10000l;
	private final long TIME_PER_MOVE = 500l;
	private final int SIZE_WASTELANDS = 6; // size of wastelands, <= 0 for no wastelands

	public Warlight2(String mapFile)
	{
		this.mapFile = mapFile;
		this.playerName1 = "player1";
		this.playerName2 = "player2";
	}
	
	
	/**
	 * sets up everything that's needed before a round can be played
	 * @param players : list of bots that have already been initialized
	 */
	@Override
    public void setupGame(ArrayList<IOPlayer> players) throws IncorrectPlayerCountException, IOException {
		
		Map initMap, map;
		
		System.out.println("setting up game");
		
        // Determine array size is two players
        if (players.size() != 2) {
            throw new IncorrectPlayerCountException("Should be two players");
        }
        
        this.player1 = new Player(playerName1, players.get(0), STARTING_ARMIES, TIMEBANK_MAX, TIME_PER_MOVE);
        this.player2 = new Player(playerName2, players.get(1), STARTING_ARMIES, TIMEBANK_MAX, TIME_PER_MOVE);
        
        // get map string from database and setup the map
  		initMap = MapCreator.createMap(getMapString());
  		map = MapCreator.setupMap(initMap, SIZE_WASTELANDS);
  		this.maxRounds = MapCreator.determineMaxRounds(map);
  		
  		// start the processor
  		System.out.println("Starting game...");
  		this.processor = new Processor(map, player1, player2);
	
  		sendSettings(player1);
  		sendSettings(player2);
  		MapCreator.sendSetupMapInfo(player1, map);
  		MapCreator.sendSetupMapInfo(player2, map);

  		player1.setTimeBank(TIMEBANK_MAX);
		player2.setTimeBank(TIMEBANK_MAX);

  		this.processor.distributeStartingRegions(); //decide the player's starting regions
		this.processor.recalculateStartingArmies(); //calculate how much armies the players get at the start of the round (depending on owned SuperRegions)
		this.processor.sendAllInfo();
    }
	
	
	/**
	 * play one round of the game
	 * @param roundNumber : round number
	 */
	@Override
    public void playRound(int roundNumber) 
	{
		player1.getBot().addToDump(String.format("Round %d\n", roundNumber));
		player2.getBot().addToDump(String.format("Round %d\n", roundNumber));
		
		this.processor.playRound(roundNumber);
	}
	
	
	/**
	 * @return : True when the game is over
	 */
	@Override
    public boolean isGameWon()
	{
        if (this.processor.getWinner() != null || this.processor.getRoundNr() > this.maxRounds) {
        	return true;
        }
        return false;
    }
	
	/**
	 * Sends all game settings to given player
	 * @param player : player to send settings to
	 */
	private void sendSettings(Player player) {
		player.sendInfo("settings timebank " + TIMEBANK_MAX);
		player.sendInfo("settings time_per_move " + TIME_PER_MOVE); 
		player.sendInfo("settings max_rounds " + this.maxRounds);
		player.sendInfo("settings your_bot " + player.getName());
		
		if (player.getName().equals(player1.getName()))
			player.sendInfo("settings opponent_bot " + player2.getName());
		else
			player.sendInfo("settings opponent_bot " + player1.getName());
	}
	
	/**
	 * Reads the string from the map file
	 * @return : string representation of the map
	 * @throws IOException
	 */
	private String getMapString() throws IOException 
	{
		File file = new File(this.mapFile);
	    StringBuilder fileContents = new StringBuilder((int) file.length());
	    Scanner scanner = new Scanner(file);
	    String lineSeparator = System.getProperty("line.separator");

	    try {
	        while(scanner.hasNextLine()) {        
	            fileContents.append(scanner.nextLine() + lineSeparator);
	        }
	        return fileContents.toString();
	    } finally {
	        scanner.close();
	    }
	}
	
	/**
	 * close the bot processes, save, exit program
	 */
	@Override
	public void finish() throws Exception
	{
		this.player1.getBot().finish();
		this.player2.getBot().finish();
		Thread.sleep(100);

		// write everything
		try { 
			this.saveGame(); 
		} catch(Exception e) {
			e.printStackTrace();
		}
		
		System.out.println("Done.");
		
        System.exit(0);
	}

	/**
	 * Turns the game that is stored in the processor to a nice string for the visualization
	 * @param winner : winner
	 * @param gameView : type of view
	 * @return : string that the visualizer can read
	 */
	private String getPlayedGame(Player winner, String gameView)
	{
		StringBuilder out = new StringBuilder();		

		LinkedList<MoveResult> playedGame;
		if(gameView.equals("player1"))
			playedGame = this.processor.getPlayer1PlayedGame();
		else if(gameView.equals("player2"))
			playedGame = this.processor.getPlayer2PlayedGame();
		else
			playedGame = this.processor.getFullPlayedGame();
			
		playedGame.removeLast();
		int roundNr = 0;
		for(MoveResult moveResult : playedGame)
		{
			if(moveResult != null)
			{
				if(moveResult.getMove() != null)
				{
					try {
						PlaceArmiesMove plm = (PlaceArmiesMove) moveResult.getMove();
						out.append(plm.getString() + "\n");
					}
					catch(Exception e) {
						AttackTransferMove atm = (AttackTransferMove) moveResult.getMove();
						out.append(atm.getString() + "\n");
					}
					
				}
				out.append("map " + moveResult.getMap().getMapString() + "\n");
			}
			else
			{
				out.append("round " + roundNr + "\n");
				roundNr++;
			}
		}
		
		if(winner != null)
			out.append(winner.getName() + " won\n");
		else
			out.append("Nobody won\n");

		return out.toString();
	}


	/**
	 * Does everything that is needed to store the output of a game
	 */
	public void saveGame() {
		
		Player winner = this.processor.getWinner();
		int score = this.processor.getRoundNr() - 1;
		
		if(winner != null) {
			System.out.println("winner: " + winner.getName());
		} else {
			System.out.println("winner: draw");
		}
		
		System.out.println("Saving the game...");
		// do stuff here if you want to save results
	}
	
	/**
	 * main
	 * @param args : the map file should be given, along with the commands that start the bot processes
	 * @throws Exception
	 */
	public static void main(String args[]) throws Exception
	{	
		String mapFile = args[0];
		String bot1Cmd = args[1];
		String bot2Cmd = args[2];

		// Construct engine
        Engine engine = new Engine();
        
        // Set logic
        engine.setLogic(new Warlight2(mapFile));
		
        // Add players
        engine.addPlayer(bot1Cmd);
        engine.addPlayer(bot2Cmd);
		
        engine.start();
	}
}
