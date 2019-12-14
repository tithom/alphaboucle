package alpha.boucle.process;

import static alpha.boucle.process.ProcessUtil.pos;
import static java.lang.Math.abs;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.TreeSet;

import alpha.boucle.data.CoordinatesLoader;
import alpha.boucle.model.Node;
import alpha.boucle.model.Position;


public final class ProcessAS {

	/** Distance a path can travel in opposite to target destination in one move */
	public static short ECART_ALLOW = 2;
	/** Distance a path can traval in opposite to target destination compared to closest it went */
	public static short ECART_GLOBAL_ALLOW = 10;

	/** average threshold of bad path in 0/5/10/20/30/50/100 moves */
	public static int[] seuil;

	/** Starting position of the section */
	Position start;
	/** Target position of the section */
	Position end;

	/** The complete path calculated */
	private PathBoucle calculPath;

	/** Case visited in previous sections */
	public static Set<Position> caseVisited = new HashSet<>();
	/** Path of the previous sections */
	private static List<Node> cumulPath = new ArrayList<>();

	/**
	 * Continue a path.
	 *
	 * @param pEnd new target position.
	 * @param began to go to the next case before starting the search to go to the target.
	 *
	 * @return true if a path has been found.
	 */
	public boolean continuePath(Position pEnd, Position... next) {
		end = pEnd;

		// change the restriction of the position to the next target
		CoordinatesLoader.setNextPosition(pEnd);

		// append the current section of the path
		appendPath();

		// reset the objects to continue the path
		cumulPath.remove(calculPath.path.get(calculPath.path.size()-1));
		calculPath.resetToContinue(end);

		Position start = calculPath.path.get(0).position;
		LogResult.out("Trajet " + start + " vers " + end + " - Previous = " + calculPath.nbPrevious);
		// append the "force" case to go
		for (Position position : next) {
			calculPath.add(position);
			start = position;
		}
		// launch the A* process to the next section
		return process();
	}

	/** Append current section to the path. */
	private void appendPath() {
		cumulPath.addAll(calculPath.path);
		for (Node n : cumulPath) {
			caseVisited.add(n.position);
		}
	}

	/***
	 * A* process
	 *
	 * @return true if a path has been found.
	 */
	public boolean process() {
		Map<Position, PathBoucle> currentBestPaths = new HashMap<>();
		PathBoucle bestCompletePath = null; // complete path with the best average
		PathBoucle closestPath = calculPath; // return no complete path has been found
		PathBoucle bestCurrentPath = calculPath; // current path with the best average

		// open paths to process
		Queue<PathBoucle> openPaths = new ArrayDeque<>();
		openPaths.add(calculPath);

		int i = 0;
		long startTime = System.currentTimeMillis();

		while(!openPaths.isEmpty()) {
			i++;
			// extract next open path to process from the queue
			PathBoucle currentPath = openPaths.poll();

			int distanceToEnd = currentPath.lastPosition.getLast().position.distance(end);
			// if the past has reached the end or is longer than 400 (security, could not really happened).
			if (currentPath.path.size() >= 1 && distanceToEnd == 0 || currentPath.path.size() > 400) {
				// if better than another complete bath, set it has the new complete path
				if (bestCompletePath == null || currentPath.average() > bestCompletePath.average()) {
					bestCompletePath = currentPath;
				}
			}
			else {
				// check if the path should reject
				if (closingCondition(bestCompletePath, bestCurrentPath, currentPath)) {
					continue;
				}

				// set the best current average to eliminate too bad path
				if (currentPath.average() > bestCurrentPath.average()) {
					bestCurrentPath = currentPath;
				}
				// set closest path if no path reach the end
				if (distanceToEnd < closestPath.lastPosition.getLast().distance(end)) {
					closestPath = currentPath;
				}

				Set<Position> accessibleCases = getAllAccessiblePosition(currentPath);

				for (Position newPos : accessibleCases) {
					// for each position, check if the position has already been access from another path.
					// continue the path only if the average is better, and drop the other path if it's the case.
					PathBoucle currentBest = currentBestPaths.get(newPos);
					Node newNode = new Node(newPos);
					int currentAverage = currentPath.averageNext(newNode);
					int bestAverage = (currentBest == null)? 0 : currentBest.getPosCost(newPos);
					if (currentAverage > bestAverage || currentAverage == bestAverage
							&& currentPath.path.size() < currentBest.path.size()) {
						PathBoucle currentNext = currentPath.clone().add(newPos);
						cleanOpenBadPath(openPaths, currentBest, currentPath);
						currentBestPaths.put(newPos, currentNext);
						// add sons path to process
						openPaths.add(currentNext);
					}
					// else path is dropped (no son added to the queue)
				}
			}
		}

		// check if a complete path has been found.
		boolean success = bestCompletePath != null;
		if (success) {
			if (!cumulPath.isEmpty() && cumulPath.get(0).position.equals(end)) {
				bestCompletePath.closeBoucle();
			}
			LogResult.out(bestCompletePath.toString());
			calculPath = bestCompletePath;
		}
		else {
			LogResult.out("CURRENT " + bestCurrentPath);
			LogResult.out("CLOSEST " + closestPath);
			calculPath = closestPath;
		}
		float duration = (System.currentTimeMillis() - startTime) / 1000f;
		LogResult.out("Nb calcul : " + i + ", Duration : " + duration + "\n");

		// force cleaning memory
		openPaths.clear();
		currentBestPaths.clear();
		System.gc();

		return success;
	}

