package alpha.boucle.process;

import static alpha.boucle.process.ProcessUtil.DISTANCE;
import static alpha.boucle.process.ProcessUtil.VISION;
import static alpha.boucle.process.ProcessUtil.pos;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;
import java.util.stream.Collectors;

import alpha.boucle.data.CoordinatesLoader;
import alpha.boucle.model.ChangePathOpti;
import alpha.boucle.model.Node;
import alpha.boucle.model.Position;

/**
 * Optimize a path by searching to optimize each node.
 * - Should a node be deleted
 * - Should a node be moved
 * - Should a node be split in two
 * - Should two node be merge in a new one
 * - In each case, should dying nodes should be delete or added
 *
 * At the end of the process, redo it if the path has been modified.
 *
 * @author Escape
 */
public class ProcessOpti {


	/** The path to optimize */
	LinkedList<Node> processPath = new LinkedList<>();

	/** Cache to accelerate the process but lead to high memory usage (almost 1 Go). Needed for replaceAnyByThree */
	private Map<Position, Map<Position, List<Position>>> cache = new HashMap<>();
	private static final boolean CACHE = true;
	private static final boolean REPLACE_BY_THREE = true;

	Map<Integer, Long> checkDuration = null;

	/** Same path in a set to optimize the "contains" test */
	Set<Position> allPathPosition = new HashSet<>();
	/**  Current average of the path */
	float pathMoyenne = 0;
	/** Current total minerai of the path */
	int totalMinerai = 0;

	/** Minimize size a path should get. Used in one specific case, not a absolute constraint */
	private static final int PATH_SIZE_EXPECTED = 200;

	/** Number of minerai needed to increase the size of the path for each iteration. Can be negative. */
	private int[] tabIncreaseLoopMinima = new int[] { 0 };

	/** Number of minerai needed to decrease the size of the path for each iteration. Can probably be negative too. */
	private int[] tabDecreaseLoopMinima = new int[] { 0 };

	/** Current number of minerai needed to increase the size of the path. */
	private int increaseLoopMinima = 0;
	/** Current number of minerai needed to decrease the size of the path. */
	private int decreaseLoopMinima = 0;

	/** True if an optimization has been found. */
	boolean isChange = false;

	/** Position deleted this optimization. Used in a specific case to get exactly to 200 positions */
	private List<Position> posDeleted = new ArrayList<>();

	/** Position added this optimization. Used in a specific case to get exactly to 200 positions */
	private List<Position> posAdded = new ArrayList<>();

	public ProcessOpti(String fileBoucle) {
		init(fileBoucle);

		//		checkDuration = new HashMap<>();
		//		for (int i = 0; i < 8; i++) {
		//			checkDuration.put(i, 0l);
		//
		//		}
	}

	/**
	 * Remove a node from the path.
	 *
	 * @param n the node
	 */
	public void removeNode(Node n) {
		posDeleted.add(n.position);
		pathMoyenne = calcNewMoyenne(n);
		processPath.remove(n);
		allPathPosition.remove(n.position);
		totalMinerai -= n.getMinerai();
	}

	/**
	 * Add a node to the path.
	 *
	 * @param n the node
	 */
	public void addNode(Node n, int index) {
		posAdded.add(n.position);
		pathMoyenne = calcNewMoyenne(null, n);
		processPath.add(index, n);
		allPathPosition.add(n.position);
		totalMinerai += n.getMinerai();
	}

	/**
	 * Init the path with a file.
	 *
	 * @param fileBoucle the file containing the path.
	 */
	private void init(String fileBoucle) {
		CoordinatesLoader.loadCoordFromFile(fileBoucle, true);
		Set<Position> posRoute = CoordinatesLoader.getExisingRoute();

		// init list
		for (Position position : posRoute) {
			short minerai = CoordinatesLoader.getMinerai(position);
			processPath.add(new Node(position, minerai));
			totalMinerai += minerai;
			allPathPosition.add(position);
		}
		// calcul die flag
		int pathSize = processPath.size();
		for (int i = 1 ; i < pathSize ; i++) {
			int index = i;
			Node previousNode = null;
			do {
				previousNode = processPath.get(--index);
			} while (previousNode.die);
			Node currentNode = processPath.get(i);
			currentNode.die = false;

			Node previousNodeLoop = previousNode;
			while (currentNode.isNotInRange(previousNodeLoop)) {
				previousNodeLoop.die = true;
				if (index-1 < 0) {
					// the path is not correct
					break;
				}
				previousNodeLoop = processPath.get(--index);
			}
		}

		pathMoyenne = totalMinerai / (float) (processPath.size());

		ProcessUtil.checkPath(processPath);
		//ProcessUtil.displayPath(processPath, true, true);
		ProcessUtil.displayPath(processPath, false, false);
	}

