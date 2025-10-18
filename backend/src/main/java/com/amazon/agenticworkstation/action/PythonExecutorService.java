package com.amazon.agenticworkstation.action;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.amazon.agenticworkstation.action.scenario.models.PythonFunctionMetadata;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Service for executing Python functions and extracting metadata
 */
public final class PythonExecutorService {
	private static final Logger log = LoggerFactory.getLogger(PythonExecutorService.class);
	private static final String TAU_BENCH_TASKS_DIR = "amazon-tau-bench-tasks-main";
	private static final String PYTHON_TOOLS_BASE_DIR = TAU_BENCH_TASKS_DIR + "/envs";
	private static final String PYTHON_TOOLS_SUBDIR = "tools";
	private static final String INTERFACE_PREFIX = "interface_";
	private static final String MOCK_TAU_BENCH_PATH = TAU_BENCH_TASKS_DIR + "/scripts";
	private static final ObjectMapper objectMapper = new ObjectMapper();
	private static final long PYTHON_EXECUTION_TIMEOUT_SECONDS = 30L;
	private static final String PYTHON_FILE_EXTENSION = ".py";
	private static final String TEMP_SCRIPT_PREFIX = "exec_";
	private static final String PYTHON_COMMAND = "python";

	private PythonExecutorService() {
		// Private constructor to prevent instantiation
	}

	/**
	 * Get the base path for Python tools
	 * 
	 * @param envName         The environment name (e.g., "hr_experts")
	 * @param interfaceNumber The interface number (e.g., 1 for "interface_1")
	 */
	private static Path getToolsBasePath(String envName, int interfaceNumber) {
		String currentDir = System.getProperty("user.dir");
		Path backendPath = Paths.get(currentDir);

		// If we're in the backend directory, go up one level
		if (backendPath.endsWith("backend")) {
			backendPath = backendPath.getParent();
		}

		// Build path:
		// amazon-tau-bench-tasks-main/envs/{envName}/tools/interface_{interfaceNumber}
		String interfaceDir = INTERFACE_PREFIX + interfaceNumber;
		return backendPath.resolve(PYTHON_TOOLS_BASE_DIR).resolve(envName).resolve(PYTHON_TOOLS_SUBDIR)
				.resolve(interfaceDir);
	}

	/**
	 * Extract function metadata from Python file by calling get_info()
	 * 
	 * @param actionName      The name of the action/function
	 * @param envName         The environment name (e.g., "hr_experts")
	 * @param interfaceNumber The interface number (e.g., 1 for "interface_1")
	 */
	public static PythonFunctionMetadata extractMetadata(String actionName, String envName, int interfaceNumber)
			throws IOException {
		log.info("Extracting metadata for action: {} in env: {} interface: {}", actionName, envName, interfaceNumber);

		String pythonFileName = actionName + PYTHON_FILE_EXTENSION;
		Path pythonFilePath = getToolsBasePath(envName, interfaceNumber).resolve(pythonFileName);

		if (!Files.exists(pythonFilePath)) {
			log.error("Python file not found: {}", pythonFilePath);
			throw new IOException("Python file not found: " + pythonFilePath);
		}

		Path mockTauBenchPath = getMockTauBenchPath();
		String extractScript = String.format(
				"import sys\n" + "import json\n" + "# Load mock tau_bench before any other imports\n"
						+ "sys.path.insert(0, '%s')\n" + "import mock_tau_bench\n" + "sys.path.insert(0, '%s')\n"
						+ "from %s import *\n" + "info = %s.get_info()\n" + "print(json.dumps(info))\n",
				mockTauBenchPath.toString().replace("\\", "\\\\"),
				pythonFilePath.getParent().toString().replace("\\", "\\\\"), actionName, toPascalCase(actionName));

		String jsonOutput = executePythonScript(extractScript);
		return parseMetadata(jsonOutput, actionName);
	}

