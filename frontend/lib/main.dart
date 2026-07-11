import 'package:flutter/material.dart';

import 'screens/login_screen.dart';

void main() {
  runApp(const MuklogApp());
}

class MuklogApp extends StatelessWidget {
  const MuklogApp({super.key});

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      title: 'muklog',
      theme: ThemeData(
        colorScheme: ColorScheme.fromSeed(seedColor: Colors.teal),
      ),
      home: const LoginScreen(),
    );
  }
}