	/**
	 * Initialize the first N element of a nuplet
	 *
	 * @param start position to start search the N element
	 * @param nbItem nb element to get
	 * @param mapResult the result map
	 * @return index to search the next node. Decremente it if getFirstNodes is called inside the loop (it will be incremented by the loop)
	 */
	private int getFirstNodes(int start, int nbItem, LinkedHashMap<Node, List<Node>> mapResult) {
		int index = start;
		Node currentNode = null;
		int nb = 0;
		for ( ; index < processPath.size() ; index++) {
			Node node = processPath.get(index);
			if (node.die) {
				if (currentNode == null) {
					continue;
				}
				mapResult.get(currentNode).add(node);
			}
			else {
				if (nb == nbItem) {
					// dont change the loop condition to get the die node of the last inter node
					break;
				}
				mapResult.put(node, new ArrayList<>());
				currentNode = node;
				nb++;
			}
		}
		return index;
	}

	/**
	 * Launch one complete iteration on the path.
	 * @return
	 */
	private boolean optimize() {
		isChange = false;
		processQuintuplet();
		processQuadruplet();
		processTriplet();
		return isChange;
	}

	/***
	 * Optimization to process on each quintuplet of nodes.
	 * - Call searchReplaceTwoByOne to check if the two inside nodes should be merge in a new one
	 */
	private void processQuintuplet() {
		Node previous = null;
		Node inter1 = null;
		Node inter2 = null;
		Node inter3 = null;
		List<Node> dieNodes1 = new ArrayList<>();
		List<Node> dieNodes2 = new ArrayList<>();
		List<Node> dieNodes3 = new ArrayList<>();

		// initialize the four first node which don't die (previous, inter1 and inter2)
		LinkedHashMap<Node, List<Node>> mapStart = new LinkedHashMap<>();
		int start = getFirstNodes(0, 4, mapStart);
		Node[] keyNodes = mapStart.keySet().toArray(new Node[0]);
		previous = keyNodes[0];
		inter1 = keyNodes[1];
		inter2 = keyNodes[2];
		inter3 = keyNodes[3];
		dieNodes1 = mapStart.get(inter1);
		dieNodes2 = mapStart.get(inter2);
		dieNodes3 = mapStart.get(inter3);

		for (int index=start ; index < processPath.size() ; index++) {
			Node next = processPath.get(index);
			if (next.die) {
				dieNodes3.add(next);
				continue;
			}

			List<Node> dieNodes = new ArrayList<>(dieNodes1);
			dieNodes.addAll(dieNodes2);
			dieNodes.addAll(dieNodes3);
			inRangeOfAllDieNode(previous.position, dieNodes); // clear die position accessible from previous position
			inRangeOfAllDieNode(next.position, dieNodes); // clear die position accessible from next position

			ChangePathOpti changePath = searchReplaceAnyByAny(previous, next, dieNodes, inter1, inter2, inter3);
			if (changePath.isChange()) {
				changePath.apply(this, previous, next);
				// reset the nodes with the modification and redo the same loop turn
				mapStart = new LinkedHashMap<>();
				index = getFirstNodes(processPath.indexOf(previous), 4, mapStart) - 1;
				keyNodes = mapStart.keySet().toArray(new Node[0]);
				previous = keyNodes[0];
				inter1 = keyNodes[1];
				inter2 = keyNodes[2];
				inter3 = keyNodes[3];
				dieNodes1 = mapStart.get(inter1);
				dieNodes2 = mapStart.get(inter2);
				dieNodes3 = mapStart.get(inter3);
			}
			else {
				// no modification has been done, shift the quadruplet
				previous = inter1;
				inter1 = inter2;
				inter2 = inter3;
				inter3 = next;

				// shift the die nodes
				dieNodes1 = dieNodes2;
				dieNodes2 = dieNodes3;
				dieNodes3 = new ArrayList<>();
			}
		}

		// check last node (close the path loop)
		if (inter3 != null) {
			dieNodes1.addAll(dieNodes2);
			dieNodes1.addAll(dieNodes3);
			ChangePathOpti changePath = searchReplaceAnyByAny(previous, processPath.get(0), dieNodes1, inter1, inter2, inter3);
			if (changePath.isChange()) {
				changePath.apply(this, previous, processPath.get(0));
			}
		}
	}

