package alpha.boucle.process;

import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import alpha.boucle.data.CoordinatesLoader;
import alpha.boucle.model.Node;
import alpha.boucle.model.Position;

/**
 * Share variable and function of the algorithm.
 *
 * @author Escape
 */
public class ProcessUtil {

	public static short VISION = 4;
	public static short DISTANCE = 6;


	/** to shorter the code to access a position. */
	public static Position pos(int x, int y) {
		return Position.getPosition(x, y);
	}

	/**
	 * Display a path.
	 *
	 * @param path the path
	 * @param withPath true to display all the path. False to show the statistics only.
	 * @param toExcel false to have a pretty display. True to be ready to copy to Excel.
	 */
	public static void displayPath(List<? extends Node> path, boolean withPath, boolean toExcel) {
		int sizePath = path.size();
		Position first = path.get(0).position;
		Position last = path.get(sizePath-1).position;

		StringBuilder pathStr = new StringBuilder();
		int totalMinerai = 0;
		int totalNull = 0;
		int nbDiePos = 0;
		int nbCanDiePos = 0;
		for (Node n : path) {
			pathStr.append(toExcel? n.toStringCopy() : n).append("\n");
			totalMinerai += n.getMinerai();
			if (n.getMinerai() == 0 && CoordinatesLoader.checkNull(n.position)) {
				totalNull++;
			}
			if (n.die) {
				nbDiePos++;
			}
			else if (n.getCanDie()) {
				nbCanDiePos++;
			}
		}
		String moyenneTot = String.format("%,.2f", totalMinerai / (float) sizePath);

		String resume = "Trajet " + first + " vers " + last +
				" - Nb Case : " + sizePath + " (dont " + totalNull + " null) - Total : " + totalMinerai +
				", Moyenne : " + moyenneTot + " - Nb Die : " + nbDiePos + " (can die : " + nbCanDiePos + ")";
		LogResult.out(resume);
		if (withPath) {
			LogResult.out(pathStr.toString());
			LogResult.out(resume);
		}
	}

	/**
	 * Check if a Path is valid.
	 * Also calculated the "can die" position.
	 *
	 * @param path the path.
	 * @return True is the path is valid.
	 */
	public static boolean checkPath(Collection<? extends Node> path) {
		boolean result = true;
		Node first = null;
		Node previous = null;
		Node pPrevious = null;
		Node realpPrevious = null;
		int num = 0;
		Set<Node> checkDoublon = new HashSet<>();
		for (Node node : path) {
			num++;
			if (previous == null) {
				first = node;
				previous = node;
				continue;
			}
			if (!checkDoublon.add(node)) {
				result = false;
				LogResult.out("CheckPath KO - " + node.toStringShort() + " already exist " + num);
			}
			if (node.isNotInRange(previous)) {
				result = false;
				LogResult.out("CheckPath KO - " + previous.toStringShort() + " -> " + node + " - " + num);
			}
			if (node.position == pos(-362,34)) {
				int t=0;
				t = t+1;
			}

			// update can die

			if (node.isNotInRange(pPrevious) || node.isNotInRange(realpPrevious)) {
				previous.updateCanDie(false);
			}

			if (!node.die) {
				if (pPrevious == null)  {
					pPrevious = previous;
				}
				else {
					if (node.isInRange(pPrevious)) {
						previous.updateCanDie(true);
					}
					else {
						previous.updateCanDie(false);
						pPrevious = previous;
					}
					realpPrevious = previous;
				}
				previous = node;
			}
		}
		// check if the loop is closed
		if (first.isNotInRange(previous)) {
			result = false;
			LogResult.out("CheckPath KO - " + previous.toStringShort() + " -> " + first + " - " + num);
		}
		return result;
	}

	/**
	 * Reset the can die flag before modifying the path.
	 */
	public static void resetCanDie(LinkedList<Node> processPath) {
		for (Node node : processPath) {
			node.resetCanDie();
		}
	}
}
