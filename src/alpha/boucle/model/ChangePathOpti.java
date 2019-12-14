package alpha.boucle.model;

import static alpha.boucle.model.Node.toStringArrayShort;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

import alpha.boucle.process.LogResult;
import alpha.boucle.process.ProcessOpti;
import alpha.boucle.process.ProcessUtil;

/**
 * Manage the modification of a path by setting all changed node.
 * Also contains the function to apply the modification to the path.
 *
 * @author Escape
 */
public class ChangePathOpti {

	private static final boolean DEBUG = true;

	private List<Node> currentDieNodes = null;

	private List<Node> deletedPathNodes = new ArrayList<>();
	private List<Node> deletedDieNodes = new ArrayList<>();

	private List<Node> addedPathNodes = new ArrayList<>();
	private List<Node> addedDieNodes = new ArrayList<>();

	private String libelle;
	boolean change = false;

	public ChangePathOpti(String libelle, List<Node> currentDieNodes) {
		this.libelle = libelle;
		this.currentDieNodes = currentDieNodes;
	}

	/**
	 * Apply the modification to the process path
	 *
	 * @param po process optimizer
	 */
	public void apply(ProcessOpti po, Node previous, Node next) {
		LinkedList<Node> processPath = po.getProcessPath();

		/// log change
		List<Position> deletePosition = deletedPathNodes.stream().map(a -> a.position).collect(Collectors.toList());
		List<Position> addedPosition = addedPathNodes.stream().map(a -> a.position).collect(Collectors.toList());
		List<Position> commonPosition = new ArrayList<>(addedPosition);
		commonPosition.retainAll(deletePosition);

		if (commonPosition.size() != deletePosition.size() || commonPosition.size() != addedPosition.size()) {
			List<Node> trueAddedNode = addedPathNodes.stream().filter(a -> !commonPosition.contains(a.position)).collect(Collectors.toList());
			List<Node> trueDeletedNode = deletedPathNodes.stream().filter(a -> !commonPosition.contains(a.position)).collect(Collectors.toList());
			if (trueAddedNode.isEmpty()) {
				LogResult.out(libelle + " " + toStringArrayShort(trueDeletedNode) + " deleted || prev="
						+ previous.toStringShort() + ", next=" + next.toStringShort() + ", die=" + toStringArrayShort(currentDieNodes));

			}
			else {
				LogResult.out(libelle + " " + toStringArrayShort(trueDeletedNode) + " replace by " + toStringArrayShort(trueAddedNode) +
						" || prev=" + previous.toStringShort() + ", next=" + next.toStringShort() + ", die=" + toStringArrayShort(currentDieNodes));
			}
		}
		if (!deletedDieNodes.isEmpty()) {
			LogResult.out(libelle + " Delete die node " + toStringArrayShort(deletedDieNodes) + " with " + toStringArrayShort(deletedPathNodes)
			+ " || prev=" + previous.toStringShort() + ", next=" + next.toStringShort());
		}
		if (!addedDieNodes.isEmpty()) {
			LogResult.out(libelle + " Added die node " + toStringArrayShort(addedDieNodes) + " with " + toStringArrayShort(addedPathNodes)
			+ " || prev=" + previous.toStringShort() + ", next=" + next.toStringShort());
		}
		/// end log change

		for (Node cNode : currentDieNodes) {
			boolean found = false;
			for (Node dNode : deletedDieNodes) {
				if (dNode.position == cNode.position) {
					found = true;
					break;
				}
			}
			if (!found) {
				// if die node is not delete, we delete and recreate it
				deletedDieNodes.add(cNode);
				addedDieNodes.add(cNode);
			}
		}

		int posInit = processPath.indexOf(deletedPathNodes.get(0));
		for (Node node : deletedPathNodes) {
			po.removeNode(node);
		}
		for (Node dNode : deletedDieNodes) {
			po.removeNode(dNode);
		}
		for (Node node : addedPathNodes) {
			po.addNode(node, posInit++);
			if (!addedDieNodes.isEmpty()) {
				List<Node> dieNodeAdd = new ArrayList<>();
				for (Node dNode : addedDieNodes) {
					if (dNode.isInRange(node)) {
						po.addNode(dNode, posInit++);
						dieNodeAdd.add(dNode);
					}
				}
				addedDieNodes.removeAll(dieNodeAdd);
			}
		}
		if (!addedDieNodes.isEmpty()) {
			ProcessUtil.displayPath(processPath, true, false);
			throw new IllegalArgumentException("Unreachable die node " + addedDieNodes + " with added nodes " + addedPathNodes);
		}
		if (DEBUG) {
			if (! ProcessUtil.checkPath(processPath)) {
				ProcessUtil.displayPath(processPath, true, false);
				throw new IllegalArgumentException("BUG");
			}
			ProcessUtil.resetCanDie(processPath);
		}
	}

	// getter

	public List<Node> getDeletedPathNodes() {
		return deletedPathNodes;
	}
	public List<Node> getDeletedDieNodes() {
		return deletedDieNodes;
	}
	public List<Node> getAddedPathNodes() {
		return addedPathNodes;
	}
	public List<Node> getAddedDieNodes() {
		return addedDieNodes;
	}

	public boolean isChange() {
		return change;
	}

	// specific set - if node unchanged, add them in the delete and added node
	public void setUnchangedNodes(Node... unchanged) {
		this.deletedPathNodes = Arrays.asList(unchanged);
		this.addedPathNodes = Arrays.asList(unchanged);
	}

	public void setUnchangedDieNodes(Node... unchanged) {
		this.deletedDieNodes = Arrays.asList(unchanged);
		this.addedDieNodes = Arrays.asList(unchanged);
	}

	// setter with list

	public void setDeletedPathNodes(List<Node> deletedPathNodes) {
		this.deletedPathNodes = deletedPathNodes;
		change = true;
	}
	public void setDeletedDieNodes(List<Node> deletedDieNodes) {
		this.deletedDieNodes = deletedDieNodes;
		change = true;
	}
	public void setAddedPathNodes(List<Node> addedPathNodes) {
		this.addedPathNodes = addedPathNodes;
		change = true;
	}
	public void setAddedDieNodes(List<Node> addedDieNodes) {
		this.addedDieNodes = addedDieNodes;
		change = true;
	}

	// setter with array

	public void setDeletedPathNodes(Node... deletedPathNodes) {
		this.deletedPathNodes = Arrays.asList(deletedPathNodes);
		change = true;
	}
	public void setDeletedDieNodes(Node... deletedDieNodes) {
		this.deletedDieNodes = Arrays.asList(deletedDieNodes);
		change = true;
	}
	public void setAddedPathNodes(Node... addedPathNodes) {
		this.addedPathNodes = Arrays.asList(addedPathNodes);
		change = true;
	}
	public void setAddedDieNodes(Node... addedDieNodes) {
		this.addedDieNodes = Arrays.asList(addedDieNodes);
		change = true;
	}
}
