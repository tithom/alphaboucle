package alpha.boucle.model;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import alpha.boucle.data.CoordinatesLoader;

/**
 * A Node (position / coordinates) extend to more information
 *
 * @author Escape
 */
public class Node {

	/** For facultative dying node */
	List<Node> childs = new ArrayList<>();

	/** coordinates */
	public Position position;

	/** if we need to die on the coordinate */
	public boolean die = false;

	/** if we can die on the coordinate (calculated by ProcessUtil.checkPath) */
	private Boolean canDie = null;

	/** the minerai */
	short minerai = 0;

	/**
	 * Constructor. Call class Coordinates to get the minerai on the position
	 *
	 * @param p the coordinate
	 */
	public Node(Position p) {
		this.position = p;
		setMinerai();
	}

	public Node(Position p, short minerai) {
		this.position = p;
		this.minerai = minerai;
	}

	public Node(Position p, short minerai, boolean die) {
		this.position = p;
		this.minerai = minerai;
		this.die = die;
	}

	public short getMinerai() {
		return minerai;
	}

	public short setMinerai() {
		minerai = CoordinatesLoader.getCalcMinerai(position);
		return minerai;
	}

	/**
	 * To change the node with another but keep the current object
	 *
	 * @param n node to copy
	 */
	public void replaceByOther(Node n) {
		// to change the node and keep the reference
		this.position = n.position;
		this.minerai = n.minerai;
		this.die = n.die;
	}

	/**
	 * If this.canDie=false, do nothing. Otherwise, set canDie
	 *
	 * @param canDie value to set
	 */
	public void updateCanDie(boolean canDie) {
		// once false is set, could not override it
		if (this.canDie != Boolean.FALSE) {
			this.canDie = canDie;
		}
	}

	/**
	 * Reset canDie to null to allow to set another value
	 */
	public void resetCanDie() {
		this.canDie = null;
	}

	public boolean getCanDie() {
		if (canDie == null) {
			return false;
		}
		return canDie.booleanValue();
	}

	@Override
	public String toString() {
		return new StringBuilder("[").append(position).append(" = ").append(minerai)
				.append(" - ").append(die).append("]").toString();
	}

	public String toStringShort() {
		StringBuilder sb = new StringBuilder("[").append(position).append(" = ").append(minerai);
		if (!childs.isEmpty()) {
			sb.append(" ").append(childs);
		}
		return sb.append("]").toString();
	}

	public static String toStringTabShort(Node... nodes) {
		return toStringArrayShort(Arrays.asList(nodes));
	}

	public static String toStringArrayShort(List<Node> nodes) {
		if (nodes.isEmpty()) {
			return "[]";
		}
		else {
			return String.join(" + ", nodes.stream().map(a -> a.toStringShort()).collect(Collectors.toList()));
		}
	}

	public static float getMoyenne(Node... nodes) {
		int someMinerai = 0;
		for (Node node : nodes) {
			someMinerai += node.minerai;
		}
		return someMinerai / (float) nodes.length;
	}

	/** Tabulation separator to copy to Excel */
	public String toStringCopy() {
		String dieStr = null;
		if (die) {
			dieStr="O";
		}
		else if (getCanDie()) {
			dieStr="?";
		}
		else {
			dieStr = "N";
		}
		StringBuilder sb = new StringBuilder().append(position.toStringCopy()).append("\t\t").append(minerai)
				.append("\t").append(dieStr);
		if (!childs.isEmpty()) {
			sb.append("\t");
			for (Node node : childs) {
				sb.append(node.position).append("=").append(node.minerai).append(" | ");
			}
			sb.setLength( sb.length() - 3 ); // remove last useless space
		}
		return sb.toString();
	}

	public String toCoordString() {
		return position.toString();
	}

	public int distance(Position p) {
		return position.distance(p);
	}

	public int distance(Node n) {
		return position.distance(n.position);
	}

	public boolean isNotInRange(Position p) {
		return position.isNotInRange(p);
	}

	public boolean isNotInRange(Node n) {
		return n != null && position.isNotInRange(n.position);
	}

	public boolean isInRange(Node n) {
		return !isNotInRange(n);
	}

	public boolean isInRange(Position p) {
		return p.isInRange(position);
	}

	public int getNbChild() {
		return childs.size();
	}

	public void addChild(Node n) {
		childs.add(n);
	}

	@Override
	public Node clone() {
		Node n = new Node(position, minerai);
		n.die = die;
		return n;
	}

}
