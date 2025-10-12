// Platform-agnostic stubs (non-web) for directory and JSON file picking.
// These return null indicating unsupported operations on this platform.

Future<String?> pickDirectory() async => null;

Future<Map<String, dynamic>?> pickAndReadJsonFile() async => null;

Future<bool> copyTextWeb(String text) async => false; // no-op on non-web
