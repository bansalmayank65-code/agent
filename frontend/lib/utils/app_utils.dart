class AppConstants {
  // API endpoints
  static const String defaultBackendUrl = 'http://localhost:8080';
  
  // Step information
  static const List<Map<String, dynamic>> steps = [
    {
      'title': 'Instruction',
      'description': 'Enter the main instruction for your task',
      'icon': 'description',
      'required': true,
    },
    {
      'title': 'Validate Instruction',
      'description': 'Validate the entered instruction',
      'icon': 'check_circle',
      'required': false,
    },
    {
      'title': 'Actions',
      'description': 'Define actions for your task (comma-separated)',
      'icon': 'list',
      'required': false,
    },
    {
      'title': 'User ID',
      'description': 'Specify the user identifier',
      'icon': 'person',
      'required': false,
    },
    {
      'title': 'Outputs',
      'description': 'Define expected outputs (comma-separated)',
      'icon': 'output',
      'required': false,
    },
    {
      'title': 'Edges',
      'description': 'Define task relationships (JSON format)',
      'icon': 'account_tree',
      'required': false,
    },
    {
      'title': 'Generate task.json',
      'description': 'Generate the task configuration file',
      'icon': 'code',
      'required': false,
    },
    {
      'title': 'Validate task.json',
      'description': 'Validate the generated task configuration',
      'icon': 'verified',
      'required': false,
    },
    {
      'title': 'Download All',
      'description': 'Download all files as a zip archive',
      'icon': 'download',
      'required': false,
    },
  ];

  // Validation messages
  static const String instructionRequiredMessage = 'Instruction is required';
  static const String invalidJsonMessage = 'Invalid JSON format';
  static const String serverConnectionErrorMessage = 'Unable to connect to server';
  
  // Success messages
  static const String taskGeneratedMessage = 'Task JSON generated successfully';
  static const String filesDownloadedMessage = 'Files downloaded successfully';
  static const String validationSuccessMessage = 'Validation completed successfully';
}

class AppHelpers {
  /// Validates if a string is valid JSON
  static bool isValidJson(String jsonString) {
    try {
      if (jsonString.trim().isEmpty) return false;
      // Add basic JSON validation logic here
      return jsonString.trim().startsWith('[') && jsonString.trim().endsWith(']') ||
             jsonString.trim().startsWith('{') && jsonString.trim().endsWith('}');
    } catch (e) {
      return false;
    }
  }

  /// Formats JSON string for display
  static String formatJson(String jsonString) {
    // Basic JSON formatting - in a real app, you'd use a proper JSON formatter
    return jsonString
        .replaceAll(',', ',\n  ')
        .replaceAll('[', '[\n  ')
        .replaceAll(']', '\n]')
        .replaceAll('{', '{\n  ')
        .replaceAll('}', '\n}');
  }

  /// Parses comma-separated values into a list
  static List<String> parseCommaSeparated(String input) {
    if (input.trim().isEmpty) return [];
    return input
        .split(',')
        .map((item) => item.trim())
        .where((item) => item.isNotEmpty)
        .toList();
  }

  /// Validates email format
  static bool isValidEmail(String email) {
    return RegExp(r'^[^@]+@[^@]+\.[^@]+').hasMatch(email);
  }

  /// Generates a random user ID
  static String generateRandomUserId() {
    final timestamp = DateTime.now().millisecondsSinceEpoch;
    return 'user_$timestamp';
  }

  /// Calculates completion percentage based on filled fields
  static int calculateCompletionPercentage(Map<String, dynamic> taskData) {
    int filledFields = 0;
    int totalFields = 5; // instruction, actions, userId, outputs, edges

    if (taskData['instruction']?.toString().isNotEmpty == true) filledFields++;
    if (taskData['actions']?.isNotEmpty == true) filledFields++;
    if (taskData['userId']?.toString().isNotEmpty == true) filledFields++;
    if (taskData['outputs']?.isNotEmpty == true) filledFields++;
    if (taskData['edges']?.isNotEmpty == true) filledFields++;

    return ((filledFields / totalFields) * 100).round();
  }
}

class AppStyles {
  static const double defaultPadding = 16.0;
  static const double smallPadding = 8.0;
  static const double largePadding = 24.0;
  
  static const double defaultBorderRadius = 8.0;
  static const double largeBorderRadius = 12.0;
  
  static const double defaultElevation = 2.0;
  static const double largeElevation = 4.0;
}