	/**
	 * Get the path to tau_bench mock module
	 */
	private static Path getMockTauBenchPath() {
		String currentDir = System.getProperty("user.dir");
		Path backendPath = Paths.get(currentDir);

		// If we're in the backend directory, go up one level
		if (backendPath.endsWith("backend")) {
			backendPath = backendPath.getParent();
		}

		return backendPath.resolve(MOCK_TAU_BENCH_PATH);
	}

	/**
	 * Execute a Python function with given arguments
	 * 
	 * @param actionName      The name of the action/function
	 * @param arguments       The arguments to pass to the function
	 * @param dataFilePath    The path to the data file
	 * @param envName         The environment name (e.g., "hr_experts")
	 * @param interfaceNumber The interface number (e.g., 1 for "interface_1")
	 */
	public static String executePythonFunction(String actionName, Map<String, Object> arguments, String dataFilePath,
			String envName, int interfaceNumber) throws IOException {
		log.info("Executing Python function: {} with arguments: {} in env: {} interface: {}", actionName, arguments,
				envName, interfaceNumber);

		Path pythonFilePath = getToolsBasePath(envName, interfaceNumber).resolve(actionName + PYTHON_FILE_EXTENSION);
		if (!Files.exists(pythonFilePath)) {
			throw new IOException("Python file not found: " + pythonFilePath);
		}

		Path mockTauBenchPath = getMockTauBenchPath();
		StringBuilder scriptBuilder = new StringBuilder();
		scriptBuilder.append("import sys\n");
		scriptBuilder.append("import json\n");
		scriptBuilder.append("# Load mock tau_bench before any other imports\n");
		scriptBuilder
				.append(String.format("sys.path.insert(0, '%s')\n", mockTauBenchPath.toString().replace("\\", "\\\\")));
		scriptBuilder.append("import mock_tau_bench\n");
		scriptBuilder.append(String.format("sys.path.insert(0, '%s')\n",
				pythonFilePath.getParent().toString().replace("\\", "\\\\")));
		scriptBuilder.append(String.format("from %s import *\n", actionName));

		// Load data file
		scriptBuilder.append(String.format("with open(r'%s', 'r') as f:\n", dataFilePath));
		scriptBuilder.append("    data = json.load(f)\n");

		// Prepare arguments
		scriptBuilder.append("args = ").append(objectMapper.writeValueAsString(arguments)).append("\n");

		// Call the function
		scriptBuilder.append(String.format("result = %s.invoke(data, **args)\n", toPascalCase(actionName)));

		// Write modified data back to file for subsequent calls in the same session
		scriptBuilder.append(String.format("with open(r'%s', 'w') as f:\n", dataFilePath));
		scriptBuilder.append("    json.dump(data, f, indent=2)\n");

		scriptBuilder.append("print(result)\n");

		log.debug("Python script will read from and write back to: {}", dataFilePath);
		return executePythonScript(scriptBuilder.toString());
	}

