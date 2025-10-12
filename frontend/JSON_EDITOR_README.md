# 🧩 JSON Editor + Viewer

A comprehensive Flutter widget that combines powerful JSON editing capabilities with an intuitive tree visualization. Perfect for debugging, configuration management, and data manipulation.

## ✨ Features

### 🎯 Core Functionality
- **Live JSON Editing** - Real-time syntax highlighting and validation
- **Tree Visualization** - Collapsible/expandable JSON structure viewer
- **Split/Tabbed Views** - Responsive design for desktop and mobile
- **Format Tools** - Pretty print, minify, copy to clipboard
- **Validation** - Real-time JSON syntax error detection and reporting

### 🔧 Editing Tools
- **Format JSON** (`Ctrl+F`) - Pretty print with proper indentation
- **Minify JSON** - Compress JSON to single line
- **Copy to Clipboard** - Platform-aware clipboard operations
- **Parse/Refresh** - Manual re-parsing trigger
- **Clear JSON** - Quick content clearing
- **Load Sample** - Load example JSON structures

### 📊 Visualization
- **Syntax Highlighting** - Color-coded JSON elements
- **Collapsible Tree** - Navigate complex nested structures
- **Type Indicators** - Visual distinction for strings, numbers, booleans, arrays
- **Error Highlighting** - Clear indication of JSON syntax issues

## 🚀 Usage

### Basic Usage

```dart
import 'package:flutter/material.dart';
import '../widgets/json_editor_viewer.dart';

class MyJsonEditor extends StatelessWidget {
  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(title: Text('JSON Editor')),
      body: JsonEditorViewer(
        initialJson: '{"hello": "world"}',
        onJsonChanged: (jsonString) {
          print('JSON updated: $jsonString');
        },
        showToolbar: true,
        splitView: true,
      ),
    );
  }
}
```

### Advanced Usage with Custom Configuration

```dart
JsonEditorViewer(
  initialJson: complexJsonString,
  onJsonChanged: (jsonString) {
    // Handle JSON changes
    try {
      final parsed = jsonDecode(jsonString);
      // Process valid JSON
      updateDataModel(parsed);
    } catch (e) {
      // Handle parse errors
      showErrorDialog(e.toString());
    }
  },
  title: 'Configuration Editor',
  showToolbar: true,
  splitView: MediaQuery.of(context).size.width > 800,
)
```

### Integration with Task Provider

```dart
// In your TaskProvider integration
Widget _buildJsonEditor(TaskProvider provider) {
  return JsonEditorViewer(
    initialJson: _getCurrentTaskJson(provider),
    onJsonChanged: (jsonString) {
      try {
        final parsed = jsonDecode(jsonString);
        if (_isValidTaskJson(parsed)) {
          provider.importTaskJson(parsed);
        }
      } catch (e) {
        // Ignore parse errors during editing
      }
    },
    showToolbar: true,
    splitView: true,
  );
}
```

## 🎨 UI Components

### Toolbar Features

| Button | Icon | Function | Shortcut |
|--------|------|----------|----------|
| **Format** | `🧱` | Pretty print JSON | `Ctrl+F` |
| **Minify** | `📦` | Compress JSON | `Ctrl+M` |
| **Copy** | `📋` | Copy to clipboard | `Ctrl+C` |
| **Refresh** | `🔄` | Re-parse JSON | `F5` |
| **Sample** | `📄` | Load example | - |
| **Clear** | `🗑️` | Clear content | `Ctrl+Del` |

### Validation Indicators

- **✅ Valid JSON** - Green indicator with checkmark
- **❌ Invalid JSON** - Red indicator with error details
- **📊 Character Count** - Live character counter
- **🔍 Error Details** - Detailed syntax error messages

## 📱 Responsive Design

### Desktop (Wide Screen)
- **Split View** - Editor on left, viewer on right
- **Full Toolbar** - All tools visible
- **Keyboard Shortcuts** - Enhanced productivity

### Mobile/Tablet (Narrow Screen)
- **Tabbed View** - Switch between editor and viewer
- **Compact Toolbar** - Essential tools only
- **Touch Optimized** - Finger-friendly interface

## 🔌 Integration Points

### With Agentic Workstation
```dart
// Integrated as section 7 in left navigation
case 7:
  return _sectionWrapper(key, 'JSON Editor & Viewer', _buildJsonEditor(provider));
```

