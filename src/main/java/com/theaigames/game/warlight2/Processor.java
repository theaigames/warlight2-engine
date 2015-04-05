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

import java.util.ArrayList;
import java.util.LinkedList;

import com.theaigames.game.warlight2.map.Map;
import com.theaigames.game.warlight2.map.Region;
import com.theaigames.game.warlight2.map.SuperRegion;
import com.theaigames.game.warlight2.move.AttackTransferMove;
import com.theaigames.game.warlight2.move.Move;
import com.theaigames.game.warlight2.move.MoveQueue;
import com.theaigames.game.warlight2.move.MoveResult;
import com.theaigames.game.warlight2.move.PlaceArmiesMove;

/**
 * Processor class
 * 
 * @author Jim van Eeden <jim@starapple.nl>
 */

public class Processor {
	
	private Player player1;
	private Player player2;
	private Map map;
	private Parser parser;
	private int roundNr;
	private LinkedList<MoveResult> pickedStartingRegions;
	private LinkedList<MoveResult> fullPlayedGame;
	private LinkedList<MoveResult> player1PlayedGame;
	private LinkedList<MoveResult> player2PlayedGame;
	private LinkedList<Move> opponentMovesPlayer1;
	private LinkedList<Move> opponentMovesPlayer2;
	private MoveQueue moveQueue;
	private String pickableStartingRegionsString;
	
	private final double LUCK_MODIFIER = 0.16;
	private final int MINIMAL_STARTING_PICKS = 6;

	public Processor(Map initMap, Player player1, Player player2)
	{
		this.map = initMap;
		this.player1 = player1;
		this.player2 = player2;
		moveQueue = new MoveQueue(player1, player2);
		
		parser = new Parser(map);

		fullPlayedGame = new LinkedList<MoveResult>();
		player1PlayedGame = new LinkedList<MoveResult>();
		player2PlayedGame = new LinkedList<MoveResult>();
		opponentMovesPlayer1 = new LinkedList<Move>();
		opponentMovesPlayer2 = new LinkedList<Move>();
		pickableStartingRegionsString = "";

		fullPlayedGame.add(new MoveResult(null, map.getMapCopy())); //empty map
		player1PlayedGame.add(new MoveResult(null, map.getMapCopy()));
		player2PlayedGame.add(new MoveResult(null, map.getMapCopy()));
		fullPlayedGame.add(null); //round 0
		player1PlayedGame.add(null);
		player2PlayedGame.add(null);
	}
	
	/**
	 * asks in a ABBAAB fashion where the players would like to start, 
	 * each superRegion could get one random region that can be picked, but
	 * some randomness decides if it will be less than that
	 */
	public void distributeStartingRegions() 
	{
		ArrayList<Region> pickableRegions = new ArrayList<Region>();
		ArrayList<Region> player1Regions = new ArrayList<Region>();
		ArrayList<Region> player2Regions = new ArrayList<Region>();
		
		// get one random region from each superRegion
		for(SuperRegion superRegion : map.getSuperRegions())
		{
			// wastelands can't be picked
			LinkedList<Region> nonWasteLandRegions = new LinkedList<Region>();
			for (Region region : superRegion.getSubRegions()) {
				if (region.getArmies() == 2)
					nonWasteLandRegions.add(region);
			}

			int nrOfRegions = nonWasteLandRegions.size();
			if (nrOfRegions > 0) {
				double rand = Math.random();
				int index = (int) (rand*nrOfRegions);
				Region randomRegion = nonWasteLandRegions.get(index);
				pickableRegions.add(randomRegion);
				pickableStartingRegionsString += randomRegion.getId() + " "; 
			}
		}

		int superRegionAmount = pickableRegions.size();
		int nrOfPicks = getAmountOfStartingPicks(superRegionAmount);
		int i = 0;
		int k;
		Player currentPlayer;
		
		sendStartingRegionsInfO(player1, pickableRegions, true);
		sendStartingRegionsInfO(player2, pickableRegions, true);
		sendStartingRegionPickAmount(player1, nrOfPicks / 2);
		sendStartingRegionPickAmount(player2, nrOfPicks / 2);

		while(i < nrOfPicks) {
			if (i % 4 <= 1) {
				k = 0;
			} else {
				k = 1;
			}
			if (i % 2 == k) {
				currentPlayer = player1;
			} else {
				currentPlayer = player2;
			}
			
			Region region = parser.parseStartingRegion(currentPlayer.requestStartingArmies(pickableRegions), pickableRegions, currentPlayer);
			if(region == null) { // get random region
				double rand = Math.random();
				int index = (int) (rand*pickableRegions.size());
				region = pickableRegions.get(index);
			}
			
			if(currentPlayer == player1)
				player1Regions.add(region);
			else
				player2Regions.add(region);

			region.setPlayerName(currentPlayer.getName());

			// storing the picking phase for output
			PlaceArmiesMove pickMove = new PlaceArmiesMove(currentPlayer.getName(), region, 2);
			fullPlayedGame.add(new MoveResult(pickMove, map.getMapCopy()));
			player1PlayedGame.add(new MoveResult(pickMove, map.getMapCopy()));
			player2PlayedGame.add(new MoveResult(pickMove, map.getMapCopy()));

			pickableRegions.remove(region);
			i++;
		}

		sendStartingRegionsInfO(player1, player2Regions, false);
		sendStartingRegionsInfO(player2, player1Regions, false);
		
		// start of the output for after the picking phase
		fullPlayedGame.add(new MoveResult(null, map.getMapCopy()));
		player1PlayedGame.add(new MoveResult(null, map.getVisibleMapCopyForPlayer(player1)));
		player2PlayedGame.add(new MoveResult(null, map.getVisibleMapCopyForPlayer(player2)));
		fullPlayedGame.add(null);
		player1PlayedGame.add(null);
		player2PlayedGame.add(null);
	}

