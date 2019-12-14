package alpha.boucle;

import static alpha.boucle.process.ProcessAS.ECART_ALLOW;
import static alpha.boucle.process.ProcessAS.seuil;
import static alpha.boucle.process.ProcessUtil.pos;

import alpha.boucle.data.CoordinatesLoader;
import alpha.boucle.process.LogResult;
import alpha.boucle.process.ProcessAS;
import alpha.boucle.process.ProcessUtil;


/**
 * Launch a calcul A* on a path.
 * For each path, need to choose some seuil, init vision/distance access, and all the intermediate force coordinates.
 *
 * @author Escape
 */
@SuppressWarnings("unused")
public class AStarLauncher {


	@SuppressWarnings("preview")
	public static void main(String[] args) {

		int loop = 52;

		long time = System.currentTimeMillis();
		switch (loop) {
		case 5 -> boucleV5();
		case 52 -> boucleV5_first();
		case 6 -> boucleV6();
		}

		LogResult.writeFile("routeAS_V" + loop + ".txt");

	}


	private static void boucleV5() {
		CoordinatesLoader.loadCoordFromFile("routeV5.csv", false);

		seuil = new int[] { 100, 120, 140, 180, 190, 200, 210};
		ProcessUtil.VISION = 6;
		ProcessUtil.DISTANCE = 6;

		ECART_ALLOW = 3;
		ProcessAS p = new ProcessAS();
		p.init(pos(-310, -14), pos(-371, 1), pos(-306, -16));
		p.process();
		ECART_ALLOW = 2;
		p.continuePath(pos(-396, 51));
		ECART_ALLOW = 4;
		p.continuePath(pos(-483, 97));
		ECART_ALLOW = 3;
		//p.continuePath(pos(-460, 111));
		p.continuePath(pos(-465, 103));
		//p.continuePath(pos(-488, 121));
		p.continuePath(pos(-453, 127));
		ECART_ALLOW = 3;
		p.continuePath(pos(-440, 149), pos(-453, 132), pos(-451, 136), pos(-447, 135), pos(-455, 138), pos(-457, 140), pos(-458, 143));
		ECART_ALLOW = 2;
		p.continuePath(pos(-415, 136), pos(-438, 145), pos(-438, 141));
		p.continuePath(pos(-422, 114), pos(-415, 131), pos(-417, 131), pos(-422, 131), pos(-415, 129), pos(-415, 125), pos(-416, 120), pos(-420, 121));
		p.continuePath(pos(-419, 112), pos(-423, 110), pos(-424, 108), pos(-424, 103));
		p.continuePath(pos(-408, 127), pos(-415, 111), pos(-413, 107), pos(-412, 109), pos(-408, 109), pos(-409, 105), pos(-408, 100), pos(-404, 99), pos(-406, 101), pos(-406, 106), pos(-407, 108), pos(-410, 111), pos(-409, 114));
		//p.continuePath(pos(-394, 126), pos(-412, 109), pos(-410, 111), pos(-408, 109), pos(-409, 105), pos(-408, 100), pos(-404, 99), pos(-406, 101));
		//p.continuePath(pos(-394, 126), pos(-412, 109), pos(-408, 109), pos(-409, 105), pos(-408, 100), pos(-404, 99), pos(-406, 101), pos(-406, 106), pos(-407, 108), pos(-410, 111), pos(-409, 114), pos(-408, 119), pos(-409, 122));
		p.continuePath(pos(-293, 81), pos(-406, 130));
		//p.continuePath(pos(-318, 26));
		p.continuePath(pos(-311, 13));
		p.continuePath(pos(-310, -14), pos(-311, 8), pos(-311, 2), pos(-311, -3));

		p.finish(true);

	}

