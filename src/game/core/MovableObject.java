package game.core;

import static game.core.Engine.*;
import game.core.Engine.Direction;
import game.essentials.Animation;
import game.essentials.Image2D;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

/**
 * This class extends GameObject to add more capabilities such as movement handling and control, multi and double facing, tile intersection and collision response.
 * @author Pojahn Moradi
 */
public class MovableObject extends GameObject
{
	/**
	 * A {@code triggerable MovableObject} is going to call its {@code TileEvent} N times every frame, where N is the amount of different tile types the object is currently standing on.
	 * The objects hitbox is <i>not</i> taken into consideration when scanning. All objects are treated as rectangles.<br>
	 * {@code TileEvents} are called automatically by the engine and should not be called manually.
	 */
	public interface TileEvent
	{
		/**
		 * The events handling code.
		 * @param tileType The tile type this unit is currently standing on.
		 */
		void eventHandling(byte tileType);
	};
	
	/**
	 * This is the current facing if the unit. Is set by the engine by default.
	 */
	public Direction facing;
	
	protected float moveSpeed;
	protected boolean canMove, triggerable, doubleFaced, multiFacings, flipImage, manualFacings;
	protected ArrayList<TileEvent> tileEvents;
	protected Set<GameObject> solidObjects;
	protected Set<Byte> occupyingCells;
	float prevX, prevY;
	boolean halted;
	private float tempX, tempY;
	
	/**
	 * Constructs a {@code MovableObject} with move speed set to 3.
	 */
	public MovableObject ()
	{
		super();
		moveSpeed = 3;
		prevX = prevY = 1;
		canMove = true;
		solidObjects   = new HashSet<>();
		occupyingCells = new HashSet<>();
		tileEvents 	   = new ArrayList<>();
		facing = Direction.E;
	}
	
	/**
	 * Whether or not this unit have 8 direction images rather than one.<br>
	 * This require a special structure in its image array, example:<br>
	 * NORTH:img1 img2 img3 <br>
	 * NORTH EAST: img4, img5, im6<br>
	 * EAST: img7 img8 img9<br>
	 * SOUTH EAST: img10 img11 img12<br>
	 * SOUTH: img13 img14 img15<br>
	 * SOUTH WEST: img16 img17 img18<br>
	 * WEST: img19 img20 img21<br>
	 * NORTH WEST: img22 img23 img24<br><br>
	 * 
	 * Every angle must have exact amount of frames and the order must match with the given example.
	 * @param multiFacings If this object is multi-faced.
	 */
	public void setMultiFaced(boolean multiFacings)
	{
		if(doubleFaced && multiFacings)
			throw new IllegalStateException("Cannot enable multi facing while double facing is enabled.");
		
		if((this.multiFacings = multiFacings))
			image.setLimit(image.getArray().length / 8);
	}
	
	/**
	 * Allow you enable double facing for the unit. The unit will face east when moving E,NE and SE and west when facing W,NW and SW.<br>
	 * By default, the image is single faced, meaning no matter what the current direction is, it will always use the same image.
	 * @param doubleFaced Whether or not to activate double facing.
	 * @param flipImage If false, the first half of the image array is used when moving east and the other half when moving west.<br>
	 * If true, the entire image array will be used when moving east and flipped horizontally when moving west.
	 */
	public void setDoubleFaced(boolean doubleFaced, boolean flipImage)
	{
		if(multiFacings && doubleFaced)
			throw new IllegalStateException("Cannot enable double facing while multi facing is enabled.");
		
		this.doubleFaced = doubleFaced;
		this.flipImage = flipImage;
		
		if(doubleFaced && !flipImage)
			image.setLimit(image.getArray().length / 2);
	}
	
	
	/**
	 * The facing of a {@code MovableObject} is by default set automatically by the engine. It can be switched off here.
	 * @param manualFacings True if you want to disable automatic facing initiations.
	 */
	public void setManualFacing(boolean manualFacings)
	{
		this.manualFacings = manualFacings;
	}
	