	/**
	 * Get the amount of picks that the two players can make from the avaiable starting regions
	 * minimum of 3 picks each (= 6 total)
	 * @param availablePicks : total picks available
	 * @return : less than total picks
	 */
	private int getAmountOfStartingPicks(int availablePicks)
	{
		int actualPicks = availablePicks;

		if (actualPicks % 2 != 0)
			actualPicks--;

		if (actualPicks <= MINIMAL_STARTING_PICKS)
			return actualPicks;

		int k = (actualPicks - MINIMAL_STARTING_PICKS) / 2;
		for (int i=0; i<k; i++) { 
			double rand = Math.random();
			if(rand < 0.25) { // 0.25 chance amount is decremented by 1 for each player
				actualPicks -= 2;
			}
		}

		return actualPicks;
	}
	
	/**
	 * Plays one round of the game
	 * @param roundNumber
	 */
	public void playRound(int roundNumber)
	{
		this.roundNr = roundNumber;
		
		getMoves(player1.requestPlaceArmiesMoves(), player1);
		getMoves(player2.requestPlaceArmiesMoves(), player2);
		
		executePlaceArmies();
		
		getMoves(player1.requestAttackTransferMoves(), player1);
		getMoves(player2.requestAttackTransferMoves(), player2);
		
		executeAttackTransfer();
		
		moveQueue.clear();
		recalculateStartingArmies();
		sendAllInfo();	
		fullPlayedGame.add(null); //indicates round end	
		player1PlayedGame.add(null);
		player2PlayedGame.add(null);
		roundNr++;	
	}
	
	/**
	 * Queues the moves given by the player
	 * @param movesInput : bot's output
	 * @param player : player who the output belongs to
	 */
	private void getMoves(String movesInput, Player player)
	{
		ArrayList<Move> moves = parser.parseMoves(movesInput, player);
		
		for(Move move : moves)
		{
			try //PlaceArmiesMove
			{
				PlaceArmiesMove plm = (PlaceArmiesMove) move;
				queuePlaceArmies(plm);
			}
			catch(Exception e) //AttackTransferMove
			{
				AttackTransferMove atm = (AttackTransferMove) move;
				queueAttackTransfer(atm);
			}
		}
	}

