package net.lordofthecraft.Scenarios;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.craftbukkit.v1_11_R1.CraftWorld;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;

import com.comphenix.protocol.wrappers.EnumWrappers.Particle;

import net.lordofthecraft.HookahMain;
import net.lordofthecraft.PacketHandler;
import net.minecraft.server.v1_11_R1.EntityArmorStand;

/*
 * Credit goes to SteeZyyy for the math contained in this class.
 * The math is mostly based off of his creation.
 * 
 * His channel: https://www.youtube.com/channel/UCIA3ywM1G19ZlIfD1HOKOjg
 */
public class Spirit {

	private BukkitTask mainTask;
	private EntityArmorStand stand; //An entity that follows the spirit to display it's name
	private double t = 35.0;
	
	public void spawn(final Player player, Particle main, Particle secondary, int speed, String name) {
		stand = initStand(player, name);
		//Task that moves the spirit around the player.
		mainTask = Bukkit.getServer().getScheduler().runTaskTimerAsynchronously(HookahMain.plugin, new Runnable() {
			public void run() {
				t = t + Math.PI/200;
				final Location loc = player.getLocation();
				double x, y, z;
				double x1, y1, z1;
				double x2, y2, z2;
				double r = 0.5;
				
				x2 = sin(2*t);
				y2 = 2*cos(t);
				z2 = sin(3*t);
				
				t -= Math.PI/200;
				
				x1 = sin(2*t);
				y1 = 2*cos(t);
				z1 = sin(3*t);
				
				t += Math.PI/200;
				
				Vector dir = new Vector(x2-x1, y2-y1, z2-z1);
				Location loc2 = new Location(player.getWorld(), 0, 0, 0).setDirection(dir.normalize());
				loc2.setDirection(dir.normalize());
				
				for (double i = 0; i <= 2*Math.PI; i = i + Math.PI/8) {
					x = 0.2*t;
					y = r*sin(i)+2*sin(10*t)+2.8;
					z = r*cos(i);
					Vector v = new Vector(x, y, z);
					v = rotateFunction(v, loc2);
					loc.add(v.getX(), v.getY(), v.getZ());
					moveStand(player, stand, loc);
					if (main != null)
						PacketHandler.sendFakeParticle(player, loc, main, 1);
					if (i == 0 && secondary != null)
						PacketHandler.sendFakeParticle(player, loc, secondary, 1);
					loc.subtract(v.getX(), v.getY(), v.getZ());
				}
			}
		}, 0, speed);
	}
	
	public void remove(Player player) {
		mainTask.cancel();
		PacketHandler.removeFakeMobs(player, new int[]{stand.getId()});
	}
	
	//Set the distance from the player the spirit spawns at. (t is arbitrary)
	public void setStartT(double t) {
		this.t = t;
	}
	
	//ArmorStand that displays the name of the spirit
	private EntityArmorStand initStand(Player player, String name) {
		EntityArmorStand spiritName = new EntityArmorStand(((CraftWorld) player.getWorld()).getHandle());
		if (name != null) {
			spiritName.setCustomName(name);
			spiritName.setCustomNameVisible(true);
		}
		spiritName.setLocation(player.getLocation().getX(), player.getLocation().getY(), player.getLocation().getZ(), 0, 0);
		spiritName.setInvisible(true);
		spiritName.setNoGravity(true);
		
		PacketHandler.spawnNMSLivingEntity(player, spiritName);
		
		return spiritName;
	}
	
	public EntityArmorStand getStand() {
		return stand;
	}
	
	private void moveStand(Player player, EntityArmorStand stand, Location loc) {
		loc.setDirection(player.getLocation().subtract(loc).toVector()); //look at player
		Location location = loc.clone().subtract(0, 1, 0);
		
		PacketHandler.teleportFakeEntity(player, stand.getId(), location);
		
		//stores the new values 
		stand.locX = location.getX();
		stand.locY = location.getY();
		stand.locZ = location.getZ();
		stand.yaw = location.getYaw();
		stand.pitch = location.getPitch();
	}
	
	//two methods to help with the math's readability
	private static final double cos(double angle) {
		return Math.cos(angle);
	}
	private static final double sin(double angle) {
		return Math.sin(angle);
	}
	
	private static Vector rotateFunction(Vector v, Location loc) {
		double yawR = loc.getYaw()/180.0*Math.PI;
		double pitchR = loc.getPitch()/180.0*Math.PI;
		
		v = rotateAboutX(v, pitchR);
		v = rotateAboutY(v, -yawR);
		return v;
	}
	
	private static Vector rotateAboutX(Vector vect, double a) {
		double Y = cos(a)*vect.getY() - sin(a)*vect.getZ();
		double Z = sin(a)*vect.getY() + cos(a)*vect.getZ();
		return vect.setY(Y).setZ(Z);
	}
	
	private static Vector rotateAboutY(Vector vect, double b) {
		double X = cos(b)*vect.getX() + sin(b)*vect.getZ();
		double Z = -sin(b)*vect.getX() + cos(b)*vect.getZ();
		return vect.setX(X).setZ(Z);
	}
	
}
