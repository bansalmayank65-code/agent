// Web implementation using dart:html (isolated so the rest of the app avoids direct dart:html imports)
// ignore: avoid_web_libraries_in_flutter
import 'dart:html' as html;
import 'dart:convert';

Future<String?> pickDirectory() async {
  try {
    final input = html.FileUploadInputElement();
    
    // Set attributes for directory picking - this will open a folder picker
    input.setAttribute('webkitdirectory', 'true');
    input.setAttribute('directory', 'true');
    input.multiple = false; // Set to false to avoid multiple file selection
    
    // Style the input to be invisible but still functional
    input.style.position = 'absolute';
    input.style.left = '-9999px';
    input.style.opacity = '0';
    input.style.pointerEvents = 'none';
    
    // Append to body temporarily
    html.document.body?.append(input);
    
    // Trigger the directory picker
    input.click();
    
    // Wait for the user to make a selection
    await input.onChange.first;
    
    // Clean up the input element
    input.remove();
    
    // Check if any files were selected
    if (input.files == null || input.files!.isEmpty) {
      return null;
    }
    
    // Extract directory name from the first file's path
    String virtualPath = 'selected-directory';
    try {
      final file = input.files!.first;
      final relativePath = (file as dynamic).webkitRelativePath as String?;
      
      if (relativePath != null && relativePath.isNotEmpty) {
        // Get the root directory name from the relative path
        final pathSegments = relativePath.split('/');
        if (pathSegments.isNotEmpty && pathSegments.first.isNotEmpty) {
          virtualPath = pathSegments.first;
        }
      } else if (file.name.isNotEmpty) {
        // Fallback: use a generic name based on file name
        virtualPath = 'directory-${DateTime.now().millisecondsSinceEpoch}';
      }
    } catch (e) {
      // If we can't extract the path, use a timestamped fallback
      virtualPath = 'web-directory-${DateTime.now().millisecondsSinceEpoch}';
    }
    
    // Return a web-friendly path format
    return 'web:/$virtualPath';
    
  } catch (e) {
    // Return null if anything goes wrong
    return null;
  }
}

Future<Map<String, dynamic>?> pickAndReadJsonFile() async {
  try {
    final input = html.FileUploadInputElement();
    input.accept = '.json,application/json';
    input.click();
    await input.onChange.first;
    if (input.files == null || input.files!.isEmpty) return null;
    final file = input.files!.first;
    final reader = html.FileReader();
    reader.readAsText(file);
    await reader.onLoad.first;
    final content = reader.result?.toString();
    if (content == null) return null;
    final decoded = jsonDecode(content);
    if (decoded is Map<String, dynamic>) {
      return {
        'data': decoded,
        'fileName': file.name,
      };
    }
    return null;
  } catch (_) { return null; }
}

Future<bool> copyTextWeb(String text) async {
  try {
    await html.window.navigator.clipboard?.writeText(text);
    return true;
  } catch (_) { return false; }
}
