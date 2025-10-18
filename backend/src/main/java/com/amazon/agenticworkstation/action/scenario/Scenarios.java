package com.amazon.agenticworkstation.action.scenario;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.amazon.agenticworkstation.action.scenario.ActionGeneratorBasedOnScenario.ScenarioExecutionResult;
import com.amazon.agenticworkstation.action.scenario.definitions.hr_experts.interface_1.AddCandidateRecordScenario;
import com.amazon.agenticworkstation.action.scenario.definitions.hr_experts.interface_1.CloseJobOpeningScenario;
import com.amazon.agenticworkstation.action.scenario.definitions.hr_experts.interface_1.CreateBenefitsPlanScenario;
import com.amazon.agenticworkstation.action.scenario.definitions.hr_experts.interface_1.CreateDepartmentScenario;
import com.amazon.agenticworkstation.action.scenario.definitions.hr_experts.interface_1.CreateExpenseReimbursementScenario;
import com.amazon.agenticworkstation.action.scenario.definitions.hr_experts.interface_1.CreateJobApplicationScenario;
import com.amazon.agenticworkstation.action.scenario.definitions.hr_experts.interface_1.CreateJobPositionScenario;
import com.amazon.agenticworkstation.action.scenario.definitions.hr_experts.interface_1.CreatePerformanceReviewScenario;
import com.amazon.agenticworkstation.action.scenario.definitions.hr_experts.interface_1.CreateTrainingProgramScenario;
import com.amazon.agenticworkstation.action.scenario.definitions.hr_experts.interface_1.DocumentUploadScenario;
import com.amazon.agenticworkstation.action.scenario.definitions.hr_experts.interface_1.EmployeeBenefitsEnrollmentScenario;
import com.amazon.agenticworkstation.action.scenario.definitions.hr_experts.interface_1.EmployeeOffboardingScenario;
import com.amazon.agenticworkstation.action.scenario.definitions.hr_experts.interface_1.EmployeeOnboardingScenario;
import com.amazon.agenticworkstation.action.scenario.definitions.hr_experts.interface_1.EmployeeTrainingEnrollmentScenario;
import com.amazon.agenticworkstation.action.scenario.definitions.hr_experts.interface_1.LeaveRequestProcessingScenario;
import com.amazon.agenticworkstation.action.scenario.definitions.hr_experts.interface_1.ManageApplicationStageScenario;
import com.amazon.agenticworkstation.action.scenario.definitions.hr_experts.interface_1.ManageJobPositionSkillsScenario;
import com.amazon.agenticworkstation.action.scenario.definitions.hr_experts.interface_1.ManageSkillsCreateScenario;
import com.amazon.agenticworkstation.action.scenario.definitions.hr_experts.interface_1.PayrollCorrectionScenario;
import com.amazon.agenticworkstation.action.scenario.definitions.hr_experts.interface_1.PayrollDeductionManagementScenario;
import com.amazon.agenticworkstation.action.scenario.definitions.hr_experts.interface_1.PostJobOpeningScenario;
import com.amazon.agenticworkstation.action.scenario.definitions.hr_experts.interface_1.ProcessExpenseReimbursementScenario;
import com.amazon.agenticworkstation.action.scenario.definitions.hr_experts.interface_1.ProcessPayrollRunScenario;
import com.amazon.agenticworkstation.action.scenario.definitions.hr_experts.interface_1.RecordInterviewOutcomeScenario;
import com.amazon.agenticworkstation.action.scenario.definitions.hr_experts.interface_1.ScheduleInterviewScenario;
import com.amazon.agenticworkstation.action.scenario.definitions.hr_experts.interface_1.TimesheetApprovalScenario;
import com.amazon.agenticworkstation.action.scenario.definitions.hr_experts.interface_1.TimesheetSubmissionScenario;
import com.amazon.agenticworkstation.action.scenario.definitions.hr_experts.interface_1.UpdateEmployeeProfileScenario;
import com.amazon.agenticworkstation.action.scenario.definitions.hr_experts.interface_1.UserProvisioningScenario;
import com.amazon.agenticworkstation.action.scenario.models.ScenarioExecutionException;
import com.amazon.agenticworkstation.action.scenario.models.ScenarioMetadata;

/**
 * Central registry for all predefined scenarios.
 * 
 * Scenarios are organized hierarchically by: - Environment (e.g., hr_experts,
 * finance_system) - Interface number (e.g., 1, 2, 3) - Scenario name (e.g.,
 * manage_skills)
 */
public final class Scenarios {
	// Hierarchical storage: Environment -> Interface -> List of Scenario Classes
	private static final Map<String, Map<Integer, List<Class<? extends BaseScenario>>>> scenariosByEnvAndInterface = new HashMap<>();

	static {
		registerHrExpertsScenarios();
	}

