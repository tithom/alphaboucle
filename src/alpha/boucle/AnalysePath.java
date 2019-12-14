package alpha.boucle;

import java.util.LinkedList;
import java.util.Set;

import alpha.boucle.data.CoordinatesLoader;
import alpha.boucle.model.NodeAvg;
import alpha.boucle.model.Position;
import alpha.boucle.process.ProcessUtil;

/**
 * Analyse a path by calculating the 3-closed, 6-closed and 9-closed average of each position.
 *
 * @author Escape
 */
public class AnalysePath {

	LinkedList<NodeAvg> processPath = new LinkedList<>();
	int totalMinerai = 0;
	int totalNull = 0;

	public AnalysePath(String fileBoucle) {
		init(fileBoucle);
	}

	/**
	 * Initialize the path to analyse.
	 *
	 * @param fileBoucle file of the path
	 */
	private void init(String fileBoucle) {
		CoordinatesLoader.loadCoordFromFile(fileBoucle, true);
		Set<Position> posRoute = CoordinatesLoader.getExisingRoute();
		for (Position position : posRoute) {
			short minerai = CoordinatesLoader.getMinerai(position);
			if (minerai == 0 && CoordinatesLoader.checkNull(position)) {
				totalNull++;
			}
			processPath.add(new NodeAvg(position, minerai, false));
			totalMinerai += minerai;
		}
	}

	/**
	 * Calculate for each Node the 3/6/9 closed node average.
	 */
	private void calculTotal() {
		int lengthPath = processPath.size();
		int num = 0;
		for (NodeAvg nodeAvg : processPath) {
			short total3 = 0;
			short total6 = 0;
			short total9 = 0;
			for(int i=-9 ; i <= 9 ; i++) {
				int absI = Math.abs(i);
				short minCase = processPath.get((num + i + lengthPath)%lengthPath).getMinerai();
				total9 += minCase;
				if (absI <= 6) {
					total6 += minCase;
					if (absI <= 3) {
						total3 += minCase;
					}
				}
			}
			nodeAvg.setTotal(total3, total6, total9);
			num++;
		}
	}

	public static void main(String[] args) {
		CoordinatesLoader.rangAllow = 5000;
		AnalysePath analyse = new AnalysePath("routeV5_opti.csv");
		analyse.calculTotal();

		//ProcessUtil.checkPath(analyse.processPath);
		// don't set die position to check path is useless

		ProcessUtil.displayPath(analyse.processPath, true, true);
	}
}
