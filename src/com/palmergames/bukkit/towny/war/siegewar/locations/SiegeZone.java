package com.palmergames.bukkit.towny.war.siegewar.locations;

import com.palmergames.bukkit.towny.object.Nation;
import com.palmergames.bukkit.towny.object.Town;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;

/**
 * @author Goosius
 */
public class SiegeZone {

    private Location siegeBannerLocation;
    private Nation attackingNation;
    private Town defendingTown;
    private int siegePoints;
    private Map<Player, Long> attackerPlayerScoreTimeMap; //player, time when they will score
    private Map<Player, Long> defenderPlayerScoreTimeMap; //player, time when they will score

    public SiegeZone() {
        attackingNation = null;
        defendingTown = null;
        siegePoints = 0;
        siegeBannerLocation = null;
        attackerPlayerScoreTimeMap = new HashMap<>();
        defenderPlayerScoreTimeMap = new HashMap<>();
    }

    public SiegeZone(Nation attackingNation, Town defendingTown) {
        this.defendingTown = defendingTown;
        this.attackingNation = attackingNation;
        siegePoints = 0;
        siegeBannerLocation = null;
        attackerPlayerScoreTimeMap = new HashMap<>();
        defenderPlayerScoreTimeMap = new HashMap<>();
    }

    public String getName() {
        return attackingNation.getName().toLowerCase() + "#vs#" + defendingTown.getName().toLowerCase();
    }

    public static String generateName(String attackingNationName, String defendingTownName) {
        return attackingNationName.toLowerCase() + "#vs#" + defendingTownName.toLowerCase();
    }
    
    public static String[] generateTownAndNationName(String siegeZoneName) {
        return siegeZoneName.split("#vs#");
    }

    public com.palmergames.bukkit.towny.war.siegewar.locations.Siege getSiege() {
        return defendingTown.getSiege();
    }

    public Nation getAttackingNation() {
        return attackingNation;
    }

    public Location getFlagLocation() {
        return siegeBannerLocation;
    }

    public void setFlagLocation(Location location) {
        this.siegeBannerLocation = location;
    }

    public Map<Player, Long> getAttackerPlayerScoreTimeMap() {
        return attackerPlayerScoreTimeMap;
    }

    public Map<Player, Long> getDefenderPlayerScoreTimeMap() {
        return defenderPlayerScoreTimeMap;
    }

    public Map<String, Long> getAttackerPlayerIdScoreTimeMap() {
        Map<String, Long> result = new HashMap<>();
        for(Map.Entry<Player, Long> entry: attackerPlayerScoreTimeMap.entrySet()) {
            System.out.println("TEST");
            System.out.println(entry);
            System.out.println(entry.getKey());
            System.out.println(entry.getKey().getUniqueId());
            System.out.println(entry.getKey().getUniqueId().toString());
            System.out.println(entry.getValue());

            result.put(entry.getKey().getUniqueId().toString(), entry.getValue());
        }
        return result;
    }

    public Map<String, Long> getDefenderPlayerIdScoreTimeMap() {
        Map<String, Long> result = new HashMap<>();
        for(Map.Entry<Player, Long> entry: defenderPlayerScoreTimeMap.entrySet()) {
            result.put(entry.getKey().getUniqueId().toString(), entry.getValue());
        }
        return result;
    }

    public Integer getSiegePoints() {
        return siegePoints;
    }

    public void setSiegePoints(int siegePoints) {
        this.siegePoints = siegePoints;
    }

    public void addSiegePoints(int siegePointsForAttackingPlayer) {
        siegePoints += siegePointsForAttackingPlayer;
    }
    
    public void setAttackingNation(Nation attackingNation) {
        this.attackingNation = attackingNation;
    }

    public void setDefendingTown(Town defendingTown) {
        this.defendingTown = defendingTown;
    }

    public Town getDefendingTown() {
        return defendingTown;
    }

    public void adjustSiegePoints(int adjustment) {
        siegePoints += adjustment;
    }

}
