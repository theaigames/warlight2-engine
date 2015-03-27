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

import java.awt.Point;
import java.util.ArrayList;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.theaigames.game.warlight2.map.Map;
import com.theaigames.game.warlight2.map.Region;
import com.theaigames.game.warlight2.map.SuperRegion;

/**
 * MapCreator class
 * 
 * Static class that provides methods to create the map and send
 * information about it to the bots
 * 
 * @author Jim van Eeden <jim@starapple.nl>
 */

public class MapCreator {
	
	/**
	 * @param mapString : string that represents the map to be created
	 * @return : a Map object to use in the game
	 */
	public static Map createMap(String mapString)
	{
		Map map = new Map();

		//parse the map string
		try {
			JSONObject jsonMap = new JSONObject(mapString);
			
			// create SuperRegion objects
			JSONArray superRegions = jsonMap.getJSONArray("SuperRegions");
			for (int i = 0; i < superRegions.length(); i++) { 
				JSONObject jsonSuperRegion = superRegions.getJSONObject(i);
				map.add(new SuperRegion(jsonSuperRegion.getInt("id"), jsonSuperRegion.getInt("bonus")));
			}
			
			// create Region object
			JSONArray regions = jsonMap.getJSONArray("Regions");
			for (int i = 0; i < regions.length(); i++) { 
				JSONObject jsonRegion = regions.getJSONObject(i);
				SuperRegion superRegion = map.getSuperRegion(jsonRegion.getInt("superRegion"));
				map.add(new Region(jsonRegion.getInt("id"), superRegion));
			}
			
			// add the Regions' neighbors
			for (int i = 0; i < regions.length(); i++) { 
				JSONObject jsonRegion = regions.getJSONObject(i);
				Region region = map.getRegion(jsonRegion.getInt("id"));
				JSONArray neighbors = jsonRegion.getJSONArray("neighbors");
				for (int j = 0; j < neighbors.length(); j++) {
					Region neighbor = map.getRegion(neighbors.getInt(j));
					region.addNeighbor(neighbor);
				}
			}
		} catch (JSONException e) {
			System.err.println("JSON: Can't parse map string: " + e);
		}
		
		map.sort();

		return map;
	}
	
	/**
	 * Sets up the map.
	 * Make every region neutral with 2 armies to start with.
	 * Adds wastelands (> 2 armies on a neutral) if wastelandSize > 0.
	 * @param initMap : the map object that hasn't been set up yet, i.e. no armies yet
	 * @param wastelandSize : the amount of armies that a wasteland contains
	 * @return : the fully initialized and setup Map object
	 */
	public static Map setupMap(Map initMap, int wastelandSize)
	{
		Map map = initMap;
		for(Region region : map.regions)
		{
			region.setPlayerName("neutral");
			region.setArmies(2);
		}
		if (wastelandSize > 0) {
			int nrOfWastelands = (int) (map.getSuperRegions().size() / 2); // amount of wastelands is half of the amount of superRegions

			for(int i = 0; i < nrOfWastelands; i++) {
				double rand = Math.random();
				int index = (int) (rand*map.getRegions().size());
				Region wasteland = map.getRegions().get(index);
				
				if(wasteland.getArmies() > 2 && !roomForWasteland(wasteland.getSuperRegion())) {
					i--;
					continue;
				}
				
				wasteland.setArmies(wastelandSize);
			}
		}
		return map;
	}

	/**
	 * Checks if superRegion has at least two neutral non-wasteland regions
	 * @param superRegion
	 * @return : true if another wasteland could be added to this superRegion
	 */
	private static boolean roomForWasteland(SuperRegion superRegion) {
		int count = 0;
		for(Region region : superRegion.getSubRegions()) {
			if(region.getArmies() == 2) {
				count++;
				if(count >= 2)
					return true;
			}
		}
		return false;
	}
	
	/**
	 *  Determines how much rounds a game can take before it's a draw, depending on map size
	 * @param map : the created map
	 * @return : the maximum number of rounds for this game
	 */
	public static int determineMaxRounds(Map map) {
		
		return (int) Math.max(60, map.getRegions().size() * 2.5); // minimum of 60, otherwise 2.5 times the number of regions
	}
	
	/**
	 * Sends all the info to the bots about what the map looks like
	 * @param player
	 * @param map
	 */
	public static void sendSetupMapInfo(Player player, Map map)
	{
		String setupSuperRegionsString, setupRegionsString, setupNeighborsString, setupWastelandsString;
		setupSuperRegionsString = getSuperRegionsString(map);
		setupRegionsString = getRegionsString(map);
		setupNeighborsString = getNeighborsString(map);
		setupWastelandsString = getWastelandsString(map);
		
		player.sendInfo(setupSuperRegionsString);
		// System.out.println(setupSuperRegionsString);
		player.sendInfo(setupRegionsString);
		// System.out.println(setupRegionsString);
		player.sendInfo(setupNeighborsString);
		// System.out.println(setupNeighborsString);
		player.sendInfo(setupWastelandsString);
	}
	
	/**
	 * @param map
	 * @return : the string representation of given map's superRegions
	 */
	private static String getSuperRegionsString(Map map)
	{
		String superRegionsString = "setup_map super_regions";
		for(SuperRegion superRegion : map.superRegions)
		{
			int id = superRegion.getId();
			int reward = superRegion.getArmiesReward();
			superRegionsString = superRegionsString.concat(" " + id + " " + reward);
		}
		return superRegionsString;
	}
	
	/**
	 * @param map
	 * @return : the string representation of given map's regions
	 */
	private static String getRegionsString(Map map)
	{
		String regionsString = "setup_map regions";
		for(Region region : map.regions)
		{
			int id = region.getId();
			int superRegionId = region.getSuperRegion().getId();
			regionsString = regionsString.concat(" " + id + " " + superRegionId);
		}
		return regionsString;
	}
	
	/**
	 * @param map
	 * @return : the string representation of how given map's regions are connected
	 */
	private static String getNeighborsString(Map map)
	{
		String neighborsString = "setup_map neighbors";
		ArrayList<Point> doneList = new ArrayList<Point>();
		for(Region region : map.regions)
		{
			int id = region.getId();
			String neighbors = "";
			for(Region neighbor : region.getNeighbors())
			{
				if(checkDoneList(doneList, id, neighbor.getId()))
				{
					neighbors = neighbors.concat("," + neighbor.getId());
					doneList.add(new Point(id,neighbor.getId()));
				}
			}
			if(neighbors.length() != 0)
			{
				neighbors = neighbors.replaceFirst(","," ");
				neighborsString = neighborsString.concat(" " + id + neighbors);
			}
		}
		return neighborsString;
	}
	
	/**
	 * @param map
	 * @return : the string representation of given map's wastelands
	 */
	private static String getWastelandsString(Map map) 
	{
		String wastelandsString = "setup_map wastelands";
		for(Region region : map.getRegions()) 
		{
			if(region.getArmies() > 2) 
			{
				int id = region.getId();
				wastelandsString = wastelandsString.concat(" " + id);
			}
		}
		return wastelandsString;
	}
	
	/**
	 * Used in getNeighborsString
	 * @param doneList
	 * @param regionId
	 * @param neighborId
	 * @return
	 */
	private static Boolean checkDoneList(ArrayList<Point> doneList, int regionId, int neighborId)
	{
		for(Point p : doneList)
			if((p.x == regionId && p.y == neighborId) || (p.x == neighborId && p.y == regionId))
				return false;
		return true;
	}
}
