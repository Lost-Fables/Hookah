package net.lordofthecraft.hookah;

import java.util.Objects;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;

public class WeakLocation {
	
	private String world;
	private int x;
	private int y;
	private int z;
	
	public WeakLocation(String world, int x, int y, int z) {
		this.world = world;
		this.x = x;
		this.y = y;
		this.z = z;
	}
	
	public WeakLocation(Location loc) {
		this.world = loc.getWorld().getName();
		this.x = loc.getBlockX();
		this.y = loc.getBlockY();
		this.z = loc.getBlockZ();
	}
	
	public boolean isLocation(Location loc) {
		return loc.getBlockX() == x && loc.getBlockY() == y && loc.getBlockZ() == z &&
			loc.getWorld().getName().equals(world);
	}
	
	public Location convertToLocation() {
		World parsedWorld = Bukkit.getServer().getWorld(world);
		if (parsedWorld == null) return null;
		return new Location(parsedWorld, x, y, z);
	}
	
	@Override
	public String toString() {
		return world + ";" + x + ";" + y + ";" + z;
	}
	
	@Override
    public boolean equals(Object object) {
        boolean sameSame = false;

        if (object instanceof WeakLocation) {
        	WeakLocation weakLoc = (WeakLocation) object;
            sameSame = (this.world.equals(weakLoc.world) &&
            			this.x == weakLoc.x &&
            			this.y == weakLoc.y &&
            			this.z == weakLoc.z);
        }

        return sameSame;
    }
	
	@Override
	public int hashCode() {
		return Objects.hash(world, x, y, z);
	}
}
