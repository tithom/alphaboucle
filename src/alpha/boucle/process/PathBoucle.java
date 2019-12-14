package alpha.boucle.process;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;

import alpha.boucle.model.Node;
import alpha.boucle.model.Position;

/**
 * An A* path. Keep all the past position and keep some statistics
 *
 * @author Escape
 */
public class PathBoucle {

	/** To reduce memory usage, register in the map only the 10 last positions. */
	private static final int KEEPONLYPATH = 10;

	/** A path is calculated in many section. Contained the length of the previous sections of the path. */
	short nbPrevious = 0;

	/** The complete path of the current section. Use an Array to allow reuse position. */
	List<Node> path = new ArrayList<>();

	/** Register for each position the "cost", the average of the current path. */
	LinkedHashMap<Position, Short> pathCost = new LinkedHashMap<>();

	/** Path derived from this path. If a path is rejected, the derived path will be too. */
	List<PathBoucle> sons = new ArrayList<>();

	/** Total minerai of this path. */
	int totalMinerai = 0;

	/** Two last positions of this path to search for next position the path can go. Don't manage to die two times in a raw. */
	LinkedList<Node> lastPosition = new LinkedList<>();

	/** Target position of the path. */
	Position end = null;

	/** Min distance to end the path has gone. Start with an exagerate high value. */
	int minDistanceToEnd = 1000;

	/**
	 * Initialize the path with a start and a end position.
	 */
	public PathBoucle(Position start, Position end) {
		this.end = end;
		add(start);
	}

	/**
	 * Clone constructor. To derived a path with his future path.
	 *
	 * @param clone path to clone.
	 */
	public PathBoucle(PathBoucle clone) {
		path = new ArrayList<>(clone.path);
		Node lastNode = null;
		if (path.size() > 0) {
			int last = path.size() - 1;
			lastNode = path.get(last).clone();
			path.set(last, lastNode);
		}
		pathCost = new LinkedHashMap<>(clone.pathCost);
		totalMinerai = clone.totalMinerai;
		lastPosition = new LinkedList<>(clone.lastPosition);
		if (lastNode != null) {
			lastPosition.set(lastPosition.size() - 1, lastNode);
		}
		nbPrevious = clone.nbPrevious;
		end = clone.end;
		minDistanceToEnd = clone.minDistanceToEnd;
		clone.sons.add(this);
	}

	/**
	 * Continue the path by resetting it, keep distance and total minerais.
	 *
	 * @param end new end position.
	 */
	public void resetToContinue(Position end) {
		this.end = end;
		minDistanceToEnd = 1000;
		nbPrevious += path.size() - 1;
		sons.clear();
		path.clear();
		pathCost.clear();
		Position p = lastPosition.getLast().position;
		//lastPosition.clear();
		pathCost.put(p, average());
		add(p);
	}

	/**
	 * Add a position to the path.
	 *
	 * @param p the position.
	 * @return this
	 */
	public PathBoucle add(Position p) {
		minDistanceToEnd = Math.min(minDistanceToEnd, p.distance(end));
		Node n = new Node(p);
		path.add(n);
		totalMinerai += notNewCase(p)? 0 : n.getMinerai();
		calcIfDie(n);
		pathCost.put(p, average());
		if (KEEPONLYPATH > 0 && pathCost.size() > KEEPONLYPATH) {
			pathCost.remove(pathCost.keySet().iterator().next());
		}
		return this;
	}

	/**
	 * Check if the position is already used.
	 */
	public boolean notNewCase(Position p) {
		return pathCost.containsKey(p) || ProcessAS.caseVisited.contains(p);
	}

	/**
	 * Check if the previous node need to die and update the lastPosition.
	 */
	public void calcIfDie(Node target) {
		if (lastPosition.size() > 1) {
			Node last = lastPosition.getLast();
			if (last.distance(target) > ProcessUtil.DISTANCE
					|| Math.abs(last.position.x - target.position.x) > ProcessUtil.VISION
					|| Math.abs(last.position.y - target.position.y) > ProcessUtil.VISION) {
				last.die = true;
				lastPosition.removeLast();
			}
			else {
				lastPosition.removeFirst();
				last.die = false;
			}
		}
		lastPosition.addLast(target);
	}

	/**
	 * @return average of the path.
	 */
	public short average() {
		return (short) (totalMinerai / (path.size() + nbPrevious));
	}

	/**
	 * @return average if we add the node n.
	 */
	public int averageNext(Node n) {
		short minerai = notNewCase(n.position)? 0 : n.getMinerai();
		return (totalMinerai + minerai) / (path.size() + nbPrevious + 1);
	}

	/**
	 * @return true if the path in parameter has a better average. Not used.
	 */
	public boolean comparePath(PathBoucle p2) {
		return average() < p2.average();
	}

	/**
	 * Get the average of a position of the path.
	 */
	public int getPosCost(Position p) {
		return pathCost.get(p);
	}

	/**
	 * Clone the path.
	 */
	@Override
	public PathBoucle clone() {
		return new PathBoucle(this);
	}

	/**
	 * End the loop path by removing the last node which is the same as the first node.
	 */
	public void closeBoucle() {
		// remove last element
		path.remove(path.size()-1);
	}

	/**
	 * @return the average formatting with two decimals.
	 */
	public String averageFinal() {
		return String.format ("%,.2f", totalMinerai / (float) (path.size() + nbPrevious));
	}

	/**
	 * @return statistics of the path exept the path itself.
	 */
	public String resume() {
		return new StringBuilder().append("Trajet ").append(path.get(0).toCoordString()).append(" vers ").append(path.get(path.size()-1).toCoordString())
				.append(" - ").append("Nb cases : ").append(path.size() + nbPrevious).append(", moyenne = ").append(averageFinal())
				.append(", total = ").append(totalMinerai).toString();
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		if (path.get(0).equals(path.get(path.size()-1))) {
			nbPrevious--;
			// loop over, dont count the last case
		}

		sb.append(resume()).append("\n");
		String firstLine = sb.toString();
		for (Node n : path) {
			sb.append(n).append("\n");
		}
		sb.append(firstLine);
		return sb.toString();
	}
}
