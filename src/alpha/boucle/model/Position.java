package alpha.boucle.model;

import java.util.HashMap;
import java.util.Map;

import alpha.boucle.process.ProcessUtil;

/**
 * Coordinate class. Private constructor, all position are in cache. A same position is the same object.
 *
 * @author Escape
 */
public class Position {

	public final short x;
	public final short y;

	/** All positions in cache. Probably should have make a double map to avoid redefined equals, but no matter */
	//private static Map<Position,Position> cache = new HashMap<>();
	private static Map<Integer,Position> cache = new HashMap<>();

	public static Position getPosition(int x, int y) {
		return getPosition((short)x, (short)y);
	}

	public static Position getPosition(short x, short y) {
		int key = x * 100000 + y;
		Position result = cache.get(key);
		if (result == null) {
			result = new Position(x, y);
			cache.put(key, result);
		}
		return result;
	}

	public static Position getPosition(String[] entry) {
		return getPosition(Short.parseShort(entry[0]), Short.parseShort(entry[1]));
	}

	private Position(short x, short y) {
		this.x = x;
		this.y = y;
	}

	/** Distance between p and the current position */
	public int distance(Position p) {
		return Math.abs(p.x - x) + Math.abs(p.y - y);
	}

	/** Check if position is not in range */
	public boolean isInRange(Position p) {
		return !isNotInRangeNMove(p, 1);
	}

	/** Check if position is not in range */
	public boolean isNotInRange(Position p) {
		return isNotInRangeNMove(p, 1);
	}

	/** Check if position is not in range in two moves */
	public boolean isNotInRangeTwoMove(Position p) {
		return isNotInRangeNMove(p, 2);
	}

	/** Check if position is not in range in two moves */
	public boolean isNotInRangeNMove(Position p, int nbMove) {
		int distanceX = Math.abs(p.x - x);
		if (distanceX > ProcessUtil.VISION * nbMove) {
			return true;
		}
		int distanceY = Math.abs(p.y - y);
		return (distanceY > ProcessUtil.VISION * nbMove || (distanceX + distanceY) > ProcessUtil.DISTANCE * nbMove);
	}

	public String toStringCopy() {
		return new StringBuilder().append(x).append("\t").append(y).toString();
	}

	@Override
	public String toString() {
		return new StringBuilder().append(x).append(",").append(y).toString();
	}
}
