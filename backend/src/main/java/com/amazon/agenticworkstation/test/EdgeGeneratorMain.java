package com.amazon.agenticworkstation.test;

import java.nio.file.Path;
import java.util.List;

import com.amazon.agenticworkstation.dto.TaskDto;
import com.amazon.agenticworkstation.service.EdgeGenerator;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

/**
 * Small CLI demo that invokes EdgeGenerator and prints results. Usage: java ...
 * EdgeGeneratorMain [path/to/task.json]
 */
public class EdgeGeneratorMain {
	// Set this to a task.json path string to use it instead of passing as a
	// command-line arg.
	public static String TASK_JSON_PATH = "C:\\Users\\bansa\\turing\\agenticWorkstation\\backend\\test\\tasks_files\\task.json";

	public static void main(String[] args) {
		if (TASK_JSON_PATH != null && !TASK_JSON_PATH.isBlank()) {
			Path p = Path.of(TASK_JSON_PATH);
			List<TaskDto.EdgeDto> edges = EdgeGenerator.edgesFromTaskJson(p);
			// Print edges as JSON
			ObjectMapper mapper = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);
			try {
				String json = mapper.writeValueAsString(edges != null ? edges : List.of());
				System.out.println(json);
			} catch (Exception ex) {
				System.err.println("Failed to serialize edges to JSON: " + ex.getMessage());
				ex.printStackTrace();
			}
			return;
		}
	}

}
