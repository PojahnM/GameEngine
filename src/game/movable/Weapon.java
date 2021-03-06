package game.movable;

import com.badlogic.gdx.math.Vector2;

import game.core.Fundementals;
import game.core.GameObject;
import game.essentials.Animation;
import game.essentials.Image2D;
import game.objects.Particle;

/**
 * The {@code Weapon} can act as a machine gun or rocker launcher. It is a turret, rotating towards the target(if enabled), spawning clones of the given projectile.
 * @author Pojahn Moradi
 *
 */
public class Weapon extends PathDrone
{
	private float targetX, targetY, firingOffsetX, firingOffsetY, rotationSpeed;
	private int burst, burstDelay, reload, burstCounter, delayCounter, reloadCounter;
	private boolean rotationAllowed, alwaysRotate, frontFire, firing, rotateWhileRecover;
	private GameObject targets[], currTarget;
	private Projectile proj;
	private Particle firingParticle;
	private Animation<Image2D> firingImage, orgImage;
	private boolean usingTemp;
	
	/**
	 * Constructs a {@code Weapon}. The projectile to fire is set with {@code setProjectile} and is required to avoid a null pointer exception.
	 * @param x The X coordinate to start at.
	 * @param y The Y coordinate to start at.
	 * @param burst The amount of ammo the turret fires before reloading.
	 * @param burstDelay The amount of frames between shots.
	 * @param emptyReload The amount of frames to reload when running out of ammo.
	 * @param targets The units to be targeted and shot at.
	 */
	public Weapon(float x, float y, int burst, int burstDelay, int emptyReload, GameObject... targets)
	{
		super(x,y);
		this.burst = burst;
		this.reload = emptyReload;
		this.targets = targets;
		this.burstDelay = burstDelay;
		alwaysRotate = firing = false;
		rotationAllowed = rotateWhileRecover = true;
		burstCounter = reloadCounter = 0;
		delayCounter = burstDelay - 1;
	}

	@Override
	public void moveEnemy()
	{
		if(proj == null)
			throw new IllegalStateException("The projectile must be set before usage.");
		
		if(usingTemp && image.hasEnded())
		{
			usingTemp = false;
			firingImage = image;
			firingImage.reset();
			image = orgImage;
		}
		
		super.moveEnemy();

		findTarget();
		
		rotateWeapon();
		
		if (--reloadCounter > 0)
			return;
		else if(reloadCounter == 0)
			rotationAllowed = true;
		
		if(haveTarget())
		{
			if(!haveBullets())
				reset();
			else if((firing || isTargeting()) && ++delayCounter % burstDelay == 0)
			{
				if(firingImage != null)
				{
					orgImage = image;
					image = firingImage;
					usingTemp = true;
				}
				rotationAllowed = false;
				firing = true;
				burstCounter++;
				
				Projectile projClone = null;
				Particle partClone = null;
				if(frontFire)
				{
					Vector2 front = getFrontPosition();
					projClone = proj.getClone(front.x - proj.width / 2 + firingOffsetX, front.y - proj.height / 2 + firingOffsetY);
					if(firingParticle != null)
						partClone = firingParticle.getClone(front.x - firingParticle.width / 2 + firingOffsetX, front.y - firingParticle.height / 2 + firingOffsetY);
				}
				else
				{
					projClone = proj.getClone(loc.x + firingOffsetX, loc.y + firingOffsetY);
					if(firingParticle != null)
						partClone = firingParticle.getClone(loc.x + firingOffsetX, loc.y + firingOffsetY);
				}
				projClone.setTarget(targetX, targetY);
				projClone.setDisposable(true);
				stage.add(projClone);
				if(partClone != null)
					stage.add(partClone);
			}
		}
		else
			reset();
	}
	
	/**
	 * Checks if the turret currently have a target.
	 * @return True if the turret have a target.
	 */
	public boolean haveTarget()
	{
		return currTarget != null;
	}
	
	/**
	 * Checks if the turret can fire instantly or have to reload.
	 * @return True if the turret can fire instantly.
	 */
	public boolean haveBullets()
	{
		return burst > burstCounter;
	}
	
	/**
	 * The projectile to fire.
	 * @param proj The {@code Projectile}.
	 */
	public void setProjectile(Projectile proj)
	{
		this.proj = proj;
	}
	
