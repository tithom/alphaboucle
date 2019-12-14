package alpha.boucle.process;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class LogResult {

	private static StringBuilder output = new StringBuilder();

	public static void out(String s) {
		output.append(s).append("\n");
		System.out.println(s);
	}

	public static void writeFile(String fileName) {
		Path path = Paths.get("output", fileName);
		try {
			Files.writeString(path, output.toString());
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
