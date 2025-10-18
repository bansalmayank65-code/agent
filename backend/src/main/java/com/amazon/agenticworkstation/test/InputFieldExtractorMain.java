package com.amazon.agenticworkstation.test;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import com.amazon.agenticworkstation.action.InputFieldExtractor;

/**
 * Example demonstrating how to use InputFieldExtractor to get required fields
 * for a set of actions.
 */
public class InputFieldExtractorMain {

	public static void main(String[] args) {
		try {
			String envName = "hr_experts";
			int interfaceNumber = 1;

			List<String> actionNames = Arrays.asList("discover_user_employee_entities", "check_approval",
					"discover_job_entities", "manage_skill");

			System.out.println("=== Extracting Required Fields ===\n");

			// Option 1: Extract all required fields (includes common fields)
			// Now returns Map<String, Map<String, String>> where outer key is action name
			Map<String, Map<String, String>> actionFieldsMap = InputFieldExtractor.extractRequiredFields(actionNames,
					envName, interfaceNumber);

			System.out.println("===All Required Fields grouped by action===:");
			for (Map.Entry<String, Map<String, String>> entry : actionFieldsMap.entrySet()) {
				System.out.println("  " + entry.getKey() + ": " + entry.getValue().keySet());
			}
			System.out.println();

			// Option 2: Print as code that can be copied
			System.out.println("===Copy-paste code===:");
			InputFieldExtractor.printAsCode(actionFieldsMap);
			System.out.println();

			// Option 3: Print with example values
			System.out.println("===With example values===:");
			InputFieldExtractor.printWithExamples(actionFieldsMap);
			System.out.println();

			// Option 4: Extract only strictly required fields
			Map<String, Map<String, String>> strictlyRequired = InputFieldExtractor
					.extractStrictlyRequiredFields(actionNames, envName, interfaceNumber);

			System.out.println("Strictly Required Fields Only (grouped by action):");
			for (Map.Entry<String, Map<String, String>> entry : strictlyRequired.entrySet()) {
				System.out.println("  " + entry.getKey() + ": " + entry.getValue().keySet());
			}
			System.out.println();

			// Option 5: Extract with null values
			Map<String, Map<String, String>> withNull = InputFieldExtractor.extractRequiredFieldsWithNull(actionNames,
					envName, interfaceNumber);

			System.out.println("With null values (grouped by action):");
			for (Map.Entry<String, Map<String, String>> entry : withNull.entrySet()) {
				System.out.println("  " + entry.getKey() + ": " + entry.getValue());
			}

		} catch (IOException e) {
			System.err.println("Error: " + e.getMessage());
			e.printStackTrace();
		}
	}
}
