# Amazon Agentic Workstation - Flutter Frontend

A modern, responsive Flutter web application for the Amazon Agentic Workstation task orchestration system.

## Features

- **Modern UI/UX**: Clean, Amazon-branded interface with dark/light theme support
- **9-Step Workflow**: Intuitive collapsible steps for task configuration
- **Real-time Validation**: Live validation of inputs and server responses
- **State Management**: Robust state management using Provider pattern
- **Responsive Design**: Works seamlessly on desktop, tablet, and mobile
- **Error Handling**: Comprehensive error handling with user-friendly messages
- **File Downloads**: Integrated file download functionality
- **Animation**: Smooth animations and transitions using flutter_animate

## Architecture

```
lib/
├── main.dart                 # App entry point
├── models/                   # Data models
│   └── task_model.dart
├── providers/                # State management
│   └── task_provider.dart
├── screens/                  # UI screens
│   └── main_screen.dart
├── services/                 # API services
│   └── api_service.dart
├── theme/                    # App theming
│   └── app_theme.dart
├── utils/                    # Utilities and constants
│   └── app_utils.dart
└── widgets/                  # Reusable widgets
    ├── connection_status_widget.dart
    ├── custom_widgets.dart
    ├── progress_indicator_widget.dart
    └── step_card.dart
```

## Dependencies

- **flutter**: Framework
- **provider**: State management
- **http**: HTTP client for API calls
- **flutter_animate**: Animations
- **json_annotation**: JSON serialization
- **shared_preferences**: Local storage
- **file_picker**: File selection
- **path_provider**: File system access

## Getting Started

### Prerequisites

- Flutter SDK (>=3.0.0)
- Dart SDK
- Backend server running on http://localhost:8080

### Installation

1. Install dependencies:
```bash
flutter pub get
```

2. Generate model files:
```bash
flutter packages pub run build_runner build
```

3. Run the application:
```bash
flutter run -d chrome
```

## Usage

### 9-Step Workflow

1. **Instruction**: Enter the main task instruction
2. **Validate Instruction**: Verify the instruction with the backend
3. **Actions**: Define comma-separated actions
4. **User ID**: Specify user identifier
5. **Outputs**: Define expected outputs
6. **Edges**: Define task relationships in JSON format
7. **Generate task.json**: Create the task configuration file
8. **Validate task.json**: Verify the generated configuration
9. **Download All**: Download all files as a zip archive

### Features

- **Connection Status**: Real-time backend connection monitoring
- **Progress Indicators**: Visual feedback for ongoing operations
- **Error Handling**: Clear error messages and recovery options
- **Data Persistence**: Form data is maintained across sessions
- **Responsive Design**: Adapts to different screen sizes

### API Integration

The frontend communicates with the Spring Boot backend through these endpoints:

- `POST /validate-instruction`: Validate instruction text
- `POST /generate-task`: Generate task.json from form data
- `POST /validate-task`: Validate generated task.json
- `GET /download-all`: Download all files as zip

## Customization

### Theming

The app uses Amazon's brand colors and supports both light and dark themes. Customize themes in `lib/theme/app_theme.dart`.

### Adding New Steps

To add new steps to the workflow:

1. Update `AppConstants.steps` in `lib/utils/app_utils.dart`
2. Add corresponding UI in `lib/screens/main_screen.dart`
3. Update the provider logic in `lib/providers/task_provider.dart`

### Modifying API Endpoints

Update base URL and endpoints in `lib/services/api_service.dart`.

## Performance Optimizations

- **Lazy Loading**: Widgets are built only when needed
- **State Optimization**: Efficient state updates using Provider
- **Memory Management**: Proper disposal of controllers and animations
- **Network Optimization**: Request debouncing and caching

## Testing

```bash
# Run all tests
flutter test

# Run with coverage
flutter test --coverage
```

## Building for Production

```bash
# Build web app
flutter build web

# Build for different platforms
flutter build apk     # Android
flutter build ios     # iOS
flutter build windows # Windows
flutter build macos   # macOS
flutter build linux   # Linux
```

## Troubleshooting

### Common Issues

1. **Backend Connection Failed**
   - Ensure backend server is running on http://localhost:8080
   - Check network connectivity
   - Verify CORS settings on backend

2. **Build Errors**
   - Run `flutter clean && flutter pub get`
   - Check Flutter and Dart SDK versions
   - Regenerate model files with build_runner

3. **Performance Issues**
   - Enable Flutter DevTools for debugging
   - Check for memory leaks in provider patterns
   - Optimize widget rebuilds

## Contributing

1. Follow Flutter/Dart coding standards
2. Add tests for new features
3. Update documentation
4. Use meaningful commit messages

## License

This project is part of the Amazon Agentic Workstation suite.