	private static void boucleV5_first() {
		CoordinatesLoader.loadCoordFromFile("routeV5_first.csv", false);

		seuil = new int[] { 50, 100, 100, 125, 140, 150, 170};
		ProcessUtil.VISION = 4;
		ProcessUtil.DISTANCE = 6;

		ECART_ALLOW = 6;
		ProcessAS p = new ProcessAS();
		p.init(pos(-310, -14), pos(-313, 5), pos(-306, -16), pos(-312, -10));
		p.process();
		p.continuePath(pos(-371, 1));
		p.continuePath(pos(-369, 19));
		p.continuePath(pos(-384, 32));
		p.continuePath(pos(-396, 51));
		p.continuePath(pos(-483, 97));
		//p.continuePath(pos(-460, 111));
		p.continuePath(pos(-463, 107));
		//ECART_ALLOW = 3;
		//p.continuePath(pos(-488, 121));
		//p.continuePath(pos(-453, 127));
		ECART_ALLOW = 3;
		//p.continuePath(pos(-440, 149), pos(-453, 130), pos(-453, 132), pos(-451, 136), pos(-447, 135), pos(-455, 138), pos(-457, 140), pos(-458, 143));
		p.continuePath(pos(-440, 149));
		p.continuePath(pos(-415, 136), pos(-438, 145), pos(-438, 141));
		p.continuePath(pos(-422, 114));
		p.continuePath(pos(-419, 112), pos(-423, 110), pos(-424, 108));
		p.continuePath(pos(-406, 130), pos(-415, 111), pos(-413, 107), pos(-412, 109), pos(-408, 109), pos(-409, 105));
		//p.continuePath(pos(-410, 111), pos(-415, 111), pos(-413, 107), pos(-412, 109), pos(-408, 109), pos(-409, 105));
		//p.continuePath(pos(-394, 126), pos(-412, 109), pos(-410, 111), pos(-408, 109), pos(-409, 105), pos(-408, 100), pos(-404, 99), pos(-406, 101));
		//p.continuePath(pos(-394, 126), pos(-412, 109), pos(-408, 109), pos(-409, 105), pos(-408, 100), pos(-404, 99), pos(-406, 101), pos(-406, 106), pos(-407, 108), pos(-410, 111), pos(-409, 114), pos(-408, 119), pos(-409, 122));
		ECART_ALLOW = 6;
		p.continuePath(pos(-294, 81), pos(-402, 130), pos(-398, 131), pos(-398, 128));
		p.continuePath(pos(-290, 57));
		p.continuePath(pos(-310, 48));
		//p.continuePath(pos(-300, 1));
		p.continuePath(pos(-310, -14));

		p.finish(true);
	}

	private static void boucleV6() {
		CoordinatesLoader.loadCoordFromFile("routeV6.csv", false);

		seuil = new int[] { 100, 120, 140, 180, 190, 200, 210};
		ProcessUtil.VISION = 7;
		ProcessUtil.DISTANCE = 7;

		ECART_ALLOW = 3;
		ProcessAS p = new ProcessAS();
		p.init(pos(-310, -14), pos(-371, 1), pos(-306, -16));
		p.process();
		ECART_ALLOW = 2;
		p.continuePath(pos(-396, 51));
		ECART_ALLOW = 4;
		p.continuePath(pos(-483, 97));
		ECART_ALLOW = 3;
		//p.continuePath(pos(-460, 111));
		p.continuePath(pos(-465, 103));
		//p.continuePath(pos(-488, 121));
		p.continuePath(pos(-453, 127));
		ECART_ALLOW = 3;
		p.continuePath(pos(-440, 149), pos(-453, 132), pos(-451, 136), pos(-447, 135), pos(-455, 138), pos(-457, 140), pos(-458, 143));
		ECART_ALLOW = 2;
		p.continuePath(pos(-414, 139), pos(-435, 147), pos(-431, 144));
		p.continuePath(pos(-422, 114), pos(-412, 134), pos(-415, 131), pos(-422, 131), pos(-415, 129), pos(-415, 129), pos(-414, 125), pos(-416, 120), pos(-422,119));
		p.continuePath(pos(-422, 119), pos(-423, 110), pos(-424, 108), pos(-424, 103));
		p.continuePath(pos(-406, 130), pos(-422, 114), pos(-421, 111), pos(-424, 108), pos(-424, 103), pos(-418, 102), pos(-414, 99));
		//p.continuePath(pos(-394, 126), pos(-412, 109), pos(-410, 111), pos(-408, 109), pos(-409, 105), pos(-408, 100), pos(-404, 99), pos(-406, 101));
		//p.continuePath(pos(-394, 126), pos(-412, 109), pos(-408, 109), pos(-409, 105), pos(-408, 100), pos(-404, 99), pos(-406, 101), pos(-406, 106), pos(-407, 108), pos(-410, 111), pos(-409, 114), pos(-408, 119), pos(-409, 122));
		p.continuePath(pos(-366, 103));
		p.continuePath(pos(-365, 112));
		p.continuePath(pos(-312, 14));
		p.continuePath(pos(-310, -14));

		p.finish(true);

	}
}