	/**
	 * Condition to stop analyzing a path because it has a too bad average.
	 *
	 * @param bestCompletePath current best complete path
	 * @param bestCurrentPath current best path not complete
	 * @param currentPath current path to check
	 * @return
	 */
	private boolean closingCondition(PathBoucle bestCompletePath, PathBoucle bestCurrentPath, PathBoucle currentPath) {
		return bestCompletePath != null && bestCompletePath.average() * 8 / 10f > currentPath.average()
				//|| bestCurrentPath != null && bestCurrentPath.average() / 10f > currentPath.average()
				|| currentPath.average() < seuil[0]
						|| currentPath.path.size() + currentPath.nbPrevious > 5 && currentPath.average() < seuil[1]
								|| currentPath.path.size() + currentPath.nbPrevious > 10 && currentPath.average() < seuil[2]
										|| currentPath.path.size() + currentPath.nbPrevious > 20 && currentPath.average() < seuil[3]
												|| currentPath.path.size() + currentPath.nbPrevious > 30 && currentPath.average() < seuil[4]
														|| currentPath.path.size() + currentPath.nbPrevious > 50 && currentPath.average() < seuil[5]
																|| currentPath.path.size() + currentPath.nbPrevious > 100 && currentPath.average() < seuil[6]
																		;
	}

	/***
	 * Drop a path that are worst than another path and any sons of it.
	 *
	 * @param openPaths the list of the opens path
	 * @param badPath the path to drop
	 * @param currentPath the current path (to avoid to drop itself)
	 */
	private void cleanOpenBadPath(Queue<PathBoucle> openPaths, PathBoucle badPath, PathBoucle currentPath) {
		if (badPath != null && badPath != currentPath && !badPath.sons.contains(currentPath)) {
			openPaths.remove(badPath);
			for (PathBoucle sonBadPath : badPath.sons) {
				cleanOpenBadPath(openPaths, sonBadPath, currentPath);
			}
		}
	}