	/***
	 * Optimization to process on each quadruplet of nodes.
	 * - Call searchReplaceTwoByOne to check if the two inside nodes should be merge in a new one
	 */
	private void processQuadruplet() {
		Node previous = null;
		Node inter1 = null;
		Node inter2 = null;
		List<Node> dieNodes1 = new ArrayList<>();
		List<Node> dieNodes2 = new ArrayList<>();

		// initialize the three first node which don't die (previous, inter1 and inter2)
		LinkedHashMap<Node, List<Node>> mapStart = new LinkedHashMap<>();
		int start = getFirstNodes(0, 3, mapStart);
		Node[] keyNodes = mapStart.keySet().toArray(new Node[0]);
		previous = keyNodes[0];
		inter1 = keyNodes[1];
		inter2 = keyNodes[2];
		dieNodes1 = mapStart.get(inter1);
		dieNodes2 = mapStart.get(inter2);

		for (int index=start ; index < processPath.size() ; index++) {
			Node next = processPath.get(index);
			if (next.die) {
				dieNodes2.add(next);
				continue;
			}

			List<Node> dieNodes = new ArrayList<>(dieNodes1);
			dieNodes.addAll(dieNodes2);
			inRangeOfAllDieNode(previous.position, dieNodes); // clear die position accessible from previous position
			inRangeOfAllDieNode(next.position, dieNodes); // clear die position accessible from next position

			ChangePathOpti changePath = searchReplaceAnyByAny(previous, next, dieNodes, inter1, inter2);
			if (changePath.isChange()) {
				changePath.apply(this, previous, next);
				// reset the nodes with the modification and redo the same loop turn
				mapStart = new LinkedHashMap<>();
				index = getFirstNodes(processPath.indexOf(previous), 3, mapStart) - 1;
				keyNodes = mapStart.keySet().toArray(new Node[0]);
				previous = keyNodes[0];
				inter1 = keyNodes[1];
				inter2 = keyNodes[2];
				dieNodes1 = mapStart.get(inter1);
				dieNodes2 = mapStart.get(inter2);
			}
			else {
				// no modification has been done, shift the quadruplet
				previous = inter1;
				inter1 = inter2;
				inter2 = next;

				// shift the die nodes
				dieNodes1 = dieNodes2;
				dieNodes2 = new ArrayList<>();
			}
		}

		// check last node (close the path loop)
		if (inter2 != null) {
			dieNodes1.addAll(dieNodes2);
			ChangePathOpti changePath = searchReplaceAnyByAny(previous, processPath.get(0), dieNodes1, inter1, inter2);
			if (changePath.isChange()) {
				changePath.apply(this, previous, processPath.get(0));
			}
		}
	}

	/***
	 * Optimization to process on each triplet of nodes.
	 * - Call two functions to optimize the middle node of the triplet.
	 */
	private void processTriplet() {
		Node previous = null; // previous node
		Node inter1 = null; // middle node
		List<Node> dieNodes = new ArrayList<>(); // dying node

		// initialize the two first node which don't die (lastNotDie and nodeToOptimize)
		LinkedHashMap<Node, List<Node>> mapStart = new LinkedHashMap<>();
		int start = getFirstNodes(0, 2, mapStart);
		Node[] keyNodes = mapStart.keySet().toArray(new Node[0]);
		previous = keyNodes[0];
		inter1 = keyNodes[1];
		dieNodes = mapStart.get(inter1);

		for (int index=start ; index < processPath.size() ; index++) {
			Node next = processPath.get(index);
			if (next.die) {
				if (next.getMinerai() < pathMoyenne - decreaseLoopMinima) {
					isChange = true;
					LogResult.out("L00 - Remove bad dying node " + next.toStringShort());
					removeNode(next);
					index--;
				}
				else {
					dieNodes.add(next);
				}
				continue;
			}
			inRangeOfAllDieNode(previous.position, dieNodes); // clear die position accessible from next case
			inRangeOfAllDieNode(next.position, dieNodes); // clear die position accessible from next case

			ChangePathOpti changePath = searchReplaceAnyByAny(previous, next, dieNodes, inter1);
			if (changePath.isChange()) {
				changePath.apply(this, previous, next);
				// reset the nodes with the modification and redo the same loop turn
				mapStart = new LinkedHashMap<>();
				index = getFirstNodes(processPath.indexOf(previous), 2, mapStart) - 1;
				keyNodes = mapStart.keySet().toArray(new Node[0]);
				previous = keyNodes[0];
				inter1 = keyNodes[1];

				dieNodes = mapStart.get(inter1);
			}
			else {
				previous = inter1;
				inter1 = next;
				dieNodes.clear();
			}
		}

		// check last node (close the path loop). but don't check to delete the start position (marchand)
		ChangePathOpti changePath = searchReplaceAnyByAny(previous, processPath.get(0), dieNodes, inter1);
		if (changePath.isChange()) {
			changePath.apply(this, previous, processPath.get(0));
		}
	}

