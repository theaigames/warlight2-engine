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

import com.theaigames.game.warlight2.map.Map;
import com.theaigames.game.warlight2.map.Region;
import com.theaigames.game.warlight2.move.AttackTransferMove;
import com.theaigames.game.warlight2.move.Move;
import com.theaigames.game.warlight2.move.PlaceArmiesMove;

/**
 * Parser class
 * 
 * Parses input from the bots
 * 
 * @author Jim van Eeden <jim@starapple.nl>
 */

public class Parser {
	
	private Map map;
	
	public Parser(Map map)
	{
		this.map = map;
	}
	
	/**
	 * Parses sequence of moves given by player
	 * @param input : input string
	 * @param player : player who gave the input
	 * @return : list parsed of moves
	 */
	public ArrayList<Move> parseMoves(String input, Player player)
	{
		ArrayList<Move> moves = new ArrayList<Move>();
		
		try {
			input = input.trim();
			if(input.length() <= 1)
				return moves;
			
			String[] split = input.split(",");
			
			for(int i=0; i<split.length; i++)
			{
				if(i > 50){
					player.getBot().addToDump("Maximum number of moves reached, max 50 moves are allowed");
					break;
				}
				Move move = parseMove(split[i], player);
				if(move != null)
					moves.add(move);
			}
		}
		catch(Exception e) {
			player.getBot().addToDump("Move input is null\n");
		}
		return moves;
	}

	/**
	 * Parses a move from input string given by player
	 * @param input : input string
	 * @param player : player who gave the input
	 * @return : parsed move
	 */
	private Move parseMove(String input, Player player)
	{
		int armies = -1;
		
		String[] split = input.trim().split(" ");

		if(!split[0].equals(player.getName()))
		{
			errorOut("Incorrect player name or move format incorrect", input, player);
			return null;
		}	
		
		if(split[1].equals("place_armies"))		
		{
			Region region = null;

			region = parseRegion(split[2], input, player);

			try { armies = Integer.parseInt(split[3]); }
			catch(Exception e) { errorOut("Number of armies input incorrect", input, player);}
		
			if(!(region == null || armies == -1))
				return new PlaceArmiesMove(player.getName(), region, armies);
			return null;
		}
		else if(split[1].equals("attack/transfer"))
		{
			Region fromRegion = null;
			Region toRegion = null;
			
			fromRegion = parseRegion(split[2], input, player);
			toRegion = parseRegion(split[3], input, player);
			
			try { armies = Integer.parseInt(split[4]); }
			catch(Exception e) { errorOut("Number of armies input incorrect", input, player);}

			if(!(fromRegion == null || toRegion == null || armies == -1))
				return new AttackTransferMove(player.getName(), fromRegion, toRegion, armies);
			return null;
		}

		errorOut("Bot's move format incorrect", input, player);
		return null;
	}
	
	/**
	 * @param regionId : id of region
	 * @param input : full input used for error logging
	 * @param player : player who gave the input
	 * @return : parsed region
	 */
	private Region parseRegion(String regionId, String input, Player player)
	{
		int id = -1;
		Region region;
		
		try { id = Integer.parseInt(regionId); }
		catch(Exception e) { errorOut("Region id input incorrect", input, player); return null;}
		
		region = map.getRegion(id);
		
		return region;
	}
	
	/**
	 * Parses the starting region picks
	 * @param input : input string
	 * @param pickableRegions : list of regions that can be picked as starting regions
	 * @param player : player who gave the input
	 * @return : parsed starting region
	 */
	public Region parseStartingRegion(String input, ArrayList<Region> pickableRegions, Player player) {
		
		Region startingRegion = parseRegion(input, input, player);
		
		if(startingRegion == null || !pickableRegions.contains(startingRegion)) 
		{
			errorOut("Pick starting region: Chosen region is not in the given pickable regions list", input, player);
			return null;
		}
		
		return startingRegion;
	}

	/**
	 * Adds parse error to player dump
	 * @param error
	 * @param input
	 * @param player
	 */
	private void errorOut(String error, String input, Player player)
	{
		player.getBot().addToDump("Parse error: " + error + " (" + input + ")\n");
	}

}