	@Override
	public void setImage(Animation<Image2D> obj)
	{
		super.setImage(obj);
		
		if(obj.isMultiFaced())
			multiFacings = true;
	}
	
	@Override
	public Image2D getFrame()
	{
		if (!visible || image == null)
			return null;
		
		image.getObject();
		
		if(multiFacings)
		{
			Image2D[] objImage = image.getArray();
			int animationCounter = image.getIndex(),
				framesPerAngle = objImage.length / 8;

			switch (facing)
			{
				case N:
					return objImage[animationCounter];
				case NE:
					return objImage[animationCounter + framesPerAngle];
				case E:
					return objImage[animationCounter + (framesPerAngle * 2)];
				case SE:
					return objImage[animationCounter + (framesPerAngle * 3)];
				case S:
					return objImage[animationCounter + (framesPerAngle * 4)];
				case SW:
					return objImage[animationCounter + (framesPerAngle * 5)];
				case W:
					return objImage[animationCounter + (framesPerAngle * 6)];
				case NW:
					return objImage[animationCounter + (framesPerAngle * 7)];
			}
		}
			
		if(doubleFaced)
		{
			if(flipImage)
			{
				Image2D img = image.getCurrentObject();
				switch (facing)
				{
					case NE:
					case E:
					case SE:
						flipX = false;
						break;
					case SW:
					case W:
					case NW:
						flipX = true;
						break;
					default:
						break;
				}
				return img;
			}
			else
			{
				Image2D[] objImage = image.getArray();
				int animationCounter = image.getIndex(),
					framesPerAngle = objImage.length / 2;

				switch (facing)
				{
					case NE:
					case E:
					case SE:
						return objImage[animationCounter];
					case SW:
					case W:
					case NW:
						return objImage[animationCounter + framesPerAngle];
					default:
						throw new RuntimeException();
				}
			}
		}
		
		return image.getCurrentObject();
	}
	
	@Override
	public MovableObject getClone(float x, float y)
	{
		MovableObject mo = new MovableObject();
		mo.loc.x = x;
		mo.loc.y = y;
		copyData(mo);
		
		return mo;
	}
	
	protected void copyData(MovableObject dest)
	{
		super.copyData(dest);
		dest.prevX = prevX;
		dest.prevY = prevY;
		dest.moveSpeed = moveSpeed;
		dest.canMove = canMove;
		dest.triggerable = triggerable;
		dest.solidObjects.addAll(solidObjects);
		dest.multiFacings = multiFacings;
		dest.manualFacings = manualFacings; 
		dest.facing = facing;
		dest.doubleFaced = doubleFaced;
		dest.flipImage = flipImage;
	}
	
	/**
	 * Moves this object to the specified points and do a tile and object inspection.<br>
	 * This function also checks if it is out of bounds and it that case returns false.
	 * @param targetX The x coordinate we want to move to.
	 * @param targetY The y coordinate we want to move to.
	 * @return True if we can move to the specified point.
	 */
	public boolean canGoTo(float targetX, float targetY)
	{
		if (!canMove)
			return false;
		
		if(outOfBounds(targetX + width, targetY + height) ||
		   outOfBounds(targetX, targetY))
			return false;
		
		tempTo(targetX, targetY);
		Set<Byte> cells =  new HashSet<>();
		standingOn(cells, this);
		tempBack();
		
		if(cells.contains(SOLID))
			return false;
		
		return !isOverlapping(targetX, targetY);
	}
	
	/**
	 * Tests if this unit can move one step up.
	 * Performs a tile inspection, solid object check and out of bounds control.
	 * @return True if this entity can move one step up.
	 */
	public boolean canGoUp()
	{
		return canGoUp(loc.y - 1);
	}
	