	private static void registerHrExpertsScenarios() {
		// User & Department Management
		registerScenario("hr_experts", 1, UserProvisioningScenario.class);
		registerScenario("hr_experts", 1, CreateDepartmentScenario.class);

		// Job Position & Recruitment
		registerScenario("hr_experts", 1, CreateJobPositionScenario.class);
		registerScenario("hr_experts", 1, PostJobOpeningScenario.class);
		registerScenario("hr_experts", 1, ManageJobPositionSkillsScenario.class);
		registerScenario("hr_experts", 1, ManageSkillsCreateScenario.class);
		registerScenario("hr_experts", 1, CloseJobOpeningScenario.class);
		registerScenario("hr_experts", 1, AddCandidateRecordScenario.class);
		registerScenario("hr_experts", 1, CreateJobApplicationScenario.class);
		registerScenario("hr_experts", 1, ManageApplicationStageScenario.class);

		// Interview Management
		registerScenario("hr_experts", 1, ScheduleInterviewScenario.class);
		registerScenario("hr_experts", 1, RecordInterviewOutcomeScenario.class);

		// Employee Lifecycle
		registerScenario("hr_experts", 1, EmployeeOnboardingScenario.class);
		registerScenario("hr_experts", 1, UpdateEmployeeProfileScenario.class);
		registerScenario("hr_experts", 1, EmployeeOffboardingScenario.class);

		// Timesheet Management
		registerScenario("hr_experts", 1, TimesheetSubmissionScenario.class);
		registerScenario("hr_experts", 1, TimesheetApprovalScenario.class);

		// Payroll Management
		registerScenario("hr_experts", 1, ProcessPayrollRunScenario.class);
		registerScenario("hr_experts", 1, PayrollDeductionManagementScenario.class);
		registerScenario("hr_experts", 1, PayrollCorrectionScenario.class);

		// Benefits Management
		registerScenario("hr_experts", 1, CreateBenefitsPlanScenario.class);
		registerScenario("hr_experts", 1, EmployeeBenefitsEnrollmentScenario.class);

		// Performance Management
		registerScenario("hr_experts", 1, CreatePerformanceReviewScenario.class);

		// Training Management
		registerScenario("hr_experts", 1, CreateTrainingProgramScenario.class);
		registerScenario("hr_experts", 1, EmployeeTrainingEnrollmentScenario.class);

		// Document Management
		registerScenario("hr_experts", 1, DocumentUploadScenario.class);

		// Leave Management
		registerScenario("hr_experts", 1, LeaveRequestProcessingScenario.class);

		// Expense Management
		registerScenario("hr_experts", 1, CreateExpenseReimbursementScenario.class);
		registerScenario("hr_experts", 1, ProcessExpenseReimbursementScenario.class);
	}

	private Scenarios() {
		// Private constructor to prevent instantiation
	}

	/**
	 * Register a scenario class into the hierarchical structure.
	 * 
	 * @param env             The environment name
	 * @param interfaceNumber The interface number
	 * @param scenarioClass   The scenario class to register
	 */
	private static void registerScenario(String env, int interfaceNumber, Class<? extends BaseScenario> scenarioClass) {
		// Add to hierarchical structure
		scenariosByEnvAndInterface.computeIfAbsent(env, k -> new HashMap<>())
				.computeIfAbsent(interfaceNumber, k -> new ArrayList<>()).add(scenarioClass);
	}

	// ========================================================================
	// Scenario Discovery Methods (for UI)
	// ========================================================================

	/**
	 * Get scenario class by environment and interface number.
	 * 
	 */
	public static Class<? extends BaseScenario> getScenarioClass(String scenarioName, String environment,
			int interfaceNumber) {
		Map<Integer, List<Class<? extends BaseScenario>>> interfaceMap = scenariosByEnvAndInterface.get(environment);
		if (interfaceMap == null) {
			throw new IllegalArgumentException("No scenarios found for environment: " + environment);
		}

		List<Class<? extends BaseScenario>> scenarios = interfaceMap.get(interfaceNumber);
		if (scenarios != null) {
			return scenarios.stream().filter(s -> s.getSimpleName().equalsIgnoreCase(scenarioName)).findFirst()
					.orElseThrow(() -> new IllegalArgumentException("No scenario found with name: " + scenarioName
							+ " in environment: " + environment + " interface: " + interfaceNumber));
		}

		throw new IllegalArgumentException("No scenarios found for environment: " + environment);
	}

	/**
	 * Get scenarios by environment and interface number.
	 * 
	 * @param environment     The environment name
	 * @param interfaceNumber The interface number
	 * @return List of scenario classes for the given environment and interface
	 */
	public static List<Class<? extends BaseScenario>> getScenarios(String environment, int interfaceNumber) {
		Map<Integer, List<Class<? extends BaseScenario>>> interfaceMap = scenariosByEnvAndInterface.get(environment);
		if (interfaceMap == null) {
			return Collections.emptyList();
		}

		return interfaceMap.get(interfaceNumber) != null ? new ArrayList<>(interfaceMap.get(interfaceNumber))
				: Collections.emptyList();
	}

	/**
	 * Get all available environments.
	 * 
	 * @return List of environment names
	 */
	public static List<String> getEnvironments() {
		return new ArrayList<>(scenariosByEnvAndInterface.keySet());
	}