	/**
	 * Call all search replace function and stop as soon as one function find an optimization.
	 * Set the isChange flag.
	 *
	 * @param previous node before the nodes to optimize
	 * @param next node after the nodes to optimize
	 * @param dieNodes node that currently die in range of the middle nodes
	 * @param inter nodes to move or to delete
	 * @return ChangePathOpti the optimization to do to the path
	 */
	private ChangePathOpti searchReplaceAnyByAny(Node previous, Node next, List<Node> dieNodes, Node... inter) {
		ChangePathOpti changePath = new ChangePathOpti(null, null);
		List<Node> dieNodesWork = new ArrayList<>(dieNodes);
		if (REPLACE_BY_THREE) {
			changePath = searchReplaceAnyByThree(previous, next, dieNodesWork, inter);
		}
		if (!changePath.isChange()) {
			changePath = searchReplaceAnyByTwo(previous, next, dieNodesWork, inter);
			if (!changePath.isChange()) {
				changePath = searchReplaceAnyByOne(previous, next, dieNodesWork, inter);
				if (!changePath.isChange()) {
					changePath = searchReplaceAnyByNone(previous, next, dieNodesWork, inter);
				}
			}
		}
		if (changePath.isChange()) {
			isChange = true;
		}
		return changePath;
	}

	/**
	 * Check optimization for the inter nodes to be deleted.
	 * Search for die node to be delete (if outside the range of the deleted nodes)
	 *
	 * @param previous node before the nodes to optimize
	 * @param next node after the nodes to optimize
	 * @param dieNodes node that currently die in range of the middle nodes
	 * @param oldInter nodes to move or to delete
	 * @return ChangePathOpti the optimization to do to the path
	 */
	private ChangePathOpti searchReplaceAnyByNone(Node previousNode, Node nextNode, List<Node> dieNodes, Node... oldInter) {
		
		ChangePathOpti changePath = new ChangePathOpti("L" + (oldInter.length + 2) + "0", dieNodes);

		// check for node to be deleted
		if (previousNode.isInRange(nextNode)) {
			List<Node> deletedNode = new ArrayList<>(Arrays.asList(oldInter));
			deletedNode.addAll(dieNodes);
			float moyenneInterNode = Node.getMoyenne(deletedNode.toArray(new Node[0]));
			boolean reallyBadNode = moyenneInterNode < pathMoyenne - decreaseLoopMinima;

			if (reallyBadNode) {
				changePath.setDeletedPathNodes(oldInter);
				changePath.setDeletedDieNodes(dieNodes);
			}
		}

		return changePath;
	}

	/**
	 * Check optimization for the inter nodes to be replace with one other.
	 * Search for die node to be delete (if outside the range of the new node) or to be added.
	 *
	 * @param previous node before the nodes to optimize
	 * @param next node after the nodes to optimize
	 * @param dieNodes node that currently die in range of the middle nodes
	 * @param oldInter nodes to move or delete
	 * @return ChangePathOpti the optimization to do to the path
	 */
	private ChangePathOpti searchReplaceAnyByOne(Node previous, Node next, List<Node> dieNodes, Node... oldInter) {
		ChangePathOpti changePath = new ChangePathOpti("L" + (oldInter.length + 2) + "1", dieNodes);

		if (previous.position.isNotInRangeTwoMove(next.position)) {
			// ignore optimization possible with a simple delete / ignore if impossible
			return changePath;
		}

		Node bestInterNode = null; // current better node
		List<Node> nodeDieToDelete = new ArrayList<>();
		List<Node> nodeDieToAdd = new ArrayList<>();
		float currentBestMoyenne = pathMoyenne;

		Collection<Position> interCases = getCommonAccessiblePosition(previous.position, next.position);
		if (oldInter.length == 1) {
			// to search for die node
			interCases.add(oldInter[0].position);
		}
		for (Position inter : interCases) {
			if (allPathPosition.contains(inter) && (oldInter.length != 1 || oldInter[0].position != inter)) {
				// avoid existing position except for the one case to search die node without moving
				continue;
			}

			Node interNew = new Node(inter);

			List<Node> dieNodeOutOfRange = new ArrayList<>(dieNodes);
			inRangeOfAllDieNode(inter, dieNodeOutOfRange);

			List<Node> nodesToDel = new ArrayList<>(dieNodeOutOfRange);
			nodesToDel.addAll(Arrays.asList(oldInter));

			List<Node> nodesToAdd = new ArrayList<>();
			nodesToAdd.add(interNew);

			// average with simple replacement without added dying nodes
			float moveBestMoyenne = calcNewMoyenneArray(nodesToDel, nodesToAdd);

			// check with die node
			Collection<Position> accessibleDiePosition = getAccessiblePosition(inter, interCases);
			List<Node> dieNodesToAdd = new ArrayList<>();
			float dieAndMoveBestMoyenne = searchForNodeToDieWithMove(accessibleDiePosition, currentBestMoyenne, dieNodesToAdd, interNew, nodesToDel);
			if (!dieNodesToAdd.isEmpty() && dieAndMoveBestMoyenne > moveBestMoyenne) {
				// calculate if the average is beat by taking account the decrease or increase threshold (in case there is less or more node after).
				float bestMoyenne = currentBestMoyenne;
				int diffNode = dieNodesToAdd.size() - dieNodeOutOfRange.size() - oldInter.length + 1;
				if (diffNode < 0) {
					bestMoyenne = calcDecreaseMoyenne(currentBestMoyenne, diffNode);
				}

				if (dieAndMoveBestMoyenne > bestMoyenne) {
					bestInterNode = interNew;
					currentBestMoyenne = dieAndMoveBestMoyenne;
					nodeDieToDelete = dieNodeOutOfRange;
					nodeDieToAdd = dieNodesToAdd;
				}
			}
			else if (moveBestMoyenne > currentBestMoyenne && oldInter[0].position != inter) {
				// calculate if the average is beat by taking account the decrease threshold
				int nbLooseMinerai = -interNew.getMinerai();
				for (Node nOld : oldInter) {
					nbLooseMinerai += nOld.getMinerai();
				}
				float nbDecrease = oldInter.length - 1 + dieNodeOutOfRange.size();
				for (Node dieNode : dieNodeOutOfRange) {
					nbLooseMinerai += dieNode.getMinerai();
				}
				if (nbLooseMinerai / nbDecrease < currentBestMoyenne - decreaseLoopMinima) {
					bestInterNode = interNew;
					currentBestMoyenne = moveBestMoyenne;
					nodeDieToDelete = dieNodeOutOfRange;
					nodeDieToAdd.clear();
				}
			}
		}

		if (bestInterNode != null) {
			if (oldInter.length == 1 && oldInter[0].position == bestInterNode.position) {
				changePath.setUnchangedNodes(oldInter);
			}
			else {
				changePath.setDeletedPathNodes(oldInter);
				changePath.setAddedPathNodes(bestInterNode);
			}

			changePath.setDeletedDieNodes(nodeDieToDelete);
			changePath.setAddedDieNodes(nodeDieToAdd);
		}

		return changePath;
	}