	/**
	 * Tests if this unit can move one step down.
	 * Performs a tile inspection, solid object check and out of bounds control.
	 * @return True if this entity can move one step down.
	 */
	public boolean canGoDown()
	{
		return canGoDown(loc.y + 1);
	}
	
	/**
	 * Tests if this unit can move one step left.
	 * Performs a tile inspection, solid object check and out of bounds control.
	 * @return True if this entity can move one step left.
	 */
	public boolean canGoLeft()
	{
		return canGoLeft(loc.x - 1);
	}
	
	/**
	 * Tests if this unit can move one step right.
	 * Performs a tile inspection, solid object check and out of bounds control.
	 * @return True if this entity can move one step right.
	 */
	public boolean canGoRight()
	{
		return canGoRight(loc.x + 1);
	}
	
	/**
	 * Use canGoTo instead.
	 */
	@Deprecated
	public boolean canGoLeft(float targetX)
	{
		if (!canMove)
			return false;
		
		int y = (int)loc.y, tar = (int) targetX;
		
		if(outOfBounds(tar,y))
			return false;
		
		byte[][] d  = Stage.STAGE.stageData;
		for (int i = 0; i < height; i++)
			if(d[y + i][tar] == SOLID)
				return false;
		
		if(isOverlapping(targetX, loc.y))
			return false;
		return true;
	}
	
	/**
	 * Use canGoTo instead.
	 */
	@Deprecated
	public boolean canGoRight(float targetX)
	{
		if (!canMove)
			return false;
		
		int y = (int)loc.y, tar = (int) (targetX + width);
		
		if(outOfBounds(tar, y))
			return false;
		
		byte[][] d = Stage.STAGE.stageData;
		for (int i = 0; i < height; i++)
			if(d[y + i][tar] == SOLID)
				return false;

		if(isOverlapping(targetX, loc.y))
			return false;
		return true;
	}
	
	/**
	 * Use canGoTo instead.
	 */
	@Deprecated
	public boolean canGoUp(float targetY)
	{		
		if (!canMove)
			return false;
		
		int x = (int)loc.x, tar = (int) targetY;
		
		if(outOfBounds(x,tar))
			return false;
		
		byte[][] d  = Stage.STAGE.stageData;
		for (int i = 0; i < width; i++)
			if(d[tar][x + i] == SOLID)
				return false;
		
		if(isOverlapping(loc.x, targetY))
			return false;
		return true;
	}
	
	/**
	 * Use canGoTo instead.
	 */
	@Deprecated
	public boolean canGoDown(float targetY)
	{
		if (!canMove)
			return false;
		
		int x = (int)loc.x, tar = (int) (targetY + height);

		if(outOfBounds(x,tar))
			return false;
		
		byte[][] d  = Stage.STAGE.stageData;
		for (int i = 0; i < width; i++)
			if(d[tar][x + i] == SOLID)
				return false;
		
		if(isOverlapping(loc.x, targetY))
			return false;
		return true;
	}
	
	/**
	 * Tries to move this unit upwards X amount of times, where X is {@code steps}.
	 * @param steps The amount of steps we should try to move upwards.
	 * @return True if all the steps was successful. 
	 */
	public boolean tryUp(float steps)
	{
		for (int i = 0; i < steps; i++)
		{
			if (canGoUp())
				loc.y--;
			else
				return false;
		}
		return true;
	}
	
	/**
	 * Tries to move this unit downwards X amount of times, where X is {@code steps}.
	 * @param steps The amount of steps we should try to move downwards.
	 * @return True if all the steps was successful. 
	 */
	public boolean tryDown(float steps)
	{
		for (int i = 0; i < steps; i++)
		{
			if (canGoDown())
				loc.y++;
			else
				return false;
		}
		return true;
	}
	
	/**
	 * Tries to move this unit leftwards X amount of times, where X is {@code steps}.
	 * @param steps The amount of steps we should try to move leftwards.
	 * @return True if all the steps was successful. 
	 */
	public boolean tryLeft(float steps)
	{
		for (int i = 0; i < steps; i++)
		{
			if (canGoLeft())
				loc.x--;
			else
				return false;
		}
		return true;
	}
	