	/**
	 * Queues the placeArmies moves given by the player
	 * Checks if the moves are legal
	 * @param plm : placeArmiesMove to be queued
	 */
	private void queuePlaceArmies(PlaceArmiesMove plm)
	{
		//should not ever happen
		if(plm == null) { System.err.println("Error on place_armies input."); return; }
		
		Region region = plm.getRegion();
		Player player = getPlayer(plm.getPlayerName());
		int armies = plm.getArmies();
		
		//check legality
		if(region.ownedByPlayer(player.getName()))
		{
			if(armies < 1)
			{
				plm.setIllegalMove(" place-armies " + "cannot place less than 1 army");
			}
			else
			{
				if(armies > player.getArmiesLeft()) //player wants to place more armies than he has left
					plm.setArmies(player.getArmiesLeft()); //place all armies he has left
				if(player.getArmiesLeft() <= 0)
					plm.setIllegalMove(" place-armies " + "no armies left to place");
				
				player.setArmiesLeft(player.getArmiesLeft() - plm.getArmies());
			}
		}
		else 
			plm.setIllegalMove(plm.getRegion().getId() + " place-armies " + " not owned");

		moveQueue.addMove(plm);
	}
	
	/**
	 * Queues the attackTransfer moves given by the player
	 * Does the first checks for legality
	 * @param atm : attackTransferMove to be queued
	 */
	private void queueAttackTransfer(AttackTransferMove atm)
	{
		//should not ever happen
		if(atm == null){ System.err.println("Error on attack/transfer input."); return; }
		
		Region fromRegion = atm.getFromRegion();
		Region toRegion = atm.getToRegion();
		Player player = getPlayer(atm.getPlayerName());
		int armies = atm.getArmies();
		
		//check legality
		if(fromRegion.ownedByPlayer(player.getName()))
		{
			if(fromRegion.isNeighbor(toRegion))
			{
				if(armies < 1)
					atm.setIllegalMove(" attack/transfer " + "cannot use less than 1 army");
			}
			else
				atm.setIllegalMove(atm.getToRegion().getId() + " attack/transfer " + "not a neighbor");
		}
		else
			atm.setIllegalMove(atm.getFromRegion().getId() + " attack/transfer " + "not owned");

		moveQueue.addMove(atm);
	}
	
	/**
	 * Executes all placeArmies move currently in the queue
	 * Moves have already been checked if they are legal
	 * Also stores the moves for the visualizer
	 */
	private void executePlaceArmies()
	{
		for(PlaceArmiesMove move : moveQueue.placeArmiesMoves)
		{
			if(move.getIllegalMove().equals("")) //the move is not illegal
				move.getRegion().setArmies(move.getRegion().getArmies() + move.getArmies());
			
			Map mapCopy = map.getMapCopy();
			fullPlayedGame.add(new MoveResult(move, mapCopy));
			if(map.visibleRegionsForPlayer(player1).contains(move.getRegion()))
			{
				player1PlayedGame.add(new MoveResult(move, map.getVisibleMapCopyForPlayer(player1))); //for the game file
				if(move.getPlayerName().equals(player2.getName()))
					opponentMovesPlayer1.add(move); //for the opponent_moves output
			}
			if(map.visibleRegionsForPlayer(player2).contains(move.getRegion()))
			{
				player2PlayedGame.add(new MoveResult(move, map.getVisibleMapCopyForPlayer(player2))); //for the game file
				if(move.getPlayerName().equals(player1.getName()))
					opponentMovesPlayer2.add(move); //for the opponent_moves output
			}
		}
	}

