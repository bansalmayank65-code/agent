import 'package:flutter/material.dart';

class GradientBackground extends StatelessWidget {
  final Widget child;
  final List<Color>? colors;
  final AlignmentGeometry begin;
  final AlignmentGeometry end;

  const GradientBackground({
    Key? key,
    required this.child,
    this.colors,
    this.begin = Alignment.topLeft,
    this.end = Alignment.bottomRight,
  }) : super(key: key);

  @override
  Widget build(BuildContext context) {
    return Container(
      decoration: BoxDecoration(
        gradient: LinearGradient(
          begin: begin,
          end: end,
          colors: colors ?? [
            const Color(0xFFF7FAFC),
            const Color(0xFFEDF2F7),
            const Color(0xFFE2E8F0),
          ],
        ),
      ),
      child: child,
    );
  }
}

class AnimatedGradientBackground extends StatefulWidget {
  final Widget child;
  final Duration duration;
  final List<List<Color>> gradients;

  const AnimatedGradientBackground({
    Key? key,
    required this.child,
    this.duration = const Duration(seconds: 4),
    this.gradients = const [
      [Color(0xFF667eea), Color(0xFF764ba2)],
      [Color(0xFFf093fb), Color(0xFFf5576c)],
      [Color(0xFF4facfe), Color(0xFF00f2fe)],
      [Color(0xFF43e97b), Color(0xFF38f9d7)],
    ],
  }) : super(key: key);

  @override
  State<AnimatedGradientBackground> createState() => _AnimatedGradientBackgroundState();
}

class _AnimatedGradientBackgroundState extends State<AnimatedGradientBackground>
    with TickerProviderStateMixin {
  late AnimationController _controller;
  late Animation<double> _animation;
  int _currentIndex = 0;

  @override
  void initState() {
    super.initState();
    _controller = AnimationController(
      duration: widget.duration,
      vsync: this,
    );
    _animation = Tween<double>(
      begin: 0.0,
      end: 1.0,
    ).animate(_controller);

    _controller.addStatusListener((status) {
      if (status == AnimationStatus.completed) {
        setState(() {
          _currentIndex = (_currentIndex + 1) % widget.gradients.length;
        });
        _controller.reset();
        _controller.forward();
      }
    });

    _controller.forward();
  }

  @override
  void dispose() {
    _controller.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    return AnimatedBuilder(
      animation: _animation,
      builder: (context, child) {
        final currentGradient = widget.gradients[_currentIndex];
        final nextGradient = widget.gradients[(_currentIndex + 1) % widget.gradients.length];
        
        return Container(
          decoration: BoxDecoration(
            gradient: LinearGradient(
              begin: Alignment.topLeft,
              end: Alignment.bottomRight,
              colors: [
                Color.lerp(currentGradient[0], nextGradient[0], _animation.value)!,
                Color.lerp(currentGradient[1], nextGradient[1], _animation.value)!,
              ],
            ),
          ),
          child: widget.child,
        );
      },
    );
  }
}