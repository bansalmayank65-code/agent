import 'dart:html' as html;

import 'package:flutter/foundation.dart';

class InstructionValidationProvider extends ChangeNotifier {
  static const String externalUrl =
      'https://turing-amazon-toolings.vercel.app/instruction_validation';

  void redirect({String? policy}) {
    html.window.location.href = externalUrl;
  }

  Future<void> redirectWithPolicy(String policy) async {
    await _copyToClipboard(policy);
    redirect(policy: policy);
  }

  Future<void> _copyToClipboard(String text) async {
    try {
      // Modern async clipboard API
      // ignore: undefined_prefixed_name
      await html.window.navigator.clipboard?.writeText(text);
      return;
    } catch (_) {
      // Fallback: create hidden textarea and execCommand
      try {
        final textarea = html.TextAreaElement();
        textarea.value = text;
        textarea.style
          ..position = 'fixed'
          ..top = '-1000px'
          ..left = '-1000px';
        html.document.body?.append(textarea);
        textarea.focus();
        textarea.select();
        html.document.execCommand('copy');
        textarea.remove();
      } catch (_) {
        // Swallow; user can still paste manually.
      }
    }
  }
}