	/**
	 * Executes all attackTransfer moves currently in the queue
	 * Does a lot of legality checks and determines whether it is an attack or a transfer
	 * Also stores the moves for the visualizer
	 */
	private void executeAttackTransfer()
	{
		LinkedList<Region> visibleRegionsPlayer1Map = map.visibleRegionsForPlayer(player1);
		LinkedList<Region> visibleRegionsPlayer2Map = map.visibleRegionsForPlayer(player2);
		LinkedList<Region> visibleRegionsPlayer1OldMap = visibleRegionsPlayer1Map;
		LinkedList<Region> visibleRegionsPlayer2OldMap = visibleRegionsPlayer2Map;
		ArrayList<ArrayList<Integer>> usedRegions = new ArrayList<ArrayList<Integer>>();
		for(int i = 0; i <= map.getRegions().size(); i++) {
			usedRegions.add(new ArrayList<Integer>());
		}
		Map oldMap = map.getMapCopy();

		int moveNr = 1;
		Boolean previousMoveWasIllegal = false;
		String previousMovePlayer = "";
		while(moveQueue.hasNextAttackTransferMove())
		{	
			AttackTransferMove move = moveQueue.getNextAttackTransferMove(moveNr, previousMovePlayer, previousMoveWasIllegal);

			if(move.getIllegalMove().equals("")) //the move is not illegal
			{
				Region fromRegion = move.getFromRegion();
				Region oldFromRegion = oldMap.getRegion(move.getFromRegion().getId());
				Region oldToRegion = oldMap.getRegion(move.getToRegion().getId());
				Region toRegion = move.getToRegion();
				Player player = getPlayer(move.getPlayerName());
				
				if(fromRegion.ownedByPlayer(player.getName())) //check if the fromRegion still belongs to this player
				{
					if(!usedRegions.get(fromRegion.getId()).contains(toRegion.getId())) //between two regions there can only be attacked/transfered once
					{
						if(oldFromRegion.getArmies() > 1) //there are still armies that can be used
						{
							if(oldFromRegion.getArmies() < fromRegion.getArmies() && oldFromRegion.getArmies() - 1 < move.getArmies()) //not enough armies on fromRegion at the start of the round?
								move.setArmies(oldFromRegion.getArmies() - 1); //move the maximal number.
							else if(oldFromRegion.getArmies() >= fromRegion.getArmies() && fromRegion.getArmies() - 1 < move.getArmies()) //not enough armies on fromRegion currently?
								move.setArmies(fromRegion.getArmies() - 1); //move the maximal number.

							oldFromRegion.setArmies(oldFromRegion.getArmies() - move.getArmies()); //update oldFromRegion so new armies cannot be used yet

							if(toRegion.ownedByPlayer(player.getName())) //transfer
							{
								if(fromRegion.getArmies() > 1)
								{
									fromRegion.setArmies(fromRegion.getArmies() - move.getArmies());
									toRegion.setArmies(toRegion.getArmies() + move.getArmies());
									usedRegions.get(fromRegion.getId()).add(toRegion.getId());
								}
								else
									move.setIllegalMove(move.getFromRegion().getId() + " transfer " + "only has 1 army");
							}
							else //attack
							{
								int armiesDestroyed = doAttack(move);
								if(armiesDestroyed == 0) { //attack was succes
									oldToRegion.setArmies(1); //region was taken, so cannot be used anymore, even if it's taken back.
								} else if(armiesDestroyed > 0) { //attack failed
									oldToRegion.setArmies(oldToRegion.getArmies() - armiesDestroyed); //armies destroyed and replaced cannot be used again this turn
								}
								usedRegions.get(fromRegion.getId()).add(toRegion.getId());
							}
						}
						else
							move.setIllegalMove(move.getFromRegion().getId() + " attack/transfer " + "has used all available armies");
					}
					else
						move.setIllegalMove(move.getFromRegion().getId() + " attack/transfer " + "has already attacked/transfered to this region");
				}
				else
					move.setIllegalMove(move.getFromRegion().getId() + " attack/transfer " + "was taken this round");
			}

			visibleRegionsPlayer1Map = map.visibleRegionsForPlayer(player1);
			visibleRegionsPlayer2Map = map.visibleRegionsForPlayer(player2);
			
			fullPlayedGame.add(new MoveResult(move, map.getMapCopy()));
			if(visibleRegionsPlayer1Map.contains(move.getFromRegion()) || visibleRegionsPlayer1Map.contains(move.getToRegion()) ||
					visibleRegionsPlayer1OldMap.contains(move.getToRegion()))
			{
				player1PlayedGame.add(new MoveResult(move, map.getVisibleMapCopyForPlayer(player1))); //for the game file
				if(move.getPlayerName().equals(player2.getName()))
					opponentMovesPlayer1.add(move); //for the opponent_moves output
			}
			if(visibleRegionsPlayer2Map.contains(move.getFromRegion()) || visibleRegionsPlayer2Map.contains(move.getToRegion()) ||
					visibleRegionsPlayer2OldMap.contains(move.getToRegion()))
			{
				player2PlayedGame.add(new MoveResult(move, map.getVisibleMapCopyForPlayer(player2))); //for the game file
				if(move.getPlayerName().equals(player1.getName()))
					opponentMovesPlayer2.add(move); //for the opponent_moves output
			}
			
			visibleRegionsPlayer1OldMap = visibleRegionsPlayer1Map;
			visibleRegionsPlayer2OldMap = visibleRegionsPlayer2Map;

			//set some stuff to know what next move to get
			if(move.getIllegalMove().equals("")) {
				previousMoveWasIllegal = false;
				moveNr++;
			}
			else {
				previousMoveWasIllegal = true;
			}
			previousMovePlayer = move.getPlayerName();
		}
	}
	
