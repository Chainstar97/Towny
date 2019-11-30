package com.palmergames.bukkit.towny.war.siegewar;

import com.palmergames.bukkit.towny.Towny;
import com.palmergames.bukkit.towny.TownyMessaging;
import com.palmergames.bukkit.towny.TownySettings;
import com.palmergames.bukkit.towny.TownyUniverse;
import com.palmergames.bukkit.towny.exceptions.NotRegisteredException;
import com.palmergames.bukkit.towny.object.*;
import com.palmergames.bukkit.towny.war.siegewar.enums.SiegeStatus;
import com.palmergames.bukkit.towny.war.siegewar.locations.Siege;
import com.palmergames.bukkit.towny.war.siegewar.locations.SiegeZone;
import com.palmergames.bukkit.towny.war.siegewar.playeractions.*;
import com.palmergames.bukkit.towny.war.siegewar.utils.SiegeWarBlockUtil;
import org.bukkit.Material;
import org.bukkit.block.Banner;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.block.BlockPlaceEvent;

import java.util.List;

/**
 * @author Goosius
 */
public class SiegeWarPlaceBlockController {
	
	/*
	 * coloured standing banner - could be attack or invade
	 * white standing banner - could be surrender
	 * chest - could be plunder
	 *
	 * Return - skipOtherPerChecks
	 */
	public static boolean evaluateSiegeWarPlaceBlockRequest(Player player,
													 Block block,
													 BlockPlaceEvent event,
													 Towny plugin)
	{
		try {
			String blockTypeName = block.getType().getKey().getKey();
			if (blockTypeName.endsWith("banner") && !blockTypeName.contains("wall")) {
				return evaluateSiegeWarPlaceBannerRequest(player, block, blockTypeName, event, plugin);
			} else if (block.getType().equals(Material.CHEST)) {
				return evaluateSiegeWarPlaceChestRequest(player, block, event);
			} else {
				return false;
			}
		} catch (NotRegisteredException e) {
			TownyMessaging.sendErrorMsg(player, "Problem placing siege related block");
			e.printStackTrace();
			return false;
		}
	}


	private static boolean evaluateSiegeWarPlaceBannerRequest(Player player,
													   Block block,
													   String blockTypeName,
													   BlockPlaceEvent event,
													   Towny plugin) throws NotRegisteredException
	{
		TownyUniverse townyUniverse = TownyUniverse.getInstance();
		TownyWorld townyWorld = townyUniverse.getDataSource().getWorld(block.getWorld().getName());
		Coord blockCoord = Coord.parseCoord(block);

		if(!townyWorld.hasTownBlock(blockCoord)) {
			//Wilderness found
			//Possible abandon or attack request
			
			if (blockTypeName.contains("white")  && ((Banner) block.getState()).getPatterns().size() == 0) {
				return evaluatePlaceWhiteBannerOutsideTown(block, player, event);
			} else {
				return evaluatePlaceColouredBannerOutsideTown(block, player, event);
			}
			
		} else {
			//Town block found
			//Possible invade or surrender request
			
			TownBlock townBlock = null;
			if(townyWorld.hasTownBlock(blockCoord)) {
				townBlock = townyWorld.getTownBlock(blockCoord);
			}

			if (townBlock == null) {
				return false;
			}
			
			Town town;
			if(townBlock.hasTown()) {
				town = townBlock.getTown();
			} else {
				return false;
			}
			
			//If there is no siege, do normal block placement
			if (!town.hasSiege())
				return false;

			if (blockTypeName.contains("white")  && ((Banner) block.getState()).getPatterns().size() == 0) {
				return evaluatePlaceWhiteBannerInTown(player, town, event);
			} else {
				return evaluatePlaceColouredBannerInTown(plugin, player, town, event);
			}
		}
	}
	
