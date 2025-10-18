package com.amazon.agenticworkstation.action;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;

import static com.amazon.agenticworkstation.action.ActionGeneratorConstants.*;

/**
 * Manages data file preparation and loading for Python execution
 */
public final class DataFileManager {
	private static final Logger log = LoggerFactory.getLogger(DataFileManager.class);
	private static final ObjectMapper objectMapper = new ObjectMapper();

	private DataFileManager() {
		// Private constructor to prevent instantiation
	}

	/**
	 * Resolve the temp directory to use for consolidated data files.
	 * Order of precedence:
	 *  1. System property 'agent.data.tmpdir'
	 *  2. Environment variable 'AGENT_DATA_TMP_DIR'
	 *  3. JVM default 'java.io.tmpdir'
	 * Ensures the directory exists and is writable where possible.
	 */
	private static Path getConfiguredTempDirectory() {
		try {
			String configured = System.getProperty("agent.data.tmpdir");
			if (configured == null || configured.isEmpty()) {
				configured = System.getenv("AGENT_DATA_TMP_DIR");
			}

			Path dir;
			if (configured != null && !configured.isEmpty()) {
				dir = Paths.get(configured);
				if (!Files.exists(dir)) {
					try {
						Files.createDirectories(dir);
					} catch (Exception e) {
						log.warn("Could not create configured temp dir '{}', falling back to JVM temp dir: {}", configured, e.getMessage());
						dir = Paths.get(System.getProperty("java.io.tmpdir"));
					}
				}
			} else {
				dir = Paths.get(System.getProperty("java.io.tmpdir"));
			}

			return dir;
		} catch (Exception e) {
			log.warn("Failed to resolve configured temp directory, using JVM tempdir: {}", e.getMessage());
			return Paths.get(System.getProperty("java.io.tmpdir"));
		}
	}

	/**
	 * Prepare data file for Python execution by loading all data from the
	 * environment's data directory.
	 * 
	 * @param envName Environment name (e.g., "hr_experts")
	 * @return Path to the consolidated data file to use
	 * @throws IOException if data cannot be loaded
	 */
	public static String prepareDataFile(String envName) throws IOException {
		Path envDataDir = getEnvironmentDataDirectory(envName);

		if (envDataDir == null || !Files.exists(envDataDir)) {
			throw new IOException("Environment data directory not found for environment: " + envName
					+ ". Expected path: " + TAU_BENCH_DIR + "/" + ENVS_DIR + "/" + envName + "/" + DATA_DIR);
		}

		log.info("Loading environment data from: {}", envDataDir);
		return loadEnvironmentData(envDataDir, envName);
	}

	/**
	 * Get the environment data directory path
	 * 
	 * @param envName Environment name (e.g., "hr_experts")
	 * @return Path to environment data directory, or null if not found
	 */
	public static Path getEnvironmentDataDirectory(String envName) {
		try {
			// Try to find tau bench directory
			String currentDir = System.getProperty(USER_DIR_PROPERTY);
			Path workspaceRoot = Paths.get(currentDir);

			// If we're in backend directory, go up one level
			if (workspaceRoot.endsWith(BACKEND_DIR)) {
				workspaceRoot = workspaceRoot.getParent();
			}

			// Try multiple possible locations for the data directory
			// Location 1: Inside agenticWorkstation/amazon-tau-bench-tasks-main
			Path path1 = workspaceRoot.resolve(TAU_BENCH_DIR).resolve(ENVS_DIR).resolve(envName)
					.resolve(DATA_DIR);
			
			// Location 2: Sibling to agenticWorkstation (go up one more level)
			Path path2 = workspaceRoot.getParent().resolve(TAU_BENCH_DIR).resolve(ENVS_DIR).resolve(envName)
					.resolve(DATA_DIR);

			if (Files.exists(path1) && Files.isDirectory(path1)) {
				log.debug("Found environment data directory at location 1: {}", path1);
				return path1;
			} else if (Files.exists(path2) && Files.isDirectory(path2)) {
				log.debug("Found environment data directory at location 2: {}", path2);
				return path2;
			}

			log.warn("Environment data directory not found. Tried:\n  1. {}\n  2. {}", path1, path2);
			return null;
		} catch (Exception e) {
			log.warn("Failed to locate environment data directory for {}: {}", envName, e.getMessage());
			return null;
		}
	}

	/**
	 * Load all JSON files from environment data directory and create a consolidated
	 * data file
	 * 
	 * @param envDataDir Environment data directory
	 * @param envName    Environment name for temp file naming
	 * @return Path to temporary consolidated data file
	 * @throws IOException if data loading fails
	 */
	private static String loadEnvironmentData(Path envDataDir, String envName) throws IOException {
		Map<String, Object> consolidatedData = new HashMap<>();

		// Read all JSON files in the data directory
		Files.list(envDataDir).filter(path -> path.toString().endsWith(JSON_EXTENSION)).forEach(jsonFile -> {
			try {
				String key = jsonFile.getFileName().toString().replace(JSON_EXTENSION, EMPTY_STRING);
				Object data = objectMapper.readValue(jsonFile.toFile(), Object.class);
				consolidatedData.put(key, data);
				log.debug("Loaded {} data from {}", key, jsonFile.getFileName());
			} catch (Exception e) {
				log.warn("Failed to load data file {}: {}", jsonFile, e.getMessage());
			}
		});

		log.info("Loaded {} data files for environment '{}'", consolidatedData.size(), envName);

		// Create temporary file with consolidated data in configured temp directory
		Path tempDir = getConfiguredTempDirectory();
		Path tempDataFile;
		try {
			tempDataFile = Files.createTempFile(tempDir, TEMP_FILE_PREFIX + envName + UNDERSCORE, TEMP_FILE_SUFFIX);
		} catch (IOException e) {
			// Fallback: try default JVM temp dir
			Path jvmTemp = Paths.get(System.getProperty("java.io.tmpdir"));
			tempDataFile = Files.createTempFile(jvmTemp, TEMP_FILE_PREFIX + envName + UNDERSCORE, TEMP_FILE_SUFFIX);
		}

		objectMapper.writerWithDefaultPrettyPrinter().writeValue(tempDataFile.toFile(), consolidatedData);

		log.debug("Created consolidated data file: {}", tempDataFile);

		// Register shutdown hook to clean up temp file
		tempDataFile.toFile().deleteOnExit();

		return tempDataFile.toString();
	}
}