	/**
	 * Get all accessible position from the current path.
	 * Check the position accessible from the two previous position (if the last die).
	 *
	 * @param currentPath the path
	 * @return the list of the position to go
	 */
	private Set<Position> getAllAccessiblePosition(PathBoucle currentPath) {
		LinkedList<Node> source = currentPath.lastPosition;
		Position pFirst = source.getFirst().position;
		Position pLast = source.getLast().position;
		int distanceMax = Math.max(pFirst.distance(end),pLast.distance(end));
		Set<Position> pSource = new LinkedHashSet<>();
		pSource.add(pLast);
		pSource.add(pFirst);

		// use a treeset to make the randomness disappear.
		// I had changed the position/node object and that make the path calculated different because the set was not order the same way.
		// Calculate first the node with the best average, and then closer to the end.
		Set<Position> result = new TreeSet<>(new Comparator<Position>() {
			@Override
			public int compare(Position o1, Position o2) {
				int diff = ((currentPath.pathCost.containsKey(o2) || caseVisited.contains(o2))? 0 : CoordinatesLoader.getCalcMinerai(o2)) -
						((currentPath.pathCost.containsKey(o1) || caseVisited.contains(o1))? 0 : CoordinatesLoader.getCalcMinerai(o1));
				if (diff == 0) {
					diff = o2.distance(end) - o1.distance(end);
				}
				if (diff == 0) {
					diff = 1;
				}
				return diff;
			}
		});

		//Set<Position> result = new HashSet<>();

		// add all the position accessible from both source position (the last and the before last)
		for (Position p : pSource) {
			for (int x = -ProcessUtil.DISTANCE ; x <= ProcessUtil.DISTANCE ; x++) {
				if (Math.abs(x) > ProcessUtil.VISION) { continue; }
				int remainingDistance = ProcessUtil.DISTANCE - Math.abs(x);
				for (int y = -remainingDistance ; y <= remainingDistance ; y++) {
					if (Math.abs(y) > ProcessUtil.VISION) { continue; }
					if (x != 0 || y != 0) {
						addAccessiblePosition(currentPath, result, distanceMax, p,
								(p == pFirst)? pLast : pFirst, x, y);
					}
				}
			}
		}
		return result;
	}

	/**
	 * Add new accessible position if some condition are verified.
	 *
	 * @param currentPath the path
	 * @param result the set to add the position
	 * @param oldDistanceToEnd the distance to end before moving
	 * @param source old position 2
	 * @param otherSource old position 2
	 * @param x shift x from the source of the new pos
	 * @param y shift y from the source of the new pos
	 */
	private void addAccessiblePosition(PathBoucle currentPath, Set<Position> result, int oldDistanceToEnd, Position source, Position otherSource, int x, int y) {
		Position newPos = pos(source.x + x, source.y + y);
		int newPosDistToEnd = newPos.distance(end);
		if (newPosDistToEnd == 0 || !caseVisited.contains(newPos) && !newPos.equals(otherSource)
				&& (newPosDistToEnd <= oldDistanceToEnd || newPosDistToEnd <= oldDistanceToEnd + ECART_ALLOW
				&& newPosDistToEnd <= currentPath.minDistanceToEnd + ECART_GLOBAL_ALLOW
				&& checkInterestingPosition(source, otherSource, newPos))) {
			result.add(newPos);
		}
	}

	/**
	 * Forbid to go the an intermediate position (
	 *
	 * @param old1 old position 1
	 * @param old2 old position 2
	 * @param newPos new position to go
	 * @return
	 */
	private boolean checkInterestingPosition(Position old1, Position old2, Position newPos) {
		int o1x = abs(old1.x);
		int o1y = abs(old1.y);
		int o2x = abs(old2.x);
		int o2y = abs(old2.y);
		int nx = abs(newPos.x);
		int ny = abs(newPos.y);

		if (nx >= o1x && nx >= o2x || nx <= o1x && nx <= o2x) {
			// outside x range
			return true;
		}
		if (ny >= o1y && ny >= o2y || ny <= o1y && ny <= o2y) {
			// outside y range
			return true;
		}
		//		if (nx + ny <= o1x + o1y && nx + ny <= o2x + o2y) {
		//			// closer to the end
		//			// edit : wtf? closer to zero, not the end.
		//			return true;
		//		}
		return false;
	}


	/**
	 * Initialize the path.
	 *
	 * @param pStart start position
	 * @param pEnd target position
	 * @param next force position to go after the start position
	 */
	public void init(Position pStart, Position pEnd, Position... next) {

		start = pStart;
		end = pEnd;

		CoordinatesLoader.setNextPosition(pEnd);

		calculPath = new PathBoucle(start, end);
		for (Position position : next) {
			calculPath.add(position);
		}
	}

	/**
	 * Finish the path calculated, check it and display it.
	 *
	 * @param displayPath the path.
	 */
	public void finish(boolean displayPath) {
		caseVisited.addAll(calculPath.pathCost.keySet());
		cumulPath.addAll(calculPath.path);

		ProcessUtil.checkPath(cumulPath);
		LogResult.out("");
		ProcessUtil.displayPath(cumulPath, displayPath, true);
	}



}