	/**
	 * Check optimization for the inter nodes to be replace with two others.
	 * Limitation: don't allow to drop die node, the new nodes must be in range of all the die nodes.
	 *
	 * @param previous node before the nodes to optimize
	 * @param next node after the nodes to optimize
	 * @param dieNodes node that currently die in range of the middle nodes
	 * @param oldInter nodes to move or delete
	 * @return ChangePathOpti the optimization to do to the path
	 */
	private ChangePathOpti searchReplaceAnyByTwo(Node previous, Node next, List<Node> dieNodes, Node... oldInter) {

		ChangePathOpti changePath = new ChangePathOpti("L" + (oldInter.length + 2) + "2", dieNodes);

		if (previous.position.isNotInRangeNMove(next.position, 3)) {
			return changePath;
		}

		List<Node> deleteNodes = new ArrayList<>();
		deleteNodes.addAll(Arrays.asList(oldInter));
		List<Position> deletePosition = deleteNodes.stream().map(a -> a.position).collect(Collectors.toList());

		// get first replacement node
		Set<Position> alternatives = getAccessiblePositionNMove(previous.position, next.position, 2);
		if (oldInter.length == 1) {
			// allow inter position replace one by two because there is no function add intermediate node from nowhere: it simulate replaceNoneByOne
			// note: if we want to delete the oldInter.length condition, we need to check the range of the dying node two
			// but it's useless to check existing position in any other case
			for (Position delPos : deletePosition) {
				if (previous.isInRange(delPos) && !delPos.isNotInRangeTwoMove(next.position)) {
					alternatives.add(delPos);
				}
			}
		}

		// new node calculated
		Stack<Node> newNodes = new Stack<>();
		List<Node> bestNodes = null;

		float currentBestMoyenne = pathMoyenne;
		if (oldInter.length < 2) {
			currentBestMoyenne = calcIncreaseMoyenne(pathMoyenne);
		}
		for (Position alt : alternatives) {
			if (oldInter.length < 2 && processPath.size() >= PATH_SIZE_EXPECTED && posDeleted.contains(alt)) {
				continue;
			}

			Node altNode = new Node(alt);
			newNodes.add(altNode);
			List<Node> currentDieNodes = new ArrayList<>(dieNodes);

			boolean isInRangeOfDies = inRangeOfAllDieNode(alt, currentDieNodes);

			// get second replacement node
			Collection<Position> altNext = getCommonAccessiblePosition(alt, next.position);

			if (oldInter.length == 1) {
				// allow inter position replace one by two because there is no function add intermediate node
				for (Position delPos : deletePosition) {
					if (delPos != alt && alt.isInRange(delPos) && next.isInRange(delPos)) {
						altNext.add(delPos);
					}
				}
			}
			for (Position pNext : altNext) {
				if (oldInter.length < 2 && processPath.size() >= PATH_SIZE_EXPECTED && posDeleted.contains(pNext) || allPathPosition.contains(pNext)) {
					continue;
				}

				List<Node> subCurrentDieNodes = new ArrayList<>(currentDieNodes);
				if (pNext != alt && (isInRangeOfDies || inRangeOfAllDieNode(pNext, subCurrentDieNodes))) {
					Node altNextNode = new Node(pNext);
					newNodes.add(altNextNode);
					float virtualCurrentMoy = calcNewMoyenneArray(deleteNodes, newNodes);
					if (virtualCurrentMoy > currentBestMoyenne) {
						bestNodes = new ArrayList<>(newNodes);
						currentBestMoyenne = virtualCurrentMoy;
					}
					newNodes.pop();
				}
			}
			newNodes.pop();
		}

		if (bestNodes != null) {
			changePath.setDeletedPathNodes(oldInter);
			changePath.setAddedPathNodes(bestNodes);
		}
		return changePath;
	}


