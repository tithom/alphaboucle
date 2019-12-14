package alpha.boucle.data;

import static alpha.boucle.model.Position.getPosition;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import alpha.boucle.model.Node;
import alpha.boucle.model.Position;
import alpha.boucle.process.LogResult;

/**
 * Static class to load the coordinates known and one path
 *
 * @author Escape
 */
public final class CoordinatesLoader {

	public static short rangAllow = 0;
	public static final short ZERO = 0;

	private static Map<Position, Short> allPositions = new HashMap<>();
	private static Map<Position, Short> exisingRoute;

	private static final String separator1 = ",";
	private static final String separator2 = "\\s";

	/**
	 * Load all coord/minerai + one path
	 *
	 * @param guideRoute the extra path to load
	 * @param keepOrder to keep the extra path in order
	 */
	public static void loadCoordFromFile(String guideRoute, boolean keepOrder) {
		String fileName = "allknownlevel.csv"; // testroute.csv
		//String fileName = "testroute.csv";
		Path path = Paths.get("data", fileName);
		try {
			List<String> lines = Files.readAllLines(path);
			for (String line : lines) {
				if (line.isEmpty()) { continue; }
				String[] result = line.split(separator1);
				if (result.length != 3) {
					LogResult.out("Incorrect line " + line);
					continue;
				}
				Position p = getPosition(result);
				allPositions.put(p, Short.parseShort(result[2]));
			}
			LogResult.out("Coordinates loaded : " + allPositions.size());
		} catch (IOException e) {
			e.printStackTrace();
		}

		if (guideRoute != null) {
			if (keepOrder) {
				exisingRoute = new LinkedHashMap<>();
			}
			else {
				exisingRoute = new HashMap<>();
			}
			path = Paths.get("data", guideRoute);
			try {
				short i = 1;
				List<String> lines = Files.readAllLines(path);
				String separator = separator1;
				if (!lines.get(0).contains(separator)) {
					separator = separator2;
				}
				for (String line : lines) {
					String[] result = line.split(separator);
					if (result.length < 2) {
						LogResult.out("Incorrect line " + line);
						continue;
					}
					Position p = getPosition(result);
					exisingRoute.put(p, i++);
				}
				LogResult.out("Route loaded : " + exisingRoute.size());
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	/***
	 * For the A* algorithm.
	 * Forbid to use coordinate further than the parameter
	 *
	 * @param p next position to go, next are ignored
	 */
	public static void setNextPosition(Position p) {
		Short num = exisingRoute.get(p);
		if (num == null) {
			throw new IllegalArgumentException("Position " + p + " de destination de chemin hors route ");
		}
		rangAllow = (short) (num + 1);
		if (rangAllow == 2) {
			// first position = last
			rangAllow = (short) (exisingRoute.size() + 1);
		}
		LogResult.out("Rang Allow = " + rangAllow);
	}

	/**
	 * Get the number of minerai of a coordinate.
	 *
	 * @param p the coordinate
	 * @return the minerai of the coordinate, or 0 if the coordinate is further than the next position to go or not known
	 */
	public static short getCalcMinerai(Position p) {
		if (rangAllow != 0 && exisingRoute != null) {
			short pos = exisingRoute.getOrDefault(p, ZERO);
			if (pos > rangAllow) {
				return 0;
			}
		}
		return allPositions.getOrDefault(p, ZERO);
	}

	/**
	 * Get the number of minerai of a coordinate
	 *
	 * @param p the coordinate
	 * @return the minerai of the coordinate, or 0 if the coordinate is not known
	 */

	public static short getMinerai(Position p) {
		return allPositions.getOrDefault(p, ZERO);
	}

	/**
	 * Check if the coordinate is known
	 *
	 * @param p the coordinate
	 * @return true if the coordinate is known
	 */
	public static boolean checkNull(Position p) {
		return allPositions.get(p) == null;
	}

	/***
	 * @return the existing road (guide path)
	 */
	public static Set<Position> getExisingRoute() {
		return exisingRoute.keySet();
	}

	// test main
	public static void main(String[] args) {
		CoordinatesLoader.loadCoordFromFile(null, false);
		CoordinatesLoader.rangAllow = 5000;
		rangAllow = 1000;
		Position p = getPosition(-306, -16);
		Node n = new Node(p);
		System.out.println(n.setMinerai());
	}

}