	/**
	 * Get all interface numbers for a given environment.
	 * 
	 * @param environment The environment name
	 * @return List of interface numbers
	 */
	public static List<Integer> getInterfaceNumbers(String environment) {
		Map<Integer, List<Class<? extends BaseScenario>>> interfaceMap = scenariosByEnvAndInterface.get(environment);
		if (interfaceMap == null) {
			return Collections.emptyList();
		}
		return new ArrayList<>(interfaceMap.keySet());
	}

	/**
	 * Get all scenarios with their metadata (name, description, required inputs).
	 * 
	 * This method instantiates each scenario class (with empty parameters) to
	 * extract metadata information. Useful for displaying available scenarios in
	 * UI.
	 * 
	 * @return List of ScenarioMetadata containing all scenario information
	 */
	public static List<ScenarioMetadata> getAllScenariosMetadata() {
		List<ScenarioMetadata> allMetadata = new ArrayList<>();

		// Iterate through all environments
		for (String environment : getEnvironments()) {
			// Iterate through all interface numbers in this environment
			for (Integer interfaceNumber : getInterfaceNumbers(environment)) {
				// Get all scenarios for this environment and interface
				List<Class<? extends BaseScenario>> scenarios = getScenarios(environment, interfaceNumber);

				// Extract metadata from each scenario
				for (Class<? extends BaseScenario> scenarioClass : scenarios) {
					// Create a temporary instance to get metadata (with empty parameters)
					BaseScenario scenario = createScenarioInstance(scenarioClass, new HashMap<>());

					ScenarioMetadata metadata = new ScenarioMetadata(scenario.getScenarioName(),
							scenario.getDescription(), scenario.getEnvironment(), scenario.getInterfaceNumber(),
							scenario.getRequiredInputs(), scenarioClass.getSimpleName());

					allMetadata.add(metadata);
				}
			}
		}

		return allMetadata;
	}

	/**
	 * Get metadata for a single scenario by name, environment and interface number.
	 *
	 * This mirrors the behavior of getAllScenariosMetadata but for a single
	 * scenario. If the scenario cannot be fully instantiated, a minimal metadata
	 * object is returned with description unavailable and empty inputs.
	 *
	 * @param scenarioName    The simple name of the scenario class
	 *                        (case-insensitive)
	 * @param environment     The environment name
	 * @param interfaceNumber The interface number
	 * @return ScenarioMetadata for the requested scenario
	 */
	public static ScenarioMetadata getScenarioMetadata(String scenarioName, String environment, int interfaceNumber) {
		// Locate the scenario class (this will throw IllegalArgumentException if not
		// found)
		Class<? extends BaseScenario> scenarioClass = getScenarioClass(scenarioName, environment, interfaceNumber);

		// Create a temporary instance to get metadata (with empty parameters)
		BaseScenario scenario = createScenarioInstance(scenarioClass, null);

		return new ScenarioMetadata(scenario.getScenarioName(), scenario.getDescription(), scenario.getEnvironment(),
				scenario.getInterfaceNumber(), scenario.getRequiredInputs(), scenarioClass.getSimpleName());
	}

	/**
	 * Clear all registered scenarios (mainly for testing).
	 */
	public static void clearAll() {
		scenariosByEnvAndInterface.clear();
	}

	// ========================================================================
	// Scenario Execution Methods
	// ========================================================================

	/**
	 * Execute a scenario by name with parameters. This method requires specifying
	 * the environment and interface explicitly.
	 */
	public static ScenarioExecutionResult executeScenario(String scenarioName, String environment, int interfaceNumber,
			Map<String, Object> parameters) throws ScenarioExecutionException {
		return executeScenario(scenarioName, environment, interfaceNumber, parameters, null);
	}

	public static ScenarioExecutionResult executeScenario(String scenarioName, String environment, int interfaceNumber,
			Map<String, Object> parameters, String dataFilePath) throws ScenarioExecutionException {

		// Get the scenario class
		Class<? extends BaseScenario> scenarioClass = getScenarioClass(scenarioName, environment, interfaceNumber);

		// Create scenario instance
		BaseScenario scenario = createScenarioInstance(scenarioClass, parameters);

		// Get scenario configuration and inputs
		Map<String, Object> scenarioInputs = scenario.getScenarioInputs();

		// Execute the scenario using ActionGeneratorBasedOnScenario, optionally with provided data file path
		return ActionGeneratorBasedOnScenario.executeScenario(scenario.getScenarioConfig(), scenarioInputs, dataFilePath);
	}

	private static BaseScenario createScenarioInstance(Class<? extends BaseScenario> scenarioClass,
			Map<String, Object> parameters) {
		try {
			// Try to find constructor that takes Map<String, Object>
			return parameters == null ? scenarioClass.getConstructor().newInstance()
					: scenarioClass.getConstructor(Map.class).newInstance(parameters);
		} catch (SecurityException | InstantiationException | IllegalAccessException | IllegalArgumentException
				| InvocationTargetException | NoSuchMethodException e) {
			throw new IllegalArgumentException("Failed to create instance of scenario: " + scenarioClass.getSimpleName()
					+ ". Ensure the scenario has a public constructor that accepts Map<String, Object>", e);
		}
	}
}