	/**
	 * Check optimization for the inter nodes to be replace with three others.
	 * Limitation: don't allow to drop die node, the new nodes must be in range of all the die nodes.
	 *
	 * @param previous node before the nodes to optimize
	 * @param next node after the nodes to optimize
	 * @param dieNodes node that currently die in range of the middle nodes
	 * @param oldInter nodes to move or delete
	 * @return ChangePathOpti the optimization to do to the path
	 */
	private ChangePathOpti searchReplaceAnyByThree(Node previous, Node next, List<Node> dieNodes, Node... oldInter) {
		ChangePathOpti changePath = new ChangePathOpti("L" + (oldInter.length + 2) + "3", dieNodes);

		if (previous.position.isNotInRangeNMove(next.position, 4)) {
			return changePath;
		}

		List<Node> deleteNodes = new ArrayList<>();
		deleteNodes.addAll(Arrays.asList(oldInter));

		// get first replacement node
		Set<Position> alter1 = getAccessiblePositionNMove(previous.position, next.position, 3);

		// new node calculated
		Stack<Node> newNodes = new Stack<>();
		List<Node> bestNodes = null;

		float currentBestMoyenne = pathMoyenne;
		if (oldInter.length < 3) {
			currentBestMoyenne = calcIncreaseMoyenne(pathMoyenne);
		}
		for (Position alt1 : alter1) {
			if (oldInter.length < 3 && processPath.size() >= PATH_SIZE_EXPECTED && posDeleted.contains(alt1)) {
				continue;
			}

			Node alt1Node = new Node(alt1);
			newNodes.add(alt1Node);

			List<Node> currentDieNodes1 = new ArrayList<>(dieNodes);
			boolean isInRangeOfDies = inRangeOfAllDieNode(alt1, currentDieNodes1);

			// get second replacement node
			Set<Position> alter2 = getAccessiblePositionNMove(alt1, next.position, 2);

			for (Position alt2 : alter2) {
				if (alt1 == alt2 || alt1.distance(next.position) <= alt2.distance(next.position)
						|| oldInter.length < 3 && processPath.size() >= PATH_SIZE_EXPECTED && posDeleted.contains(alt2)) {
					continue;
				}

				Node alt2Node = new Node(alt2);
				newNodes.add(alt2Node);

				List<Node> currentDieNodes2 = isInRangeOfDies? currentDieNodes1 : new ArrayList<>(currentDieNodes1);
				boolean isInRangeOfDies2 = isInRangeOfDies || inRangeOfAllDieNode(alt2, currentDieNodes2);

				// get second replacement node
				Collection<Position> alter3 = getCommonAccessiblePosition(alt2, next.position);
				for (Position alt3 : alter3) {
					if (alt1 == alt3 || alt2 == alt3 || alt2.distance(next.position) <= alt3.distance(next.position) || allPathPosition.contains(alt3)
							|| oldInter.length < 3 && processPath.size() >= PATH_SIZE_EXPECTED && posDeleted.contains(alt2)) {
						continue;
					}
					Node alt3Node = new Node(alt3);
					newNodes.add(alt3Node);
					List<Node> currentDieNodes3 = isInRangeOfDies2? currentDieNodes2 : new ArrayList<>(currentDieNodes2);
					boolean isInRangeOfDies3 = isInRangeOfDies2 || inRangeOfAllDieNode(alt3, currentDieNodes3);
					if (isInRangeOfDies3) {
						float virtualCurrentMoy = calcNewMoyenneArray(deleteNodes, newNodes);
						if (virtualCurrentMoy > currentBestMoyenne) {
							bestNodes = new ArrayList<>(newNodes);
							currentBestMoyenne = virtualCurrentMoy;
						}
					}
					newNodes.pop();
				}
				newNodes.pop();
			}
			newNodes.pop();
		}

		if (bestNodes != null) {
			changePath.setDeletedPathNodes(oldInter);
			changePath.setAddedPathNodes(bestNodes);
		}
		return changePath;
	}