	/**
	 * Execute a Python script and return output
	 */
	private static String executePythonScript(String script) throws IOException {
		Path tempScript = Files.createTempFile(TEMP_SCRIPT_PREFIX, PYTHON_FILE_EXTENSION);
		try {
			Files.writeString(tempScript, script);

			ProcessBuilder pb = new ProcessBuilder(PYTHON_COMMAND, tempScript.toString());
			pb.redirectErrorStream(true);

			Process process = pb.start();

			StringBuilder output = new StringBuilder();
			List<String> jsonLines = new ArrayList<>();
			try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
				String line;
				while ((line = reader.readLine()) != null) {
					output.append(line).append("\n");
					log.debug("Python output: {}", line);

					// Collect lines that look like JSON (start with { or [)
					String trimmed = line.trim();
					if (trimmed.startsWith("{") || trimmed.startsWith("[")) {
						jsonLines.add(line);
					}
				}
			}

			boolean finished = process.waitFor(PYTHON_EXECUTION_TIMEOUT_SECONDS, TimeUnit.SECONDS);
			if (!finished) {
				process.destroyForcibly();
				throw new IOException("Python script execution timed out");
			}

			int exitCode = process.exitValue();

			if (exitCode != 0) {
				String result = output.toString().trim();
				log.error("Python script failed with exit code {}: {}", exitCode, result);
				throw new IOException("Python execution failed: " + result);
			}

			// Return only JSON lines, joined together
			// If there's only one JSON line, return it; if multiple, return the last one
			// (most recent output)
			if (!jsonLines.isEmpty()) {
				String jsonOutput = String.join("\n", jsonLines);
				log.debug("Extracted JSON output: {}", jsonOutput);
				return jsonOutput.trim();
			}

			// Fallback to full output if no JSON-like lines found
			String result = output.toString().trim();
			log.warn("No JSON lines found in Python output, returning full output");
			return result;

		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new IOException("Python execution interrupted", e);
		} finally {
			Files.deleteIfExists(tempScript);
		}
	}

	/**
	 * Parse metadata JSON from Python get_info()
	 */
	private static PythonFunctionMetadata parseMetadata(String jsonOutput, String actionName) throws IOException {
		try {
			@SuppressWarnings("unchecked")
			Map<String, Object> info = objectMapper.readValue(jsonOutput, Map.class);

			@SuppressWarnings("unchecked")
			Map<String, Object> function = (Map<String, Object>) info.get("function");

			PythonFunctionMetadata metadata = new PythonFunctionMetadata();
			metadata.setFunctionName((String) function.get("name"));
			metadata.setDescription((String) function.get("description"));

			@SuppressWarnings("unchecked")
			Map<String, Object> parameters = (Map<String, Object>) function.get("parameters");

			@SuppressWarnings("unchecked")
			Map<String, Object> properties = (Map<String, Object>) parameters.get("properties");

			@SuppressWarnings("unchecked")
			List<String> required = (List<String>) parameters.get("required");

			metadata.setRequiredParams(required != null ? required : new ArrayList<>());

			// Parse parameter details
			Map<String, PythonFunctionMetadata.ParameterInfo> paramMap = new java.util.HashMap<>();
			if (properties != null) {
				for (Map.Entry<String, Object> entry : properties.entrySet()) {
					@SuppressWarnings("unchecked")
					Map<String, Object> paramDetails = (Map<String, Object>) entry.getValue();

					PythonFunctionMetadata.ParameterInfo paramInfo = new PythonFunctionMetadata.ParameterInfo();
					paramInfo.setType((String) paramDetails.get("type"));
					paramInfo.setDescription((String) paramDetails.get("description"));
					paramInfo.setRequired(required != null && required.contains(entry.getKey()));

					if (paramDetails.containsKey("enum")) {
						@SuppressWarnings("unchecked")
						List<String> enumValues = (List<String>) paramDetails.get("enum");
						paramInfo.setEnumValues(enumValues);
					}

					paramMap.put(entry.getKey(), paramInfo);
				}
			}
			metadata.setParameters(paramMap);

			log.info("Parsed metadata for {}: {} required params, {} total params", actionName,
					metadata.getRequiredParams().size(), paramMap.size());

			return metadata;

		} catch (Exception e) {
			log.error("Failed to parse metadata JSON: {}", jsonOutput, e);
			throw new IOException("Failed to parse Python metadata", e);
		}
	}

	/**
	 * Convert snake_case to PascalCase
	 */
	private static String toPascalCase(String snakeCase) {
		StringBuilder result = new StringBuilder();
		boolean capitalizeNext = true;

		for (char c : snakeCase.toCharArray()) {
			if (c == '_') {
				capitalizeNext = true;
			} else {
				if (capitalizeNext) {
					result.append(Character.toUpperCase(c));
					capitalizeNext = false;
				} else {
					result.append(c);
				}
			}
		}

		return result.toString();
	}
}