	private static boolean evaluatePlaceWhiteBannerOutsideTown(Block block, Player player, BlockPlaceEvent event) {
		//White banner
		//Possible abandon request
		
		if (!TownySettings.getWarSiegeAbandonEnabled())
			return false;

		//Find the nearest siege zone to the player,from IN_PROGRESS sieges
		SiegeZone nearestSiegeZone = null;
		double distanceToNearestSiegeZone = -1;
		for(SiegeZone siegeZone: com.palmergames.bukkit.towny.TownyUniverse.getInstance().getDataSource().getSiegeZones()) {

			if(siegeZone.getSiege().getStatus() != SiegeStatus.IN_PROGRESS)
				continue;

			if (nearestSiegeZone == null) {
				nearestSiegeZone = siegeZone;
				distanceToNearestSiegeZone = block.getLocation().distance(nearestSiegeZone.getFlagLocation());
			} else {
				double distanceToNewTarget = block.getLocation().distance(siegeZone.getFlagLocation());
				if(distanceToNewTarget < distanceToNearestSiegeZone) {
					nearestSiegeZone = siegeZone;
					distanceToNearestSiegeZone = distanceToNewTarget;
				}
			}
		}

		//If there are no in-progress sieges at all,then regular block request
		if(nearestSiegeZone == null)
			return false;

		//If the player is too far from the nearest zone, then regular block request
		if(distanceToNearestSiegeZone > TownySettings.getTownBlockSize())
			return false;

		AbandonAttack.processAbandonSiegeRequest(player,
			nearestSiegeZone,
			event);

		return true;
	}

	
	private static boolean evaluatePlaceColouredBannerOutsideTown(Block block, Player player, BlockPlaceEvent event) {
		//Coloured banner
		//Possible attack request
		
		if (!TownySettings.getWarSiegeAttackEnabled())
			return false;

		List<TownBlock> nearbyTownBlocks = SiegeWarBlockUtil.getAdjacentTownBlocks(player, block);
		if (nearbyTownBlocks.size() == 0)
			return false;   //No town blocks are nearby. Normal block placement

		AttackTown.processAttackTownRequest(
			player,
			block,
			nearbyTownBlocks,
			event);

		return true;
	}


    private static boolean evaluatePlaceWhiteBannerInTown(Player player, Town town, BlockPlaceEvent event) {
		//White Banner: Evaluate Surrender request if siege exists in target town

		if (!TownySettings.getWarSiegeSurrenderEnabled())
			return false;

		SurrenderTown.processTownSurrenderRequest(
			player,
			town,
			event);
		return true;
	}

	public static boolean evaluatePlaceColouredBannerInTown(Towny plugin, Player player, Town town, BlockPlaceEvent event) throws NotRegisteredException {
		//Coloured Banner: Evaluate invade request if siege exists in target town,
		// and player is a member of any of the attacking nations
		
		if (!TownySettings.getWarSiegeInvadeEnabled())
			return false;

		TownyUniverse townyUniverse = TownyUniverse.getInstance();
		Resident resident = townyUniverse.getDataSource().getResident(player.getName());
		Siege siege = town.getSiege();

		if(resident.hasTown()
			&& resident.hasNation()
			&& siege.getSiegeZones().containsKey(resident.getTown().getNation())) {

			InvadeTown.processInvadeTownRequest(plugin, player, resident, town, siege, event);
			return true;
		} else {
			return false;
		}
	}
	
	private static boolean evaluateSiegeWarPlaceChestRequest(Player player,
													  Block block,
													  BlockPlaceEvent event) throws NotRegisteredException {
		//Plunder chest: Evaluate plunder action if a member of any attacking nation attempts to place banner
		
		if (!TownySettings.getWarSiegePlunderEnabled())
			return false;

		Town town;
		TownyUniverse townyUniverse = TownyUniverse.getInstance();
		TownyWorld world = townyUniverse.getDataSource().getWorld(player.getWorld().getName());
		Coord coord = Coord.parseCoord(block);
		
		if (!world.hasTownBlock(coord)) 
			return false;
		
		TownBlock townBlock = world.getTownBlock(coord);
		
		if(townBlock.hasTown()) {
			town = townBlock.getTown();
		} else {
			return false;
		}
		
		if (town.hasSiege()) {
			Resident resident = townyUniverse.getDataSource().getResident(player.getName());
			Siege siege = town.getSiege();
	
			if(resident.hasTown()
				&& resident.hasNation()
				&& siege.getSiegeZones().containsKey(resident.getTown().getNation())) {

				PlunderTown.processPlunderTownRequest(player, resident, town, siege, event);
				return true;
			} else {
				return false;
			}
		} else {
			return false;
		}
	}
}