	/**
	 * Processes the result of an attack
	 * see wiki.warlight.net/index.php/Combat_Basics
	 * @param move : attackTransfer move
	 * @return : amount of defenders destroyed, used for correcting coming moves
	 */
	private int doAttack(AttackTransferMove move)
	{
		Region fromRegion = move.getFromRegion();
		Region toRegion = move.getToRegion();
		int attackingArmies;
		int defendingArmies = toRegion.getArmies();
		
		int defendersDestroyed = 0;
		int attackersDestroyed = 0;
		
		if(fromRegion.getArmies() > 1)
		{
			if(fromRegion.getArmies()-1 >= move.getArmies()) //are there enough armies on fromRegion?
				attackingArmies = move.getArmies();
			else
				attackingArmies = fromRegion.getArmies()-1;
			
			for(int t=1; t<=attackingArmies; t++) //calculate how much defending armies are destroyed with 100% luck
			{
				double rand = Math.random();
				if(rand < 0.6) //60% chance to destroy one defending army
					defendersDestroyed++;
			}
			for(int t=1; t<=defendingArmies; t++) //calculate how much attacking armies are destroyed with 100% luck
			{
				double rand = Math.random();
				if(rand < 0.7) //70% chance to destroy one attacking army
					attackersDestroyed++;
			}
			
			// apply luck modifier to get actual amount of destroyed armies
			// straight round method is used (instead of weighted random round)
			defendersDestroyed = (int) Math.round( ((attackingArmies * 0.6) * (1 - LUCK_MODIFIER)) 
											+ (defendersDestroyed * LUCK_MODIFIER) );
			attackersDestroyed = (int) Math.round( ((defendingArmies * 0.7) * (1 - LUCK_MODIFIER)) 
											+ (attackersDestroyed * LUCK_MODIFIER) );
			
			if(attackersDestroyed >= attackingArmies)
			{
				if(defendersDestroyed >= defendingArmies)
					defendersDestroyed = defendingArmies - 1;
				
				attackersDestroyed = attackingArmies;
			}		
			
			//process result of attack
			if(defendersDestroyed >= defendingArmies) //attack success
			{
				fromRegion.setArmies(fromRegion.getArmies() - attackingArmies);
				toRegion.setPlayerName(move.getPlayerName());
				toRegion.setArmies(attackingArmies - attackersDestroyed);
				return 0;

			}
			else //attack fail
			{
				fromRegion.setArmies(fromRegion.getArmies() - attackersDestroyed);
				toRegion.setArmies(toRegion.getArmies() - defendersDestroyed);
				return defendersDestroyed;
			}
			
		}
		else
			move.setIllegalMove(move.getFromRegion().getId() + " attack " + "only has 1 army");

		return -1;
	}
	
	/**
	 * @return : the winner of the game, null if the game is not over
	 */
	public Player getWinner()
	{
		if(map.ownedRegionsByPlayer(player1).isEmpty())
			return player2;
		else if(map.ownedRegionsByPlayer(player2).isEmpty())
			return player1;
		else
			return null;
	}
	
	/**
	 * Calculates how many armies each player is able to place on the map for the next round
	 */
	public void recalculateStartingArmies()
	{
		player1.setArmiesLeft(player1.getArmiesPerTurn());
		player2.setArmiesLeft(player2.getArmiesPerTurn());
		
		for(SuperRegion superRegion : map.getSuperRegions())
		{
			Player player = getPlayer(superRegion.ownedByPlayer());
			if(player != null)
				player.setArmiesLeft(player.getArmiesLeft() + superRegion.getArmiesReward());
		}
	}
	
	/**
	 * Sends everything to the players about this round
	 */
	public void sendAllInfo()
	{
		sendStartingArmiesInfo(player1);
		sendStartingArmiesInfo(player2);
		sendUpdateMapInfo(player1);
		sendUpdateMapInfo(player2);
		sendOpponentMovesInfo(player1);
		opponentMovesPlayer1.clear();
		sendOpponentMovesInfo(player2);
		opponentMovesPlayer2.clear();
	}

