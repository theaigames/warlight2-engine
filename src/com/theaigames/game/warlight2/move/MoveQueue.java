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

package com.theaigames.game.warlight2.move;
import java.util.ArrayList;

import com.theaigames.game.warlight2.Player;

/**
 * MoveQueue class
 * 
 * Stores all moves returned by the bots for one round and determines
 * the ordering in which they are executed
 * 
 * @author Jim van Eeden <jim@starapple.nl>
 */

public class MoveQueue {
	
	public ArrayList<PlaceArmiesMove> placeArmiesMoves;
	public ArrayList<AttackTransferMove> attackTransferMovesP1;
	public ArrayList<AttackTransferMove> attackTransferMovesP2;
	private Player player1, player2;
	
	public MoveQueue(Player player1, Player player2)
	{
		this.placeArmiesMoves = new ArrayList<PlaceArmiesMove>();
		this.attackTransferMovesP1 = new ArrayList<AttackTransferMove>();
		this.attackTransferMovesP2 = new ArrayList<AttackTransferMove>();
		this.player1 = player1;
		this.player2 = player2;
	}
	
	/**
	 * @param move : stores move in the correct list
	 */
	public void addMove(Move move)
	{
		try { //add PlaceArmiesMove
			PlaceArmiesMove plm = (PlaceArmiesMove) move;
			placeArmiesMoves.add(plm);
		}
		catch(Exception e) { //add AttackTransferMove
			AttackTransferMove atm = (AttackTransferMove) move;
			if(player1.getName().equals(move.getPlayerName()))
				attackTransferMovesP1.add(atm);
			else if(player2.getName().equals(move.getPlayerName()))
				attackTransferMovesP2.add(atm);
		}
	}

	/**
	 * Empties the move queue
	 */
	public void clear()
	{
		placeArmiesMoves.clear();
		attackTransferMovesP1.clear();
		attackTransferMovesP2.clear();
	}

	/**
	 * @return : true if there is an attackTransfer move in the queue, false otherwise
	 */
	public boolean hasNextAttackTransferMove()
	{
		if(attackTransferMovesP1.isEmpty() && attackTransferMovesP2.isEmpty())
			return false;
		return true;
	}

	/**
	 * Here is determined which player can do the next attackTransfer move. Player to move first each move is chosen random.
	 * Makes sure that if a player has an illegal move, the next legal move is selected.
	 * @param moveNr : the number of this attackTransfer move
	 * @param previousMovePlayer : the player name who last executed a move
	 * @param previousWasIllegal : true if the previous move was illegal
	 * @return : the next attackTransfer move from the queue
	 */
	public AttackTransferMove getNextAttackTransferMove(int moveNr, String previousMovePlayer, Boolean previousWasIllegal)
	{
		if(!hasNextAttackTransferMove()) //shouldn't ever happen
		{
			System.err.println("No more AttackTransferMoves left in MoveQueue");
			return null;
		}

		if(!previousWasIllegal)
		{
			if(moveNr % 2 == 1 || previousMovePlayer.equals("")) //first move of the two
			{
				double rand = Math.random();
				return getMove(rand < 0.5);
			}
			else //it's the other player's turn
			{
				return getMove(previousMovePlayer.equals(player2.getName()));
			}
		}
		else //return another move by the same player
		{
			return getMove(previousMovePlayer.equals(player1.getName()));
		}
	}

	/**
	 * @param conditionForPlayer1 : true if the next move should be player1's
	 * @return the next attackTransfer move to be executed
	 */
	private AttackTransferMove getMove(Boolean conditionForPlayer1)
	{
		AttackTransferMove move;
		if(!attackTransferMovesP1.isEmpty() && (conditionForPlayer1 || attackTransferMovesP2.isEmpty())) //get player1's move
		{
			move = attackTransferMovesP1.get(0);
			attackTransferMovesP1.remove(0);
			return move;
		}
		else // get player2's move
		{
			move = attackTransferMovesP2.get(0);
			attackTransferMovesP2.remove(0);
			return move;
		}
	}
}