	/**
	 * Tries to move this unit rightwards X amount of times, where X is {@code steps}.
	 * @param steps The amount of steps we should try to move rightwards.
	 * @return True if all the steps was successful. 
	 */
	public boolean tryRight(float steps)
	{
		for (int i = 0; i < steps; i++)
		{
			if (canGoRight())
				loc.x++;
			else
				return false;
		}
		return true;
	}
	
	/**
	 * Jumps back to the position this unit was standing on the previous frame.
	 */
	public void goBack()
	{
		loc.x = prevX;
		loc.y = prevY;
	}
	
	/**
	 * Moves to a temporary position, without performing any checks.<br>
	 * You can later use {@code tempBack()} to jump back to your original position.
	 * @param x The X coordinate to jump to.
	 * @param y The Y coordinate to jump to.
	 */
	protected void tempTo(float x, float y)
	{
		tempX = loc.x;
		tempY = loc.y;
		
		loc.x = x;
		loc.y = y;
	}
	
	/**
	 * Jumps back from your temporary position to your original position.
	 */
	protected void tempBack()
	{
		loc.x = tempX;
		loc.y = tempY;
	}
	
	/**
	 * Returns the X coordinate you were standing on the previous frame.
	 * @return The X coordinate.
	 */
	public float getPrevX()
	{
		return prevX;
	}
	
	/**
	 * Returns the Y coordinate you were standing on the previous frame.
	 * @return The Y coordinate.
	 */
	public float getPrevY()
	{
		return prevY;
	}
	
	/**
	 * "Walks" towards the specified point, using property {@code moveSpeed} as the "walking speed".
	 * @param targetX The X target coordinate to walk to.
	 * @param targetY The Y target coordinate to walk to.
	 */
	public void moveToward(float targetX, float targetY)
	{
		moveToward(targetX, targetY, moveSpeed);
	}
	
	/**
	 * "Walks" towards the specified point, using the given variable {@code steps} as the "walking speed".
	 * @param targetX The X target coordinate to walk to.
	 * @param targetY The Y target coordinate to walk to.
	 * @param steps The speed of the walking.
	 */
	public void moveToward(float targetX, float targetY, float steps)
	{
		if (canMove)
		{
		    float fX = targetX - loc.x;
		    float fY = targetY - loc.y;
		    double dist = Math.sqrt( fX*fX + fY*fY );
		    double step = steps / dist;

		    loc.x += fX * step;
		    loc.y += fY * step;
		}
	}
	
	/**
	 * Changes the previous position to the current position.<br>
	 * This function is only used when developing.
	 */
	public final void resetPrevs()
	{
		prevX = loc.x;
		prevY = loc.y;
	}
	
	/**
	 * This is used when creating movable platforms and causes the target object to follow this object.
	 * @param target The object that should follow this one.
	 * @param harsh True if you want to ignore walls(solid space) when ajusting the target.
	 */
	public void adjust(MovableObject target, boolean harsh)
	{
		float nextX = target.loc.x + loc.x - prevX;
		float nextY = target.loc.y + loc.y - prevY;
		
		if(target.canGoTo(nextX, nextY))
			target.moveTo(nextX, nextY);
		
		if(harsh)
			collisionRespone(target);
	}
	
	/**
	 * In case the two rectangles are colliding, push the {@code target} object away in a appropriate direction to the point that they are not colliding anymore.
	 * @param target The object to push away in case of collision.
	 */
	public void collisionRespone(MovableObject target)
	{
		if(collidesWithMultiple(target) == null)
			return;
		
		float centerX = loc.x + width() / 2;
		float centerY = loc.y + height() / 2;
		
        double overX = ((target.width()  + width() ) /  2.0) - Math.abs((target.loc.x + target.width()  / 2) - centerX);
        double overY = ((target.height() + height()) /  2.0) - Math.abs((target.loc.y + target.height() / 2) - centerY);
       
        if(overY > overX)
        {
            if(target.loc.x > centerX)
            	target.loc.x += overX;
            else
            	target.loc.x -= overX;
        }
        else
        {
        	if(target.loc.y > centerY)
        		target.loc.y += overY;
        	else
        		target.loc.y -= overY;
        }
	}
	