	/**
	 * Sends the list of available starting regions to pick from or the list of
	 * regions the opponent has picked
	 * @param player : player to send info to
	 * @param regions : list of regions
	 * @param beforeDistribution : true if we are still in the process of picking armies
	 */
	private void sendStartingRegionsInfO(Player player, ArrayList<Region> regions, boolean beforeDistribution) 
	{
		String startingRegionsString;
		if(beforeDistribution)
			startingRegionsString = "settings starting_regions";
		else
			startingRegionsString = "setup_map opponent_starting_regions";

		for(Region region : regions) {
			int id = region.getId();
			startingRegionsString = startingRegionsString.concat(" " + id);
		}

		player.sendInfo(startingRegionsString);
	}

	/**
	 * Informs the player about how many regions he can pick from the list of starting regions
	 * @param player : player to send info to
	 * @param amount : the amount of armies the player can pick
	 */
	private void sendStartingRegionPickAmount(Player player, int amount)
	{
		String pickAmountString = "settings starting_pick_amount " + amount;

		//System.out.println("sending to " + player.getName() + ": " + pickAmountString);
		player.sendInfo(pickAmountString);
	}
		
	/**
	 * Informs the player about how much armies he can place at the start next round
	 * @param player : player to send the info to
	 */
	private void sendStartingArmiesInfo(Player player)
	{
		String updateStartingArmiesString = "settings starting_armies";
		
		updateStartingArmiesString = updateStartingArmiesString.concat(" " + player.getArmiesLeft());
		
		//System.out.println("sending to " + player.getName() + ": " + updateStartingArmiesString);
		player.sendInfo(updateStartingArmiesString);
	}
	
	/**
	 * Informs the player about how his visible map looks now
	 * @param player : player to send the info to
	 */
	private void sendUpdateMapInfo(Player player)
	{
		LinkedList<Region> visibleRegions = map.visibleRegionsForPlayer(player);
		String updateMapString = "update_map";
		for(Region region : visibleRegions)
		{
			int id = region.getId();
			String playerName = region.getPlayerName();
			int armies = region.getArmies();
			
			updateMapString = updateMapString.concat(" " + id + " " + playerName + " " + armies);
		}
		player.sendInfo(updateMapString);
	}

	/**
	 * Informs the player about all his opponents' moves
	 * @param player : player to send the info to
	 */
	private void sendOpponentMovesInfo(Player player)
	{
		String opponentMovesString = "opponent_moves ";
		LinkedList<Move> opponentMoves = new LinkedList<Move>();

		if(player == player1)
			opponentMoves = opponentMovesPlayer1;
		else if(player == player2)
			opponentMoves = opponentMovesPlayer2;

		for(Move move : opponentMoves)
		{
			if(move.getIllegalMove().equals(""))
			{
				try {
					PlaceArmiesMove plm = (PlaceArmiesMove) move;
					opponentMovesString = opponentMovesString.concat(plm.getString() + " ");
				}
				catch(Exception e) {
					AttackTransferMove atm = (AttackTransferMove) move;
					opponentMovesString = opponentMovesString.concat(atm.getString() + " ");					
				}
			}
		}
		
		opponentMovesString = opponentMovesString.substring(0, opponentMovesString.length()-1);

		player.sendInfo(opponentMovesString);
	}
	
	/**
	 * @param playerName : string of player's name
	 * @return : Player object who has given name
	 */
	private Player getPlayer(String playerName)
	{
		if(player1.getName().equals(playerName))
			return player1;
		else if(player2.getName().equals(playerName))
			return player2;
		else
			return null;
	}
	
	/**
	 * @return : stored game for 'All' view in visualizer
	 */
	public LinkedList<MoveResult> getFullPlayedGame() {
		return fullPlayedGame;
	}
	
	/**
	 * @return : stored game for '1' view in visualizer
	 */
	public LinkedList<MoveResult> getPlayer1PlayedGame() {
		return player1PlayedGame;
	}
	
	/**
	 * @return : stored game for '2' view in visualizer
	 */
	public LinkedList<MoveResult> getPlayer2PlayedGame() {
		return player2PlayedGame;
	}

	/**
	 * @return : current round number
	 */
	public int getRoundNr() {
		return roundNr;
	}
}
