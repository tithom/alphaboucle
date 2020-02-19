package alpha.boucle;

import static alpha.boucle.process.ProcessUtil.DISTANCE;
import static alpha.boucle.process.ProcessUtil.VISION;

import alpha.boucle.process.LogResult;
import alpha.boucle.process.ProcessOpti;

/**
 * Launch a optimizer algorithm on a path.
 *
 * @author Escape
 */
public class OptiLauncher {

	@SuppressWarnings("preview")
	public static void main(String[] args) {
		int loop = 6;

		long time = System.currentTimeMillis();
		switch (loop) {
		case 4 -> boucleV4(false);
		case 5 -> boucleV5(false);
		case 52 -> boucleV5(true);
		case 6 -> boucleV6(false);
		case 7 -> boucleV7(false);
		}
		LogResult.writeFile("routeOpti_V" + loop + ".txt");
		System.out.println("Loop " + loop + " calculated in " + (System.currentTimeMillis() - time)/1000f + " seconds");
	}

	private static void boucleV4(boolean first) {
		DISTANCE = 5;
		VISION = 5;
		ProcessOpti opti = null;
		if (first) {
			VISION = 4;
			opti = new ProcessOpti("routeV4_test.csv");
			opti.setTabIncreaseLoopMinima(28);
			System.out.println("Not implemented");
			return;
		}
		else {
			opti = new ProcessOpti("routeV4_test.csv");
			opti.setTabIncreaseLoopMinima(15);
			opti.setThresholdOtherDieNode(180);
		}

		opti.process();
	}

	private static void boucleV6(boolean first) {
		DISTANCE = 7;
		VISION = 7;
		ProcessOpti opti = null;
		if (first) {
			VISION = 4;
			opti = new ProcessOpti("routeV6_first.csv");
			// TODO dont exist
		}
		else {
			opti = new ProcessOpti("routeV6_opti.csv");
			opti.setTabDecreaseLoopMinima(11);
			opti.setTabIncreaseLoopMinima(-10);
			opti.setThresholdOtherDieNode(200);
		}

		opti.process();
	}

	private static void boucleV7(boolean first) {
		DISTANCE = 8;
		VISION = 8;
		ProcessOpti opti = null;
		if (first) {
			VISION = 5;
			opti = new ProcessOpti("routeV7_first.csv");
		}
		else {
			opti = new ProcessOpti("routeV7_opti.csv");
			opti.setTabDecreaseLoopMinima(31);
			opti.setTabIncreaseLoopMinima(-30);
		}

		opti.process();
	}

	private static void boucleV5(boolean first) {
		DISTANCE = 6;
		VISION = 6;
		ProcessOpti opti = null;
		if (first) {
			VISION = 4;
			opti = new ProcessOpti("routeV5_first.csv");
			opti.setTabIncreaseLoopMinima(0);
			opti.setThresholdOtherDieNode(180);
		}
		else {
			opti = new ProcessOpti("routeV5_opti.csv");
			opti.setTabIncreaseLoopMinima(0);
		}

		opti.process();
	}
}