	/**
	 * Tells the stage what this MovableObject is currently "standing" on.<br>
	 * It is up to the map to decide what to happen.
	 */
	void inspectIntersections()
	{
		Stage stage = Stage.STAGE;
		
		for (byte value : occupyingCells)
		{
			switch (value)
			{
			case HOLLOW:
				stage.tileIntersection(this, HOLLOW);
				break;
			case LETHAL:
				stage.tileIntersection(this, LETHAL);
				break;
			case SOLID:
				stage.tileIntersection(this, SOLID);
				break;
			case GOAL:
				stage.tileIntersection(this, GOAL);
				break;
			case AREA_TRIGGER_0:
				stage.tileIntersection(this, AREA_TRIGGER_0);
				break;
			case AREA_TRIGGER_1:
				stage.tileIntersection(this, AREA_TRIGGER_1);
				break;
			case AREA_TRIGGER_2:
				stage.tileIntersection(this, AREA_TRIGGER_2);
				break;
			case AREA_TRIGGER_3:
				stage.tileIntersection(this, AREA_TRIGGER_3);
				break;
			case AREA_TRIGGER_4:
				stage.tileIntersection(this, AREA_TRIGGER_4);
				break;
			case AREA_TRIGGER_5:
				stage.tileIntersection(this, AREA_TRIGGER_5);
				break;
			case AREA_TRIGGER_6:
				stage.tileIntersection(this, AREA_TRIGGER_6);
				break;
			case AREA_TRIGGER_7:
				stage.tileIntersection(this, AREA_TRIGGER_7);
				break;
			case AREA_TRIGGER_8:
				stage.tileIntersection(this, AREA_TRIGGER_8);
				break;
			case AREA_TRIGGER_9:
				stage.tileIntersection(this, AREA_TRIGGER_9);
				break;
			}
		}
	}
	
	void tileCheck()
	{
		occupyingCells.clear();
		standingOn(occupyingCells, this);
	}
	
	/**
	 * Fills the given {@code Collection} with the tile types the given rectangle is standing on.
	 * @param cells The collection to fill.
	 * @param go The rectangle to check on.
	 */
	public static void standingOn (Collection<Byte> cells, GameObject go)
	{
		byte[][] d = Stage.STAGE.stageData;
		
		int x  = (int) go.loc.x + 1,
			y  = (int) go.loc.y + 1,
			x2 = (int) (go.loc.x + go.width  - 1),
		    y2 = (int) (go.loc.y + go.height - 1);
		
		for(int lx = x; lx < x2; lx++)
		{
			cells.add(d[y ][lx]);
			cells.add(d[y2][lx]);
		}
		for(int ly = y; ly < y2; ly++)
		{
			cells.add(d[ly][x]);
			cells.add(d[ly][x2]);
		}
	}
	
	/**
	 * Whether or not this {@code MovableObject} can interact with tile.<br>
	 * A triggerable object will get special treatment by the engine by calling its tile events if it is interacting with non-hollow tile.
	 * @param triggerable True if this unit should be triggerable.
	 */
	public void setTriggerable(boolean triggerable)
	{
		this.triggerable = triggerable;
	}
	
	/**
	 * {@code GameObjects} added to this method will act as solid objects.<br>
	 * {@code canGoTo} for example would return false if the given target is occupied by a solid object.
	 * @param mo
	 */
	public void avoidOverlapping(GameObject go)
	{
		solidObjects.add(go);
	}
	