	/**
	 * Search good node to die. Take in count the fact that the current node is moved and the eventual die node delete with it.
	 *
	 * @param accessiblePosition all position in range
	 * @param currentMoyenne current average of the path
	 * @param dieNodeToAdd nodes to add, return object in parameter
	 * @param nodeMove new node after move
	 * @param dieNodeToDel die node deleted with the move
	 * @return new average after added die node
	 */
	private float searchForNodeToDieWithMove(Collection<Position> accessiblePosition, float currentMoyenne, List<Node> dieNodeToAdd, Node nodeMove, List<Node> dieNodeToDel) {
		for (Position p : accessiblePosition) {
			Node newNode = new Node(p);
			// add a die node if it's better than the average + the increase loop threshold
			if (newNode.getMinerai() > currentMoyenne + increaseLoopMinima
					&& (processPath.size() < PATH_SIZE_EXPECTED || ! posDeleted.contains(newNode.position))) {
				newNode.die = true;
				dieNodeToAdd.add(newNode);
			}
		}
		if (!dieNodeToAdd.isEmpty()) {
			if (nodeMove == null) {
				return calcNewMoyenneArray(dieNodeToDel, dieNodeToAdd);
			}
			else {
				dieNodeToAdd.add(nodeMove);
				float result = calcNewMoyenneArray(dieNodeToDel, dieNodeToAdd);
				dieNodeToAdd.remove(dieNodeToAdd.size()-1);
				return result;
			}
		}
		return 0f;
	}

	/**
	 * Calculate the average to beat taking into account of increase threshold.
	 *
	 * @param currentMoyenne current average
	 * @return average to beat
	 */
	private float calcIncreaseMoyenne(float currentMoyenne) {
		if (increaseLoopMinima == 0) {
			return currentMoyenne;
		}
		else {
			float totMinerai = totalMinerai + currentMoyenne + increaseLoopMinima;
			return totMinerai / (processPath.size() + 1);
		}
	}

	/**
	 * Calculate the average to beat taking into account of decrease threshold.
	 *
	 * @param currentMoyenne current average
	 * @return average to beat
	 */
	private float calcDecreaseMoyenne(float currentMoyenne, int nbNodeDel) {
		if (increaseLoopMinima == 0) {
			return currentMoyenne;
		}
		else {
			int nbDel = Math.abs(nbNodeDel);
			float totMinerai = totalMinerai - (currentMoyenne - decreaseLoopMinima) * nbDel;
			return totMinerai / (processPath.size() - nbDel);
		}
	}

	/**
	 * Calculate new average with the added and deleted node.
	 *
	 * @param nodeToDelete deleted nodes
	 * @param nodesToAdd added nodes
	 * @return new average
	 */
	private float calcNewMoyenne(Node nodeToDelete, Node... nodesToAdd) {
		List<Node> delNodes = new ArrayList<>();
		if (nodeToDelete != null) {
			delNodes.add(nodeToDelete);
		}
		return calcNewMoyenneArray(delNodes, Arrays.asList(nodesToAdd));
	}

	/**
	 * Calculate new average with the added and deleted node.
	 *
	 * @param nodeToDelete deleted nodes
	 * @param nodesToAdd added nodes
	 * @return new average
	 */
	private float calcNewMoyenneArray(Collection<Node> nodeToDelete, Collection<Node> nodesToAdd) {
		int newPathSize = processPath.size();
		int newTotalMinerai = totalMinerai;
		if (nodesToAdd != null) {
			for (Node nodeToAdd : nodesToAdd) {
				newPathSize++;
				newTotalMinerai += nodeToAdd.getMinerai();
			}
		}
		if (nodeToDelete != null) {
			for (Node nodeToDel : nodeToDelete) {
				newPathSize--;
				newTotalMinerai -= nodeToDel.getMinerai();
			}
		}
		return newTotalMinerai / (float) newPathSize;
	}

	/**
	 * Check if the position is in range of die node, and delete die node in range from the list.
	 *
	 * @param p the position
	 * @param dieNodes list of die nodes
	 * @return true if the position is in range (and therefore the list dieNodes become empty)
	 */
	private boolean inRangeOfAllDieNode(Position p, List<Node> dieNodes) {
		// to check if the two new case can matche all die node
		List<Node> okNode = new ArrayList<>();
		for (Node dieNode : dieNodes) {
			if (!dieNode.isNotInRange(p)) {
				okNode.add(dieNode);
			}
		}
		for (Node node : okNode) {
			dieNodes.remove(node);
		}

		return dieNodes.isEmpty();
	}

	/**
	 * Get all accessible position from the position p excluded the position in second parameter.
	 *
	 * @param p position
	 * @param exclusion positions excluded
	 * @return list of accessible position
	 */
	private Collection<Position> getAccessiblePosition(Position p, Collection<Position> exclusion) {
		Set<Position> result = new HashSet<>();
		for (int x = -DISTANCE ; x <= DISTANCE ; x++) {
			if (Math.abs(x) > VISION) { continue; }
			int remainingDistance = DISTANCE - Math.abs(x);
			for (int y = -remainingDistance ; y <= remainingDistance ; y++) {
				if (Math.abs(y) > VISION) { continue; }
				Position newPos = pos(p.x + x, p.y + y);
				if ((x != 0 || y != 0) && !allPathPosition.contains(newPos)
						&& (exclusion == null || !exclusion.contains(newPos))) {
					result.add(newPos);
				}
			}
		}
		return result;
	}

