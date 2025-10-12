# Instruction Validation Screen - Flutter UI

## Overview
A beautifully designed Flutter UI page that replicates the HTML instruction validation interface. This screen provides a comprehensive validation interface for AI task instructions.

## Features

### 🎨 **Design Elements**
- **Gradient Background**: Smooth gradient background with customizable colors
- **Custom App Bar**: Branded Turing-style app bar with logo
- **Animated UI**: Smooth fade-in and slide animations on page load
- **Responsive Layout**: Adapts to different screen sizes

### 🔧 **Functionality**
- **Model Selection**: Dropdown to select Anthropic Claude models
- **Multi-field Input**: 
  - Initial Prompt (read-only template)
  - Examples input area
  - Instruction input area
  - Policy input area
- **Real-time Validation**: Connect to backend API for instruction validation
- **Error Handling**: Comprehensive error display and handling
- **Loading States**: Visual feedback during API calls

### 📱 **Navigation**
- Accessible from the main screen via the menu (three dots) → "Instruction Validation"
- Clean back navigation to return to main screen

## Files Created

### Core Screen
- `lib/screens/instruction_validation_screen.dart` - Main validation screen

### Provider
- `lib/providers/instruction_validation_provider.dart` - State management for validation

### Widgets
- `lib/widgets/gradient_background.dart` - Reusable gradient background widget
- `lib/widgets/custom_app_bar.dart` - Custom branded app bar widget

## Integration

The screen is integrated into the main application:
1. Added navigation menu item in `main_screen.dart`
2. Route handling for navigation
3. Proper state management with Provider pattern

## API Integration

The screen connects to the Spring Boot backend:
- **Endpoint**: `POST /validate-instruction`
- **Base URL**: `http://localhost:8080`
- **Request Format**: JSON with task data, model, examples, and policy
- **Response**: Validation result text

## Design Philosophy

The UI follows the HTML template design with:
- **Turing branding** with gradient colors
- **Clean, professional layout**
- **Material Design 3** components
- **Accessibility considerations**
- **Consistent spacing and typography**

## Usage

1. Navigate to the screen from the main menu
2. Select an Anthropic model from the dropdown
3. Enter examples, instruction, and policy text
4. Tap "Validate Instruction" to submit for validation
5. View results or error messages

The screen provides a seamless experience for validating AI task instructions with professional UI/UX design matching the original HTML interface.