	/**
	 * Reefer to {@code avoidOverlapping(GameObject)} for usage.
	 */
	public void avoidOverlapping(GameObject... gos)
	{
		for(GameObject go : gos)
			solidObjects.add(go);
	}
	
	/**
	 * Removes the specified unit from the solid object list.
	 * @param mo The entity to remove.
	 */
	public void allowOverlapping(GameObject mo)
	{
		solidObjects.remove(mo);
	}
	
	/**
	 * Moves this object to the specified point and checks if it collides with any solid object.
	 * @param targetX The X coordinate to check.
	 * @param targetY The Y coordinate to check.
	 * @return True if there is a collision, false if there isn't.
	 */
	protected boolean isOverlapping(float targetX, float targetY)
	{
		if(solidObjects.isEmpty())
			return false;
		
		boolean bool = false;
		
		float realX = loc.x,
			  realY = loc.y;
		loc.x = targetX;
		loc.y = targetY;

		for (GameObject go : solidObjects)
			if (collidesWithMultiple(go) != null)
			{
				bool = true;
				break;
			}
		loc.x = realX;
		loc.y = realY;
		
		return bool;
	}
	
	/**
	 * Checks if the specified point is out of the stages boundaries.
	 * @param targetX The X coordinate to check.
	 * @param targetY The Y coordinate to check.
	 * @return True if the given point is out of bounds.
	 */
	public static boolean outOfBounds(float x, float y)
	{
		Stage st = Stage.STAGE;
		
		if(x >= st.size.width  ||
		   y >= st.size.height || 
		   x < 0 ||
		   y < 0)
			return true;
		
		return false;
	}
	
	
	/**
	 * Stops this unit from moving.
	 */
	public void freeze()
	{
		canMove = false;
	}
	
	/**
	 * Allow this unit to move again.
	 */
	public void unfreeze()
	{
		canMove = true;
	}
	
	/**
	 * Checks if this unit is frozen.
	 * @return True if this unit is frozen.
	 */
	public boolean isFrozen()
	{
		return !canMove;
	}
	
	/**
	 * Tells the game to ignore this entity. No events or movement can be made. Even pushing it forces it back on its previous position.<br>
	 * Note: Does not stop it from being rendered.
	 * @param halt True to suspend the entity.
	 */
	public void halt(boolean halt)
	{
		this.halted = halt;
	}
	
	/**
	 * Test if the entity is currently moving.
	 * @return True if its moving.
	 */
	public boolean isMoving()
	{
		return loc.x != prevX || loc.y != prevY;
	}
	
	/**
	 * Sets the move speed of this unit.
	 * @param moveSpeed The move speed to use.
	 */
	public void setMoveSpeed(float moveSpeed)
	{
		this.moveSpeed = moveSpeed;
	}
	
	/**
	 * Returns the move speed of this unit.
	 * @return The move speed.
	 */
	public float getMoveSpeed()
	{
		return moveSpeed;
	}
	
	/**
	 * Adds an {@code TileEvent} to this unit.
	 * @param tileEvent The {@code TileEvent to add.
	 */
	public void addTileEvent (TileEvent tileEvent)
	{
		tileEvents.add(tileEvent);
	}
	
	/**
	 * Runs the {@code TileEvents} this unit holds, using the given tile as argument to the events.<br>
	 * This function is usually called automatically and not manually.
	 * @param type The tile type to pass to the TileEvents.
	 */
	public void runTileEvents(byte type)
	{
		for(TileEvent tileEvent : tileEvents)
			tileEvent.eventHandling(type);
	}
	
	/**
	 * Removes the given {@code TileEvent}.
	 * @param tileEvent The event to remove.
	 */
	public void removeTileEvent(TileEvent tileEvent)
	{
		if(tileEvent == null)
		{
			if(tileEvents.size() > 0)
				tileEvents.remove(0);
		}
		else
			tileEvents.remove(tileEvent);
	}
}