### With File Management
```dart
// Load from file
final content = await apiService.readFileContent(filePath);
JsonEditorViewer(initialJson: content, ...)

// Save to file
onJsonChanged: (jsonString) async {
  await apiService.writeFileContent(filePath, jsonString);
}
```

## 🎯 Use Cases

### 1. Task Configuration Editing
```json
{
  "env": "finance",
  "interface_num": 4,
  "task": {
    "instruction": "Process financial data",
    "actions": [...],
    "edges": [...]
  }
}
```

### 2. API Response Debugging
```json
{
  "status": "success",
  "data": {
    "users": [...],
    "metadata": {...}
  },
  "errors": []
}
```

### 3. Configuration Management
```json
{
  "database": {
    "host": "localhost",
    "port": 5432,
    "credentials": {...}
  },
  "features": {
    "enabled": [...],
    "disabled": [...]
  }
}
```

## 🛠️ Technical Implementation

### Dependencies
```yaml
dependencies:
  flutter_json_viewer: ^1.0.1  # Tree visualization
  flutter/services: any        # Clipboard operations
```

### Key Components
- `JsonEditorViewer` - Main widget container
- `_JsonEditorViewerState` - State management
- `_buildToolbar()` - Toolbar with formatting tools
- `_buildEditorView()` - Text editing interface
- `_buildViewerView()` - Tree visualization pane

### Platform Support
- ✅ **Web** - Full clipboard and file support
- ✅ **Windows** - Native clipboard operations
- ✅ **macOS** - Native clipboard operations
- ✅ **Linux** - Native clipboard operations
- ✅ **Android** - Mobile-optimized interface
- ✅ **iOS** - Mobile-optimized interface

## 🔍 Error Handling

### JSON Validation
```dart
void _parseJson() {
  setState(() {
    try {
      _parsedJson = jsonDecode(_textController.text);
      _isValidJson = true;
      _validationError = null;
    } catch (e) {
      _isValidJson = false;
      _validationError = e.toString();
      _parsedJson = {'error': 'Invalid JSON: $e'};
    }
  });
}
```

### User Feedback
- **Toast Messages** - Success/error notifications
- **Visual Indicators** - Color-coded validation status
- **Error Details** - Specific syntax error descriptions
- **Graceful Degradation** - Continues working with invalid JSON

## 🎨 Customization

### Theming
```dart
// Custom colors and styling
const Color primaryColor = Color(0xFF2563EB);
const Color successColor = Color(0xFF059669);
const Color errorColor = Color(0xFFDC2626);
const Color warningColor = Color(0xFF7C3AED);
```

### Layout Options
```dart
// Responsive layout switching
bool splitView = MediaQuery.of(context).size.width > 800;

JsonEditorViewer(
  splitView: splitView,
  showToolbar: true,
  // ... other options
)
```

## 🚨 Best Practices

### Performance
- **Large JSON Handling** - Efficient parsing and rendering
- **Memory Management** - Proper disposal of controllers
- **Debounced Updates** - Prevent excessive re-parsing

### UX Guidelines
- **Clear Visual Feedback** - Always show validation status
- **Keyboard Shortcuts** - Support power users
- **Error Recovery** - Allow easy correction of mistakes
- **Auto-formatting** - Help users maintain clean JSON

### Security
- **Input Validation** - Sanitize JSON content
- **Safe Parsing** - Handle malformed input gracefully
- **No Eval** - Pure JSON parsing without code execution

---

## 📄 Example Screenshots

### Split View (Desktop)
```
┌─────────────────┬─────────────────┐
│   JSON Editor   │   Tree Viewer   │
│                 │                 │
│ {               │ 📁 root         │
│   "name": "...", │ ├─ 📝 name      │
│   "data": {     │ └─ 📁 data      │
│     "items": [] │    └─ 📋 items  │
│   }             │                 │
│ }               │                 │
└─────────────────┴─────────────────┘
```

### Mobile View (Tabbed)
```
┌─────────────────────────────────────┐
│ [Editor Tab] [Viewer Tab]           │
├─────────────────────────────────────┤
│                                     │
│ {                                   │
│   "mobile": "friendly",             │
│   "interface": "optimized"          │
│ }                                   │
│                                     │
└─────────────────────────────────────┘
```

This JSON Editor + Viewer provides a comprehensive solution for JSON manipulation within the Agentic Workstation, offering both power-user features and intuitive visual feedback.