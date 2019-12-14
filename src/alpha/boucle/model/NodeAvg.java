package alpha.boucle.model;

/**
 * Node with average data of the n+/-3, n+/-6 and n+/-0 node.
 * Used in AnalaysePath, to check where are the bad section.
 *
 * @author Escape
 */
public class NodeAvg extends Node {

	private short tot3 = 0;
	private short tot6 = 0;
	private short tot9 = 0;

	public NodeAvg(Position p, short minerai, boolean die) {
		super(p, minerai);
		this.die = die;
	}

	public void setTotal(short tot3, short tot6, short tot9) {
		this.tot3 = tot3;
		this.tot6 = tot6;
		this.tot9 = tot9;
	}

	public short getTot3() {
		return tot3;
	}

	public short getTot6() {
		return tot6;
	}

	public short getTot9() {
		return tot9;
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder(super.toString());
		sb.append(" - ").append(tot3).append("/").append(tot6).append("/").append(tot9);
		return sb.toString();
	}

	@Override
	public String toStringCopy() {
		String moy3 = String.format("%,.2f", tot3 / 7f);
		String moy6 = String.format("%,.2f", tot6 / 13f);
		String moy9 = String.format("%,.2f", tot9 / 19f);
		StringBuilder sb = new StringBuilder(super.toStringCopy());
		sb.append("\t").append(moy3).append("\t").append(moy6).append("\t").append(moy9);
		return sb.toString();
	}

}