	/**
	 * The given values + the {@code Weapons} position will be the starting point of the bullet(unless {@code frontfire} is used).
	 * @param x The X offset.
	 * @param y The Y offset.
	 */
	public void setFiringOffsets(float x, float y)
	{
		firingOffsetX = x;
		firingOffsetY = y;
	}
	
	/**
	 * The {@code Particle} to spawn at the bullets firing position when attacking.
	 * @param firingParticle The particle.
	 */
	public void setFiringParticle(Particle firingParticle)
	{
		this.firingParticle = firingParticle;
	}
	
	/**
	 * Whether or not to fire at the front position, in case the image for example have a barrel. This is used together with {@code setRotationSpeed(>0)} and requires the used image to be formated in a special way.<br>
	 * Reefer to the tutorial list for usage.
	 * @param frontFire True to fire at the front of the barrel.
	 */
	public void setFrontFire(boolean frontFire)
	{
		this.frontFire = frontFire;
	}
	
	/**
	 * The rotation speed of the turret(0 default and disables). Enabling rotating also means that the turret have to face the target before allowed to fire.
	 * @param rotationSpeed The rotation speed. Should always be positive and low(example 0.09f).
	 */
	public void setRotationSpeed(float rotationSpeed)
	{
		this.rotationSpeed = rotationSpeed;
	}
	
	/**
	 * Determines if the turret should rotate even if no target is visible(rather than freezing).
	 * @param alwaysRotate True to enables constant rotation.
	 */
	public void setAlwaysRotate(boolean alwaysRotate)
	{
		this.alwaysRotate = alwaysRotate;
	}
	
	/**
	 * The image the use for the turret when firing. The turrets image will change back to its initial image when the last frame of the firing image have been rendered, meaning no support for looping firing image.
	 * @param firingImage The firing image.
	 */
	public void setFiringImage(Animation<Image2D> firingImage)
	{
		this.firingImage = firingImage;
	}
	
	/**
	 * Whether or not to rotate towards the target when reloading rather than just freezing.
	 * @param rotateWhileRecover False to disable rotation while recovering.
	 */
	public void setRotateWhileRecover(boolean rotateWhileRecover)
	{
		this.rotateWhileRecover = rotateWhileRecover;
	}
	
	/**
	 * Initiate the current target.
	 */
	protected void findTarget()
	{
		currTarget = Fundementals.findClosestSeeable(this, targets);
	}
	
	/**
	 * Tries to rotate the turret towards the current target.
	 */
	protected void rotateWeapon()
	{
		if(rotationAllowed && rotationSpeed != 0.0f && haveTarget())
		{
			float x1 = loc.x + width / 2,
				  y1 = loc.y + height / 2,
				  x2 = currTarget.loc.x + currTarget.width / 2,
				  y2 = currTarget.loc.y + currTarget.height / 2;
			
			if(alwaysRotate || currTarget.canSee(this, Accuracy.MID))
				rotation = Fundementals.rotateTowardsPoint(x1,y1,x2,y2, rotation, rotationSpeed);
		}
	}
	
	protected void reset()
	{
		burstCounter = 0;
		delayCounter = burstDelay - 1;
		reloadCounter = reload;
		rotationAllowed = rotateWhileRecover;
		firing = false;
		currTarget = null;
	}
	
	/**
	 * Checks if the turret is currently targeting the current target.
	 * @return True if the turret is aiming at a target.
	 */
	protected boolean isTargeting()
	{
		if(rotationSpeed == 0.0f)
		{
			Vector2 position = Fundementals.findEdgePoint(this, currTarget);
			targetX = position.x;
			targetY = position.y;
			return canSee(currTarget, Accuracy.MID);
		}
		
		float centerX = loc.x + width / 2;
		float centerY = loc.y + height / 2;
		
		Vector2 front = getFrontPosition();
		Vector2 edge = Fundementals.findEdgePoint(centerX, centerY, front.x, front.y);
		Vector2 wall = Fundementals.findWallPoint(centerX, centerY, edge.x, edge.y);
		
		GameObject dummy = new GameObject();
		dummy.loc.x = currTarget.loc.x + currTarget.width / 2;
		dummy.loc.y = currTarget.loc.y + currTarget.height / 2;
		boolean targeting = Fundementals.checkLine((int)centerX, (int)centerY, (int)wall.x, (int)wall.y, dummy);
		if(targeting)
		{
			Vector2 edge2 = Fundementals.findEdgePoint(this, dummy);
			targetX = edge2.x;
			targetY = edge2.y;
		}
		return targeting;
	}
}