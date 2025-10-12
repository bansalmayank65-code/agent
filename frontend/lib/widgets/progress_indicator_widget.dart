import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import '../providers/task_provider.dart';

enum ProgressType { linear, circular, dots }

class ProgressIndicatorWidget extends StatelessWidget {
  final String? message;
  final ProgressType type;
  final bool showMessage;
  final double? value;
  final Color? color;

  const ProgressIndicatorWidget({
    Key? key,
    this.message,
    this.type = ProgressType.linear,
    this.showMessage = true,
    this.value,
    this.color,
  }) : super(key: key);

  @override
  Widget build(BuildContext context) {
    return Consumer<TaskProvider>(
      builder: (context, provider, child) {
        if (!provider.isLoading && message == null) {
          return const SizedBox.shrink();
        }

        final displayMessage = message ?? 'Processing...';
        final progressColor = color ?? Theme.of(context).primaryColor;

        Widget progressWidget;
        switch (type) {
          case ProgressType.circular:
            progressWidget = SizedBox(
              width: 24,
              height: 24,
              child: CircularProgressIndicator(
                value: value,
                strokeWidth: 2,
                valueColor: AlwaysStoppedAnimation<Color>(progressColor),
              ),
            );
            break;
          case ProgressType.dots:
            progressWidget = _buildDotsIndicator(progressColor);
            break;
          case ProgressType.linear:
            progressWidget = LinearProgressIndicator(
              value: value,
              valueColor: AlwaysStoppedAnimation<Color>(progressColor),
            );
            break;
        }

        return Container(
          width: double.infinity,
          padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 8),
          child: Column(
            children: [
              progressWidget,
              if (showMessage) ...[
                const SizedBox(height: 8),
                Text(
                  displayMessage,
                  style: TextStyle(
                    color: Colors.grey.shade600,
                    fontSize: 14,
                  ),
                  textAlign: TextAlign.center,
                ),
              ],
            ],
          ),
        );
      },
    );
  }

  Widget _buildDotsIndicator(Color color) {
    return Row(
      mainAxisAlignment: MainAxisAlignment.center,
      children: List.generate(3, (index) {
        return Container(
          margin: const EdgeInsets.symmetric(horizontal: 2),
          child: AnimatedContainer(
            duration: Duration(milliseconds: 600 + (index * 200)),
            width: 8,
            height: 8,
            decoration: BoxDecoration(
              color: color,
              shape: BoxShape.circle,
            ),
          ),
        );
      }),
    );
  }
}

/// Specialized progress indicator for validation steps
class ValidationProgressIndicator extends StatefulWidget {
  final String stepName;
  final bool isRunning;
  final String? currentAction;

  const ValidationProgressIndicator({
    Key? key,
    required this.stepName,
    required this.isRunning,
    this.currentAction,
  }) : super(key: key);

  @override
  State<ValidationProgressIndicator> createState() => _ValidationProgressIndicatorState();
}

class _ValidationProgressIndicatorState extends State<ValidationProgressIndicator>
    with TickerProviderStateMixin {
  late AnimationController _controller;
  late Animation<double> _animation;

  @override
  void initState() {
    super.initState();
    _controller = AnimationController(
      duration: const Duration(seconds: 2),
      vsync: this,
    );
    _animation = Tween<double>(begin: 0, end: 1).animate(
      CurvedAnimation(parent: _controller, curve: Curves.easeInOut),
    );
    
    if (widget.isRunning) {
      _controller.repeat();
    }
  }

  @override
  void didUpdateWidget(ValidationProgressIndicator oldWidget) {
    super.didUpdateWidget(oldWidget);
    if (widget.isRunning != oldWidget.isRunning) {
      if (widget.isRunning) {
        _controller.repeat();
      } else {
        _controller.stop();
      }
    }
  }

  @override
  void dispose() {
    _controller.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    if (!widget.isRunning) {
      return const SizedBox.shrink();
    }

    return Container(
      padding: const EdgeInsets.all(16),
      margin: const EdgeInsets.symmetric(vertical: 8),
      decoration: BoxDecoration(
        color: Colors.blue[50],
        borderRadius: BorderRadius.circular(8),
        border: Border.all(color: Colors.blue[200]!),
      ),
      child: Column(
        children: [
          Row(
            children: [
              AnimatedBuilder(
                animation: _animation,
                builder: (context, child) {
                  return Transform.rotate(
                    angle: _animation.value * 2 * 3.14159,
                    child: const Icon(
                      Icons.refresh,
                      color: Colors.blue,
                      size: 20,
                    ),
                  );
                },
              ),
              const SizedBox(width: 12),
              Expanded(
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Text(
                      'Running ${widget.stepName}',
                      style: const TextStyle(
                        fontWeight: FontWeight.w500,
                        color: Colors.blue,
                      ),
                    ),
                    if (widget.currentAction != null)
                      Text(
                        widget.currentAction!,
                        style: TextStyle(
                          fontSize: 12,
                          color: Colors.grey[600],
                        ),
                      ),
                  ],
                ),
              ),
            ],
          ),
          const SizedBox(height: 8),
          const LinearProgressIndicator(
            valueColor: AlwaysStoppedAnimation<Color>(Colors.blue),
          ),
        ],
      ),
    );
  }
}