	/**
	 * Get position accessible from the source position in one move and to the target position in two moves.
	 * @param source initial position
	 * @param target position to go
	 * @param n number of move
	 *
	 * @return list of accessible position from the source position accessible to the target in two moves
	 */
	private Set<Position> getAccessiblePositionNMove(Position source, Position target, int n) {
		Set<Position> result = new HashSet<>();
		for (int x = -DISTANCE ; x <= DISTANCE ; x++) {
			if (Math.abs(x) > VISION) { continue; }
			int remainingDistance = ProcessUtil.DISTANCE - Math.abs(x);
			for (int y = -remainingDistance ; y <= remainingDistance ; y++) {
				if (Math.abs(y) > VISION) { continue; }
				Position newPos = pos(source.x + x, source.y + y);
				if (newPos.isNotInRange(source) || newPos.isNotInRangeNMove(target, n) || allPathPosition.contains(newPos)) {
					continue;
				}

				result.add(newPos);
			}
		}

		return result;
	}

	/***
	 * Get position accessible from both p1 and p2. Return a list if cache enable because it's way faster to iterate
	 *
	 * @param p1 position 1
	 * @param p2 position 2
	 * @return list of position accessible (Set if not cache, List if cache is enable)
	 */
	private Collection<Position> getCommonAccessiblePosition(Position p1, Position p2) {
		Map<Position, List<Position>> cache1 = null;
		if (CACHE) {
			cache1 = cache.get(p1);
			if (cache1 != null) {
				List<Position> cacheEntry = cache1.get(p2);
				if (cacheEntry != null) {
					return cacheEntry;
				}
			}
			else {
				cache1 = new HashMap<>();
				cache.put(p1, cache1);
			}
		}

		int minX = Math.min(p1.x, p2.x);
		int maxX = Math.max(p1.x, p2.x);
		int minY = Math.min(p1.y, p2.y);
		int maxY = Math.max(p1.y, p2.y);

		Set<Position> result = new HashSet<>();
		for (int x = maxX - VISION ; x <= minX + VISION ; x++) {
			for (int y = maxY - VISION ; y <= minY + VISION ; y++) {
				Position newPos = pos(x, y);
				if (newPos.distance(p1) > DISTANCE || newPos.distance(p2) > DISTANCE) {
					continue;
				}
				result.add(pos(x, y));
			}
		}

		if (CACHE) {
			List<Position> resultArray = new ArrayList<>(result);
			cache1.put(p2, resultArray);
			return resultArray;
		}

		return result;
	}

	/**
	 * Run the algorithm. Check the starting path is valid, and loop on the optimize function as long as it finds new optimization.
	 */
	public void process() {
		if (!ProcessUtil.checkPath(processPath)) {
			LogResult.out("ALGO ERROR");
			return;
		}
		ProcessUtil.resetCanDie(processPath);

		// loop on the threshold in parameter (only used one threshold for now. Trying to set multiple threshold did not seems to do anything, the last take over)
		for (int i=0 ; i<tabIncreaseLoopMinima.length ; i++) {
			increaseLoopMinima = tabIncreaseLoopMinima[i];
			decreaseLoopMinima = tabDecreaseLoopMinima[i];
			int nbMaxOpti = 5; // avoid infinite loop when increase and decrease threshold make the algorithm to do and undo modifications.
			while(nbMaxOpti-- >= 0 && optimize()) {
				if (nbMaxOpti < 3 && PATH_SIZE_EXPECTED == processPath.size()) {
					nbMaxOpti = -1;
				}
				// posDeleted and posAdded are here to limit infinite loop and stay at 200 exact positions if possible
				posDeleted.clear();
				posAdded.clear();
				LogResult.out("## New moyenne = " + pathMoyenne + ", total " + totalMinerai + ", nb case : " + processPath.size() + " ##");
			}
		}

		if (!ProcessUtil.checkPath(processPath)) {
			LogResult.out("ALGO ERROR");
			//return;
		}

		ProcessUtil.displayPath(processPath, true, true);

		if (checkDuration != null) {
			System.out.println("getAccessiblePositionDuration in ms : " + checkDuration);
		}
	}

	/** Set increase threshold */
	public void setTabIncreaseLoopMinima(int... tabIncreaseLoopMinima) {
		this.tabIncreaseLoopMinima = tabIncreaseLoopMinima;
	}

	/** Set decrease threshold */
	public void setTabDecreaseLoopMinima(int... tabDecreaseLoopMinima) {
		this.tabDecreaseLoopMinima = tabDecreaseLoopMinima;
	}

	public LinkedList<Node> getProcessPath() {
		return processPath;
	}
}
